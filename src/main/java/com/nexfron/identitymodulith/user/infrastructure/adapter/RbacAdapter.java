package com.nexfron.identitymodulith.user.infrastructure.adapter;

import com.nexfron.identitymodulith.rbac.application.service.RbacManagementService;
import com.nexfron.identitymodulith.user.application.port.RbacPort;
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
    public void revokeRoleFromAgent(String agentId, String roleName) {
        log.debug("[User->RBAC] 역할 제거 요청 - agentId={}, roleName={}", agentId, roleName);
        rbacManagementService.revokeRoleFromAgent(agentId, roleName);
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
}
