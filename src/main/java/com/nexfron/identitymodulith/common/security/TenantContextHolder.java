package com.nexfron.identitymodulith.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * TenantContextHolder - 테넌트 컨텍스트 관리
 *
 * <h2>목적:</h2>
 * Spring Security의 인증 정보에서 테넌트 ID를 안전하게 추출합니다.
 *
 * <h2>보안 원칙:</h2>
 * <ul>
 *   <li>인증 실패 시 예외 발생 (기본값 반환 금지)</li>
 *   <li>멀티테넌시 격리 보장</li>
 *   <li>명확한 에러 메시지</li>
 * </ul>
 *
 * @author Identity System Team
 * @version 1.0
 */
@Component
@Slf4j
public class TenantContextHolder {

    /**
     * 현재 인증된 사용자의 테넌트 ID 반환
     *
     * <h3>추출 우선순위:</h3>
     * <ol>
     *   <li>AuthPrincipal 인터페이스 구현체</li>
     *   <li>UserDetails의 username 파싱 (tenantId:userId 형식)</li>
     *   <li>Authentication의 principal을 문자열로 직접 사용</li>
     * </ol>
     *
     * @return 테넌트 ID
     * @throws UnauthorizedException 인증 정보가 없거나 테넌트 ID를 추출할 수 없는 경우
     */
    public static String getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 1. 인증 정보 검증
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("[TenantContext] 인증 정보가 없습니다.");
            throw new UnauthorizedException("인증 정보가 없습니다.");
        }

        // 2. "anonymousUser" 체크 (Spring Security 기본 미인증 사용자)
        if ("anonymousUser".equals(auth.getPrincipal())) {
            log.warn("[TenantContext] 익명 사용자는 테넌트 ID를 가질 수 없습니다.");
            throw new UnauthorizedException("인증되지 않은 사용자입니다.");
        }

        Object principal = auth.getPrincipal();

        // 3. AuthPrincipal 인터페이스 구현체 (가장 명시적인 방법)
        if (principal instanceof AuthPrincipal) {
            String tenantId = ((AuthPrincipal) principal).getTenantId();
            log.debug("[TenantContext] AuthPrincipal에서 테넌트 ID 추출: {}", tenantId);
            return tenantId;
        }

        // 4. UserDetails에서 추출 (username이 "tenantId:userId" 형식)
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            if (username != null && username.contains(":")) {
                String tenantId = username.split(":")[0];
                log.debug("[TenantContext] UserDetails username에서 테넌트 ID 추출: {}", tenantId);
                return tenantId;
            }
            log.warn("[TenantContext] UserDetails username이 'tenantId:userId' 형식이 아닙니다: {}", username);
        }

        // 5. principal이 문자열인 경우 직접 사용
        if (principal instanceof String) {
            String principalStr = (String) principal;
            if (principalStr.contains(":")) {
                String tenantId = principalStr.split(":")[0];
                log.debug("[TenantContext] Principal 문자열에서 테넌트 ID 추출: {}", tenantId);
                return tenantId;
            }
            // "::" 구분자가 없으면 전체를 테넌트 ID로 간주
            log.debug("[TenantContext] Principal 문자열을 테넌트 ID로 사용: {}", principalStr);
            return principalStr;
        }

        // 6. 추출 실패
        log.error("[TenantContext] 테넌트 ID 추출 실패. Principal 타입: {}", principal.getClass().getName());
        throw new UnauthorizedException("테넌트 정보를 찾을 수 없습니다.");
    }

    /**
     * 현재 인증된 사용자의 사용자 ID 반환
     *
     * @return 사용자 ID
     * @throws UnauthorizedException 인증 정보가 없는 경우
     */
    public static String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("인증 정보가 없습니다.");
        }

        Object principal = auth.getPrincipal();

        // AuthPrincipal 인터페이스 구현체
        if (principal instanceof AuthPrincipal) {
            return ((AuthPrincipal) principal).getUserId();
        }

        // UserDetails에서 추출 (username이 "tenantId:userId" 형식)
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            if (username != null && username.contains(":")) {
                return username.split(":")[1];
            }
            return username;
        }

        // principal이 문자열인 경우
        if (principal instanceof String) {
            String principalStr = (String) principal;
            if (principalStr.contains(":")) {
                return principalStr.split(":")[1];
            }
            return principalStr;
        }

        throw new UnauthorizedException("사용자 정보를 찾을 수 없습니다.");
    }

    /**
     * 테넌트 ID가 유효한지 검증
     *
     * @param tenantId 검증할 테넌트 ID
     * @return 유효 여부
     */
    public static boolean isValidTenantId(String tenantId) {
        return tenantId != null && !tenantId.isBlank() && !tenantId.equals("anonymousUser");
    }
}

