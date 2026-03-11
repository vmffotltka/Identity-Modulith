package com.identitymodulith.organization.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "부서별 사용자 목록")
public class DepartmentMembersResponse {

    @Schema(description = "부서 ID")
    private String deptId;

    @Schema(description = "부서명")
    private String deptName;

    @Schema(description = "하위 부서 포함 여부")
    private Boolean includeSubDepartments;

    @Schema(description = "전체 사용자 수")
    private Integer totalCount;

    @Schema(description = "활성 사용자 수")
    private Long activeCount;

    @Schema(description = "퇴직 사용자 수")
    private Long retiredCount;

    @Schema(description = "사용자 목록")
    private List<MemberInfo> members;

    @Schema(description = "부서 소속 사용자 정보")
    public record MemberInfo(
            @Schema(description = "사용자 ID") String userId,
            @Schema(description = "로그인 ID") String loginId,
            @Schema(description = "사용자명") String name,
            @Schema(description = "소속 부서 ID") String deptId,
            @Schema(description = "직급/직책") String jobTitle,
            @Schema(description = "상태") String status
    ) {}
}

