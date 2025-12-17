package com.nexfron.identitymodulith.user.application;

import com.nexfron.identitymodulith.user.domain.Agent.Role;

import java.util.Set;
import java.util.UUID;

public interface ManageRoleUseCase {

    void assignRoles(UUID agentId, Set<Role> roles);
}
