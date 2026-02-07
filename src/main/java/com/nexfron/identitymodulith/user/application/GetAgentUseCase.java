package com.nexfron.identitymodulith.user.application;

import com.nexfron.identitymodulith.user.domain.model.Agent.Role;
import com.nexfron.identitymodulith.user.domain.model.AgentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface GetAgentUseCase {

    AgentInfo getAgent(UUID agentId);

    List<AgentInfo> getAgents(AgentSearchCriteria criteria);

    @Getter
    @Builder
    class AgentInfo {
        private final UUID id;
        private final String loginId;
        private final String name;
        private final String organizationId;
        private final String departmentName;      // 소속 부서명
        private final String departmentPath;      // 소속 부서 전체 경로
        private final String employeeId;          // 사원번호
        private final String email;               // 이메일
        private final String phone;               // 전화번호
        private final AgentStatus status;
        private final boolean passwordMustChange;
        private final LocalDateTime createdAt;
        private final LocalDateTime retiredAt;
        private final Set<Role> roles;
        // 비밀번호는 절대 포함하지 않음
    }

    @Getter
    @Builder
    class AgentSearchCriteria {
        private final String tenantId;
        private final String organizationId;
        private final AgentStatus status;  // 상태 필터 (null = 전체)
        private final String nameKeyword;  // 이름 검색 (부분 일치)
        private final String loginIdKeyword;  // 로그인 ID 검색 (부분 일치)
        private final boolean includeRetired;  // 퇴사자 포함 여부
    }
}
