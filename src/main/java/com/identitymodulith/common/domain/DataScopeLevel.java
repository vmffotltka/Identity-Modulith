package com.identitymodulith.common.domain;

import java.util.Map;

/** 조직 데이터 접근 범위 레벨. */
public enum DataScopeLevel {

    MEMBER,

    TEAM_LEAD,

    ADMIN;

    public boolean canSeeWholeTenant() {
        return this == ADMIN;
    }

    public boolean canSeeSubTree() {
        return this == TEAM_LEAD || this == ADMIN;
    }

    private static final Map<String, DataScopeLevel> ROLE_SCOPE_MAP = Map.ofEntries(
            Map.entry("ADMIN", ADMIN),
            Map.entry("MANAGER", ADMIN),
            Map.entry("TEAM_LEAD", TEAM_LEAD),
            Map.entry("MEMBER", MEMBER),

            Map.entry("SUPERVISOR", TEAM_LEAD),
            Map.entry("PHONE_AGENT", MEMBER),
            Map.entry("CHAT_AGENT", MEMBER),
            Map.entry("EMAIL_AGENT", MEMBER)
    );

    /** 역할명을 DataScopeLevel로 매핑하며 미정의 역할은 MEMBER로 처리한다. */
    public static DataScopeLevel fromRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return MEMBER;
        }
        return ROLE_SCOPE_MAP.getOrDefault(roleName.trim().toUpperCase(), MEMBER);
    }
}

