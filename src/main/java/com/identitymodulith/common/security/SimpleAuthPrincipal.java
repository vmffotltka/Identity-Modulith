package com.identitymodulith.common.security;

import java.util.UUID;

/**
 * SimpleAuthPrincipal - AuthPrincipal 인터페이스의 기본 구현체
 *
 * <p>Spring Security Authentication의 Principal 객체로 사용되는 표준 구현체입니다.
 * JWT 토큰 파싱, Keycloak 연동, 테스트 등 다양한 상황에서 활용됩니다.</p>
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li>멀티테넌시 지원: tenantId로 테넌트 격리</li>
 *   <li>사용자 식별: agentId(UUID)로 사용자 고유 식별</li>
 *   <li>불변 객체: record로 구현되어 안전함</li>
 * </ul>
 *
 * <h3>사용 예시:</h3>
 * <pre>{@code
 * // 인증 필터에서 생성
 * SimpleAuthPrincipal principal = new SimpleAuthPrincipal(
 *     "tenant-001",
 *     UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
 * );
 *
 * UsernamePasswordAuthenticationToken authentication =
 *     new UsernamePasswordAuthenticationToken(principal, null, authorities);
 * SecurityContextHolder.getContext().setAuthentication(authentication);
 * }</pre>
 *
 * @param tenantId 테넌트 ID (조직/회사 식별, 1~50자)
 * @param agentId 에이전트(사용자) ID (UUID)
 */
public record SimpleAuthPrincipal(String tenantId, UUID agentId) implements AuthPrincipal {

    /**
     * Compact Constructor - 유효성 검증
     */
    public SimpleAuthPrincipal {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId는 필수값입니다");
        }
        if (agentId == null) {
            throw new IllegalArgumentException("agentId는 필수값입니다");
        }
        if (tenantId.length() > 50) {
            throw new IllegalArgumentException("tenantId는 50자 이하여야 합니다");
        }
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String getUserId() {
        return agentId.toString();
    }

    /**
     * 사용자 ID를 UUID 형식으로 반환
     *
     * @return 사용자 ID (UUID 객체)
     */
    public UUID getAgentIdAsUuid() {
        return agentId;
    }

    @Override
    public String toString() {
        return "SimpleAuthPrincipal[tenantId=%s, agentId=%s]".formatted(tenantId, agentId);
    }
}
