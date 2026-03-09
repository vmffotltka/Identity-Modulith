package com.identitymodulith.user.application;

import com.identitymodulith.user.domain.exception.BusinessException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상담사 정지 UseCase
 * <p>
 * ACTIVE 상태의 상담사를 SUSPENDED 상태로 변경합니다.
 * 정지된 상담사는 로그인할 수 없으며, 모든 활성 세션이 종료됩니다.
 * </p>
 *
 * 비즈니스 규칙:
 * - ACTIVE 상태만 정지 가능
 * - 본인 정지 불가
 * - 권한 검증 필요 (DataScope)
 * - KeyCloak 동기화 필요
 */
public interface SuspendAgentUseCase {

    /**
     * 상담사를 정지 상태로 변경합니다.
     *
     * @param command 정지 명령
     * @throws BusinessException
     *         - AGENT_NOT_FOUND: 상담사를 찾을 수 없음
     *         - INVALID_STATUS_TRANSITION: ACTIVE 상태가 아님
     *         - CANNOT_SUSPEND_SELF: 본인 정지 시도
     *         - UNAUTHORIZED: 권한 없음
     */
    void suspendAgent(SuspendAgentCommand command);

    @Getter
    @Builder
    class SuspendAgentCommand {
        private final String tenantId;
        private final UUID agentId;
        private final UUID actorId;  // 정지를 요청한 사용자
    }

    @Getter
    @Builder
    class SuspendAgentResult {
        private final UUID agentId;
        private final String status;
        private final LocalDateTime suspendedAt;
    }
}
