package com.nexfron.identitymodulith.user.adapter.in.web;

import com.nexfron.identitymodulith.user.adapter.in.web.dto.*;
import com.nexfron.identitymodulith.user.application.port.in.*;
import com.nexfron.identitymodulith.user.application.port.in.CreateAgentUseCase.CreateAgentCommand;
import com.nexfron.identitymodulith.user.application.port.in.CreateAgentUseCase.CreateAgentResult;
import com.nexfron.identitymodulith.user.application.port.in.GetAgentUseCase.AgentSearchCriteria;
import com.nexfron.identitymodulith.user.application.port.in.ResetPasswordUseCase.ResetPasswordResult;
import com.nexfron.identitymodulith.user.application.port.in.UpdateAgentUseCase.UpdateAgentCommand;
import com.nexfron.identitymodulith.user.domain.Skill;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final CreateAgentUseCase createAgentUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final UpdateAgentUseCase updateAgentUseCase;
    private final RetireAgentUseCase retireAgentUseCase;
    private final GetAgentUseCase getAgentUseCase;
    private final ManageRoleSkillUseCase manageRoleSkillUseCase;
    private final CheckUsernameUseCase checkUsernameUseCase;

    @PostMapping
    public ResponseEntity<CreateAgentResponse> createAgent(@RequestBody CreateAgentRequest request) {
        CreateAgentCommand command = CreateAgentCommand.builder()
                .username(request.getUsername())
                .name(request.getName())
                .organizationId(request.getOrganizationId())
                .build();

        CreateAgentResult result = createAgentUseCase.createAgent(command);

        CreateAgentResponse response = CreateAgentResponse.builder()
                .agentId(result.getAgentId())
                .username(result.getUsername())
                .tempPassword(result.getTempPassword())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        boolean isUnique = checkUsernameUseCase.isUsernameUnique(username);
        return ResponseEntity.ok(Map.of("isUnique", isUnique));
    }

    @GetMapping("/{agentId}")
    public ResponseEntity<AgentResponse> getAgent(@PathVariable UUID agentId) {
        var agentInfo = getAgentUseCase.getAgent(agentId);
        return ResponseEntity.ok(AgentResponse.from(agentInfo));
    }

    @GetMapping
    public ResponseEntity<List<AgentResponse>> getAgents(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(defaultValue = "false") boolean includeRetired) {

        AgentSearchCriteria criteria = AgentSearchCriteria.builder()
                .organizationId(organizationId)
                .includeRetired(includeRetired)
                .build();

        List<AgentResponse> responses = getAgentUseCase.getAgents(criteria).stream()
                .map(AgentResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{agentId}")
    public ResponseEntity<Void> updateAgent(
            @PathVariable UUID agentId,
            @RequestBody UpdateAgentRequest request) {

        UpdateAgentCommand command = UpdateAgentCommand.builder()
                .agentId(agentId)
                .name(request.getName())
                .build();

        updateAgentUseCase.updateAgent(command);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{agentId}/organization")
    public ResponseEntity<Void> transferOrganization(
            @PathVariable UUID agentId,
            @RequestBody TransferOrganizationRequest request) {

        updateAgentUseCase.transferOrganization(agentId, request.getOrganizationId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{agentId}/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@PathVariable UUID agentId) {
        ResetPasswordResult result = resetPasswordUseCase.resetPassword(agentId);

        ResetPasswordResponse response = ResetPasswordResponse.builder()
                .agentId(result.getAgentId())
                .tempPassword(result.getTempPassword())
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{agentId}")
    public ResponseEntity<Void> retireAgent(@PathVariable UUID agentId) {
        retireAgentUseCase.retireAgent(agentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{agentId}/roles")
    public ResponseEntity<Void> assignRoles(
            @PathVariable UUID agentId,
            @RequestBody AssignRolesRequest request) {

        var roles = request.getRoles().stream()
                .map(AssignRolesRequest.RoleDto::toDomain)
                .collect(Collectors.toSet());

        manageRoleSkillUseCase.assignRoles(agentId, roles);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{agentId}/skills")
    public ResponseEntity<Void> assignSkills(
            @PathVariable UUID agentId,
            @RequestBody AssignSkillsRequest request) {

        var skills = request.getSkills().stream()
                .map(Skill::new)
                .collect(Collectors.toSet());

        manageRoleSkillUseCase.assignSkills(agentId, skills);
        return ResponseEntity.noContent().build();
    }
}