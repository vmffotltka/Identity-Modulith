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


import java.util.List;
import java.util.UUID;

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