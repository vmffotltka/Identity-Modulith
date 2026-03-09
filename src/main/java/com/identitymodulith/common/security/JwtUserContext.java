package com.identitymodulith.common.security;

import lombok.extern.slf4j.Slf4j;

/**
 * JWT 기반 User Context 관리
 * ThreadLocal을 사용하여 요청 스레드별로 사용자 정보를 관리합니다.
 */
@Slf4j
public class JwtUserContext {

    private static final ThreadLocal<String> tenantIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();

    /**
     * 현재 스레드의 Tenant ID 설정
     */
    public static void setCurrentTenantId(String tenantId) {
        log.debug("[JwtUserContext] Tenant ID 설정: {}", tenantId);
        tenantIdHolder.set(tenantId);
    }

    /**
     * 현재 스레드의 User ID 설정
     */
    public static void setCurrentUserId(String userId) {
        log.debug("[JwtUserContext] User ID 설정: {}", userId);
        userIdHolder.set(userId);
    }

    /**
     * 현재 스레드의 Username 설정
     */
    public static void setCurrentUsername(String username) {
        log.debug("[JwtUserContext] Username 설정: {}", username);
        usernameHolder.set(username);
    }

    /**
     * 현재 스레드의 Tenant ID 조회
     */
    public static String getCurrentTenantId() {
        String tenantId = tenantIdHolder.get();
        if (tenantId == null) {
            log.warn("[JwtUserContext] Tenant ID가 설정되지 않음, 기본값 사용");
            return "default-tenant";
        }
        return tenantId;
    }

    /**
     * 현재 스레드의 User ID 조회
     */
    public static String getCurrentUserId() {
        String userId = userIdHolder.get();
        if (userId == null) {
            log.warn("[JwtUserContext] User ID가 설정되지 않음");
        }
        return userId;
    }

    /**
     * 현재 스레드의 Username 조회
     */
    public static String getCurrentUsername() {
        String username = usernameHolder.get();
        if (username == null) {
            log.warn("[JwtUserContext] Username이 설정되지 않음");
        }
        return username;
    }

    /**
     * 현재 스레드의 모든 정보 제거
     * (메모리 누수 방지를 위해 요청 종료 시 반드시 호출)
     */
    public static void clear() {
        log.trace("[JwtUserContext] Context 클리어");
        tenantIdHolder.remove();
        userIdHolder.remove();
        usernameHolder.remove();
    }
}

