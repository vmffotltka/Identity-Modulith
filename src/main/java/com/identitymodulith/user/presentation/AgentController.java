package com.identitymodulith.user.presentation;

import com.identitymodulith.common.security.context.JwtUserContext;
import com.identitymodulith.common.security.context.TenantContextHolder;
import com.identitymodulith.common.security.context.UnauthorizedException;
import com.identitymodulith.user.application.*;
import com.identitymodulith.user.domain.exception.BusinessException;
import com.identitymodulith.user.domain.exception.ErrorCode;
import com.identitymodulith.user.domain.model.AgentStatus;
import com.identitymodulith.user.presentation.dto.request.*;
import com.identitymodulith.user.presentation.dto.response.AgentResponse;
import com.identitymodulith.user.presentation.dto.response.CreateAgentResponse;
import com.identitymodulith.user.presentation.dto.response.ResetPasswordResponse;
import com.identitymodulith.user.presentation.dto.response.TransferAgentResponse;
import com.identitymodulith.user.application.CreateAgentUseCase.CreateAgentCommand;
import com.identitymodulith.user.application.CreateAgentUseCase.CreateAgentResult;
import com.identitymodulith.user.application.GetAgentUseCase.AgentSearchCriteria;
import com.identitymodulith.user.application.ResetPasswordUseCase.ResetPasswordResult;
import com.identitymodulith.user.application.UpdateAgentUseCase.UpdateAgentCommand;
import com.identitymodulith.user.application.port.RbacPort;
import com.identitymodulith.user.domain.model.Agent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 상담사(Agent) 관리 REST API Controller
 *
 * DDD 원칙을 준수하여 RBAC 모듈과의 직접 의존을 제거하고
 * Port/Adapter 패턴을 통해 간접적으로 연동합니다.
 *
 * 인증 방식: SAML 2.0 (Keycloak) - SecurityContext에서 자동 추출
 * - tenantId: TenantContextHolder.getCurrentTenantId()
 * - userId:   JwtUserContext.getCurrentUserId()
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agent Management", description = "상담사 관리 API - 상담사 생성, 조회, 수정, 조직 이동, 역할 관리, 상태 관리 등을 제공합니다.")
public class AgentController {

    private final CreateAgentUseCase createAgentUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final UpdateAgentUseCase updateAgentUseCase;
    private final RetireAgentUseCase retireAgentUseCase;
    private final GetAgentUseCase getAgentUseCase;
    private final GetAgentStatisticsUseCase getAgentStatisticsUseCase;
    private final ManageRoleUseCase manageRoleUseCase;
    private final CheckLoginIdUseCase checkLoginIdUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final SuspendAgentUseCase suspendAgentUseCase;
    private final ActivateAgentUseCase activateAgentUseCase;
    private final TransferAgentUseCase transferAgentUseCase;
    private final RbacPort rbacPort;  // Port 인터페이스 사용 (DDD 원칙)

    // ─── SecurityContext 헬퍼 ─────────────────────────────────────────────────

    /** SAML 인증 사용자의 tenantId 추출 */
    private String currentTenantId() {
        return TenantContextHolder.getCurrentTenantId();
    }

