package com.nexfron.identitymodulith.rbac.application;

import com.nexfron.identitymodulith.rbac.application.exception.RbacException;
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
    private final AuditLogService auditLogService;

    /**
     * 현재 요청의 tenantId 추출
     *
     * @return tenantId (Authentication 객체의 principal에서 추출)
     */
    private String getTenantId() {
        // SecurityContext에서 tenantId 추출 (구현은 프로젝트의 Authentication 구조에 따라)
        // 임시로 "default-tenant" 반환 (실제 구현 필요)
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            return principal.toString();
        }
        // 실제 구현: principal이 Custom AuthPrincipal 객체라면 getTenantId() 호출
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
                .map(role -> new RoleDto(role.getName(), role.getType()))
                .collect(Collectors.toList());
    }

    @Override
    public RoleDto getRoleByName(String roleName) {
        String tenantId = getTenantId();
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        return new RoleDto(role.getName(), role.getType());
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
                .createdAt(LocalDateTime.now())
                .build();

        // 3. 저장
        RoleJpaEntity savedRole = roleRepository.save(role);

        // 4. 감사 로그 기록
        String operatorId = getCurrentUserId();
        auditLogService.recordRoleCreation(tenantId, savedRole.getName(), savedRole.getType(), operatorId);

        return new RoleDto(savedRole.getName(), savedRole.getType());
    }

    @Override
    public List<PermissionDto> getAllPermissions() {
        String tenantId = getTenantId();
        return permissionRepository.findByTenantId(tenantId).stream()
                .map(perm -> new PermissionDto(perm.getCode()))
                .collect(Collectors.toList());
    }

    @Override
    public PermissionDto getPermissionByCode(String code) {
        String tenantId = getTenantId();
        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        return new PermissionDto(permission.getCode());
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

        return new PermissionDto(savedPermission.getCode());
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

        // 5. 감사 로그 기록
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

        // 4. 감사 로그 기록
        String operatorId = getCurrentUserId();
        auditLogService.recordRolePermissionRevocation(tenantId, roleName, role.getRoleId(),
                permissionCode, operatorId);

        log.info("[RBAC] 역할-권한 회수: roleName={}, permissionCode={}, 모든 사용자 캐시 무효화", roleName, permissionCode);
    }

    @Override
    public Set<PermissionDto> getPermissionsByRole(String roleName) {
        String tenantId = getTenantId();

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        return rolePermissionRepository.findPermissionsByRoleId(role.getRoleId()).stream()
                .map(perm -> new PermissionDto(perm.getCode()))
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void deleteRole(String roleName) {
        String tenantId = getTenantId();

        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND));

        // 역할에 할당된 권한 삭제 (카스케이드)
        rolePermissionRepository.deleteByRoleId(role.getRoleId());

        // 역할 삭제
        roleRepository.delete(role);
    }

    @Override
    @Transactional
    public void deletePermission(String code) {
        String tenantId = getTenantId();

        PermissionJpaEntity permission = permissionRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new RbacException(RbacException.RbacErrorCode.PERMISSION_NOT_FOUND));

        // 권한에 매핑된 모든 역할 관계 삭제 (카스케이드)
        rolePermissionRepository.deleteByPermissionId(permission.getPermissionId());

        // 권한 삭제
        permissionRepository.delete(permission);
    }

    // ========== 사용자-역할 관리 메서드 ==========

    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", key = "#agentId")
    public void assignRoleToAgent(String agentId, String roleName) {
        String tenantId = getTenantId();
        long startTime = System.currentTimeMillis();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> {
                    log.warn("[RBAC] 역할 할당 실패: roleName={}, 존재하지 않음", roleName);
                    return new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
                });

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

        // 5. 감사 로그 기록
        String operatorId = getCurrentUserId();
        auditLogService.recordAgentRoleAssignment(tenantId, agentId, roleName, operatorId);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[RBAC 역할 할당] agentId={}, roleName={}, roleId={}, 소요시간={}ms",
                agentId, roleName, role.getRoleId(), duration);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", key = "#agentId")
    public void revokeRoleFromAgent(String agentId, String roleName) {
        String tenantId = getTenantId();
        long startTime = System.currentTimeMillis();

        // 1. 역할 조회
        RoleJpaEntity role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                .orElseThrow(() -> {
                    log.warn("[RBAC] 역할 회수 실패: roleName={}, 존재하지 않음", roleName);
                    return new RbacException(RbacException.RbacErrorCode.ROLE_NOT_FOUND);
                });

        // 2. 매핑 삭제
        // AgentRoleJpaRepository에는 deleteByAgentIdAndRoleId 메서드가 있을 것으로 가정
        // 없다면 직접 쿼리를 작성해야 함
        agentRoleRepository.findByAgentId(agentId).stream()
                .filter(ar -> ar.getRoleId().equals(role.getRoleId()))
                .forEach(agentRoleRepository::delete);

        // 3. 감사 로그 기록
        String operatorId = getCurrentUserId();
        auditLogService.recordAgentRoleRevocation(tenantId, agentId, roleName, operatorId);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[RBAC 역할 회수] agentId={}, roleName={}, roleId={}, 소요시간={}ms",
                agentId, roleName, role.getRoleId(), duration);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<RoleDto> getRolesByAgent(String agentId) {
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
                .map(role -> new RoleDto(role.getName(), role.getType()))
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
}

