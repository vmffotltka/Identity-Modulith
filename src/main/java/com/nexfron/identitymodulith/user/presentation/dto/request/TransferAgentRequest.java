package com.nexfron.identitymodulith.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상담사 부서 이동 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상담사 부서 이동 요청")
public class TransferAgentRequest {

    @Schema(
        description = "새로 배정될 조직(부서) ID",
        example = "550e8400-e29b-41d4-a716-446655440002",
        required = true
    )
    @NotBlank(message = "조직 ID는 필수입니다")
    private String newOrganizationId;
}
