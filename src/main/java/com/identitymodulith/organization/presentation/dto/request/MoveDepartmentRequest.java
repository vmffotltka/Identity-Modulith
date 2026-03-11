package com.identitymodulith.organization.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "부서 이동 요청")
public class MoveDepartmentRequest {

    @Schema(description = "새 상위 부서 ID (null이면 루트로 이동)", example = "550e8400-e29b-41d4-a716-446655440001")
    private String newParentId;
}

