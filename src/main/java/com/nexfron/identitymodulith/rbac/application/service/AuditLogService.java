package com.nexfron.identitymodulith.rbac.application.service;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.AuditLogJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.*;

/**
 * RBAC 감사 로그 서비스
 *
 * <h2>책임:</h2>
 * 1. 권한 관리 작업의 감시(Audit) 로그 기록
 * 2. 변경 이력 조회 및 분석
 * 3. 규정 준수(Compliance) 데이터 관리
 * 4. 보안 모니터링 데이터 제공
 *
 * <h2>기록 자동화:</h2>
 * - RbacManagementServiceImpl의 주요 메서드에 AOP/Aspect로 자동 연결
 * - 또는 Service 메서드 마지막에 명시적으로 호출
 *
 * <h2>사용 예시:</h2>
 * {@code
 * // 역할 생성 후 감시 로그 기록
 * @Transactional
 * public void createRole(CreateRoleRequest request) {
 *     RoleJpaEntity role = createRoleLogic(request);
 *     auditLogService.recordRoleCreation(tenantId, request.name(), currentUserId);
 * }
 *
 * // 역할-권한 할당 후 감시 로그 기록
 * @Transactional
 * public void assignPermissionToRole(String roleName, String permissionCode) {
 *     assignPermissionLogic(roleName, permissionCode);
 *     auditLogService.recordRolePermissionAssignment(
 *         tenantId, roleId, permissionId, currentUserId
 *     );
 * }
 * }
 *
 * <h2>쿼리 예시:</h2>
 * {@code
 * // 특정 역할의 변경 이력
 * List<AuditLog> changes = auditLogService.getResourceHistory(
 *     tenantId, "ROLE", roleId
 * );
 *
 * // 특정 사용자의 작업 이력
 * List<AuditLog> userActions = auditLogService.getOperatorActions(
 *     tenantId, operatorId
 * );
 *
 * // 특정 기간의 모든 권한 변경
 * List<AuditLog> changes = auditLogService.getChangesByDateRange(
 *     tenantId, startDate, endDate
 * );
 * }
 *
 * @see AuditLogJpaEntity
 * @see AuditLogJpaRepository
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuditLogService {

    private final AuditLogJpaRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 역할 생성을 기록합니다.
     *
     * @param tenantId 테넌트 ID
     * @param roleName 생성된 역할명
     * @param roleType 역할 타입 (POSITION, CHANNEL, SKILL)
     * @param operatorId 작업 수행자 ID
     */
    @Transactional
    public void recordRoleCreation(String tenantId, String roleName, String roleType, String operatorId) {
        Map<String, String> changes = Map.of(
                "name", roleName,
                "type", roleType
        );
        recordAuditLog(tenantId, "CREATE", "ROLE", roleName, operatorId, changes);
        log.info("[RBAC 감사] 역할 생성: tenantId={}, roleName={}, operatorId={}",
                tenantId, roleName, operatorId);
    }

    /**
     * 역할 업데이트를 기록합니다.
     *
     * @param tenantId 테넌트 ID
     * @param roleId 업데이트된 역할 ID
     * @param roleName 업데이트된 역할명
     * @param operatorId 작업 수행자 ID
     * @param changeDetails 변경 상세 정보
     */
    @Transactional
    public void recordRoleUpdate(String tenantId, String roleId, String roleName, String operatorId, String changeDetails) {
        Map<String, String> changes = Map.of(
                "name", roleName,
                "action", "updated",
                "changes", changeDetails
        );
        recordAuditLog(tenantId, "UPDATE", "ROLE", roleId, operatorId, changes);
        log.info("[RBAC 감사] 역할 업데이트: tenantId={}, roleName={}, roleId={}, operatorId={}, changes={}",
                tenantId, roleName, roleId, operatorId, changeDetails);
    }

    /**
     * 역할 삭제를 기록합니다.
     *
     * @param tenantId 테넌트 ID
     * @param roleName 삭제된 역할명
     * @param roleId 삭제된 역할 ID
     * @param operatorId 작업 수행자 ID
     */
    @Transactional
    public void recordRoleDeletion(String tenantId, String roleName, String roleId, String operatorId) {
        Map<String, String> changes = Map.of("name", roleName);
        recordAuditLog(tenantId, "DELETE", "ROLE", roleId, operatorId, changes);
        log.info("[RBAC 감사] 역할 삭제: tenantId={}, roleName={}, roleId={}, operatorId={}",
                tenantId, roleName, roleId, operatorId);
    }

    /**
     * 역할 비활성화를 기록합니다.
     *
     * @param tenantId 테넌트 ID
     * @param roleName 비활성화된 역할명
     * @param roleId 비활성화된 역할 ID
     * @param operatorId 작업 수행자 ID
     */
    @Transactional
    public void recordRoleDeactivation(String tenantId, String roleName, String roleId, String operatorId) {
        Map<String, String> changes = Map.of(
                "name", roleName,
                "action", "deactivated",
                "is_active", "false"
        );
        recordAuditLog(tenantId, "DEACTIVATE", "ROLE", roleId, operatorId, changes);
        log.info("[RBAC 감사] 역할 비활성화: tenantId={}, roleName={}, roleId={}, operatorId={}",
                tenantId, roleName, roleId, operatorId);
    }

    /**
     * 역할 활성화를 기록합니다.
     *
     * @param tenantId 테넌트 ID
     * @param roleName 활성화된 역할명
     * @param roleId 활성화된 역할 ID
     * @param operatorId 작업 수행자 ID
     */
    @Transactional
    public void recordRoleActivation(String tenantId, String roleName, String roleId, String operatorId) {
        Map<String, String> changes = Map.of(
                "name", roleName,
                "action", "activated",
                "is_active", "true"
        );
        recordAuditLog(tenantId, "ACTIVATE", "ROLE", roleId, operatorId, changes);
        log.info("[RBAC 감사] 역할 활성화: tenantId={}, roleName={}, roleId={}, operatorId={}",
                tenantId, roleName, roleId, operatorId);
    }

    /**
     * 권한 생성을 기록합니다.
     *
     * @param tenantId 테넌트 ID
     * @param permissionCode 생성된 권한 코드
     * @param operatorId 작업 수행자 ID
     */
    @Transactional
    public void recordPermissionCreation(String tenantId, String permissionCode, String operatorId) {
        Map<String, String> changes = Map.of("code", permissionCode);
        recordAuditLog(tenantId, "CREATE", "PERMISSION", permissionCode, operatorId, changes);
        log.info("[RBAC 감사] 권한 생성: tenantId={}, code={}, operatorId={}",
                tenantId, permissionCode, operatorId);
    }

    /**
     * 권한 업데이트를 기록합니다.
     *
     * @param tenantId 테넌트 ID
     * @param permissionId 업데이트된 권한 ID
     * @param permissionCode 업데이트된 권한 코드
     * @param operatorId 작업 수행자 ID
     * @param changeDetails 변경 상세 정보
     */
    @Transactional
    public void recordPermissionUpdate(String tenantId, String permissionId, String permissionCode, String operatorId, String changeDetails) {
        Map<String, String> changes = Map.of(
                "code", permissionCode,
                "action", "updated",
                "changes", changeDetails
        );
        recordAuditLog(tenantId, "UPDATE", "PERMISSION", permissionId, operatorId, changes);
        log.info("[RBAC 감사] 권한 업데이트: tenantId={}, code={}, permissionId={}, operatorId={}, changes={}",
                tenantId, permissionCode, permissionId, operatorId, changeDetails);
    }

    /**
     * 권한 삭제를 기록합니다.
     *
     * @param tenantId 테넌트 ID
     * @param permissionCode 삭제된 권한 코드
     * @param permissionId 삭제된 권한 ID
     * @param operatorId 작업 수행자 ID
     */
    @Transactional
    public void recordPermissionDeletion(String tenantId, String permissionCode, String permissionId, String operatorId) {
        Map<String, String> changes = Map.of("code", permissionCode);
        recordAuditLog(tenantId, "DELETE", "PERMISSION", permissionId, operatorId, changes);
        log.info("[RBAC 감사] 권한 삭제: tenantId={}, code={}, permissionId={}, operatorId={}",
                tenantId, permissionCode, permissionId, operatorId);
    }

    /**
     * 역할-권한 할당을 기록합니다.
     *
     * @param tenantId 테넌트 ID
     * @param roleName 대상 역할명
     * @param roleId 대상 역할 ID
     * @param permissionCode 할당된 권한 코드
     * @param permissionId 할당된 권한 ID
     * @param operatorId 작업 수행자 ID
     */
    @Transactional
    public void recordRolePermissionAssignment(String tenantId, String roleName, String roleId,
                                                String permissionCode, String permissionId, String operatorId) {
        Map<String, String> changes = Map.of(
                "role", roleName,
                "permission", permissionCode
        );
        recordAuditLog(tenantId, "ASSIGN", "ROLE_PERMISSION", roleId, operatorId, changes);
        log.info("[RBAC 감사] 역할-권한 할당: tenantId={}, role={}, permission={}, operatorId={}",
                tenantId, roleName, permissionCode, operatorId);
    }

    /**
     * 역할-권한 회수를 기록합니다.
     *
     * @param tenantId 테넌트 ID
     * @param roleName 대상 역할명
     * @param roleId 대상 역할 ID
     * @param permissionCode 회수된 권한 코드
     * @param operatorId 작업 수행자 ID
     */
    @Transactional
    public void recordRolePermissionRevocation(String tenantId, String roleName, String roleId,
                                               String permissionCode, String operatorId) {
        Map<String, String> changes = Map.of(
                "role", roleName,
                "permission", permissionCode
        );
        recordAuditLog(tenantId, "REVOKE", "ROLE_PERMISSION", roleId, operatorId, changes);
        log.info("[RBAC 감사] 역할-권한 회수: tenantId={}, role={}, permission={}, operatorId={}",
                tenantId, roleName, permissionCode, operatorId);
    }

    /**
     * 사용자-역할 할당을 기록합니다.
     *
     * @param tenantId 테넌트 ID
     * @param agentId 대상 사용자 ID
     * @param roleName 할당된 역할명
     * @param operatorId 작업 수행자 ID
     */
    @Transactional
    public void recordAgentRoleAssignment(String tenantId, String agentId, String roleName, String operatorId) {
        Map<String, String> changes = Map.of(
                "agent", agentId,
                "role", roleName
        );
        recordAuditLog(tenantId, "ASSIGN", "AGENT_ROLE", agentId, operatorId, changes);
        log.info("[RBAC 감사] 사용자-역할 할당: tenantId={}, agentId={}, role={}, operatorId={}",
                tenantId, agentId, roleName, operatorId);
    }

    /**
     * 사용자-역할 회수를 기록합니다.
     *
     * @param tenantId 테넌트 ID
     * @param agentId 대상 사용자 ID
     * @param roleName 회수된 역할명
     * @param operatorId 작업 수행자 ID
     */
    @Transactional
    public void recordAgentRoleRevocation(String tenantId, String agentId, String roleName, String operatorId) {
        Map<String, String> changes = Map.of(
                "agent", agentId,
                "role", roleName
        );
        recordAuditLog(tenantId, "REVOKE", "AGENT_ROLE", agentId, operatorId, changes);
        log.info("[RBAC 감사] 사용자-역할 회수: tenantId={}, agentId={}, role={}, operatorId={}",
                tenantId, agentId, roleName, operatorId);
    }

    /**
     * 특정 리소스의 변경 이력을 조회합니다.
     *
     * @param tenantId 테넌트 ID
     * @param resourceType 리소스 타입 (ROLE, PERMISSION, ROLE_PERMISSION, AGENT_ROLE)
     * @param resourceId 리소스 ID
     * @return 변경 이력 (최신순)
     */
    public List<AuditLogJpaEntity> getResourceHistory(String tenantId, String resourceType, String resourceId) {
        return auditLogRepository.findByTenantIdAndResourceTypeAndResourceIdOrderByTimestampDesc(
                tenantId, resourceType, resourceId
        );
    }

    /**
     * 특정 사용자의 작업 이력을 조회합니다.
     *
     * @param tenantId 테넌트 ID
     * @param operatorId 작업 수행자 ID
     * @return 작업 이력 (최신순)
     */
    public List<AuditLogJpaEntity> getOperatorActions(String tenantId, String operatorId) {
        return auditLogRepository.findByTenantIdAndOperatorIdOrderByTimestampDesc(tenantId, operatorId);
    }

    /**
     * 특정 기간의 감사 로그를 조회합니다.
     *
     * @param tenantId 테넌트 ID
     * @param startTime 시작 일시
     * @param endTime 종료 일시
     * @return 감사 로그 (시간순)
     */
    public List<AuditLogJpaEntity> getChangesByDateRange(String tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findByTenantIdAndTimestampBetween(tenantId, startTime, endTime);
    }


    /**
     * 내부 헬퍼: 감사 로그 기록
     *
     * @param tenantId 테넌트 ID
     * @param action 작업 유형
     * @param resourceType 리소스 타입
     * @param resourceId 리소스 ID
     * @param operatorId 작업 수행자 ID
     * @param changes 변경 내용
     */
    private void recordAuditLog(String tenantId, String action, String resourceType,
                               String resourceId, String operatorId, Map<String, String> changes) {
        try {
            String changesJson = objectMapper.writeValueAsString(changes);

            AuditLogJpaEntity auditLog = AuditLogJpaEntity.builder()
                    .auditId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .operatorId(operatorId)
                    .changes(changesJson)
                    .timestamp(LocalDateTime.now())
                    .ipAddress(getClientIpAddress())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("[RBAC 감사] 로그 저장 실패: action={}, resourceType={}, error={}",
                    action, resourceType, e.getMessage(), e);
        }
    }

    // ============================================================
    // 감사 로그 조회 메서드 (권한 변경 이력)
    // ============================================================

    /**
     * 특정 사용자의 권한 변경 이력 조회
     *
     * @param tenantId 테넌트 ID
     * @param agentId 사용자 ID
     * @param from 시작 일시 (null이면 무제한)
     * @param to 종료 일시 (null이면 현재)
     * @return 사용자의 권한 변경 이력 목록 (최신순)
     */
    public List<AuditLogJpaEntity> getAgentPermissionChangeHistory(
            String tenantId,
            String agentId,
            LocalDateTime from,
            LocalDateTime to) {

        if (to == null) {
            to = LocalDateTime.now();
        }

        if (from == null) {
            return auditLogRepository.findByTenantIdAndResourceIdAndResourceTypeInAndTimestampBeforeOrderByTimestampDesc(
                    tenantId,
                    agentId,
                    List.of("AGENT_ROLE", "ROLE_PERMISSION"),
                    to
            );
        } else {
            return auditLogRepository.findByTenantIdAndResourceIdAndResourceTypeInAndTimestampBetweenOrderByTimestampDesc(
                    tenantId,
                    agentId,
                    List.of("AGENT_ROLE", "ROLE_PERMISSION"),
                    from,
                    to
            );
        }
    }

    /**
     * 특정 역할의 권한 변경 이력 조회
     *
     * @param tenantId 테넌트 ID
     * @param roleName 역할명
     * @param from 시작 일시 (null이면 무제한)
     * @param to 종료 일시 (null이면 현재)
     * @return 역할의 권한 변경 이력 목록 (최신순)
     */
    public List<AuditLogJpaEntity> getRolePermissionChangeHistory(
            String tenantId,
            String roleName,
            LocalDateTime from,
            LocalDateTime to) {

        if (to == null) {
            to = LocalDateTime.now();
        }

        if (from == null) {
            return auditLogRepository.findByTenantIdAndResourceIdAndResourceTypeInAndTimestampBeforeOrderByTimestampDesc(
                    tenantId,
                    roleName,
                    List.of("ROLE", "ROLE_PERMISSION", "ROLE_PERMISSION_GROUP"),
                    to
            );
        } else {
            return auditLogRepository.findByTenantIdAndResourceIdAndResourceTypeInAndTimestampBetweenOrderByTimestampDesc(
                    tenantId,
                    roleName,
                    List.of("ROLE", "ROLE_PERMISSION", "ROLE_PERMISSION_GROUP"),
                    from,
                    to
            );
        }
    }

    /**
     * 전체 권한 변경 이력 조회 (관리자용)
     *
     * @param tenantId 테넌트 ID
     * @param from 시작 일시 (null이면 무제한)
     * @param to 종료 일시 (null이면 현재)
     * @param pageSize 페이지 크기 (기본 100)
     * @return 권한 변경 이력 목록 (최신순)
     */
    public List<AuditLogJpaEntity> getAllPermissionChangeHistory(
            String tenantId,
            LocalDateTime from,
            LocalDateTime to,
            Integer pageSize) {

        if (pageSize == null || pageSize <= 0) {
            pageSize = 100;
        }

        if (to == null) {
            to = LocalDateTime.now();
        }

        if (from == null) {
            return auditLogRepository.findTop100ByTenantIdAndResourceTypeInAndTimestampBeforeOrderByTimestampDesc(
                    tenantId,
                    List.of("ROLE", "PERMISSION", "ROLE_PERMISSION", "AGENT_ROLE", "ROLE_PERMISSION_GROUP"),
                    to
            );
        } else {
            return auditLogRepository.findByTenantIdAndResourceTypeInAndTimestampBetweenOrderByTimestampDesc(
                    tenantId,
                    List.of("ROLE", "PERMISSION", "ROLE_PERMISSION", "AGENT_ROLE", "ROLE_PERMISSION_GROUP"),
                    from,
                    to
            );
        }
    }

    /**
     * 특정 작업자의 권한 관련 작업 이력 조회
     *
     * @param tenantId 테넌트 ID
     * @param operatorId 작업자 ID
     * @param from 시작 일시 (null이면 무제한)
     * @param to 종료 일시 (null이면 현재)
     * @return 작업자의 권한 관련 작업 이력 (최신순)
     */
    public List<AuditLogJpaEntity> getOperatorPermissionActions(
            String tenantId,
            String operatorId,
            LocalDateTime from,
            LocalDateTime to) {

        if (to == null) {
            to = LocalDateTime.now();
        }

        if (from == null) {
            return auditLogRepository.findByTenantIdAndOperatorIdAndResourceTypeInAndTimestampBeforeOrderByTimestampDesc(
                    tenantId,
                    operatorId,
                    List.of("ROLE", "PERMISSION", "ROLE_PERMISSION", "AGENT_ROLE", "ROLE_PERMISSION_GROUP"),
                    to
            );
        } else {
            return auditLogRepository.findByTenantIdAndOperatorIdAndResourceTypeInAndTimestampBetweenOrderByTimestampDesc(
                    tenantId,
                    operatorId,
                    List.of("ROLE", "PERMISSION", "ROLE_PERMISSION", "AGENT_ROLE", "ROLE_PERMISSION_GROUP"),
                    from,
                    to
            );
        }
    }

    /**
     * 클라이언트 IP 주소 조회
     *
     * @return 클라이언트 IP 주소 (없으면 null)
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getRemoteAddr();
            }
        } catch (Exception e) {
            // 서블릿 환경이 아닐 수 있음 (배치, 스케줄러 등)
        }
        return null;
    }
}

