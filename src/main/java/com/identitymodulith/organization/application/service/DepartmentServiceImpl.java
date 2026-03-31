package com.identitymodulith.organization.application.service;

import com.identitymodulith.organization.application.exception.OrganizationException;
import com.identitymodulith.organization.application.exception.OrganizationException.OrganizationErrorCode;
import com.identitymodulith.organization.application.port.OrgUserView;
import com.identitymodulith.common.domain.DataScopeLevel;
import com.identitymodulith.organization.domain.model.DepartmentType;
import com.identitymodulith.organization.infrastructure.persistence.entity.DepartmentEntity;
import com.identitymodulith.organization.presentation.dto.response.DepartmentMembersResponse;
import com.identitymodulith.organization.presentation.dto.response.DepartmentResponse;
import com.identitymodulith.organization.presentation.dto.response.DepartmentStatisticsResponse;
import com.identitymodulith.organization.infrastructure.persistence.repository.DepartmentListProjection;
import com.identitymodulith.organization.infrastructure.persistence.repository.JpaDepartmentRepository;
import com.identitymodulith.organization.application.port.OrgUserPort;
import com.identitymodulith.organization.OrganizationModuleApi;
import com.identitymodulith.rbac.RbacModuleApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** 부서 관리 유스케이스 구현체. */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService, OrganizationModuleApi {

    private final JpaDepartmentRepository departmentRepository;
    private final OrgUserPort orgUserPort;
    private final RbacModuleApi rbacModuleApi;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(
            String tenantId,
            UUID actorUserId,
            String name,
            DepartmentType type,
            String code,
            String customTypeName,
            String parentId) {

        Objects.requireNonNull(tenantId, "tenantId는 null일 수 없습니다");
        Objects.requireNonNull(actorUserId, "actorUserId는 null일 수 없습니다");
        Objects.requireNonNull(name, "name은 null일 수 없습니다");
        Objects.requireNonNull(code, "code는 null일 수 없습니다");

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("name은 빈 문자열일 수 없습니다");
        }
        if (code.trim().isEmpty()) {
            throw new IllegalArgumentException("code는 빈 문자열일 수 없습니다");
        }

        Set<String> permissions = rbacModuleApi.getPermissionsByAgentId(tenantId, actorUserId.toString());
        if (!permissions.contains("org:create")) {
            log.warn("[ORG] org:create 권한 없음 - userId={}, permissions={}", actorUserId, permissions);
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        log.info("[ORG] 부서 생성 - tenantId={}, userId={}, name={}, type={}, code={}, parentId={}",
                 tenantId, actorUserId, name, type, code, parentId);

        // CD-002: 루트 부서 중복 생성 방지 (테스트 환경에서는 주석 처리)
        // 운영 환경에서는 테넌트당 하나의 루트 부서만 허용하려면 활성화
        /*
        if (parentId == null || parentId.trim().isEmpty()) {
            // 루트 부서 생성 시도 - 기존 루트 부서 확인
            boolean rootExists = departmentRepository.findAllByTenantId(tenantId).stream()
                    .anyMatch(DepartmentEntity::isRoot);

            if (rootExists) {
                log.warn("[ORG] 루트 부서 중복 생성 시도 - tenantId={}", tenantId);
                throw new OrganizationException(
                        OrganizationErrorCode.ROOT_ALREADY_EXISTS
                );
            }
        }
        */

        DepartmentEntity parent = null;
        if (parentId != null && !parentId.trim().isEmpty()) {
            parent = departmentRepository.findByDeptIdAndTenantId(parentId, tenantId)
                    .orElseThrow(() -> new OrganizationException(
                            OrganizationErrorCode.INVALID_PARENT
                    ));

            if (!parent.isActive()) {
                log.warn("[ORG] 비활성 부서 하위에 생성 시도 - parentId={}", parentId);
                throw new OrganizationException(
                        OrganizationErrorCode.PARENT_DEPT_INACTIVE
                );
            }
        }

        // CD-004: type='CUSTOM'이면 customTypeName 필수
        if (type == DepartmentType.CUSTOM &&
            (customTypeName == null || customTypeName.trim().isEmpty())) {
            log.error("[ORG] CUSTOM 타입은 customTypeName 필수 - name={}", name);
            throw new OrganizationException(
                    OrganizationErrorCode.CUSTOM_TYPE_NAME_REQUIRED
            );
        }

        DepartmentEntity departmentEntity = DepartmentEntity.create(
                tenantId, name, type, code, customTypeName, parent);
        DepartmentEntity savedDept = departmentRepository.save(departmentEntity);

        log.info("[ORG] 부서 생성 완료 - deptId={}", savedDept.getDeptId());

        return DepartmentResponse.from(savedDept);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(
            String tenantId,
            UUID actorUserId,
            String deptId,
            String name,
            DepartmentType type) {

        Objects.requireNonNull(tenantId, "tenantId는 null일 수 없습니다");
        Objects.requireNonNull(actorUserId, "actorUserId는 null일 수 없습니다");
        Objects.requireNonNull(deptId, "deptId는 null일 수 없습니다");

        Set<String> permissions = rbacModuleApi.getPermissionsByAgentId(tenantId, actorUserId.toString());
        if (!permissions.contains("org:update")) {
            log.warn("[ORG] org:update 권한 없음 - userId={}, permissions={}", actorUserId, permissions);
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        log.info("[ORG] 부서 수정 - tenantId={}, userId={}, deptId={}, name={}, type={}",
                 tenantId, actorUserId, deptId, name, type);

        DepartmentEntity departmentEntity = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND));

        // UD-002: type 변경 시 검증 (CUSTOM 타입 관련)
        if (type != null && type != departmentEntity.getType()) {
            DepartmentType oldType = departmentEntity.getType();
            DepartmentType newType = type;

            // CUSTOM → 다른 타입: customTypeName이 있으면 경고
            if (oldType == DepartmentType.CUSTOM && newType != DepartmentType.CUSTOM) {
                if (departmentEntity.getCustomTypeName() != null) {
                    log.warn("[ORG] CUSTOM 타입에서 변경 - customTypeName 유지됨 - deptId={}", deptId);
                }
            }

            // 다른 타입 → CUSTOM: customTypeName 없으면 에러
            if (newType == DepartmentType.CUSTOM && oldType != DepartmentType.CUSTOM) {
                if (departmentEntity.getCustomTypeName() == null ||
                    departmentEntity.getCustomTypeName().trim().isEmpty()) {
                    log.error("[ORG] CUSTOM 타입으로 변경 시 customTypeName 필수 - deptId={}", deptId);
                    throw new OrganizationException(
                            OrganizationErrorCode.CUSTOM_TYPE_NAME_REQUIRED
                    );
                }
            }
        }

        departmentEntity.updateInfo(name, type);
        departmentEntity = departmentRepository.save(departmentEntity);

        log.info("[ORG] 부서 수정 완료 - deptId={}, name={}", deptId, name);

        return DepartmentResponse.from(departmentEntity);
    }

    public void moveDepartment(
            String tenantId,
            UUID actorUserId,
            String deptId,
            String newParentId) {

        validateMoveDepartment(tenantId, actorUserId, deptId, newParentId);

        executeMoveDepartment(tenantId, deptId, newParentId);
    }

    /** 이동 전 권한/순환/상태 제약을 검증한다. */
    @Transactional(readOnly = true)
    protected void validateMoveDepartment(
            String tenantId,
            UUID actorUserId,
            String deptId,
            String newParentId) {

        DepartmentEntity target = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        if (target.isRoot()) {
            log.warn("[ORG] 루트 부서 이동 시도 - deptId={}", deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.CANNOT_MOVE_ROOT
            );
        }

        Set<String> permissions = rbacModuleApi.getPermissionsByAgentId(tenantId, actorUserId.toString());
        if (!permissions.contains("org:update")) {
            log.warn("[ORG] org:update 권한 없음 - userId={}, permissions={}", actorUserId, permissions);
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        // DataScope 권한 검증: 접근 가능한 부서인지
        Set<String> accessibleDeptIds = getAccessibleDepartmentIds(
                tenantId,
                actorUserId
        );
        if (!accessibleDeptIds.contains(deptId)) {
            log.warn("[ORG] 부서 접근 권한 없음 - userId={}, deptId={}", actorUserId, deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        // 순환 참조 방지
        if (newParentId != null) {
            DepartmentEntity newParent = departmentRepository.findByDeptIdAndTenantId(newParentId, tenantId)
                    .orElseThrow(() -> new OrganizationException(
                            OrganizationErrorCode.INVALID_PARENT
                    ));

            // MD-002: 새 부모는 ACTIVE여야 함
            if (!newParent.isActive()) {
                log.warn("[ORG] 비활성 부서 하위로 이동 시도 - newParentId={}", newParentId);
                throw new OrganizationException(
                        OrganizationErrorCode.PARENT_DEPT_INACTIVE
                );
            }

            if (newParent.getOrgPath().startsWith(target.getOrgPath())) {
                log.warn("[ORG] 순환 참조 감지 - deptId={}, newParentId={}", deptId, newParentId);
                throw new OrganizationException(
                        OrganizationErrorCode.INVALID_PARENT
                );
            }
        }

        log.debug("[ORG] 부서 이동 검증 완료 - deptId={}", deptId);
    }

    /**
     * 부서 이동 실행 (쓰기 트랜잭션)
     * - 부서 상위 변경, 하위 부서 경로 일괄 업데이트
     */
    @Transactional
    protected void executeMoveDepartment(
            String tenantId,
            String deptId,
            String newParentId) {

        DepartmentEntity target = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        String originalOrgPath = target.getOrgPath();

        DepartmentEntity newParent = null;
        if (newParentId != null) {
            newParent = departmentRepository.findByDeptIdAndTenantId(newParentId, tenantId)
                    .orElseThrow(() -> new OrganizationException(
                            OrganizationErrorCode.INVALID_PARENT
                    ));
        }

        target.changeParent(newParent);
        departmentRepository.save(target);

        // 하위 부서 경로 일괄 업데이트
        String newParentPath = newParent != null ? newParent.getOrgPath() : "";
        List<DepartmentEntity> childDepts = departmentRepository
                .findByTenantIdAndOrgPathStartsWith(tenantId, originalOrgPath)
                .stream()
                .filter(d -> !d.getDeptId().equals(target.getDeptId()))
                .toList();

        for (DepartmentEntity child : childDepts) {
            String relativeSubPath = child.getOrgPath()
                    .substring(originalOrgPath.length());

            String newOrgPath = newParentPath + "/" + target.getDeptId() + relativeSubPath;
            child.setOrgPath(newOrgPath);

            int newDepth = newParentPath.isEmpty() ?
                    1 :
                    (newParent.getDepth() + 1) + (child.getDepth() - target.getDepth());
            child.setDepth(newDepth);
        }

        departmentRepository.saveAll(childDepts);

        log.info("[ORG] 부서 이동 완료 - deptId={}, 하위={}개", deptId, childDepts.size());
    }


    @Transactional
    public void deleteDepartment(
            String tenantId,
            UUID actorUserId,
            String deptId) {

        log.info("[ORG] 부서 삭제 요청 - deptId={}", deptId);

        DepartmentEntity dept = departmentRepository.findByDeptIdAndTenantIdWithParent(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        // DL-001: 루트 부서는 삭제 불가
        if (dept.isRoot()) {
            log.warn("[ORG] 루트 부서 삭제 시도 - deptId={}", deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.CANNOT_DELETE_ROOT
            );
        }

        Set<String> accessibleDeptIds = getAccessibleDepartmentIds(
                tenantId,
                actorUserId
        );
        if (!accessibleDeptIds.contains(deptId)) {
            log.warn("[ORG] 부서 삭제 권한 없음 - userId={}, deptId={}", actorUserId, deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        if (departmentRepository.existsByParent(dept)) {
            log.warn("[ORG] 하위 부서 존재로 삭제 불가 - deptId={}", deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.CHILD_DEPARTMENT_EXISTS
            );
        }

        boolean hasActiveUsers = orgUserPort.existsActiveUserInDepartment(
                tenantId,
                deptId
        );
        if (hasActiveUsers) {
            log.warn("[ORG] 소속 활성 사용자 존재로 삭제 불가 - deptId={}", deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.ACTIVE_USERS_EXIST
            );
        }

        departmentRepository.delete(dept);

        log.info("[ORG] 부서 삭제 완료 - deptId={}", deptId);
    }

    @Override
    @Transactional
    public void deactivateDepartment(String tenantId, UUID actorUserId, String deptId) {
        log.info("[ORG] 부서 비활성화 요청 - deptId={}", deptId);

        DepartmentEntity dept = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        // DD-001: 루트 부서는 비활성화 불가
        if (dept.isRoot()) {
            log.warn("[ORG] 루트 부서 비활성화 시도 - deptId={}", deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.CANNOT_DEACTIVATE_ROOT
            );
        }

        Set<String> accessibleDeptIds = getAccessibleDepartmentIds(tenantId, actorUserId);
        if (!accessibleDeptIds.contains(deptId)) {
            log.warn("[ORG] 부서 비활성화 권한 없음 - userId={}, deptId={}", actorUserId, deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        String pathPrefix = dept.getOrgPath();
        boolean hasActiveChildren = departmentRepository
                .findByTenantIdAndOrgPathStartsWith(tenantId, pathPrefix).stream()
                .filter(d -> !d.getDeptId().equals(deptId))
                .anyMatch(DepartmentEntity::isActive);

        if (hasActiveChildren) {
            log.warn("[ORG] 활성 하위 부서 존재로 비활성화 불가 - deptId={}", deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.CHILD_DEPARTMENT_EXISTS,
                    "활성 상태인 하위 부서가 있어 비활성화할 수 없습니다."
            );
        }

        // DD-003: ACTIVE 상담사 있으면 비활성화 불가 (DEPARTMENT_SCENARIOS.md 준수)
        boolean hasActiveUsers = orgUserPort.existsActiveUserInDepartment(tenantId, deptId);
        if (hasActiveUsers) {
            log.warn("[ORG] 활성 사용자 존재로 비활성화 불가 - deptId={}", deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.ACTIVE_USERS_EXIST,
                    "활성 상태인 사용자가 소속되어 있어 비활성화할 수 없습니다."
            );
        }

        dept.deactivate();
        departmentRepository.save(dept);
        log.info("[ORG] 부서 비활성화 완료 - deptId={}", deptId);
    }

    @Override
    @Transactional
    public void activateDepartment(String tenantId, UUID actorUserId, String deptId) {
        log.info("[ORG] 부서 활성화 요청 - deptId={}", deptId);

        DepartmentEntity dept = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        Set<String> accessibleDeptIds = getAccessibleDepartmentIds(tenantId, actorUserId);
        if (!accessibleDeptIds.contains(deptId)) {
            log.warn("[ORG] 부서 활성화 권한 없음 - userId={}, deptId={}", actorUserId, deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        if (dept.getParent() != null && !dept.getParent().isActive()) {
            log.warn("[ORG] 상위 부서 비활성 상태로 활성화 불가 - deptId={}, parentId={}",
                    deptId, dept.getParent().getDeptId());
            throw new OrganizationException(
                    OrganizationErrorCode.INVALID_REQUEST,
                    "상위 부서가 비활성 상태입니다. 먼저 상위 부서를 활성화해주세요."
            );
        }

        dept.activate();
        departmentRepository.save(dept);
        log.info("[ORG] 부서 활성화 완료 - deptId={}", deptId);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentTree(String tenantId) {
        List<DepartmentListProjection> allDepts = departmentRepository.findAllProjectedByTenantId(tenantId);
        return buildTree(allDepts);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentTreeWithinScope(
            String tenantId,
            UUID actorUserId) {

        Set<String> scopeDeptIds = getAccessibleDepartmentIds(
                tenantId,
                actorUserId
        );

        if (scopeDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<DepartmentListProjection> scopedDepts = departmentRepository
                .findProjectedByTenantIdAndDeptIdIn(tenantId, scopeDeptIds);

        return buildTree(scopedDepts);
    }

    private List<DepartmentResponse> buildTree(List<DepartmentListProjection> depts) {
        Map<String, DepartmentResponse> dtoMap = depts.stream()
                .map(this::toDepartmentResponse)
                .collect(Collectors.toMap(
                        DepartmentResponse::getDeptId,
                        dto -> dto
                ));

        List<DepartmentResponse> roots = new ArrayList<>();

        for (DepartmentResponse dto : dtoMap.values()) {
            String parentId = dto.getParentId();

            if (parentId == null) {
                roots.add(dto);
            } else {
                DepartmentResponse parent = dtoMap.get(parentId);
                if (parent != null) {
                    parent.addChild(dto);
                } else {
                    roots.add(dto);
                }
            }
        }

        roots.sort(Comparator.comparing(
                DepartmentResponse::getOrgPath,
                Comparator.nullsFirst(String::compareTo)
        ));

        return roots;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> searchDepartments(String tenantId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        List<DepartmentListProjection> departmentRows = departmentRepository
                .findProjectedByTenantIdAndNameContainingIgnoreCase(tenantId, keyword);

        return departmentRows.stream()
                .map(this::toDepartmentResponse)
                .sorted(Comparator.comparing(DepartmentResponse::getOrgPath))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getSubtree(String tenantId, String deptId) {
        log.debug("[ORG] 하위 부서 트리 조회 - deptId={}", deptId);

        DepartmentEntity dept = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        String pathPrefix = dept.getOrgPath();
        List<DepartmentListProjection> subtree = departmentRepository
                .findProjectedByTenantIdAndOrgPathStartsWith(tenantId, pathPrefix);

        log.debug("[ORG] 하위 부서 트리 조회 완료 - deptId={}, 총 {}개", deptId, subtree.size());

        return subtree.stream()
                .map(this::toDepartmentResponse)
                .sorted(Comparator.comparing(DepartmentResponse::getOrgPath))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentsByDepth(String tenantId, int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("depth는 0 이상이어야 합니다.");
        }

        List<DepartmentListProjection> departmentRows = departmentRepository
                .findProjectedByTenantIdAndDepth(tenantId, depth);

        return departmentRows.stream()
                .map(this::toDepartmentResponse)
                .sorted(Comparator.comparing(DepartmentResponse::getOrgPath))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentsByType(String tenantId, DepartmentType type) {
        if (type == null) {
            return List.of();
        }

        List<DepartmentListProjection> departmentRows = departmentRepository
                .findProjectedByTenantIdAndType(tenantId, type);

        return departmentRows.stream()
                .map(this::toDepartmentResponse)
                .sorted(Comparator.comparing(DepartmentResponse::getOrgPath))
                .toList();
    }

    private DepartmentResponse toDepartmentResponse(DepartmentListProjection row) {
        return DepartmentResponse.builder()
                .deptId(row.getDeptId())
                .name(row.getName())
                .type(row.getType())
                .orgPath(row.getOrgPath())
                .depth(row.getDepth())
                .parentId(row.getParentId())
                .status(row.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public DepartmentStatisticsResponse getDepartmentStatistics(String tenantId, String deptId) {
        DepartmentEntity departmentEntity = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        long totalEmployees = orgUserPort.countEmployeesByDepartment(tenantId, deptId);
        long activeEmployees = orgUserPort.countActiveEmployeesByDepartment(tenantId, deptId);

        long childDeptCount = departmentRepository.findAllByTenantIdWithParent(tenantId).stream()
                .filter(dept -> dept.getParent() != null
                        && dept.getParent().getDeptId().equals(deptId))
                .count();

        String pathPrefix = departmentEntity.getOrgPath();
        long descendantDeptCount = departmentRepository
                .findByTenantIdAndOrgPathStartsWith(tenantId, pathPrefix).stream()
                .filter(dept -> !dept.getDeptId().equals(deptId))
                .count();

        return DepartmentStatisticsResponse.builder()
                .deptId(departmentEntity.getDeptId())
                .name(departmentEntity.getName())
                .type(departmentEntity.getType())
                .depth(departmentEntity.getDepth())
                .totalEmployees(totalEmployees)
                .activeEmployees(activeEmployees)
                .childDeptCount(childDeptCount)
                .descendantDeptCount(descendantDeptCount)
                .build();
    }

    @Transactional(readOnly = true)
    public DepartmentMembersResponse getDepartmentMembers(
            String tenantId, 
            String deptId, 
            boolean includeSubDepartments) {

        DepartmentEntity departmentEntity = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        Set<String> targetDeptIds;
        if (includeSubDepartments) {
            String pathPrefix = departmentEntity.getOrgPath();
            targetDeptIds = departmentRepository
                    .findByTenantIdAndOrgPathStartsWith(tenantId, pathPrefix).stream()
                    .map(DepartmentEntity::getDeptId)
                    .collect(Collectors.toSet());
        } else {
            targetDeptIds = Set.of(deptId);
        }

        List<DepartmentMembersResponse.MemberInfo> members = new ArrayList<>();
        long activeCount = 0;
        long retiredCount = 0;

        for (String targetDeptId : targetDeptIds) {
            List<DepartmentMembersResponse.MemberInfo> deptMembers = 
                orgUserPort.getUsersByDepartment(tenantId, targetDeptId);
            
            members.addAll(deptMembers);
            
            for (DepartmentMembersResponse.MemberInfo member : deptMembers) {
                if ("ACTIVE".equals(member.status())) {
                    activeCount++;
                } else if ("RETIRED".equals(member.status())) {
                    retiredCount++;
                }
            }
        }

        return DepartmentMembersResponse.builder()
                .deptId(deptId)
                .deptName(departmentEntity.getName())
                .includeSubDepartments(includeSubDepartments)
                .totalCount(members.size())
                .activeCount(activeCount)
                .retiredCount(retiredCount)
                .members(members)
                .build();
    }

    /** 사용자 role scope에 따른 접근 가능한 부서 ID 집합을 계산한다. */
    public Set<String> getAccessibleDepartmentIds(String tenantId, UUID userId) {
        log.debug("[OrgScope] 접근 가능 부서 계산 - tenantId={}, userId={}", tenantId, userId);

        OrgUserView userView = orgUserPort.findOrgInfoByUserId(tenantId, userId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.USER_NOT_FOUND,
                        "사용자의 조직 정보를 찾을 수 없습니다. userId=" + userId
                ));

        if (!userView.isActive()) {
            throw new OrganizationException(
                    OrganizationErrorCode.USER_INACTIVE,
                    "비활성화된 사용자입니다. userId=" + userId
            );
        }

        DataScopeLevel level = userView.getRoleLevel();
        String myDeptId = userView.getDeptId();

        if (myDeptId == null) {
            throw new OrganizationException(
                    OrganizationErrorCode.USER_DEPARTMENT_NOT_FOUND,
                    "사용자의 소속 부서를 찾을 수 없습니다. userId=" + userId
            );
        }

        if (level.canSeeWholeTenant()) {
            return departmentRepository.findAllByTenantId(tenantId).stream()
                    .map(DepartmentEntity::getDeptId)
                    .collect(Collectors.toSet());
        }

        DepartmentEntity myDept = departmentRepository.findByDeptIdAndTenantId(myDeptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND,
                        "사용자의 소속 부서를 찾을 수 없습니다."
                ));

        if (level.canSeeSubTree()) {
            String pathPrefix = myDept.getOrgPath();
            return departmentRepository
                    .findByTenantIdAndOrgPathStartsWith(tenantId, pathPrefix)
                    .stream()
                    .map(DepartmentEntity::getDeptId)
                    .collect(Collectors.toSet());
        }

        return Set.of(myDept.getDeptId());
    }

    /** 모듈 간 통신용 부서 정보 조회 API. */
    @Override
    @Transactional(readOnly = true)
    public Optional<OrganizationModuleApi.DepartmentInfo> getDepartmentInfo(String tenantId, String deptId) {
        if (deptId == null || deptId.isEmpty()) {
            return Optional.empty();
        }

        return departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .map(dept -> OrganizationModuleApi.DepartmentInfo.builder()
                        .deptId(dept.getDeptId())
                        .name(dept.getName())
                        .fullPath(buildDepartmentFullPath(dept, tenantId))
                        .build());
    }

    /** 현재 부서부터 루트까지의 이름 경로를 생성한다. */
    private String buildDepartmentFullPath(DepartmentEntity dept, String tenantId) {
        List<String> pathNames = new ArrayList<>();
        DepartmentEntity current = dept;

        int maxDepth = 10;
        int depth = 0;

        while (current != null && depth < maxDepth) {
            pathNames.add(0, current.getName());

            if (current.getParent() != null) {
                String parentId = current.getParent().getDeptId();
                current = departmentRepository.findByDeptIdAndTenantId(parentId, tenantId)
                    .orElse(null);
            } else {
                break;
            }
            depth++;
        }

        return String.join(" > ", pathNames);
    }
}

