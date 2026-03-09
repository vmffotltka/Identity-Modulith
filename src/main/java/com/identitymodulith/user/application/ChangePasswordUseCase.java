package com.identitymodulith.user.application;

import com.identitymodulith.user.domain.exception.BusinessException;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 상담사 비밀번호 변경 UseCase
 * <p>
 * 상담사 본인이 현재 비밀번호를 알고 있는 상태에서 새 비밀번호로 변경합니다.
 * 관리자가 수행하는 비밀번호 초기화(ResetPassword)와는 다릅니다.
 * </p>
 *
 * <h3>비즈니스 규칙</h3>
 * <ul>
 *   <li>PC-001: 현재 비밀번호 검증 필수</li>
 *   <li>PC-002: 새 비밀번호 != 현재 비밀번호</li>
 *   <li>PC-003: 변경 후 passwordMustChange = false</li>
 *   <li>PC-004: 본인만 변경 가능</li>
 * </ul>
 *
 * @see ResetPasswordUseCase
 */
public interface ChangePasswordUseCase {

    /**
     * 상담사 본인의 비밀번호를 변경합니다.
     *
     * @param command 비밀번호 변경 명령 (agentId, currentPassword, newPassword 포함)
     * @throws BusinessException
     *         - AGENT_NOT_FOUND: 상담사를 찾을 수 없음
     *         - INVALID_PASSWORD: 현재 비밀번호가 일치하지 않음
     *         - SAME_PASSWORD: 새 비밀번호가 현재 비밀번호와 동일함
     *         - CANNOT_CHANGE_OTHERS_PASSWORD: 본인이 아닌 다른 사용자의 비밀번호 변경 시도
     *         - AGENT_ALREADY_RETIRED: 퇴사한 상담사
     */
    void changePassword(ChangePasswordCommand command);

    @Getter
    @Builder
    class ChangePasswordCommand {
        private final String tenantId;
        private final UUID agentId;
        private final UUID actorId;  // 변경 요청자 (본인 확인용)
        private final String currentPassword;
        private final String newPassword;
    }
}
