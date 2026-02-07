package com.nexfron.identitymodulith.user.application;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상담사 활성화 UseCase
 * <p>
 * SUSPENDED 상태의 상담사를 ACTIVE 상태로 변경합니다.
 * 활성화된 상담사는 정상적으로 로그인할 수 있습니다.
 * </p>
 *
 * 비즈니스 규칙:
 * - SUSPENDED 상태만 활성화 가능
 * - RETIRED 상태는 활성화 불가 (복구 불가)
 * - 소속 부서가 INACTIVE면 활성화 불가
 * - 권한 검증 필요 (DataScope)
 * - KeyCloak 동기화 필요
 */
public interface ActivateAgentUseCase {

    /**
     * 상담사를 활성 상태로 변경합니다.
     *
     * @param command 활성화 명령
     * @throws com.nexfron.identitymodulith.user.domain.exception.BusinessException
     *         - AGENT_NOT_FOUND: 상담사를 찾을 수 없음
     *         - INVALID_STATUS_TRANSITION: SUSPENDED 상태가 아님
     *         - AGENT_ALREADY_RETIRED: 이미 퇴사한 상담사 (복구 불가)
     *         - DEPT_INACTIVE: 소속 부서가 비활성
     *         - UNAUTHORIZED: 권한 없음
     */
    void activateAgent(ActivateAgentCommand command);

    @Getter
    @Builder
    class ActivateAgentCommand {
        private final String tenantId;
        private final UUID agentId;
        private final UUID actorId;  // 활성화를 요청한 사용자
    }

    @Getter
    @Builder
    class ActivateAgentResult {
        private final UUID agentId;
        private final String status;
        private final LocalDateTime activatedAt;
    }
}
