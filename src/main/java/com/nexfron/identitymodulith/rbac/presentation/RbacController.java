package com.nexfron.identitymodulith.rbac.presentation;

import com.nexfron.identitymodulith.rbac.application.RbacManagementService;
import com.nexfron.identitymodulith.rbac.application.RbacManagementService.*;
import com.nexfron.identitymodulith.rbac.application.RbacQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * RbacController - 역할기반 접근제어(RBAC) 관리 REST API
 *
 * <h2>기능:</h2>
 * <ul>
 *   <li><b>역할 관리</b>: 역할 생성, 조회, 삭제</li>
 *   <li><b>권한 관리</b>: 권한 생성, 조회, 삭제</li>
 *   <li><b>역할-권한 할당</b>: 역할에 권한 할당/해제</li>
 *   <li><b>권한 조회</b>: 사용자/역할의 권한 조회</li>
 * </ul>
 *
 * <h2>에러 응답:</h2>
 * <p>
 * 모든 에러는 {@link RbacExceptionHandler}에서 일관된 형식으로 변환됨:
 * </p>
 * <pre>
 * {
 *   "code": "ERROR_CODE",
 *   "message": "상세 메시지"
 * }
 * </pre>
 *
 * <h2>HTTP 상태 코드:</h2>
 * <ul>
 *   <li><b>201 Created</b>: 역할/권한 생성 성공</li>
 *   <li><b>200 OK</b>: 조회 또는 업데이트 성공</li>
 *   <li><b>204 No Content</b>: 삭제 성공</li>
 *   <li><b>400 Bad Request</b>: 잘못된 요청 또는 이미 존재하는 항목</li>
 *   <li><b>404 Not Found</b>: 항목을 찾을 수 없음</li>
 *   <li><b>409 Conflict</b>: 충돌 (이미 할당됨 등)</li>
 * </ul>
 *
 * @author Identity System Team
 * @version 1.0
 */
@Tag(
        name = "RBAC Management",
        description = "역할기반 접근제어 관리 API"
)
@RestController
@RequestMapping("/api/rbac")
@RequiredArgsConstructor
public class RbacController {

    private final RbacManagementService rbacManagementService;
    private final RbacQueryService rbacQueryService;

    // ============================================================
    // 역할(Role) 관리 엔드포인트
    // ============================================================

