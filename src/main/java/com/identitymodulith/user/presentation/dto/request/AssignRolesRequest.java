package com.identitymodulith.user.presentation.dto.request;

import com.identitymodulith.user.domain.model.Agent.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "상담사 역할 일괄 지정 요청")
public class AssignRolesRequest {

    @Schema(description = "상담사 ID", example = "10000000-0000-0000-0000-000000000003")
    private UUID agentId;

    @Schema(description = "할당할 역할 목록 (최소 1개 이상)", required = true)
    @Valid
    private Set<RoleDto> roles;

    @Schema(description = "할당할 역할 ID 목록 (roles 대신 사용 가능)", example = "[\"20000000-0000-0000-0000-000000000002\"]")
    private Set<String> roleIds;

    @Schema(description = "할당할 역할 이름 목록 (roles, roleIds 대신 사용 가능)", example = "[\"TEAM_LEAD\", \"MEMBER\"]")
    private Set<String> roleNames;

    /**
     * 역할 정보가 유효한지 검증
     */
    public boolean hasValidRoles() {
        return (roles != null && !roles.isEmpty())
            || (roleIds != null && !roleIds.isEmpty())
            || (roleNames != null && !roleNames.isEmpty());
    }

    @Getter
    @Setter
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
