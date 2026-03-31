package com.identitymodulith.common.security.context;

import com.identitymodulith.common.security.principal.AuthPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/** 인증 컨텍스트에서 tenant/user 식별자를 추출하는 유틸리티. */
@Component
@Slf4j
public class TenantContextHolder {

    /** 인증 principal에서 tenantId를 추출한다. */
    public static String getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            log.warn("[TenantContext] 인증 정보가 없습니다.");
            throw new UnauthorizedException("인증 정보가 없습니다.");
        }

        if ("anonymousUser".equals(auth.getPrincipal())) {
            log.warn("[TenantContext] 익명 사용자는 테넌트 ID를 가질 수 없습니다.");
            throw new UnauthorizedException("인증되지 않은 사용자입니다.");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof AuthPrincipal) {
            String tenantId = ((AuthPrincipal) principal).getTenantId();
            log.debug("[TenantContext] AuthPrincipal에서 테넌트 ID 추출: {}", tenantId);
            return tenantId;
        }

        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            if (username != null && username.contains(":")) {
                String tenantId = username.split(":")[0];
                log.debug("[TenantContext] UserDetails username에서 테넌트 ID 추출: {}", tenantId);
                return tenantId;
            }
            log.warn("[TenantContext] UserDetails username이 'tenantId:userId' 형식이 아닙니다: {}", username);
        }

        if (principal instanceof String principalStr) {
            if (principalStr.contains(":")) {
                String tenantId = principalStr.split(":")[0];
                log.debug("[TenantContext] Principal 문자열에서 테넌트 ID 추출: {}", tenantId);
                return tenantId;
            }
            log.debug("[TenantContext] Principal 문자열을 테넌트 ID로 사용: {}", principalStr);
            return principalStr;
        }

        log.error("[TenantContext] 테넌트 ID 추출 실패. Principal 타입: {}", principal.getClass().getName());
        throw new UnauthorizedException("테넌트 정보를 찾을 수 없습니다.");
    }

    /** 인증 principal에서 userId를 추출한다. */
    public static String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("인증 정보가 없습니다.");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof AuthPrincipal) {
            return ((AuthPrincipal) principal).getUserId();
        }

        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            if (username != null && username.contains(":")) {
                return username.split(":")[1];
            }
            return username;
        }

        if (principal instanceof String principalStr) {
            if (principalStr.contains(":")) {
                return principalStr.split(":")[1];
            }
            return principalStr;
        }

        throw new UnauthorizedException("사용자 정보를 찾을 수 없습니다.");
    }

    public static boolean isValidTenantId(String tenantId) {
        return tenantId != null && !tenantId.isBlank() && !tenantId.equals("anonymousUser");
    }
}

