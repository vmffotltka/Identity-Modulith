package com.nexfron.identitymodulith.user.presentation.dto.request;

import com.nexfron.identitymodulith.user.domain.model.Agent.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
@Schema(description = "상담사 역할 일괄 지정 요청")
public class AssignRolesRequest {

    @Schema(description = "할당할 역할 목록 (최소 1개 이상)", required = true)
    @NotEmpty(message = "역할은 최소 1개 이상이어야 합니다")
    @Valid
    private Set<RoleDto> roles;

    @Getter
    @NoArgsConstructor
    @Schema(description = "역할 정보")
    public static class RoleDto {

        @Schema(description = "역할 이름", example = "SENIOR_AGENT", required = true)
        @NotBlank(message = "역할 이름은 필수입니다")
        private String name;

        @Schema(
            description = "역할 유형",
            example = "POSITION",
            allowableValues = {"POSITION", "CHANNEL"},
            required = true
        )
        @NotNull(message = "역할 유형은 필수입니다")
        private Role.RoleType type;

        public Role toDomain() {
            return new Role(name, type);
        }
    }
}
