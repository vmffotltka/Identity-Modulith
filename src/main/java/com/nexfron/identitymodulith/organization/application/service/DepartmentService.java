package com.nexfron.identitymodulith.organization.application.service;

import com.nexfron.identitymodulith.organization.common.exception.BusinessException;
import com.nexfron.identitymodulith.organization.common.exception.EntityNotFoundException;
import com.nexfron.identitymodulith.organization.presentation.dto.DepartmentDto;
import com.nexfron.identitymodulith.organization.domain.model.Department;
import com.nexfron.identitymodulith.organization.domain.repository.JpaDepartmentRepository;
import com.nexfron.identitymodulith.organization.application.port.OrgUserPort;
import com.nexfron.identitymodulith.organization.common.exception.InvalidDepartmentMoveException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DepartmentService
 *
 * 역할:
 * - Organization(부서) 트리의 비즈니스 규칙을 담당한다.
 * - Controller는 요청/응답만, "판단"은 여기서 한다.
 *
 * 핵심 설계 규칙(팀 공통):
 * 1) 모든 데이터 접근은 tenantId 기준으로 격리되어야 한다.
 * 2) Level2(RBAC Scope) 권한 검증은 OrgScopeService에서 수행한다.
 * 3) Users 테이블과의 결합은 OrgUserPort로만 한다(직접 JPA 접근 금지).
 *
 * DB 제약:
 * - 스키마(테이블/컬럼) 변경은 불가하므로,
 *   조회 메서드/서비스 로직으로 안전장치를 만든다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final JpaDepartmentRepository jpaDepartmentRepository;
    private final OrgScopeService orgScopeService;
    private final OrgUserPort orgUserPort;

    /* ==========================================================
     * 공통 헬퍼 (팀 규칙 강제)
     * ========================================================== */

    /**
     * tenantId 포함 단건 조회를 강제한다.
     * - Service에서 findById + tenant filter 패턴을 없애기 위한 안전장치
     * - "tenant 조건 누락" 실수를 구조적으로 방지한다.
     */
    private Department getDeptOrThrow(String tenantId, Long deptId, String notFoundMessage) {
        return jpaDepartmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(notFoundMessage));
    }

    /* ==========================================================
     * 부서 생성
     * ========================================================== */

    @Transactional
    public DepartmentDto.Response createDepartment(String tenantId, String name, String type, Long parentId) {
        Department parent = null;

        if (parentId != null) {
            parent = getDeptOrThrow(tenantId, parentId, "상위 부서를 찾을 수 없습니다.");
        }

        Department department = Department.create(tenantId, name, type, parent);
        Department savedDept = jpaDepartmentRepository.save(department);

        return DepartmentDto.Response.from(savedDept);
    }

    /* ==========================================================
     * 부서 이동
     * ========================================================== */

    @Transactional
    public void moveDepartment(String tenantId,
                               UUID actorUserId,
                               Long deptId,
                               Long newParentId) {

        if (newParentId == null) {
            throw new BusinessException("새 상위 부서는 필수입니다.");
        }

        // ✅ 404를 먼저 확정 (tenant 포함)
        Department target = getDeptOrThrow(tenantId, deptId, "이동할 부서를 찾을 수 없습니다.");
        Department newParent = getDeptOrThrow(tenantId, newParentId, "새 상위 부서를 찾을 수 없습니다.");

        // ✅ 그 다음 403 (스코프 기반 권한 체크)
        if (!orgScopeService.canAccessDepartment(tenantId, actorUserId, deptId)) {
            throw new BusinessException("해당 부서를 이동할 권한이 없습니다.");
        }
        if (!orgScopeService.canAccessDepartment(tenantId, actorUserId, newParentId)) {
            throw new BusinessException("새 상위 부서를 지정할 권한이 없습니다.");
        }

        String oldPath = target.getOrgPath();
        if (oldPath == null) {
            throw new BusinessException("부서 경로(orgPath)가 비어 있어 이동할 수 없습니다.");
        }

        try {
            target.changeParent(newParent);
        } catch (InvalidDepartmentMoveException e) {
            throw new BusinessException(e.getMessage());
        }

        List<Department> descendants =
                jpaDepartmentRepository.findByTenantIdAndOrgPathStartsWith(tenantId, oldPath + "/");

        descendants.sort(Comparator.comparingInt(Department::getDepth));
        for (Department child : descendants) {
            child.updatePath();
        }
    }


    /* ==========================================================
     * 부서 삭제
     * ========================================================== */

    @Transactional
    public void deleteDepartment(String tenantId, UUID actorUserId, Long deptId) {

        // ✅ 1) 404 먼저 확정 (tenant 격리 포함)
        Department target = getDeptOrThrow(tenantId, deptId, "삭제할 부서를 찾을 수 없습니다.");

        // ✅ 2) 403 (권한)
        if (!orgScopeService.canAccessDepartment(tenantId, actorUserId, deptId)) {
            throw new BusinessException("해당 부서를 삭제할 권한이 없습니다.");
        }

        // ✅ 3) 삭제 불가 조건들 (정확한 메시지)
        // 3-1) 하위 부서가 있으면 삭제 불가
        // (레포 메서드는 너희 코드에 맞춰 이름만 조정하면 됨)
        boolean hasChildren = jpaDepartmentRepository.existsByTenantIdAndParent_DeptId(tenantId, deptId);
        if (hasChildren) {
            throw new BusinessException("하위 부서가 존재하여 삭제할 수 없습니다.");
        }

        // 3-2) 활성 사용자가 있으면 삭제 불가
        if (orgUserPort.existsActiveUserInDepartment(tenantId, deptId)) {
            throw new BusinessException("활성 사용자가 존재하여 삭제할 수 없습니다.");
        }

        // ✅ 4) 삭제
        jpaDepartmentRepository.delete(target);
    }


    /* ==========================================================
     * 트리 조회
     * ========================================================== */

    public List<DepartmentDto.Response> getDepartmentTree(String tenantId) {
        List<Department> allDepts = jpaDepartmentRepository.findAllByTenantId(tenantId);
        return buildTree(allDepts);
    }

    public List<DepartmentDto.Response> getDepartmentTreeWithinScope(String tenantId, UUID actorUserId) {
        Set<Long> scopeDeptIds = orgScopeService.getAccessibleDepartmentIds(tenantId, actorUserId);

        if (scopeDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        // ✅ 개선 포인트: 전체 조회 + stream filter 제거
        List<Department> scopedDepts = jpaDepartmentRepository.findAllByTenantIdAndDeptIdIn(
                tenantId,
                new ArrayList<>(scopeDeptIds)
        );

        return buildTree(scopedDepts);
    }

    /**
     * 리스트 → 트리 조립 헬퍼
     *
     * 규칙:
     * - parentId가 null이면 루트
     * - parent가 누락된 노드는 데이터 불일치로 간주하고 루트로 승격(화면 깨짐 방지)
     */
    private List<DepartmentDto.Response> buildTree(List<Department> depts) {
        Map<Long, DepartmentDto.Response> dtoMap = depts.stream()
                .map(DepartmentDto.Response::from)
                .collect(Collectors.toMap(DepartmentDto.Response::getDeptId, dto -> dto));

        List<DepartmentDto.Response> roots = new ArrayList<>();

        for (DepartmentDto.Response dto : dtoMap.values()) {
            Long parentId = dto.getParentId();
            if (parentId == null) {
                roots.add(dto);
                continue;
            }

            DepartmentDto.Response parent = dtoMap.get(parentId);
            if (parent != null) {
                parent.addChild(dto);
            } else {
                roots.add(dto);
            }
        }

        roots.sort(Comparator.comparing(
                DepartmentDto.Response::getOrgPath,
                Comparator.nullsFirst(String::compareTo)
        ));

        return roots;
    }
}
