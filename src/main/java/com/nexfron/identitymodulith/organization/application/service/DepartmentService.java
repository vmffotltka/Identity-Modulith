package com.nexfron.identitymodulith.organization.application.service;

import com.nexfron.identitymodulith.organization.common.exception.OrganizationException;
import com.nexfron.identitymodulith.organization.common.exception.OrganizationException.OrganizationErrorCode;
import com.nexfron.identitymodulith.organization.presentation.dto.DepartmentDto;
import com.nexfron.identitymodulith.organization.domain.model.Department;
import com.nexfron.identitymodulith.organization.domain.repository.JpaDepartmentRepository;
import com.nexfron.identitymodulith.organization.application.port.OrgUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DepartmentService - 조직(부서) 관리 핵심 비즈니스 로직
 *
 * <h2>주요 책임:</h2>
 * <ul>
 *   <li><b>부서 생성</b>: 새로운 부서를 조직 트리에 추가</li>
 *   <li><b>부서 이동</b>: 부서를 다른 부모 하위로 이동 (순환 참조 방지, 자식 경로 재계산)</li>
 *   <li><b>부서 삭제</b>: 부서 삭제 전 하위 부서/소속 인원 검증</li>
 *   <li><b>조직도 조회</b>: 전체 트리 또는 사용자 권한 범위(Level 2 RBAC) 내의 트리 조회</li>
 * </ul>
 *
 * <h2>핵심 개념:</h2>
 * <ul>
 *   <li><b>orgPath</b>: "/" 구분자로 계층을 표현하는 경로 (예: /1/5/10)</li>
 *   <li><b>depth</b>: 루트로부터의 깊이 (루트=0)</li>
 *   <li><b>Level 2 RBAC</b>: OrgScopeService를 통한 사용자별 접근 가능 부서 범위 제어</li>
 *   <li><b>모듈 간 통신</b>: OrgUserPort를 통해 user 모듈의 활성 사용자 정보 조회</li>
 * </ul>
 *
 * <h2>트랜잭션 전략:</h2>
 * <ul>
 *   <li>클래스 레벨: 기본적으로 읽기 전용 (@Transactional(readOnly = true))</li>
 *   <li>개별 메서드: 쓰기 작업에 대해서만 @Transactional 명시</li>
 * </ul>
 *
 * @author Identity System Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    /** 부서 데이터 접근 리포지토리 */
    private final JpaDepartmentRepository departmentRepository;

    /** Level 2 RBAC 스코프 계산 및 권한 검증 서비스 */
    private final OrgScopeService orgScopeService;

    /** User 모듈의 사용자 정보 조회 (포트-어댑터 패턴) */
    private final OrgUserPort orgUserPort;

    /* ============================================================
     * CRUD: 부서 생성
     * ============================================================ */

    /**
     * 새로운 부서를 조직 트리에 생성합니다.
     *
     * <h3>처리 흐름:</h3>
     * <ol>
     *   <li>parentId가 지정되면 상위 부서 존재 여부 검증</li>
     *   <li>Department 도메인 엔티티 생성 (팩토리 메서드 사용)</li>
     *   <li>DB에 저장 후 @PostPersist로 orgPath 확정</li>
     *   <li>DTO 변환 후 응답</li>
     * </ol>
     *
     * <h3>예외:</h3>
     * <ul>
     *   <li>parentId가 유효하지 않으면 INVALID_PARENT (400)</li>
     * </ul>
     *
     * @param tenantId  멀티테넌트 환경의 테넌트 ID
     * @param name      부서명 (필수)
     * @param type      부서 타입 (예: TEAM, DIVISION 등)
     * @param parentId  상위 부서 ID (null이면 루트)
     * @return 생성된 부서 정보 DTO
     * @throws OrganizationException INVALID_PARENT - 상위 부서가 존재하지 않는 경우
     */
    @Transactional
    public DepartmentDto.Response createDepartment(
            String tenantId,
            String name,
            String type,
            Long parentId) {

        // 1) 상위 부서 존재 여부 검증 (있는 경우만)
        Department parent = null;
        if (parentId != null) {
            parent = departmentRepository.findById(parentId)
                    .orElseThrow(() -> new OrganizationException(
                            OrganizationErrorCode.INVALID_PARENT
                    ));
        }

        // 2) 도메인 엔티티 생성 (팩토리 메서드)
        Department department = Department.create(tenantId, name, type, parent);

        // 3) 저장 (JPA @PostPersist에서 orgPath가 확정됨)
        Department savedDept = departmentRepository.save(department);

        // 4) DTO 변환 후 반환
        return DepartmentDto.Response.from(savedDept);
    }

    /* ============================================================
     * CRUD: 부서 이동 (재조직)
     * ============================================================ */

    /**
     * 부서를 다른 부모 부서 하위로 이동합니다.
     *
     * <h3>처리 흐름:</h3>
     * <ol>
     *   <li>이동 대상 부서 조회 및 존재 여부 검증</li>
     *   <li>새 상위 부서 조회 및 존재 여부 검증</li>
     *   <li>순환 참조 방지 검사 (자신의 하위로 이동 불가)</li>
     *   <li>부모 변경 및 자신의 orgPath/depth 업데이트</li>
     *   <li>하위 부서들의 orgPath 일괄 재계산</li>
     *   <li>Level 2 RBAC 권한 검증</li>
     * </ol>
     *
     * <h3>예외:</h3>
     * <ul>
     *   <li>DEPARTMENT_NOT_FOUND (404) - 이동 대상 부서 없음</li>
     *   <li>INVALID_PARENT (400) - 새 상위 부서 없음</li>
     *   <li>CIRCULAR_REFERENCE (400) - 순환 참조 시도</li>
     *   <li>INSUFFICIENT_PERMISSION (403) - 권한 없음</li>
     * </ul>
     *
     * @param tenantId      멀티테넌트 ID
     * @param actorUserId   작업 수행 사용자 ID (권한 검증용)
     * @param deptId        이동할 부서 ID
     * @param newParentId   새 상위 부서 ID
     * @throws OrganizationException 위의 4가지 에러 중 하나
     */
    @Transactional
    public void moveDepartment(
            String tenantId,
            UUID actorUserId,
            Long deptId,
            Long newParentId) {

        // 1) 이동 대상 부서 조회
        Department target = departmentRepository.findById(deptId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        // 2) 권한 검증: Level 2 RBAC (사용자가 해당 부서에 접근 가능한지)
        Set<Long> accessibleDeptIds = orgScopeService.getAccessibleDepartmentIds(
                tenantId,
                actorUserId
        );
        if (!accessibleDeptIds.contains(deptId)) {
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        // 3) 새 상위 부서 조회 및 검증
        Department newParent = null;
        if (newParentId != null) {
            newParent = departmentRepository.findById(newParentId)
                    .orElseThrow(() -> new OrganizationException(
                            OrganizationErrorCode.INVALID_PARENT
                    ));
        }

        // 4) 순환 참조 검사
        // 예: /1/5 부서를 /1/5/10 부서 하위로 옮기려는 시도 방지
        if (newParent != null
                && newParent.getOrgPath() != null
                && target.getOrgPath() != null
                && newParent.getOrgPath().startsWith(target.getOrgPath())) {
            throw new OrganizationException(
                    OrganizationErrorCode.CIRCULAR_REFERENCE
            );
        }

        // 5) 부모 변경 및 자신의 경로 업데이트
        target.changeParent(newParent);

        // 6) 하위 부서들의 경로 일괄 재계산
        // orgPath가 변경되었으므로, 모든 자식 부서의 경로도 재계산해야 함
        List<Department> childDepts = departmentRepository
                .findByOrgPathStartsWith(target.getOrgPath())
                .stream()
                .filter(d -> !d.getDeptId().equals(deptId)) // 자신 제외
                .collect(Collectors.toList());

        // depth 순서대로 정렬하여 상위 부서부터 업데이트
        childDepts.sort(Comparator.comparingInt(Department::getDepth));

        for (Department child : childDepts) {
            child.updatePath();
            departmentRepository.save(child);
        }
    }

    /* ============================================================
     * CRUD: 부서 삭제
     * ============================================================ */

    /**
     * 부서를 삭제합니다.
     *
     * <h3>삭제 전 검증 사항:</h3>
     * <ol>
     *   <li>부서 존재 여부</li>
     *   <li>사용자 권한 (Level 2 RBAC)</li>
     *   <li>하위 부서 존재 여부</li>
     *   <li>소속 활성 사용자 존재 여부</li>
     * </ol>
     *
     * <h3>예외:</h3>
     * <ul>
     *   <li>DEPARTMENT_NOT_FOUND (404) - 부서 없음</li>
     *   <li>INSUFFICIENT_PERMISSION (403) - 권한 없음</li>
     *   <li>CHILD_DEPARTMENT_EXISTS (409) - 하위 부서 존재</li>
     *   <li>ACTIVE_USERS_EXIST (409) - 소속 사용자 존재</li>
     * </ul>
     *
     * @param tenantId      멀티테넌트 ID
     * @param actorUserId   작업 수행 사용자 ID
     * @param deptId        삭제할 부서 ID
     * @throws OrganizationException 위의 4가지 에러 중 하나
     */
    @Transactional
    public void deleteDepartment(
            String tenantId,
            UUID actorUserId,
            Long deptId) {

        // 1) 부서 존재 여부 검증
        Department dept = departmentRepository.findById(deptId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        // 2) 권한 검증
        Set<Long> accessibleDeptIds = orgScopeService.getAccessibleDepartmentIds(
                tenantId,
                actorUserId
        );
        if (!accessibleDeptIds.contains(deptId)) {
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        // 3) 하위 부서 존재 여부 검증
        if (departmentRepository.existsByParent(dept)) {
            throw new OrganizationException(
                    OrganizationErrorCode.CHILD_DEPARTMENT_EXISTS
            );
        }

        // 4) 소속 활성 사용자 존재 여부 검증 (포트를 통한 간접 접근)
        boolean hasActiveUsers = orgUserPort.existsActiveUserInDepartment(
                tenantId,
                deptId
        );
        if (hasActiveUsers) {
            throw new OrganizationException(
                    OrganizationErrorCode.ACTIVE_USERS_EXIST
            );
        }

        // 5) 모든 검증 통과 후 삭제
        departmentRepository.delete(dept);
    }

    /* ============================================================
     * Query: 조직도 트리 조회 (전체)
     * ============================================================ */

    /**
     * 특정 테넌트의 <b>전체 조직도 트리</b>를 조회합니다.
     *
     * <p>
     * 권한 검증 없이 모든 부서를 반환하므로,
     * 시스템 관리자나 권한이 충분한 사용자만 호출해야 합니다.
     * </p>
     *
     * <h3>반환 구조:</h3>
     * <ul>
     *   <li>루트 부서들이 최상위 리스트</li>
     *   <li>각 부서는 children 리스트로 자식 부서 포함</li>
     *   <li>orgPath 순서로 정렬됨</li>
     * </ul>
     *
     * @param tenantId 멀티테넌트 ID
     * @return 조직도 트리 DTO 리스트 (루트부터)
     */
    public List<DepartmentDto.Response> getDepartmentTree(String tenantId) {
        // 모든 부서 조회
        List<Department> allDepts = departmentRepository.findAllByTenantId(tenantId);
        // 트리 구조로 변환
        return buildTree(allDepts);
    }

    /* ============================================================
     * Query: 조직도 트리 조회 (스코프 기반, Level 2 RBAC)
     * ============================================================ */

    /**
     * 사용자의 <b>접근 가능한 범위 내에서만</b> 조직도 트리를 조회합니다.
     *
     * <p>
     * Level 2 RBAC(Data Scope) 기반으로, 특정 사용자가 접근할 수 있는 부서들만
     * 필터링하여 트리를 구성합니다.
     * </p>
     *
     * <h3>동작:</h3>
     * <ol>
     *   <li>OrgScopeService를 통해 사용자의 접근 가능 부서 ID 집합 조회</li>
     *   <li>해당 부서들만 필터링</li>
     *   <li>필터링된 부서들로 트리 구성</li>
     * </ol>
     *
     * <p>
     * <b>주의:</b> 스코프 필터링으로 인해 부모 부서가 범위 밖에 있으면,
     * 자식 부서들이 루트로 표시될 수 있습니다.
     * </p>
     *
     * @param tenantId    멀티테넌트 ID
     * @param actorUserId 조회하는 사용자 ID
     * @return 사용자 권한 범위 내 조직도 트리
     */
    public List<DepartmentDto.Response> getDepartmentTreeWithinScope(
            String tenantId,
            UUID actorUserId) {

        // 1) 사용자가 접근 가능한 부서 ID 집합 조회 (Level 2 RBAC)
        Set<Long> scopeDeptIds = orgScopeService.getAccessibleDepartmentIds(
                tenantId,
                actorUserId
        );

        if (scopeDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) 모든 부서 중 접근 가능한 부서만 필터링
        List<Department> allDepts = departmentRepository.findAllByTenantId(tenantId);
        List<Department> scopedDepts = allDepts.stream()
                .filter(d -> scopeDeptIds.contains(d.getDeptId()))
                .collect(Collectors.toList());

        // 3) 필터링된 부서들로 트리 구성
        return buildTree(scopedDepts);
    }

    /* ============================================================
     * Helper: 부서 리스트를 트리 구조 DTO로 변환
     * ============================================================ */

    /**
     * Department 리스트를 계층 구조(트리) DTO로 변환합니다.
     *
     * <h3>알고리즘:</h3>
     * <ol>
     *   <li>모든 Department를 DTO로 변환하고 ID-DTO 맵 구성</li>
     *   <li>각 DTO의 parentId를 기준으로 부모-자식 관계 연결</li>
     *   <li>부모가 없는(또는 부모 정보가 누락된) DTO를 루트로 추가</li>
     *   <li>orgPath 순서로 정렬</li>
     * </ol>
     *
     * <p>
     * <b>데이터 불일치 처리:</b> 부모 정보가 누락된 경우(예: 스코프 필터링으로 인해
     * 부모 부서가 범위 밖), 해당 부서를 루트로 취급합니다.
     * </p>
     *
     * @param depts 변환할 부서 리스트
     * @return 루트부터 시작하는 트리 구조 DTO 리스트
     */
    private List<DepartmentDto.Response> buildTree(List<Department> depts) {
        // 1) ID -> DTO 맵 구성 (빠른 조회용)
        Map<Long, DepartmentDto.Response> dtoMap = depts.stream()
                .map(DepartmentDto.Response::from)
                .collect(Collectors.toMap(
                        DepartmentDto.Response::getDeptId,
                        dto -> dto
                ));

        List<DepartmentDto.Response> roots = new ArrayList<>();

        // 2) 부모-자식 관계 구성
        for (DepartmentDto.Response dto : dtoMap.values()) {
            Long parentId = dto.getParentId();

            if (parentId == null) {
                // 루트 노드 (부모가 없음)
                roots.add(dto);
            } else {
                // 부모 노드에 자식 추가
                DepartmentDto.Response parent = dtoMap.get(parentId);
                if (parent != null) {
                    parent.addChild(dto);
                } else {
                    // 부모 정보가 누락된 경우 루트로 처리 (데이터 불일치 대응)
                    roots.add(dto);
                }
            }
        }

        // 3) orgPath 순서로 정렬 (가독성 향상)
        roots.sort(Comparator.comparing(
                DepartmentDto.Response::getOrgPath,
                Comparator.nullsFirst(String::compareTo)
        ));

        return roots;
    }
}

