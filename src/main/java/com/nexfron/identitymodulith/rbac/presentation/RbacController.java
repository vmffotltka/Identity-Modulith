package com.nexfron.identitymodulith.rbac.presentation;

import com.nexfron.identitymodulith.rbac.application.RbacManagementService;
import com.nexfron.identitymodulith.rbac.application.RbacManagementService.*;
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
    public ResponseEntity<RoleDto> createRole(@RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rbacManagementService.createRole(request));
    }

    @Operation(summary = "역할 삭제", description = "역할을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "역할 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음")
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
    public ResponseEntity<PermissionDto> createPermission(@RequestBody CreatePermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rbacManagementService.createPermission(request));
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
}

