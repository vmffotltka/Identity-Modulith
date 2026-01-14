// organization.domain.model.DataScopeLevel.java
package com.nexfron.identitymodulith.organization.domain.model;

/**
 * DataScopeLevel (데이터 범위 레벨) Enum
 *
 * Level 2 RBAC (Role-Based Access Control)의 데이터 범위 관점에서
 * 사용자가 조직 데이터에 접근할 수 있는 범위를 정의합니다.
 *
 * 개념:
 * - Level 1 RBAC: 기능 기반 접근 제어 (Permission → Role → Agent)
 * - Level 2 RBAC: 조직 범위 기반 데이터 접근 제어 (DataScopeLevel로 관리)
 *
 * 계층 구조:
 * 조직 트리에서 사용자의 위치에 따라 보이는 범위가 달라집니다:
 *
 * 전체 조직:
 * ├─ 본부A
 * │  ├─ 부서A1
 * │  │  ├─ 팀A1a ← MEMBER (자신의 팀만 조회 가능)
 * │  │  └─ 팀A1b ← TEAM_LEAD (팀A1b + 하위 팀 모두 조회 가능)
 * │  └─ 부서A2
 * ├─ 본부B ← ADMIN (전체 조직 조회 가능)
 * └─ 본부C
 *
 * 사용 예시:
 *
 * 1. MEMBER (일반 사원)
 *    - 자신이 속한 부서의 데이터만 접근 가능
 *    - 예: 팀A의 사원은 팀A의 사원 목록만 조회 가능
 *    - 상사나 다른 팀의 사원 정보 조회 불가
 *
 * 2. TEAM_LEAD (팀장)
 *    - 자신이 속한 부서 + 직속 하위 부서의 데이터 접근 가능
 *    - 예: 부서A의 팀장은 부서A의 모든 팀과 그 산하 팀의 데이터 조회 가능
 *    - 형제 부서나 상위 부서는 조회 불가
 *
 * 3. ADMIN (관리자)
 *    - 전체 조직의 모든 데이터에 접근 가능
 *    - 예: 시스템 관리자는 모든 부서의 모든 직원 데이터 조회 가능
 *    - 권한 제한 없음
 *
 * DB 쿼리 최적화:
 * - MEMBER: WHERE org_path = 'user의 org_path'
 * - TEAM_LEAD: WHERE org_path LIKE 'user의 org_path/%' OR org_path = 'user의 org_path'
 * - ADMIN: WHERE 1=1 (조건 없음)
 *
 * 통합 고려사항:
 * - RBAC 모듈의 roles/permissions와는 독립적으로 동작
 * - 사용자의 조직 위치와 이 enum을 함께 사용하여 데이터 접근 범위 결정
 * - 부서 이동 시 DataScopeLevel 변경 여부 확인 필요
 */
public enum DataScopeLevel {

    /**
     * MEMBER (일반 사원)
     * - 조회 범위: 자신의 부서만
     * - 접근 가능: 자신이 속한 조직 단위의 데이터만 조회/관리
     * - 예시: 팀원, 일반 직원
     */
    MEMBER,

    /**
     * TEAM_LEAD (팀장/부서장)
     * - 조회 범위: 자신 + 직속 하위 부서
     * - 접근 가능: 자신의 팀/부서 + 산하의 모든 부서와 직원 데이터
     * - 예시: 팀장, 부서장, 센터장
     * - 권한: 부하 직원 관리, 하위 부서 현황 파악 가능
     */
    TEAM_LEAD,

    /**
     * ADMIN (조직 관리자)
     * - 조회 범위: 전체 조직
     * - 접근 가능: 모든 부서, 모든 직원의 모든 데이터
     * - 예시: 임원, HR 담당자, 시스템 관리자
     * - 권한: 전체 조직 관리, 정책 결정, 보고서 작성 등
     */
    ADMIN;

    /**
     * 전체 조직 조회 가능 여부
     *
     * @return ADMIN인 경우 true, 그 외 false
     *
     * 활용:
     * - 보고서나 대시보드에서 전사 통계 표시 여부 결정
     * - 조직 전체 조회 권한 확인
     */
    public boolean canSeeWholeTenant() {
        return this == ADMIN;
    }

    /**
     * 자신의 부서 + 하위 부서 조회 가능 여부
     *
     * @return TEAM_LEAD 또는 ADMIN인 경우 true, 그 외 false
     *
     * 활용:
     * - 부하 직원 관리 UI 표시 여부
     * - 자신의 조직 단위 관리 기능 활성화 여부
     * - 부하의 부서원 정보 조회 권한
     */
    public boolean canSeeSubTree() {
        return this == TEAM_LEAD || this == ADMIN;
    }
}
