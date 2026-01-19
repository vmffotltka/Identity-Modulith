package com.nexfron.identitymodulith.common.security;

/**
 * AuthPrincipal - 인증 주체 인터페이스
 *
 * <h2>목적:</h2>
 * Spring Security의 Authentication.getPrincipal()에서 반환될 수 있는
 * 커스텀 Principal 객체의 표준 인터페이스입니다.
 *
 * <h2>구현 클래스:</h2>
 * <ul>
 *   <li>JwtAuthenticationToken의 Principal</li>
 *   <li>CustomUserDetails</li>
 *   <li>기타 인증 메커니즘의 Principal</li>
 * </ul>
 *
 * @author Identity System Team
 * @version 1.0
 */
public interface AuthPrincipal {

    /**
     * 테넌트 ID 반환
     *
     * @return 테넌트 ID
     */
    String getTenantId();

    /**
     * 사용자 ID 반환
     *
     * @return 사용자 ID (UUID 문자열)
     */
    String getUserId();

    /**
     * 사용자명 반환 (선택사항)
     *
     * @return 사용자명
     */
    default String getUsername() {
        return getUserId();
    }
}

