package com.identitymodulith.user.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@Schema(description = "비밀번호 초기화 응답")
public class ResetPasswordResponse {

    @Schema(description = "상담사 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID agentId;

    @Schema(
        description = "새로 생성된 임시 비밀번호 (일회성, 팝업으로 표시 후 재조회 불가)",
        example = "Reset1234!@#$"
    )
    private String tempPassword;
}
