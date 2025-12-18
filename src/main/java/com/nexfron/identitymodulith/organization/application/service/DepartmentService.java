package com.nexfron.identitymodulith.organization.application.service;

import com.nexfron.identitymodulith.organization.common.BusinessException;
import com.nexfron.identitymodulith.organization.common.EntityNotFoundException;
import com.nexfron.identitymodulith.organization.api.dto.DepartmentDto;
import com.nexfron.identitymodulith.organization.domain.model.Department;
import com.nexfron.identitymodulith.organization.infrastructure.repository.DepartmentRepository;
import com.nexfron.identitymodulith.organization.application.port.OrgUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DepartmentService
 *
 * - 조직(부서) 트리 관리의 핵심 비즈니스 로직
 *   - 부서 생성
 *   - 부서 이동 (순환 참조 방지 + orgPath 재계산)
 *   - 부서 삭제 (하위 부서 + 소속 인원 체크)
 *   - 전체/스코프별 조직도 트리 조회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrgScopeService orgScopeService; // Level 2 RBAC 스코프 계산용
    private final OrgUserPort orgUserPort;         // Users 테이블 간접 접근 (소속 인원 체크용)

    /* ==========================================================
     * 부서 생성
     * ========================================================== */

    /**
     * 부서 생성
     *
     * @param tenantId 테넌트 ID
     * @param name     부서명
     * @param type     부서 타입 (팀/본부 등)
     * @param parentId 상위 부서 ID (없으면 루트)
     */
    @Transactional
    public DepartmentDto.Response createDepartment(String tenantId, String name, String type, Long parentId) {
        // 1) 상위 부서 조회 (있으면)
        Department parent = null;
        if (parentId != null) {
            parent = departmentRepository.findById(parentId)
                    .filter(d -> d.getTenantId().equals(tenantId))
                    .orElseThrow(() -> new EntityNotFoundException("상위 부서를 찾을 수 없습니다."));
        }

        // 2) 엔티티 생성 및 저장
        Department department = Department.create(tenantId, name, type, parent);
        Department savedDept = departmentRepository.save(department);

        // 3) 응답 DTO 변환
        return DepartmentDto.Response.from(savedDept);
    }

    /* ==========================================================
     * 부서 이동 (Restructure)
     * ========================================================== */

    /**
     * 부서 이동
     *
     * - actorUserId 의 데이터 스코프를 기반으로 권한 체크 수행
     * - 순환 참조(자기 하위로 이동) 방지
     * - orgPath 기반으로 하위 부서의 경로까지 재계산
     */
    @Transactional
    public void moveDepartment(String tenantId,
                               UUID actorUserId,
                               Long deptId,
                               Long newParentId) {

        // 0) 권한 체크: 이동 대상 부서를 건드릴 수 있는지
        if (!orgScopeService.canAccessDepartment(tenantId, actorUserId, deptId)) {
            throw new BusinessException("해당 부서를 이동할 권한이 없습니다.");
        }
        // (선택) 새 상위 부서에 대한 접근 권한도 체크하고 싶다면 아래 같이 한 줄 추가
        if (!orgScopeService.canAccessDepartment(tenantId, actorUserId, newParentId)) {
            throw new BusinessException("새 상위 부서를 지정할 권한이 없습니다.");
        }

        // 1) 이동 대상 부서 조회
        Department target = departmentRepository.findById(deptId)
                .filter(d -> d.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException("이동할 부서를 찾을 수 없습니다."));

        // 2) 새 상위 부서 조회
        Department newParent = departmentRepository.findById(newParentId)
                .filter(d -> d.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException("새 상위 부서를 찾을 수 없습니다."));

        // 3) 순환 참조 검사
        if (newParent.getOrgPath() != null
                && target.getOrgPath() != null
                && newParent.getOrgPath().startsWith(target.getOrgPath())) {
            // 예: /1/5 를 /1/5/10 밑으로 옮기려는 경우
            throw new BusinessException("자신의 하위 부서로 이동할 수 없습니다 (순환 참조).");
        }

        // 4) 이동 전 기존 경로를 기억
        String oldPath = target.getOrgPath();

        // 5) 부모 변경 + 자신의 orgPath/depth 업데이트
        target.changeParent(newParent);

        // 6) 하위 부서 orgPath 재계산
        //    oldPath 로 시작하는 모든 부서를 가져와 부모 depth 순으로 정렬한 뒤 updatePath 호출
        List<Department> descendants = departmentRepository
                .findByTenantIdAndOrgPathStartsWith(tenantId, oldPath + "/");

        descendants.sort(Comparator.comparingInt(Department::getDepth));

        for (Department child : descendants) {
            child.updatePath();
        }
    }

    /* ==========================================================
     * 부서 삭제
     * ========================================================== */

    /**
     * 부서 삭제
     *
     * - 하위 부서가 존재하면 삭제 불가
     * - 활성 사용자가 한 명이라도 소속되어 있으면 삭제 불가
     * - actorUserId 의 스코프를 기준으로 삭제 권한 체크
     */
    @Transactional
    public void deleteDepartment(String tenantId,
                                 UUID actorUserId,
                                 Long deptId) {

        // 0) 권한 체크
        if (!orgScopeService.canAccessDepartment(tenantId, actorUserId, deptId)) {
            throw new BusinessException("해당 부서를 삭제할 권한이 없습니다.");
        }

        // 1) 삭제 대상 조회
        Department dept = departmentRepository.findById(deptId)
                .filter(d -> d.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException("삭제할 부서를 찾을 수 없습니다."));

        // 2) 하위 부서 존재 체크
        if (departmentRepository.existsByParent(dept)) {
            throw new BusinessException("하위 부서가 존재하여 삭제할 수 없습니다.");
        }

        // 3) 소속 인원 존재 체크 (Users 테이블은 OrgUserPort 로 간접 접근)
        boolean hasUsers = orgUserPort.existsActiveUserInDepartment(tenantId, deptId);
        if (hasUsers) {
            throw new BusinessException("소속 구성원이 존재하여 삭제할 수 없습니다.");
        }

        // 4) 실제 삭제
        departmentRepository.delete(dept);
    }

    /* ==========================================================
     * 조직도 트리 조회 (전체 / 스코프)
     * ========================================================== */

    /**
     * 특정 테넌트의 전체 조직도 트리 조회
     * - 관리 콘솔, 전체 조직도 화면 등에 사용
     */
    public List<DepartmentDto.Response> getDepartmentTree(String tenantId) {
        List<Department> allDepts = departmentRepository.findAllByTenantId(tenantId);
        return buildTree(allDepts);
    }

    /**
     * [Level 2 RBAC]
     * actorUserId 가 "접근 가능한 범위 내에서만" 조직도 트리 조회
     *
     * - OrgScopeService.getAccessibleDepartmentIds(...) 로 스코프를 계산하고,
     *   해당 부서들만 대상으로 트리를 구성한다.
     */
    public List<DepartmentDto.Response> getDepartmentTreeWithinScope(String tenantId,
                                                                     UUID actorUserId) {
        // 1) 이 유저가 접근 가능한 부서 ID 집합
        Set<Long> scopeDeptIds = orgScopeService.getAccessibleDepartmentIds(tenantId, actorUserId);

        if (scopeDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) 해당 부서들만 가져오기
        List<Department> allDepts = departmentRepository.findAllByTenantId(tenantId);
        List<Department> scopedDepts = allDepts.stream()
                .filter(d -> scopeDeptIds.contains(d.getDeptId()))
                .collect(Collectors.toList());

        // 3) 트리로 조립
        return buildTree(scopedDepts);
    }

    /* ==========================================================
     * List<Department> -> Tree DTO 변환 헬퍼
     * ========================================================== */

    /**
     * Department 리스트를 계층 구조로 조립
     *
     * 1) 모든 Department 를 Response DTO 로 변환하여 Map 에 캐싱
     * 2) parentId 기준으로 children 리스트를 채운다.
     */
    private List<DepartmentDto.Response> buildTree(List<Department> depts) {
        // 1) ID -> DTO 맵핑
        Map<Long, DepartmentDto.Response> dtoMap = depts.stream()
                .map(DepartmentDto.Response::from)
                .collect(Collectors.toMap(DepartmentDto.Response::getDeptId, dto -> dto));

        List<DepartmentDto.Response> roots = new ArrayList<>();

        // 2) 부모-자식 관계 구성
        for (DepartmentDto.Response dto : dtoMap.values()) {
            Long parentId = dto.getParentId();
            if (parentId == null) {
                // 루트 노드
                roots.add(dto);
            } else {
                DepartmentDto.Response parent = dtoMap.get(parentId);
                if (parent != null) {
                    parent.addChild(dto);
                } else {
                    // 부모 정보가 누락된 경우(데이터 불일치)는 루트처럼 취급
                    roots.add(dto);
                }
            }
        }

        // 3) 보기 좋게 orgPath 기준으로 정렬 (선택 사항)
        roots.sort(Comparator.comparing(DepartmentDto.Response::getOrgPath,
                Comparator.nullsFirst(String::compareTo)));

        return roots;
    }
}
