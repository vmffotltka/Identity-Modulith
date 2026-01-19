package com.nexfron.identitymodulith.rbac.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexfron.identitymodulith.rbac.application.RbacManagementService;
import com.nexfron.identitymodulith.rbac.application.RbacManagementService.CreatePermissionRequest;
import com.nexfron.identitymodulith.rbac.application.RbacManagementService.CreateRoleRequest;
import com.nexfron.identitymodulith.rbac.application.RbacManagementService.PermissionDto;
import com.nexfron.identitymodulith.rbac.application.RbacManagementService.RoleDto;
import com.nexfron.identitymodulith.rbac.application.exception.RbacException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RbacController 통합 테스트
 *
 * <h2>테스트 범위:</h2>
 * - 역할(Role) API 엔드포인트
 * - 권한(Permission) API 엔드포인트
 * - 역할-권한 매핑 API
 * - 사용자-역할 매핑 API
 * - HTTP 상태 코드 검증
 * - JSON 응답 형식 검증
 *
 * @author Test Team
 * @version 1.0
 */
@WebMvcTest(controllers = RbacController.class,
            excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
            })
@Import(com.nexfron.identitymodulith.rbac.presentation.RbacExceptionHandler.class)
@WithMockUser(username = "test-tenant:test-user")
@DisplayName("RBAC Controller 통합 테스트")
@Disabled("Controller 테스트는 추후 통합 테스트로 대체 예정")
class RbacControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RbacManagementService rbacManagementService;

    // ============================================================
    // ??��(Role) ?�성 API ?�스??
    // ============================================================

    @Nested
    @DisplayName("??�� ?�성 API")
    class CreateRoleTests {

        @Test
        @DisplayName("?�상 ?�청 - 201 Created 반환")
        void createRole_ValidRequest_Returns201() throws Exception {
            // Given
            CreateRoleRequest request = new CreateRoleRequest("ADMIN", "POSITION");
            RoleDto response = new RoleDto("ADMIN", "POSITION", null, true);

            when(rbacManagementService.createRole(any())).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/rbac/roles")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("ADMIN"))
                    .andExpect(jsonPath("$.type").value("POSITION"));

            verify(rbacManagementService, times(1)).createRole(any());
        }

        @Test
        @DisplayName("중복 ??�� - 409 Conflict 반환")
        void createRole_Duplicate_Returns409() throws Exception {
            // Given
            CreateRoleRequest request = new CreateRoleRequest("ADMIN", "POSITION");

            when(rbacManagementService.createRole(any()))
                    .thenThrow(new RbacException(RbacException.RbacErrorCode.ROLE_ALREADY_EXISTS));

            // When & Then
            mockMvc.perform(post("/api/rbac/roles")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    // ============================================================
    // ??��(Role) 조회 API ?�스??
    // ============================================================

    @Nested
    @DisplayName("??�� 조회 API")
    class GetRoleTests {

        @Test
        @DisplayName("모든 ??�� 조회 - 200 OK 반환")
        void getAllRoles_Returns200() throws Exception {
            // Given
            List<RoleDto> roles = List.of(
                    new RoleDto("ADMIN", "POSITION", "관리자", true),
                    new RoleDto("MANAGER", "POSITION", "매니?�", true)
            );

            when(rbacManagementService.getAllRoles()).thenReturn(roles);

            // When & Then
            mockMvc.perform(get("/api/rbac/roles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("ADMIN"))
                    .andExpect(jsonPath("$[1].name").value("MANAGER"));
        }

        @Test
        @DisplayName("?�정 ??�� 조회 - 200 OK 반환")
        void getRoleByName_Exists_Returns200() throws Exception {
            // Given
            RoleDto response = new RoleDto("ADMIN", "POSITION", "관리자", true);
            when(rbacManagementService.getRoleByName("ADMIN")).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/rbac/roles/ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("ADMIN"))
                    .andExpect(jsonPath("$.type").value("POSITION"));
        }

        @Test
        @DisplayName("존재?��? ?�는 ??�� 조회 - 404 Not Found 반환")
        void getRoleByName_NotFound_Returns404() throws Exception {
            // Given
            when(rbacManagementService.getRoleByName("UNKNOWN"))
                    .thenThrow(new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

            // When & Then
            mockMvc.perform(get("/api/rbac/roles/UNKNOWN"))
                    .andExpect(status().isNotFound());
        }
    }

    // ============================================================
    // ??��(Role) ??�� API ?�스??
    // ============================================================

    @Nested
    @DisplayName("??�� ??�� API")
    class DeleteRoleTests {

        @Test
        @DisplayName("??�� ??�� ?�공 - 200 OK 반환")
        void deleteRole_Success_Returns200() throws Exception {
            // Given
            RbacManagementService.RoleDeletionResult result =
                    new RbacManagementService.RoleDeletionResult("ADMIN", 0, 5, false, null);

            when(rbacManagementService.deleteRole(eq("ADMIN"), eq(false))).thenReturn(result);

            // When & Then
            mockMvc.perform(delete("/api/rbac/roles/ADMIN").with(csrf())
                            .param("force", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roleName").value("ADMIN"))
                    .andExpect(jsonPath("$.affectedUserCount").value(0))
                    .andExpect(jsonPath("$.removedPermissionCount").value(5));
        }

        @Test
        @DisplayName("?�용?��? ?�는 ??�� ??�� ?�도 - 409 Conflict 반환")
        void deleteRole_HasUsers_Returns409() throws Exception {
            // Given
            when(rbacManagementService.deleteRole(eq("ADMIN"), eq(false)))
                    .thenThrow(new RbacException(RbacException.RbacErrorCode.ROLE_HAS_USERS));

            // When & Then
            mockMvc.perform(delete("/api/rbac/roles/ADMIN").with(csrf())
                            .param("force", "false"))
                    .andExpect(status().isConflict());
        }
    }

    // ============================================================
    // 권한(Permission) API ?�스??
    // ============================================================

    @Nested
    @DisplayName("권한 API")
    class PermissionTests {

        @Test
        @DisplayName("권한 ?�성 - 201 Created 반환")
        void createPermission_Valid_Returns201() throws Exception {
            // Given
            CreatePermissionRequest request = new CreatePermissionRequest("user:create", "?�용???�성 권한");
            PermissionDto response = new PermissionDto("user:create", "?�용???�성 권한");

            when(rbacManagementService.createPermission(any())).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/rbac/permissions").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("user:create"))
                    .andExpect(jsonPath("$.description").value("?�용???�성 권한"));
        }

        @Test
        @DisplayName("모든 권한 조회 - 200 OK 반환")
        void getAllPermissions_Returns200() throws Exception {
            // Given
            List<PermissionDto> permissions = List.of(
                    new PermissionDto("user:create", "?�용???�성"),
                    new PermissionDto("user:read", "?�용??조회")
            );

            when(rbacManagementService.getAllPermissions()).thenReturn(permissions);

            // When & Then
            mockMvc.perform(get("/api/rbac/permissions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    // ============================================================
    // ??��-권한 매핑 API ?�스??
    // ============================================================

    @Nested
    @DisplayName("??��-권한 매핑 API")
    class RolePermissionMappingTests {

        @Test
        @DisplayName("??��??권한 ?�당 - 200 OK 반환")
        void assignPermissionToRole_Valid_Returns200() throws Exception {
            // Given
            doNothing().when(rbacManagementService)
                    .assignPermissionToRole("ADMIN", "user:create");

            // When & Then
            mockMvc.perform(post("/api/rbac/roles/ADMIN/permissions/user:create").with(csrf()))
                    .andExpect(status().isOk());

            verify(rbacManagementService, times(1))
                    .assignPermissionToRole("ADMIN", "user:create");
        }

        @Test
        @DisplayName("?��? ?�당??권한 - 409 Conflict 반환")
        void assignPermissionToRole_AlreadyAssigned_Returns409() throws Exception {
            // Given
            doThrow(new RbacException(RbacException.RbacErrorCode.PERMISSION_ALREADY_ASSIGNED))
                    .when(rbacManagementService)
                    .assignPermissionToRole("ADMIN", "user:create");

            // When & Then
            mockMvc.perform(post("/api/rbac/roles/ADMIN/permissions/user:create").with(csrf()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("??��?�서 권한 ?�수 - 200 OK 반환")
        void revokePermissionFromRole_Valid_Returns200() throws Exception {
            // Given
            doNothing().when(rbacManagementService)
                    .revokePermissionFromRole("ADMIN", "user:create");

            // When & Then
            mockMvc.perform(delete("/api/rbac/roles/ADMIN/permissions/user:create").with(csrf()))
                    .andExpect(status().isOk());

            verify(rbacManagementService, times(1))
                    .revokePermissionFromRole("ADMIN", "user:create");
        }
    }

    // ============================================================
    // ?�용????�� 매핑 API ?�스??
    // ============================================================

    @Nested
    @DisplayName("?�용????�� 매핑 API")
    class AgentRoleMappingTests {

        @Test
        @DisplayName("?�용?�에�???�� ?�당 - 200 OK 반환")
        void assignRoleToAgent_Valid_Returns200() throws Exception {
            // Given
            String agentId = "user-123";
            doNothing().when(rbacManagementService)
                    .assignRoleToAgent(agentId, "ADMIN");

            // When & Then
            mockMvc.perform(post("/api/rbac/agents/" + agentId + "/roles/ADMIN").with(csrf()))
                    .andExpect(status().isOk());

            verify(rbacManagementService, times(1))
                    .assignRoleToAgent(agentId, "ADMIN");
        }

        @Test
        @DisplayName("?��? ?�당????�� - 409 Conflict 반환")
        void assignRoleToAgent_AlreadyAssigned_Returns409() throws Exception {
            // Given
            String agentId = "user-123";
            doThrow(new RbacException(RbacException.RbacErrorCode.PERMISSION_ALREADY_ASSIGNED))
                    .when(rbacManagementService)
                    .assignRoleToAgent(agentId, "ADMIN");

            // When & Then
            mockMvc.perform(post("/api/rbac/agents/" + agentId + "/roles/ADMIN").with(csrf()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("?�용?�에게서 ??�� ?�수 - 200 OK 반환")
        void revokeRoleFromAgent_Valid_Returns200() throws Exception {
            // Given
            String agentId = "user-123";
            doNothing().when(rbacManagementService)
                    .revokeRoleFromAgent(agentId, "ADMIN");

            // When & Then
            mockMvc.perform(delete("/api/rbac/agents/" + agentId + "/roles/ADMIN").with(csrf()))
                    .andExpect(status().isOk());

            verify(rbacManagementService, times(1))
                    .revokeRoleFromAgent(agentId, "ADMIN");
        }
    }
}

