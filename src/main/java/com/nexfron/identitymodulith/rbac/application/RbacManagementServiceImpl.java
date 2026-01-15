package com.nexfron.identitymodulith.rbac.application;

import com.nexfron.identitymodulith.rbac.application.exception.RbacException;
import com.nexfron.identitymodulith.rbac.application.dto.AuditLogDto;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.AgentRoleJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionGroupJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.AgentRoleJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.PermissionGroupPermissionJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.PermissionGroupJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.PermissionJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.RoleJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionGroupJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RBAC 관리 서비스 구현체
 *
 * <h2>책임:</h2>
 * <ul>
 *   <li>역할(Role) CRUD 관리</li>
 *   <li>권한(Permission) CRUD 관리</li>
 *   <li>역할-권한 매핑 관리</li>
 *   <li>데이터 일관성 보장</li>
 *   <li>멀티테넌시 격리</li>
 * </ul>
 *
 * <h2>트랜잭션 전략:</h2>
 * <ul>
 *   <li>모든 쓰기 작업: @Transactional (필수)</li>
 *   <li>읽기 작업: readOnly=true</li>
 *   <li>관계 변경: 원자성 보장</li>
 * </ul>
 *
 * <h2>에러 처리:</h2>
 * <ul>
 *   <li>역할/권한 중복 검증</li>
 *   <li>참조 무결성 검증</li>
 *   <li>테넌트 격리 검증</li>
 * </ul>
 *
 * <h2>tenantId 획득:</h2>
 * SecurityContext의 Authentication 객체에서 tenantId를 추출합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RbacManagementServiceImpl implements RbacManagementService {

    private final RoleJpaRepository roleRepository;
    private final PermissionJpaRepository permissionRepository;
    private final RolePermissionJpaRepository rolePermissionRepository;
    private final AgentRoleJpaRepository agentRoleRepository;
    private final PermissionGroupJpaRepository permissionGroupRepository;
    private final PermissionGroupPermissionJpaRepository permissionGroupPermissionRepository;
    private final RolePermissionGroupJpaRepository rolePermissionGroupRepository;
    private final AuditLogService auditLogService;

    /**
     * 현재 요청의 tenantId 추출
     *
     * @return tenantId (Authentication 객체의 principal에서 추출)
     */
    private String getTenantId() {
        // SecurityContext에서 tenantId 추출 (프로젝트의 인증 모델에 따라 확장 필요)
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            return principal.toString();
        }
        // TODO: CustomPrincipal 도입 시 tenantId 추출 로직 교체
        return "default-tenant";
    }

    /**
     * 현재 사용자 ID 조회
     *
     * @return 현재 인증된 사용자의 ID (없으면 "system")
     */
    private String getCurrentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof String) {
                return principal.toString();
            }
            // principal이 Custom 객체라면 getId() 또는 유사 메서드 호출 필요
            return "system";
        } catch (Exception e) {
            // 인증 정보가 없을 경우
            return "system";
        }
    }

    @Override
    public List<RoleDto> getAllRoles() {
        String tenantId = getTenantId();
        return roleRepository.findByTenantId(tenantId).stream()
                .map(role -> new RoleDto(
                        role.getName(),
                        role.getType(),
                        role.getDescription(),
                        role.getIsActive()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public RoleDto getRoleByName(String roleName) {
        String tenantId = getTenantId();
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        return new RoleDto(
                role.getName(),
                role.getType(),
                role.getDescription(),
                role.getIsActive()
        );
    }

    @Override
    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        String tenantId = getTenantId();

        // 1. 중복 확인
        if (roleRepository.existsByTenantIdAndName(tenantId, request.name())) {
            throw new RbacException(RbacException.RbacErrorCode.ROLE_ALREADY_EXISTS);
        }

        // 2. 역할 생성
        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .name(request.name())
                .type(request.type())
                .description(null)  // CreateRoleRequest에는 description 필드가 없음
                .isActive(true)  // 기본값: 활성
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 3. 저장
        RoleJpaEntity savedRole = roleRepository.save(role);

        // 4. 감사 로그 기록
        String operatorId = getCurrentUserId();
        auditLogService.recordRoleCreation(tenantId, savedRole.getName(), savedRole.getType(), operatorId);

        return new RoleDto(
                savedRole.getName(),
                savedRole.getType(),
                savedRole.getDescription(),
                savedRole.getIsActive()
        );
    }

    /**
     * 역할 정보 업데이트
     *
     * @param roleName 업데이트할 역할명
     * @param request 업데이트 요청 (type, description, isActive 중 변경할 항목만 포함)
     * @return 업데이트된 역할 정보
     */
    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)  // 역할 정보 변경 시 캐시 무효화
    public RoleDto updateRole(String roleName, UpdateRoleRequest request) {
        String tenantId = getTenantId();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        // 2. 변경할 필드만 업데이트
        if (request.type() != null) {
            role.setType(request.type());
        }
        if (request.description() != null) {
            role.setDescription(request.description());
        }
        if (request.isActive() != null) {
            role.setIsActive(request.isActive());
        }
        role.setUpdatedAt(LocalDateTime.now());

        // 3. 저장
        RoleJpaEntity updatedRole = roleRepository.save(role);

        // 4. 감사 로그 기록
        String operatorId = getCurrentUserId();
        auditLogService.recordRoleUpdate(
                tenantId,
                updatedRole.getRoleId(),
                updatedRole.getName(),
                operatorId,
                String.format("type=%s, description=%s, isActive=%s",
                        request.type(), request.description(), request.isActive())
        );

        log.info("[RBAC] 역할 업데이트 완료 - tenantId: {}, roleName: {}, operator: {}",
                tenantId, roleName, operatorId);

        return new RoleDto(
                updatedRole.getName(),
                updatedRole.getType(),
                updatedRole.getDescription(),
                updatedRole.getIsActive()
        );
    }

    @Override
    public List<PermissionDto> getAllPermissions() {
        String tenantId = getTenantId();
        return permissionRepository.findByTenantId(tenantId).stream()
                .map(perm -> new PermissionDto(perm.getCode(), perm.getDescription()))
                .collect(Collectors.toList());
    }

    @Override
    public PermissionDto getPermissionByCode(String code) {
        String tenantId = getTenantId();
        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        return new PermissionDto(permission.getCode(), permission.getDescription());
    }

    @Override
    @Transactional
    public PermissionDto createPermission(CreatePermissionRequest request) {
        String tenantId = getTenantId();

        // 1. 중복 확인
        if (permissionRepository.existsByTenantIdAndCode(tenantId, request.code())) {
            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_ALREADY_EXISTS);
        }

        // 2. 권한 생성
        PermissionJpaEntity permission = PermissionJpaEntity.builder()
                .permissionId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .code(request.code())
                .createdAt(LocalDateTime.now())
                .build();

        // 3. 저장
        PermissionJpaEntity savedPermission = permissionRepository.save(permission);

        // 4. 감사 로그 기록
        String operatorId = getCurrentUserId();
        auditLogService.recordPermissionCreation(tenantId, savedPermission.getCode(), operatorId);

        return new PermissionDto(savedPermission.getCode(), savedPermission.getDescription());
    }

    /**
     * 권한 정보 업데이트
     *
     * @param code 업데이트할 권한 코드
     * @param request 업데이트 요청 (code, description 중 변경할 항목만 포함)
     * @return 업데이트된 권한 정보
     */
    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)  // 권한 정보 변경 시 캐시 무효화
    public PermissionDto updatePermission(String code, UpdatePermissionRequest request) {
        String tenantId = getTenantId();

        // 1. 권한 조회
        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 2. 변경할 필드만 업데이트
        if (request.code() != null && !request.code().equals(code)) {
            // 코드 변경 시 중복 확인
            if (permissionRepository.existsByTenantIdAndCode(tenantId, request.code())) {
                throw new RbacException(RbacException.RbacErrorCode.PERMISSION_ALREADY_EXISTS);
            }
            permission.setCode(request.code());
        }
        if (request.description() != null) {
            permission.setDescription(request.description());
        }

        // 3. 저장
        PermissionJpaEntity updatedPermission = permissionRepository.save(permission);

        // 4. 감사 로그 기록
        String operatorId = getCurrentUserId();
        auditLogService.recordPermissionUpdate(
                tenantId,
                updatedPermission.getPermissionId(),
                updatedPermission.getCode(),
                operatorId,
                String.format("code=%s, description=%s", request.code(), request.description())
        );

        log.info("[RBAC] 권한 업데이트 완료 - tenantId: {}, code: {}, operator: {}",
                tenantId, code, operatorId);

        return new PermissionDto(updatedPermission.getCode(), updatedPermission.getDescription());
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void assignPermissionToRole(String roleName, String permissionCode) {
        String tenantId = getTenantId();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        // 2. 권한 조회
        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 3. 중복 할당 확인
        if (rolePermissionRepository.existsByRoleIdAndPermissionId(role.getRoleId(), permission.getPermissionId())) {
            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_ALREADY_ASSIGNED);
        }

        // 4. 매핑 생성
        RolePermissionJpaEntity mapping = RolePermissionJpaEntity.builder()
                .roleId(role.getRoleId())
                .permissionId(permission.getPermissionId())
                .assignedAt(LocalDateTime.now())
                .build();

        rolePermissionRepository.save(mapping);

        // 5. 감사 로그 기록 (권한 변경 이력 추적)
        String operatorId = getCurrentUserId();
        auditLogService.recordRolePermissionAssignment(tenantId, roleName, role.getRoleId(),
                permissionCode, permission.getPermissionId(), operatorId);

        log.info("[RBAC] 역할-권한 할당: roleName={}, permissionCode={}, 모든 사용자 캐시 무효화", roleName, permissionCode);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void revokePermissionFromRole(String roleName, String permissionCode) {
        String tenantId = getTenantId();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        // 2. 권한 조회
        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 3. 매핑 삭제
        rolePermissionRepository.deleteByRoleIdAndPermissionId(role.getRoleId(), permission.getPermissionId());

        // 4. 감사 로그 기록 (권한 회수 이력)
        String operatorId = getCurrentUserId();
        auditLogService.recordRolePermissionRevocation(tenantId, roleName, role.getRoleId(),
                permissionCode, operatorId);

        log.info("[RBAC] 역할-권한 회수: roleName={}, permissionCode={}, 모든 사용자 캐시 무효화", roleName, permissionCode);
    }

    /**
     * 특정 역할의 모든 권한 조회 (성능 최적화됨)
     *
     * 개선 사항:
     * - 기존: 2개 쿼리 (role_permissions 조회 + permissions 조회)
     * - 개선: 1개 쿼리 (JOIN으로 한 번에 조회)
     *
     * @param roleName 역할명
     * @return 역할에 할당된 권한 DTO 집합
     */
    @Override
    @Transactional(readOnly = true)
    public Set<PermissionDto> getPermissionsByRole(String roleName) {
        String tenantId = getTenantId();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        // 2. 권한 코드를 DTO 프로젝션으로 한 번에 조회 (성능 최적화)
        List<String> permissionCodes = rolePermissionRepository
                .findPermissionCodesByRoleIdAndTenant(role.getRoleId(), tenantId);

        return permissionCodes.stream()
                .map(code -> new PermissionDto(code, null)) // 코드만 있고 설명은 나중에 추가할 수 있음
                .collect(Collectors.toSet());
    }

    // ========== 사용자-역할 관리 메서드 ==========

    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void assignRoleToAgent(String agentId, String roleName) {
        String tenantId = getTenantId();
        long startTime = System.currentTimeMillis();

        // 1. 역할 조회 (테넌트 범위)
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> {
                    log.warn("[RBAC] 역할 할당 실패: roleName={}, 존재하지 않음", roleName);
                    return new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
                });

        // 1-1. 비활성 역할 할당 차단
        if (role.getIsActive() == null || !role.getIsActive()) {
            log.warn("[RBAC] 비활성 역할 할당 시도: agentId={}, roleName={}, isActive={}",
                    agentId, roleName, role.getIsActive());
            throw new RbacException(RbacException.RbacErrorCode.ROLE_NOT_ACTIVE);
        }

        // 2. 중복 할당 확인
        if (agentRoleRepository.existsByAgentIdAndRoleId(agentId, role.getRoleId())) {
            log.warn("[RBAC] 중복 역할 할당 시도: agentId={}, roleName={}, roleId={}",
                    agentId, roleName, role.getRoleId());
            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_ALREADY_ASSIGNED);
        }

        // 3. 매핑 생성
        AgentRoleJpaEntity mapping = AgentRoleJpaEntity.builder()
                .agentId(agentId)
                .roleId(role.getRoleId())
                .assignedAt(LocalDateTime.now())
                .build();

        // 4. 저장
        agentRoleRepository.save(mapping);

        // 5. 감사 로그 기록 (사용자-역할 변경 이력)
        String operatorId = getCurrentUserId();
        auditLogService.recordAgentRoleAssignment(tenantId, agentId, roleName, operatorId);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[RBAC 역할 할당] agentId={}, roleName={}, roleId={}, 소요시간={}ms", agentId, roleName, role.getRoleId(), duration);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void revokeRoleFromAgent(String agentId, String roleName) {
        String tenantId = getTenantId();
        long startTime = System.currentTimeMillis();

        // 1. 역할 조회 (테넌트 범위)
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> {
                    log.warn("[RBAC] 역할 회수 실패: roleName={}, 존재하지 않음", roleName);
                    return new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
                });

        // 2. 매핑 삭제 (특정 테넌트 기준 roleId 매칭)
        agentRoleRepository.findByAgentId(agentId).stream()
                .filter(ar -> ar.getRoleId().equals(role.getRoleId()))
                .forEach(agentRoleRepository::delete);

        // 3. 감사 로그 기록
        String operatorId = getCurrentUserId();
        auditLogService.recordAgentRoleRevocation(tenantId, agentId, roleName, operatorId);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[RBAC 역할 회수] agentId={}, roleName={}, roleId={}, 소요시간={}ms", agentId, roleName, role.getRoleId(), duration);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getRolesByAgent(String agentId) {
        String tenantId = getTenantId();

        List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByAgentId(agentId);
        if (agentRoles.isEmpty()) {
            log.debug("[RBAC] 사용자에게 할당된 역할 없음: agentId={}", agentId);
            return Set.of();
        }

        return agentRoles.stream()
                .map(ar -> roleRepository.findByTenantIdAndRoleId(tenantId, ar.getRoleId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(RoleJpaEntity::getName)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public int getAgentCountByRole(String roleName) {
        String tenantId = getTenantId();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> {
                    log.warn("[RBAC] 역할 조회 실패: roleName={}", roleName);
                    return new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
                });

        // 2. 해당 역할을 가진 Agent 수 조회
        List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByRoleId(role.getRoleId());

        log.debug("[RBAC] 역할별 사용자 수 조회: roleName={}, count={}", roleName, agentRoles.size());
        return agentRoles.size();
    }

    /**
     * 역할 삭제 (영향 확인 및 선택적 강제 삭제)
     */
    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public RoleDeletionResult deleteRole(String roleName, boolean forceDelete) {
        String tenantId = getTenantId();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        // 2. 영향도 조회
        List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByRoleId(role.getRoleId());
        int affectedUserCount = agentRoles.size();

        // 3. 권한 매핑 수 조회
        List<RolePermissionJpaEntity> rolePermissions = rolePermissionRepository.findByRoleId(role.getRoleId());
        int removedPermissionCount = rolePermissions.size();

        // 4. 안전 모드에서 사용자가 있는 경우 예외 발생
        if (!forceDelete && affectedUserCount > 0) {
            String warningMessage = String.format(
                    "역할 '%s'을 %d명의 사용자가 사용 중이므로 삭제할 수 없습니다. " +
                            "강제 삭제하려면 force=true 옵션을 사용하세요.",
                    roleName, affectedUserCount
            );
            throw new RbacException(RbacException.RbacErrorCode.ROLE_HAS_USERS, warningMessage);
        }

        // 5. 강제 모드에서 사용자 역할 먼저 회수
        if (forceDelete && affectedUserCount > 0) {
            log.info("[RBAC] 강제 삭제 모드: {}명의 사용자에서 역할 '{}' 회수 시작", affectedUserCount, roleName);
            agentRoleRepository.deleteByRoleId(role.getRoleId());
        }

        // 6. 역할-권한 매핑 삭제
        rolePermissionRepository.deleteByRoleId(role.getRoleId());

        // 7. 역할-권한그룹 매핑 삭제
        rolePermissionGroupRepository.deleteByRoleId(role.getRoleId());

        // 8. 역할 삭제
        roleRepository.delete(role);

        // 9. 감사 로그 기록
        String operatorId = getCurrentUserId();
        auditLogService.recordRoleDeletion(tenantId, roleName, role.getRoleId(), operatorId);

        log.info("[RBAC] 역할 삭제 완료: roleName={}, 영향받은 사용자={}, 제거된 권한={}, 강제삭제={}",
                roleName, affectedUserCount, removedPermissionCount, forceDelete);

        return new RoleDeletionResult(
                roleName,
                affectedUserCount,
                removedPermissionCount,
                forceDelete,
                forceDelete && affectedUserCount > 0
                        ? String.format("%d명의 사용자에서 역할이 회수되었습니다.", affectedUserCount)
                        : null
        );
    }

    /**
     * 역할 삭제 전 영향도 조회
     */
    @Override
    @Transactional(readOnly = true)
    public RoleDeletionImpact getRoleDeletionImpact(String roleName) {
        String tenantId = getTenantId();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        // 2. 할당된 사용자 수 조회
        List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByRoleId(role.getRoleId());
        int affectedUserCount = agentRoles.size();

        // 3. 할당된 권한 수 조회
        List<RolePermissionJpaEntity> rolePermissions = rolePermissionRepository.findByRoleId(role.getRoleId());
        int assignedPermissionCount = rolePermissions.size();

        // 4. 안전하게 삭제 가능한지 판단
        boolean canDelete = affectedUserCount == 0;

        // 5. 영향 정보 생성
        StringBuilder impactDetails = new StringBuilder();
        if (affectedUserCount > 0) {
            impactDetails.append(String.format("• %d명의 사용자가 이 역할을 사용 중입니다.\n", affectedUserCount));
        }
        if (assignedPermissionCount > 0) {
            impactDetails.append(String.format("• %d개의 권한이 할당되어 있습니다.\n", assignedPermissionCount));
        }
        if (canDelete) {
            impactDetails.append("• 안전하게 삭제할 수 있습니다.");
        } else {
            impactDetails.append("• 강제 삭제 시 사용자들의 역할이 회수됩니다.");
        }

        log.debug("[RBAC] 역할 삭제 영향도 조회: roleName={}, 사용자={}, 권한={}, 삭제가능={}",
                roleName, affectedUserCount, assignedPermissionCount, canDelete);

        return new RoleDeletionImpact(
                roleName,
                affectedUserCount,
                assignedPermissionCount,
                canDelete,
                impactDetails.toString()
        );
    }

    @Override
    @Transactional
    public void deactivateRole(String roleName) {
        String tenantId = getTenantId();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> {
                    log.warn("[RBAC] 역할 비활성화 실패: roleName={}, 존재하지 않음", roleName);
                    return new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
                });

        // 2. 이미 비활성 상태인지 확인
        if (role.getIsActive() != null && !role.getIsActive()) {
            log.warn("[RBAC] 이미 비활성화된 역할: roleName={}", roleName);
            return; // 이미 비활성 상태면 아무 작업 안 함
        }

        // 3. 비활성화 처리
        role.setIsActive(false);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);

        // 4. 감사 로그 기록
        String operatorId = getCurrentUserId();
        auditLogService.recordRoleDeactivation(tenantId, roleName, role.getRoleId(), operatorId);

        log.info("[RBAC] 역할 비활성화: roleName={}, tenantId={}, 기존 할당 유지, 신규 할당 차단",
                roleName, tenantId);
    }

    @Override
    @Transactional
    public void activateRole(String roleName) {
        String tenantId = getTenantId();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> {
                    log.warn("[RBAC] 역할 활성화 실패: roleName={}, 존재하지 않음", roleName);
                    return new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
                });

        // 2. 이미 활성 상태인지 확인
        if (role.getIsActive() != null && role.getIsActive()) {
            log.warn("[RBAC] 이미 활성화된 역할: roleName={}", roleName);
            return; // 이미 활성 상태면 아무 작업 안 함
        }

        // 3. 활성화 처리
        role.setIsActive(true);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);

        // 4. 감사 로그 기록
        String operatorId = getCurrentUserId();
        auditLogService.recordRoleActivation(tenantId, roleName, role.getRoleId(), operatorId);

        log.info("[RBAC] 역할 활성화: roleName={}, tenantId={}, 할당 가능 상태로 전환",
                roleName, tenantId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void deletePermission(String code) {
        String tenantId = getTenantId();

        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 권한에 매핑된 모든 역할 관계 삭제 (카스케이드)
        rolePermissionRepository.deleteByPermissionId(permission.getPermissionId());

        // 권한 삭제
        permissionRepository.delete(permission);

        // 감사 로그 기록
        String operatorId = getCurrentUserId();
        auditLogService.recordPermissionDeletion(tenantId, code, permission.getPermissionId(), operatorId);

        log.info("[RBAC] 권한 삭제: code={}, tenantId={}, 모든 사용자 캐시 무효화", code, tenantId);
    }

    // ========== 권한 그룹 관리 메서드 ==========

    @Override
    @Transactional(readOnly = true)
    public List<PermissionGroupDto> getAllPermissionGroups() {
        String tenantId = getTenantId();
        return permissionGroupRepository.findByTenantIdAndIsActive(tenantId, true).stream()
                .map(group -> new PermissionGroupDto(group.getName(), group.getDescription(), group.getIsActive()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionGroupDto getPermissionGroupByName(String groupName) {
        String tenantId = getTenantId();
        PermissionGroupJpaEntity group = permissionGroupRepository.findByTenantIdAndName(tenantId, groupName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));
        return new PermissionGroupDto(group.getName(), group.getDescription(), group.getIsActive());
    }

    @Override
    @Transactional
    public PermissionGroupDto createPermissionGroup(CreatePermissionGroupRequest request) {
        String tenantId = getTenantId();

        // 1. 중복 확인
        if (permissionGroupRepository.existsByTenantIdAndName(tenantId, request.name())) {
            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND);  // TODO: PERMISSION_GROUP_ALREADY_EXISTS 에러 코드 추가 필요
        }

        // 2. 권한 그룹 생성
        PermissionGroupJpaEntity group = PermissionGroupJpaEntity.builder()
                .permissionGroupId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .name(request.name())
                .description(request.description())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 3. 저장
        PermissionGroupJpaEntity savedGroup = permissionGroupRepository.save(group);

        // 4. 감사 로그 기록
        String operatorId = getCurrentUserId();
        log.info("[RBAC] 권한 그룹 생성: groupName={}, tenantId={}, operatorId={}",
                savedGroup.getName(), tenantId, operatorId);

        return new PermissionGroupDto(savedGroup.getName(), savedGroup.getDescription(), savedGroup.getIsActive());
    }

    /**
     * 권한 그룹 정보 업데이트
     */
    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public PermissionGroupDto updatePermissionGroup(String groupName, UpdatePermissionGroupRequest request) {
        String tenantId = getTenantId();

        // 1. 권한 그룹 조회
        PermissionGroupJpaEntity group = permissionGroupRepository.findByTenantIdAndName(tenantId, groupName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 2. 변경할 필드만 업데이트
        if (request.description() != null) {
            group.setDescription(request.description());
        }
        if (request.isActive() != null) {
            group.setIsActive(request.isActive());
        }
        group.setUpdatedAt(LocalDateTime.now());

        // 3. 저장
        PermissionGroupJpaEntity updatedGroup = permissionGroupRepository.save(group);

        // 4. 감사 로그 기록 (새 메서드 필요 - 임시로 로그만)
        String operatorId = getCurrentUserId();
        log.info("[RBAC] 권한 그룹 업데이트: groupName={}, description={}, isActive={}, tenantId={}, operatorId={}",
                groupName, request.description(), request.isActive(), tenantId, operatorId);

        return new PermissionGroupDto(
                updatedGroup.getName(),
                updatedGroup.getDescription(),
                updatedGroup.getIsActive()
        );
    }

    /**
     * 권한 그룹 비활성화
     */
    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void deactivatePermissionGroup(String groupName) {
        String tenantId = getTenantId();

        // 1. 권한 그룹 조회
        PermissionGroupJpaEntity group = permissionGroupRepository.findByTenantIdAndName(tenantId, groupName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 2. 이미 비활성화된 경우 예외
        if (group.getIsActive() != null && !group.getIsActive()) {
            throw new RbacException(RbacException.RbacErrorCode.ROLE_NOT_ACTIVE);
        }

        // 3. 비활성화
        group.setIsActive(false);
        group.setUpdatedAt(LocalDateTime.now());
        permissionGroupRepository.save(group);

        // 4. 로그
        String operatorId = getCurrentUserId();
        log.info("[RBAC] 권한 그룹 비활성화: groupName={}, tenantId={}, operatorId={}",
                groupName, tenantId, operatorId);
    }

    /**
     * 권한 그룹 활성화
     */
    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void activatePermissionGroup(String groupName) {
        String tenantId = getTenantId();

        // 1. 권한 그룹 조회
        PermissionGroupJpaEntity group = permissionGroupRepository.findByTenantIdAndName(tenantId, groupName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 2. 이미 활성화된 경우 예외
        if (group.getIsActive() != null && group.getIsActive()) {
            log.warn("[RBAC] 이미 활성화된 권한 그룹: groupName={}", groupName);
            return; // 이미 활성화되어 있으면 아무것도 하지 않음
        }

        // 3. 활성화
        group.setIsActive(true);
        group.setUpdatedAt(LocalDateTime.now());
        permissionGroupRepository.save(group);

        // 4. 로그
        String operatorId = getCurrentUserId();
        log.info("[RBAC] 권한 그룹 활성화: groupName={}, tenantId={}, operatorId={}",
                groupName, tenantId, operatorId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void addPermissionToGroup(String groupName, String permissionCode) {
        String tenantId = getTenantId();

        // 1. 권한 그룹 조회
        PermissionGroupJpaEntity group = permissionGroupRepository.findByTenantIdAndName(tenantId, groupName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 2. 권한 조회
        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 3. 중복 확인
        if (permissionGroupPermissionRepository.existsByPermissionGroupIdAndPermissionId(group.getPermissionGroupId(), permission.getPermissionId())) {
            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_ALREADY_ASSIGNED);
        }

        // 4. 매핑 생성 및 저장
        com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionGroupPermissionJpaEntity mapping =
                com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionGroupPermissionJpaEntity.builder()
                        .permissionGroupId(group.getPermissionGroupId())
                        .permissionId(permission.getPermissionId())
                        .addedAt(LocalDateTime.now())
                        .build();

        permissionGroupPermissionRepository.save(mapping);

        log.info("[RBAC] 권한을 그룹에 추가: groupName={}, permissionCode={}, tenantId={}",
                groupName, permissionCode, tenantId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void removePermissionFromGroup(String groupName, String permissionCode) {
        String tenantId = getTenantId();

        // 1. 권한 그룹 조회
        PermissionGroupJpaEntity group = permissionGroupRepository.findByTenantIdAndName(tenantId, groupName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 2. 권한 조회
        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 3. 매핑 삭제
        permissionGroupPermissionRepository.deleteByPermissionGroupIdAndPermissionId(
                group.getPermissionGroupId(), permission.getPermissionId());

        log.info("[RBAC] 권한을 그룹에서 제거: groupName={}, permissionCode={}, tenantId={}",
                groupName, permissionCode, tenantId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void assignPermissionGroupToRole(String roleName, String groupName) {
        String tenantId = getTenantId();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        // 2. 권한 그룹 조회
        PermissionGroupJpaEntity group = permissionGroupRepository.findByTenantIdAndName(tenantId, groupName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 3. 비활성 그룹 할당 차단
        if (group.getIsActive() == null || !group.getIsActive()) {
            throw new RbacException(RbacException.RbacErrorCode.ROLE_NOT_ACTIVE);
        }

        // 4. 중복 할당 확인
        if (rolePermissionGroupRepository.existsByRoleIdAndPermissionGroupId(role.getRoleId(), group.getPermissionGroupId())) {
            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_ALREADY_ASSIGNED);
        }

        // 5. 매핑 생성 및 저장
        com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionGroupJpaEntity mapping =
                com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionGroupJpaEntity.builder()
                        .roleId(role.getRoleId())
                        .permissionGroupId(group.getPermissionGroupId())
                        .assignedAt(LocalDateTime.now())
                        .build();

        rolePermissionGroupRepository.save(mapping);

        log.info("[RBAC] 권한 그룹을 역할에 할당: roleName={}, groupName={}, tenantId={}",
                roleName, groupName, tenantId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void revokePermissionGroupFromRole(String roleName, String groupName) {
        String tenantId = getTenantId();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        // 2. 권한 그룹 조회
        PermissionGroupJpaEntity group = permissionGroupRepository.findByTenantIdAndName(tenantId, groupName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 3. 매핑 삭제
        rolePermissionGroupRepository.deleteByRoleIdAndPermissionGroupId(role.getRoleId(), group.getPermissionGroupId());

        log.info("[RBAC] 권한 그룹을 역할에서 회수: roleName={}, groupName={}, tenantId={}",
                roleName, groupName, tenantId);
    }

    // ============================================================
    // 권한 변경 이력 조회 (Audit Log) 구현
    // ============================================================

    /**
     * 특정 사용자의 권한 변경 이력 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> getAgentPermissionChangeHistory(String agentId, LocalDateTime from, LocalDateTime to) {
        String tenantId = getTenantId();

        return auditLogService.getAgentPermissionChangeHistory(tenantId, agentId, from, to)
                .stream()
                .map(AuditLogDto::from)
                .toList();
    }

    /**
     * 특정 역할의 권한 변경 이력 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> getRolePermissionChangeHistory(String roleName, LocalDateTime from, LocalDateTime to) {
        String tenantId = getTenantId();

        return auditLogService.getRolePermissionChangeHistory(tenantId, roleName, from, to)
                .stream()
                .map(AuditLogDto::from)
                .toList();
    }

    /**
     * 전체 권한 변경 이력 조회 (관리자용)
     */
    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> getAllPermissionChangeHistory(LocalDateTime from, LocalDateTime to, Integer pageSize) {
        String tenantId = getTenantId();

        // 페이지 크기 제한 (최대 1000개)
        if (pageSize == null || pageSize <= 0) {
            pageSize = 100;
        } else if (pageSize > 1000) {
            pageSize = 1000;
        }

        return auditLogService.getAllPermissionChangeHistory(tenantId, from, to, pageSize)
                .stream()
                .map(AuditLogDto::from)
                .toList();
    }

    /**
     * 특정 작업자의 권한 관련 작업 이력 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> getOperatorPermissionActions(String operatorId, LocalDateTime from, LocalDateTime to) {
        String tenantId = getTenantId();

        return auditLogService.getOperatorPermissionActions(tenantId, operatorId, from, to)
                .stream()
                .map(AuditLogDto::from)
                .toList();
    }
}
