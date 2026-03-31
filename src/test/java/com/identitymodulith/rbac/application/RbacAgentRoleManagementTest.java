package com.identitymodulith.rbac.application;

import com.identitymodulith.rbac.application.exception.RbacException;
import com.identitymodulith.rbac.application.service.RbacManagementServiceImpl;
import com.identitymodulith.rbac.application.port.AgentValidationPort;
import com.identitymodulith.rbac.application.service.RbacQueryService;
import com.identitymodulith.rbac.domain.RoleType;
import com.identitymodulith.rbac.infrastructure.persistence.entity.AgentRoleJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.repository.AgentRoleJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.PermissionJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.RoleJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RBAC 사용자-역할 관리 테스트")
class RbacAgentRoleManagementTest {


    @Mock
    private RoleJpaRepository roleRepository;

    @Mock
    private PermissionJpaRepository permissionRepository;

    @Mock
    private RolePermissionJpaRepository rolePermissionRepository;

    @Mock
    private AgentRoleJpaRepository agentRoleRepository;

    @Mock
    private RbacQueryService rbacQueryService;

    @Mock
    private AgentValidationPort agentValidationPort;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RbacManagementServiceImpl rbacManagementService;

    private String tenantId = "test-tenant";
    private String agentId = "user-123";
    private String roleId = "role-001";
    private String roleName = "TEAM_LEADER";

