package com.identitymodulith.common.security.principal;

/** 인증 컨텍스트에 담기는 공통 principal 계약. */
public interface AuthPrincipal {

    String getTenantId();

    String getUserId();

    default String getUsername() {
        return getUserId();
    }
}

