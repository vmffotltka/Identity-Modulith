package com.identitymodulith.organization.application.port;

import com.identitymodulith.organization.presentation.dto.request.CreateDepartmentRequest;
import com.identitymodulith.organization.presentation.dto.request.MoveDepartmentRequest;
import com.identitymodulith.organization.presentation.dto.request.UpdateDepartmentRequest;
import com.identitymodulith.organization.presentation.dto.response.DepartmentMembersResponse;
import com.identitymodulith.organization.presentation.dto.response.DepartmentResponse;
import com.identitymodulith.organization.presentation.dto.response.DepartmentStatisticsResponse;
import java.util.List;
import java.util.UUID;

/**
 * User 모듈 ↔ Organization 모듈 사이의 경계 (포트-어댑터 패턴)
 *
 * <h2>목적:</h2>
 * Organization 모듈이 User 모듈의 구현에 직접 의존하지 않도록
 * 추상화 계층을 제공합니다.
 *
 * <h2>구현체:</h2>
 * - AgentOrgUserAdapter (infrastructure.adapter 패키지)
 *   User 모듈의 Agent 정보를 Organization 모듈이 필요로 하는 형태로 변환
 *
 * <h2>데이터 타입 표준:</h2>
 * <ul>
 *   <li>deptId: UUID 문자열 (String, VARCHAR(36))</li>
 *   <li>tenantId: 문자열 (String)</li>
 *   <li>userId: UUID (java.util.UUID)</li>
 * </ul>
 *
 * @author Identity System Team
 * @version 1.0
 */
public interface OrgUserPort {

    /**
     * 특정 테넌트 + 부서에 "활성 유저"가 1명이라도 있는지 여부
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID (UUID 문자열)
     * @return true: 활성 유저 존재, false: 없음
     *
     * 사용처:
     * - 부서 삭제 시 "소속 직원 존재 검증"
     */
    boolean existsActiveUserInDepartment(String tenantId, String deptId);

    /**
     * 특정 유저의 조직/역할 정보 조회
     *
     * <p><b>반환 타입 변경:</b> null 대신 {@link java.util.Optional}을 반환하여 null 안전성을 확보합니다.
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID (UUID)
     * @return 사용자의 조직 정보 및 권한 레벨 (Optional)
     *         <ul>
     *           <li>사용자가 존재하면: {@link java.util.Optional#of(Object)}</li>
     *           <li>사용자가 없으면: {@link java.util.Optional#empty()}</li>
     *         </ul>
     *
     * 사용처:
     * - Level 1 RBAC 스코프 계산
     * - DepartmentServiceImpl.getAccessibleDepartmentIds()
     *
     * 사용 예시:
     * <pre>
     * OrgUserView user = orgUserPort.findOrgInfoByUserId(tenantId, userId)
     *         .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
     * </pre>
     */
    java.util.Optional<OrgUserView> findOrgInfoByUserId(String tenantId, UUID userId);

    /**
     * (옵션) 여러 부서에 속한 활성 유저들 목록 조회
     *
     * @param tenantId 테넌트 ID
     * @param deptIds 부서 ID 리스트 (UUID 문자열들)
     * @return 해당 부서들에 속한 활성 유저 목록
     *
     * 사용처:
     * - 아직 안 쓰더라도 시그니처만 잡아두면 나중에 유저 리스트 API에 쓰기 좋음
     */
    List<OrgUserView> findActiveUsersByDeptIds(String tenantId, List<String> deptIds);

    /**
     * 특정 부서의 전체 직원 수 조회 (활성 + 비활성)
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID (UUID 문자열)
     * @return 전체 직원 수
     *
     * 사용처:
     * - 부서 통계 API
     */
    long countEmployeesByDepartment(String tenantId, String deptId);

    /**
     * 특정 부서의 활성 직원 수 조회 (ACTIVE 상태만)
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID (UUID 문자열)
     * @return 활성 직원 수
     *
     * 사용처:
     * - 부서 통계 API
     */
    long countActiveEmployeesByDepartment(String tenantId, String deptId);

    /**
     * 특정 부서에 속한 사용자 목록 조회
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID (UUID 문자열)
     * @return 부서 소속 사용자 정보 목록
     *
     * 사용처:
     * - 부서별 사용자 목록 조회 API
     */
    List<DepartmentMembersResponse.MemberInfo> getUsersByDepartment(String tenantId, String deptId);
}