    @Operation(
            summary = "모든 역할 조회",
            description = "시스템에 정의된 모든 역할을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = RoleDto.class))
    )
    @GetMapping("/roles")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        return ResponseEntity.ok(rbacManagementService.getAllRoles());
    }

    @Operation(
            summary = "특정 역할 조회",
            description = "역할명으로 특정 역할의 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RoleDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "역할을 찾을 수 없음"
            )
    })
    @GetMapping("/roles/{roleName}")
    public ResponseEntity<RoleDto> getRoleByName(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName) {
        return ResponseEntity.ok(rbacManagementService.getRoleByName(roleName));
    }

    @Operation(
            summary = "역할 생성",
            description = "새로운 역할을 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "역할 생성 성공",
                    content = @Content(schema = @Schema(implementation = RoleDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "이미 존재하는 역할 또는 잘못된 요청"
            )
    })
    @PostMapping("/roles")
    public ResponseEntity<RoleDto> createRole(
            @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rbacManagementService.createRole(request));
    }

    @Operation(
            summary = "역할 삭제",
            description = "역할을 삭제합니다. 해당 역할이 할당된 권한들도 함께 삭제됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "역할 삭제 성공"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "역할을 찾을 수 없음"
            )
    })
    @DeleteMapping("/roles/{roleName}")
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName) {
        rbacManagementService.deleteRole(roleName);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // 권한(Permission) 관리 엔드포인트
    // ============================================================

    @Operation(
            summary = "모든 권한 조회",
            description = "시스템에 정의된 모든 권한을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = PermissionDto.class))
    )
    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        return ResponseEntity.ok(rbacManagementService.getAllPermissions());
    }

    @Operation(
            summary = "특정 권한 조회",
            description = "권한 코드로 특정 권한의 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PermissionDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "권한을 찾을 수 없음"
            )
    })
    @GetMapping("/permissions/{code}")
    public ResponseEntity<PermissionDto> getPermissionByCode(
            @Parameter(description = "권한 코드", example = "user:manage", required = true)
            @PathVariable String code) {
        return ResponseEntity.ok(rbacManagementService.getPermissionByCode(code));
    }

    @Operation(
            summary = "권한 생성",
            description = "새로운 권한을 생성합니다. (예: 'user:manage', 'org:view')"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "권한 생성 성공",
                    content = @Content(schema = @Schema(implementation = PermissionDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "이미 존재하는 권한 또는 잘못된 요청"
            )
    })
    @PostMapping("/permissions")
    public ResponseEntity<PermissionDto> createPermission(
            @RequestBody CreatePermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rbacManagementService.createPermission(request));
    }

    @Operation(
            summary = "권한 삭제",
            description = "권한을 삭제합니다. 해당 권한이 할당된 모든 역할에서도 제거됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "권한 삭제 성공"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "권한을 찾을 수 없음"
            )
    })
    @DeleteMapping("/permissions/{code}")
    public ResponseEntity<Void> deletePermission(
            @Parameter(description = "권한 코드", example = "user:manage", required = true)
            @PathVariable String code) {
        rbacManagementService.deletePermission(code);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // 역할-권한 할당 엔드포인트
    // ============================================================

    @Operation(
            summary = "역할의 권한 조회",
            description = "특정 역할에 할당된 모든 권한을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PermissionDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "역할을 찾을 수 없음"
            )
    })
    @GetMapping("/roles/{roleName}/permissions")
    public ResponseEntity<Set<PermissionDto>> getPermissionsByRole(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName) {
        return ResponseEntity.ok(rbacManagementService.getPermissionsByRole(roleName));
    }

    @Operation(
            summary = "역할에 권한 할당",
            description = "특정 역할에 권한을 할당합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "권한 할당 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "이미 할당된 권한"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "역할 또는 권한을 찾을 수 없음"
            )
    })
    @PostMapping("/roles/{roleName}/permissions/{permissionCode}")
    public ResponseEntity<Void> assignPermissionToRole(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName,
            @Parameter(description = "권한 코드", example = "user:manage", required = true)
            @PathVariable String permissionCode) {
        rbacManagementService.assignPermissionToRole(roleName, permissionCode);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(
            summary = "역할에서 권한 제거",
            description = "특정 역할에서 권한을 제거합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "권한 제거 성공"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "역할 또는 권한 할당을 찾을 수 없음"
            )
    })
    @DeleteMapping("/roles/{roleName}/permissions/{permissionCode}")
    public ResponseEntity<Void> revokePermissionFromRole(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName,
            @Parameter(description = "권한 코드", example = "user:manage", required = true)
            @PathVariable String permissionCode) {
        rbacManagementService.revokePermissionFromRole(roleName, permissionCode);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // 권한 조회 엔드포인트 (사용자별)
    // ============================================================

    @Operation(
            summary = "사용자의 권한 목록 조회",
            description = "특정 사용자가 가진 모든 권한을 조회합니다. " +
                    "사용자의 역할들로부터 권한이 합산됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (권한 코드 Set)",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "X-Tenant-ID 헤더 누락"
            )
    })
    @GetMapping("/agents/{agentId}/permissions")
    public ResponseEntity<Set<String>> getAgentPermissions(
            @Parameter(description = "에이전트 ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true)
            @PathVariable UUID agentId,
            @Parameter(description = "테넌트 ID", example = "tenant-001", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        Set<String> permissions = rbacQueryService.permissionsOf(tenantId, agentId);
        return ResponseEntity.ok(permissions);
    }

    @Operation(
            summary = "사용자의 특정 권한 확인",
            description = "사용자가 특정 권한을 가지고 있는지 확인합니다. " +
                    "true/false 로 응답합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (true/false)",
                    content = @Content(schema = @Schema(type = "boolean"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "X-Tenant-ID 헤더 누락"
            )
    })
    @GetMapping("/agents/{agentId}/permissions/{permissionCode}")
    public ResponseEntity<Boolean> hasPermission(
            @Parameter(description = "에이전트 ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", required = true)
            @PathVariable UUID agentId,
            @Parameter(description = "권한 코드", example = "user:manage", required = true)
            @PathVariable String permissionCode,
            @Parameter(description = "테넌트 ID", example = "tenant-001", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId) {
        boolean hasPermission = rbacQueryService.permissionsOf(tenantId, agentId)
                .contains(permissionCode);
        return ResponseEntity.ok(hasPermission);
    }
}


    @GetMapping("/roles")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        return ResponseEntity.ok(rbacManagementService.getAllRoles());
    }

    @GetMapping("/roles/{roleName}")
    public ResponseEntity<RoleDto> getRoleByName(@PathVariable String roleName) {
        return ResponseEntity.ok(rbacManagementService.getRoleByName(roleName));
    }

    @PostMapping("/roles")
    public ResponseEntity<RoleDto> createRole(@RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rbacManagementService.createRole(request));
    }

    @DeleteMapping("/roles/{roleName}")
    public ResponseEntity<Void> deleteRole(@PathVariable String roleName) {
        rbacManagementService.deleteRole(roleName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        return ResponseEntity.ok(rbacManagementService.getAllPermissions());
    }

    @GetMapping("/permissions/{code}")
    public ResponseEntity<PermissionDto> getPermissionByCode(@PathVariable String code) {
        return ResponseEntity.ok(rbacManagementService.getPermissionByCode(code));
    }

    @PostMapping("/permissions")
    public ResponseEntity<PermissionDto> createPermission(@RequestBody CreatePermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rbacManagementService.createPermission(request));
    }

    @DeleteMapping("/permissions/{code}")
    public ResponseEntity<Void> deletePermission(@PathVariable String code) {
        rbacManagementService.deletePermission(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles/{roleName}/permissions")
    public ResponseEntity<Set<PermissionDto>> getPermissionsByRole(@PathVariable String roleName) {
        return ResponseEntity.ok(rbacManagementService.getPermissionsByRole(roleName));
    }

    @PostMapping("/roles/{roleName}/permissions/{permissionCode}")
    public ResponseEntity<Void> assignPermissionToRole(@PathVariable String roleName, @PathVariable String permissionCode) {
        rbacManagementService.assignPermissionToRole(roleName, permissionCode);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/roles/{roleName}/permissions/{permissionCode}")
    public ResponseEntity<Void> revokePermissionFromRole(@PathVariable String roleName, @PathVariable String permissionCode) {
        rbacManagementService.revokePermissionFromRole(roleName, permissionCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/agents/{agentId}/permissions")
    public ResponseEntity<Set<String>> getAgentPermissions(@PathVariable UUID agentId, @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(rbacQueryService.permissionsOf(tenantId, agentId));
    }

    @GetMapping("/agents/{agentId}/permissions/{permissionCode}")
    public ResponseEntity<Boolean> hasPermission(@PathVariable UUID agentId, @PathVariable String permissionCode, @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(rbacQueryService.permissionsOf(tenantId, agentId).contains(permissionCode));
    }
}

