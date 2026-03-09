package com.identitymodulith.user;

import com.identitymodulith.user.application.AgentService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User 모듈의 Public API
 * <p>
 * 다른 모듈(Organization 등)에서 User 모듈의 기능을 사용할 때 이 인터페이스를 통해 접근합니다.
 * Spring Modulith의 모듈 가시성 규칙에 따라 루트 패키지에 위치합니다.
 * </p>
 *
 * <p>
 * 구현체: {@link AgentService}
 * </p>
 */
public interface UserModuleApi {

    /**
     * 특정 부서에 속한 활성 상태의 상담사 목록을 조회합니다.
     *
     * @param tenantId       테넌트 ID
     * @param organizationId 부서(조직) ID
     * @return 해당 부서의 활성 상담사 목록
     */
    List<AgentExternalInfo> findActiveAgentsByOrganizationId(String tenantId, String organizationId);

    /**
     * 여러 부서 ID에 해당하는 활성 상담사들을 일괄 조회합니다.
     * - Organization 모듈의 findActiveUsersByDeptIds 에 매핑되어 사용됩니다.
     */
    List<AgentExternalInfo> findActiveAgentsByOrganizationIds(String tenantId, List<String> organizationIds);

    /**
     * 상담사 ID로 상담사 정보를 조회합니다.
     *
     * @param tenantId 테넌트 ID
     * @param agentId  상담사 UUID
     * @return 상담사 정보 (없으면 empty)
     */
    Optional<AgentExternalInfo> findAgentById(String tenantId, UUID agentId);

    /**
     * 로그인 ID로 상담사 정보를 조회합니다.
     * - SAML 인증 후 사용자 매핑에 사용됩니다.
     *
     * @param loginId 로그인 ID
     * @return 상담사 정보 (없으면 empty)
     */
    Optional<AgentExternalInfo> findAgentByLoginId(String loginId);
}
