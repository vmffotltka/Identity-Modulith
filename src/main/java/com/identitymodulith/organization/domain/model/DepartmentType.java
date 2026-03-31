package com.identitymodulith.organization.domain.model;

/** 조직 단위 타입. */
public enum DepartmentType {

    COMPANY("회사"),

    DIVISION("본부"),

    TEAM("팀"),

    GROUP("그룹"),

    CUSTOM("커스텀");

    private final String displayName;

    DepartmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isCustomType() {
        return this == CUSTOM;
    }

    public boolean isRootType() {
        return this == COMPANY;
    }
}
