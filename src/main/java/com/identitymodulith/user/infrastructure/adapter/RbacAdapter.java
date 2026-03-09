package com.identitymodulith.user.infrastructure.adapter;

import com.identitymodulith.rbac.application.service.RbacManagementService;
import com.identitymodulith.user.application.port.RbacPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RBAC 모듈 연동 Adapter
 *
 * User 모듈의 RbacPort 인터페이스 구현체로,
 * 실제로는 RBAC 모듈의 서비스를 호출합니다.
 *
 * 이 Adapter는 Infrastructure layer에 위치하며,
 * User 모듈이 RBAC 모듈을 직접 의존하는 유일한 지점입니다.
 * (Presentation/Application layer는 Port만 의존)
 *
 * @author Identity System Team
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RbacAdapter implements RbacPort {

    private final RbacManagementService rbacManagementService;

    @Override
    public void assignRoleToAgent(String agentId, String roleName) {
        log.debug("[User->RBAC] 역할 할당 요청 - agentId={}, roleName={}", agentId, roleName);
        rbacManagementService.assignRoleToAgent(agentId, roleName);
    }

    @Override
    public void assignRoleToAgentByRoleId(String agentId, String roleId) {
        log.debug("[User->RBAC] 역할 할당 요청 (roleId) - agentId={}, roleId={}", agentId, roleId);
        rbacManagementService.assignRoleToAgentByRoleId(agentId, roleId);
    }

    @Override
    public void assignRoleToAgentWithoutAutoReplace(String agentId, String roleName) {
        log.debug("[User->RBAC] 역할 할당 요청 (자동 교체 없음) - agentId={}, roleName={}", agentId, roleName);
        rbacManagementService.assignRoleToAgentWithoutAutoReplace(agentId, roleName);
    }

    @Override
    public void revokeRoleFromAgent(String agentId, String roleName) {
        log.debug("[User->RBAC] 역할 제거 요청 - agentId={}, roleName={}", agentId, roleName);
        rbacManagementService.revokeRoleFromAgent(agentId, roleName);
    }

    @Override
    public void removeAllRolesFromAgent(String agentId) {
        log.debug("[User->RBAC] 모든 역할 제거 요청 - agentId={}", agentId);
        rbacManagementService.removeAllRolesFromAgent(agentId);
    }

    @Override
    public boolean roleExists(String roleName) {
        try {
            rbacManagementService.getRoleByName(roleName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasRole(String agentId, String roleName) {
        log.debug("[User->RBAC] 역할 보유 확인 - agentId={}, roleName={}", agentId, roleName);
        try {
            return rbacManagementService.hasRole(agentId, roleName);
        } catch (Exception e) {
            log.warn("[User->RBAC] 역할 확인 실패 - agentId={}, roleName={}, error={}",
                    agentId, roleName, e.getMessage());
            return false;
        }
    }
}
