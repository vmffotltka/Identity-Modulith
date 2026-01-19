package com.nexfron.identitymodulith.rbac.application;

import com.nexfron.identitymodulith.rbac.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.nexfron.identitymodulith.rbac.infrastructure.persistence.repository.AuditLogJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuditLogService 테스트
 *
 * <h2>테스트 범위:</h2>
 * - recordRoleCreation() - 역할 생성 기록
 * - recordRoleDeletion() - 역할 삭제 기록
 * - recordPermissionCreation() - 권한 생성 기록
 * - recordRolePermissionAssignment() - 역할-권한 할당 기록
 * - recordAgentRoleAssignment() - 사용자-역할 할당 기록
 * - getResourceHistory() - 리소스 변경 이력 조회
 * - getOperatorActions() - 사용자 작업 이력 조회
 * - getChangesByDateRange() - 기간별 감사 로그 조회
 *
 * @author Test Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("감사 로그 서비스 테스트")
class AuditLogServiceTest {

    @Mock
    private AuditLogJpaRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private final String tenantId = "test-tenant";
    private final String operatorId = "admin-user";

    @BeforeEach
    void setup() {
        // ObjectMapper는 AuditLogService 내부에서 사용되므로 별도 설정 불필요
    }

    // ============================================================
    // 역할 관리 기록 테스트
    // ============================================================

    @Test
    @DisplayName("역할 생성 기록 - 성공")
    void testRecordRoleCreation_Success() {
        // Given
        String roleName = "ADMIN";
        String roleType = "POSITION";

        when(auditLogRepository.save(any(AuditLogJpaEntity.class)))
                .thenReturn(new AuditLogJpaEntity());

        // When
        auditLogService.recordRoleCreation(tenantId, roleName, roleType, operatorId);

        // Then
        ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLogJpaEntity saved = captor.getValue();
        assertEquals(tenantId, saved.getTenantId());
        assertEquals("CREATE", saved.getAction());
        assertEquals("ROLE", saved.getResourceType());
        assertEquals(operatorId, saved.getOperatorId());
        assertNotNull(saved.getChanges());
        assertTrue(saved.getChanges().contains(roleName));
        assertTrue(saved.getChanges().contains(roleType));
    }

    @Test
    @DisplayName("역할 삭제 기록 - 성공")
    void testRecordRoleDeletion_Success() {
        // Given
        String roleName = "DEPRECATED_ROLE";
        String roleId = "role-001";

        when(auditLogRepository.save(any(AuditLogJpaEntity.class)))
                .thenReturn(new AuditLogJpaEntity());

        // When
        auditLogService.recordRoleDeletion(tenantId, roleName, roleId, operatorId);

        // Then
        ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLogJpaEntity saved = captor.getValue();
        assertEquals("DELETE", saved.getAction());
        assertEquals("ROLE", saved.getResourceType());
        assertEquals(roleId, saved.getResourceId());
    }

    // ============================================================
    // 권한 관리 기록 테스트
    // ============================================================

    @Test
    @DisplayName("권한 생성 기록 - 성공")
    void testRecordPermissionCreation_Success() {
        // Given
        String permissionCode = "user:manage";

        when(auditLogRepository.save(any(AuditLogJpaEntity.class)))
                .thenReturn(new AuditLogJpaEntity());

        // When
        auditLogService.recordPermissionCreation(tenantId, permissionCode, operatorId);

        // Then
        ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLogJpaEntity saved = captor.getValue();
        assertEquals("CREATE", saved.getAction());
        assertEquals("PERMISSION", saved.getResourceType());
        assertTrue(saved.getChanges().contains(permissionCode));
    }

    // ============================================================
    // 관계 관리 기록 테스트
    // ============================================================

    @Test
    @DisplayName("역할-권한 할당 기록 - 성공")
    void testRecordRolePermissionAssignment_Success() {
        // Given
        String roleName = "ADMIN";
        String roleId = "role-001";
        String permissionCode = "user:delete";
        String permissionId = "perm-001";

        when(auditLogRepository.save(any(AuditLogJpaEntity.class)))
                .thenReturn(new AuditLogJpaEntity());

        // When
        auditLogService.recordRolePermissionAssignment(tenantId, roleName, roleId,
                permissionCode, permissionId, operatorId);

        // Then
        ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLogJpaEntity saved = captor.getValue();
        assertEquals("ASSIGN", saved.getAction());
        assertEquals("ROLE_PERMISSION", saved.getResourceType());
        assertTrue(saved.getChanges().contains(roleName));
        assertTrue(saved.getChanges().contains(permissionCode));
    }

