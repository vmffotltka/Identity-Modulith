package com.identitymodulith.organization.domain;

import com.identitymodulith.organization.infrastructure.persistence.entity.DepartmentEntity;

/** 조직 도메인 검증에 사용하는 상수 모음. */
public final class OrganizationConstants {

    public static final int DEPARTMENT_NAME_MIN_LENGTH = 2;

    public static final int DEPARTMENT_NAME_MAX_LENGTH = 100;

    public static final int DEPARTMENT_TYPE_MAX_LENGTH = 50;

    /** 순환/과도한 계층을 막기 위한 최대 깊이. */
    public static final int MAX_ORGANIZATION_DEPTH = 5;

    /** materialized path 저장 길이 제한. */
    public static final int MAX_ORG_PATH_LENGTH = 500;

    private OrganizationConstants() {
        throw new AssertionError("상수 클래스는 인스턴스화할 수 없습니다");
    }
}

