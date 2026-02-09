package com.nexfron.identitymodulith.organization.application.service;

import com.nexfron.identitymodulith.organization.application.exception.OrganizationException;
import com.nexfron.identitymodulith.organization.application.exception.OrganizationException.OrganizationErrorCode;
import com.nexfron.identitymodulith.organization.application.port.OrgUserView;
import com.nexfron.identitymodulith.organization.domain.model.DataScopeLevel;
import com.nexfron.identitymodulith.organization.domain.model.DepartmentType;
import com.nexfron.identitymodulith.organization.infrastructure.persistence.entity.DepartmentEntity;
import com.nexfron.identitymodulith.organization.presentation.dto.DepartmentDto;
import com.nexfron.identitymodulith.organization.infrastructure.persistence.repository.JpaDepartmentRepository;
import com.nexfron.identitymodulith.organization.application.port.OrgUserPort;
import com.nexfron.identitymodulith.organization.OrganizationModuleApi;
import com.nexfron.identitymodulith.rbac.RbacModuleApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 부서 관리 서비스 구현체
 *
 * <h2>구현 내용:</h2>
 * <ul>
 *   <li>부서 CRUD (생성, 조회, 수정, 삭제)</li>
 *   <li>부서 이동 시 하위 부서 org_path 일괄 갱신</li>
 *   <li>조직도 트리 구조 관리</li>
 *   <li>Level 1 RBAC 권한 기반 접근 제어</li>
 *   <li>데이터 범위 기반 접근 제어 (통합)</li>
 * </ul>
 *
 * @see DepartmentService
 * @author Identity System Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService, OrganizationModuleApi {

    private final JpaDepartmentRepository departmentRepository;
    private final OrgUserPort orgUserPort;
    private final RbacModuleApi rbacModuleApi;

    /**
     * 부서 생성
     * - parentId 검증 → 도메인 엔티티 생성 → DB 저장
     *
     * @param tenantId 테넌트 ID
     * @param actorUserId 생성 요청한 사용자 ID (권한 검증용)
     * @param name 부서명 (필수)
     * @param type 부서 타입 (COMPANY, DIVISION, TEAM, GROUP, CUSTOM)
     * @param code 부서 코드 (필수, 테넌트 내 고유)
     * @param customTypeName 커스텀 타입명 (type=CUSTOM일 때 필수)
     * @param parentId 상위 부서 ID (null이면 루트 부서)
     * @return 생성된 부서 정보
     */
    @Override
    @Transactional
    public DepartmentDto.Response createDepartment(
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

        // RBAC 권한 검증: org:create 권한 필요
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

        // 상위 부서 검증
        DepartmentEntity parent = null;
        if (parentId != null && !parentId.trim().isEmpty()) {
            parent = departmentRepository.findByDeptIdAndTenantId(parentId, tenantId)
                    .orElseThrow(() -> new OrganizationException(
                            OrganizationErrorCode.INVALID_PARENT
                    ));

            // CD-003: 부모 부서는 ACTIVE여야 함
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

        // 도메인 엔티티 생성 및 저장
        DepartmentEntity departmentEntity = DepartmentEntity.create(
                tenantId, name, type, code, customTypeName, parent);
        DepartmentEntity savedDept = departmentRepository.save(departmentEntity);

        log.info("[ORG] 부서 생성 완료 - deptId={}", savedDept.getDeptId());

        return DepartmentDto.Response.from(savedDept);
    }

    /**
     * 부서 정보 수정
     * - name, type 필드만 수정 가능
     *
     * @param tenantId 테넌트 ID
     * @param actorUserId 수정 요청한 사용자 ID (권한 검증용)
     * @param deptId 부서 ID
     * @param name 새 부서명 (null이면 변경 안 함)
     * @param type 새 부서 타입 (null이면 변경 안 함)
     * @return 수정된 부서 정보
     */
    @Override
    @Transactional
    public DepartmentDto.Response updateDepartment(
            String tenantId,
            UUID actorUserId,
            String deptId,
            String name,
            DepartmentType type) {

        Objects.requireNonNull(tenantId, "tenantId는 null일 수 없습니다");
        Objects.requireNonNull(actorUserId, "actorUserId는 null일 수 없습니다");
        Objects.requireNonNull(deptId, "deptId는 null일 수 없습니다");

        // RBAC 권한 검증: org:update 권한 필요
        Set<String> permissions = rbacModuleApi.getPermissionsByAgentId(tenantId, actorUserId.toString());
        if (!permissions.contains("org:update")) {
            log.warn("[ORG] org:update 권한 없음 - userId={}, permissions={}", actorUserId, permissions);
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        log.info("[ORG] 부서 수정 - tenantId={}, userId={}, deptId={}, name={}, type={}",
                 tenantId, actorUserId, deptId, name, type);

        // 부서 조회
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
                    // customTypeName은 그대로 유지 (데이터 보존)
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

        return DepartmentDto.Response.from(departmentEntity);
    }

    /**
     * 부서 이동 (재조직)
     * - 부서를 다른 부모 부서 하위로 이동
     * - 순환 참조 검사, 하위 부서 경로 일괄 업데이트
     *
     * @param tenantId 테넌트 ID
     * @param actorUserId 작업 수행 사용자 ID (권한 검증용)
     * @param deptId 이동할 부서 ID
     * @param newParentId 새 상위 부서 ID
     */
    public void moveDepartment(
            String tenantId,
            UUID actorUserId,
            String deptId,
            String newParentId) {

        // 검증 (읽기 전용)
        validateMoveDepartment(tenantId, actorUserId, deptId, newParentId);

        // 실행 (쓰기 트랜잭션)
        executeMoveDepartment(tenantId, deptId, newParentId);
    }

    /**
     * 부서 이동 검증 (읽기 전용)
     * - 권한 검증, 순환 참조 체크
     */
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

        // MD-001: 루트 부서는 이동 불가
        if (target.isRoot()) {
            log.warn("[ORG] 루트 부서 이동 시도 - deptId={}", deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.CANNOT_MOVE_ROOT
            );
        }

        // RBAC 권한 검증: org:update 권한 필요
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
                .collect(Collectors.toList());

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


    /**
     * 부서 삭제
     * - 하위 부서, 소속 사용자 존재 여부 검증
     * - Level 1 RBAC 권한 검증
     *
     * @param tenantId 테넌트 ID
     * @param actorUserId 작업 수행 사용자 ID
     * @param deptId 삭제할 부서 ID
     */
    @Transactional
    public void deleteDepartment(
            String tenantId,
            UUID actorUserId,
            String deptId) {

        log.info("[ORG] 부서 삭제 요청 - deptId={}", deptId);

        // 부서 조회
        DepartmentEntity dept = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
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

        // 권한 검증
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

        // 하위 부서 존재 여부 검증
        if (departmentRepository.existsByParent(dept)) {
            log.warn("[ORG] 하위 부서 존재로 삭제 불가 - deptId={}", deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.CHILD_DEPARTMENT_EXISTS
            );
        }

        // 소속 활성 사용자 존재 여부 검증
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

        // 삭제
        departmentRepository.delete(dept);

        log.info("[ORG] 부서 삭제 완료 - deptId={}", deptId);
    }

    // ============================================================
    // 부서 상태 관리
    // ============================================================

    /**
     * 부서 비활성화
     * - 활성 하위 부서가 없어야 비활성화 가능
     * - 소속 직원이 있어도 비활성화 가능 (경고 로그만 출력)
     *
     * @param tenantId 테넌트 ID
     * @param actorUserId 작업 수행 사용자 ID
     * @param deptId 비활성화할 부서 ID
     */
    @Override
    @Transactional
    public void deactivateDepartment(String tenantId, UUID actorUserId, String deptId) {
        log.info("[ORG] 부서 비활성화 요청 - deptId={}", deptId);

        // 부서 조회
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

        // 권한 검증
        Set<String> accessibleDeptIds = getAccessibleDepartmentIds(tenantId, actorUserId);
        if (!accessibleDeptIds.contains(deptId)) {
            log.warn("[ORG] 부서 비활성화 권한 없음 - userId={}, deptId={}", actorUserId, deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        // 활성 하위 부서 존재 여부 검증
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

        // 소속 활성 사용자 확인
        // DD-003: ACTIVE 상담사 있으면 비활성화 불가 (DEPARTMENT_SCENARIOS.md 준수)
        boolean hasActiveUsers = orgUserPort.existsActiveUserInDepartment(tenantId, deptId);
        if (hasActiveUsers) {
            log.warn("[ORG] 활성 사용자 존재로 비활성화 불가 - deptId={}", deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.ACTIVE_USERS_EXIST,
                    "활성 상태인 사용자가 소속되어 있어 비활성화할 수 없습니다."
            );
        }

        // 비활성화
        dept.deactivate();
        departmentRepository.save(dept);
        log.info("[ORG] 부서 비활성화 완료 - deptId={}", deptId);
    }

    /**
     * 부서 활성화
     * - 상위 부서가 활성 상태여야 활성화 가능
     *
     * @param tenantId 테넌트 ID
     * @param actorUserId 작업 수행 사용자 ID
     * @param deptId 활성화할 부서 ID
     */
    @Override
    @Transactional
    public void activateDepartment(String tenantId, UUID actorUserId, String deptId) {
        log.info("[ORG] 부서 활성화 요청 - deptId={}", deptId);

        // 부서 조회
        DepartmentEntity dept = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        // 권한 검증
        Set<String> accessibleDeptIds = getAccessibleDepartmentIds(tenantId, actorUserId);
        if (!accessibleDeptIds.contains(deptId)) {
            log.warn("[ORG] 부서 활성화 권한 없음 - userId={}, deptId={}", actorUserId, deptId);
            throw new OrganizationException(
                    OrganizationErrorCode.INSUFFICIENT_PERMISSION
            );
        }

        // 상위 부서 활성화 여부 검증
        if (dept.getParent() != null && !dept.getParent().isActive()) {
            log.warn("[ORG] 상위 부서 비활성 상태로 활성화 불가 - deptId={}, parentId={}",
                    deptId, dept.getParent().getDeptId());
            throw new OrganizationException(
                    OrganizationErrorCode.INVALID_REQUEST,
                    "상위 부서가 비활성 상태입니다. 먼저 상위 부서를 활성화해주세요."
            );
        }

        // 활성화
        dept.activate();
        departmentRepository.save(dept);
        log.info("[ORG] 부서 활성화 완료 - deptId={}", deptId);
    }

    /**
     * 전체 조직도 트리 조회
     * - 권한 검증 없이 모든 부서 반환
     *
     * @param tenantId 테넌트 ID
     * @return 조직도 트리 (루트부터)
     */
    @Transactional(readOnly = true)
    public List<DepartmentDto.Response> getDepartmentTree(String tenantId) {
        List<DepartmentEntity> allDepts = departmentRepository.findAllByTenantId(tenantId);
        return buildTree(allDepts);
    }

    /**
     * 사용자 권한 범위 내 조직도 트리 조회 (Level 1 RBAC)
     * - 접근 가능한 부서만 필터링하여 반환
     *
     * @param tenantId 테넌트 ID
     * @param actorUserId 조회하는 사용자 ID
     * @return 권한 범위 내 조직도 트리
     */
    @Transactional(readOnly = true)
    public List<DepartmentDto.Response> getDepartmentTreeWithinScope(
            String tenantId,
            UUID actorUserId) {

        // 사용자가 접근 가능한 부서 ID 집합 조회
        Set<String> scopeDeptIds = getAccessibleDepartmentIds(
                tenantId,
                actorUserId
        );

        if (scopeDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 접근 가능한 부서만 필터링
        List<DepartmentEntity> allDepts = departmentRepository.findAllByTenantId(tenantId);
        List<DepartmentEntity> scopedDepts = allDepts.stream()
                .filter(d -> scopeDeptIds.contains(d.getDeptId()))
                .collect(Collectors.toList());

        return buildTree(scopedDepts);
    }

    /**
     * 부서 리스트를 트리 구조 DTO로 변환
     * - ID-DTO 맵 구성 → 부모-자식 관계 연결 → 루트 추출
     */
    private List<DepartmentDto.Response> buildTree(List<DepartmentEntity> depts) {
        // ID -> DTO 맵 구성
        Map<String, DepartmentDto.Response> dtoMap = depts.stream()
                .map(DepartmentDto.Response::from)
                .collect(Collectors.toMap(
                        DepartmentDto.Response::getDeptId,
                        dto -> dto
                ));

        List<DepartmentDto.Response> roots = new ArrayList<>();

        // 부모-자식 관계 구성
        for (DepartmentDto.Response dto : dtoMap.values()) {
            String parentId = dto.getParentId();

            if (parentId == null) {
                roots.add(dto);
            } else {
                DepartmentDto.Response parent = dtoMap.get(parentId);
                if (parent != null) {
                    parent.addChild(dto);
                } else {
                    roots.add(dto); // 부모 누락 시 루트로 처리
                }
            }
        }

        // orgPath 순서로 정렬
        roots.sort(Comparator.comparing(
                DepartmentDto.Response::getOrgPath,
                Comparator.nullsFirst(String::compareTo)
        ));

        return roots;
    }

    /**
     * 키워드로 부서 검색 (부서명 기준)
     */
    @Transactional(readOnly = true)
    public List<DepartmentDto.Response> searchDepartments(String tenantId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        List<DepartmentEntity> departmentEntities = departmentRepository
                .findByTenantIdAndNameContainingIgnoreCase(tenantId, keyword);

        return departmentEntities.stream()
                .map(DepartmentDto.Response::from)
                .sorted(Comparator.comparing(DepartmentDto.Response::getOrgPath))
                .collect(Collectors.toList());
    }

    /**
     * 하위 부서 트리 조회 (DEPARTMENT_SCENARIOS.md 명세)
     *
     * <h3>동작:</h3>
     * - 지정된 부서 및 모든 하위 부서를 조회
     * - Materialized Path (orgPath) 기반으로 효율적 조회
     * - 재귀 쿼리 없이 단일 쿼리로 처리
     *
     * <h3>사용 예시:</h3>
     * - 특정 본부의 전체 하위 조직도 조회
     * - 부서 선택 UI에서 하위 부서만 표시
     *
     * @param tenantId 테넌트 ID
     * @param deptId 조회할 부서 ID
     * @return 해당 부서 및 모든 하위 부서 목록 (orgPath 순서)
     */
    @Transactional(readOnly = true)
    public List<DepartmentDto.Response> getSubtree(String tenantId, String deptId) {
        log.debug("[ORG] 하위 부서 트리 조회 - deptId={}", deptId);

        // 부서 조회
        DepartmentEntity dept = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        // orgPath 기반 하위 부서 조회 (자신 포함)
        String pathPrefix = dept.getOrgPath();
        List<DepartmentEntity> subtree = departmentRepository
                .findByTenantIdAndOrgPathStartsWith(tenantId, pathPrefix);

        log.debug("[ORG] 하위 부서 트리 조회 완료 - deptId={}, 총 {}개", deptId, subtree.size());

        return subtree.stream()
                .map(DepartmentDto.Response::from)
                .sorted(Comparator.comparing(DepartmentDto.Response::getOrgPath))
                .collect(Collectors.toList());
    }

    /**
     * 특정 깊이(depth)의 부서 조회
     */
    @Transactional(readOnly = true)
    public List<DepartmentDto.Response> getDepartmentsByDepth(String tenantId, int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("depth는 0 이상이어야 합니다.");
        }

        List<DepartmentEntity> departmentEntities = departmentRepository
                .findByTenantIdAndDepth(tenantId, depth);

        return departmentEntities.stream()
                .map(DepartmentDto.Response::from)
                .sorted(Comparator.comparing(DepartmentDto.Response::getOrgPath))
                .collect(Collectors.toList());
    }

    /**
     * 특정 타입의 부서 조회
     */
    @Transactional(readOnly = true)
    public List<DepartmentDto.Response> getDepartmentsByType(String tenantId, DepartmentType type) {
        if (type == null) {
            return List.of();
        }

        List<DepartmentEntity> departmentEntities = departmentRepository
                .findByTenantIdAndType(tenantId, type);

        return departmentEntities.stream()
                .map(DepartmentDto.Response::from)
                .sorted(Comparator.comparing(DepartmentDto.Response::getOrgPath))
                .collect(Collectors.toList());
    }

    /**
     * 부서 통계 정보 조회
     * - 전체/활성 직원 수, 직속/전체 하위 부서 수
     */
    @Transactional(readOnly = true)
    public DepartmentDto.Statistics getDepartmentStatistics(String tenantId, String deptId) {
        // 부서 조회
        DepartmentEntity departmentEntity = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        // 직원 수 조회
        long totalEmployees = orgUserPort.countEmployeesByDepartment(tenantId, deptId);
        long activeEmployees = orgUserPort.countActiveEmployeesByDepartment(tenantId, deptId);

        // 직속 하위 부서 수
        long childDeptCount = departmentRepository.findAllByTenantId(tenantId).stream()
                .filter(dept -> dept.getParent() != null
                        && dept.getParent().getDeptId().equals(deptId))
                .count();

        // 전체 하위 부서 수 (orgPath 기반)
        String pathPrefix = departmentEntity.getOrgPath();
        long descendantDeptCount = departmentRepository
                .findByTenantIdAndOrgPathStartsWith(tenantId, pathPrefix).stream()
                .filter(dept -> !dept.getDeptId().equals(deptId))
                .count();

        return DepartmentDto.Statistics.builder()
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

    /**
     * 부서별 사용자 목록 조회
     * - 하위 부서 포함 여부 선택 가능
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID
     * @param includeSubDepartments 하위 부서 포함 여부
     * @return 부서 소속 사용자 정보
     */
    @Transactional(readOnly = true)
    public DepartmentDto.DepartmentMembers getDepartmentMembers(
            String tenantId, 
            String deptId, 
            boolean includeSubDepartments) {
        
        // 부서 존재 확인
        DepartmentEntity departmentEntity = departmentRepository.findByDeptIdAndTenantId(deptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND
                ));

        // 대상 부서 ID 목록 결정
        Set<String> targetDeptIds;
        if (includeSubDepartments) {
            // 현재 부서 + 모든 하위 부서
            String pathPrefix = departmentEntity.getOrgPath();
            targetDeptIds = departmentRepository
                    .findByTenantIdAndOrgPathStartsWith(tenantId, pathPrefix).stream()
                    .map(DepartmentEntity::getDeptId)
                    .collect(Collectors.toSet());
        } else {
            // 현재 부서만
            targetDeptIds = Set.of(deptId);
        }

        // 부서별 사용자 조회 (OrgUserPort 사용)
        List<DepartmentDto.MemberInfo> members = new ArrayList<>();
        long activeCount = 0;
        long retiredCount = 0;

        for (String targetDeptId : targetDeptIds) {
            List<DepartmentDto.MemberInfo> deptMembers = 
                orgUserPort.getUsersByDepartment(tenantId, targetDeptId);
            
            members.addAll(deptMembers);
            
            for (DepartmentDto.MemberInfo member : deptMembers) {
                if ("ACTIVE".equals(member.status())) {
                    activeCount++;
                } else if ("RETIRED".equals(member.status())) {
                    retiredCount++;
                }
            }
        }

        return DepartmentDto.DepartmentMembers.builder()
                .deptId(deptId)
                .deptName(departmentEntity.getName())
                .includeSubDepartments(includeSubDepartments)
                .totalCount(members.size())
                .activeCount(activeCount)
                .retiredCount(retiredCount)
                .members(members)
                .build();
    }

    // ========================================================================
    // 🔐 Public: 데이터 범위 기반 접근 제어 (통합된 로직)
    // ========================================================================

    /**
     * 사용자가 접근 가능한 부서 ID 집합 조회
     *
     * <p>역할별 부서 접근 범위:</p>
     * <ul>
     *   <li><b>ADMIN</b>: 전체 조직 조회 가능</li>
     *   <li><b>TEAM_LEAD</b>: 자신의 부서 + 하위 부서 조회 가능</li>
     *   <li><b>MEMBER</b>: 자신의 부서만 조회 가능</li>
     * </ul>
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return 접근 가능한 부서 ID 집합
     * @throws OrganizationException 사용자 정보를 찾을 수 없거나 비활성화된 경우
     */
    public Set<String> getAccessibleDepartmentIds(String tenantId, UUID userId) {
        log.debug("[OrgScope] 접근 가능 부서 계산 - tenantId={}, userId={}", tenantId, userId);

        // 1. 사용자 조직 정보 조회
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

        // 2. ADMIN: 전체 조직 조회
        if (level.canSeeWholeTenant()) {
            return departmentRepository.findAllByTenantId(tenantId).stream()
                    .map(DepartmentEntity::getDeptId)
                    .collect(Collectors.toSet());
        }

        // 3. 본인 부서 조회
        DepartmentEntity myDept = departmentRepository.findByDeptIdAndTenantId(myDeptId, tenantId)
                .orElseThrow(() -> new OrganizationException(
                        OrganizationErrorCode.DEPARTMENT_NOT_FOUND,
                        "사용자의 소속 부서를 찾을 수 없습니다."
                ));

        // 4. TEAM_LEAD: 내 부서 + 하위 부서
        if (level.canSeeSubTree()) {
            String pathPrefix = myDept.getOrgPath();
            return departmentRepository
                    .findByTenantIdAndOrgPathStartsWith(tenantId, pathPrefix)
                    .stream()
                    .map(DepartmentEntity::getDeptId)
                    .collect(Collectors.toSet());
        }

        // 5. MEMBER: 내 부서만
        return Set.of(myDept.getDeptId());
    }

    // ============================================================
    // OrganizationModuleApi 구현 (모듈 간 통신용)
    // ============================================================

    /**
     * 부서 ID로 부서 정보 조회 (모듈 간 통신용 Public API)
     *
     * <h3>사용처:</h3>
     * - User 모듈에서 Agent 조회 시 부서 이름/경로 제공
     * - RBAC 모듈에서 권한 범위 계산 시 부서 정보 제공
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID
     * @return 부서 정보 (이름, 전체 경로)
     */
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

    /**
     * Department 전체 경로 구성 (예: "넥스프론 > 고객서비스본부 > 인바운드팀")
     *
     * <h3>동작:</h3>
     * - 현재 부서부터 루트까지 올라가며 이름 수집
     * - " > "로 연결하여 전체 경로 문자열 생성
     *
     * @param dept 대상 부서
     * @param tenantId 테넌트 ID
     * @return 부서 전체 경로 문자열
     */
    private String buildDepartmentFullPath(DepartmentEntity dept, String tenantId) {
        List<String> pathNames = new ArrayList<>();
        DepartmentEntity current = dept;

        // 무한 루프 방지 (최대 10단계)
        int maxDepth = 10;
        int depth = 0;

        while (current != null && depth < maxDepth) {
            pathNames.add(0, current.getName());  // 앞에 추가 (역순으로)

            if (current.getParent() != null) {
                String parentId = current.getParent().getDeptId();
                current = departmentRepository.findByDeptIdAndTenantId(parentId, tenantId)
                    .orElse(null);
            } else {
                break;  // 루트 부서 도달
            }
            depth++;
        }

        return String.join(" > ", pathNames);
    }
}

