package com.nexfron.identitymodulith.organization.presentation;

import com.nexfron.identitymodulith.organization.presentation.dto.DepartmentDto;
import com.nexfron.identitymodulith.organization.application.service.DepartmentService;
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
import java.util.UUID;

/**
 * DepartmentController - 조직(부서) 관리 REST API
 *
 * <h2>멀티테넌트 처리:</h2>
 * <ul>
 *   <li><b>X-Tenant-Id</b>: 모든 요청에 필수 헤더로 테넌트 ID 전달</li>
 *   <li>모든 데이터는 테넌트 별로 격리됨</li>
 * </ul>
 *
 * <h2>권한 제어 (Level 2 RBAC):</h2>
 * <ul>
 *   <li><b>X-User-Id</b>: 조회/수정/삭제 작업 시 권한 검증에 사용</li>
 *   <li>OrgScopeService를 통해 사용자별 접근 가능 부서 범위 제어</li>
 * </ul>
 *
 * <h2>에러 응답:</h2>
 * <p>
 * 모든 에러는 {@link OrganizationExceptionHandler}에서 일관된 형식으로 변환됨:
 * </p>
 * <pre>
 * {
 *   "code": "ERROR_CODE",
 *   "message": "상세 메시지"
 * }
 * </pre>
 *
 * @author Identity System Team
 * @version 1.0
 */
