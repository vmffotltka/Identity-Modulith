package com.identitymodulith.user.application.port;

/**
 * RBAC 모듈 연동을 위한 Port 인터페이스
 *
 * DDD 원칙에 따라 User 모듈이 RBAC 모듈을 직접 의존하지 않고,
 * Port/Adapter 패턴을 통해 간접적으로 연동합니다.
 *
 * 이 인터페이스는 User 모듈 내부(application layer)에 정의되며,
 * 실제 구현체는 infrastructure layer에 위치합니다.
 *
 * @author Identity System Team
 * @version 1.0
 */
public interface RbacPort {

    /**
     * 사용자에게 역할 할당
     *
     * @param agentId 사용자 ID
     * @param roleName 역할명
     * @throws RuntimeException 역할이 존재하지 않거나 이미 할당된 경우
     */
    void assignRoleToAgent(String agentId, String roleName);

    /**
     * 사용자에게 역할 할당 (roleId 사용)
     *
     * @param agentId 사용자 ID
     * @param roleId 역할 ID (UUID)
     * @throws RuntimeException 역할이 존재하지 않거나 이미 할당된 경우
     */
    void assignRoleToAgentByRoleId(String agentId, String roleId);

    /**
     * 사용자에게 역할 할당 (POSITION 자동 교체 없음)
     * 역할 일괄 지정 시 사용
     *
     * @param agentId 사용자 ID
     * @param roleName 역할명
     * @throws RuntimeException 역할이 존재하지 않거나 이미 할당된 경우
     */
    void assignRoleToAgentWithoutAutoReplace(String agentId, String roleName);

    /**
     * 사용자에게서 역할 제거
     *
     * @param agentId 사용자 ID
     * @param roleName 역할명
     * @throws RuntimeException 역할이 존재하지 않거나 할당되지 않은 경우
     */
    void revokeRoleFromAgent(String agentId, String roleName);

    /**
     * 사용자에게서 모든 역할 제거
     * 역할 일괄 지정 시 기존 역할을 제거하기 위해 사용됩니다.
     *
     * @param agentId 사용자 ID
     */
    void removeAllRolesFromAgent(String agentId);

    /**
     * 역할 존재 여부 확인
     *
     * @param roleName 역할명
     * @return 존재 여부
     */
    boolean roleExists(String roleName);

    /**
     * 사용자가 특정 역할을 가지고 있는지 확인
     *
     * @param agentId 사용자 ID
     * @param roleName 역할명 (예: "ADMIN")
     * @return 역할 보유 여부
     */
    boolean hasRole(String agentId, String roleName);
}
