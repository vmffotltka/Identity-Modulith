package com.identitymodulith.user.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상담사 부서 이동 도메인 이벤트
 *
 * <p>상담사의 소속 부서가 변경될 때 발행됩니다.</p>
 * <ul>
 *   <li>Organization 모듈: 부서별 인원 캐시 갱신</li>
 *   <li>RBAC 모듈: DataScope 재계산</li>
 * </ul>
 */
public record AgentTransferredEvent(
        UUID agentId,
        String tenantId,
        String fromOrganizationId,
        String toOrganizationId,
        LocalDateTime transferredAt
) {}

