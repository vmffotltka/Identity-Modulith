package com.identitymodulith.organization.presentation;

import com.identitymodulith.common.security.context.JwtUserContext;
import com.identitymodulith.common.security.context.TenantContextHolder;
import com.identitymodulith.common.security.context.UnauthorizedException;
import com.identitymodulith.organization.application.service.DepartmentService;
import com.identitymodulith.organization.domain.model.DepartmentType;
import com.identitymodulith.organization.presentation.dto.request.CreateDepartmentRequest;
import com.identitymodulith.organization.presentation.dto.request.MoveDepartmentRequest;
import com.identitymodulith.organization.presentation.dto.request.UpdateDepartmentRequest;
import com.identitymodulith.organization.presentation.dto.response.DepartmentMembersResponse;
import com.identitymodulith.organization.presentation.dto.response.DepartmentResponse;
import com.identitymodulith.organization.presentation.dto.response.DepartmentStatisticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * DepartmentController - 조직(부서) 관리 REST API
 *
 * <h2>제공 기능:</h2>
 * <ul>
 *   <li>조직 구조(부서)의 CRUD</li>
 *   <li>부서 이동 (조직 구조 변경)</li>
 *   <li>조직도 조회 (전체 및 스코프 기반)</li>
 * </ul>
 *
 * <h2>RBAC 통합:</h2>
 * - 데이터 범위 기반 스코프 조회 지원
 * - DataScopeLevel (ADMIN, TEAM_LEAD, MEMBER)에 따른 접근 제어
 */
