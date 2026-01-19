package com.nexfron.identitymodulith.rbac.application;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.AgentRoleJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.PermissionJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.RoleJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = RbacCacheEvictIntegrationTest.TestConfig.class)
@EnableCaching
class RbacCacheEvictIntegrationTest {

    @Configuration
    @Import(RbacManagementServiceImpl.class)
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("userPermissions");
        }
    }

    @MockBean
    private RoleJpaRepository roleRepository;
    @MockBean
    private PermissionJpaRepository permissionRepository;
    @MockBean
    private RolePermissionJpaRepository rolePermissionRepository;
    @MockBean
    private AgentRoleJpaRepository agentRoleRepository;
    @MockBean
    private AuditLogService auditLogService;
    @MockBean
    private Authentication authentication;

    @Autowired
    private RbacManagementService rbacManagementService;
    @Autowired
    private CacheManager cacheManager;

    @DisplayName("assignRoleToAgent 호출 시 userPermissions 캐시가 비워진다")
    @Test
    void assignRoleToAgent_evictsUserPermissions() {
        String agentId = "agent-1";
        String roleName = "ADMIN";
        String tenantId = "tenant-x";

        // SecurityContext principal을 tenantId로 설정 (서비스 내부 getTenantId가 String principal을 반환)
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getPrincipal()).thenReturn(tenantId);

        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId("role-1")
                .tenantId(tenantId)
                .name(roleName)
                .type("POSITION")
                .createdAt(LocalDateTime.now())
                .build();

        when(roleRepository.findByTenantIdAndName(tenantId, roleName)).thenReturn(Optional.of(role));
        when(agentRoleRepository.existsByAgentIdAndRoleId(agentId, "role-1")).thenReturn(false);

        Cache cache = cacheManager.getCache("userPermissions");
        assert cache != null;
        cache.put(tenantId + ":" + agentId, Set.of("dummy"));

        // when
        rbacManagementService.assignRoleToAgent(agentId, roleName);

        // then: allEntries=true 이므로 기존 키 삭제
        assertThat(cache.get(tenantId + ":" + agentId)).isNull();
    }

    @DisplayName("assignPermissionToRole 호출 시 userPermissions 캐시가 전체 무효화된다")
    @Test
    void assignPermissionToRole_evictsAllUserPermissions() {
        String tenantId = "tenant-x";
        String roleName = "ADMIN";

        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getPrincipal()).thenReturn(tenantId);

        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId("role-1")
                .tenantId(tenantId)
                .name(roleName)
                .type("POSITION")
                .createdAt(LocalDateTime.now())
                .build();
        PermissionJpaEntity perm = PermissionJpaEntity.builder()
                .permissionId("perm-1")
                .tenantId(tenantId)
                .code("sample:perm")
                .createdAt(LocalDateTime.now())
                .build();

        when(roleRepository.findByTenantIdAndName(tenantId, roleName)).thenReturn(Optional.of(role));
        when(permissionRepository.findByTenantIdAndCode(tenantId, "sample:perm")).thenReturn(Optional.of(perm));
        // ✅ P0: existsByRoleIdAndPermissionId 제거

        Cache cache = cacheManager.getCache("userPermissions");
        assert cache != null;
        cache.put(tenantId + ":agent-1", Set.of("dummy"));
        cache.put(tenantId + ":agent-2", Set.of("dummy"));

        rbacManagementService.assignPermissionToRole(roleName, "sample:perm");

        assertThat(cache.get(tenantId + ":agent-1")).isNull();
        assertThat(cache.get(tenantId + ":agent-2")).isNull();
    }
}
