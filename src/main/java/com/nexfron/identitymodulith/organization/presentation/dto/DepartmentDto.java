package com.nexfron.identitymodulith.organization.presentation.dto;

import com.nexfron.identitymodulith.organization.domain.model.Department;
import io.swagger.v3.oas.annotations.media.Schema;
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

        @Schema(
                description = "부서명",
                example = "플랫폼개발팀"
        )
        private String name;

        @Schema(
                description = "부서 타입 (TEAM, DIVISION 등)",
                example = "TEAM"
        )
        private String type;

        @Schema(
                description = "상위 부서 ID (최상위 부서인 경우 null)",
                example = "1",
                nullable = true
        )
        private Long parentId;
    }

    /**
     * 부서 이동 요청 바디
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "부서 이동 요청")
    public static class MoveRequest {

        @Schema(
                description = "새 상위 부서 ID",
                example = "2"
        )
        private Long newParentId;
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
                description = "부서 ID",
                example = "10"
        )
        private Long deptId;

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
                example = "/1/3/10"
        )
        private String orgPath;

        @Schema(
                description = "조직 트리 깊이 (Root = 0)",
                example = "2"
        )
        private Integer depth;

        @Schema(
                description = "상위 부서 ID (Root 부서는 null)",
                example = "3",
                nullable = true
        )
        private Long parentId;

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
}
