package com.identitymodulith.user.presentation.dto.response;

import com.identitymodulith.user.application.GetAgentUseCase.AgentInfo;
import com.identitymodulith.user.domain.model.Agent.Role;
import com.identitymodulith.user.domain.model.AgentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
@Schema(description = "상담사 정보 응답")
public class AgentResponse {

    @Schema(description = "상담사 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "로그인 아이디", example = "agent001")
    private String loginId;

    @Schema(description = "상담사 이름", example = "홍길동")
    private String name;

    @Schema(description = "소속 조직 ID", example = "550e8400-e29b-41d4-a716-446655440001")
    private String organizationId;

    @Schema(description = "소속 부서명", example = "고객지원팀")
    private String departmentName;

    @Schema(description = "소속 부서 전체 경로", example = "본사 > 고객지원본부 > 고객지원팀")
    private String departmentPath;

    @Schema(description = "사원번호", example = "EMP-2024-001")
    private String employeeId;

    @Schema(description = "이메일", example = "hong@example.com")
    private String email;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;

    @Schema(description = "상담사 상태", example = "ACTIVE", allowableValues = {"ACTIVE", "SUSPENDED", "RETIRED"})
    private AgentStatus status;

    @Schema(description = "비밀번호 변경 필요 여부", example = "false")
    private boolean passwordMustChange;

    @Schema(description = "생성 일시", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "퇴사 일시 (퇴사한 경우에만)", example = "2024-12-31T18:00:00")
    private LocalDateTime retiredAt;

    @Schema(description = "할당된 역할 목록")
    private Set<RoleDto> roles;

    public static AgentResponse from(AgentInfo info) {
        return AgentResponse.builder()
                .id(info.getId())
                .loginId(info.getLoginId())
                .name(info.getName())
                .organizationId(info.getOrganizationId())
                .departmentName(info.getDepartmentName())
                .departmentPath(info.getDepartmentPath())
                .employeeId(info.getEmployeeId())
                .email(info.getEmail())
                .phone(info.getPhone())
                .status(info.getStatus())
                .passwordMustChange(info.isPasswordMustChange())
                .createdAt(info.getCreatedAt())
                .retiredAt(info.getRetiredAt())
                .roles(info.getRoles().stream()
                        .map(RoleDto::from)
                        .collect(Collectors.toSet()))
                .build();
    }

    @Getter
    @Builder
    @Schema(description = "역할 정보")
    public static class RoleDto {
        @Schema(description = "역할 이름", example = "SENIOR_AGENT")
        private String name;

        @Schema(description = "역할 유형", example = "POSITION", allowableValues = {"POSITION", "CHANNEL"})
        private Role.RoleType type;

        public static RoleDto from(Role role) {
            return RoleDto.builder()
                    .name(role.getName())
                    .type(role.getType())
                    .build();
        }
    }
}
