package com.nexfron.identitymodulith.rbac.application;

import com.nexfron.identitymodulith.rbac.application.exception.RbacException;
import com.nexfron.identitymodulith.rbac.application.service.RbacManagementService;
import com.nexfron.identitymodulith.rbac.application.service.RbacManagementServiceImpl;
import com.nexfron.identitymodulith.rbac.domain.RoleType;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.AgentRoleJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.PermissionJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.RoleJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionJpaRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RbacManagementServiceImpl 단위 테스트
 *
 * <h2>테스트 범위:</h2>
 * - 역할(Role) CRUD 기능
 * - 권한(Permission) CRUD 기능
 * - 역할-권한 관계 관리
 * - 예외 처리
 * - 데이터 무결성
 *
 * @author Test Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RBAC 관리 서비스 단위 테스트")
class RbacManagementServiceImplTest {

    @Mock
    private RoleJpaRepository roleRepository;

    @Mock
    private PermissionJpaRepository permissionRepository;

    @Mock
    private RolePermissionJpaRepository rolePermissionRepository;

    @Mock
    private AgentRoleJpaRepository agentRoleRepository;


    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RbacManagementServiceImpl rbacManagementService;

    private String tenantId = "test-tenant";
    private String userId = "test-user";
    private String roleId = UUID.randomUUID().toString();
    private String permissionId = UUID.randomUUID().toString();

    @BeforeEach
    void setup() {
        // SecurityContext 설정 - "tenantId:userId" 형식으로 Principal 설정
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        // TenantContextHolder가 인식하는 "tenantId:userId" 형식
        lenient().when(authentication.getPrincipal()).thenReturn(tenantId + ":" + userId);
    }

    // ============================================================
    // 역할(Role) 테스트
    // ============================================================

    @Test
    @DisplayName("모든 역할 조회 - 성공")
    void testGetAllRoles_Success() {
        // Given
        RoleJpaEntity role1 = RoleJpaEntity.builder()
                .roleId(roleId)
                .tenantId(tenantId)
                .name("ADMIN")
                .type(RoleType.POSITION)
                .createdAt(LocalDateTime.now())
                .build();

        when(roleRepository.findByTenantId(tenantId))
                .thenReturn(java.util.List.of(role1));

        // When
        var result = rbacManagementService.getAllRoles();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ADMIN", result.get(0).name());
        verify(roleRepository, times(1)).findByTenantId(tenantId);
    }

    @Test
    @DisplayName("역할명으로 역할 조회 - 성공")
    void testGetRoleByName_Success() {
        // Given
        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId(roleId)
                .tenantId(tenantId)
                .name("ADMIN")
                .type(RoleType.POSITION)
                .createdAt(LocalDateTime.now())
                .build();

        when(roleRepository.findByTenantIdAndName(tenantId, "ADMIN"))
                .thenReturn(Optional.of(role));

        // When
        var result = rbacManagementService.getRoleByName("ADMIN");

        // Then
        assertNotNull(result);
        assertEquals("ADMIN", result.name());
        assertEquals(RoleType.POSITION, result.type());
    }

