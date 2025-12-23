package com.nexfron.identitymodulith.user.infrastructure.rbac;

import com.nexfron.identitymodulith.rbac.application.RbacQueryService;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionJpaRepository;
import com.nexfron.identitymodulith.user.domain.model.Agent;
import com.nexfron.identitymodulith.user.domain.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RBAC 쿼리 서비스 구현체
 *
 * Agent의 역할들을 조회하고 해당 권한들을 반환합니다.
 */
@Service
@RequiredArgsConstructor
public class RbacQueryServiceImpl implements RbacQueryService {

    private final RolePermissionJpaRepository rolePermissionRepository;
    private final AgentRepository agentRepository;

    @Override
    public Set<String> permissionsOfRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of();
        }

        return rolePermissionRepository.findByRoleNameIn(roleNames).stream()
                .map(RolePermissionJpaEntity::getPermissionCode)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> permissionsOf(String tenantId, UUID agentId) {
        return agentRepository.findById(agentId)
                .filter(agent -> tenantId.equals(agent.getTenantId()))
                .map(agent -> {
                    Set<String> roleNames = agent.getRoles().stream()
                            .map(Agent.Role::getName)
                            .collect(Collectors.toSet());
                    return permissionsOfRoles(roleNames);
                })
                .orElse(Set.of());
    }
}

