package com.identitymodulith.user.presentation;

import com.identitymodulith.common.security.context.JwtUserContext;
import com.identitymodulith.common.security.context.TenantContextHolder;
import com.identitymodulith.common.security.context.UnauthorizedException;
import com.identitymodulith.rbac.RbacModuleApi;
import com.identitymodulith.user.application.GetAgentUseCase;
import com.identitymodulith.user.presentation.dto.response.AgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 현재 로그인한 사용자 정보 API
 */
@Slf4j
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Me", description = "현재 로그인한 사용자 정보 API")
public class MeController {

    private final GetAgentUseCase getAgentUseCase;
    private final RbacModuleApi rbacModuleApi;

    /**
     * 현재 로그인한 사용자의 상세 정보 조회
     * - Agent 기본 정보 (이름, 이메일, 소속 부서 등)
     * - 보유 권한 목록
     * - 면허 정보
     */
    @GetMapping
    @Operation(
        summary = "내 정보 조회",
        description = "현재 SAML 로그인된 사용자의 Agent 정보, 역할, 권한 목록을 반환합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "미인증 사용자")
    })
    public ResponseEntity<Map<String, Object>> getMe() {
        String userId = JwtUserContext.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        String tenantId = TenantContextHolder.getCurrentTenantId();

        // Agent 기본 정보
        AgentResponse agentInfo = AgentResponse.from(
            getAgentUseCase.getAgent(UUID.fromString(userId))
        );

        // SecurityContext에서 현재 부여된 권한 목록 추출
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // RBAC에서 역할 목록 추출
        Set<String> roles = rbacModuleApi.getRolesByAgentId(userId).stream()
                .map(RbacModuleApi.RoleInfo::getName)
                .collect(Collectors.toSet());

        Map<String, Object> response = Map.of(
            "tenantId", tenantId,
            "agent", agentInfo,
            "roles", roles,
            "permissions", authorities.stream()
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toSet()),
            "isAuthenticated", true
        );

        log.debug("[Me] userId={}, tenantId={}, roles={}", userId, tenantId, roles);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그인 상태 확인 (경량 엔드포인트)
     * 인증 세션 유효성 체크용으로 사용
     */
    @GetMapping("/status")
    @Operation(
        summary = "로그인 상태 확인",
        description = "현재 세션의 인증 상태와 기본 사용자 정보를 반환합니다. 미인증 시에도 200을 반환합니다."
    )
    public ResponseEntity<Map<String, Object>> getStatus() {
        String userId = JwtUserContext.getCurrentUserId();

        if (userId == null) {
            return ResponseEntity.ok(Map.of(
                "isAuthenticated", false,
                "loginUrl", "/saml2/authenticate/keycloak"
            ));
        }

        String tenantId = TenantContextHolder.getCurrentTenantId();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;

        return ResponseEntity.ok(Map.of(
            "isAuthenticated", true,
            "userId", userId,
            "tenantId", tenantId,
            "username", username != null ? username : ""
        ));
    }
}
