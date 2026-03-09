package com.identitymodulith.rbac.domain;

/**
 * 데이터 접근 범위 레벨
 *
 * POSITION 역할에만 적용되는 데이터 접근 범위를 정의합니다.
 * 상담사가 조회/수정할 수 있는 데이터의 범위를 결정합니다.
 *
 * 계층 구조:
 * ADMIN > TEAM_LEAD > MEMBER
 *
 * 적용 예시:
 * - ADMIN: 전체 조직의 모든 상담사 데이터 접근 가능
 * - TEAM_LEAD: 본인 부서 + 하위 부서의 상담사 데이터 접근 가능
 * - MEMBER: 본인 부서의 상담사 데이터만 접근 가능
 */
public enum DataScopeLevel {
    /**
     * 전체 조직 접근
     * - 테넌트의 모든 부서 접근 가능
     * - 시스템 관리자용
     */
    ADMIN,

    /**
     * 본인 부서 + 하위 부서 접근
     * - 팀장/부서장용
     * - 재귀적으로 하위 부서 포함
     */
    TEAM_LEAD,

    /**
     * 본인 부서만 접근
     * - 일반 상담사용
     * - 같은 부서 동료만 조회 가능
     */
    MEMBER
}
