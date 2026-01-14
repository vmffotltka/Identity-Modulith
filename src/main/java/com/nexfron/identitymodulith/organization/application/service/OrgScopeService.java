// organization.application.service.OrgScopeService.java
package com.nexfron.identitymodulith.organization.application.service;

import com.nexfron.identitymodulith.organization.common.exception.EntityNotFoundException;
import com.nexfron.identitymodulith.organization.application.port.OrgUserPort;
import com.nexfron.identitymodulith.organization.application.port.OrgUserView;
import com.nexfron.identitymodulith.organization.domain.model.Department;
import com.nexfron.identitymodulith.organization.domain.model.DataScopeLevel;
import com.nexfron.identitymodulith.organization.domain.repository.JpaDepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OrgScopeService - Level 2 RBAC (데이터 범위 기반 접근 제어) 핵심 로직
 *
 * <h2>목표:</h2>
 * RBAC의 2단계 접근 제어를 구현합니다:
 * - Level 1: 기능 기반 (RBAC Module)
 *   "이 사용자가 이 기능(권한)을 사용할 수 있는가?" → Permission으로 제어
 * - Level 2: 데이터 범위 기반 (이 클래스)
 *   "이 사용자가 이 조직 데이터를 볼 수 있는가?" → DataScopeLevel로 제어
 *
 * <h2>핵심 질문:</h2>
 * "이 유저가 어느 부서까지 조회/수정할 수 있나?"
 *
 * <h2>접근 범위 규칙:</h2>
 *
 * 1. MEMBER (일반 사원)
 *    - 조회 가능: 자신의 부서만
 *    - 쿼리: WHERE deptId = 자신의부서ID
 *    - 예: 마케팅부 팀원은 마케팅부 데이터만 조회 가능
 *
 * 2. TEAM_LEAD (팀장/부서장)
 *    - 조회 가능: 자신의 부서 + 모든 하위 부서
 *    - 쿼리: WHERE orgPath LIKE 자신의orgPath || '%'
 *    - 예: 마케팅부 부서장은 마케팅부 + 마케팅부의 모든 하위팀 조회 가능
 *
 * 3. ADMIN (조직 관리자)
 *    - 조회 가능: 전체 조직의 모든 부서
 *    - 쿼리: WHERE 1=1 (조건 없음)
 *    - 예: HR 담당자는 회사의 모든 부서 조회 가능
 *
 * <h2>조직 구조와 접근 범위 예시:</h2>
 * <pre>
 * 전체 조직:
 * ├─ 마케팅부 (deptId: "A", orgPath: "/A")
 * │  ├─ 디지털팀 (deptId: "B", orgPath: "/A/B") ← 팀장: 사용자1
 * │  │  ├─ SNS팀 (deptId: "D", orgPath: "/A/B/D")
 * │  │  └─ 검색팀 (deptId: "E", orgPath: "/A/B/E")
 * │  └─ 전략팀 (deptId: "C", orgPath: "/A/C") ← 팀원: 사용자2
 * └─ 개발부 (deptId: "F", orgPath: "/F")
 *
 * 사용자1 (TEAM_LEAD, 부서: 디지털팀B)의 접근 범위:
 * ✓ /A/B (자신의 부서)
 * ✓ /A/B/D (하위 부서)
 * ✓ /A/B/E (하위 부서)
 * ✗ /A (상위 부서 - 조회 불가)
 * ✗ /A/C (형제 부서 - 조회 불가)
 * ✗ /F (다른 부 - 조회 불가)
 *
 * 사용자2 (MEMBER, 부서: 전략팀C)의 접근 범위:
 * ✓ /A/C (자신의 부서만)
 * ✗ /A (상위 부서)
 * ✗ /A/B (형제 팀)
 * ✗ /F (다른 부)
 * </pre>
 *
 * <h2>주요 메서드:</h2>
 * <ul>
 *   <li>getAccessibleDepartmentIds(): 사용자가 접근 가능한 부서 ID 집합 계산</li>
 *   <li>canAccessDepartment(): 특정 부서 1개 접근 가능 여부</li>
 *   <li>canAccessAllDepartments(): 특정 부서들 모두 접근 가능 여부 (AND 조건)</li>
 * </ul>
 *
 * <h2>데이터 페칭 전략:</h2>
 * <ol>
 *   <li>OrgUserPort로 사용자의 부서(deptId)와 권한 레벨(DataScopeLevel) 조회</li>
 *   <li>사용자의 부서 정보로부터 orgPath 조회</li>
 *   <li>orgPath 기반 범위 검색으로 접근 가능한 부서들 조회</li>
 * </ol>
 *
 * <h2>성능 최적화:</h2>
 * <ul>
 *   <li>orgPath LIKE 쿼리: 인덱스 활용으로 빠른 범위 검색</li>
 *   <li>캐싱 가능: 사용자의 권한 범위는 자주 변경되지 않음</li>
 *   <li>배치 조회: canAccessAllDepartments로 여러 부서 한 번에 검증</li>
 * </ul>
 *
 * <h2>연동:</h2>
 * <ul>
 *   <li>User Module (OrgUserPort): 사용자 정보 조회</li>
 *   <li>RBAC Module: 사용자의 역할/권한은 Level 1 RBAC으로 관리</li>
 *   <li>DepartmentService: 부서 조회 시 권한 검증</li>
 * </ul>
 *
 * @author Identity System Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgScopeService {

    private final JpaDepartmentRepository jpaDepartmentRepository;
    private final OrgUserPort orgUserPort;

    /**
     * [핵심 메서드] 특정 사용자가 접근 가능한 부서 ID 집합 계산
     *
     * <h3>동작 흐름:</h3>
     * <ol>
     *   <li>OrgUserPort로 사용자의 조직 정보 조회 (부서ID, 권한 레벨)</li>
     *   <li>사용자의 권한 레벨(DataScopeLevel)에 따라 처리
     *       <br/>- ADMIN: 전체 부서 조회
     *       <br/>- TEAM_LEAD: 자신의 부서 + 하위 부서 조회
     *       <br/>- MEMBER: 자신의 부서만 반환
     *   </li>
     *   <li>접근 가능한 부서 ID 집합 반환</li>
     * </ol>
     *
     * <h3>사용 예시:</h3>
     * <pre>
     * // 디지털팀(부서B) 팀장의 경우
     * Set<String> scope = getAccessibleDepartmentIds("tenant-001", userId);
     * // 반환: {"B", "D", "E"} (디지털팀 + SNS팀 + 검색팀)
     * </pre>
     *
     * <h3>예외 처리:</h3>
     * <ul>
     *   <li>사용자 정보 없음: EntityNotFoundException 발생</li>
     *   <li>사용자 비활성화: EntityNotFoundException 발생</li>
     *   <li>부서 정보 없음: EntityNotFoundException 발생</li>
     * </ul>
     *
     * @param tenantId 테넌트 ID (멀티테넌시 격리)
     * @param userId   사용자 ID (UUID)
     *
     * @return 접근 가능한 부서 ID 집합 (UUID 문자열)
     *         - ADMIN: 모든 부서 ID
     *         - TEAM_LEAD: 자신 + 하위 부서 ID
     *         - MEMBER: {자신의 부서 ID}
     *         - 집합이 비어있을 수 없음 (최소한 자신의 부서는 포함)
     *
     * @throws EntityNotFoundException
     *         - 사용자의 조직 정보(부서)가 없는 경우
     *         - 사용자가 비활성화 상태인 경우
     *
     * @see DataScopeLevel
     * @see OrgUserPort#findOrgInfoByUserId(String, UUID)
     */
    public Set<String> getAccessibleDepartmentIds(String tenantId, UUID userId) {
        OrgUserView userView = orgUserPort.findOrgInfoByUserId(tenantId, userId);
        if (userView == null || !userView.isActive()) {
            throw new EntityNotFoundException("사용자의 조직 정보를 찾을 수 없습니다.");
        }

        DataScopeLevel level = userView.getRoleLevel();
        String myDeptId = userView.getDeptId();

        if (myDeptId == null) {
            throw new EntityNotFoundException("사용자의 소속 부서를 찾을 수 없습니다.");
        }

        // ADMIN: 전체 조직 조회
        if (level.canSeeWholeTenant()) {
            return jpaDepartmentRepository.findAllByTenantId(tenantId).stream()
                    .map(Department::getDeptId)
                    .collect(Collectors.toSet());
        }

        // tenant-aware 단건 조회로 통일 (조회 후 filter 제거)
        Department myDept = jpaDepartmentRepository.findByDeptIdAndTenantId(myDeptId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("사용자의 소속 부서를 찾을 수 없습니다."));

        // TEAM_LEAD: 내 부서 + 하위 부서
        if (level.canSeeSubTree()) {
            String pathPrefix = myDept.getOrgPath();
            return jpaDepartmentRepository
                    .findByTenantIdAndOrgPathStartsWith(tenantId, pathPrefix)
                    .stream()
                    .map(Department::getDeptId)
                    .collect(Collectors.toSet());
        }

        // MEMBER: 내 부서만
        return Set.of(myDept.getDeptId());
    }

    /**
     * 특정 부서에 대한 접근 권한 여부 확인
     *
     * <h3>동작:</h3>
     * 1. 사용자의 접근 가능 부서 집합 계산
     * 2. 대상 부서가 그 집합에 포함되는지 확인
     * 3. boolean 반환
     *
     * <h3>사용 예시:</h3>
     * <pre>
     * // 부서A의 정보를 조회하려는 사용자가 접근 가능한지 확인
     * if (orgScopeService.canAccessDepartment("tenant-001", userId, deptAId)) {
     *     // 부서A 정보 반환
     * } else {
     *     // 접근 불가 - 403 Forbidden
     * }
     * </pre>
     *
     * @param tenantId      테넌트 ID
     * @param userId        사용자 ID
     * @param targetDeptId  확인할 대상 부서 ID (UUID 문자열)
     *
     * @return true: 접근 가능, false: 접근 불가
     *
     * @throws EntityNotFoundException 사용자 정보 없음
     *
     * @see #getAccessibleDepartmentIds(String, UUID)
     */
    public boolean canAccessDepartment(String tenantId, UUID userId, String targetDeptId) {
        Set<String> scope = getAccessibleDepartmentIds(tenantId, userId);
        return scope.contains(targetDeptId);
    }

    /**
     * 특정 부서들 모두에 대한 접근 권한 여부 확인 (AND 조건)
     *
     * <h3>동작:</h3>
     * 1. 사용자의 접근 가능 부서 집합 계산
     * 2. 전달된 부서들이 모두 그 집합에 포함되는지 확인
     * 3. 하나라도 포함되지 않으면 false 반환
     *
     * <h3>사용 예시:</h3>
     * <pre>
     * // 여러 부서의 데이터를 한 번에 조회하려는 경우
     * List<String> deptIds = Arrays.asList(deptAId, deptBId, deptCId);
     * if (orgScopeService.canAccessAllDepartments("tenant-001", userId, deptIds)) {
     *     // 모든 부서에 접근 가능 - 데이터 반환
     * } else {
     *     // 일부 부서에 접근 불가 - 403 Forbidden
     * }
     * </pre>
     *
     * <h3>성능 최적화:</h3>
     * - 여러 부서 검증을 한 번의 getAccessibleDepartmentIds() 호출로 처리
     * - 각 부서별로 별도 쿼리하지 않음
     *
     * @param tenantId  테넌트 ID
     * @param userId    사용자 ID
     * @param deptIds   확인할 부서 ID 컬렉션 (UUID 문자열)
     *
     * @return true: 모든 부서에 접근 가능
     *         false: 하나 이상의 부서에 접근 불가
     *
     * @throws EntityNotFoundException 사용자 정보 없음
     *
     * @see #getAccessibleDepartmentIds(String, UUID)
     * @see #canAccessDepartment(String, UUID, String)
     */
    public boolean canAccessAllDepartments(String tenantId, UUID userId, Collection<String> deptIds) {
        Set<String> scope = getAccessibleDepartmentIds(tenantId, userId);
        return deptIds.stream().allMatch(scope::contains);
    }
}
