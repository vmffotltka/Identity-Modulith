package com.identitymodulith.organization.infrastructure.adapter;

import com.identitymodulith.organization.presentation.dto.response.DepartmentMembersResponse;
import com.identitymodulith.organization.application.port.OrgUserPort;
import com.identitymodulith.organization.application.port.OrgUserView;
import com.identitymodulith.common.domain.DataScopeLevel;
import com.identitymodulith.organization.infrastructure.persistence.entity.DepartmentEntity;
import com.identitymodulith.organization.infrastructure.persistence.repository.JpaDepartmentRepository;
import com.identitymodulith.user.UserModuleApi;
import com.identitymodulith.user.AgentExternalInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Organization에서 User 정보를 조회하기 위한 포트 어댑터 구현체. */
@Slf4j
@Service
@Primary
public class AgentOrgUserAdapter implements OrgUserPort {

    private final UserModuleApi userModuleApi;

    private final JpaDepartmentRepository departmentRepository;

    public AgentOrgUserAdapter(@Lazy UserModuleApi userModuleApi,
                               JpaDepartmentRepository departmentRepository) {
        this.userModuleApi = userModuleApi;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public boolean existsActiveUserInDepartment(String tenantId, String deptId) {
        return userModuleApi.findActiveAgentsByOrganizationId(tenantId, deptId).stream()
                .anyMatch(AgentExternalInfo::isActive);
    }

    @Override
    public java.util.Optional<OrgUserView> findOrgInfoByUserId(String tenantId, UUID userId) {
        return userModuleApi.findAgentById(tenantId, userId)
                .map(this::toViewFromExternal);
    }

    @Override
    public List<OrgUserView> findActiveUsersByDeptIds(String tenantId, List<String> deptIds) {
        List<OrgUserView> result = new ArrayList<>();
        for (String deptId : deptIds) {
            List<AgentExternalInfo> agents = userModuleApi.findActiveAgentsByOrganizationId(tenantId, deptId);
            agents.stream()
                    .map(this::toViewFromExternal)
                    .forEach(result::add);
        }
        return result;
    }

    @Override
    public long countEmployeesByDepartment(String tenantId, String deptId) {
        // 현재 UserModuleApi가 활성 사용자 조회만 제공하므로 동일 경로를 사용한다.
        return userModuleApi.findActiveAgentsByOrganizationId(tenantId, deptId).size();
    }

    @Override
    public long countActiveEmployeesByDepartment(String tenantId, String deptId) {
        return userModuleApi.findActiveAgentsByOrganizationId(tenantId, deptId).stream()
                .filter(AgentExternalInfo::isActive)
                .count();
    }

    @Override
    public List<DepartmentMembersResponse.MemberInfo> getUsersByDepartment(String tenantId, String deptId) {
        return userModuleApi.findActiveAgentsByOrganizationId(tenantId, deptId).stream()
                .map(agent -> new DepartmentMembersResponse.MemberInfo(
                        agent.getId().toString(),
                        agent.getLoginId(),
                        agent.getName(),
                        agent.getOrganizationId(),
                        agent.getEmployeeId() != null ? agent.getEmployeeId() : "",
                        agent.isActive() ? "ACTIVE" : "RETIRED"
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    private OrgUserView toViewFromExternal(AgentExternalInfo info) {
        String deptId = info.getOrganizationId();
        String departmentName = null;
        String departmentPath = null;

        if (deptId != null && !deptId.isEmpty()) {
            java.util.Optional<DepartmentEntity> deptOpt =
                departmentRepository.findByDeptIdAndTenantIdWithParent(deptId, info.getTenantId());

            if (deptOpt.isPresent()) {
                DepartmentEntity dept = deptOpt.get();
                departmentName = dept.getName();
                departmentPath = buildDepartmentPath(dept, info.getTenantId());
            }
        }

        return OrgUserView.builder()
                .userId(info.getId())
                .tenantId(info.getTenantId())
                .loginId(info.getLoginId())
                .name(info.getName())
                .email(info.getEmail())
                .employeeId(info.getEmployeeId())
                .deptId(deptId)
                .deptOrgPath(null)
                .departmentName(departmentName)
                .departmentPath(departmentPath)
                .roleLevel(mapRoleLevelFromExternal(info))
                .active(info.isActive())
                .build();
    }

    private String buildDepartmentPath(DepartmentEntity dept, String tenantId) {
        java.util.List<String> pathNames = new java.util.ArrayList<>();
        DepartmentEntity current = dept;

        while (current != null) {
            pathNames.add(0, current.getName());

            if (current.getParent() != null) {
                String parentId = current.getParent().getDeptId();
                current = departmentRepository.findByDeptIdAndTenantIdWithParent(parentId, tenantId)
                    .orElse(null);
            } else {
                break;
            }
        }

        return String.join(" > ", pathNames);
    }

    private DataScopeLevel mapRoleLevelFromExternal(AgentExternalInfo info) {
        List<String> roleNames = info.getRoles().stream()
                .map(AgentExternalInfo.RoleInfo::getName)
                .toList();

        log.debug("[Org] userId={} roles={}", info.getId(), roleNames);

        DataScopeLevel result = info.getRoles().stream()
                .map(role -> DataScopeLevel.fromRoleName(role.getName()))
                .max(java.util.Comparator.naturalOrder())
                .orElse(DataScopeLevel.MEMBER);

        log.debug("[Org] userId={} finalDataScopeLevel={}", info.getId(), result);
        return result;
    }
}