    @Test
    @DisplayName("사용자-역할 할당 기록 - 성공")
    void testRecordAgentRoleAssignment_Success() {
        // Given
        String agentId = "user-123";
        String roleName = "TEAM_LEADER";

        when(auditLogRepository.save(any(AuditLogJpaEntity.class)))
                .thenReturn(new AuditLogJpaEntity());

        // When
        auditLogService.recordAgentRoleAssignment(tenantId, agentId, roleName, operatorId);

        // Then
        ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLogJpaEntity saved = captor.getValue();
        assertEquals("ASSIGN", saved.getAction());
        assertEquals("AGENT_ROLE", saved.getResourceType());
        assertTrue(saved.getChanges().contains(agentId));
        assertTrue(saved.getChanges().contains(roleName));
    }

    // ============================================================
    // 감사 로그 조회 테스트
    // ============================================================

    @Test
    @DisplayName("리소스 변경 이력 조회 - 성공")
    void testGetResourceHistory_Success() {
        // Given
        String resourceType = "ROLE";
        String resourceId = "role-001";

        AuditLogJpaEntity log1 = new AuditLogJpaEntity();
        log1.setAction("CREATE");

        AuditLogJpaEntity log2 = new AuditLogJpaEntity();
        log2.setAction("ASSIGN");

        when(auditLogRepository.findByTenantIdAndResourceTypeAndResourceIdOrderByTimestampDesc(
                tenantId, resourceType, resourceId))
                .thenReturn(List.of(log1, log2));

        // When
        List<AuditLogJpaEntity> history = auditLogService.getResourceHistory(tenantId, resourceType, resourceId);

        // Then
        assertNotNull(history);
        assertEquals(2, history.size());
        assertEquals("CREATE", history.get(0).getAction());
        assertEquals("ASSIGN", history.get(1).getAction());
    }

    @Test
    @DisplayName("사용자 작업 이력 조회 - 성공")
    void testGetOperatorActions_Success() {
        // Given
        AuditLogJpaEntity log1 = new AuditLogJpaEntity();
        log1.setAction("CREATE");
        log1.setResourceType("ROLE");

        AuditLogJpaEntity log2 = new AuditLogJpaEntity();
        log2.setAction("ASSIGN");
        log2.setResourceType("PERMISSION");

        when(auditLogRepository.findByTenantIdAndOperatorIdOrderByTimestampDesc(tenantId, operatorId))
                .thenReturn(List.of(log1, log2));

        // When
        List<AuditLogJpaEntity> actions = auditLogService.getOperatorActions(tenantId, operatorId);

        // Then
        assertNotNull(actions);
        assertEquals(2, actions.size());
    }

    @Test
    @DisplayName("기간별 감사 로그 조회 - 성공")
    void testGetChangesByDateRange_Success() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();

        AuditLogJpaEntity log1 = new AuditLogJpaEntity();
        log1.setTimestamp(startTime.plusHours(1));

        AuditLogJpaEntity log2 = new AuditLogJpaEntity();
        log2.setTimestamp(startTime.plusHours(2));

        when(auditLogRepository.findByTenantIdAndTimestampBetween(tenantId, startTime, endTime))
                .thenReturn(List.of(log1, log2));

        // When
        List<AuditLogJpaEntity> logs = auditLogService.getChangesByDateRange(tenantId, startTime, endTime);

        // Then
        assertNotNull(logs);
        assertEquals(2, logs.size());
    }

    // ============================================================
    // 에러 처리 테스트
    // ============================================================

    @Test
    @DisplayName("감사 로그 저장 실패 - 예외 처리")
    void testRecordAuditLog_SaveFailure() {
        // Given
        when(auditLogRepository.save(any(AuditLogJpaEntity.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        // 예외가 발생해도 로깅만 하고 프로세스는 계속 진행되어야 함
        assertDoesNotThrow(() -> {
            auditLogService.recordRoleCreation(tenantId, "TEST_ROLE", "POSITION", operatorId);
        });
    }

    // ============================================================
    // 데이터 무결성 테스트
    // ============================================================

    @Test
    @DisplayName("감사 로그의 모든 필드 저장 - 성공")
    void testAuditLogDataIntegrity() {
        // Given
        when(auditLogRepository.save(any(AuditLogJpaEntity.class)))
                .thenReturn(new AuditLogJpaEntity());

        // When
        auditLogService.recordRoleCreation(tenantId, "ADMIN", "POSITION", operatorId);

        // Then
        ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLogJpaEntity saved = captor.getValue();

        // 모든 필수 필드 검증
        assertNotNull(saved.getAuditId());
        assertNotNull(saved.getTenantId());
        assertNotNull(saved.getAction());
        assertNotNull(saved.getResourceType());
        assertNotNull(saved.getResourceId());
        assertNotNull(saved.getOperatorId());
        assertNotNull(saved.getChanges());
        assertNotNull(saved.getTimestamp());
    }
}

