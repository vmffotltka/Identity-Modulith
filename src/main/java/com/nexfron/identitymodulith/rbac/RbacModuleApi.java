package com.nexfron.identitymodulith.rbac;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * RBAC 모듈의 Public API
 * <p>
 * 다른 모듈(User, Organization 등)에서 RBAC 모듈의 기능을 사용할 때 이 인터페이스를 통해 접근합니다.
 * Spring Modulith의 모듈 가시성 규칙에 따라 루트 패키지에 위치합니다.
 * </p>
 */
public interface RbacModuleApi {

    /**
     * 사용자의 역할 정보를 조회합니다.
     *
     * @param agentId 사용자 ID (UUID 문자열)
     * @return 사용자의 역할 정보 세트
     */
    Set<RoleInfo> getRolesByAgentId(String agentId);

    /**
     * 역할 정보 DTO
     */
    @Getter
    @RequiredArgsConstructor
    class RoleInfo {
        private final String name;
        private final RoleType type;
        private final DataScopeLevel dataScopeLevel;

        public enum RoleType {
            POSITION,  // 직급
            CHANNEL,   // 채널
            SKILL,     // 능력
            CUSTOM     // 커스텀
        }

        public enum DataScopeLevel {
            SELF,       // 본인만
            DEPARTMENT, // 부서
            ALL,        // 전체
            CUSTOM      // 커스텀
        }
    }
}

