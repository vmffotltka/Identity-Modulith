package com.identitymodulith.rbac.application.service;

import com.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.repository.AgentRoleJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.RoleJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/** RBAC 권한 조회 전용 쿼리 서비스. */
@Service
@RequiredArgsConstructor
@Slf4j
public class RbacQueryServiceImpl implements RbacQueryService {

    private final AgentRoleJpaRepository agentRoleRepository;
    private final RolePermissionJpaRepository rolePermissionRepository;
    private final RoleJpaRepository roleRepository;

    @Override
    public Set<String> permissionsOfRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            log.trace("역할 목록이 비어있습니다 (tenantId 미지정)");
            return Set.of();
        }
        log.warn("permissionsOfRoles(Set<String>)는 멀티테넌시 환경에서 권장되지 않습니다. tenantId가 포함된 오버로드를 사용하세요. roleNames={}", roleNames);
        return Set.of();
    }

    /** 테넌트 내 역할 집합의 권한 코드를 조회한다. */
    @Override
    public Set<String> permissionsOfRoles(String tenantId, Set<String> roleNames) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId는 필수입니다.");
        }
        if (roleNames == null || roleNames.isEmpty()) {
            log.trace("[RBAC] permissionsOfRoles - 역할 목록이 비어 있습니다: tenantId={}", tenantId);
            return Set.of();
        }

        long startTime = System.currentTimeMillis();
        log.debug("[RBAC] 테넌트 내 역할 기반 권한 조회 시작: tenantId={}, roleNames={}", tenantId, roleNames);

        List<RoleJpaEntity> roles = roleRepository.findByTenantIdAndNameIn(tenantId, roleNames);
        if (roles.isEmpty()) {
            log.debug("[RBAC] 해당 테넌트에서 일치하는 역할이 없습니다: tenantId={}, roleNames={}", tenantId, roleNames);
            return Set.of();
        }

        Set<String> roleIds = roles.stream()
                .map(RoleJpaEntity::getRoleId)
                .collect(Collectors.toSet());

        List<String> permissionCodes = rolePermissionRepository
                .findPermissionCodesByRoleIdsAndTenant(roleIds, tenantId);

        if (permissionCodes.isEmpty()) {
            log.debug("[RBAC] 역할은 있으나 권한 매핑이 없습니다: tenantId={}, roleNames={}", tenantId, roleNames);
            return Set.of();
        }

        Set<String> codes = new HashSet<>(permissionCodes);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[RBAC] permissionsOfRoles 완료 (최적화): tenantId={}, roles={}, roleCount={}, permissionCount={}, 소요시간={}ms",
                tenantId, roleNames, roleIds.size(), codes.size(), duration);

        return codes;
    }

    /** 에이전트 권한을 3-JOIN 단일 쿼리로 조회한다. */
    @Override
    public Set<String> permissionsOf(String tenantId, UUID agentId) {
        if (tenantId == null || tenantId.isBlank() || agentId == null) {
            log.warn("[RBAC] 권한 조회 입력이 올바르지 않습니다: tenantId={}, agentId={} (빈 Set 반환)", tenantId, agentId);
            return Set.of();
        }
        long startTime = System.currentTimeMillis();

        List<String> permissionCodes = agentRoleRepository
                .findPermissionCodesByAgentIdAndTenant(agentId.toString(), tenantId);

        if (permissionCodes.isEmpty()) {
            log.debug("[RBAC 권한 조회] 사용자에게 매핑된 권한이 없음: agentId={}, tenantId={}", agentId, tenantId);
            return Set.of();
        }

        Set<String> codes = new HashSet<>(permissionCodes);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[RBAC 권한 조회 완료] agentId={}, tenantId={}, 권한 수={}, 소요시간={}ms",
                agentId, tenantId, codes.size(), duration);

        return codes;
    }
}