    @Test
    @DisplayName("역할명으로 역할 조회 - 실패 (역할 없음)")
    void testGetRoleByName_NotFound() {
        // Given
        when(roleRepository.findByTenantIdAndName(tenantId, "NONEXISTENT"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(RbacException.class, () -> {
            rbacManagementService.getRoleByName("NONEXISTENT");
        });
    }

    @Test
    @DisplayName("역할 생성 - 성공")
    void testCreateRole_Success() {
        // Given
        var request = new RbacManagementService.CreateRoleRequest("NEW_ROLE", RoleType.POSITION);

        // ADMIN 권한 검증을 위한 Mock 설정
        RoleJpaEntity adminRole = RoleJpaEntity.builder()
                .roleId("admin-role-id")
                .name("ADMIN")
                .type(RoleType.POSITION)
                .build();

        when(agentRoleRepository.findByAgentId(userId))
                .thenReturn(java.util.List.of(
                    com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.AgentRoleJpaEntity.builder()
                        .roleId("admin-role-id")
                        .build()
                ));

        when(roleRepository.findById("admin-role-id"))
                .thenReturn(Optional.of(adminRole));

        when(roleRepository.existsByTenantIdAndName(tenantId, "NEW_ROLE"))
                .thenReturn(false);

        RoleJpaEntity savedRole = RoleJpaEntity.builder()
                .roleId(roleId)
                .tenantId(tenantId)
                .name("NEW_ROLE")
                .type(RoleType.POSITION)
                .createdAt(LocalDateTime.now())
                .build();

        when(roleRepository.save(any(RoleJpaEntity.class)))
                .thenReturn(savedRole);

        // When
        var result = rbacManagementService.createRole(request);

        // Then
        assertNotNull(result);
        assertEquals("NEW_ROLE", result.name());
        verify(roleRepository, times(1)).save(any(RoleJpaEntity.class));
    }

    @Test
    @DisplayName("역할 생성 - 실패 (중복 역할명)")
    void testCreateRole_Duplicate() {
        // Given
        var request = new RbacManagementService.CreateRoleRequest("ADMIN", RoleType.POSITION);

        // ADMIN 권한 검증을 위한 Mock 설정
        RoleJpaEntity adminRole = RoleJpaEntity.builder()
                .roleId("admin-role-id")
                .name("ADMIN")
                .type(RoleType.POSITION)
                .build();

        when(agentRoleRepository.findByAgentId(userId))
                .thenReturn(java.util.List.of(
                    com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.AgentRoleJpaEntity.builder()
                        .roleId("admin-role-id")
                        .build()
                ));

        when(roleRepository.findById("admin-role-id"))
                .thenReturn(Optional.of(adminRole));

        when(roleRepository.existsByTenantIdAndName(tenantId, "ADMIN"))
                .thenReturn(true);

        // When & Then
        assertThrows(RbacException.class, () -> {
            rbacManagementService.createRole(request);
        });
    }

    @Test
    @DisplayName("역할 삭제 - 성공")
    void testDeleteRole_Success() {
        // Given
        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId(roleId)
                .tenantId(tenantId)
                .name("ADMIN")
                .type(RoleType.POSITION)
                .createdAt(LocalDateTime.now())
                .build();

        when(roleRepository.findByTenantIdAndName(tenantId, "ADMIN"))
                .thenReturn(Optional.of(role));

        // When
        rbacManagementService.deleteRole("ADMIN", false);

        // Then
        verify(rolePermissionRepository, times(1)).deleteByRoleId(roleId);
        verify(roleRepository, times(1)).delete(role);
    }

    // ============================================================
    // 권한(Permission) 테스트
    // ============================================================

    @Test
    @DisplayName("모든 권한 조회 - 성공")
    void testGetAllPermissions_Success() {
        // Given
        PermissionJpaEntity permission = PermissionJpaEntity.builder()
                .permissionId(permissionId)
                .tenantId(tenantId)
                .code("user:create")
                .createdAt(LocalDateTime.now())
                .build();

        when(permissionRepository.findByTenantId(tenantId))
                .thenReturn(java.util.List.of(permission));

        // When
        var result = rbacManagementService.getAllPermissions();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("user:create", result.get(0).code());
    }

    @Test
    @DisplayName("권한 생성 - 성공")
    void testCreatePermission_Success() {
        // Given
        var request = new RbacManagementService.CreatePermissionRequest("user:manage", "사용자 관리 권한", "WRITE");

        lenient().when(permissionRepository.existsByTenantIdAndCode(tenantId, "user:manage"))
                .thenReturn(false);

        PermissionJpaEntity savedPermission = PermissionJpaEntity.builder()
                .permissionId(permissionId)
                .tenantId(tenantId)
                .code("user:manage")
                .description("사용자 관리 권한")
                .category("WRITE")
                .createdAt(LocalDateTime.now())
                .build();

        lenient().when(permissionRepository.save(any(PermissionJpaEntity.class)))
                .thenReturn(savedPermission);

        // When
        var result = rbacManagementService.createPermission(request);

        // Then
        assertNotNull(result);
        assertEquals("user:manage", result.code());
    }

    @Test
    @DisplayName("권한 생성 - 실패 (중복 권한)")
    void testCreatePermission_Duplicate() {
        // Given
        var request = new RbacManagementService.CreatePermissionRequest("user:create", "사용자 생성 권한", "WRITE");

        lenient().when(permissionRepository.existsByTenantIdAndCode(tenantId, "user:create"))
                .thenReturn(true);

        // When & Then
        assertThrows(RbacException.class, () -> {
            rbacManagementService.createPermission(request);
        });
    }

    // ============================================================
    // 역할-권한 관계 테스트
    // ============================================================

    @Test
    @DisplayName("역할에 권한 할당 - 성공")
    void testAssignPermissionToRole_Success() {
        // Given
        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId(roleId)
                .tenantId(tenantId)
                .name("ADMIN")
                .type(RoleType.POSITION)
                .build();

        PermissionJpaEntity permission = PermissionJpaEntity.builder()
                .permissionId(permissionId)
                .tenantId(tenantId)
                .code("user:manage")
                .build();

        // ADMIN 권한 검증을 위한 Mock 설정
        RoleJpaEntity adminRole = RoleJpaEntity.builder()
                .roleId("admin-role-id")
                .name("ADMIN")
                .type(RoleType.POSITION)
                .build();

        when(agentRoleRepository.findByAgentId(userId))
                .thenReturn(java.util.List.of(
                    com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.AgentRoleJpaEntity.builder()
                        .roleId("admin-role-id")
                        .build()
                ));

        when(roleRepository.findById("admin-role-id"))
                .thenReturn(Optional.of(adminRole));

        when(roleRepository.findByTenantIdAndName(tenantId, "ADMIN"))
                .thenReturn(Optional.of(role));
        when(permissionRepository.findByTenantIdAndCode(tenantId, "user:manage"))
                .thenReturn(Optional.of(permission));

        // When
        rbacManagementService.assignPermissionToRole("ADMIN", "user:manage");

        // Then
        verify(rolePermissionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("역할에 권한 할당 - 실패 (이미 할당됨)")
    void testAssignPermissionToRole_AlreadyAssigned() {
        // Given
        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId(roleId)
                .tenantId(tenantId)
                .name("ADMIN")
                .type(RoleType.POSITION)
                .build();

        PermissionJpaEntity permission = PermissionJpaEntity.builder()
                .permissionId(permissionId)
                .tenantId(tenantId)
                .code("user:manage")
                .build();

        // ADMIN 권한 검증을 위한 Mock 설정
        RoleJpaEntity adminRole = RoleJpaEntity.builder()
                .roleId("admin-role-id")
                .name("ADMIN")
                .type(RoleType.POSITION)
                .build();

        when(agentRoleRepository.findByAgentId(userId))
                .thenReturn(java.util.List.of(
                    com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.AgentRoleJpaEntity.builder()
                        .roleId("admin-role-id")
                        .build()
                ));

        when(roleRepository.findById("admin-role-id"))
                .thenReturn(Optional.of(adminRole));

        when(roleRepository.findByTenantIdAndName(tenantId, "ADMIN"))
                .thenReturn(Optional.of(role));
        when(permissionRepository.findByTenantIdAndCode(tenantId, "user:manage"))
                .thenReturn(Optional.of(permission));
        // ✅ P0: DataIntegrityViolationException 시뮬레이션
        when(rolePermissionRepository.save(any(RolePermissionJpaEntity.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate key"));

        // When & Then
        assertThrows(RbacException.class, () -> {
            rbacManagementService.assignPermissionToRole("ADMIN", "user:manage");
        });
    }

    @Test
    @DisplayName("역할에서 권한 회수 - 성공")
    void testRevokePermissionFromRole_Success() {
        // Given
        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId(roleId)
                .tenantId(tenantId)
                .name("ADMIN")
                .type(RoleType.POSITION)
                .build();

        PermissionJpaEntity permission = PermissionJpaEntity.builder()
                .permissionId(permissionId)
                .tenantId(tenantId)
                .code("user:manage")
                .build();

        when(roleRepository.findByTenantIdAndName(tenantId, "ADMIN"))
                .thenReturn(Optional.of(role));
        when(permissionRepository.findByTenantIdAndCode(tenantId, "user:manage"))
                .thenReturn(Optional.of(permission));

        // When
        rbacManagementService.revokePermissionFromRole("ADMIN", "user:manage");

        // Then
        verify(rolePermissionRepository, times(1))
                .deleteByRoleIdAndPermissionId(roleId, permissionId);
    }
}

