package com.nexfron.identitymodulith.user.application.port.in;

import com.nexfron.identitymodulith.user.domain.Role;
import com.nexfron.identitymodulith.user.domain.Skill;

import java.util.Set;
import java.util.UUID;

public interface ManageRoleSkillUseCase {

    void assignRoles(UUID agentId, Set<Role> roles);

    void assignSkills(UUID agentId, Set<Skill> skills);
}