package com.identitymodulith.user.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상담사 활성화 도메인 이벤트
 *
 * <p>SUSPENDED → ACTIVE 상태 전이 시 발행됩니다.</p>
 * <ul>
 *   <li>Keycloak 계정 재활성화 처리</li>
 * </ul>
 */
public record AgentActivatedEvent(
        UUID agentId,
        String tenantId,
        String activatedByUserId,
        LocalDateTime activatedAt
) {}

