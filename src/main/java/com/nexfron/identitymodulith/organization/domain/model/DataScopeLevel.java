// organization.domain.model.DataScopeLevel.java
package com.nexfron.identitymodulith.organization.domain.model;

/**
 * DataScopeLevel (데이터 범위 레벨) Enum
 *
 * <h2>목적:</h2>
 * RBAC (Role-Based Access Control)에서 사용자가 조직 데이터에
 * 접근할 수 있는 범위를 정의합니다.
 *
 * <h2>RBAC 구조 (Flat RBAC):</h2>
 * <ul>
 *   <li><b>기능 기반 접근 제어</b>: Permission → Role → Agent</li>
 *   <li><b>데이터 범위 기반 접근 제어</b>: DataScopeLevel로 조직 범위 관리</li>
 * </ul>
 *
 * <h2>특징:</h2>
 * - 계층형 RBAC이 아닌 Flat RBAC 구조
 * - 각 역할은 독립적이며 상하 관계 없음
 * - 상담사는 채널별 권한(PHONE, CHAT, EMAIL) + 직책별 범위(MEMBER, TEAM_LEAD)를 조합
 *
 * <h2>계층 구조:</h2>
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
 * <h2>사용 예시:</h2>
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

    // ========================================================================
    // 역할명 → DataScopeLevel 매핑 (통합됨)
    // ========================================================================

    /**
     * 역할명 → 데이터 스코프 레벨 매핑 테이블
     *
     * <h3>POSITION 타입 역할:</h3>
     * <ul>
     *   <li>ADMIN: 시스템 관리자 → ADMIN</li>
     *   <li>MANAGER: 매니저 → ADMIN</li>
     *   <li>TEAM_LEAD: 팀장 → TEAM_LEAD</li>
     *   <li>MEMBER: 일반 직원 → MEMBER</li>
     * </ul>
     *
     * <h3>CHANNEL 타입 역할:</h3>
     * <ul>
     *   <li>SUPERVISOR: 수퍼바이저 → TEAM_LEAD</li>
     *   <li>PHONE_AGENT: 전화 상담사 → MEMBER</li>
     *   <li>CHAT_AGENT: 채팅 상담사 → MEMBER</li>
     *   <li>EMAIL_AGENT: 이메일 상담사 → MEMBER</li>
     * </ul>
     */
    private static final java.util.Map<String, DataScopeLevel> ROLE_SCOPE_MAP = java.util.Map.ofEntries(
            // POSITION 타입 역할
            java.util.Map.entry("ADMIN", ADMIN),
            java.util.Map.entry("MANAGER", ADMIN),
            java.util.Map.entry("TEAM_LEAD", TEAM_LEAD),
            java.util.Map.entry("MEMBER", MEMBER),

            // CHANNEL 타입 역할
            java.util.Map.entry("SUPERVISOR", TEAM_LEAD),
            java.util.Map.entry("PHONE_AGENT", MEMBER),
            java.util.Map.entry("CHAT_AGENT", MEMBER),
            java.util.Map.entry("EMAIL_AGENT", MEMBER)
    );

    /**
     * 역할명으로 데이터 스코프 레벨 조회
     *
     * <p>매핑 테이블에 없는 역할은 MEMBER(최소 권한)로 간주합니다.
     *
     * <h3>사용 예시:</h3>
     * <pre>
     * DataScopeLevel level = DataScopeLevel.fromRoleName("ADMIN");
     * // 결과: DataScopeLevel.ADMIN
     *
     * DataScopeLevel level = DataScopeLevel.fromRoleName("UNKNOWN_ROLE");
     * // 결과: DataScopeLevel.MEMBER (기본값)
     * </pre>
     *
     * @param roleName 역할명 (예: "ADMIN", "PHONE_AGENT")
     * @return 데이터 스코프 레벨
     */
    public static DataScopeLevel fromRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return MEMBER;
        }

        // 대소문자 무시하고 매핑
        String normalizedRoleName = roleName.trim().toUpperCase();
        return ROLE_SCOPE_MAP.getOrDefault(normalizedRoleName, MEMBER);
    }
}
