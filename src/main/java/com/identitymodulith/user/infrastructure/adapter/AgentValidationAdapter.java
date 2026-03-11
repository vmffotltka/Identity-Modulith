package com.identitymodulith.user.infrastructure.adapter;

import com.identitymodulith.rbac.application.port.AgentValidationPort;
import com.identitymodulith.user.infrastructure.persistence.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AgentValidationPort 구현체
 *
 * <p>RBAC 모듈의 AgentValidationPort를 User 모듈에서 구현합니다.
 * 이 Adapter는 순환의존을 제거하기 위해 UserModuleApi 대신 사용됩니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentValidationAdapter implements AgentValidationPort {

    private final AgentRepository agentRepository;

    @Override
    public boolean isActiveAgent(String agentId) {
        // loginId로 조회 시도
        return agentRepository.findByLoginId(agentId)
                .map(agent -> {
                    boolean active = agent.isActive();
                    if (!active) {
                        log.debug("[User] 비활성/퇴사 상담사 역할 할당 차단 - agentId={}", agentId);
                    }
                    return active;
                })
                .orElse(true); // 존재하지 않으면 검증 스킵 (UUID agentId 케이스)
    }
}

