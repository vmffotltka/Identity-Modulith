package com.identitymodulith.user.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상담사 정지 도메인 이벤트
 *
 * <p>ACTIVE → SUSPENDED 상태 전이 시 발행됩니다.</p>
 * <ul>
 *   <li>Keycloak 계정 비활성화 처리</li>
 *   <li>활성 세션 종료</li>
 * </ul>
 */
public record AgentSuspendedEvent(
        UUID agentId,
        String tenantId,
        String suspendedByUserId,
        LocalDateTime suspendedAt
) {}

