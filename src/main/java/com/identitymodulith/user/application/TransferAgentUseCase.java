package com.identitymodulith.user.application;

import com.identitymodulith.user.domain.exception.BusinessException;
import com.identitymodulith.user.domain.model.Agent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상담사 부서 이동 유스케이스
 *
 * <p>상담사를 다른 조직(부서)으로 이동시킵니다.</p>
 *
 * <h3>비즈니스 규칙 (AGENT_SCENARIOS 6절)</h3>
 * <ul>
 *   <li>T-001: 대상 부서 존재 및 ACTIVE 확인 필요</li>
 *   <li>T-002: 동일 부서로 이동 불가</li>
 *   <li>T-003: RETIRED 상담사 이동 불가</li>
 *   <li>T-004: 행위자의 DataScope에 양쪽 부서 포함 필요</li>
 * </ul>
 *
 * @see Agent
 */
public interface TransferAgentUseCase {

    /**
     * 상담사를 다른 조직으로 이동시킵니다.
     *
     * @param command 부서 이동 명령
     * @return 부서 이동 결과
     * @throws BusinessException
     *         - AGENT_NOT_FOUND: 상담사를 찾을 수 없음
     *         - AGENT_ALREADY_RETIRED: 퇴사한 상담사는 이동 불가
     *         - SAME_ORGANIZATION: 동일한 조직으로 이동 시도
     *         - ORGANIZATION_NOT_FOUND: 대상 조직을 찾을 수 없음
     *         - ORGANIZATION_INACTIVE: 대상 조직이 비활성 상태
     *         - UNAUTHORIZED: 권한 없음
     */
    TransferAgentResult transferAgent(TransferAgentCommand command);

    @Getter
    @Builder
    class TransferAgentCommand {
        private final String tenantId;
        private final UUID agentId;
        private final String newOrganizationId;  // 새 조직 ID (deptId)
        private final UUID actorId;  // 이동을 처리한 사용자
    }

    @Getter
    @Builder
    class TransferAgentResult {
        private final UUID agentId;
        private final String fromOrganizationId;
        private final String toOrganizationId;
        private final LocalDateTime transferredAt;
    }
}
