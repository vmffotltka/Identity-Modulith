package com.nexfron.identitymodulith.rbac.application;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RbacQueryService {

    private final RolePermissionRepository rolePermissionRepository;

    public Set<String> permissionsOfRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of();
        }

        return rolePermissionRepository.findByRoleNameIn(roleNames).stream()
                .map(RolePermission::getPermissionCode)
                .collect(Collectors.toSet());
    }
}