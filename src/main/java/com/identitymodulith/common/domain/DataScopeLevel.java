package com.identitymodulith.common.domain;

import java.util.Map;

/**
 * 데이터 접근 범위 레벨 (공통 모듈)
 *
 * <p>RBAC 및 Organization 모듈에서 공통으로 사용하는 데이터 범위 정의입니다.
 * 사용자가 조직 데이터에 접근할 수 있는 범위를 결정합니다.</p>
 *
 * <h2>계층 구조:</h2>
 * ADMIN > TEAM_LEAD > MEMBER
 *
 * <h2>적용 예시:</h2>
 * <ul>
 *   <li>ADMIN: 전체 조직의 모든 데이터 접근 가능</li>
 *   <li>TEAM_LEAD: 본인 부서 + 하위 부서 데이터 접근 가능</li>
 *   <li>MEMBER: 본인 부서 데이터만 접근 가능</li>
 * </ul>
 *
 * <h2>DB 쿼리 최적화:</h2>
 * <ul>
 *   <li>MEMBER: WHERE org_path = 'user의 org_path'</li>
 *   <li>TEAM_LEAD: WHERE org_path LIKE 'user의 org_path/%' OR org_path = 'user의 org_path'</li>
 *   <li>ADMIN: WHERE 1=1 (조건 없음)</li>
 * </ul>
 */
public enum DataScopeLevel {

    /**
     * MEMBER (일반 사원)
     * - 조회 범위: 자신의 부서만
     * - 예시: 팀원, 일반 직원
     */
    MEMBER,

    /**
     * TEAM_LEAD (팀장/부서장)
     * - 조회 범위: 자신 + 직속 하위 부서
     * - 예시: 팀장, 부서장, 센터장
     */
    TEAM_LEAD,

    /**
     * ADMIN (조직 관리자)
     * - 조회 범위: 전체 조직
     * - 예시: 임원, HR 담당자, 시스템 관리자
     */
    ADMIN;

    // =========================================================
    // 편의 메서드
    // =========================================================

    /**
     * 전체 조직 조회 가능 여부
     */
    public boolean canSeeWholeTenant() {
        return this == ADMIN;
    }

    /**
     * 본인 부서 + 하위 부서 조회 가능 여부
     */
    public boolean canSeeSubTree() {
        return this == TEAM_LEAD || this == ADMIN;
    }

    // =========================================================
    // 역할명 → DataScopeLevel 매핑
    // =========================================================

    private static final Map<String, DataScopeLevel> ROLE_SCOPE_MAP = Map.ofEntries(
            // POSITION 타입 역할
            Map.entry("ADMIN", ADMIN),
            Map.entry("MANAGER", ADMIN),
            Map.entry("TEAM_LEAD", TEAM_LEAD),
            Map.entry("MEMBER", MEMBER),

            // CHANNEL 타입 역할
            Map.entry("SUPERVISOR", TEAM_LEAD),
            Map.entry("PHONE_AGENT", MEMBER),
            Map.entry("CHAT_AGENT", MEMBER),
            Map.entry("EMAIL_AGENT", MEMBER)
    );

    /**
     * 역할명으로 데이터 스코프 레벨 조회.
     * 매핑 테이블에 없는 역할은 MEMBER(최소 권한)로 간주합니다.
     *
     * @param roleName 역할명 (예: "ADMIN", "PHONE_AGENT")
     * @return 데이터 스코프 레벨
     */
    public static DataScopeLevel fromRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return MEMBER;
        }
        return ROLE_SCOPE_MAP.getOrDefault(roleName.trim().toUpperCase(), MEMBER);
    }
}

