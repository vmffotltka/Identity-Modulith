package com.nexfron.identitymodulith.user.application.service;

import com.nexfron.identitymodulith.user.application.port.in.*;
import com.nexfron.identitymodulith.user.application.port.out.AgentRepository;
import com.nexfron.identitymodulith.user.application.port.out.PasswordEncoder;
import com.nexfron.identitymodulith.user.application.port.out.PasswordGenerator;
import com.nexfron.identitymodulith.user.domain.Agent;
import com.nexfron.identitymodulith.user.domain.AgentStatus;
import com.nexfron.identitymodulith.user.domain.Role;
import com.nexfron.identitymodulith.user.domain.Skill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AgentService implements
        CreateAgentUseCase,
        ResetPasswordUseCase,
        UpdateAgentUseCase,
        RetireAgentUseCase,
        GetAgentUseCase,
        ManageRoleSkillUseCase,
        CheckUsernameUseCase {

    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;

    @Override
    public CreateAgentResult createAgent(CreateAgentCommand command) {
        if (!isUsernameUnique(command.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + command.getUsername());
        }

        String tempPassword = passwordGenerator.generateTempPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);

        Agent agent = Agent.builder()
                .username(command.getUsername())
                .passwordHash(encodedPassword)
                .name(command.getName())
                .organizationId(command.getOrganizationId())
                .status(AgentStatus.ACTIVE)
                .passwordMustChange(true)
                .build();

        Agent savedAgent = agentRepository.save(agent);

        return CreateAgentResult.builder()
                .agentId(savedAgent.getId())
                .username(savedAgent.getUsername())
                .tempPassword(tempPassword)
                .build();
    }

    @Override
    public ResetPasswordResult resetPassword(UUID agentId) {
        Agent agent = findAgentById(agentId);

        String tempPassword = passwordGenerator.generateTempPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);

        agent.resetPassword(encodedPassword);
        agentRepository.save(agent);

        return ResetPasswordResult.builder()
                .agentId(agent.getId())
                .tempPassword(tempPassword)
                .build();
    }

    @Override
    public void updateAgent(UpdateAgentCommand command) {
        Agent agent = findAgentById(command.getAgentId());
        agent.updateName(command.getName());
        agentRepository.save(agent);
    }

    @Override
    public void transferOrganization(UUID agentId, UUID newOrganizationId) {
        Agent agent = findAgentById(agentId);
        agent.transferOrganization(newOrganizationId);
        agentRepository.save(agent);
    }

    @Override
    public void retireAgent(UUID agentId) {
        Agent agent = findAgentById(agentId);
        agent.retire();
        agentRepository.save(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentInfo getAgent(UUID agentId) {
        Agent agent = findAgentById(agentId);
        return toAgentInfo(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentInfo> getAgents(AgentSearchCriteria criteria) {
        List<Agent> agents;

        if (criteria.getOrganizationId() != null) {
            if (criteria.isIncludeRetired()) {
                agents = agentRepository.findByOrganizationId(criteria.getOrganizationId());
            } else {
                agents = agentRepository.findByOrganizationIdAndStatus(
                        criteria.getOrganizationId(), AgentStatus.ACTIVE);
            }
        } else {
            if (criteria.isIncludeRetired()) {
                agents = agentRepository.findAll();
            } else {
                agents = agentRepository.findAllByStatus(AgentStatus.ACTIVE);
            }
        }

        return agents.stream()
                .map(this::toAgentInfo)
                .toList();
    }

    @Override
    public void assignRoles(UUID agentId, Set<Role> roles) {
        Agent agent = findAgentById(agentId);
        agent.getRoles().clear();
        roles.forEach(agent::addRole);
        agentRepository.save(agent);
    }

    @Override
    public void assignSkills(UUID agentId, Set<Skill> skills) {
        Agent agent = findAgentById(agentId);
        agent.getSkills().clear();
        skills.forEach(agent::addSkill);
        agentRepository.save(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameUnique(String username) {
        return !agentRepository.existsByUsername(username);
    }

    private Agent findAgentById(UUID agentId) {
        return agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
    }

    private AgentInfo toAgentInfo(Agent agent) {
        return AgentInfo.builder()
                .id(agent.getId())
                .username(agent.getUsername())
                .name(agent.getName())
                .organizationId(agent.getOrganizationId())
                .status(agent.getStatus())
                .passwordMustChange(agent.isPasswordMustChange())
                .createdAt(agent.getCreatedAt())
                .retiredAt(agent.getRetiredAt())
                .roles(agent.getRoles())
                .skills(agent.getSkills())
                .build();
    }
}