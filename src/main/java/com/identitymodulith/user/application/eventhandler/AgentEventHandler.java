package com.identitymodulith.user.application.eventhandler;

import com.identitymodulith.user.application.port.RbacPort;
import com.identitymodulith.user.domain.event.AgentActivatedEvent;
import com.identitymodulith.user.domain.event.AgentRetiredEvent;
import com.identitymodulith.user.domain.event.AgentSuspendedEvent;
import com.identitymodulith.user.domain.event.AgentTransferredEvent;
import com.identitymodulith.user.infrastructure.keycloak.KeycloakAdminClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 상담사 도메인 이벤트 핸들러
 *
 * <p>트랜잭션 커밋 후(AFTER_COMMIT) 처리하여 DB 저장 성공 시에만 후속 작업을 수행합니다.</p>
 *
 * <h2>처리 이벤트</h2>
 * <ul>
 *   <li>{@link AgentSuspendedEvent}   - Keycloak 계정 비활성화 (역할 유지, 정지는 복구 가능)</li>
 *   <li>{@link AgentActivatedEvent}   - Keycloak 계정 재활성화</li>
 *   <li>{@link AgentRetiredEvent}     - RBAC 모든 역할 제거 + Keycloak 계정 비활성화</li>
 *   <li>{@link AgentTransferredEvent} - 부서 이동 로그 기록 (DataScope는 요청 시 재계산)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentEventHandler {

    private final RbacPort rbacPort;
    private final KeycloakAdminClient keycloakAdminClient;

    /**
     * 상담사 정지 이벤트 처리
     * - 정지는 복구 가능하므로 RBAC 역할은 유지합니다.
     * - Keycloak 계정을 비활성화하여 로그인을 차단합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAgentSuspended(AgentSuspendedEvent event) {
        log.info("[EVENT] 상담사 정지 처리 - agentId={}, tenantId={}, suspendedBy={}",
                event.agentId(), event.tenantId(), event.suspendedByUserId());
        keycloakAdminClient.disableUser(event.agentId().toString());
    }

    /**
     * 상담사 활성화 이벤트 처리
     * - Keycloak 계정을 재활성화하여 로그인을 허용합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAgentActivated(AgentActivatedEvent event) {
        log.info("[EVENT] 상담사 활성화 처리 - agentId={}, tenantId={}, activatedBy={}",
                event.agentId(), event.tenantId(), event.activatedByUserId());
        keycloakAdminClient.enableUser(event.agentId().toString());
    }

    /**
     * 상담사 퇴사 이벤트 처리
     * - 퇴사는 복구 불가이므로 RBAC 모든 역할을 즉시 제거합니다.
     * - Keycloak 계정을 비활성화하여 로그인을 영구 차단합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAgentRetired(AgentRetiredEvent event) {
        log.info("[EVENT] 상담사 퇴사 처리 시작 - agentId={}, tenantId={}, policy={}",
                event.agentId(), event.tenantId(), event.deletePolicy());

        // 1. RBAC 역할 전체 제거
        try {
            rbacPort.removeAllRolesFromAgent(event.agentId().toString());
            log.info("[EVENT] 퇴사 상담사 RBAC 역할 전체 제거 완료 - agentId={}", event.agentId());
        } catch (Exception e) {
            log.error("[EVENT] 퇴사 상담사 RBAC 역할 제거 실패 - agentId={}, 원인: {}",
                    event.agentId(), e.getMessage(), e);
        }

        // 2. Keycloak 계정 비활성화
        keycloakAdminClient.disableUser(event.agentId().toString());
    }

    /**
     * 상담사 부서 이동 이벤트 처리
     * - DataScope는 ThreadLocal 기반으로 요청 시마다 재계산되므로 별도 캐시 처리 불필요.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAgentTransferred(AgentTransferredEvent event) {
        log.info("[EVENT] 상담사 부서 이동 완료 - agentId={}, from={}, to={}",
                event.agentId(), event.fromOrganizationId(), event.toOrganizationId());
    }
}
