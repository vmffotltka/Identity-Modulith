package com.nexfron.identitymodulith.user.application;

import com.nexfron.identitymodulith.user.domain.exception.BusinessException;
import com.nexfron.identitymodulith.user.domain.exception.ErrorCode;
import com.nexfron.identitymodulith.user.domain.model.Agent;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public interface CreateAgentUseCase {

    CreateAgentResult createAgent(CreateAgentCommand command);

    @Getter
    @Builder
    class CreateAgentCommand {
        private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        private static final Pattern PHONE_PATTERN =
            Pattern.compile("^01[0-9]-\\d{3,4}-\\d{4}$|^02-\\d{3,4}-\\d{4}$|^\\d{2,3}-\\d{3,4}-\\d{4}$");
        private static final Pattern LOGIN_ID_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_.-]{4,20}$");

        private final String tenantId;
        private final String loginId;
        private final String name;
        private final String organizationId;
        private final String employeeId;  // 사번 (선택)
        private final String email;
        private final String phone;
        private final Set<Agent.Role> roles;  // C-003: 최소 1개 역할 필요

        /**
         * Command 유효성 검증
         *
         * @throws BusinessException 필수 필드 누락 또는 형식 오류
         */
        public void validate() {
            // 필수 필드 검증
            if (loginId == null || loginId.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "loginId는 필수입니다.");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "name은 필수입니다.");
            }
            if (organizationId == null || organizationId.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "organizationId는 필수입니다.");
            }

            // C-003: roles는 최소 1개 이상
            if (roles == null || roles.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "roles는 최소 1개 이상이어야 합니다.");
            }

            // loginId 형식 검증 (4-20자, 알파벳/숫자/특수문자(_.-) 포함)
            if (!LOGIN_ID_PATTERN.matcher(loginId).matches()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "loginId는 4-20자이며 알파벳, 숫자, _, ., -만 포함 가능합니다.");
            }

            // name 길이 검증 (1-100자)
            if (name.length() > 100) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "name은 100자 이내여야 합니다.");
            }

            // email 형식 검증 (선택사항이나 입력된 경우 유효성 검사)
            if (email != null && !email.trim().isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "email 형식이 올바르지 않습니다.");
            }

            // phone 형식 검증 (선택사항이나 입력된 경우 유효성 검사)
            if (phone != null && !phone.trim().isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "phone은 01X-XXXX-XXXX 또는 0XX-XXXX-XXXX 형식이어야 합니다.");
            }
        }
    }

    @Getter
    @Builder
    class CreateAgentResult {
        private final UUID agentId;
        private final String loginId;
        private final String tempPassword;  // 일회성 임시 비밀번호
    }
}
