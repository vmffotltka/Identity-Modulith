package com.identitymodulith.common.security;

/**
 * AuthPrincipal - 인증 주체 인터페이스
 *
 * <h2>목적:</h2>
 * Spring Security의 Authentication.getPrincipal()에서 반환될 수 있는
 * 커스텀 Principal 객체의 표준 인터페이스입니다.
 *
 * <h2>구현 클래스:</h2>
 * <ul>
 *   <li>{@link SimpleAuthPrincipal} - 기본 구현체 (멀티테넌시 지원)</li>
 *   <li>JwtAuthenticationToken의 Principal (Keycloak 연동 시)</li>
 *   <li>CustomUserDetails (향후 확장 가능)</li>
 * </ul>
 *
 * <h2>사용 예시:</h2>
 * <pre>{@code
 * // 1. 컨트롤러에서 사용
 * @GetMapping("/me")
 * public UserInfo getMyInfo(@AuthenticationPrincipal AuthPrincipal principal) {
 *     String tenantId = principal.getTenantId();
 *     String userId = principal.getUserId();
 *     return userService.getUserInfo(tenantId, userId);
 * }
 *
 * // 2. 인증 필터에서 설정
 * SimpleAuthPrincipal principal = new SimpleAuthPrincipal(
 *     "tenant-001",
 *     UUID.fromString("550e8400-...")
 * );
 * UsernamePasswordAuthenticationToken auth =
 *     new UsernamePasswordAuthenticationToken(principal, null, authorities);
 * SecurityContextHolder.getContext().setAuthentication(auth);
 * }</pre>
 *
 * @see SimpleAuthPrincipal
 * @see org.springframework.security.core.Authentication
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

