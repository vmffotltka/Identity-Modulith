package com.identitymodulith.organization.presentation.dto.request;

import com.identitymodulith.organization.domain.OrganizationConstants;
import com.identitymodulith.organization.domain.model.DepartmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "부서 수정 요청")
public class UpdateDepartmentRequest {

    @Size(
            min = OrganizationConstants.DEPARTMENT_NAME_MIN_LENGTH,
            max = OrganizationConstants.DEPARTMENT_NAME_MAX_LENGTH,
            message = "부서명은 " + OrganizationConstants.DEPARTMENT_NAME_MIN_LENGTH +
                    "-" + OrganizationConstants.DEPARTMENT_NAME_MAX_LENGTH + "자 사이여야 합니다"
    )
    @Schema(description = "변경할 부서명 (선택)", example = "AI개발팀")
    private String name;

    @Schema(description = "변경할 부서 타입 (선택)", example = "TEAM",
            allowableValues = {"COMPANY", "DIVISION", "TEAM", "GROUP", "CUSTOM"})
    private DepartmentType type;
}

