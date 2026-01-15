package com.nexfron.identitymodulith.organization.application.port;

import java.util.List;
import java.util.UUID;

/**
 * User 모듈 ↔ Organization 모듈 사이의 경계
 *
 * - 구현체는 나중에 User 모듈로 옮길 예정.
 * - 지금은 Dummy 구현체 하나만 만들어서 테스트한다.
 *
 * 데이터 타입 표준:
 * - deptId: UUID 문자열 (String, VARCHAR(36))
 * - tenantId: 문자열 (String)
 * - userId: UUID (java.util.UUID)
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
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID (UUID)
     * @return 사용자의 조직 정보 및 권한 레벨
     *
     * 사용처:
     * - Level 2 RBAC 스코프 계산
     * - OrgScopeService에서 사용
     */
    OrgUserView findOrgInfoByUserId(String tenantId, UUID userId);

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
}
