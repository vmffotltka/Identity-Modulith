package com.nexfron.identitymodulith.user.application;

import com.nexfron.identitymodulith.user.domain.exception.BusinessException;
import com.nexfron.identitymodulith.user.domain.exception.ErrorCode;
import com.nexfron.identitymodulith.user.domain.model.Agent.Role;

import java.util.Set;
import java.util.UUID;

public interface ManageRoleUseCase {

    /**
     * 상담사에게 역할(Role)을 지정합니다.
     * 기존에 할당된 역할은 모두 제거되고 새로운 역할 세트로 대체됩니다.
     *
     * <h3>검증 규칙</h3>
     * - roles 필수: 최소 1개 이상의 역할 필요
     * - 역할 종류: POSITION (직급), CHANNEL (채널)
     * - 일반적인 할당 방식:
     *   - 1개 직급 (예: ADMIN, TEAM_LEAD, AGENT)
     *   - 0개 이상의 채널 (예: VOICE_INBOUND, VOICE_OUTBOUND, CHAT, EMAIL, CALLBACK)
     *
     * @param agentId 역할을 지정할 상담사 ID
     * @param roles 지정할 역할 세트 (최소 1개 필수)
     * @throws BusinessException roles가 null이거나 empty인 경우
     */
    void assignRoles(UUID agentId, Set<Role> roles);
}
