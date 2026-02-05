package com.nexfron.identitymodulith.user.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@Schema(description = "상담사 생성 응답")
public class CreateAgentResponse {

    @Schema(description = "생성된 상담사 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID agentId;

    @Schema(description = "로그인 아이디", example = "agent001")
    private String loginId;

    @Schema(
        description = "임시 비밀번호 (일회성, 팝업으로 표시 후 재조회 불가)",
        example = "Temp1234!@#$"
    )
    private String tempPassword;
}
