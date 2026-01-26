package com.nexfron.identitymodulith.organization.application.service;

import com.nexfron.identitymodulith.common.cache.CacheKeyGenerator;
import com.nexfron.identitymodulith.organization.exception.EntityNotFoundException;
import com.nexfron.identitymodulith.organization.application.port.OrgUserPort;
import com.nexfron.identitymodulith.organization.application.port.OrgUserView;
import com.nexfron.identitymodulith.organization.domain.model.Department;
import com.nexfron.identitymodulith.organization.domain.model.DataScopeLevel;
import com.nexfron.identitymodulith.organization.domain.repository.JpaDepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 데이터 범위 기반 접근 제어 서비스
 *
 * 역할별 부서 접근 범위:
 * - ADMIN: 전체 부서
 * - TEAM_LEAD: 자신의 부서 + 하위 부서
 * - MEMBER: 자신의 부서만
 *
 * 캐싱: 사용자별 접근 가능 부서 ID 집합 캐시
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrgScopeService {

    private final JpaDepartmentRepository jpaDepartmentRepository;
    private final OrgUserPort orgUserPort;

    /**
     * 사용자가 접근 가능한 부서 ID 집합 조회
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return 접근 가능한 부서 ID 집합
     */
    @Cacheable(
        value = "accessibleDepts",
        key = "T(com.nexfron.identitymodulith.common.cache.CacheKeyGenerator).accessibleDepartments(#tenantId, #userId.toString())",
        unless = "#result == null || #result.isEmpty()"
    )
    public Set<String> getAccessibleDepartmentIds(String tenantId, UUID userId) {
        log.debug("[OrgScope] 접근 가능 부서 계산 - tenantId={}, userId={}", tenantId, userId);

        OrgUserView userView = orgUserPort.findOrgInfoByUserId(tenantId, userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자의 조직 정보를 찾을 수 없습니다. userId=" + userId));

        if (!userView.isActive()) {
            throw new EntityNotFoundException("비활성화된 사용자입니다. userId=" + userId);
        }

        DataScopeLevel level = userView.getRoleLevel();
        String myDeptId = userView.getDeptId();

        if (myDeptId == null) {
            throw new EntityNotFoundException("사용자의 소속 부서를 찾을 수 없습니다. userId=" + userId);
        }

        // ADMIN: 전체 조직 조회
        if (level.canSeeWholeTenant()) {
            return jpaDepartmentRepository.findAllByTenantId(tenantId).stream()
                    .map(Department::getDeptId)
                    .collect(Collectors.toSet());
        }

        Department myDept = jpaDepartmentRepository.findByDeptIdAndTenantId(myDeptId, tenantId)
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
}
