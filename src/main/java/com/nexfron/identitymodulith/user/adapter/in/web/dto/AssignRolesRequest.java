package com.nexfron.identitymodulith.user.adapter.in.web.dto;

import com.nexfron.identitymodulith.user.domain.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
public class AssignRolesRequest {

    private Set<RoleDto> roles;

    @Getter
    @NoArgsConstructor
    public static class RoleDto {
        private String name;
        private Role.RoleType type;

        public Role toDomain() {
            return new Role(name, type);
        }
    }
}