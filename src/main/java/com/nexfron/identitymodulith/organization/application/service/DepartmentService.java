package com.nexfron.identitymodulith.organization.application.service;

import com.nexfron.identitymodulith.organization.exception.OrganizationException;
import com.nexfron.identitymodulith.organization.exception.OrganizationException.OrganizationErrorCode;
import com.nexfron.identitymodulith.organization.presentation.dto.DepartmentDto;
import com.nexfron.identitymodulith.organization.domain.model.Department;
import com.nexfron.identitymodulith.organization.domain.repository.JpaDepartmentRepository;
import com.nexfron.identitymodulith.organization.application.port.OrgUserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 부서 관리 서비스
 *
 * 주요 기능:
 * - 부서 CRUD (생성, 조회, 수정, 삭제)
 * - 부서 이동 (재조직)
 * - 조직도 트리 조회
 * - Level 1 RBAC 권한 기반 접근 제어
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final JpaDepartmentRepository departmentRepository;
    private final OrgScopeService orgScopeService;
    private final OrgUserPort orgUserPort;

    /**
     * 부서 생성
     * - parentId 검증 → 도메인 엔티티 생성 → DB 저장
     *
     * @param tenantId 테넌트 ID
     * @param name 부서명 (필수)
     * @param type 부서 타입 (선택)
     * @param parentId 상위 부서 ID (null이면 루트 부서)
     * @return 생성된 부서 정보
     */
    @Transactional
    public DepartmentDto.Response createDepartment(
            String tenantId,
            String name,
            String type,
            String parentId) {

        Objects.requireNonNull(tenantId, "tenantId는 null일 수 없습니다");
        Objects.requireNonNull(name, "name은 null일 수 없습니다");

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("name은 빈 문자열일 수 없습니다");
        }

        log.info("[ORG] 부서 생성 - tenantId={}, name={}, parentId={}", tenantId, name, parentId);

        // 상위 부서 검증
        Department parent = null;
        if (parentId != null && !parentId.trim().isEmpty()) {
            parent = departmentRepository.findByDeptIdAndTenantId(parentId, tenantId)
                    .orElseThrow(() -> new OrganizationException(
                            OrganizationErrorCode.INVALID_PARENT
                    ));
        }

        // 도메인 엔티티 생성 및 저장
        Department department = Department.create(tenantId, name, type, parent);
        Department savedDept = departmentRepository.save(department);

        log.info("[ORG] 부서 생성 완료 - deptId={}", savedDept.getDeptId());

        return DepartmentDto.Response.from(savedDept);
    }

    /**
     * 부서 정보 수정
     * - name, type 필드만 수정 가능
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID
     * @param name 새 부서명 (null이면 변경 안 함)
     * @param type 새 부서 타입 (null이면 변경 안 함)
     * @return 수정된 부서 정보
     */
    @Transactional
    public DepartmentDto.Response updateDepartment(
            String tenantId,
            String deptId,
            String name,
            String type) {

        Department department = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationException.OrganizationErrorCode.DEPARTMENT_NOT_FOUND));

        department.updateInfo(name, type);
        department = departmentRepository.save(department);

        log.info("[ORG] 부서 수정 완료 - deptId={}, name={}", deptId, name);

        return DepartmentDto.Response.from(department);
    }

    /**
     * 부서 이동 (재조직)
     * - 부서를 다른 부모 부서 하위로 이동
     * - 순환 참조 검사, 하위 부서 경로 일괄 업데이트
     *
     * @param tenantId 테넌트 ID
     * @param actorUserId 작업 수행 사용자 ID (권한 검증용)
     * @param deptId 이동할 부서 ID
     * @param newParentId 새 상위 부서 ID
     */
    public void moveDepartment(
            String tenantId,
            UUID actorUserId,
            String deptId,
            String newParentId) {

        // 검증 (읽기 전용)
        validateMoveDepartment(tenantId, actorUserId, deptId, newParentId);

        // 실행 (쓰기 트랜잭션)
        executeMoveDepartment(tenantId, deptId, newParentId);
    }

    /**
     * 부서 이동 검증 (읽기 전용)
     * - 권한 검증, 순환 참조 체크
     */
    @Transactional(readOnly = true)
    private void validateMoveDepartment(
            String tenantId,
            UUID actorUserId,
            String deptId,
            String newParentId) {

        Department target = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        // 권한 검증
        Set<String> accessibleDeptIds = orgScopeService.getAccessibleDepartmentIds(
                tenantId,
                actorUserId
        );
        if (!accessibleDeptIds.contains(deptId)) {
            log.warn("[ORG] 부서 이동 권한 없음 - userId={}, deptId={}", actorUserId, deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        // 순환 참조 방지
        if (newParentId != null) {
            Department newParent = departmentRepository.findByDeptIdAndTenantId(newParentId, tenantId)
                    .orElseThrow(() -> new OrganizationException(
                            OrganizationErrorCode.INVALID_PARENT
                    ));

            if (newParent.getOrgPath().startsWith(target.getOrgPath())) {
                log.warn("[ORG] 순환 참조 감지 - deptId={}, newParentId={}", deptId, newParentId);
                throw new OrganizationException(
                        OrganizationErrorCode.INVALID_PARENT
                );
            }
        }

        log.debug("[ORG] 부서 이동 검증 완료 - deptId={}", deptId);
    }

    /**
     * 부서 이동 실행 (쓰기 트랜잭션)
     * - 부서 상위 변경, 하위 부서 경로 일괄 업데이트
     */
    @Transactional
    private void executeMoveDepartment(
            String tenantId,
            String deptId,
            String newParentId) {

        Department target = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        String originalOrgPath = target.getOrgPath();

        Department newParent = null;
        if (newParentId != null) {
            newParent = departmentRepository.findByDeptIdAndTenantId(newParentId, tenantId)
                    .orElseThrow(() -> new OrganizationException(
                            OrganizationErrorCode.INVALID_PARENT
                    ));
        }

        target.changeParent(newParent);
        departmentRepository.save(target);

        // 하위 부서 경로 일괄 업데이트
        String newParentPath = newParent != null ? newParent.getOrgPath() : "";
        List<Department> childDepts = departmentRepository
                .findByTenantIdAndOrgPathStartsWith(tenantId, originalOrgPath)
                .stream()
                .filter(d -> !d.getDeptId().equals(target.getDeptId()))
                .collect(Collectors.toList());

        for (Department child : childDepts) {
            String relativeSubPath = child.getOrgPath()
                    .substring(originalOrgPath.length());

            String newOrgPath = newParentPath + "/" + target.getDeptId() + relativeSubPath;
            child.setOrgPath(newOrgPath);

            int newDepth = newParentPath.isEmpty() ?
                    1 :
                    (newParent.getDepth() + 1) + (child.getDepth() - target.getDepth());
            child.setDepth(newDepth);
        }

        departmentRepository.saveAll(childDepts);

        log.info("[ORG] 부서 이동 완료 - deptId={}, 하위={}개", deptId, childDepts.size());
    }


    /**
     * 부서 삭제
     * - 하위 부서, 소속 사용자 존재 여부 검증
     * - Level 1 RBAC 권한 검증
     *
     * @param tenantId 테넌트 ID
     * @param actorUserId 작업 수행 사용자 ID
     * @param deptId 삭제할 부서 ID
     */
    @Transactional
    public void deleteDepartment(
            String tenantId,
            UUID actorUserId,
            String deptId) {

        log.info("[ORG] 부서 삭제 요청 - deptId={}", deptId);

        // 부서 조회
        Department dept = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        // 권한 검증
        Set<String> accessibleDeptIds = orgScopeService.getAccessibleDepartmentIds(
                tenantId,
                actorUserId
        );
        if (!accessibleDeptIds.contains(deptId)) {
            log.warn("[ORG] 부서 삭제 권한 없음 - userId={}, deptId={}", actorUserId, deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        // 하위 부서 존재 여부 검증
        if (departmentRepository.existsByParent(dept)) {
            log.warn("[ORG] 하위 부서 존재로 삭제 불가 - deptId={}", deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.CHILD_DEPARTMENT_EXISTS
            );
        }

        // 소속 활성 사용자 존재 여부 검증
        boolean hasActiveUsers = orgUserPort.existsActiveUserInDepartment(
                tenantId,
                deptId
        );
        if (hasActiveUsers) {
            log.warn("[ORG] 소속 활성 사용자 존재로 삭제 불가 - deptId={}", deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.ACTIVE_USERS_EXIST
            );
        }

        // 삭제
        departmentRepository.delete(dept);

        log.info("[ORG] 부서 삭제 완료 - deptId={}", deptId);
    }

    /**
     * 전체 조직도 트리 조회
     * - 권한 검증 없이 모든 부서 반환
     *
     * @param tenantId 테넌트 ID
     * @return 조직도 트리 (루트부터)
     */
    @Transactional(readOnly = true)
    public List<DepartmentDto.Response> getDepartmentTree(String tenantId) {
        List<Department> allDepts = departmentRepository.findAllByTenantId(tenantId);
        return buildTree(allDepts);
    }

    /**
     * 사용자 권한 범위 내 조직도 트리 조회 (Level 1 RBAC)
     * - 접근 가능한 부서만 필터링하여 반환
     *
     * @param tenantId 테넌트 ID
     * @param actorUserId 조회하는 사용자 ID
     * @return 권한 범위 내 조직도 트리
     */
    @Transactional(readOnly = true)
    public List<DepartmentDto.Response> getDepartmentTreeWithinScope(
            String tenantId,
            UUID actorUserId) {

        // 사용자가 접근 가능한 부서 ID 집합 조회
        Set<String> scopeDeptIds = orgScopeService.getAccessibleDepartmentIds(
                tenantId,
                actorUserId
        );

        if (scopeDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 접근 가능한 부서만 필터링
        List<Department> allDepts = departmentRepository.findAllByTenantId(tenantId);
        List<Department> scopedDepts = allDepts.stream()
                .filter(d -> scopeDeptIds.contains(d.getDeptId()))
                .collect(Collectors.toList());

        return buildTree(scopedDepts);
    }

    /**
     * 부서 리스트를 트리 구조 DTO로 변환
     * - ID-DTO 맵 구성 → 부모-자식 관계 연결 → 루트 추출
     */
    private List<DepartmentDto.Response> buildTree(List<Department> depts) {
        // ID -> DTO 맵 구성
        Map<String, DepartmentDto.Response> dtoMap = depts.stream()
                .map(DepartmentDto.Response::from)
                .collect(Collectors.toMap(
                        DepartmentDto.Response::getDeptId,
                        dto -> dto
                ));

        List<DepartmentDto.Response> roots = new ArrayList<>();

        // 부모-자식 관계 구성
        for (DepartmentDto.Response dto : dtoMap.values()) {
            String parentId = dto.getParentId();

            if (parentId == null) {
                roots.add(dto);
            } else {
                DepartmentDto.Response parent = dtoMap.get(parentId);
                if (parent != null) {
                    parent.addChild(dto);
                } else {
                    roots.add(dto); // 부모 누락 시 루트로 처리
                }
            }
        }

        // orgPath 순서로 정렬
        roots.sort(Comparator.comparing(
                DepartmentDto.Response::getOrgPath,
                Comparator.nullsFirst(String::compareTo)
        ));

        return roots;
    }

    /**
     * 키워드로 부서 검색 (부서명 기준)
     */
    @Transactional(readOnly = true)
    public List<DepartmentDto.Response> searchDepartments(String tenantId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        List<Department> departments = departmentRepository
                .findByTenantIdAndNameContainingIgnoreCase(tenantId, keyword);

        return departments.stream()
                .map(DepartmentDto.Response::from)
                .sorted(Comparator.comparing(DepartmentDto.Response::getOrgPath))
                .collect(Collectors.toList());
    }

    /**
     * 특정 깊이(depth)의 부서 조회
     */
    @Transactional(readOnly = true)
    public List<DepartmentDto.Response> getDepartmentsByDepth(String tenantId, int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("depth는 0 이상이어야 합니다.");
        }

        List<Department> departments = departmentRepository
                .findByTenantIdAndDepth(tenantId, depth);

        return departments.stream()
                .map(DepartmentDto.Response::from)
                .sorted(Comparator.comparing(DepartmentDto.Response::getOrgPath))
                .collect(Collectors.toList());
    }

    /**
     * 특정 타입의 부서 조회
     */
    @Transactional(readOnly = true)
    public List<DepartmentDto.Response> getDepartmentsByType(String tenantId, String type) {
        if (type == null || type.trim().isEmpty()) {
            return List.of();
        }

        List<Department> departments = departmentRepository
                .findByTenantIdAndType(tenantId, type);

        return departments.stream()
                .map(DepartmentDto.Response::from)
                .sorted(Comparator.comparing(DepartmentDto.Response::getOrgPath))
                .collect(Collectors.toList());
    }

    /**
     * 부서 통계 정보 조회
     * - 전체/활성 직원 수, 직속/전체 하위 부서 수
     */
    @Transactional(readOnly = true)
    public DepartmentDto.Statistics getDepartmentStatistics(String tenantId, String deptId) {
        // 부서 조회
        Department department = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        // 직원 수 조회
        long totalEmployees = orgUserPort.countEmployeesByDepartment(tenantId, deptId);
        long activeEmployees = orgUserPort.countActiveEmployeesByDepartment(tenantId, deptId);

        // 직속 하위 부서 수
        long childDeptCount = departmentRepository.findAllByTenantId(tenantId).stream()
                .filter(dept -> dept.getParent() != null
                        && dept.getParent().getDeptId().equals(deptId))
                .count();

        // 전체 하위 부서 수 (orgPath 기반)
        String pathPrefix = department.getOrgPath();
        long descendantDeptCount = departmentRepository
                .findByTenantIdAndOrgPathStartsWith(tenantId, pathPrefix).stream()
                .filter(dept -> !dept.getDeptId().equals(deptId))
                .count();

        return DepartmentDto.Statistics.builder()
                .deptId(department.getDeptId())
                .name(department.getName())
                .type(department.getType())
                .depth(department.getDepth())
                .totalEmployees(totalEmployees)
                .activeEmployees(activeEmployees)
                .childDeptCount(childDeptCount)
                .descendantDeptCount(descendantDeptCount)
                .build();
    }

    /**
     * 부서별 사용자 목록 조회
     * - 하위 부서 포함 여부 선택 가능
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID
     * @param includeSubDepartments 하위 부서 포함 여부
     * @return 부서 소속 사용자 정보
     */
    @Transactional(readOnly = true)
    public DepartmentDto.DepartmentMembers getDepartmentMembers(
            String tenantId, 
            String deptId, 
            boolean includeSubDepartments) {
        
        // 부서 존재 확인
        Department department = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        // 대상 부서 ID 목록 결정
        Set<String> targetDeptIds;
        if (includeSubDepartments) {
            // 현재 부서 + 모든 하위 부서
            String pathPrefix = department.getOrgPath();
            targetDeptIds = departmentRepository
                    .findByTenantIdAndOrgPathStartsWith(tenantId, pathPrefix).stream()
                    .map(Department::getDeptId)
                    .collect(Collectors.toSet());
        } else {
            // 현재 부서만
            targetDeptIds = Set.of(deptId);
        }

        // 부서별 사용자 조회 (OrgUserPort 사용)
        List<DepartmentDto.MemberInfo> members = new ArrayList<>();
        long activeCount = 0;
        long retiredCount = 0;

        for (String targetDeptId : targetDeptIds) {
            List<DepartmentDto.MemberInfo> deptMembers = 
                orgUserPort.getUsersByDepartment(tenantId, targetDeptId);
            
            members.addAll(deptMembers);
            
            for (DepartmentDto.MemberInfo member : deptMembers) {
                if ("ACTIVE".equals(member.status())) {
                    activeCount++;
                } else if ("RETIRED".equals(member.status())) {
                    retiredCount++;
                }
            }
        }

        return DepartmentDto.DepartmentMembers.builder()
                .deptId(deptId)
                .deptName(department.getName())
                .includeSubDepartments(includeSubDepartments)
                .totalCount(members.size())
                .activeCount(activeCount)
                .retiredCount(retiredCount)
                .members(members)
                .build();
    }
}

