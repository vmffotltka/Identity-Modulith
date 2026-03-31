package com.identitymodulith.user.application.port;

/** User 모듈에서 RBAC 기능을 호출하기 위한 포트. */
public interface RbacPort {

    void assignRoleToAgent(String agentId, String roleName);

    void assignRoleToAgentByRoleId(String agentId, String roleId);

    /** 일괄 지정 모드(자동 교체 없음)로 역할을 할당한다. */
    void assignRoleToAgentWithoutAutoReplace(String agentId, String roleName);

    void revokeRoleFromAgent(String agentId, String roleName);

    void removeAllRolesFromAgent(String agentId);

    boolean roleExists(String roleName);

    boolean hasRole(String agentId, String roleName);
}
