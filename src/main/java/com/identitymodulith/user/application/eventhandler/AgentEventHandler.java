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

/** 상담사 도메인 이벤트 후처리 핸들러(AFTER_COMMIT). */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentEventHandler {

    private final RbacPort rbacPort;
    private final KeycloakAdminClient keycloakAdminClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAgentSuspended(AgentSuspendedEvent event) {
        log.info("[EVENT] 상담사 정지 처리 - agentId={}, tenantId={}, suspendedBy={}",
                event.agentId(), event.tenantId(), event.suspendedByUserId());
        keycloakAdminClient.disableUser(event.agentId().toString());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAgentActivated(AgentActivatedEvent event) {
        log.info("[EVENT] 상담사 활성화 처리 - agentId={}, tenantId={}, activatedBy={}",
                event.agentId(), event.tenantId(), event.activatedByUserId());
        keycloakAdminClient.enableUser(event.agentId().toString());
    }

    /** 퇴사 시 RBAC 역할 제거 후 Keycloak 계정을 비활성화한다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAgentRetired(AgentRetiredEvent event) {
        log.info("[EVENT] 상담사 퇴사 처리 시작 - agentId={}, tenantId={}, policy={}",
                event.agentId(), event.tenantId(), event.deletePolicy());

        try {
            rbacPort.removeAllRolesFromAgent(event.agentId().toString());
            log.info("[EVENT] 퇴사 상담사 RBAC 역할 전체 제거 완료 - agentId={}", event.agentId());
        } catch (Exception e) {
            log.error("[EVENT] 퇴사 상담사 RBAC 역할 제거 실패 - agentId={}, 원인: {}",
                    event.agentId(), e.getMessage(), e);
        }

        keycloakAdminClient.disableUser(event.agentId().toString());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAgentTransferred(AgentTransferredEvent event) {
        log.info("[EVENT] 상담사 부서 이동 완료 - agentId={}, from={}, to={}",
                event.agentId(), event.fromOrganizationId(), event.toOrganizationId());
    }
}
