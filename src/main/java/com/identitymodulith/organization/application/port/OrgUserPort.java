package com.identitymodulith.organization.application.port;

import com.identitymodulith.organization.presentation.dto.response.DepartmentMembersResponse;
import java.util.List;
import java.util.UUID;

/** Organization 모듈에서 User 정보를 조회하기 위한 포트. */
public interface OrgUserPort {

    boolean existsActiveUserInDepartment(String tenantId, String deptId);

    java.util.Optional<OrgUserView> findOrgInfoByUserId(String tenantId, UUID userId);

    List<OrgUserView> findActiveUsersByDeptIds(String tenantId, List<String> deptIds);

    long countEmployeesByDepartment(String tenantId, String deptId);

    long countActiveEmployeesByDepartment(String tenantId, String deptId);

    List<DepartmentMembersResponse.MemberInfo> getUsersByDepartment(String tenantId, String deptId);
}
