package com.identitymodulith.user.application;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/** 상담사 퇴사 처리 유스케이스 계약. */
public interface RetireAgentUseCase {

    void retireAgent(RetireAgentCommand command);

    @Getter
    @Builder
    class RetireAgentCommand {
        private final String tenantId;
        private final UUID agentId;
        private final UUID actorId;
        private final RetireDeletePolicy deletePolicy;
        private final Integer retentionDays;
    }

    @Getter
    @Builder
    class RetireAgentResult {
        private final UUID agentId;
        private final String status;
        private final LocalDateTime retiredAt;
        private final LocalDateTime scheduledDeleteAt;
        private final RetireDeletePolicy deletePolicy;
    }

    /** 퇴사 후 개인정보 처리 정책. */
    enum RetireDeletePolicy {
        /** 즉시 익명화 처리. */
        IMMEDIATE,

        /** 보관 기간 후 스케줄러가 삭제 처리. */
        SCHEDULED,

        /** 상태만 퇴사로 바꾸고 데이터는 보존. */
        PRESERVE
    }
}
