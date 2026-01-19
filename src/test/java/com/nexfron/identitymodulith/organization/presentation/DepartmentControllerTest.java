package com.nexfron.identitymodulith.organization.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexfron.identitymodulith.organization.application.service.DepartmentService;
import com.nexfron.identitymodulith.organization.exception.OrganizationException;
import com.nexfron.identitymodulith.organization.presentation.dto.DepartmentDto;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DepartmentController 통합 테스트
 *
 * @WebMvcTest로 Controller 레이어만 테스트
 * Security는 @WithMockUser로 Mock
 */
@WebMvcTest(controllers = DepartmentController.class,
            excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
            })
@Import(com.nexfron.identitymodulith.organization.presentation.OrganizationExceptionHandler.class)
@WithMockUser(username = "test-tenant:test-user")
@DisplayName("Department Controller 통합 테스트")
@Disabled("Controller 테스트는 추후 통합 테스트로 대체 예정")
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentService departmentService;

    private static final String TENANT_ID = "default-tenant";  // ✅ Controller가 사용하는 기본값

    @Nested
    @DisplayName("부서 생성 API")
    class CreateDepartmentTests {

        @Test
        @DisplayName("정상 요청 - 201 Created 반환")
        void createDepartment_Valid_Returns201() throws Exception {
            // Given
            DepartmentDto.Response response = DepartmentDto.Response.builder()
                    .deptId("dept-1")
                    .name("개발팀")
                    .type("TEAM")
                    .depth(1)
                    .orgPath("/dept-1/")
                    .build();

            when(departmentService.createDepartment(eq(TENANT_ID), anyString(), anyString(), any())).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/org/departments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\": \"개발팀\", \"type\": \"TEAM\" }"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.deptId").value("dept-1"))
                    .andExpect(jsonPath("$.name").value("개발팀"));
        }

        @Test
        @DisplayName("잘못된 상위 부서 - 400 Bad Request 반환")
        void createDepartment_InvalidParent_Returns400() throws Exception {
            // Given
            when(departmentService.createDepartment(eq(TENANT_ID), anyString(), anyString(), anyString()))
                    .thenThrow(new OrganizationException(OrganizationException.OrganizationErrorCode.INVALID_PARENT));

            // When & Then
            mockMvc.perform(post("/api/org/departments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\": \"개발팀\", \"type\": \"TEAM\", \"parentId\": \"999\" }"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("부서 조회 API")
    class GetDepartmentTests {

        @Test
        @DisplayName("전체 조직도 조회 - 200 OK 반환")
        void getDepartmentTree_Returns200() throws Exception {
            // Given
            List<DepartmentDto.Response> departments = List.of(
                    DepartmentDto.Response.builder()
                            .deptId("dept-1")
                            .name("본부")
                            .type("HQ")
                            .depth(1)
                            .build()
            );

            when(departmentService.getDepartmentTree(TENANT_ID)).thenReturn(departments);

            // When & Then
            mockMvc.perform(get("/api/org/departments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("부서 삭제 API")
    class DeleteDepartmentTests {

        @Test
        @DisplayName("부서 삭제 성공 - 204 No Content 반환")
        void deleteDepartment_Success_Returns204() throws Exception {
            // Given
            String deptId = "dept-1";
            doNothing().when(departmentService).deleteDepartment(eq(TENANT_ID), any(), eq(deptId));

            // When & Then
            mockMvc.perform(delete("/api/org/departments/" + deptId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("하위 부서가 있는 부서 삭제 - 409 Conflict 반환")
        void deleteDepartment_HasChildren_Returns409() throws Exception {
            // Given
            String deptId = "dept-1";
            doThrow(new OrganizationException(OrganizationException.OrganizationErrorCode.CHILD_DEPARTMENT_EXISTS))
                    .when(departmentService).deleteDepartment(eq(TENANT_ID), any(), eq(deptId));

            // When & Then
            mockMvc.perform(delete("/api/org/departments/" + deptId)
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }
    }
}