@Tag(
        name = "Department",
        description = "조직(부서) 관리 API - 부서 생성, 이동, 삭제 및 조직도 트리 조회"
)
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 새로운 부서를 생성합니다.
     *
     * <h3>요청 예시:</h3>
     * <pre>
     * POST /api/v1/departments
     * Headers:
     *   X-Tenant-Id: tenant-001
     * Body:
     * {
     *   "name": "마케팅팀",
     *   "type": "TEAM",
     *   "parentId": 1
     * }
     * </pre>
     *
     * @param tenantId  테넌트 ID (필수)
     * @param request   부서 생성 요청 DTO
     * @return 201 Created + 생성된 부서 정보
     */
    @Operation(
            summary = "부서 생성",
            description = "새로운 부서를 생성합니다. parentId를 지정하면 해당 부서 하위로 생성되고, " +
                    "지정하지 않으면 루트 부서로 생성됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "부서 생성 성공",
                    content = @Content(schema = @Schema(implementation = DepartmentDto.Response.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (예: 상위 부서가 존재하지 않음)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류"
            )
    })
    @PostMapping
    public ResponseEntity<DepartmentDto.Response> createDepartment(
            @Parameter(
                    description = "테넌트 ID",
                    example = "tenant-001",
                    required = true
            )
            @RequestHeader("X-Tenant-Id") String tenantId,

            @RequestBody DepartmentDto.CreateRequest request
    ) {
        DepartmentDto.Response response = departmentService.createDepartment(
                tenantId,
                request.getName(),
                request.getType(),
                request.getParentId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 부서를 다른 부모 부서로 이동합니다.
     *
     * <h3>동작:</h3>
     * <ul>
     *   <li>부서의 부모를 변경합니다</li>
     *   <li>하위 부서들의 orgPath와 depth를 자동으로 재계산합니다</li>
     *   <li>순환 참조(자신의 하위 부서로 이동)를 방지합니다</li>
     *   <li>사용자 권한(Level 2 RBAC)을 검증합니다</li>
     * </ul>
     *
     * @param tenantId      테넌트 ID (필수)
     * @param actorUserId   작업 수행 사용자 ID (권한 검증용, 필수)
     * @param deptId        이동할 부서 ID
     * @param request       새 상위 부서 ID를 포함한 요청
     * @return 200 OK (본문 없음)
     */
    @Operation(
            summary = "부서 이동",
            description = "부서를 다른 부모 부서로 이동합니다. 자동으로 하위 부서의 경로가 재계산됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "이동 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (예: 순환 참조, 부모 부서 없음)"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (사용자의 접근 범위 외)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "부서를 찾을 수 없음"
            )
    })
    @PutMapping("/{deptId}/move")
    public ResponseEntity<Void> moveDepartment(
            @Parameter(description = "테넌트 ID", example = "tenant-001", required = true)
            @RequestHeader("X-Tenant-Id") String tenantId,

            @Parameter(
                    description = "요청 사용자 ID (권한 검증용)",
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    required = true
            )
            @RequestHeader("X-User-Id") UUID actorUserId,

            @Parameter(description = "이동할 부서 ID", example = "10", required = true)
            @PathVariable Long deptId,

            @RequestBody DepartmentDto.MoveRequest request
    ) {
        departmentService.moveDepartment(tenantId, actorUserId, deptId, request.getNewParentId());
        return ResponseEntity.ok().build();
    }

    /**
     * 부서를 삭제합니다.
     *
     * <h3>삭제 조건:</h3>
     * <ul>
     *   <li>하위 부서가 없어야 함</li>
     *   <li>소속 활성 사용자가 없어야 함</li>
     *   <li>사용자에게 삭제 권한이 있어야 함 (Level 2 RBAC)</li>
     * </ul>
     *
     * @param tenantId      테넌트 ID (필수)
     * @param actorUserId   작업 수행 사용자 ID (필수)
     * @param deptId        삭제할 부서 ID
     * @return 204 No Content
     */
    @Operation(
            summary = "부서 삭제",
            description = "부서를 삭제합니다. 하위 부서나 소속 인원이 있으면 삭제할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "삭제 성공"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "부서를 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "충돌 (하위 부서 또는 소속 인원 존재)"
            )
    })
    @DeleteMapping("/{deptId}")
    public ResponseEntity<Void> deleteDepartment(
            @Parameter(description = "테넌트 ID", example = "tenant-001", required = true)
            @RequestHeader("X-Tenant-Id") String tenantId,

            @Parameter(
                    description = "요청 사용자 ID",
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    required = true
            )
            @RequestHeader("X-User-Id") UUID actorUserId,

            @Parameter(description = "삭제할 부서 ID", example = "10", required = true)
            @PathVariable Long deptId
    ) {
        departmentService.deleteDepartment(tenantId, actorUserId, deptId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 테넌트의 전체 조직도 트리를 조회합니다.
     *
     * <h3>반환 구조:</h3>
     * <ul>
     *   <li>루트 부서들이 최상위 리스트</li>
     *   <li>각 부서는 children 배열로 자식 부서 포함</li>
     *   <li>계층적 트리 구조</li>
     * </ul>
     *
     * @param tenantId 테넌트 ID (필수)
     * @return 200 OK + 조직도 트리 DTO 리스트
     */
    @Operation(
            summary = "전체 조직도 트리 조회",
            description = "테넌트 내 모든 부서를 트리 구조로 조회합니다. " +
                    "권한 검증 없이 모든 부서를 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = DepartmentDto.Response.class))
    )
    @GetMapping("/tree")
    public ResponseEntity<List<DepartmentDto.Response>> getTree(
            @Parameter(description = "테넌트 ID", example = "tenant-001", required = true)
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        return ResponseEntity.ok(departmentService.getDepartmentTree(tenantId));
    }

    /**
     * 사용자의 권한 범위 내에서 조직도 트리를 조회합니다.
     *
     * <h3>특징:</h3>
     * <ul>
     *   <li>Level 2 RBAC 기반 필터링</li>
     *   <li>사용자가 접근 가능한 부서만 포함</li>
     *   <li>부모 부서가 범위 밖이면 자식이 루트로 표시될 수 있음</li>
     * </ul>
     *
     * @param tenantId    테넌트 ID (필수)
     * @param actorUserId 조회하는 사용자 ID (필수)
     * @return 200 OK + 필터링된 조직도 트리
     */
    @Operation(
            summary = "내 권한 범위 조직도 조회",
            description = "사용자의 Data Scope(Level 2 RBAC)에 따라 " +
                    "접근 가능한 부서만 트리로 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = DepartmentDto.Response.class))
    )
    @GetMapping("/my-scope")
    public ResponseEntity<List<DepartmentDto.Response>> getMyScopeTree(
            @Parameter(description = "테넌트 ID", example = "tenant-001", required = true)
            @RequestHeader("X-Tenant-Id") String tenantId,

            @Parameter(
                    description = "요청 사용자 ID",
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    required = true
            )
            @RequestHeader("X-User-Id") UUID actorUserId
    ) {
        return ResponseEntity.ok(
                departmentService.getDepartmentTreeWithinScope(tenantId, actorUserId)
        );
    }
}


/**
 * DepartmentController
 *
 * - 조직(부서) 관련 REST API
 * - 테넌트는 X-Tenant-Id 헤더로 구분
 * - Level 2 RBAC(Data Scope)를 위해 X-User-Id 헤더를 추가로 받는 엔드포인트가 있음
 */
@Tag(
        name = "Department",
        description = "조직(부서) 관리 API"
)
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(
            summary = "부서 생성",
            description = "새로운 부서를 생성한다. 부모 부서를 지정하면 해당 부서 하위로 생성된다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "부서 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping
    public ResponseEntity<DepartmentDto.Response> createDepartment(
            @Parameter(
                    description = "테넌트 ID",
                    example = "tenant-001",
                    required = true
            )
            @RequestHeader("X-Tenant-Id") String tenantId,

            @RequestBody DepartmentDto.CreateRequest request
    ) {
        DepartmentDto.Response response = departmentService.createDepartment(
                tenantId,
                request.getName(),
                request.getType(),
                request.getParentId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "부서 이동",
            description = "부서를 다른 부모 부서로 이동한다. 사용자 권한(Data Scope)을 검사한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이동 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "부서 없음")
    })
    @PutMapping("/{deptId}/move")
    public ResponseEntity<Void> moveDepartment(
            @Parameter(description = "테넌트 ID", example = "tenant-001")
            @RequestHeader("X-Tenant-Id") String tenantId,

            @Parameter(description = "요청 사용자 ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestHeader("X-User-Id") UUID actorUserId,

            @Parameter(description = "이동 대상 부서 ID", example = "10")
            @PathVariable Long deptId,

            @RequestBody DepartmentDto.MoveRequest request
    ) {
        departmentService.moveDepartment(tenantId, actorUserId, deptId, request.getNewParentId());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "부서 삭제",
            description = "부서를 삭제한다. 하위 부서 또는 소속 인원이 존재하면 실패한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "409", description = "하위 부서 또는 소속 인원 존재")
    })
    @DeleteMapping("/{deptId}")
    public ResponseEntity<Void> deleteDepartment(
            @Parameter(description = "테넌트 ID", example = "tenant-001")
            @RequestHeader("X-Tenant-Id") String tenantId,

            @Parameter(description = "요청 사용자 ID")
            @RequestHeader("X-User-Id") UUID actorUserId,

            @Parameter(description = "삭제할 부서 ID", example = "10")
            @PathVariable Long deptId
    ) {
        departmentService.deleteDepartment(tenantId, actorUserId, deptId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "전체 조직도 트리 조회",
            description = "테넌트 내 모든 부서를 트리 구조로 조회한다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/tree")
    public ResponseEntity<List<DepartmentDto.Response>> getTree(
            @Parameter(description = "테넌트 ID", example = "tenant-001")
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        return ResponseEntity.ok(departmentService.getDepartmentTree(tenantId));
    }

    @Operation(
            summary = "내 권한 범위 조직도 조회",
            description = "사용자의 Data Scope(Level 2 RBAC)에 따라 접근 가능한 부서만 트리로 조회한다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/my-scope")
    public ResponseEntity<List<DepartmentDto.Response>> getMyScopeTree(
            @Parameter(description = "테넌트 ID", example = "tenant-001")
            @RequestHeader("X-Tenant-Id") String tenantId,

            @Parameter(description = "요청 사용자 ID")
            @RequestHeader("X-User-Id") UUID actorUserId
    ) {
        return ResponseEntity.ok(
                departmentService.getDepartmentTreeWithinScope(tenantId, actorUserId)
        );
    }
}