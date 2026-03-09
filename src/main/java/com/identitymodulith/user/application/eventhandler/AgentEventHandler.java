package com.identitymodulith.user.application.eventhandler;

import com.identitymodulith.user.application.port.RbacPort;
import com.identitymodulith.user.domain.event.AgentActivatedEvent;
import com.identitymodulith.user.domain.event.AgentRetiredEvent;
import com.identitymodulith.user.domain.event.AgentSuspendedEvent;
import com.identitymodulith.user.domain.event.AgentTransferredEvent;
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
 *   <li>{@link AgentSuspendedEvent} - RBAC 역할 유지 (정지는 복구 가능), 로그 기록</li>
 *   <li>{@link AgentActivatedEvent} - 활성화 로그 기록</li>
 *   <li>{@link AgentRetiredEvent}   - RBAC 모든 역할 제거 (퇴사는 복구 불가)</li>
 *   <li>{@link AgentTransferredEvent} - 부서 이동 로그 기록</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentEventHandler {

    private final RbacPort rbacPort;

    /**
     * 상담사 정지 이벤트 처리
     * - 정지는 복구 가능하므로 역할은 유지합니다.
     * - 향후 Keycloak 계정 비활성화 연동 시 여기에 추가합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAgentSuspended(AgentSuspendedEvent event) {
        log.info("[EVENT] 상담사 정지 처리 - agentId={}, tenantId={}, suspendedBy={}",
                event.agentId(), event.tenantId(), event.suspendedByUserId());
        // 정지 상태는 복구 가능 → 역할 유지
        // TODO: Keycloak 계정 비활성화 연동
    }

    /**
     * 상담사 활성화 이벤트 처리
     * - 향후 Keycloak 계정 재활성화 연동 시 여기에 추가합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAgentActivated(AgentActivatedEvent event) {
        log.info("[EVENT] 상담사 활성화 처리 - agentId={}, tenantId={}, activatedBy={}",
                event.agentId(), event.tenantId(), event.activatedByUserId());
        // TODO: Keycloak 계정 재활성화 연동
    }

    /**
     * 상담사 퇴사 이벤트 처리
     * - 퇴사는 복구 불가이므로 RBAC 모든 역할을 즉시 제거합니다.
     * - 향후 Keycloak 계정 비활성화 연동 시 여기에 추가합니다.
     */
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
        // TODO: Keycloak 계정 비활성화 연동
    }

    /**
     * 상담사 부서 이동 이벤트 처리
     * - 향후 DataScope 캐시 갱신 연동 시 여기에 추가합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAgentTransferred(AgentTransferredEvent event) {
        log.info("[EVENT] 상담사 부서 이동 처리 - agentId={}, from={}, to={}",
                event.agentId(), event.fromOrganizationId(), event.toOrganizationId());
        // TODO: Organization 모듈 DataScope 캐시 갱신 연동
    }
}

