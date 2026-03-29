package com.identitymodulith.rbac.application;

import com.identitymodulith.rbac.application.service.RbacQueryServiceImpl;
import com.identitymodulith.rbac.domain.RoleType;
import com.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.repository.AgentRoleJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.PermissionJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.RoleJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RbacQueryServiceImpl 단위 테스트
 *
 * <h2>테스트 범위:</h2>
 * - permissionsOfRoles: 역할별 권한 조회
 * - 테넌트 격리 검증
 * - 빈 결과 처리
 *
 * @author Test Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RbacQueryServiceImpl 단위 테스트")
class RbacQueryServiceImplTest {

    @Mock
    private AgentRoleJpaRepository agentRoleRepository;

    @Mock
    private RolePermissionJpaRepository rolePermissionRepository;

    @Mock
    private PermissionJpaRepository permissionRepository;

    @Mock
    private RoleJpaRepository roleRepository;

    @InjectMocks
    private RbacQueryServiceImpl rbacQueryService;

    private static final String TENANT_ID = "test-tenant";

    @Nested
    @DisplayName("역할별 권한 조회 테스트")
    class PermissionsOfRolesTests {

        @Test
        @DisplayName("역할에 할당된 권한을 정상적으로 조회한다")
        void permissionsOfRoles_WithValidRoles_ReturnsPermissions() {
            // Given
            Set<String> roleNames = Set.of("ADMIN", "TEAM_LEADER");

            RoleJpaEntity adminRole = RoleJpaEntity.builder()
                    .roleId("role-1")
                    .tenantId(TENANT_ID)
                    .name("ADMIN")
                    .type(RoleType.POSITION)
                    .build();

            RoleJpaEntity teamLeaderRole = RoleJpaEntity.builder()
                    .roleId("role-2")
                    .tenantId(TENANT_ID)
                    .name("TEAM_LEADER")
                    .type(RoleType.POSITION)
                    .build();

            when(roleRepository.findByTenantIdAndNameIn(TENANT_ID, roleNames))
                    .thenReturn(List.of(adminRole, teamLeaderRole));

            // ✅ List 반환 - any() 또는 구체적인 매처 사용
            when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenant(
                    any(), eq(TENANT_ID)))
                    .thenReturn(List.of("user:read", "user:create", "org:view"));

            // When
            Set<String> permissions = rbacQueryService.permissionsOfRoles(TENANT_ID, roleNames);

            // Then
            assertThat(permissions)
                    .hasSize(3)
                    .contains("user:read", "user:create", "org:view");

            verify(roleRepository, times(1)).findByTenantIdAndNameIn(TENANT_ID, roleNames);
            verify(rolePermissionRepository, times(1))
                    .findPermissionCodesByRoleIdsAndTenant(any(), eq(TENANT_ID));
        }

        @Test
        @DisplayName("역할이 비어있으면 빈 권한 집합을 반환한다")
        void permissionsOfRoles_WithEmptyRoles_ReturnsEmptySet() {
            // Given
            Set<String> roleNames = Set.of();

            // When
            Set<String> permissions = rbacQueryService.permissionsOfRoles(TENANT_ID, roleNames);

            // Then
            assertThat(permissions).isEmpty();
            verify(roleRepository, never()).findByTenantIdAndNameIn(anyString(), anySet());
        }

        @Test
        @DisplayName("권한이 없는 역할도 빈 권한 집합을 반환한다")
        void permissionsOfRoles_WithRolesButNoPermissions_ReturnsEmptySet() {
            // Given
            Set<String> roleNames = Set.of("EMPTY_ROLE");

            RoleJpaEntity emptyRole = RoleJpaEntity.builder()
                    .roleId("role-1")
                    .tenantId(TENANT_ID)
                    .name("EMPTY_ROLE")
                    .type(RoleType.POSITION)
                    .build();

            when(roleRepository.findByTenantIdAndNameIn(TENANT_ID, roleNames))
                    .thenReturn(List.of(emptyRole));
            // ✅ List 반환 - any() 사용
            when(rolePermissionRepository.findPermissionCodesByRoleIdsAndTenant(
                    any(), eq(TENANT_ID)))
                    .thenReturn(List.of());

            // When
            Set<String> permissions = rbacQueryService.permissionsOfRoles(TENANT_ID, roleNames);

            // Then
            assertThat(permissions).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 역할도 빈 권한 집합을 반환한다")
        void permissionsOfRoles_WithNonExistentRoles_ReturnsEmptySet() {
            // Given
            Set<String> roleNames = Set.of("NON_EXISTENT");

            when(roleRepository.findByTenantIdAndNameIn(TENANT_ID, roleNames))
                    .thenReturn(List.of());

            // When
            Set<String> permissions = rbacQueryService.permissionsOfRoles(TENANT_ID, roleNames);

            // Then
            assertThat(permissions).isEmpty();
        }
    }

    @Nested
    @DisplayName("경계 조건 테스트")
    class EdgeCaseTests {

        @Test
        @DisplayName("null roleNames도 빈 권한을 반환한다")
        void permissionsOfRoles_WithNullRoleNames_ReturnsEmptySet() {
            // When
            Set<String> permissions = rbacQueryService.permissionsOfRoles(TENANT_ID, null);

            // Then
            assertThat(permissions).isEmpty();
            verify(roleRepository, never()).findByTenantIdAndNameIn(anyString(), anySet());
        }
        
    }

    @Nested
    @DisplayName("사용자 권한 조회 테스트")
    class PermissionsOfAgentTests {

        @Test
        @DisplayName("단일 JOIN 조회 결과를 Set으로 중복 제거해 반환한다")
        void permissionsOf_UsesJoinProjectionAndDeduplicates() {
            // Given
            UUID agentId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            when(agentRoleRepository.findPermissionCodesByAgentIdAndTenant(agentId.toString(), TENANT_ID))
                    .thenReturn(List.of("user:read", "user:read", "org:view"));

            // When
            Set<String> permissions = rbacQueryService.permissionsOf(TENANT_ID, agentId);

            // Then
            assertThat(permissions).containsExactlyInAnyOrder("user:read", "org:view");
            verify(agentRoleRepository, times(1))
                    .findPermissionCodesByAgentIdAndTenant(agentId.toString(), TENANT_ID);
            verify(rolePermissionRepository, never()).findPermissionsByRoleIdAndTenant(anyString(), anyString());
        }

        @Test
        @DisplayName("tenantId 또는 agentId가 유효하지 않으면 빈 Set을 반환한다")
        void permissionsOf_WithInvalidInput_ReturnsEmptySet() {
            // When
            Set<String> result1 = rbacQueryService.permissionsOf(null, UUID.randomUUID());
            Set<String> result2 = rbacQueryService.permissionsOf("   ", UUID.randomUUID());
            Set<String> result3 = rbacQueryService.permissionsOf(TENANT_ID, null);

            // Then
            assertThat(result1).isEmpty();
            assertThat(result2).isEmpty();
            assertThat(result3).isEmpty();
            verifyNoInteractions(agentRoleRepository);
        }
    }
}