@Tag(name = "Organization Management", description = "조직(부서) 관리 API")
@RestController
@RequestMapping("/api/org/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * SAML 인증된 현재 사용자의 UUID를 반환합니다.
     * JwtUserContext (ThreadLocal) 에서 추출하며, 미인증 시 UnauthorizedException 발생.
     */
    private UUID currentUserId() {
        String userId = JwtUserContext.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("인증 정보가 없습니다. SAML 로그인이 필요합니다.");
        }
        return UUID.fromString(userId);
    }

    // ============================================================
    // 부서 생성
    // ============================================================

    /**
     * 새로운 부서 생성
     *
     * @param request 부서 생성 요청 (name, type, code, customTypeName, parentId)
     * @return 생성된 부서 정보 (HTTP 201)
     */
    @Operation(summary = "부서 생성", description = "새로운 부서를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "부서 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (상위 부서 없음 등)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        DepartmentResponse response = departmentService.createDepartment(
                tenantId, currentUserId(),
                request.getName(), request.getType(),
                request.getCode(), request.getCustomTypeName(), request.getParentId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================================
    // 부서 수정
    // ============================================================

    /**
     * 부서 정보 수정 (name, type 변경 가능)
     *
     * @param deptId  수정할 부서 ID (UUID)
     * @param request 수정 요청 바디
     * @return 수정된 부서 정보
     */
    @Operation(summary = "부서 정보 수정", description = "부서의 이름이나 타입을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "부서 수정 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "부서를 찾을 수 없음")
    })
    @PatchMapping("/{deptId}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @Parameter(description = "부서 ID (UUID)", required = true)
            @PathVariable String deptId,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        DepartmentResponse response = departmentService.updateDepartment(
                tenantId, currentUserId(), deptId, request.getName(), request.getType()
        );
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // 부서 조회
    // ============================================================

    /**
     * 전체 조직도 조회 (관리자용)
     *
     * @return 조직도 트리 구조 (HTTP 200)
     */
    @Operation(summary = "전체 조직도 조회", description = "전체 조직 구조를 트리 형식으로 조회합니다. (관리자용)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (관리자만)")
    })
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getDepartmentTree() {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        return ResponseEntity.ok(departmentService.getDepartmentTree(tenantId));
    }

    /**
     * 부서 검색 (키워드)
     *
     * @param keyword 검색 키워드 (부서명 부분 일치)
     * @return 검색된 부서 목록
     */
    @Operation(summary = "부서 검색 (키워드)", description = "부서명에 키워드가 포함된 부서를 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/search")
    public ResponseEntity<List<DepartmentResponse>> searchDepartments(
            @Parameter(description = "검색 키워드", example = "개발", required = true)
            @RequestParam String keyword) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        return ResponseEntity.ok(departmentService.searchDepartments(tenantId, keyword));
    }

    /**
     * 하위 부서 트리 조회
     * orgPath 기반으로 지정된 부서 및 모든 하위 부서를 반환합니다.
     *
     * @param deptId 조회할 부서 ID
     * @return 해당 부서 및 모든 하위 부서 목록
     */
    @Operation(summary = "하위 부서 트리 조회", description = "지정된 부서와 그 하위의 모든 부서를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "부서 없음")
    })
    @GetMapping("/{deptId}/subtree")
    public ResponseEntity<List<DepartmentResponse>> getSubtree(
            @Parameter(description = "부서 ID (UUID)", required = true)
            @PathVariable String deptId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        return ResponseEntity.ok(departmentService.getSubtree(tenantId, deptId));
    }

    /**
     * 부서 조회 (깊이별)
     * depth=0: 루트, depth=1: 1단계 하위
     *
     * @param depth 조회할 깊이
     * @return 해당 깊이의 부서 목록
     */
    @Operation(summary = "부서 조회 (깊이별)", description = "특정 깊이(depth)의 부서를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/by-depth")
    public ResponseEntity<List<DepartmentResponse>> getDepartmentsByDepth(
            @Parameter(description = "부서 깊이", example = "0", required = true)
            @RequestParam int depth) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        return ResponseEntity.ok(departmentService.getDepartmentsByDepth(tenantId, depth));
    }

    /**
     * 부서 조회 (타입별)
     *
     * @param type 부서 타입 (Enum)
     * @return 해당 타입의 부서 목록
     */
    @Operation(summary = "부서 조회 (타입별)", description = "특정 타입의 부서를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/by-type")
    public ResponseEntity<List<DepartmentResponse>> getDepartmentsByType(
            @Parameter(description = "부서 타입", example = "TEAM", required = true)
            @RequestParam DepartmentType type) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        return ResponseEntity.ok(departmentService.getDepartmentsByType(tenantId, type));
    }

    /**
     * 부서 통계 조회 (직원 수, 하위 부서 수 등)
     *
     * @param deptId 조회할 부서 ID (UUID)
     * @return 부서 통계 정보
     */
    @Operation(summary = "부서 통계 조회", description = "특정 부서의 직원 수 및 하위 부서 수 통계를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "부서를 찾을 수 없음")
    })
    @GetMapping("/{deptId}/statistics")
    public ResponseEntity<DepartmentStatisticsResponse> getDepartmentStatistics(
            @Parameter(description = "부서 ID (UUID)", required = true)
            @PathVariable String deptId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        return ResponseEntity.ok(departmentService.getDepartmentStatistics(tenantId, deptId));
    }

    /**
     * 부서별 사용자 목록 조회
     *
     * @param deptId                부서 ID (UUID)
     * @param includeSubDepartments 하위 부서 포함 여부
     * @return 부서별 사용자 목록
     */
    @Operation(summary = "부서별 사용자 목록 조회", description = "특정 부서에 소속된 사용자 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "부서를 찾을 수 없음")
    })
    @GetMapping("/{deptId}/members")
    public ResponseEntity<DepartmentMembersResponse> getDepartmentMembers(
            @Parameter(description = "부서 ID (UUID)", required = true)
            @PathVariable String deptId,
            @Parameter(description = "하위 부서 포함 여부", example = "true")
            @RequestParam(defaultValue = "false") boolean includeSubDepartments) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        return ResponseEntity.ok(
                departmentService.getDepartmentMembers(tenantId, deptId, includeSubDepartments));
    }

    // ============================================================
    // 스코프 기반 조회 (Level 1 RBAC)
    // ============================================================

    /**
     * 현재 사용자의 접근 가능 범위 내 조직도 조회 (Level 1 RBAC)
     *
     * <h3>권한 레벨별 조회 범위:</h3>
     * - ADMIN: 전체 부서
     * - TEAM_LEAD: 자신 부서 + 하위 부서들
     * - MEMBER: 자신 부서만
     *
     * @return 접근 가능한 부서들의 조직도 (HTTP 200)
     */
    @Operation(summary = "스코프 기반 조직도 조회", description = "현재 사용자의 접근 가능 범위 내에서 조직도를 조회합니다. (Level 1 RBAC)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "사용자 정보 없음")
    })
    @GetMapping("/scoped")
    public ResponseEntity<List<DepartmentResponse>> getDepartmentTreeWithinScope() {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        return ResponseEntity.ok(
                departmentService.getDepartmentTreeWithinScope(tenantId, currentUserId()));
    }

    // ============================================================
    // 부서 이동
    // ============================================================

    /**
     * 부서를 다른 부서 하위로 이동
     *
     * <h3>주의:</h3>
     * - 자신의 하위 부서로 이동 불가 (순환 참조 방지)
     * - 하위 부서들의 orgPath가 자동 재계산됨
     *
     * @param deptId  이동할 부서 ID
     * @param request 이동 요청 (newParentId: 새 상위 부서 ID, null이면 루트로 이동)
     * @return HTTP 204 No Content
     */
    @Operation(summary = "부서 이동", description = "부서를 다른 부서 하위로 이동합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "부서 이동 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (순환 참조 등)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "부서 없음")
    })
    @PutMapping("/{deptId}/move")
    public ResponseEntity<Void> moveDepartment(
            @Parameter(description = "이동할 부서 ID (UUID)", required = true)
            @PathVariable String deptId,
            @RequestBody MoveDepartmentRequest request) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        departmentService.moveDepartment(tenantId, currentUserId(), deptId, request.getNewParentId());
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // 부서 삭제
    // ============================================================

    /**
     * 부서 삭제
     *
     * <h3>삭제 조건:</h3>
     * - 하위 부서가 없어야 함
     * - 소속 활성 사용자가 없어야 함
     *
     * @param deptId 삭제할 부서 ID
     * @return HTTP 204 No Content
     */
    @Operation(summary = "부서 삭제", description = "부서를 삭제합니다. (하위 부서 없고 소속 직원 없을 때만 가능)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "부서 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "삭제 불가 (하위 부서 또는 소속 직원 있음)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "부서 없음")
    })
    @DeleteMapping("/{deptId}")
    public ResponseEntity<Void> deleteDepartment(
            @Parameter(description = "삭제할 부서 ID (UUID)", required = true)
            @PathVariable String deptId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        departmentService.deleteDepartment(tenantId, currentUserId(), deptId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // 부서 상태 관리
    // ============================================================

    /**
     * 부서 비활성화
     *
     * <h3>비활성화 조건:</h3>
     * - 활성 하위 부서가 없어야 함
     * - 소속 직원이 있어도 비활성화 가능 (경고 로그 출력)
     *
     * @param deptId 비활성화할 부서 ID
     * @return HTTP 204 No Content
     */
    @Operation(summary = "부서 비활성화", description = "부서를 비활성화합니다. (활성 하위 부서가 없어야 함)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "부서 비활성화 성공"),
            @ApiResponse(responseCode = "400", description = "활성 하위 부서 존재"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "부서 없음")
    })
    @PostMapping("/{deptId}/deactivate")
    public ResponseEntity<Void> deactivateDepartment(
            @Parameter(description = "비활성화할 부서 ID (UUID)", required = true)
            @PathVariable String deptId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        departmentService.deactivateDepartment(tenantId, currentUserId(), deptId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 부서 활성화
     *
     * <h3>활성화 조건:</h3>
     * - 상위 부서가 활성 상태여야 함
     *
     * @param deptId 활성화할 부서 ID
     * @return HTTP 204 No Content
     */
    @Operation(summary = "부서 활성화", description = "비활성화된 부서를 다시 활성화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "부서 활성화 성공"),
            @ApiResponse(responseCode = "400", description = "상위 부서가 비활성 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "부서 없음")
    })
    @PostMapping("/{deptId}/activate")
    public ResponseEntity<Void> activateDepartment(
            @Parameter(description = "활성화할 부서 ID (UUID)", required = true)
            @PathVariable String deptId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        departmentService.activateDepartment(tenantId, currentUserId(), deptId);
        return ResponseEntity.noContent().build();
    }
}

