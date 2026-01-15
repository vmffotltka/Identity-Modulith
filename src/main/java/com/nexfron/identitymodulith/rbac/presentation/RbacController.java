package com.nexfron.identitymodulith.rbac.presentation;

import com.nexfron.identitymodulith.rbac.application.RbacManagementService;
import com.nexfron.identitymodulith.rbac.application.RbacManagementService.*;
import com.nexfron.identitymodulith.rbac.application.dto.AuditLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDateTime;
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

    @Operation(summary = "역할 업데이트", description = "역할 정보(타입, 설명, 활성화 상태)를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "역할 업데이트 성공"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음")
    })
    @PatchMapping("/roles/{roleName}")
    public ResponseEntity<RoleDto> updateRole(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName,
            @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(rbacManagementService.updateRole(roleName, request));
    }

    @Operation(summary = "역할 삭제 영향도 조회", description = "역할 삭제 시 영향받을 사용자와 권한 정보를 조회합니다.")
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

    @Operation(summary = "역할 삭제 (강화된 버전)", description = "역할을 삭제합니다. 사용자 확인 및 강제 삭제 옵션을 지원합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "역할 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "사용자가 할당된 역할 (force=false인 경우)"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음")
    })
    @DeleteMapping("/roles/{roleName}")
    public ResponseEntity<RoleDeletionResult> deleteRole(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName,
            @Parameter(description = "강제 삭제 여부", example = "false")
            @RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.ok(rbacManagementService.deleteRole(roleName, force));
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
            @RequestBody UpdatePermissionRequest request) {
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

    // ============================================================
    // 권한 그룹(Permission Group) 관리 엔드포인트
    // ============================================================

    @Operation(summary = "권한 그룹 업데이트", description = "권한 그룹의 설명이나 활성화 상태를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "권한 그룹 업데이트 성공"),
            @ApiResponse(responseCode = "404", description = "권한 그룹을 찾을 수 없음")
    })
    @PatchMapping("/permission-groups/{groupName}")
    public ResponseEntity<PermissionGroupDto> updatePermissionGroup(
            @Parameter(description = "권한 그룹명", example = "USER_FULL_ACCESS", required = true)
            @PathVariable String groupName,
            @RequestBody UpdatePermissionGroupRequest request) {
        return ResponseEntity.ok(rbacManagementService.updatePermissionGroup(groupName, request));
    }

    @Operation(summary = "권한 그룹 비활성화", description = "특정 권한 그룹을 비활성화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "권한 그룹 비활성화 성공"),
            @ApiResponse(responseCode = "404", description = "권한 그룹을 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "이미 비활성화된 권한 그룹")
    })
    @PostMapping("/permission-groups/{groupName}/deactivate")
    public ResponseEntity<Void> deactivatePermissionGroup(
            @Parameter(description = "권한 그룹명", example = "USER_FULL_ACCESS", required = true)
            @PathVariable String groupName) {
        rbacManagementService.deactivatePermissionGroup(groupName);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "권한 그룹 활성화", description = "비활성화된 권한 그룹을 다시 활성화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "권한 그룹 활성화 성공"),
            @ApiResponse(responseCode = "404", description = "권한 그룹을 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "이미 활성화된 권한 그룹")
    })
    @PostMapping("/permission-groups/{groupName}/activate")
    public ResponseEntity<Void> activatePermissionGroup(
            @Parameter(description = "권한 그룹명", example = "USER_FULL_ACCESS", required = true)
            @PathVariable String groupName) {
        rbacManagementService.activatePermissionGroup(groupName);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // 권한 변경 이력 조회 (Audit Log) 엔드포인트
    // ============================================================

    @Operation(summary = "사용자 권한 변경 이력 조회", description = "특정 사용자의 권한 관련 변경 이력을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/audit/agents/{agentId}")
    public ResponseEntity<List<AuditLogDto>> getAgentPermissionChangeHistory(
            @Parameter(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
            @PathVariable String agentId,
            @Parameter(description = "시작 일시 (ISO 형식)", example = "2024-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "종료 일시 (ISO 형식)", example = "2024-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(rbacManagementService.getAgentPermissionChangeHistory(agentId, from, to));
    }

    @Operation(summary = "역할 권한 변경 이력 조회", description = "특정 역할의 권한 관련 변경 이력을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없음")
    })
    @GetMapping("/audit/roles/{roleName}")
    public ResponseEntity<List<AuditLogDto>> getRolePermissionChangeHistory(
            @Parameter(description = "역할명", example = "ADMIN", required = true)
            @PathVariable String roleName,
            @Parameter(description = "시작 일시 (ISO 형식)", example = "2024-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "종료 일시 (ISO 형식)", example = "2024-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(rbacManagementService.getRolePermissionChangeHistory(roleName, from, to));
    }

    @Operation(summary = "전체 권한 변경 이력 조회", description = "전체 권한 관련 변경 이력을 조회합니다. (관리자용)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 페이지 크기")
    })
    @GetMapping("/audit/all")
    public ResponseEntity<List<AuditLogDto>> getAllPermissionChangeHistory(
            @Parameter(description = "시작 일시 (ISO 형식)", example = "2024-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "종료 일시 (ISO 형식)", example = "2024-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "페이지 크기", example = "100")
            @RequestParam(defaultValue = "100") Integer pageSize) {
        return ResponseEntity.ok(rbacManagementService.getAllPermissionChangeHistory(from, to, pageSize));
    }

    @Operation(summary = "작업자 권한 작업 이력 조회", description = "특정 작업자의 권한 관련 작업 이력을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "작업자를 찾을 수 없음")
    })
    @GetMapping("/audit/operators/{operatorId}")
    public ResponseEntity<List<AuditLogDto>> getOperatorPermissionActions(
            @Parameter(description = "작업자 ID", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
            @PathVariable String operatorId,
            @Parameter(description = "시작 일시 (ISO 형식)", example = "2024-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "종료 일시 (ISO 형식)", example = "2024-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(rbacManagementService.getOperatorPermissionActions(operatorId, from, to));
    }
}

