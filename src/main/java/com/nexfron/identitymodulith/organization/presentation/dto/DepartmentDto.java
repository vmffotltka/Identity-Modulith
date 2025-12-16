package com.nexfron.identitymodulith.organization.presentation.dto;

import com.nexfron.identitymodulith.organization.domain.model.Department;
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
    public static class CreateRequest {
        private String name;     // 부서명
        private String type;     // 부서 타입 (팀, 본부 등)
        private Long parentId;   // 상위 부서 ID (없으면 null)
    }

    /**
     * 부서 이동 요청 바디
     */
    @Getter
    @NoArgsConstructor
    public static class MoveRequest {
        private Long newParentId;   // 새 상위 부서 ID
    }

    /**
     * 부서 정보 응답
     * - 조직 트리 구조를 표현하기 위해 children 필드를 포함
     */
    @Getter
    @Builder
    public static class Response {
        private Long deptId;
        private String name;
        private String type;
        private String orgPath;
        private Integer depth;
        private Long parentId;

        /**
         * 트리 구조 표현용 자식 노드 리스트
         * - 기본값으로 빈 리스트를 만들어 두고, addChild 로 채운다.
         */
        @Builder.Default
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
                    .parentId(dept.getParent() != null ? dept.getParent().getDeptId() : null)
                    .build();
        }
    }
}
