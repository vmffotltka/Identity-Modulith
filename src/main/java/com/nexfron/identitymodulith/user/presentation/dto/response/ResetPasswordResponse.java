package com.nexfron.identitymodulith.user.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ResetPasswordResponse {

    private UUID agentId;
    private String tempPassword;  // 일회성 임시 비밀번호
}
