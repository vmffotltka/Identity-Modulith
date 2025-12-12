package com.nexfron.identitymodulith.user.application.port.in;

import com.nexfron.identitymodulith.user.domain.Role;

import java.util.Set;
import java.util.UUID;

public interface ManageRoleUseCase {

    void assignRoles(UUID agentId, Set<Role> roles);
}
