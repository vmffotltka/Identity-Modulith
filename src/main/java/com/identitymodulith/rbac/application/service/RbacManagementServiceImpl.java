package com.identitymodulith.rbac.application.service;

import com.identitymodulith.common.security.context.TenantContextHolder;
import com.identitymodulith.common.domain.DataScopeLevel;
import com.identitymodulith.rbac.RbacModuleApi;
import com.identitymodulith.rbac.application.exception.RbacException;
import com.identitymodulith.rbac.domain.RoleType;
import com.identitymodulith.rbac.infrastructure.persistence.entity.AgentRoleJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.repository.AgentRoleJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.PermissionJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.RoleJpaRepository;
import com.identitymodulith.rbac.infrastructure.persistence.repository.RolePermissionJpaRepository;
import com.identitymodulith.rbac.presentation.dto.request.*;
import com.identitymodulith.rbac.presentation.dto.response.*;
import com.identitymodulith.rbac.application.port.AgentValidationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** RBAC 관리 유스케이스 구현체. */
@Service
@Transactional(readOnly = true)
@Slf4j
public class RbacManagementServiceImpl implements RbacManagementService, RbacModuleApi {

    private final RoleJpaRepository roleRepository;
    private final PermissionJpaRepository permissionRepository;
    private final RolePermissionJpaRepository rolePermissionRepository;
    private final AgentRoleJpaRepository agentRoleRepository;
    private final RbacQueryService rbacQueryService;
    private final AgentValidationPort agentValidationPort;

