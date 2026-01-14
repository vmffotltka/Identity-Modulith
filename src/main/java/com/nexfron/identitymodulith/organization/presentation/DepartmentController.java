package com.nexfron.identitymodulith.organization.presentation;

import com.nexfron.identitymodulith.organization.application.service.DepartmentService;
import com.nexfron.identitymodulith.organization.presentation.dto.DepartmentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * DepartmentController - 조직(부서) 관리 REST API
 *
 * 조직 구조(부서)의 CRUD, 이동, 조회 기능을 제공합니다.
 * Level 2 RBAC 기반 스코프 조회도 지원합니다.
 *
 * @author Organization Module Team
 * @version 1.0
 */
@Tag(
        name = "Organization Management",
        description = "조직(부서) 관리 API"
)
@RestController
@RequestMapping("/api/org/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    // ============================================================
    // 부서 생성
    // ============================================================

    /**
     * 새로운 부서 생성
     *
     * <h3>동작:</h3>
     * 1. 부서명 및 타입 검증
     * 2. 상위 부서 존재 여부 확인 (있는 경우)
     * 3. 새로운 부서 엔티티 생성
     * 4. orgPath 및 depth 자동 계산
     * 5. DB 저장
     *
     * @param tenantId 테넌트 ID (Request Header 또는 SecurityContext에서 추출)
     * @param request 부서 생성 요청
     *        - name: 부서명 (필수)
     *        - type: 부서 타입 (필수, 예: "본부", "팀", "센터")
     *        - parentId: 상위 부서 ID (선택, 없으면 루트 부서)
     * @return 생성된 부서 정보 (HTTP 201)
     *
     * @apiNote
     * 요청 예시:
     * POST /api/org/departments
     * {
     *   "name": "개발본부",
     *   "type": "본부",
     *   "parentId": null
     * }
     *
     * 응답:
     * {
     *   "deptId": "550e8400-e29b-41d4-a716-446655440000",
     *   "name": "개발본부",
     *   "type": "본부",
     *   "orgPath": "/550e8400-e29b-41d4-a716-446655440000",
     *   "depth": 0
     * }
     */
    @Operation(summary = "부서 생성", description = "새로운 부서를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "부서 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (상위 부서 없음 등)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping
    public ResponseEntity<DepartmentDto.Response> createDepartment(
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @RequestBody DepartmentDto.CreateRequest request) {
        // tenantId 없으면 SecurityContext에서 추출 (SecurityContext 통합 시)
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = "default-tenant";
        }

        DepartmentDto.Response response = departmentService.createDepartment(
                tenantId,
                request.getName(),
                request.getType(),
                request.getParentId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================================
    // 부서 조회 (전체)
    // ============================================================

    /**
     * 전체 조직도 조회 (권한 검증 없음)
     *
     * <h3>주의:</h3>
     * 이 엔드포인트는 권한 검증을 하지 않으므로,
     * 반드시 Spring Security @PreAuthorize로 보호되어야 합니다.
     *
     * <h3>반환 구조:</h3>
     * - 루트 부서들을 최상위로 반환
     * - 각 부서는 하위 부서들을 children 속성으로 포함
     * - orgPath 순서로 정렬됨
     *
     * @param tenantId 테넌트 ID
     * @return 조직도 트리 구조 (HTTP 200)
     *
     * @apiNote
     * 응답 예시:
     * [
     *   {
     *     "deptId": "550e8400-...",
     *     "name": "총무부",
     *     "type": "본부",
     *     "depth": 0,
     *     "children": [
     *       {
     *         "deptId": "550e8400-...",
     *         "name": "HR팀",
     *         "type": "팀",
     *         "depth": 1,
     *         "children": []
     *       }
     *     ]
     *   }
     * ]
     */
    @Operation(
            summary = "전체 조직도 조회",
            description = "권한 검증 없이 전체 조직 구조를 트리 형식으로 조회합니다. (관리자용)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (관리자만)")
    })
    @GetMapping
    public ResponseEntity<List<DepartmentDto.Response>> getDepartmentTree(
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = "default-tenant";
        }

        List<DepartmentDto.Response> tree = departmentService.getDepartmentTree(tenantId);
        return ResponseEntity.ok(tree);
    }

    // ============================================================
    // 부서 조회 (스코프 기반, Level 2 RBAC)
    // ============================================================

    /**
     * 사용자의 접근 가능 범위 내 조직도 조회 (Level 2 RBAC)
     *
     * <h3>동작:</h3>
     * 1. X-User-Id 헤더에서 사용자 ID 추출
     * 2. OrgScopeService로 접근 가능 부서 범위 계산
     * 3. 접근 가능한 부서들만 필터링
     * 4. 트리 구조로 변환 및 반환
     *
     * <h3>권한 레벨별 조회 범위:</h3>
     * - ADMIN: 전체 부서
     * - TEAM_LEAD: 자신 부서 + 하위 부서들
     * - MEMBER: 자신 부서만
     *
     * @param tenantId 테넌트 ID
     * @return 접근 가능한 부서들의 조직도 (HTTP 200)
     *
     * @apiNote
     * 헤더 예시:
     * GET /api/org/departments/scoped
     * X-Tenant-Id: tenant-001
     * X-User-Id: 550e8400-e29b-41d4-a716-446655440100
     *
     * 응답: 해당 사용자가 접근 가능한 부서들만의 트리 구조
     */
    @Operation(
            summary = "스코프 기반 조직도 조회",
            description = "현재 사용자의 접근 가능 범위 내에서 조직도를 조회합니다. (Level 2 RBAC)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "사용자 정보 없음")
    })
    @GetMapping("/scoped")
    public ResponseEntity<List<DepartmentDto.Response>> getDepartmentTreeWithinScope(
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @Parameter(description = "사용자 ID (UUID)", required = true)
            @RequestHeader(value = "X-User-Id") String userIdStr) {
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = "default-tenant";
        }

        UUID userId = UUID.fromString(userIdStr);
        List<DepartmentDto.Response> tree = departmentService.getDepartmentTreeWithinScope(tenantId, userId);
        return ResponseEntity.ok(tree);
    }

    // ============================================================
    // 부서 이동 (재조직)
    // ============================================================

    /**
     * 부서를 다른 부서 하위로 이동
     *
     * <h3>동작:</h3>
     * 1. 이동 대상 부서 조회
     * 2. 새 상위 부서 조회 및 검증
     * 3. 순환 참조 방지 검사 (자신의 하위로 이동 불가)
     * 4. 부모 변경
     * 5. 하위 부서들의 orgPath 일괄 재계산
     * 6. Level 2 RBAC 권한 검증
     *
     * <h3>주의:</h3>
     * - 자신의 하위 부서로 이동할 수 없음 (순환 참조 방지)
     * - 하위 부서들의 경로가 자동으로 재계산됨
     * - 권한: 이동 대상 부서에 대한 접근 권한 필요
     *
     * @param tenantId 테넌트 ID
     * @param userIdStr 사용자 ID
     * @param deptId 이동할 부서 ID
     * @param request 이동 요청
     *        - newParentId: 새 상위 부서 ID (null이면 루트로 이동)
     * @return HTTP 204 No Content
     *
     * @apiNote
     * 요청 예시:
     * PUT /api/org/departments/550e8400.../move
     * X-Tenant-Id: tenant-001
     * X-User-Id: 550e8400-e29b-41d4-a716-446655440100
     * {
     *   "newParentId": "550e8400-e29b-41d4-a716-446655440001"
     * }
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
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @Parameter(description = "사용자 ID (UUID)", required = true)
            @RequestHeader(value = "X-User-Id") String userIdStr,
            @Parameter(description = "이동할 부서 ID (UUID)", required = true)
            @PathVariable String deptId,
            @RequestBody DepartmentDto.MoveRequest request) {
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = "default-tenant";
        }

        UUID userId = UUID.fromString(userIdStr);
        departmentService.moveDepartment(tenantId, userId, deptId, request.getNewParentId());
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // 부서 삭제
    // ============================================================

    /**
     * 부서 삭제
     *
     * <h3>삭제 조건:</h3>
     * 1. 하위 부서가 없어야 함
     * 2. 소속 활성 사용자가 없어야 함
     * 3. 사용자가 삭제 권한을 가져야 함
     *
     * <h3>주의:</h3>
     * - 하위 부서가 있으면 먼저 이동하거나 삭제해야 함
     * - 소속 직원이 있으면 먼저 다른 부서로 이동시켜야 함
     *
     * @param tenantId 테넌트 ID
     * @param userIdStr 사용자 ID
     * @param deptId 삭제할 부서 ID
     * @return HTTP 204 No Content
     *
     * @apiNote
     * 요청 예시:
     * DELETE /api/org/departments/550e8400-...
     * X-Tenant-Id: tenant-001
     * X-User-Id: 550e8400-e29b-41d4-a716-446655440100
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
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @Parameter(description = "사용자 ID (UUID)", required = true)
            @RequestHeader(value = "X-User-Id") String userIdStr,
            @Parameter(description = "삭제할 부서 ID (UUID)", required = true)
            @PathVariable String deptId) {
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = "default-tenant";
        }

        UUID userId = UUID.fromString(userIdStr);
        departmentService.deleteDepartment(tenantId, userId, deptId);
        return ResponseEntity.noContent().build();
    }
}

