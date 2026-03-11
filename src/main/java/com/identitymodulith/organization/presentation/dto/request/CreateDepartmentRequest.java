package com.identitymodulith.organization.presentation.dto.request;

import com.identitymodulith.organization.domain.OrganizationConstants;
import com.identitymodulith.organization.domain.model.DepartmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "부서 생성 요청")
public class CreateDepartmentRequest {

    @NotBlank(message = "부서명은 필수입니다")
    @Size(
            min = OrganizationConstants.DEPARTMENT_NAME_MIN_LENGTH,
            max = OrganizationConstants.DEPARTMENT_NAME_MAX_LENGTH,
            message = "부서명은 " + OrganizationConstants.DEPARTMENT_NAME_MIN_LENGTH +
                    "-" + OrganizationConstants.DEPARTMENT_NAME_MAX_LENGTH + "자 사이여야 합니다"
    )
    @Schema(description = "부서명", example = "플랫폼개발팀")
    private String name;

    @NotNull(message = "부서 타입은 필수입니다")
    @Schema(
            description = "부서 타입 (COMPANY, DIVISION, TEAM, GROUP, CUSTOM)",
            example = "TEAM",
            allowableValues = {"COMPANY", "DIVISION", "TEAM", "GROUP", "CUSTOM"}
    )
    private DepartmentType type;

    @NotBlank(message = "부서 코드는 필수입니다")
    @Size(min = 2, max = 30, message = "부서 코드는 2-30자 사이여야 합니다")
    @Schema(description = "부서 코드 (테넌트 내 고유)", example = "DEV-BE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "커스텀 타입명 (type=CUSTOM일 때 필수)", example = "센터", nullable = true)
    private String customTypeName;

    @Schema(description = "상위 부서 ID (최상위 부서인 경우 null)", example = "550e8400-e29b-41d4-a716-446655440000", nullable = true)
    private String parentId;
}

