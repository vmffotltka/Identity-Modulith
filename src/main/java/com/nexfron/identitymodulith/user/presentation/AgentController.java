package com.nexfron.identitymodulith.user.presentation;

import com.nexfron.identitymodulith.user.application.*;
import com.nexfron.identitymodulith.user.application.CreateAgentUseCase.CreateAgentCommand;
import com.nexfron.identitymodulith.user.application.CreateAgentUseCase.CreateAgentResult;
import com.nexfron.identitymodulith.user.application.GetAgentUseCase.AgentSearchCriteria;
import com.nexfron.identitymodulith.user.application.ResetPasswordUseCase.ResetPasswordResult;
import com.nexfron.identitymodulith.user.application.UpdateAgentUseCase.UpdateAgentCommand;
import com.nexfron.identitymodulith.user.application.port.RbacPort;
import com.nexfron.identitymodulith.user.presentation.dto.request.*;
import com.nexfron.identitymodulith.user.presentation.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 상담사(Agent) 관리 REST API Controller
 *
 * DDD 원칙을 준수하여 RBAC 모듈과의 직접 의존을 제거하고
 * Port/Adapter 패턴을 통해 간접적으로 연동합니다.
 */
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
    @Operation(
        summary = "상담사 생성",
        description = "새로운 상담사를 생성합니다. 임시 비밀번호가 자동 생성되며 최초 로그인 시 비밀번호 변경이 필요합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "상담사 생성 성공",
            content = @Content(schema = @Schema(implementation = CreateAgentResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 필드 누락, 형식 오류)"),
        @ApiResponse(responseCode = "409", description = "이미 존재하는 로그인 아이디")
    })
    public ResponseEntity<CreateAgentResponse> createAgent(
        @Valid @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "상담사 생성 요청",
            required = true
        ) CreateAgentRequest request) {
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
    @Operation(
        summary = "로그인 아이디 중복 체크",
        description = "상담사 생성 전 로그인 아이디의 사용 가능 여부를 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "중복 체크 성공",
            content = @Content(schema = @Schema(example = "{\"isUnique\": true}"))
        )
    })
    public ResponseEntity<Map<String, Boolean>> checkLoginId(
        @Parameter(description = "검사할 로그인 아이디", required = true, example = "agent001")
        @RequestParam String loginId) {
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
    @Operation(
        summary = "상담사 단건 조회",
        description = "상담사 ID로 상담사 상세 정보를 조회합니다. 비밀번호 정보는 포함되지 않습니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = AgentResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음")
    })
    public ResponseEntity<AgentResponse> getAgent(
        @Parameter(description = "상담사 ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID agentId) {
        var agentInfo = getAgentUseCase.getAgent(agentId);
        return ResponseEntity.ok(AgentResponse.from(agentInfo));
    }

    /**
     * 상담사 목록 조회 (필터링 및 검색 지원)
     *
     * @param tenantId 테넌트 ID
     * @param organizationId 조직 ID로 필터링 (optional)
     * @param status 상태 필터 (ACTIVE, SUSPENDED, RETIRED) (optional)
     * @param nameKeyword 이름 검색 키워드 (부분 일치) (optional)
     * @param loginIdKeyword 로그인 ID 검색 키워드 (부분 일치) (optional)
     * @param includeRetired 퇴사자 포함 여부 (default: false)
     * @return 200 OK - AgentResponse 목록
     *         ※ 기본적으로 ACTIVE 상태만 조회, includeRetired=true 시 퇴사자 포함
     */
    @GetMapping
    @Operation(
        summary = "상담사 목록 조회",
        description = "필터링 및 검색 조건에 따라 상담사 목록을 조회합니다. 기본적으로 활성 상담사만 조회됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = AgentResponse.class))
        )
    })
    public ResponseEntity<List<AgentResponse>> getAgents(
            @Parameter(description = "테넌트 ID", required = true, example = "tenant-001")
            @RequestParam String tenantId,
            @Parameter(description = "조직 ID 필터", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) String organizationId,
            @Parameter(description = "상태 필터 (ACTIVE, SUSPENDED, RETIRED)")
            @RequestParam(required = false) com.nexfron.identitymodulith.user.domain.model.AgentStatus status,
            @Parameter(description = "이름 검색 키워드 (부분 일치)", example = "홍길동")
            @RequestParam(required = false) String nameKeyword,
            @Parameter(description = "로그인 ID 검색 키워드 (부분 일치)", example = "hong")
            @RequestParam(required = false) String loginIdKeyword,
            @Parameter(description = "퇴사자 포함 여부", example = "false")
            @RequestParam(defaultValue = "false") boolean includeRetired) {

        AgentSearchCriteria criteria = AgentSearchCriteria.builder()
                .tenantId(tenantId)
                .organizationId(organizationId)
                .status(status)
                .nameKeyword(nameKeyword)
                .loginIdKeyword(loginIdKeyword)
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
    @Operation(
        summary = "상담사 정보 수정",
        description = "상담사의 기본 정보(이름 등)를 수정합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "수정 성공"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<Void> updateAgent(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Valid @RequestBody UpdateAgentRequest request) {

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
    @Operation(
        summary = "상담사 조직 이동",
        description = "상담사를 다른 조직으로 이동시킵니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "조직 이동 성공"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (존재하지 않는 조직 등)")
    })
    public ResponseEntity<Void> transferOrganization(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Valid @RequestBody TransferOrganizationRequest request) {

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
    @Operation(
        summary = "비밀번호 초기화",
        description = "관리자가 상담사의 비밀번호를 초기화하고 임시 비밀번호를 발급합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "초기화 성공",
            content = @Content(schema = @Schema(implementation = ResetPasswordResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음")
    })
    public ResponseEntity<ResetPasswordResponse> resetPassword(
        @Parameter(description = "상담사 ID", required = true)
        @PathVariable UUID agentId) {
        ResetPasswordResult result = resetPasswordUseCase.resetPassword(agentId);

        ResetPasswordResponse response = ResetPasswordResponse.builder()
                .agentId(result.getAgentId())
                .tempPassword(result.getTempPassword())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 비밀번호 변경 (본인용)
     *
     * @param agentId 상담사 ID (UUID)
     * @param request 요청 본문
     *                - currentPassword: 현재 비밀번호
     *                - newPassword: 새 비밀번호
     * @return 204 No Content
     *         ※ 본인만 변경 가능
     *         ※ 변경 후 passwordMustChange가 false로 설정됨
     */
    @PostMapping("/{agentId}/change-password")
    @Operation(
        summary = "비밀번호 변경",
        description = "상담사가 자신의 비밀번호를 변경합니다. 현재 비밀번호 확인이 필요합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공"),
        @ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치 또는 새 비밀번호 형식 오류"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음")
    })
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Valid @RequestBody ChangePasswordRequest request) {

        String tenantId = com.nexfron.identitymodulith.common.security.TenantContextHolder.getCurrentTenantId();
        String actorId = com.nexfron.identitymodulith.common.security.TenantContextHolder.getCurrentUserId();

        ChangePasswordUseCase.ChangePasswordCommand command = ChangePasswordUseCase.ChangePasswordCommand.builder()
                .tenantId(tenantId)
                .agentId(agentId)
                .actorId(UUID.fromString(actorId))
                .currentPassword(request.getCurrentPassword())
                .newPassword(request.getNewPassword())
                .build();

        changePasswordUseCase.changePassword(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * 비밀번호 변경 (본인용 - /me 경로)
     * API 명세서에 정의된 /me/change-password 엔드포인트
     *
     * @param request 요청 본문
     *                - currentPassword: 현재 비밀번호
     *                - newPassword: 새 비밀번호
     * @return 204 No Content
     *         ※ 현재 로그인한 사용자의 비밀번호만 변경 가능
     *         ※ 변경 후 passwordMustChange가 false로 설정됨
     */
    @PostMapping("/me/change-password")
    @Operation(
        summary = "내 비밀번호 변경",
        description = "현재 로그인한 상담사가 자신의 비밀번호를 변경합니다. 현재 비밀번호 확인이 필요합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공"),
        @ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치 또는 새 비밀번호 형식 오류"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<Void> changeMyPassword(@Valid @RequestBody ChangePasswordRequest request) {
        String tenantId = com.nexfron.identitymodulith.common.security.TenantContextHolder.getCurrentTenantId();
        String actorId = com.nexfron.identitymodulith.common.security.TenantContextHolder.getCurrentUserId();
        UUID agentId = UUID.fromString(actorId);

        ChangePasswordUseCase.ChangePasswordCommand command = ChangePasswordUseCase.ChangePasswordCommand.builder()
                .tenantId(tenantId)
                .agentId(agentId)
                .actorId(agentId)
                .currentPassword(request.getCurrentPassword())
                .newPassword(request.getNewPassword())
                .build();

        changePasswordUseCase.changePassword(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * 상담사 정지 (Suspend)
     *
     * @param agentId 상담사 ID (UUID)
     * @return 204 No Content
     *         ※ ACTIVE 상태만 정지 가능
     *         ※ 정지 시 로그인 차단
     *         ※ activate로 복구 가능
     */
    @PostMapping("/{agentId}/suspend")
    @Operation(
        summary = "상담사 정지",
        description = "상담사를 정지 상태로 변경합니다. 정지된 상담사는 로그인할 수 없습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "정지 성공"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "정지할 수 없는 상태 (이미 정지됨 또는 퇴사함)")
    })
    public ResponseEntity<Void> suspendAgent(
        @Parameter(description = "상담사 ID", required = true)
        @PathVariable UUID agentId) {
        String tenantId = com.nexfron.identitymodulith.common.security.TenantContextHolder.getCurrentTenantId();
        String actorId = com.nexfron.identitymodulith.common.security.TenantContextHolder.getCurrentUserId();

        SuspendAgentUseCase.SuspendAgentCommand command = SuspendAgentUseCase.SuspendAgentCommand.builder()
                .tenantId(tenantId)
                .agentId(agentId)
                .actorId(UUID.fromString(actorId))
                .build();

        suspendAgentUseCase.suspendAgent(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * 상담사 활성화 (Activate)
     *
     * @param agentId 상담사 ID (UUID)
     * @return 204 No Content
     *         ※ SUSPENDED 상태만 활성화 가능
     *         ※ RETIRED는 복구 불가능
     *         ※ 활성화 후 로그인 허용
     */
    @PostMapping("/{agentId}/activate")
    @Operation(
        summary = "상담사 활성화",
        description = "정지된 상담사를 활성화 상태로 복구합니다. 퇴사한 상담사는 활성화할 수 없습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "활성화 성공"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "활성화할 수 없는 상태 (이미 활성화됨 또는 퇴사함)")
    })
    public ResponseEntity<Void> activateAgent(
        @Parameter(description = "상담사 ID", required = true)
        @PathVariable UUID agentId) {
        String tenantId = com.nexfron.identitymodulith.common.security.TenantContextHolder.getCurrentTenantId();
        String actorId = com.nexfron.identitymodulith.common.security.TenantContextHolder.getCurrentUserId();

        ActivateAgentUseCase.ActivateAgentCommand command = ActivateAgentUseCase.ActivateAgentCommand.builder()
                .tenantId(tenantId)
                .agentId(agentId)
                .actorId(UUID.fromString(actorId))
                .build();

        activateAgentUseCase.activateAgent(command);
        return ResponseEntity.noContent().build();
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
    @Operation(
        summary = "상담사 퇴사 처리",
        description = "상담사를 퇴사 처리합니다. 실제 데이터는 삭제되지 않으며 상태만 변경됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "퇴사 처리 성공"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "이미 퇴사 처리됨")
    })
    public ResponseEntity<Void> retireAgent(
        @Parameter(description = "상담사 ID", required = true)
        @PathVariable UUID agentId) {
        String tenantId = com.nexfron.identitymodulith.common.security.TenantContextHolder.getCurrentTenantId();
        String actorId = com.nexfron.identitymodulith.common.security.TenantContextHolder.getCurrentUserId();

        RetireAgentUseCase.RetireAgentCommand command = RetireAgentUseCase.RetireAgentCommand.builder()
                .tenantId(tenantId)
                .agentId(agentId)
                .actorId(UUID.fromString(actorId))
                .deletePolicy(RetireAgentUseCase.RetireDeletePolicy.PRESERVE)
                .build();

        retireAgentUseCase.retireAgent(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * 상담사 부서 이동 (Transfer)
     *
     * @param agentId 상담사 ID (UUID)
     * @param request 요청 본문
     *                - newOrganizationId: 새 조직(부서) ID
     * @return 200 OK
     *         - agentId: 상담사 ID
     *         - fromOrganizationId: 이전 조직 ID
     *         - toOrganizationId: 새 조직 ID
     *         - transferredAt: 이동 일시
     *         ※ 동일 부서로 이동 불가
     *         ※ RETIRED 상담사 이동 불가
     *         ※ 대상 부서는 ACTIVE 상태여야 함
     */
    @PostMapping("/{agentId}/transfer")
    @Operation(
        summary = "상담사 부서 이동",
        description = "상담사를 다른 부서로 이동시킵니다. 이동 이력이 기록됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "부서 이동 성공",
            content = @Content(schema = @Schema(implementation = TransferAgentResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (동일 부서, 퇴사한 상담사 등)"),
        @ApiResponse(responseCode = "404", description = "상담사 또는 대상 부서를 찾을 수 없음")
    })
    public ResponseEntity<TransferAgentResponse> transferAgent(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Valid @RequestBody TransferAgentRequest request) {

        String tenantId = com.nexfron.identitymodulith.common.security.TenantContextHolder.getCurrentTenantId();
        String actorId = com.nexfron.identitymodulith.common.security.TenantContextHolder.getCurrentUserId();

        TransferAgentUseCase.TransferAgentCommand command = TransferAgentUseCase.TransferAgentCommand.builder()
                .tenantId(tenantId)
                .agentId(agentId)
                .newOrganizationId(request.getNewOrganizationId())
                .actorId(UUID.fromString(actorId))
                .build();

        TransferAgentUseCase.TransferAgentResult result = transferAgentUseCase.transferAgent(command);

        TransferAgentResponse response = TransferAgentResponse.builder()
                .agentId(result.getAgentId())
                .fromOrganizationId(result.getFromOrganizationId())
                .toOrganizationId(result.getToOrganizationId())
                .transferredAt(result.getTransferredAt())
                .build();

        return ResponseEntity.ok(response);
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
    @Operation(
        summary = "상담사 역할 일괄 지정",
        description = "상담사의 역할을 일괄 지정합니다. 기존 역할은 모두 제거되고 새로운 역할로 대체됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "역할 지정 성공"),
        @ApiResponse(responseCode = "404", description = "상담사를 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 역할 정보")
    })
    public ResponseEntity<Void> assignRoles(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Valid @RequestBody AssignRolesRequest request) {

        var roles = request.getRoles().stream()
                .map(AssignRolesRequest.RoleDto::toDomain)
                .collect(Collectors.toSet());

        manageRoleUseCase.assignRoles(agentId, roles);
        return ResponseEntity.noContent().build();
    }

    /**
     * 상담사에게 특정 역할 추가
     * API 명세서에 정의된 개별 역할 할당 엔드포인트
     *
     * @param agentId 상담사 ID (UUID)
     * @param roleName 추가할 역할 이름
     * @return 201 Created
     *         ※ 기존 역할은 유지되며 새 역할만 추가됨
     *         ※ 이미 할당된 역할인 경우 무시됨 (멱등성)
     *
     * @apiNote Port/Adapter 패턴을 통해 RBAC 모듈과 연동
     *          직접 RBAC API(/api/rbac/agents/{id}/roles/{name}) 사용도 가능합니다.
     */
    @PostMapping("/{agentId}/roles/{roleName}")
    @Operation(
        summary = "상담사에게 역할 추가",
        description = "상담사에게 특정 역할을 추가합니다. 기존 역할은 유지됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "역할 추가 성공 (이미 할당된 경우에도 201 반환)"),
        @ApiResponse(responseCode = "404", description = "상담사 또는 역할을 찾을 수 없음")
    })
    public ResponseEntity<Void> addRole(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Parameter(description = "추가할 역할 이름", required = true, example = "SENIOR_AGENT")
            @PathVariable String roleName) {
        rbacPort.assignRoleToAgent(agentId.toString(), roleName);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 상담사에게서 특정 역할 제거
     * API 명세서에 정의된 개별 역할 제거 엔드포인트
     *
     * @param agentId 상담사 ID (UUID)
     * @param roleName 제거할 역할 이름
     * @return 204 No Content
     *         ※ 다른 역할은 유지되며 지정된 역할만 제거됨
     *         ※ 할당되지 않은 역할인 경우 404 에러 반환
     *
     * @apiNote Port/Adapter 패턴을 통해 RBAC 모듈과 연동
     *          직접 RBAC API(/api/rbac/agents/{id}/roles/{name}) 사용도 가능합니다.
     */
    @DeleteMapping("/{agentId}/roles/{roleName}")
    @Operation(
        summary = "상담사에게서 역할 제거",
        description = "상담사에게서 특정 역할을 제거합니다. 다른 역할은 유지됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "역할 제거 성공"),
        @ApiResponse(responseCode = "404", description = "상담사, 역할을 찾을 수 없거나 해당 역할이 할당되지 않음")
    })
    public ResponseEntity<Void> removeRole(
            @Parameter(description = "상담사 ID", required = true)
            @PathVariable UUID agentId,
            @Parameter(description = "제거할 역할 이름", required = true, example = "SENIOR_AGENT")
            @PathVariable String roleName) {
        rbacPort.revokeRoleFromAgent(agentId.toString(), roleName);
        return ResponseEntity.noContent().build();
    }

    /**
     * 테넌트별 상담사 통계 조회
     * AG-021: 대시보드용 실시간 통계
     *
     * @param tenantId 테넌트 ID
     * @return 200 OK
     *         - totalCount: 전체 상담사 수
     *         - activeCount: 활성 상담사 수
     *         - suspendedCount: 정지된 상담사 수
     *         - retiredCount: 퇴사 상담사 수
     *         - passwordChangeRequired: 비밀번호 변경 필요 상담사 수
     *         - byOrganization: 조직별 상담사 수 맵
     *         - byStatus: 상태별 상담사 수 맵
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "상담사 통계 조회",
        description = "대시보드용 실시간 상담사 통계를 제공합니다. 전체/활성/정지/퇴사 수 및 조직별 통계를 포함합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "통계 조회 성공",
            content = @Content(schema = @Schema(implementation = GetAgentStatisticsUseCase.AgentStatistics.class))
        )
    })
    public ResponseEntity<GetAgentStatisticsUseCase.AgentStatistics> getStatistics(
            @Parameter(description = "테넌트 ID", required = true, example = "tenant-001")
            @RequestParam String tenantId) {

        GetAgentStatisticsUseCase.AgentStatistics statistics =
            getAgentStatisticsUseCase.getStatistics(tenantId);

        return ResponseEntity.ok(statistics);
    }

    /**
     * 조직별 상담사 통계 조회
     * AG-021: 조직별 통계 제공
     *
     * @param tenantId 테넌트 ID
     * @param organizationId 조직 ID
     * @return 200 OK - 해당 조직의 상담사 통계
     */
    @GetMapping("/statistics/organization/{organizationId}")
    @Operation(
        summary = "조직별 상담사 통계 조회",
        description = "특정 조직(부서)의 상담사 통계를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "통계 조회 성공",
            content = @Content(schema = @Schema(implementation = GetAgentStatisticsUseCase.AgentStatistics.class))
        ),
        @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음")
    })
    public ResponseEntity<GetAgentStatisticsUseCase.AgentStatistics> getStatisticsByOrganization(
            @Parameter(description = "테넌트 ID", required = true, example = "tenant-001")
            @RequestParam String tenantId,
            @Parameter(description = "조직 ID", required = true, example = "550e8400-e29b-41d4-a716-446655440001")
            @PathVariable String organizationId) {

        GetAgentStatisticsUseCase.AgentStatistics statistics =
            getAgentStatisticsUseCase.getStatisticsByOrganization(tenantId, organizationId);

        return ResponseEntity.ok(statistics);
    }
}
