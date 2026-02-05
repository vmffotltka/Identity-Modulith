package com.nexfron.identitymodulith.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "상담사 조직 이동 요청")
public class TransferOrganizationRequest {

    @Schema(
        description = "이동할 조직 ID",
        example = "550e8400-e29b-41d4-a716-446655440003",
        required = true
    )
    @NotBlank(message = "조직 ID는 필수입니다")
    private String organizationId;
}
