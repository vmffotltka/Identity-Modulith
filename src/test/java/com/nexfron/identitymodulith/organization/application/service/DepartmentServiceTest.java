package com.nexfron.identitymodulith.organization.application.service;

import com.nexfron.identitymodulith.organization.domain.model.Department;
import com.nexfron.identitymodulith.organization.presentation.dto.DepartmentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DepartmentService 단위 테스트
 *
 * 부서 엔티티의 기본 기능을 테스트합니다:
 * - 부서 생성 및 계층 구조
 * - 경로(orgPath) 계산
 * - 깊이(depth) 자동 계산
 * - 부모 변경 시 깊이 업데이트
 * - 순환 참조 방지
 * - DTO 변환
 */
@DisplayName("부서 관리 서비스 단위 테스트")
class DepartmentServiceTest {

    private final String tenantId = "test-tenant";
    private Department rootDept;
    private Department childDept;

    @BeforeEach
    void setup() {
        rootDept = Department.create(tenantId, "총무부", "본부", null);
        childDept = Department.create(tenantId, "HR팀", "팀", rootDept);
    }

    @Test
    @DisplayName("루트 부서 생성")
    void testCreateRootDepartment() {
        assertNotNull(rootDept);
        assertEquals("총무부", rootDept.getName());
        assertEquals("본부", rootDept.getType());
        assertEquals(0, rootDept.getDepth());
    }

    @Test
    @DisplayName("자식 부서 생성")
    void testCreateChildDepartment() {
        assertNotNull(childDept);
        assertEquals("HR팀", childDept.getName());
        assertEquals("팀", childDept.getType());
        assertEquals(1, childDept.getDepth());
    }

    @Test
    @DisplayName("부서 경로 계산")
    void testDepartmentOrgPath() {
        assertNotNull(rootDept.getOrgPath());
        assertNotNull(childDept.getOrgPath());
        assertTrue(childDept.getOrgPath().contains(rootDept.getDeptId()));
    }

    @Test
    @DisplayName("부서 깊이 계산")
    void testDepartmentDepth() {
        assertEquals(0, rootDept.getDepth());
        assertEquals(1, childDept.getDepth());

        Department grandchild = Department.create(tenantId, "채용팀", "센터", childDept);
        assertEquals(2, grandchild.getDepth());
    }

    @Test
    @DisplayName("부서 부모 변경")
    void testChangeParentDepartment() {
        Department newParent = Department.create(tenantId, "개발본부", "본부", null);
        childDept.changeParent(newParent);

        assertEquals(1, childDept.getDepth());
        assertNotNull(childDept.getOrgPath());
    }

    @Test
    @DisplayName("순환 참조 방지")
    void testCircularReferencePreventionInChangeParent() {
        childDept.changeParent(rootDept);
        assertThrows(Exception.class, () -> rootDept.changeParent(childDept));
    }

    @Test
    @DisplayName("3단계 계층 구조")
    void testThreeLayerDepartmentHierarchy() {
        Department level1 = Department.create(tenantId, "총무부", "본부", null);
        Department level2 = Department.create(tenantId, "HR팀", "팀", level1);
        Department level3 = Department.create(tenantId, "채용팀", "센터", level2);

        assertEquals(0, level1.getDepth());
        assertEquals(1, level2.getDepth());
        assertEquals(2, level3.getDepth());

        assertNotNull(level1.getOrgPath());
        assertNotNull(level2.getOrgPath());
        assertNotNull(level3.getOrgPath());
    }

    @Test
    @DisplayName("Department to DTO 변환")
    void testDepartmentToDto() {
        DepartmentDto.Response dto = DepartmentDto.Response.from(rootDept);

        assertNotNull(dto);
        assertEquals("총무부", dto.getName());
        assertEquals("본부", dto.getType());
        assertEquals(0, dto.getDepth());
    }
}