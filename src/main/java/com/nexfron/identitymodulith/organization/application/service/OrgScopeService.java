// organization.application.service.OrgScopeService.java
package com.nexfron.identitymodulith.organization.application.service;

import com.nexfron.identitymodulith.organization.common.exception.EntityNotFoundException;
import com.nexfron.identitymodulith.organization.application.port.OrgUserPort;
import com.nexfron.identitymodulith.organization.application.port.OrgUserView;
import com.nexfron.identitymodulith.organization.domain.model.Department;
import com.nexfron.identitymodulith.organization.domain.model.DataScopeLevel;
import com.nexfron.identitymodulith.organization.domain.repository.JpaDepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OrgScopeService
 *
 * - Level 2 RBAC (Data Scope) 핵심 로직
 * - "이 유저가 어느 부서까지 볼 수 있냐?"를 계산한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgScopeService {

    private final JpaDepartmentRepository jpaDepartmentRepository;
    private final OrgUserPort orgUserPort;

    /**
     * [핵심] 특정 유저가 접근 가능한 부서 ID 집합 계산
     *
     * - MEMBER: 본인 부서만
     * - TEAM_LEAD: 본인 부서 + 하위 부서
     * - ADMIN: 테넌트 전체 부서
     */
    public Set<Long> getAccessibleDepartmentIds(String tenantId, UUID userId) {
        OrgUserView userView = orgUserPort.findOrgInfoByUserId(tenantId, userId);
        if (userView == null || !userView.isActive()) {
            throw new EntityNotFoundException("사용자의 조직 정보를 찾을 수 없습니다.");
        }

        DataScopeLevel level = userView.getRoleLevel();
        Long myDeptId = userView.getDeptId();

        if (myDeptId == null) {
            throw new EntityNotFoundException("사용자의 소속 부서를 찾을 수 없습니다.");
        }

        // ADMIN: 전체 조직 조회
        if (level.canSeeWholeTenant()) {
            return jpaDepartmentRepository.findAllByTenantId(tenantId).stream()
                    .map(Department::getDeptId)
                    .collect(Collectors.toSet());
        }

        Department myDept = jpaDepartmentRepository.findById(myDeptId)
                .filter(d -> d.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException("사용자의 소속 부서를 찾을 수 없습니다."));

        // TEAM_LEAD: 내 부서 + 하위 부서
        if (level.canSeeSubTree()) {
            String pathPrefix = myDept.getOrgPath();
            return jpaDepartmentRepository
                    .findByTenantIdAndOrgPathStartsWith(tenantId, pathPrefix)
                    .stream()
                    .map(Department::getDeptId)
                    .collect(Collectors.toSet());
        }

        // MEMBER: 내 부서만
        return Set.of(myDept.getDeptId());
    }

    /**
     * 특정 부서를 조회/수정할 수 있는 권한이 있는지 여부
     */
    public boolean canAccessDepartment(String tenantId, UUID userId, Long targetDeptId) {
        Set<Long> scope = getAccessibleDepartmentIds(tenantId, userId);
        return scope.contains(targetDeptId);
    }

    /**
     * 특정 부서들을 조회할 수 있는 권한이 있는지 여부 (AND 조건)
     */
    public boolean canAccessAllDepartments(String tenantId, UUID userId, Collection<Long> deptIds) {
        Set<Long> scope = getAccessibleDepartmentIds(tenantId, userId);
        return deptIds.stream().allMatch(scope::contains);
    }
}
