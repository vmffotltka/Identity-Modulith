package com.identitymodulith.user.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상담사 퇴사 도메인 이벤트
 *
 * <p>상태가 RETIRED로 전이될 때 발행됩니다.</p>
 * <ul>
 *   <li>RBAC 모듈: 모든 역할/권한 제거</li>
 *   <li>Keycloak 계정 비활성화</li>
 * </ul>
 */
public record AgentRetiredEvent(
        UUID agentId,
        String tenantId,
        String retiredByUserId,
        String deletePolicy,
        LocalDateTime retiredAt
) {}

