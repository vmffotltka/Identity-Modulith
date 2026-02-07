package com.nexfron.identitymodulith.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "상담사 정보 수정 요청")
public class UpdateAgentRequest {

    @Schema(description = "변경할 상담사 이름", example = "홍길동", required = true)
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자 이내여야 합니다")
    private String name;
}
