package com.nexfron.identitymodulith.rbac.presentation;

import com.nexfron.identitymodulith.rbac.application.service.RbacManagementService;
import com.nexfron.identitymodulith.rbac.application.service.RbacManagementService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Set;

/**
 * RbacController - 역할기반 접근제어(RBAC) 관리 REST API
 *
 * 역할과 권한의 CRUD 및 할당 기능을 제공합니다.
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

    // ============================================================
    // 역할(Role) 관리 엔드포인트
    // ============================================================

    @Operation(summary = "모든 역할 조회", description = "시스템에 정의된 모든 역할을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/roles")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        return ResponseEntity.ok(rbacManagementService.getAllRoles());
    }

    @Operation(summary = "특정 역할 조회", description = "역할명으로 특정 역할의 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음")
    })
    @GetMapping("/roles/{roleName}")
    public ResponseEntity<RoleDto> getRoleByName(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName) {
        return ResponseEntity.ok(rbacManagementService.getRoleByName(roleName));
    }

    @Operation(summary = "역할 생성", description = "새로운 역할을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "역할 생성 성공"),
            @ApiResponse(responseCode = "400", description = "이미 존재하는 역할 또는 잘못된 요청")
    })
    @PostMapping("/roles")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rbacManagementService.createRole(request));
    }

    @Operation(summary = "역할 업데이트", description = "역할 정보(타입, 설명, 활성화 상태)를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "역할 업데이트 성공"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음")
    })
    @PatchMapping("/roles/{roleName}")
    public ResponseEntity<RoleDto> updateRole(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(rbacManagementService.updateRole(roleName, request));
    }

    @Operation(summary = "역할 삭제", description = "역할을 삭제합니다. force=true일 경우 사용자가 있어도 강제 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "역할 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "사용자가 존재하여 삭제 불가 (force=false일 때)")
    })
    @DeleteMapping("/roles/{roleName}")
    public ResponseEntity<RoleDeletionResult> deleteRole(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName,
            @Parameter(description = "강제 삭제 여부 (true: 사용자가 있어도 삭제)", example = "false")
            @RequestParam(defaultValue = "false") boolean forceDelete) {
        return ResponseEntity.ok(rbacManagementService.deleteRole(roleName, forceDelete));
    }

    @Operation(summary = "역할 삭제 영향도 조회", description = "역할 삭제 시 영향을 미치는 사용자 수와 권한 수를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음")
    })
    @GetMapping("/roles/{roleName}/deletion-impact")
    public ResponseEntity<RoleDeletionImpact> getRoleDeletionImpact(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName) {
        return ResponseEntity.ok(rbacManagementService.getRoleDeletionImpact(roleName));
    }

    @Operation(summary = "역할 비활성화", description = "역할을 비활성화합니다. 비활성화된 역할은 새로 할당할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비활성화 성공"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음")
    })
    @PostMapping("/roles/{roleName}/deactivate")
    public ResponseEntity<Void> deactivateRole(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName) {
        rbacManagementService.deactivateRole(roleName);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "역할 활성화", description = "비활성화된 역할을 다시 활성화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "활성화 성공"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음")
    })
    @PostMapping("/roles/{roleName}/activate")
    public ResponseEntity<Void> activateRole(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName) {
        rbacManagementService.activateRole(roleName);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // 권한(Permission) 관리 엔드포인트
    // ============================================================

    @Operation(summary = "모든 권한 조회", description = "시스템에 정의된 모든 권한을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        return ResponseEntity.ok(rbacManagementService.getAllPermissions());
    }

    @Operation(summary = "특정 권한 조회", description = "권한 코드로 특정 권한의 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "권한을 찾을 수 없음")
    })
    @GetMapping("/permissions/{code}")
    public ResponseEntity<PermissionDto> getPermissionByCode(
            @Parameter(description = "권한 코드", example = "user:manage", required = true)
            @PathVariable String code) {
        return ResponseEntity.ok(rbacManagementService.getPermissionByCode(code));
    }

    @Operation(summary = "권한 생성", description = "새로운 권한을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "권한 생성 성공"),
            @ApiResponse(responseCode = "400", description = "이미 존재하는 권한 또는 잘못된 요청")
    })
    @PostMapping("/permissions")
    public ResponseEntity<PermissionDto> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rbacManagementService.createPermission(request));
    }

    @Operation(summary = "권한 업데이트", description = "권한 정보(코드, 설명)를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "권한 업데이트 성공"),
            @ApiResponse(responseCode = "404", description = "권한을 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "변경하려는 코드가 이미 존재함")
    })
    @PatchMapping("/permissions/{code}")
    public ResponseEntity<PermissionDto> updatePermission(
            @Parameter(description = "권한 코드", example = "user:create", required = true)
            @PathVariable String code,
            @Valid @RequestBody UpdatePermissionRequest request) {
        return ResponseEntity.ok(rbacManagementService.updatePermission(code, request));
    }

    @Operation(summary = "권한 삭제", description = "권한을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "권한 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "권한을 찾을 수 없음")
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

    @Operation(summary = "역할의 권한 조회", description = "특정 역할에 할당된 모든 권한을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음")
    })
    @GetMapping("/roles/{roleName}/permissions")
    public ResponseEntity<Set<PermissionDto>> getPermissionsByRole(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName) {
        return ResponseEntity.ok(rbacManagementService.getPermissionsByRole(roleName));
    }

    @Operation(summary = "역할에 권한 할당", description = "특정 역할에 권한을 할당합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "권한 할당 성공"),
            @ApiResponse(responseCode = "400", description = "이미 할당된 권한"),
            @ApiResponse(responseCode = "404", description = "역할 또는 권한을 찾을 수 없음")
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

    @Operation(summary = "역할에서 권한 제거", description = "특정 역할에서 권한을 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "권한 제거 성공"),
            @ApiResponse(responseCode = "404", description = "역할 또는 권한 할당을 찾을 수 없음")
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

    @Operation(summary = "역할에 여러 권한 한 번에 할당", description = "특정 역할에 여러 권한을 한 번에 할당합니다. 이미 할당된 권한은 건너뜁니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대량 할당 완료"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음")
    })
    @PostMapping("/roles/{roleName}/permissions/batch")
    public ResponseEntity<BatchAssignmentResult> batchAssignPermissionsToRole(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName,
            @RequestBody Set<String> permissionCodes) {
        return ResponseEntity.ok(rbacManagementService.batchAssignPermissionsToRole(roleName, permissionCodes));
    }

    @Operation(summary = "역할에서 여러 권한 한 번에 제거", description = "특정 역할에서 여러 권한을 한 번에 제거합니다. 할당되지 않은 권한은 건너뜁니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대량 제거 완료"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음")
    })
    @DeleteMapping("/roles/{roleName}/permissions/batch")
    public ResponseEntity<BatchAssignmentResult> batchRevokePermissionsFromRole(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName,
            @RequestBody Set<String> permissionCodes) {
        return ResponseEntity.ok(rbacManagementService.batchRevokePermissionsFromRole(roleName, permissionCodes));
    }

    // ============================================================
    // 사용자-역할 할당 엔드포인트
    // ============================================================

    @Operation(summary = "사용자에게 역할 할당", description = "특정 사용자에게 역할을 할당합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "역할 할당 성공"),
            @ApiResponse(responseCode = "400", description = "이미 할당된 역할"),
            @ApiResponse(responseCode = "404", description = "사용자 또는 역할을 찾을 수 없음")
    })
    @PostMapping("/agents/{agentId}/roles/{roleName}")
    public ResponseEntity<Void> assignRoleToAgent(
            @Parameter(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
            @PathVariable String agentId,
            @Parameter(description = "역할명", example = "TEAM_LEAD", required = true)
            @PathVariable String roleName) {
        rbacManagementService.assignRoleToAgent(agentId, roleName);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "사용자에게서 역할 회수", description = "특정 사용자에게서 역할을 회수합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "역할 회수 성공"),
            @ApiResponse(responseCode = "404", description = "사용자 또는 역할 할당을 찾을 수 없음")
    })
    @DeleteMapping("/agents/{agentId}/roles/{roleName}")
    public ResponseEntity<Void> revokeRoleFromAgent(
            @Parameter(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
            @PathVariable String agentId,
            @Parameter(description = "역할명", example = "TEAM_LEAD", required = true)
            @PathVariable String roleName) {
        rbacManagementService.revokeRoleFromAgent(agentId, roleName);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // 사용자-역할 관계 엔드포인트
    // ============================================================

    @Operation(summary = "사용자의 역할 목록 조회", description = "특정 사용자에게 할당된 모든 역할을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/agents/{agentId}/roles")
    public ResponseEntity<Set<String>> getRolesByAgent(
            @Parameter(description = "사용자 ID", example = "a0000000-0000-0000-0000-000000000001", required = true)
            @PathVariable String agentId) {
        return ResponseEntity.ok(rbacManagementService.getRolesByAgent(agentId));
    }

    @Operation(summary = "사용자의 실제 권한 조회", description = "사용자가 가진 모든 역할로부터 계산된 실제 권한 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/agents/{agentId}/effective-permissions")
    public ResponseEntity<Set<String>> getEffectivePermissions(
            @Parameter(description = "사용자 ID", example = "a0000000-0000-0000-0000-000000000001", required = true)
            @PathVariable String agentId) {
        return ResponseEntity.ok(rbacManagementService.getEffectivePermissions(agentId));
    }

    // ============================================================
    // 권한-역할 역검색 엔드포인트
    // ============================================================

    @Operation(summary = "특정 권한을 가진 역할 조회", description = "특정 권한을 가진 모든 역할을 조회합니다 (역검색).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "권한을 찾을 수 없음")
    })
    @GetMapping("/permissions/{permissionCode}/roles")
    public ResponseEntity<Set<String>> getRolesWithPermission(
            @Parameter(description = "권한 코드", example = "user:manage", required = true)
            @PathVariable String permissionCode) {
        return ResponseEntity.ok(rbacManagementService.getRolesWithPermission(permissionCode));
    }

    // ============================================================
    // 역할 사용 통계 엔드포인트
    // ============================================================

    @Operation(summary = "역할을 사용하는 사용자 수 조회", description = "특정 역할이 할당된 사용자 수를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음")
    })
    @GetMapping("/roles/{roleName}/agent-count")
    public ResponseEntity<Integer> getAgentCountByRole(
            @Parameter(description = "역할명", example = "TEAM_LEAD", required = true)
            @PathVariable String roleName) {
        return ResponseEntity.ok(rbacManagementService.getAgentCountByRole(roleName));
    }
}

