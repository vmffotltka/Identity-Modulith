package com.nexfron.identitymodulith.user.adapter.in.web;

import com.nexfron.identitymodulith.user.adapter.in.web.dto.*;
import com.nexfron.identitymodulith.user.application.port.in.*;
import com.nexfron.identitymodulith.user.application.port.in.CreateAgentUseCase.CreateAgentCommand;
import com.nexfron.identitymodulith.user.application.port.in.CreateAgentUseCase.CreateAgentResult;
import com.nexfron.identitymodulith.user.application.port.in.GetAgentUseCase.AgentSearchCriteria;
import com.nexfron.identitymodulith.user.application.port.in.ResetPasswordUseCase.ResetPasswordResult;
import com.nexfron.identitymodulith.user.application.port.in.UpdateAgentUseCase.UpdateAgentCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 상담사(Agent) 관리 REST API Controller
 */
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final CreateAgentUseCase createAgentUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final UpdateAgentUseCase updateAgentUseCase;
    private final RetireAgentUseCase retireAgentUseCase;
    private final GetAgentUseCase getAgentUseCase;
    private final ManageRoleUseCase manageRoleUseCase;
    private final CheckLoginIdUseCase checkLoginIdUseCase;

    /**
     * 상담사 생성 (Onboarding)
     *
     * @param request 요청 본문
     *                - loginId: 로그인 아이디 (중복 불가)
     *                - name: 상담사 이름
     *                - organizationId: 소속 조직 ID (UUID)
     * @return 201 Created
     *         - agentId: 생성된 상담사 ID (UUID)
     *         - loginId: 로그인 아이디
     *         - tempPassword: 임시 비밀번호 (일회성, 팝업으로 표시 후 재조회 불가)
     */
    @PostMapping
    public ResponseEntity<CreateAgentResponse> createAgent(@RequestBody CreateAgentRequest request) {
        CreateAgentCommand command = CreateAgentCommand.builder()
                .tenantId(request.getTenantId())
                .loginId(request.getLoginId())
                .name(request.getName())
                .organizationId(request.getOrganizationId())
                .build();

        CreateAgentResult result = createAgentUseCase.createAgent(command);

        CreateAgentResponse response = CreateAgentResponse.builder()
                .agentId(result.getAgentId())
                .loginId(result.getLoginId())
                .tempPassword(result.getTempPassword())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 아이디 중복 체크
     *
     * @param loginId 검사할 로그인 아이디
     * @return 200 OK
     *         - isUnique: true(사용 가능) / false(중복)
     */
    @GetMapping("/check-login-id")
    public ResponseEntity<Map<String, Boolean>> checkLoginId(@RequestParam String loginId) {
        boolean isUnique = checkLoginIdUseCase.isLoginIdUnique(loginId);
        return ResponseEntity.ok(Map.of("isUnique", isUnique));
    }

    /**
     * 상담사 단건 조회
     *
     * @param agentId 상담사 ID (UUID)
     * @return 200 OK
     *         - id: 상담사 ID
     *         - loginId: 로그인 아이디
     *         - name: 상담사 이름
     *         - organizationId: 소속 조직 ID
     *         - status: 상태 (ACTIVE / RETIRED)
     *         - passwordMustChange: 비밀번호 변경 필요 여부
     *         - createdAt: 생성 일시
     *         - retiredAt: 퇴사 일시 (nullable)
     *         - roles: 역할 목록 [{name, type}]
     *         ※ 비밀번호(해시값 포함)는 절대 리턴하지 않음
     */
    @GetMapping("/{agentId}")
    public ResponseEntity<AgentResponse> getAgent(@PathVariable UUID agentId) {
        var agentInfo = getAgentUseCase.getAgent(agentId);
        return ResponseEntity.ok(AgentResponse.from(agentInfo));
    }

    /**
     * 상담사 목록 조회
     *
     * @param tenantId 테넌트 ID
     * @param organizationId 조직 ID로 필터링 (optional)
     * @param includeRetired 퇴사자 포함 여부 (default: false)
     * @return 200 OK - AgentResponse 목록
     *         ※ 기본적으로 ACTIVE 상태만 조회, includeRetired=true 시 퇴사자 포함
     */
    @GetMapping
    public ResponseEntity<List<AgentResponse>> getAgents(
            @RequestParam String tenantId,
            @RequestParam(required = false) String organizationId,
            @RequestParam(defaultValue = "false") boolean includeRetired) {

        AgentSearchCriteria criteria = AgentSearchCriteria.builder()
                .tenantId(tenantId)
                .organizationId(organizationId)
                .includeRetired(includeRetired)
                .build();

        List<AgentResponse> responses = getAgentUseCase.getAgents(criteria).stream()
                .map(AgentResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * 상담사 정보 수정
     *
     * @param agentId 상담사 ID (UUID)
     * @param request 요청 본문
     *                - name: 변경할 상담사 이름
     * @return 204 No Content
     */
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

    /**
     * 상담사 조직 이동 (Transfer)
     *
     * @param agentId 상담사 ID (UUID)
     * @param request 요청 본문
     *                - organizationId: 이동할 조직 ID (UUID)
     * @return 204 No Content
     */
    @PatchMapping("/{agentId}/organization")
    public ResponseEntity<Void> transferOrganization(
            @PathVariable UUID agentId,
            @RequestBody TransferOrganizationRequest request) {

        updateAgentUseCase.transferOrganization(agentId, request.getOrganizationId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 비밀번호 초기화 (관리자용)
     *
     * @param agentId 상담사 ID (UUID)
     * @return 200 OK
     *         - agentId: 상담사 ID
     *         - tempPassword: 새로 생성된 임시 비밀번호 (일회성, 팝업으로 표시 후 재조회 불가)
     *         ※ 초기화 후 passwordMustChange가 true로 설정됨
     */
    @PostMapping("/{agentId}/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@PathVariable UUID agentId) {
        ResetPasswordResult result = resetPasswordUseCase.resetPassword(agentId);

        ResetPasswordResponse response = ResetPasswordResponse.builder()
                .agentId(result.getAgentId())
                .tempPassword(result.getTempPassword())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 상담사 퇴사 처리 (Soft Delete)
     *
     * @param agentId 상담사 ID (UUID)
     * @return 204 No Content
     *         ※ 실제 삭제가 아닌 status를 RETIRED로 변경
     *         ※ retiredAt에 퇴사 일시 기록
     *         ※ 즉시 로그인 차단 및 상담 배정 제외
     */
    @DeleteMapping("/{agentId}")
    public ResponseEntity<Void> retireAgent(@PathVariable UUID agentId) {
        retireAgentUseCase.retireAgent(agentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 상담사 역할 지정
     *
     * @param agentId 상담사 ID (UUID)
     * @param request 요청 본문
     *                - roles: 역할 목록
     *                  - name: 역할 이름
     *                  - type: 역할 유형 (POSITION: 직급, CHANNEL: 채널)
     *         ※ 기존 역할은 모두 대체됨 (PUT semantic)
     * @return 204 No Content
     */
    @PutMapping("/{agentId}/roles")
    public ResponseEntity<Void> assignRoles(
            @PathVariable UUID agentId,
            @RequestBody AssignRolesRequest request) {

        var roles = request.getRoles().stream()
                .map(AssignRolesRequest.RoleDto::toDomain)
                .collect(Collectors.toSet());

        manageRoleUseCase.assignRoles(agentId, roles);
        return ResponseEntity.noContent().build();
    }
}