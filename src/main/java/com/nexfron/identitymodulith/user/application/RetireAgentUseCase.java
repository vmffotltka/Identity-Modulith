package com.nexfron.identitymodulith.user.application;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상담사 퇴사 UseCase
 * <p>
 * 상담사를 RETIRED(퇴사) 상태로 변경합니다.
 * RETIRED 상태는 되돌릴 수 없으며, deletePolicy에 따라 개인정보를 처리합니다.
 * </p>
 *
 * 삭제 정책 (DeletePolicy):
 * - IMMEDIATE: 즉시 개인정보 익명화
 * - SCHEDULED: 예약 삭제 (retentionDays 후)
 * - PRESERVE: 데이터 영구 보존 (감사 목적)
 *
 * 비즈니스 규칙:
 * - 이미 RETIRED인 경우 재퇴사 불가
 * - 본인 퇴사 처리 불가
 * - SCHEDULED 시 retentionDays 필수
 * - 모든 역할/권한 자동 제거
 * - 키클록 동기화 필요
 */
public interface RetireAgentUseCase {

    /**
     * 상담사를 퇴사 상태로 변경합니다.
     *
     * @param command 퇴사 명령
     * @throws com.nexfron.identitymodulith.user.domain.exception.BusinessException
     *         - AGENT_NOT_FOUND: 상담사를 찾을 수 없음
     *         - AGENT_ALREADY_RETIRED: 이미 퇴사한 상담사
     *         - CANNOT_RETIRE_SELF: 본인 퇴사 시도
     *         - INVALID_REQUEST: deletePolicy=SCHEDULED인데 retentionDays 없음
     *         - UNAUTHORIZED: 권한 없음
     */
    void retireAgent(RetireAgentCommand command);

    @Getter
    @Builder
    class RetireAgentCommand {
        private final String tenantId;
        private final UUID agentId;
        private final UUID actorId;  // 퇴사를 처리한 사용자
        private final RetireDeletePolicy deletePolicy;
        private final Integer retentionDays;  // SCHEDULED일 때 필수, null 가능
    }

    @Getter
    @Builder
    class RetireAgentResult {
        private final UUID agentId;
        private final String status;
        private final LocalDateTime retiredAt;
        private final LocalDateTime scheduledDeleteAt;  // SCHEDULED인 경우만 값 있음
        private final RetireDeletePolicy deletePolicy;
    }

    /**
     * 퇴사자 개인정보 처리 정책
     */
    enum RetireDeletePolicy {
        /**
         * 즉시 익명화
         * - loginId → "deleted_" + UUID
         * - name → "탈퇴회원"
         * - email, phone, employeeId → null
         */
        IMMEDIATE,

        /**
         * 예약 삭제
         * - status = RETIRED, retiredAt = now()
         * - scheduledDeleteAt = now() + retentionDays
         * - 스케줄러가 지정된 시간에 IMMEDIATE와 동일 처리
         */
        SCHEDULED,

        /**
         * 영구 보존
         * - status = RETIRED, retiredAt = now()
         * - 데이터 변경 없음 (감사 목적)
         */
        PRESERVE
    }
}