    /** SAML 인증 사용자의 UUID 추출 */
    private UUID currentUserId() {
        String userId = JwtUserContext.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("인증 정보가 없습니다. SAML 로그인이 필요합니다.");
        }
        return UUID.fromString(userId);
    }

    // ─── 상담사 생성 ──────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
        summary = "상담사 생성",
        description = "새로운 상담사를 생성합니다. 임시 비밀번호가 자동 생성되며 최초 로그인 시 비밀번호 변경이 필요합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "상담사 생성 성공",
            content = @Content(schema = @Schema(implementation = CreateAgentResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 필드 누락, 형식 오류)"),
        @ApiResponse(responseCode = "409", description = "이미 존재하는 로그인 아이디")
    })
    public ResponseEntity<CreateAgentResponse> createAgent(
        @Valid @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "상담사 생성 요청", required = true
        ) CreateAgentRequest request) {

        Set<Agent.Role> roles = request.getRoles().stream()
                .map(roleName -> new Agent.Role(roleName, Agent.Role.RoleType.POSITION))
                .collect(Collectors.toSet());

        CreateAgentCommand command = CreateAgentCommand.builder()
                .tenantId(currentTenantId())   // ← SecurityContext에서 자동 추출
                .loginId(request.getLoginId())
                .name(request.getName())
                .organizationId(request.getOrganizationId())
                .roles(roles)
                .email(request.getEmail())
                .phone(request.getPhone())
                .employeeId(request.getEmployeeId())
                .build();

        CreateAgentResult result = createAgentUseCase.createAgent(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(
            CreateAgentResponse.builder()
                .agentId(result.getAgentId())
                .loginId(result.getLoginId())
                .tempPassword(result.getTempPassword())
                .build());
    }

    // ─── 아이디 중복 체크 ─────────────────────────────────────────────────────

    @GetMapping("/check-login-id")
    @Operation(summary = "로그인 아이디 중복 체크",
        description = "상담사 생성 전 로그인 아이디의 사용 가능 여부를 확인합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "중복 체크 성공",
            content = @Content(schema = @Schema(example = "{\"isUnique\": true}")))
    })
    public ResponseEntity<Map<String, Boolean>> checkLoginId(
        @Parameter(description = "검사할 로그인 아이디", required = true, example = "agent001")
        @RequestParam String loginId) {
        return ResponseEntity.ok(Map.of("isUnique", checkLoginIdUseCase.isLoginIdUnique(loginId)));
    }

    // ─── 상담사 단건 조회 ─────────────────────────────────────────────────────

    @GetMapping("/{agentId}")
    @Operation(summary = "상담사 단건 조회",
        description = "상담사 ID로 상담사 상세 정보를 조회합니다. 비밀번호 정보는 포함되지 않습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = AgentResponse.class))),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음")
    })
    public ResponseEntity<AgentResponse> getAgent(
        @Parameter(description = "상담사 ID", required = true)
        @PathVariable UUID agentId) {
        return ResponseEntity.ok(AgentResponse.from(getAgentUseCase.getAgent(agentId)));
    }

    // ─── 상담사 목록 조회 ─────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "상담사 목록 조회",
        description = "필터링 및 검색 조건에 따라 상담사 목록을 조회합니다. tenantId는 인증 정보에서 자동 추출됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = AgentResponse.class)))
    })
    public ResponseEntity<List<AgentResponse>> getAgents(
            @Parameter(description = "조직 ID 필터", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) String organizationId,
            @Parameter(description = "상태 필터 (ACTIVE, SUSPENDED, RETIRED)")
            @RequestParam(required = false) AgentStatus status,
            @Parameter(description = "이름 검색 키워드 (부분 일치)", example = "홍길동")
            @RequestParam(required = false) String nameKeyword,
            @Parameter(description = "로그인 ID 검색 키워드 (부분 일치)", example = "hong")
            @RequestParam(required = false) String loginIdKeyword,
            @Parameter(description = "퇴사자 포함 여부", example = "false")
            @RequestParam(defaultValue = "false") boolean includeRetired) {

        AgentSearchCriteria criteria = AgentSearchCriteria.builder()
                .tenantId(currentTenantId())   // ← SecurityContext에서 자동 추출
                .organizationId(organizationId)
                .status(status)
                .nameKeyword(nameKeyword)
                .loginIdKeyword(loginIdKeyword)
                .includeRetired(includeRetired)
                .build();

        return ResponseEntity.ok(getAgentUseCase.getAgents(criteria).stream()
                .map(AgentResponse::from).toList());
    }

    // ─── 상담사 정보 수정 ─────────────────────────────────────────────────────

    @PatchMapping("/{agentId}")
    @Operation(summary = "상담사 정보 수정",
        description = "상담사의 기본 정보(이름 등)를 수정합니다. 본인 또는 ADMIN만 수정 가능.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "수정 성공"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 권한 없음")
    })
    public ResponseEntity<Void> updateAgent(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Valid @RequestBody UpdateAgentRequest request) {

        UpdateAgentCommand command = UpdateAgentCommand.builder()
                .tenantId(currentTenantId())
                .agentId(agentId)
                .actorId(currentUserId())      // ← SecurityContext에서 자동 추출
                .name(request.getName())
                .build();

        updateAgentUseCase.updateAgent(command);
        return ResponseEntity.noContent().build();
    }

    // ─── 상담사 조직 이동 ─────────────────────────────────────────────────────

    @PatchMapping("/{agentId}/organization")
    @Operation(summary = "상담사 조직 이동",
        description = "상담사를 다른 조직으로 이동시킵니다. ADMIN 권한 필요.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "조직 이동 성공"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 권한 없음")
    })
    public ResponseEntity<Void> transferOrganization(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Valid @RequestBody TransferOrganizationRequest request) {

        updateAgentUseCase.transferOrganization(currentTenantId(), agentId, currentUserId(), request.getOrganizationId());
        return ResponseEntity.noContent().build();
    }

    // ─── 비밀번호 초기화 (관리자용) ───────────────────────────────────────────

    @PostMapping("/{agentId}/reset-password")
    @Operation(summary = "비밀번호 초기화",
        description = "관리자가 상담사의 비밀번호를 초기화하고 임시 비밀번호를 발급합니다. ADMIN 권한 필요.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "초기화 성공",
            content = @Content(schema = @Schema(implementation = ResetPasswordResponse.class))),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "권한 없음")
    })
    public ResponseEntity<ResetPasswordResponse> resetPassword(
        @Parameter(description = "상담사 ID", required = true)
        @PathVariable UUID agentId) {

        ResetPasswordResult result = resetPasswordUseCase.resetPassword(currentTenantId(), agentId, currentUserId());

        return ResponseEntity.ok(ResetPasswordResponse.builder()
                .agentId(result.getAgentId())
                .tempPassword(result.getTempPassword())
                .build());
    }

    // ─── 비밀번호 변경 (본인용) ───────────────────────────────────────────────

    @PostMapping("/{agentId}/change-password")
    @Operation(summary = "비밀번호 변경",
        description = "상담사가 자신의 비밀번호를 변경합니다. 현재 비밀번호 확인이 필요합니다. 본인만 가능.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공"),
        @ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치 또는 새 비밀번호 형식 오류"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음")
    })
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Valid @RequestBody ChangePasswordRequest request) {

        log.info("[Controller] 비밀번호 변경 요청 - agentId={}", agentId);

        if (!request.isPasswordMatching()) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH,
                    "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        ChangePasswordUseCase.ChangePasswordCommand command = ChangePasswordUseCase.ChangePasswordCommand.builder()
                .tenantId(currentTenantId())
                .agentId(agentId)
                .actorId(currentUserId())
                .currentPassword(request.getCurrentPassword())
                .newPassword(request.getNewPassword())
                .build();

        changePasswordUseCase.changePassword(command);
        return ResponseEntity.noContent().build();
    }

    // ─── 내 비밀번호 변경 (/me) ───────────────────────────────────────────────

    @PostMapping("/me/change-password")
    @Operation(summary = "내 비밀번호 변경",
        description = "현재 로그인한 상담사가 자신의 비밀번호를 변경합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공"),
        @ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치 또는 새 비밀번호 형식 오류"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<Void> changeMyPassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        if (!request.isPasswordMatching()) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH,
                    "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        UUID agentId = currentUserId();

        ChangePasswordUseCase.ChangePasswordCommand command = ChangePasswordUseCase.ChangePasswordCommand.builder()
                .tenantId(currentTenantId())
                .agentId(agentId)
                .actorId(agentId)
                .currentPassword(request.getCurrentPassword())
                .newPassword(request.getNewPassword())
                .build();

        changePasswordUseCase.changePassword(command);
        return ResponseEntity.noContent().build();
    }

    // ─── 상담사 정지 ──────────────────────────────────────────────────────────

    @PostMapping("/{agentId}/suspend")
    @Operation(summary = "상담사 정지",
        description = "상담사를 정지 상태로 변경합니다. ADMIN 권한 필요.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "정지 성공"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "정지할 수 없는 상태 또는 권한 없음")
    })
    public ResponseEntity<Void> suspendAgent(
        @Parameter(description = "상담사 ID", required = true)
        @PathVariable UUID agentId) {

        suspendAgentUseCase.suspendAgent(SuspendAgentUseCase.SuspendAgentCommand.builder()
                .tenantId(currentTenantId())
                .agentId(agentId)
                .actorId(currentUserId())
                .build());
        return ResponseEntity.noContent().build();
    }

    // ─── 상담사 활성화 ────────────────────────────────────────────────────────

    @PostMapping("/{agentId}/activate")
    @Operation(summary = "상담사 활성화",
        description = "정지된 상담사를 활성화 상태로 복구합니다. ADMIN 권한 필요.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "활성화 성공"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "활성화할 수 없는 상태 또는 권한 없음")
    })
    public ResponseEntity<Void> activateAgent(
        @Parameter(description = "상담사 ID", required = true)
        @PathVariable UUID agentId) {

        activateAgentUseCase.activateAgent(ActivateAgentUseCase.ActivateAgentCommand.builder()
                .tenantId(currentTenantId())
                .agentId(agentId)
                .actorId(currentUserId())
                .build());
        return ResponseEntity.noContent().build();
    }

    // ─── 상담사 퇴사 처리 ─────────────────────────────────────────────────────

    @DeleteMapping("/{agentId}")
    @Operation(summary = "상담사 퇴사 처리",
        description = "상담사를 퇴사 처리합니다. 실제 데이터는 삭제되지 않으며 상태만 변경됩니다. ADMIN 권한 필요.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "퇴사 처리 성공"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "이미 퇴사 처리됨 또는 권한 없음")
    })
    public ResponseEntity<Void> retireAgent(
        @Parameter(description = "상담사 ID", required = true)
        @PathVariable UUID agentId) {

        retireAgentUseCase.retireAgent(RetireAgentUseCase.RetireAgentCommand.builder()
                .tenantId(currentTenantId())
                .agentId(agentId)
                .actorId(currentUserId())
                .deletePolicy(RetireAgentUseCase.RetireDeletePolicy.PRESERVE)
                .build());
        return ResponseEntity.noContent().build();
    }

    // ─── 상담사 부서 이동 ─────────────────────────────────────────────────────

    @PostMapping("/{agentId}/transfer")
    @Operation(summary = "상담사 부서 이동",
        description = "상담사를 다른 부서로 이동시킵니다. ADMIN 권한 필요.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "부서 이동 성공",
            content = @Content(schema = @Schema(implementation = TransferAgentResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 권한 없음"),
        @ApiResponse(responseCode = "404", description = "상담사 또는 대상 부서를 찾을 수 없음")
    })
    public ResponseEntity<TransferAgentResponse> transferAgent(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Valid @RequestBody TransferAgentRequest request) {

        TransferAgentUseCase.TransferAgentResult result = transferAgentUseCase.transferAgent(
            TransferAgentUseCase.TransferAgentCommand.builder()
                .tenantId(currentTenantId())
                .agentId(agentId)
                .newOrganizationId(request.getNewOrganizationId())
                .actorId(currentUserId())
                .build());

        return ResponseEntity.ok(TransferAgentResponse.builder()
                .agentId(result.getAgentId())
                .fromOrganizationId(result.getFromOrganizationId())
                .toOrganizationId(result.getToOrganizationId())
                .transferredAt(result.getTransferredAt())
                .build());
    }

    // ─── 역할 일괄 지정 ───────────────────────────────────────────────────────

    @PutMapping("/{agentId}/roles")
    @Operation(summary = "상담사 역할 일괄 지정",
        description = "상담사의 역할을 일괄 지정합니다. 기존 역할은 모두 제거되고 새로운 역할로 대체됩니다. ADMIN 권한 필요.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "역할 지정 성공"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 역할 정보 또는 권한 없음")
    })
    public ResponseEntity<Void> assignRoles(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Valid @RequestBody AssignRolesRequest request) {

        log.info("[Controller] 역할 일괄 지정 요청 - agentId={}", agentId);

        if (request.getAgentId() == null) {
            request.setAgentId(agentId);
        }

        if (!request.hasValidRoles()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "roles, roleIds, roleNames 중 하나는 필수입니다.");
        }

        String tenantId = currentTenantId();
        manageRoleUseCase.validateAdminPermission(tenantId, currentUserId());

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            manageRoleUseCase.assignRolesByIds(agentId, request.getRoleIds());
        } else if (request.getRoleNames() != null && !request.getRoleNames().isEmpty()) {
            manageRoleUseCase.assignRolesByNames(agentId, request.getRoleNames());
        } else {
            var roles = request.getRoles().stream()
                    .map(AssignRolesRequest.RoleDto::toDomain)
                    .collect(Collectors.toSet());
            manageRoleUseCase.assignRoles(agentId, roles);
        }

        return ResponseEntity.noContent().build();
    }

    // ─── 역할 개별 추가 ───────────────────────────────────────────────────────

    @PostMapping("/{agentId}/roles/{roleName}")
    @Operation(summary = "상담사에게 역할 추가",
        description = "상담사에게 특정 역할을 추가합니다. 기존 역할은 유지됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "역할 추가 성공"),
        @ApiResponse(responseCode = "404", description = "상담사 또는 역할을 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "권한 없음")
    })
    public ResponseEntity<Void> addRole(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Parameter(description = "추가할 역할 이름", required = true, example = "SENIOR_AGENT")
            @PathVariable String roleName) {

        manageRoleUseCase.validateAdminPermission(currentTenantId(), currentUserId());
        rbacPort.assignRoleToAgent(agentId.toString(), roleName);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ─── 역할 개별 제거 ───────────────────────────────────────────────────────

    @DeleteMapping("/{agentId}/roles/{roleName}")
    @Operation(summary = "상담사에게서 역할 제거",
        description = "상담사에게서 특정 역할을 제거합니다. 다른 역할은 유지됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "역할 제거 성공"),
        @ApiResponse(responseCode = "404", description = "상담사, 역할을 찾을 수 없거나 해당 역할이 할당되지 않음"),
        @ApiResponse(responseCode = "400", description = "권한 없음")
    })
    public ResponseEntity<Void> removeRole(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Parameter(description = "제거할 역할 이름", required = true, example = "SENIOR_AGENT")
            @PathVariable String roleName) {

        manageRoleUseCase.validateAdminPermission(currentTenantId(), currentUserId());
        rbacPort.revokeRoleFromAgent(agentId.toString(), roleName);
        return ResponseEntity.noContent().build();
    }

    // ─── 통계 조회 ────────────────────────────────────────────────────────────

    @GetMapping("/statistics")
    @Operation(summary = "상담사 통계 조회",
        description = "대시보드용 실시간 상담사 통계를 제공합니다. tenantId는 인증 정보에서 자동 추출됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "통계 조회 성공",
            content = @Content(schema = @Schema(implementation = GetAgentStatisticsUseCase.AgentStatistics.class)))
    })
    public ResponseEntity<GetAgentStatisticsUseCase.AgentStatistics> getStatistics() {
        return ResponseEntity.ok(getAgentStatisticsUseCase.getStatistics(currentTenantId()));
    }

    @GetMapping("/statistics/organization/{organizationId}")
    @Operation(summary = "조직별 상담사 통계 조회",
        description = "특정 조직(부서)의 상담사 통계를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "통계 조회 성공",
            content = @Content(schema = @Schema(implementation = GetAgentStatisticsUseCase.AgentStatistics.class))),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<GetAgentStatisticsUseCase.AgentStatistics> getStatisticsByOrganization(
            @Parameter(description = "조직 ID", required = true)
            @PathVariable String organizationId) {
        return ResponseEntity.ok(
            getAgentStatisticsUseCase.getStatisticsByOrganization(currentTenantId(), organizationId));
    }
}
