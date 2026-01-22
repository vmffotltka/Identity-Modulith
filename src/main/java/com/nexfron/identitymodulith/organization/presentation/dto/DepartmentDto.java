package com.nexfron.identitymodulith.organization.presentation.dto;

import com.nexfron.identitymodulith.organization.domain.OrganizationConstants;
import com.nexfron.identitymodulith.organization.domain.model.Department;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Department 관련 API 에서 사용하는 DTO 모음
 */
public class DepartmentDto {

    /**
     * 부서 생성 요청 바디
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "부서 생성 요청")
    public static class CreateRequest {

        @NotBlank(message = "부서명은 필수입니다")
        @Size(
                min = OrganizationConstants.DEPARTMENT_NAME_MIN_LENGTH,
                max = OrganizationConstants.DEPARTMENT_NAME_MAX_LENGTH,
                message = "부서명은 " + OrganizationConstants.DEPARTMENT_NAME_MIN_LENGTH +
                        "-" + OrganizationConstants.DEPARTMENT_NAME_MAX_LENGTH + "자 사이여야 합니다"
        )
        @Schema(
                description = "부서명",
                example = "플랫폼개발팀"
        )
        private String name;

        @NotBlank(message = "부서 타입은 필수입니다")
        @Size(
                max = OrganizationConstants.DEPARTMENT_TYPE_MAX_LENGTH,
                message = "부서 타입은 " + OrganizationConstants.DEPARTMENT_TYPE_MAX_LENGTH + "자 이하여야 합니다"
        )
        @Schema(
                description = "부서 타입 (TEAM, DIVISION 등)",
                example = "TEAM"
        )
        private String type;

        @Schema(
                description = "상위 부서 ID (최상위 부서인 경우 null, UUID 문자열)",
                example = "550e8400-e29b-41d4-a716-446655440000",
                nullable = true
        )
        private String parentId;
    }

    /**
     * 부서 업데이트 요청 바디
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "부서 업데이트 요청")
    public static class UpdateRequest {

        @Size(
                min = OrganizationConstants.DEPARTMENT_NAME_MIN_LENGTH,
                max = OrganizationConstants.DEPARTMENT_NAME_MAX_LENGTH,
                message = "부서명은 " + OrganizationConstants.DEPARTMENT_NAME_MIN_LENGTH +
                        "-" + OrganizationConstants.DEPARTMENT_NAME_MAX_LENGTH + "자 사이여야 합니다"
        )
        @Schema(
                description = "변경할 부서명 (선택)",
                example = "AI개발팀"
        )
        private String name;

        @Size(
                max = OrganizationConstants.DEPARTMENT_TYPE_MAX_LENGTH,
                message = "부서 타입은 " + OrganizationConstants.DEPARTMENT_TYPE_MAX_LENGTH + "자 이하여야 합니다"
        )
        @Schema(
                description = "변경할 부서 타입 (선택)",
                example = "TEAM"
        )
        private String type;
    }

    /**
     * 부서 이동 요청 바디
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "부서 이동 요청")
    public static class MoveRequest {

        @Schema(
                description = "새 상위 부서 ID (UUID 문자열)",
                example = "550e8400-e29b-41d4-a716-446655440001"
        )
        private String newParentId;
    }

    /**
     * 부서 정보 응답
     * - 조직 트리 구조를 표현하기 위해 children 필드를 포함
     */
    @Getter
    @Builder
    @Schema(description = "부서 응답 DTO (트리 구조)")
    public static class Response {

        @Schema(
                description = "부서 ID (UUID 문자열)",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        private String deptId;

        @Schema(
                description = "부서명",
                example = "플랫폼개발팀"
        )
        private String name;

        @Schema(
                description = "부서 타입",
                example = "TEAM"
        )
        private String type;

        @Schema(
                description = "조직 경로 (Materialized Path)",
                example = "/550e8400-e29b-41d4-a716-446655440000/550e8400-e29b-41d4-a716-446655440001"
        )
        private String orgPath;

        @Schema(
                description = "조직 트리 깊이 (Root = 0)",
                example = "1"
        )
        private Integer depth;

        @Schema(
                description = "상위 부서 ID (Root 부서는 null, UUID 문자열)",
                example = "550e8400-e29b-41d4-a716-446655440000",
                nullable = true
        )
        private String parentId;

        /**
         * 트리 구조 표현용 자식 노드 리스트
         */
        @Builder.Default
        @Schema(
                description = "하위 부서 목록 (트리 구조)",
                implementation = Response.class
        )
        private List<Response> children = new ArrayList<>();

        public void addChild(Response child) {
            this.children.add(child);
        }

        /**
         * Entity -> DTO 변환 메서드
         */
        public static Response from(Department dept) {
            return Response.builder()
                    .deptId(dept.getDeptId())
                    .name(dept.getName())
                    .type(dept.getType())
                    .orgPath(dept.getOrgPath())
                    .depth(dept.getDepth())
                    .parentId(
                            dept.getParent() != null
                                    ? dept.getParent().getDeptId()
                                    : null
                    )
                    .build();
        }
    }

    /**
     * 부서 통계 정보 DTO
     */
    @Getter
    @Builder
    @Schema(description = "부서 통계 정보")
    public static class Statistics {

        @Schema(
                description = "부서 ID",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        private String deptId;

        @Schema(
                description = "부서명",
                example = "플랫폼개발팀"
        )
        private String name;

        @Schema(
                description = "부서 타입",
                example = "TEAM"
        )
        private String type;

        @Schema(
                description = "부서 깊이 (0부터 시작)",
                example = "2"
        )
        private Integer depth;

        @Schema(
                description = "전체 직원 수 (활성 + 비활성)",
                example = "15"
        )
        private Long totalEmployees;

        @Schema(
                description = "활성 직원 수 (ACTIVE 상태)",
                example = "12"
        )
        private Long activeEmployees;

        @Schema(
                description = "직속 하위 부서 수",
                example = "3"
        )
        private Long childDeptCount;

        @Schema(
                description = "전체 하위 부서 수 (재귀적으로 모든 하위 포함)",
                example = "8"
        )
        private Long descendantDeptCount;
    }

    /**
     * 부서별 사용자 목록 DTO
     */
    @Getter
    @Builder
    @Schema(description = "부서별 사용자 목록")
    public static class DepartmentMembers {

        @Schema(
                description = "부서 ID",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        private String deptId;

        @Schema(
                description = "부서명",
                example = "플랫폼개발팀"
        )
        private String deptName;

        @Schema(
                description = "하위 부서 포함 여부",
                example = "true"
        )
        private Boolean includeSubDepartments;

        @Schema(
                description = "전체 사용자 수",
                example = "15"
        )
        private Integer totalCount;

        @Schema(
                description = "활성 사용자 수",
                example = "12"
        )
        private Long activeCount;

        @Schema(
                description = "퇴직 사용자 수",
                example = "3"
        )
        private Long retiredCount;

        @Schema(description = "사용자 목록")
        private List<MemberInfo> members;
    }

    /**
     * 부서 소속 사용자 정보 DTO
     */
    @Schema(description = "부서 소속 사용자 정보")
    public record MemberInfo(
            @Schema(description = "사용자 ID", example = "user-001")
            String userId,

            @Schema(description = "로그인 ID", example = "john.doe")
            String loginId,

            @Schema(description = "사용자명", example = "홍길동")
            String name,

            @Schema(description = "소속 부서 ID", example = "dept-123")
            String deptId,

            @Schema(description = "직급/직책", example = "대리")
            String jobTitle,

            @Schema(description = "상태", example = "ACTIVE")
            String status
    ) {}
}
