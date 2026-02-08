package com.nexfron.identitymodulith.rbac.application.service;

import com.nexfron.identitymodulith.common.security.TenantContextHolder;
import com.nexfron.identitymodulith.rbac.RbacModuleApi;
import com.nexfron.identitymodulith.rbac.application.exception.RbacException;
// ...existing code...
import com.nexfron.identitymodulith.rbac.domain.RoleType;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.AgentRoleJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.AgentRoleJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.PermissionJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.RoleJpaRepository;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RBAC 관리 서비스 구현체
 *
 * 역할(Role), 권한(Permission) CRUD 관리
 * 역할-권한, 사용자-역할 매핑 관리
 * 멀티테넌시 격리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RbacManagementServiceImpl implements RbacManagementService, RbacModuleApi {

    private final RoleJpaRepository roleRepository;
    private final PermissionJpaRepository permissionRepository;
    private final RolePermissionJpaRepository rolePermissionRepository;
    private final AgentRoleJpaRepository agentRoleRepository;
    private final RbacQueryService rbacQueryService;

    /**
     * 현재 요청의 tenantId 추출
     */
    private String getTenantId() {
        return TenantContextHolder.getCurrentTenantId();
    }

    /**
     * 현재 사용자 ID 조회
     */
    private String getCurrentUserId() {
        return TenantContextHolder.getCurrentUserId();
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
        String tenantId = TenantContextHolder.getCurrentTenantId();
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
        String tenantId = TenantContextHolder.getCurrentTenantId();
        String currentUserId = getCurrentUserId();
        log.info("[RBAC] 역할 생성 - tenantId={}, name={}, actorId={}", tenantId, request.name(), currentUserId);

        // RC-004: ADMIN 권한 검증
        checkAdminPermission(currentUserId, "역할 생성");

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
                .description(null)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 3. 저장
        RoleJpaEntity savedRole = roleRepository.save(role);


        log.info("[RBAC] 역할 생성 완료 - roleId={}", savedRole.getRoleId());

        return new RoleDto(
                savedRole.getName(),
                savedRole.getType(),
                savedRole.getDescription(),
                savedRole.getIsActive()
        );
    }

    /**
     * 역할 정보 업데이트 (type, description, isActive 중 변경할 항목만 포함)
     */
    @Override
    @Transactional
    public RoleDto updateRole(String roleName, UpdateRoleRequest request) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

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

        log.info("[RBAC] 역할 업데이트 완료 - tenantId: {}, roleName: {}", tenantId, roleName);

        return new RoleDto(
                updatedRole.getName(),
                updatedRole.getType(),
                updatedRole.getDescription(),
                updatedRole.getIsActive()
        );
    }

    @Override
    public List<PermissionDto> getAllPermissions() {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        return permissionRepository.findByTenantId(tenantId).stream()
                .map(perm -> new PermissionDto(perm.getCode(), perm.getDescription(), perm.getCategory()))
                .collect(Collectors.toList());
    }

    @Override
    public PermissionDto getPermissionByCode(String code) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        return new PermissionDto(permission.getCode(), permission.getDescription(), permission.getCategory());
    }

    @Override
    @Transactional
    public PermissionDto createPermission(CreatePermissionRequest request) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.info("[RBAC] 권한 생성 - tenantId={}, code={}", tenantId, request.code());

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


        log.info("[RBAC] 권한 생성 완료 - permissionId={}", savedPermission.getPermissionId());

        return new PermissionDto(savedPermission.getCode(), savedPermission.getDescription(), savedPermission.getCategory());
    }

    /**
     * 권한 정보 업데이트 (code, description 중 변경할 항목만 포함)
     */
    @Override
    @Transactional
    public PermissionDto updatePermission(String code, UpdatePermissionRequest request) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

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

        log.info("[RBAC] 권한 업데이트 완료 - tenantId: {}, code: {}", tenantId, code);

        return new PermissionDto(updatedPermission.getCode(), updatedPermission.getDescription(), updatedPermission.getCategory());
    }

    @Override
    @Transactional
    public void assignPermissionToRole(String roleName, String permissionCode) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        String currentUserId = getCurrentUserId();

        // PA-004: ADMIN 권한 검증
        checkAdminPermission(currentUserId, "권한 할당");

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        // 2. 권한 조회
        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 3. 매핑 생성 (DB UNIQUE 제약으로 중복 방지)
        RolePermissionJpaEntity mapping = RolePermissionJpaEntity.builder()
                .roleId(role.getRoleId())
                .permissionId(permission.getPermissionId())
                .assignedAt(LocalDateTime.now())
                .build();

        // 4. 저장
        try {
            rolePermissionRepository.save(mapping);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("[RBAC] 중복 권한 할당 차단 - roleName={}, permissionCode={}", roleName, permissionCode);
            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_ALREADY_ASSIGNED);
        }


        log.info("[RBAC] 역할-권한 할당: roleName={}, permissionCode={}", roleName, permissionCode);
    }

    @Override
    @Transactional
    public void revokePermissionFromRole(String roleName, String permissionCode) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        // 2. 권한 조회
        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 3. 매핑 삭제
        rolePermissionRepository.deleteByRoleIdAndPermissionId(role.getRoleId(), permission.getPermissionId());


        log.info("[RBAC] 역할-권한 회수: roleName={}, permissionCode={}", roleName, permissionCode);
    }

    /**
     * 여러 권한을 한 번에 역할에 할당
     */
    @Override
    @Transactional
    public BatchAssignmentResult batchAssignPermissionsToRole(String roleName, Set<String> permissionCodes) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        // 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        for (String permissionCode : permissionCodes) {
            try {
                // 권한 조회
                PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
                        .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

                // 이미 할당되었는지 확인
                boolean exists = rolePermissionRepository.existsByRoleIdAndPermissionId(
                        role.getRoleId(), permission.getPermissionId());

                if (exists) {
                    skippedCount++;
                    continue;
                }

                // 매핑 생성
                RolePermissionJpaEntity mapping = RolePermissionJpaEntity.builder()
                        .roleId(role.getRoleId())
                        .permissionId(permission.getPermissionId())
                        .build();

                rolePermissionRepository.save(mapping);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                errors.add("권한 '" + permissionCode + "' 할당 실패: " + e.getMessage());
            }
        }

        log.info("[RBAC] 대량 권한 할당 완료 - roleName={}, 성공={}, 실패={}, 건너뜀={}",
                roleName, successCount, failedCount, skippedCount);

        return new BatchAssignmentResult(successCount, failedCount, skippedCount, errors);
    }

    /**
     * 여러 권한을 한 번에 역할에서 회수
     */
    @Override
    @Transactional
    public BatchAssignmentResult batchRevokePermissionsFromRole(String roleName, Set<String> permissionCodes) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        // 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        for (String permissionCode : permissionCodes) {
            try {
                // 권한 조회
                PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
                        .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

                // 할당 여부 확인
                boolean exists = rolePermissionRepository.existsByRoleIdAndPermissionId(
                        role.getRoleId(), permission.getPermissionId());

                if (!exists) {
                    skippedCount++;
                    continue;
                }

                // 매핑 삭제
                rolePermissionRepository.deleteByRoleIdAndPermissionId(role.getRoleId(), permission.getPermissionId());
                successCount++;
            } catch (Exception e) {
                failedCount++;
                errors.add("권한 '" + permissionCode + "' 회수 실패: " + e.getMessage());
            }
        }

        log.info("[RBAC] 대량 권한 회수 완료 - roleName={}, 성공={}, 실패={}, 건너뜀={}",
                roleName, successCount, failedCount, skippedCount);

        return new BatchAssignmentResult(successCount, failedCount, skippedCount, errors);
    }

    /**
     * 특정 역할의 모든 권한 조회
     */
    @Override
    @Transactional(readOnly = true)
    public Set<PermissionDto> getPermissionsByRole(String roleName) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        // 2. 권한 ID 조회
        List<String> permissionIds = rolePermissionRepository
                .findByRoleId(role.getRoleId())
                .stream()
                .map(RolePermissionJpaEntity::getPermissionId)
                .collect(Collectors.toList());

        // 3. 권한 엔티티 조회 후 DTO 변환
        return permissionIds.stream()
                .map(permissionRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(perm -> perm.getTenantId().equals(tenantId))
                .map(perm -> new PermissionDto(perm.getCode(), perm.getDescription(), perm.getCategory()))
                .collect(Collectors.toSet());
    }

    // ========== 사용자-역할 관리 메서드 ==========

    @Override
    @Transactional
    public void assignRoleToAgent(String agentId, String roleName) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        log.info("[RBAC] 역할 할당 시작 - agentId={}, roleName={}", agentId, roleName);

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> {
                    log.warn("[RBAC] 역할 없음 - roleName={}", roleName);
                    return new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
                });

        // 2. 비활성 역할 할당 차단
        if (role.getIsActive() == null || !role.getIsActive()) {
            log.warn("[RBAC] 비활성 역할 - roleName={}", roleName);
            throw new RbacException(RbacException.RbacErrorCode.ROLE_NOT_ACTIVE);
        }

        // 3. 🔴 RA-005: RETIRED 상담사 역할 변경 불가 (User 모듈 통합 후 활성화 예정)
        // TODO: User 모듈 UserModuleApi를 통해 Agent 상태 검증
        // AgentExternalInfo agentInfo = userModuleApi.findAgentById(tenantId, UUID.fromString(agentId))
        //     .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.AGENT_NOT_FOUND));
        // if (agentInfo.getStatus() == AgentStatus.RETIRED) {
        //     log.warn("[RBAC] RETIRED 상담사 역할 할당 차단 - agentId={}", agentId);
        //     throw new RbacException(RbacException.RbacErrorCode.AGENT_ALREADY_RETIRED);
        // }

        // 4. 기존 역할 조회
        List<AgentRoleJpaEntity> existingRoles = agentRoleRepository.findByAgentId(agentId);

        // 5. ✅ POSITION 역할인 경우 기존 POSITION 자동 교체 (RA-003, 7.3절)
        if (role.getType() == RoleType.POSITION) {
            for (AgentRoleJpaEntity ar : existingRoles) {
                RoleJpaEntity existingRole = roleRepository.findById(ar.getRoleId()).orElse(null);

                if (existingRole != null && existingRole.getType() == RoleType.POSITION) {
                    log.info("[RBAC] 기존 POSITION 역할 자동 제거 - agentId={}, oldRole={}, newRole={}",
                            agentId, existingRole.getName(), roleName);
                    agentRoleRepository.delete(ar);
                }
            }
        }

        // 6. ✅ CHANNEL 역할인 경우 동일 CHANNEL 중복 방지 (문서 7.3절)
        if (role.getType() == RoleType.CHANNEL) {
            for (AgentRoleJpaEntity ar : existingRoles) {
                RoleJpaEntity existingRole = roleRepository.findById(ar.getRoleId()).orElse(null);

                if (existingRole != null
                    && existingRole.getType() == RoleType.CHANNEL
                    && existingRole.getName().equals(roleName)) {
                    log.warn("[RBAC] 동일 CHANNEL 역할 중복 할당 차단 - agentId={}, roleName={}",
                            agentId, roleName);
                    return; // 이미 할당된 경우 무시
                }
            }
        }

        // 7. 매핑 생성
        AgentRoleJpaEntity mapping = AgentRoleJpaEntity.builder()
                .agentId(agentId)
                .roleId(role.getRoleId())
                .assignedAt(LocalDateTime.now())
                .build();

        // 8. 저장 (DB UNIQUE 제약으로 중복 방지)
        try {
            agentRoleRepository.save(mapping);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("[RBAC] 중복 할당 차단 - agentId={}, roleName={}", agentId, roleName);
            // RA-004: 이미 할당된 경우 무시
            return;
        }

        log.info("[RBAC] 역할 할당 완료 - agentId={}, roleName={}, type={}",
                agentId, roleName, role.getType());
    }

    @Override
    @Transactional
    public void revokeRoleFromAgent(String agentId, String roleName) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
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


        long duration = System.currentTimeMillis() - startTime;
        log.info("[RBAC 역할 회수] agentId={}, roleName={}, roleId={}, 소요시간={}ms", agentId, roleName, role.getRoleId(), duration);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getRolesByAgent(String agentId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

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
        String tenantId = TenantContextHolder.getCurrentTenantId();

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
    public RoleDeletionResult deleteRole(String roleName, boolean forceDelete) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

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

        // 7. 역할 삭제
        roleRepository.delete(role);


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
        String tenantId = TenantContextHolder.getCurrentTenantId();

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
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.info("[RBAC] 역할 비활성화 - tenantId={}, roleName={}", tenantId, roleName);

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        if (!role.getIsActive()) {
            log.warn("[RBAC] 이미 비활성화된 역할 - roleName={}", roleName);
            return; // 이미 비활성화된 경우 무시
        }

        role.setIsActive(false);
        roleRepository.save(role);

        log.info("[RBAC] 역할 비활성화 완료 - roleId={}", role.getRoleId());
    }

    @Override
    @Transactional
    public void activateRole(String roleName) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.info("[RBAC] 역할 활성화 - tenantId={}, roleName={}", tenantId, roleName);

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        if (role.getIsActive()) {
            log.warn("[RBAC] 이미 활성화된 역할 - roleName={}", roleName);
            return; // 이미 활성화된 경우 무시
        }

        role.setIsActive(true);
        roleRepository.save(role);

        log.info("[RBAC] 역할 활성화 완료 - roleId={}", role.getRoleId());
    }

    @Override
    @Transactional
    public void deletePermission(String code) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 권한에 매핑된 모든 역할 관계 삭제 (카스케이드)
        rolePermissionRepository.deleteByPermissionId(permission.getPermissionId());

        // 권한 삭제
        permissionRepository.delete(permission);

        log.info("[RBAC] 권한 삭제: code={}, tenantId={}", code, tenantId);
    }


    /**
     * 사용자의 실제 권한 조회 (역할 → 권한 변환)
     */
    @Override
    @Transactional(readOnly = true)
    public Set<String> getEffectivePermissions(String agentId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.debug("[RBAC] 사용자 실제 권한 조회 - tenantId={}, agentId={}", tenantId, agentId);

        // 1. 사용자의 모든 역할 조회
        List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByAgentId(agentId);

        if (agentRoles.isEmpty()) {
            log.debug("[RBAC] 사용자에게 할당된 역할 없음 - agentId={}", agentId);
            return Collections.emptySet();
        }

        // 2. 각 역할의 권한 조회
        Set<String> permissionCodes = new HashSet<>();
        for (AgentRoleJpaEntity agentRole : agentRoles) {
            List<RolePermissionJpaEntity> rolePermissions =
                    rolePermissionRepository.findByRoleId(agentRole.getRoleId());

            for (RolePermissionJpaEntity rp : rolePermissions) {
                PermissionJpaEntity permission = permissionRepository.findById(rp.getPermissionId())
                        .orElse(null);
                if (permission != null) {
                    permissionCodes.add(permission.getCode());
                }
            }
        }

        log.debug("[RBAC] 사용자 실제 권한 조회 완료 - agentId={}, permissionCount={}",
                agentId, permissionCodes.size());
        return permissionCodes;
    }

    /**
     * 특정 권한을 가진 역할 조회 (역검색)
     */
    @Override
    @Transactional(readOnly = true)
    public Set<String> getRolesWithPermission(String permissionCode) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.debug("[RBAC] 권한을 가진 역할 조회 - tenantId={}, permissionCode={}", tenantId, permissionCode);

        // 1. 권한 존재 확인
        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 2. 해당 권한을 가진 역할-권한 매핑 조회
        List<RolePermissionJpaEntity> rolePermissions =
                rolePermissionRepository.findByPermissionId(permission.getPermissionId());

        // 3. 역할명 추출
        Set<String> roleNames = new HashSet<>();
        for (RolePermissionJpaEntity rp : rolePermissions) {
            RoleJpaEntity role = roleRepository.findById(rp.getRoleId()).orElse(null);
            if (role != null && role.getTenantId().equals(tenantId)) {
                roleNames.add(role.getName());
            }
        }

        log.debug("[RBAC] 권한을 가진 역할 조회 완료 - permissionCode={}, roleCount={}",
                permissionCode, roleNames.size());
        return roleNames;
    }

    /**
     * 역할 복사 (권한 포함)
     */
    @Override
    @Transactional
    public RoleDto cloneRole(String sourceRoleName, CloneRoleRequest request) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.info("[RBAC] 역할 복사 - tenantId={}, source={}, target={}",
                tenantId, sourceRoleName, request.newRoleName());

        // 1. 원본 역할 존재 확인
        RoleJpaEntity sourceRole = roleRepository.findByTenantIdAndName(tenantId, sourceRoleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        // 2. 새 역할명 중복 확인
        if (roleRepository.existsByTenantIdAndName(tenantId, request.newRoleName())) {
            throw new RbacException(RbacException.RbacErrorCode.ROLE_ALREADY_EXISTS);
        }

        // 3. 새 역할 생성
        RoleJpaEntity newRole = RoleJpaEntity.builder()
                .roleId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .name(request.newRoleName())
                .type(sourceRole.getType())
                .description(request.description() != null ? request.description() :
                        "Cloned from " + sourceRoleName)
                .isActive(sourceRole.getIsActive())
                .createdAt(LocalDateTime.now())
                .build();

        roleRepository.save(newRole);

        // 4. 원본 역할의 권한 복사
        List<RolePermissionJpaEntity> sourcePermissions =
                rolePermissionRepository.findByRoleId(sourceRole.getRoleId());

        int copiedPermissionCount = 0;
        for (RolePermissionJpaEntity sourceRP : sourcePermissions) {
            RolePermissionJpaEntity newRP = RolePermissionJpaEntity.builder()
                    .roleId(newRole.getRoleId())
                    .permissionId(sourceRP.getPermissionId())
                    .assignedAt(LocalDateTime.now())
                    .build();

            rolePermissionRepository.save(newRP);
            copiedPermissionCount++;
        }

        log.info("[RBAC] 역할 복사 완료 - newRole={}, copiedPermissions={}",
                request.newRoleName(), copiedPermissionCount);

        return new RoleDto(
                newRole.getName(),
                newRole.getType(),
                newRole.getDescription(),
                newRole.getIsActive()
        );
    }

    // ============================================================
    // 헬퍼 메서드
    // ============================================================

    /**
     * ADMIN 권한 검증
     *
     * <h3>동작:</h3>
     * 1. 사용자의 모든 역할 조회
     * 2. ADMIN 역할 보유 여부 확인
     * 3. 없으면 예외 발생
     *
     * @param userId 사용자 ID (Agent ID)
     * @param action 수행하려는 작업 (로그용)
     * @throws RbacException ADMIN 역할이 없는 경우
     */
    private void checkAdminPermission(String userId, String action) {
        if (userId == null || userId.isEmpty()) {
            log.warn("[RBAC] 권한 검증 실패 - userId 없음, action={}", action);
            throw new RbacException(
                RbacException.RbacErrorCode.INSUFFICIENT_PERMISSION,
                "사용자 정보를 확인할 수 없습니다."
            );
        }

        // Agent의 역할 조회
        List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByAgentId(userId);

        if (agentRoles.isEmpty()) {
            log.warn("[RBAC] 권한 부족 - userId={}, action={}, reason=역할 없음",
                userId, action);
            throw new RbacException(
                RbacException.RbacErrorCode.INSUFFICIENT_PERMISSION,
                action + "은(는) ADMIN 역할이 필요합니다."
            );
        }

        // ADMIN 역할 보유 여부 확인
        boolean isAdmin = agentRoles.stream()
            .map(AgentRoleJpaEntity::getRoleId)
            .map(roleId -> roleRepository.findById(roleId).orElse(null))
            .filter(role -> role != null)
            .anyMatch(role -> "ADMIN".equals(role.getName()));

        if (!isAdmin) {
            log.warn("[RBAC] 권한 부족 - userId={}, action={}, reason=ADMIN 역할 필요",
                userId, action);
            throw new RbacException(
                RbacException.RbacErrorCode.INSUFFICIENT_PERMISSION,
                action + "은(는) ADMIN 역할이 필요합니다."
            );
        }

        log.debug("[RBAC] ADMIN 권한 확인 완료 - userId={}, action={}", userId, action);
    }

    // ============================================================
    // RbacModuleApi 구현 (모듈 간 통신용 Public API)
    // ============================================================

    /**
     * 사용자의 역할 정보를 조회합니다.
     * User 모듈에서 AgentExternalInfo 생성 시 사용됩니다.
     *
     * @param agentId 사용자 ID (UUID 문자열)
     * @return 사용자의 역할 정보 세트
     */
    @Override
    public Set<RbacModuleApi.RoleInfo> getRolesByAgentId(String agentId) {
        log.debug("[RBAC] 사용자 역할 조회 - agentId={}", agentId);

        List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByAgentId(agentId);

        Set<RbacModuleApi.RoleInfo> roleInfos = new HashSet<>();

        for (AgentRoleJpaEntity agentRole : agentRoles) {
            roleRepository.findById(agentRole.getRoleId()).ifPresent(role -> {
                // RoleType 변환
                RbacModuleApi.RoleInfo.RoleType roleType;
                try {
                    roleType = RbacModuleApi.RoleInfo.RoleType.valueOf(role.getType().name());
                } catch (IllegalArgumentException e) {
                    roleType = RbacModuleApi.RoleInfo.RoleType.POSITION;
                }

                // DataScopeLevel 변환
                RbacModuleApi.RoleInfo.DataScopeLevel dataScopeLevel;
                if (role.getDataScope() != null) {
                    try {
                        dataScopeLevel = RbacModuleApi.RoleInfo.DataScopeLevel.valueOf(role.getDataScope().name());
                    } catch (IllegalArgumentException e) {
                        dataScopeLevel = RbacModuleApi.RoleInfo.DataScopeLevel.SELF;
                    }
                } else {
                    dataScopeLevel = RbacModuleApi.RoleInfo.DataScopeLevel.SELF;
                }

                roleInfos.add(new RbacModuleApi.RoleInfo(
                        role.getName(),
                        roleType,
                        dataScopeLevel
                ));
            });
        }

        log.debug("[RBAC] 사용자 역할 조회 완료 - agentId={}, roleCount={}", agentId, roleInfos.size());
        return roleInfos;
    }

    @Override
    public Set<String> getPermissionsByAgentId(String tenantId, String agentId) {
        UUID agentUuid = UUID.fromString(agentId);
        Set<String> permissions = rbacQueryService.permissionsOf(tenantId, agentUuid);
        log.debug("[RBAC] 사용자 권한 조회 - agentId={}, permissionCount={}", agentId, permissions.size());
        return permissions;
    }
}
