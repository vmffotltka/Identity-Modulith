package com.nexfron.identitymodulith.rbac.application;

import com.nexfron.identitymodulith.rbac.application.service.RbacQueryServiceImpl;
import com.nexfron.identitymodulith.rbac.domain.RoleType;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.AgentRoleJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.PermissionJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.RoleJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

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
}

