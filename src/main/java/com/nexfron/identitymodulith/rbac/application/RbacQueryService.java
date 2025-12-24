package com.nexfron.identitymodulith.rbac.application;

import java.util.Set;
import java.util.UUID;

/**
 * RBAC 쿼리 서비스 인터페이스
 *
 * 역할 기반 권한 조회를 담당합니다.
 * 구현체는 user 모듈에서 제공합니다.
 */
public interface RbacQueryService {

    /**
     * 주어진 역할명들에 대한 권한 코드들을 조회합니다.
     *
     * @param roleNames 역할명 집합
     * @return 권한 코드 집합
     */
    Set<String> permissionsOfRoles(Set<String> roleNames);

    /**
     * 특정 테넌트의 에이전트가 가진 권한 코드들을 조회합니다.
     *
     * @param tenantId 테넌트 ID
     * @param agentId 에이전트 ID
     * @return 권한 코드 집합
     */
    Set<String> permissionsOf(String tenantId, UUID agentId);
}