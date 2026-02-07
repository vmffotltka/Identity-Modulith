package com.nexfron.identitymodulith.user;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * User 모듈이 외부에 제공하는 상담사 정보 DTO
 * <p>
 * 모듈 간 통신에 사용되며, 도메인 엔티티(Agent)의 민감 정보(비밀번호 등)는 제외됩니다.
 * Spring Modulith의 모듈 가시성 규칙에 따라 루트 패키지에 위치합니다.
 * </p>
 */
@Getter
@Builder
public class AgentExternalInfo {

    private final UUID id;
    private final String tenantId;
    private final String organizationId;
    private final Set<RoleInfo> roles;
    private final boolean active;

    /**
     * 상담사 역할 정보
     */
    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode
    public static class RoleInfo {

        private final String name;
        private final RoleType type;

        public enum RoleType {
            POSITION,  // 직급
            CHANNEL    // 채널 (전화, 채팅 등)
        }
    }
}
