package com.nexfron.identitymodulith.rbac.domain;

/**
 * RBAC 모듈 상수 정의
 *
 * 매직 넘버 제거 및 유효성 검증 기준 제공
 */
public final class RbacConstants {

    // 역할 (Role)
    public static final int ROLE_NAME_MIN_LENGTH = 2;
    public static final int ROLE_NAME_MAX_LENGTH = 64;
    public static final int ROLE_DESCRIPTION_MAX_LENGTH = 255;

    // 권한 (Permission)
    public static final int PERMISSION_CODE_MAX_LENGTH = 128;
    public static final int PERMISSION_DESCRIPTION_MAX_LENGTH = 500;

    private RbacConstants() {
        throw new AssertionError("상수 클래스는 인스턴스화할 수 없습니다");
    }
}

