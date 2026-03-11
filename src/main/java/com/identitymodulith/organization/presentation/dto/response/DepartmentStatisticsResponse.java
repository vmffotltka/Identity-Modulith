package com.identitymodulith.organization.presentation.dto.response;

import com.identitymodulith.organization.domain.model.DepartmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "부서 통계 정보")
public class DepartmentStatisticsResponse {

    @Schema(description = "부서 ID")
    private String deptId;

    @Schema(description = "부서명")
    private String name;

    @Schema(description = "부서 타입")
    private DepartmentType type;

    @Schema(description = "부서 깊이 (0부터 시작)")
    private Integer depth;

    @Schema(description = "전체 직원 수 (활성 + 비활성)")
    private Long totalEmployees;

    @Schema(description = "활성 직원 수 (ACTIVE 상태)")
    private Long activeEmployees;

    @Schema(description = "직속 하위 부서 수")
    private Long childDeptCount;

    @Schema(description = "전체 하위 부서 수 (재귀적으로 모든 하위 포함)")
    private Long descendantDeptCount;
}