    public RbacManagementServiceImpl(
            RoleJpaRepository roleRepository,
            PermissionJpaRepository permissionRepository,
            RolePermissionJpaRepository rolePermissionRepository,
            AgentRoleJpaRepository agentRoleRepository,
            RbacQueryService rbacQueryService,
            AgentValidationPort agentValidationPort) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.agentRoleRepository = agentRoleRepository;
        this.rbacQueryService = rbacQueryService;
        this.agentValidationPort = agentValidationPort;
    }

    private String getTenantId() {
        return TenantContextHolder.getCurrentTenantId();
    }

    @Override
    public List<RoleResponse> getAllRoles() {
        String tenantId = getTenantId();
        return roleRepository.findByTenantId(tenantId).stream()
                .map(role -> {
                    Set<PermissionResponse> permissions = getPermissionsByRoleName(role.getName());

                    int userCount = getAgentCountByRoleId(role.getRoleId());

                    return new RoleResponse(
                            role.getRoleId(),
                            role.getName(),
                            role.getType(),
                            role.getDataScope() != null ? role.getDataScope().name() : null,
                            role.getDescription(),
                            role.getIsActive(),
                            permissions,
                            userCount,
                            role.getCreatedAt(),
                            role.getUpdatedAt()
                    );
                })
                .toList();
    }

    private Set<PermissionResponse> getPermissionsByRoleName(String roleName) {
        try {
            return getPermissionsByRole(roleName);
        } catch (Exception e) {
            log.warn("[RBAC] 권한 조회 실패 - roleName={}", roleName);
            return Set.of();
        }
    }

    private int getAgentCountByRoleId(String roleId) {
        try {
            List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByRoleId(roleId);
            return agentRoles.size();
        } catch (Exception e) {
            log.warn("[RBAC] 사용자 수 조회 실패 - roleId={}", roleId);
            return 0;
        }
    }

    @Override
    public RoleResponse getRoleByName(String roleName) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        Set<PermissionResponse> permissions = getPermissionsByRole(roleName);

        int userCount = getAgentCountByRole(roleName);

        return new RoleResponse(
                role.getRoleId(),
                role.getName(),
                role.getType(),
                role.getDataScope() != null ? role.getDataScope().name() : null,
                role.getDescription(),
                role.getIsActive(),
                permissions,
                userCount,
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request, String userId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.info("[RBAC] 역할 생성 - tenantId={}, name={}, actorId={}", tenantId, request.name(), userId);

        checkAdminPermission(userId, "역할 생성");

        boolean exists = roleRepository.existsByTenantIdAndName(tenantId, request.name());
        log.info("[RBAC] 역할 중복 체크 - tenantId={}, name={}, exists={}", tenantId, request.name(), exists);

        if (exists) {
            log.warn("[RBAC] 역할 생성 실패 - 이미 존재하는 역할명: {}", request.name());
            RbacException exception = new RbacException(RbacException.RbacErrorCode.ROLE_ALREADY_EXISTS);
            log.error("[RBAC] 예외 생성 완료, throw 직전 - exception={}", exception.getClass().getName());
            throw exception;
        }

        RoleJpaEntity role = RoleJpaEntity.builder()
                .roleId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .name(request.name())
                .type(request.type())
                .description(request.description())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        RoleJpaEntity savedRole = roleRepository.save(role);


        log.info("[RBAC] 역할 생성 완료 - roleId={}", savedRole.getRoleId());

        return new RoleResponse(
                savedRole.getName(),
                savedRole.getType(),
                savedRole.getDescription(),
                savedRole.getIsActive()
        );
    }

    /** 전달된 값만 반영해 역할 정보를 부분 업데이트한다. */
    @Override
    @Transactional
    public RoleResponse updateRole(String roleName, UpdateRoleRequest request, String userId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        checkAdminPermission(userId, "역할 수정");

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        if (request.type() != null) {
            role.setType(request.type());
        }
        if (request.description() != null) {
            role.setDescription(request.description());
        }
        if (request.dataScopeLevel() != null) {
            try {
                DataScopeLevel dataScope =
                        DataScopeLevel.valueOf(request.dataScopeLevel());
                role.setDataScope(dataScope);
            } catch (IllegalArgumentException e) {
                log.warn("[RBAC] 잘못된 DataScopeLevel - value={}", request.dataScopeLevel());
                throw new RbacException(RbacException.RbacErrorCode.INTERNAL_ERROR);
            }
        }
        if (request.isActive() != null) {
            role.setIsActive(request.isActive());
        }
        role.setUpdatedAt(LocalDateTime.now());
        role.setUpdatedBy(userId);

        RoleJpaEntity updatedRole = roleRepository.save(role);

        log.info("[RBAC] 역할 업데이트 완료 - tenantId: {}, roleName: {}", tenantId, roleName);

        Set<PermissionResponse> permissions = getPermissionsByRoleName(updatedRole.getName());

        int userCount = getAgentCountByRoleId(updatedRole.getRoleId());

        return new RoleResponse(
                updatedRole.getRoleId(),
                updatedRole.getName(),
                updatedRole.getType(),
                updatedRole.getDataScope() != null ? updatedRole.getDataScope().name() : null,
                updatedRole.getDescription(),
                updatedRole.getIsActive(),
                permissions,
                userCount,
                updatedRole.getCreatedAt(),
                updatedRole.getUpdatedAt()
        );
    }

    @Override
    public List<PermissionResponse> getAllPermissions() {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        return permissionRepository.findByTenantId(tenantId).stream()
                .map(perm -> new PermissionResponse(perm.getCode(), perm.getDescription(), perm.getCategory()))
                .toList();
    }

    @Override
    public PermissionResponse getPermissionByCode(String code) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        return new PermissionResponse(permission.getCode(), permission.getDescription(), permission.getCategory());
    }

    @Override
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request, String userId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.info("[RBAC] 권한 생성 - tenantId={}, code={}, actorId={}", tenantId, request.code(), userId);

        checkAdminPermission(userId, "권한 생성");

        boolean exists = permissionRepository.existsByTenantIdAndCode(tenantId, request.code());
        log.info("[RBAC] 권한 중복 체크 - tenantId={}, code={}, exists={}", tenantId, request.code(), exists);

        if (exists) {
            log.warn("[RBAC] 권한 생성 실패 - 이미 존재하는 권한 코드: {}", request.code());
            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_ALREADY_EXISTS);
        }

        PermissionJpaEntity permission = PermissionJpaEntity.builder()
                .permissionId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .category(request.category())
                .resource(request.resource())
                .action(request.action())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("[RBAC] 권한 엔티티 생성 완료, 저장 시도 - permissionId={}", permission.getPermissionId());

        PermissionJpaEntity savedPermission = permissionRepository.save(permission);

        log.info("[RBAC] 권한 저장 완료 - permissionId={}", savedPermission.getPermissionId());

        return new PermissionResponse(savedPermission.getCode(), savedPermission.getDescription(), savedPermission.getCategory());
    }

    /** 권한 정보를 부분 업데이트한다. */
    @Override
    @Transactional
    public PermissionResponse updatePermission(String code, UpdatePermissionRequest request, String userId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        checkAdminPermission(userId, "권한 수정");

        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        if (request.code() != null && !request.code().equals(code)) {
            if (permissionRepository.existsByTenantIdAndCode(tenantId, request.code())) {
                throw new RbacException(RbacException.RbacErrorCode.PERMISSION_ALREADY_EXISTS);
            }
            permission.setCode(request.code());
        }
        if (request.description() != null) {
            permission.setDescription(request.description());
        }
        if (request.category() != null) {
            permission.setCategory(request.category());
        }
        permission.setUpdatedAt(LocalDateTime.now());

        PermissionJpaEntity updatedPermission = permissionRepository.save(permission);

        log.info("[RBAC] 권한 업데이트 완료 - tenantId: {}, code: {}", tenantId, code);

        return new PermissionResponse(updatedPermission.getCode(), updatedPermission.getDescription(), updatedPermission.getCategory());
    }

    @Override
    @Transactional
    public void assignPermissionToRole(String roleName, String permissionCode, String userId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        checkAdminPermission(userId, "권한 할당");

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        RolePermissionJpaEntity mapping = RolePermissionJpaEntity.builder()
                .roleId(role.getRoleId())
                .permissionId(permission.getPermissionId())
                .assignedAt(LocalDateTime.now())
                .build();

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
    public void revokePermissionFromRole(String roleName, String permissionCode, String userId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        checkAdminPermission(userId, "권한 제거");

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        rolePermissionRepository.deleteByRoleIdAndPermissionId(role.getRoleId(), permission.getPermissionId());


        log.info("[RBAC] 역할-권한 회수: roleName={}, permissionCode={}", roleName, permissionCode);
    }

    /** 여러 권한을 역할에 일괄 할당한다. */
    @Override
    @Transactional
    public BatchAssignmentResponse batchAssignPermissionsToRole(String roleName, Set<String> permissionCodes, String userId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        checkAdminPermission(userId, "권한 일괄 할당");

        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        for (String permissionCode : permissionCodes) {
            try {
                PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
                        .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

                boolean exists = rolePermissionRepository.existsByRoleIdAndPermissionId(
                        role.getRoleId(), permission.getPermissionId());

                if (exists) {
                    skippedCount++;
                    continue;
                }

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

        return new BatchAssignmentResponse(successCount, failedCount, skippedCount, errors);
    }

    /** 여러 권한을 역할에서 일괄 회수한다. */
    @Override
    @Transactional
    public BatchAssignmentResponse batchRevokePermissionsFromRole(String roleName, Set<String> permissionCodes, String userId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        checkAdminPermission(userId, "권한 일괄 제거");

        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        for (String permissionCode : permissionCodes) {
            try {
                PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, permissionCode)
                        .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

                boolean exists = rolePermissionRepository.existsByRoleIdAndPermissionId(
                        role.getRoleId(), permission.getPermissionId());

                if (!exists) {
                    skippedCount++;
                    continue;
                }

                rolePermissionRepository.deleteByRoleIdAndPermissionId(role.getRoleId(), permission.getPermissionId());
                successCount++;
            } catch (Exception e) {
                failedCount++;
                errors.add("권한 '" + permissionCode + "' 회수 실패: " + e.getMessage());
            }
        }

        log.info("[RBAC] 대량 권한 회수 완료 - roleName={}, 성공={}, 실패={}, 건너뜀={}",
                roleName, successCount, failedCount, skippedCount);

        return new BatchAssignmentResponse(successCount, failedCount, skippedCount, errors);
    }

    /** 특정 역할의 권한 목록을 조회한다. */
    @Override
    @Transactional(readOnly = true)
    public Set<PermissionResponse> getPermissionsByRole(String roleName) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        List<String> permissionIds = rolePermissionRepository
                .findByRoleId(role.getRoleId())
                .stream()
                .map(RolePermissionJpaEntity::getPermissionId)
                .toList();

        return permissionIds.stream()
                .map(permissionRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(perm -> perm.getTenantId().equals(tenantId))
                .map(perm -> new PermissionResponse(perm.getCode(), perm.getDescription(), perm.getCategory()))
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void assignRoleToAgent(String agentId, String roleName) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        log.info("[RBAC] 역할 할당 시작 - agentId={}, roleName={}", agentId, roleName);

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> {
                    log.warn("[RBAC] 역할 없음 - roleName={}", roleName);
                    return new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
                });

        if (role.getIsActive() == null || !role.getIsActive()) {
            log.warn("[RBAC] 비활성 역할 - roleName={}", roleName);
            throw new RbacException(RbacException.RbacErrorCode.ROLE_NOT_ACTIVE);
        }

        // RA-005: RETIRED/비활성 상담사는 역할 변경 불가.
        if (!agentValidationPort.isActiveAgent(agentId)) {
            log.warn("[RBAC] 비활성/퇴사 상담사 역할 할당 차단 - agentId={}", agentId);
            throw new RbacException(RbacException.RbacErrorCode.AGENT_RETIRED);
        }

        List<AgentRoleJpaEntity> existingRoles = agentRoleRepository.findByAgentId(agentId);

        // RA-003: POSITION 역할은 기존 POSITION을 교체한다.
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

        // 동일 CHANNEL 역할 중복 할당은 차단한다.
        if (role.getType() == RoleType.CHANNEL) {
            for (AgentRoleJpaEntity ar : existingRoles) {
                RoleJpaEntity existingRole = roleRepository.findById(ar.getRoleId()).orElse(null);

                if (existingRole != null
                        && existingRole.getType() == RoleType.CHANNEL
                        && existingRole.getName().equals(roleName)) {
                    log.warn("[RBAC] 동일 CHANNEL 역할 중복 할당 차단 - agentId={}, roleName={}",
                            agentId, roleName);
                    return;
                }
            }
        }

        AgentRoleJpaEntity mapping = AgentRoleJpaEntity.builder()
                .agentId(agentId)
                .roleId(role.getRoleId())
                .assignedAt(LocalDateTime.now())
                .build();

        try {
            agentRoleRepository.save(mapping);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("[RBAC] 중복 할당 차단 - agentId={}, roleName={}", agentId, roleName);
            // RA-004: 이미 할당된 역할은 멱등하게 무시한다.
            return;
        }

        log.info("[RBAC] 역할 할당 완료 - agentId={}, roleName={}, type={}",
                agentId, roleName, role.getType());
    }

    /** 권한 검증 후 사용자에게 역할을 할당한다. */
    @Override
    @Transactional
    public void assignRoleToAgent(String agentId, String roleName, String userId) {
        checkAdminPermission(userId, "역할 할당");
        assignRoleToAgent(agentId, roleName);
    }

    @Override
    @Transactional
    public void assignRoleToAgentByRoleId(String agentId, String roleId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.info("[RBAC] 역할 할당 시작 (roleId) - agentId={}, roleId={}", agentId, roleId);

        RoleJpaEntity role = roleRepository.findByTenantIdAndRoleId(tenantId, roleId)
                .orElseThrow(() -> {
                    log.warn("[RBAC] 역할 없음 - roleId={}", roleId);
                    return new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
                });

        if (!role.getIsActive()) {
            log.warn("[RBAC] 비활성화된 역할 할당 차단 - roleId={}, roleName={}", roleId, role.getName());
            throw new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
        }

        AgentRoleJpaEntity mapping = AgentRoleJpaEntity.builder()
                .agentId(agentId)
                .roleId(role.getRoleId())
                .assignedAt(LocalDateTime.now())
                .build();

        try {
            agentRoleRepository.save(mapping);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("[RBAC] 중복 할당 차단 - agentId={}, roleId={}", agentId, roleId);
            return;
        }

        log.info("[RBAC] 역할 할당 완료 (roleId) - agentId={}, roleId={}, roleName={}",
                agentId, roleId, role.getName());
    }

    @Override
    @Transactional
    public void assignRoleToAgentWithoutAutoReplace(String agentId, String roleName) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.info("[RBAC] 역할 할당 시작 (일괄 지정 모드) - agentId={}, roleName={}", agentId, roleName);

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> {
                    log.warn("[RBAC] 역할 없음 - roleName={}", roleName);
                    return new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
                });

        if (!role.getIsActive()) {
            log.warn("[RBAC] 비활성 역할 - roleName={}", roleName);
            throw new RbacException(RbacException.RbacErrorCode.ROLE_NOT_ACTIVE);
        }

        List<AgentRoleJpaEntity> existingRoles = agentRoleRepository.findByAgentId(agentId);

        // RA-003: POSITION 역할은 기존 POSITION을 교체한다.
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

        AgentRoleJpaEntity mapping = AgentRoleJpaEntity.builder()
                .agentId(agentId)
                .roleId(role.getRoleId())
                .assignedAt(LocalDateTime.now())
                .build();

        try {
            agentRoleRepository.save(mapping);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("[RBAC] 중복 할당 차단 - agentId={}, roleName={}", agentId, roleName);
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

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> {
                    log.warn("[RBAC] 역할 회수 실패: roleName={}, 존재하지 않음", roleName);
                    return new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
                });

        agentRoleRepository.findByAgentId(agentId).stream()
                .filter(ar -> ar.getRoleId().equals(role.getRoleId()))
                .forEach(agentRoleRepository::delete);


        long duration = System.currentTimeMillis() - startTime;
        log.info("[RBAC 역할 회수] agentId={}, roleName={}, roleId={}, 소요시간={}ms", agentId, roleName, role.getRoleId(), duration);
    }

    /** 권한 검증 후 사용자 역할을 회수한다. */
    @Override
    @Transactional
    public void revokeRoleFromAgent(String agentId, String roleName, String userId) {
        checkAdminPermission(userId, "역할 회수");
        revokeRoleFromAgent(agentId, roleName);
    }

    @Override
    @Transactional
    public void removeAllRolesFromAgent(String agentId) {
        long startTime = System.currentTimeMillis();
        log.debug("[RBAC] 사용자의 모든 역할 제거 시작 - agentId={}", agentId);

        agentRoleRepository.deleteByAgentId(agentId);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[RBAC] 사용자의 모든 역할 제거 완료 - agentId={}, 소요시간={}ms", agentId, duration);
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
    public boolean hasRole(String agentId, String roleName) {
        Set<String> roles = getRolesByAgent(agentId);
        boolean result = roles.contains(roleName);
        log.debug("[RBAC] 역할 확인 - agentId={}, roleName={}, hasRole={}", agentId, roleName, result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public int getAgentCountByRole(String roleName) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> {
                    log.warn("[RBAC] 역할 조회 실패: roleName={}", roleName);
                    return new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
                });

        List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByRoleId(role.getRoleId());

        log.debug("[RBAC] 역할별 사용자 수 조회: roleName={}, count={}", roleName, agentRoles.size());
        return agentRoles.size();
    }

    /** 역할 삭제(필요 시 강제 삭제)와 영향도를 함께 처리한다. */
    @Override
    @Transactional
    public RoleDeletionResponse deleteRole(String roleName, boolean forceDelete, String userId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        log.info("[RBAC] 역할 삭제 요청 - tenantId={}, roleName={}, forceDelete={}, userId={}",
                tenantId, roleName, forceDelete, userId);

        checkAdminPermission(userId, "역할 삭제");

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        log.info("[RBAC] 역할 조회 완료 - roleId={}", role.getRoleId());

        List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByRoleId(role.getRoleId());
        int affectedUserCount = agentRoles.size();

        log.info("[RBAC] 영향도 조회 - affectedUserCount={}", affectedUserCount);

        List<RolePermissionJpaEntity> rolePermissions = rolePermissionRepository.findByRoleId(role.getRoleId());
        int removedPermissionCount = rolePermissions.size();

        if (!forceDelete && affectedUserCount > 0) {
            log.warn("[RBAC] 역할 삭제 차단 - roleName={}, affectedUserCount={}, forceDelete={}",
                    roleName, affectedUserCount, forceDelete);
            String warningMessage = String.format(
                    "역할 '%s'을 %d명의 사용자가 사용 중이므로 삭제할 수 없습니다. " +
                            "강제 삭제하려면 force=true 옵션을 사용하세요.",
                    roleName, affectedUserCount
            );
            throw new RbacException(RbacException.RbacErrorCode.ROLE_HAS_USERS, warningMessage);
        }

        log.info("[RBAC] 역할 삭제 진행 - 사용자 체크 통과");

        if (forceDelete && affectedUserCount > 0) {
            log.info("[RBAC] 강제 삭제 모드: {}명의 사용자에서 역할 '{}' 회수 시작", affectedUserCount, roleName);
            agentRoleRepository.deleteByRoleId(role.getRoleId());
        }

        rolePermissionRepository.deleteByRoleId(role.getRoleId());

        roleRepository.delete(role);


        log.info("[RBAC] 역할 삭제 완료: roleName={}, 영향받은 사용자={}, 제거된 권한={}, 강제삭제={}",
                roleName, affectedUserCount, removedPermissionCount, forceDelete);

        return new RoleDeletionResponse(
                roleName,
                affectedUserCount,
                removedPermissionCount,
                forceDelete,
                forceDelete && affectedUserCount > 0
                        ? String.format("%d명의 사용자에서 역할이 회수되었습니다.", affectedUserCount)
                        : null
        );
    }

    /** 역할 삭제 전 영향도를 조회한다. */
    @Override
    @Transactional(readOnly = true)
    public RoleDeletionImpactResponse getRoleDeletionImpact(String roleName) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByRoleId(role.getRoleId());
        int affectedUserCount = agentRoles.size();

        List<RolePermissionJpaEntity> rolePermissions = rolePermissionRepository.findByRoleId(role.getRoleId());
        int assignedPermissionCount = rolePermissions.size();

        boolean canDelete = affectedUserCount == 0;

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

        return new RoleDeletionImpactResponse(
                roleName,
                affectedUserCount,
                assignedPermissionCount,
                canDelete,
                impactDetails.toString()
        );
    }

    @Override
    @Transactional
    public void deactivateRole(String roleName, String userId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.info("[RBAC] 역할 비활성화 - tenantId={}, roleName={}, actorId={}", tenantId, roleName, userId);

        checkAdminPermission(userId, "역할 비활성화");

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        if (!role.getIsActive()) {
            log.warn("[RBAC] 이미 비활성화된 역할 - roleName={}", roleName);
            return;
        }

        role.setIsActive(false);
        roleRepository.save(role);

        log.info("[RBAC] 역할 비활성화 완료 - roleId={}", role.getRoleId());
    }

    @Override
    @Transactional
    public void activateRole(String roleName, String userId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.info("[RBAC] 역할 활성화 - tenantId={}, roleName={}, actorId={}", tenantId, roleName, userId);

        checkAdminPermission(userId, "역할 활성화");

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        if (role.getIsActive()) {
            log.warn("[RBAC] 이미 활성화된 역할 - roleName={}", roleName);
            return;
        }

        role.setIsActive(true);
        roleRepository.save(role);

        log.info("[RBAC] 역할 활성화 완료 - roleId={}", role.getRoleId());
    }

    @Override
    @Transactional
    public void deletePermission(String code, String userId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();

        log.info("[RBAC] 권한 삭제 요청 - tenantId={}, code={}, userId={}", tenantId, code, userId);

        checkAdminPermission(userId, "권한 삭제");

        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        List<RolePermissionJpaEntity> rolePermissions = rolePermissionRepository.findByPermissionId(permission.getPermissionId());
        int assignedRoleCount = rolePermissions.size();

        log.info("[RBAC] 권한 영향도 조회 - assignedRoleCount={}", assignedRoleCount);

        if (assignedRoleCount > 0) {
            log.warn("[RBAC] 권한 삭제 차단 - code={}, assignedRoleCount={}", code, assignedRoleCount);

            List<String> roleNames = rolePermissions.stream()
                    .map(rp -> roleRepository.findById(rp.getRoleId()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(RoleJpaEntity::getName)
                    .collect(java.util.stream.Collectors.toList());

            String warningMessage = String.format(
                    "권한 '%s'이(가) %d개의 역할에서 사용 중이므로 삭제할 수 없습니다. 사용 중인 역할: %s",
                    code, assignedRoleCount, String.join(", ", roleNames)
            );

            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_IN_USE, warningMessage);
        }

        permissionRepository.delete(permission);

        log.info("[RBAC] 권한 삭제 완료: code={}, tenantId={}, actorId={}", code, tenantId, userId);
    }


    /** 사용자의 실제 권한 코드를 조회한다. */
    @Override
    @Transactional(readOnly = true)
    public Set<String> getEffectivePermissions(String agentId) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.debug("[RBAC] 사용자 실제 권한 조회 시작 - tenantId={}, agentId={}", tenantId, agentId);

        long start = System.currentTimeMillis();

        List<String> permissionCodes =
                agentRoleRepository.findPermissionCodesByAgentIdAndTenant(agentId, tenantId);

        Set<String> result = new HashSet<>(permissionCodes);
        long elapsed = System.currentTimeMillis() - start;

        log.info("[RBAC][PERF] getEffectivePermissions 완료 - agentId={}, permissionCount={}, elapsed={}ms",
                agentId, result.size(), elapsed);
        return result;
    }

    /** 특정 권한을 가진 역할명을 조회한다. */
    @Override
    @Transactional(readOnly = true)
    public Set<String> getRolesWithPermission(String permissionCode) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.debug("[RBAC] 권한을 가진 역할 조회 시작 - tenantId={}, permissionCode={}", tenantId, permissionCode);

        long start = System.currentTimeMillis();

        if (!permissionRepository.existsByTenantIdAndCode(tenantId, permissionCode)) {
            throw new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND);
        }

        Set<String> roleNames = new HashSet<>(
                rolePermissionRepository.findRoleNamesByPermissionCodeAndTenant(permissionCode, tenantId)
        );

        long elapsed = System.currentTimeMillis() - start;
        log.info("[RBAC][PERF] getRolesWithPermission 완료 - permissionCode={}, roleCount={}, elapsed={}ms",
                permissionCode, roleNames.size(), elapsed);
        return roleNames;
    }

    /** 역할과 권한 매핑을 함께 복사한다. */
    @Override
    @Transactional
    public RoleResponse cloneRole(String sourceRoleName, CloneRoleRequest request) {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        log.info("[RBAC] 역할 복사 - tenantId={}, source={}, target={}",
                tenantId, sourceRoleName, request.newRoleName());

        RoleJpaEntity sourceRole = roleRepository.findByTenantIdAndName(tenantId, sourceRoleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        if (roleRepository.existsByTenantIdAndName(tenantId, request.newRoleName())) {
            throw new RbacException(RbacException.RbacErrorCode.ROLE_ALREADY_EXISTS);
        }

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

        return new RoleResponse(
                newRole.getName(),
                newRole.getType(),
                newRole.getDescription(),
                newRole.getIsActive()
        );
    }

    /** ADMIN 역할 보유 여부를 검증한다. */
    private void checkAdminPermission(String userId, String action) {
        if (userId == null || userId.isEmpty()) {
            log.warn("[RBAC] 권한 검증 실패 - userId 없음, action={}", action);
            throw new RbacException(
                    RbacException.RbacErrorCode.INSUFFICIENT_PERMISSION,
                    "사용자 정보를 확인할 수 없습니다."
            );
        }

        List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByAgentId(userId);

        if (agentRoles.isEmpty()) {
            log.warn("[RBAC] 권한 부족 - userId={}, action={}, reason=역할 없음",
                    userId, action);
            throw new RbacException(
                    RbacException.RbacErrorCode.INSUFFICIENT_PERMISSION,
                    action + "은(는) ADMIN 역할이 필요합니다."
            );
        }

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

    /** 사용자 역할 정보를 조회한다. */
    @Override
    public Set<RbacModuleApi.RoleInfo> getRolesByAgentId(String agentId) {
        log.debug("[RBAC] 사용자 역할 조회 - agentId={}", agentId);

        List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByAgentId(agentId);

        Set<RbacModuleApi.RoleInfo> roleInfos = new HashSet<>();

        for (AgentRoleJpaEntity agentRole : agentRoles) {
            roleRepository.findById(agentRole.getRoleId()).ifPresent(role -> {
                RbacModuleApi.RoleInfo.RoleType roleType;
                try {
                    roleType = RbacModuleApi.RoleInfo.RoleType.valueOf(role.getType().name());
                } catch (IllegalArgumentException e) {
                    roleType = RbacModuleApi.RoleInfo.RoleType.POSITION;
                }

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
