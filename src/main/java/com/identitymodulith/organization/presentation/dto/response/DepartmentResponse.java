package com.identitymodulith.organization.presentation.dto.response;

import com.identitymodulith.organization.domain.model.DepartmentStatus;
import com.identitymodulith.organization.domain.model.DepartmentType;
import com.identitymodulith.organization.infrastructure.persistence.entity.DepartmentEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@Schema(description = "부서 응답 DTO (트리 구조)")
public class DepartmentResponse {

    @Schema(description = "부서 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String deptId;

    @Schema(description = "부서명", example = "플랫폼개발팀")
    private String name;

    @Schema(description = "부서 타입", example = "TEAM")
    private DepartmentType type;

    @Schema(description = "조직 경로 (Materialized Path)")
    private String orgPath;

    @Schema(description = "조직 트리 깊이 (Root = 0)", example = "1")
    private Integer depth;

    @Schema(description = "상위 부서 ID (Root 부서는 null)", nullable = true)
    private String parentId;

    @Schema(description = "부서 상태", example = "ACTIVE")
    private DepartmentStatus status;

    @Builder.Default
    @Schema(description = "하위 부서 목록")
    private List<DepartmentResponse> children = new ArrayList<>();

    public void addChild(DepartmentResponse child) {
        this.children.add(child);
    }

    public static DepartmentResponse from(DepartmentEntity dept) {
        return DepartmentResponse.builder()
                .deptId(dept.getDeptId())
                .name(dept.getName())
                .type(dept.getType())
                .orgPath(dept.getOrgPath())
                .depth(dept.getDepth())
                .status(dept.getStatus())
                .parentId(dept.getParent() != null ? dept.getParent().getDeptId() : null)
                .build();
    }
}

