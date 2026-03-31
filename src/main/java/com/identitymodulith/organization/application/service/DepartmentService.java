package com.identitymodulith.organization.application.service;

import com.identitymodulith.organization.domain.model.DepartmentType;
import com.identitymodulith.organization.presentation.dto.response.DepartmentMembersResponse;
import com.identitymodulith.organization.presentation.dto.response.DepartmentResponse;
import com.identitymodulith.organization.presentation.dto.response.DepartmentStatisticsResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 부서 관리 유스케이스 계약. */
public interface DepartmentService {

    DepartmentResponse createDepartment(
            String tenantId,
            UUID actorUserId,
            String name,
            DepartmentType type,
            String code,
            String customTypeName,
            String parentId);

    DepartmentResponse updateDepartment(
            String tenantId,
            UUID actorUserId,
            String deptId,
            String name,
            DepartmentType type);

    void deleteDepartment(String tenantId, UUID actorUserId, String deptId);

    void deactivateDepartment(String tenantId, UUID actorUserId, String deptId);

    void activateDepartment(String tenantId, UUID actorUserId, String deptId);

    void moveDepartment(String tenantId, UUID userId, String deptId, String newParentId);

    List<DepartmentResponse> getSubtree(String tenantId, String deptId);

    List<DepartmentResponse> getDepartmentTree(String tenantId);

    List<DepartmentResponse> getDepartmentTreeWithinScope(String tenantId, UUID userId);

    List<DepartmentResponse> searchDepartments(String tenantId, String keyword);

    List<DepartmentResponse> getDepartmentsByDepth(String tenantId, int depth);

    List<DepartmentResponse> getDepartmentsByType(String tenantId, DepartmentType type);

    DepartmentStatisticsResponse getDepartmentStatistics(String tenantId, String deptId);

    DepartmentMembersResponse getDepartmentMembers(
            String tenantId,
            String deptId,
            boolean includeSubDepts);

    Set<String> getAccessibleDepartmentIds(String tenantId, UUID userId);
}
