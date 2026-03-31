package com.identitymodulith.rbac.application.service;

import java.util.Set;
import java.util.UUID;

/** 역할/사용자 기준 권한 코드 조회 계약. */
public interface RbacQueryService {

    /** 역할 집합의 권한 코드 합집합을 반환한다. */
    Set<String> permissionsOfRoles(Set<String> roleNames);

    /** 테넌트 내 역할 집합의 권한 코드를 반환한다. */
    default Set<String> permissionsOfRoles(String tenantId, Set<String> roleNames) {
        return permissionsOfRoles(roleNames);
    }

    /** 테넌트 내 특정 사용자의 권한 코드를 반환한다. */
    Set<String> permissionsOf(String tenantId, UUID agentId);
}