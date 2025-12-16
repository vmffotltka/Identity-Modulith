package com.nexfron.identitymodulith.organization.presentation;

import com.nexfron.identitymodulith.organization.presentation.dto.DepartmentDto;
import com.nexfron.identitymodulith.organization.application.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * DepartmentController
 *
 * - 조직(부서) 관련 REST API
 * - 테넌트는 X-Tenant-Id 헤더로 구분
 * - Level 2 RBAC(Data Scope)를 위해 X-User-Id 헤더를 추가로 받는 엔드포인트가 있음
 */
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 부서 생성
     *
     * POST /api/v1/departments
     * Header:
     *   X-Tenant-Id : 테넌트 ID
     *   (X-User-Id  : 생성자 – 필요하면 나중에 사용)
     */
    @PostMapping
    public ResponseEntity<DepartmentDto.Response> createDepartment(
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
     * 부서 이동
     *
     * PUT /api/v1/departments/{deptId}/move
     *
     * - actorUserId 를 받아서 DepartmentService 쪽에서
     *   OrgScopeService.canAccessDepartment(...) 로 권한 체크를 수행하도록 한다.
     */
    @PutMapping("/{deptId}/move")
    public ResponseEntity<Void> moveDepartment(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") UUID actorUserId,
            @PathVariable Long deptId,
            @RequestBody DepartmentDto.MoveRequest request
    ) {
        departmentService.moveDepartment(tenantId, actorUserId, deptId, request.getNewParentId());
        return ResponseEntity.ok().build();
    }

    /**
     * 부서 삭제
     *
     * DELETE /api/v1/departments/{deptId}
     *
     * - actorUserId 기반으로 "이 부서를 삭제할 권한이 있는지"를
     *   DepartmentService 안에서 검사한다.
     * - 동시에 OrgUserPort.existsActiveUserInDepartment(...) 를 사용해
     *   소속 인원이 남아 있는지도 체크한다.
     */
    @DeleteMapping("/{deptId}")
    public ResponseEntity<Void> deleteDepartment(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") UUID actorUserId,
            @PathVariable Long deptId
    ) {
        departmentService.deleteDepartment(tenantId, actorUserId, deptId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 전체 조직도 트리 조회
     *
     * GET /api/v1/departments/tree
     *
     * - 테넌트 내 모든 부서를 대상으로 트리를 구성해서 반환
     * - 관리 콘솔 / 전체 조직도 화면 등에 사용
     */
    @GetMapping("/tree")
    public ResponseEntity<List<DepartmentDto.Response>> getTree(
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        return ResponseEntity.ok(departmentService.getDepartmentTree(tenantId));
    }

    /**
     * [Level 2 RBAC] "내가 볼 수 있는" 조직도 트리 조회
     *
     * GET /api/v1/departments/my-scope
     *
     * - OrgScopeService.getAccessibleDepartmentIds(...) 로
     *   actorUserId 의 스코프(접근 가능한 부서 ID 집합)를 계산하고
     * - 그 범위 내에서만 트리를 구성해 반환한다.
     *
     * → 문서의 "데이터 권한 범위(Data Scope)" 기능을 실제 API로 노출하는 엔드포인트
     */
    @GetMapping("/my-scope")
    public ResponseEntity<List<DepartmentDto.Response>> getMyScopeTree(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") UUID actorUserId
    ) {
        List<DepartmentDto.Response> tree = departmentService.getDepartmentTreeWithinScope(tenantId, actorUserId);
        return ResponseEntity.ok(tree);
    }
}