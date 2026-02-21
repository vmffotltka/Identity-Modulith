package com.nexfron.identitymodulith.common.security;

import lombok.extern.slf4j.Slf4j;

/**
 * JWT 기반 Tenant Context 관리
 * ThreadLocal을 사용하여 요청 스레드별로 tenant_id를 관리합니다.
 */
@Slf4j
public class JwtTenantContext {

    private static final ThreadLocal<String> tenantIdHolder = new ThreadLocal<>();

    /**
     * 현재 스레드의 Tenant ID 설정
     */
    public static void setCurrentTenantId(String tenantId) {
        log.debug("[JwtTenantContext] Tenant ID 설정: {}", tenantId);
        tenantIdHolder.set(tenantId);
    }

    /**
     * 현재 스레드의 Tenant ID 조회
     */
    public static String getCurrentTenantId() {
        String tenantId = tenantIdHolder.get();
        if (tenantId == null) {
            log.warn("[JwtTenantContext] Tenant ID가 설정되지 않음, 기본값 사용");
            return "default-tenant";
        }
        return tenantId;
    }

    /**
     * 현재 스레드의 Tenant ID 제거
     * (메모리 누수 방지를 위해 요청 종료 시 반드시 호출)
     */
    public static void clear() {
        log.trace("[JwtTenantContext] Tenant ID 제거");
        tenantIdHolder.remove();
    }
}

