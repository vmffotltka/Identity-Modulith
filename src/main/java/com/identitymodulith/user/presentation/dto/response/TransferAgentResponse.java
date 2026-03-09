package com.identitymodulith.user.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상담사 부서 이동 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상담사 부서 이동 응답")
public class TransferAgentResponse {

    @Schema(description = "상담사 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID agentId;

    @Schema(description = "이전 조직 ID", example = "550e8400-e29b-41d4-a716-446655440001")
    private String fromOrganizationId;

    @Schema(description = "새 조직 ID", example = "550e8400-e29b-41d4-a716-446655440002")
    private String toOrganizationId;

    @Schema(description = "부서 이동 일시", example = "2024-01-15T14:30:00")
    private LocalDateTime transferredAt;
}
