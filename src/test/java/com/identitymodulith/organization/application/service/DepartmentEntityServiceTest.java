package com.identitymodulith.organization.application.service;

import com.identitymodulith.organization.application.exception.OrganizationException;
import com.identitymodulith.organization.application.exception.OrganizationException.OrganizationErrorCode;
import com.identitymodulith.organization.domain.model.DepartmentType;
import com.identitymodulith.organization.infrastructure.persistence.entity.DepartmentEntity;
import com.identitymodulith.organization.presentation.dto.DepartmentDto;
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
class DepartmentEntityServiceTest {

    private final String tenantId = "test-tenant";
    private DepartmentEntity rootDept;
    private DepartmentEntity childDept;

    @BeforeEach
    void setup() {
        rootDept = DepartmentEntity.create(tenantId, "총무부", DepartmentType.DIVISION, "ROOT-001", null, null);
        childDept = DepartmentEntity.create(tenantId, "HR팀", DepartmentType.TEAM, "HR-001", null, rootDept);
    }

    @Test
    @DisplayName("루트 부서 생성")
    void testCreateRootDepartment() {
        assertNotNull(rootDept);
        assertEquals("총무부", rootDept.getName());
        assertEquals(DepartmentType.DIVISION, rootDept.getType());
        assertEquals(0, rootDept.getDepth());
    }

    @Test
    @DisplayName("자식 부서 생성")
    void testCreateChildDepartment() {
        assertNotNull(childDept);
        assertEquals("HR팀", childDept.getName());
        assertEquals(DepartmentType.TEAM, childDept.getType());
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

        DepartmentEntity grandchild = DepartmentEntity.create(tenantId, "채용팀", DepartmentType.GROUP, "RECRUIT-001", null, childDept);
        assertEquals(2, grandchild.getDepth());
    }

    @Test
    @DisplayName("부서 부모 변경")
    void testChangeParentDepartment() {
        DepartmentEntity newParent = DepartmentEntity.create(tenantId, "개발본부", DepartmentType.DIVISION, "DEV-001", null, null);
        childDept.changeParent(newParent);

        assertEquals(1, childDept.getDepth());
        assertNotNull(childDept.getOrgPath());
    }

    @Test
    @DisplayName("순환 참조 방지 - CIRCULAR_REFERENCE 예외 발생")
    void testCircularReferencePreventionInChangeParent() {
        // given: 부모-자식 관계 설정
        childDept.changeParent(rootDept);

        // when & then: 부모가 자식의 하위로 이동하려 할 때 예외 발생
        OrganizationException exception = assertThrows(
            OrganizationException.class,
            () -> rootDept.changeParent(childDept),
            "자신의 하위 부서로 이동할 수 없어야 합니다"
        );

        // 에러 코드 검증
        assertEquals(OrganizationErrorCode.CIRCULAR_REFERENCE, exception.getErrorCode());
    }

    @Test
    @DisplayName("3단계 계층 구조")
    void testThreeLayerDepartmentHierarchy() {
        DepartmentEntity level1 = DepartmentEntity.create(tenantId, "총무부", DepartmentType.DIVISION, "ADMIN-001", null, null);
        DepartmentEntity level2 = DepartmentEntity.create(tenantId, "HR팀", DepartmentType.TEAM, "HR-002", null, level1);
        DepartmentEntity level3 = DepartmentEntity.create(tenantId, "채용팀", DepartmentType.GROUP, "RECRUIT-003", null, level2);

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
        assertEquals(DepartmentType.DIVISION, dto.getType());
        assertEquals(0, dto.getDepth());
    }
}