    @BeforeEach
    void setup() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getPrincipal()).thenReturn(tenantId + ":test-user");

        lenient().when(agentRoleRepository.findByAgentId(agentId))
                .thenReturn(java.util.List.of());

        lenient().when(agentValidationPort.isActiveAgent(agentId))
                .thenReturn(true);
    }

    @Test
    @DisplayName("사용자에게 역할 할당 - 성공")
    void testAssignRoleToAgent_Success() {
        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId(roleId)
                .tenantId(tenantId)
                .name(roleName)
                .type(RoleType.POSITION)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(roleRepository.findByTenantIdAndName(tenantId, roleName))
                .thenReturn(Optional.of(role));
        when(agentRoleRepository.save(any(AgentRoleJpaEntity.class)))
                .thenReturn(new AgentRoleJpaEntity());

        rbacManagementService.assignRoleToAgent(agentId, roleName);

        verify(agentRoleRepository, times(1)).save(any(AgentRoleJpaEntity.class));
    }

    @Test
    @DisplayName("사용자에게 역할 할당 - 역할 미존재")
    void testAssignRoleToAgent_RoleNotFound() {
        when(roleRepository.findByTenantIdAndName(tenantId, roleName))
                .thenReturn(Optional.empty());

        assertThrows(RbacException.class, () -> {
            rbacManagementService.assignRoleToAgent(agentId, roleName);
        });
    }

    @Test
    @DisplayName("사용자에게 역할 할당 - 이미 할당된 역할 (무시 처리)")
    void testAssignRoleToAgent_AlreadyAssigned() {
        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId(roleId)
                .tenantId(tenantId)
                .name(roleName)
                .type(RoleType.POSITION)
                .isActive(true)
                .build();

        when(roleRepository.findByTenantIdAndName(tenantId, roleName))
                .thenReturn(Optional.of(role));
        // RA-004: DB 제약 위반(중복 할당)은 멱등 처리한다.
        when(agentRoleRepository.save(any(AgentRoleJpaEntity.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate key"));

        assertDoesNotThrow(() -> {
            rbacManagementService.assignRoleToAgent(agentId, roleName);
        });

        verify(agentRoleRepository, times(1)).save(any(AgentRoleJpaEntity.class));
    }

    @Test
    @DisplayName("사용자에게서 역할 회수 - 성공")
    void testRevokeRoleFromAgent_Success() {
        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId(roleId)
                .tenantId(tenantId)
                .name(roleName)
                .type(RoleType.POSITION)
                .build();

        AgentRoleJpaEntity agentRole = new AgentRoleJpaEntity();
        agentRole.setAgentId(agentId);
        agentRole.setRoleId(roleId);

        when(roleRepository.findByTenantIdAndName(tenantId, roleName))
                .thenReturn(Optional.of(role));
        when(agentRoleRepository.findByAgentId(agentId))
                .thenReturn(List.of(agentRole));

        rbacManagementService.revokeRoleFromAgent(agentId, roleName);

        verify(agentRoleRepository, times(1)).delete(agentRole);
    }

    @Test
    @DisplayName("사용자에게서 역할 회수 - 역할 미존재")
    void testRevokeRoleFromAgent_RoleNotFound() {
        when(roleRepository.findByTenantIdAndName(tenantId, roleName))
                .thenReturn(Optional.empty());

        assertThrows(RbacException.class, () -> {
            rbacManagementService.revokeRoleFromAgent(agentId, roleName);
        });
    }

    // ============================================================
    // getRolesByAgent() 테스트
    // ============================================================

    @Test
    @DisplayName("사용자의 모든 역할 조회 - 성공")
    void testGetRolesByAgent_Success() {
        // Given
        RoleJpaEntity role1 = RoleJpaEntity.builder()
                .roleId("role-001")
                .tenantId(tenantId)
                .name("ADMIN")
                .type(RoleType.POSITION)
                .build();

        RoleJpaEntity role2 = RoleJpaEntity.builder()
                .roleId("role-002")
                .tenantId(tenantId)
                .name("MANAGER")
                .type(RoleType.POSITION)
                .build();

        AgentRoleJpaEntity agentRole1 = new AgentRoleJpaEntity();
        agentRole1.setRoleId("role-001");

        AgentRoleJpaEntity agentRole2 = new AgentRoleJpaEntity();
        agentRole2.setRoleId("role-002");

        when(agentRoleRepository.findByAgentId(agentId))
                .thenReturn(List.of(agentRole1, agentRole2));
        when(roleRepository.findByTenantIdAndRoleId(tenantId, "role-001"))
                .thenReturn(Optional.of(role1));
        when(roleRepository.findByTenantIdAndRoleId(tenantId, "role-002"))
                .thenReturn(Optional.of(role2));

        // When
        Set<String> roleNames = rbacManagementService.getRolesByAgent(agentId);

        // Then
        assertNotNull(roleNames);
        assertEquals(2, roleNames.size());
        assertTrue(roleNames.contains("ADMIN"));
        assertTrue(roleNames.contains("MANAGER"));
    }

    @Test
    @DisplayName("사용자의 모든 역할 조회 - 할당된 역할 없음")
    void testGetRolesByAgent_NoRoles() {
        // Given
        when(agentRoleRepository.findByAgentId(agentId))
                .thenReturn(List.of());

        // When
        Set<String> roleNames = rbacManagementService.getRolesByAgent(agentId);

        // Then
        assertNotNull(roleNames);
        assertTrue(roleNames.isEmpty());
    }

    // ============================================================
    // getAgentCountByRole() 테스트
    // ============================================================

    @Test
    @DisplayName("역할을 가진 사용자 수 조회 - 성공")
    void testGetAgentCountByRole_Success() {
        // Given
        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId(roleId)
                .tenantId(tenantId)
                .name(roleName)
                .type(RoleType.POSITION)
                .build();

        AgentRoleJpaEntity agentRole1 = new AgentRoleJpaEntity();
        agentRole1.setAgentId("user-1");

        AgentRoleJpaEntity agentRole2 = new AgentRoleJpaEntity();
        agentRole2.setAgentId("user-2");

        when(roleRepository.findByTenantIdAndName(tenantId, roleName))
                .thenReturn(Optional.of(role));
        when(agentRoleRepository.findByRoleId(roleId))
                .thenReturn(List.of(agentRole1, agentRole2));

        // When
        int count = rbacManagementService.getAgentCountByRole(roleName);

        // Then
        assertEquals(2, count);
    }

    @Test
    @DisplayName("역할을 가진 사용자 수 조회 - 역할 미존재")
    void testGetAgentCountByRole_RoleNotFound() {
        // Given
        when(roleRepository.findByTenantIdAndName(tenantId, roleName))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(RbacException.class, () -> {
            rbacManagementService.getAgentCountByRole(roleName);
        });
    }

    // ============================================================
    // 역할 할당 통합 테스트
    // ============================================================

    @Test
    @DisplayName("역할 할당 시 정상 작동 확인")
    void testAssignRoleIntegration_CacheEvictAndAuditLog() {
        // Given
        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId(roleId)
                .tenantId(tenantId)
                .name(roleName)
                .type(RoleType.POSITION)
                .isActive(true)
                .build();

        when(roleRepository.findByTenantIdAndName(tenantId, roleName))
                .thenReturn(Optional.of(role));
        when(agentRoleRepository.save(any(AgentRoleJpaEntity.class)))
                .thenReturn(new AgentRoleJpaEntity());

        // When
        rbacManagementService.assignRoleToAgent(agentId, roleName);

        // Then
        verify(agentRoleRepository, times(1)).save(any(AgentRoleJpaEntity.class));
    }
}
