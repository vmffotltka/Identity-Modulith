package com.identitymodulith.rbac.application;

import com.identitymodulith.rbac.application.service.RbacManagementService;
import com.identitymodulith.rbac.application.service.RbacQueryService;
import com.identitymodulith.rbac.domain.RoleType;
import com.identitymodulith.rbac.infrastructure.persistence.entity.AgentRoleJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.entity.PermissionJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.entity.RoleJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.entity.RolePermissionJpaEntity;
import com.identitymodulith.rbac.infrastructure.persistence.repository.*;
import com.identitymodulith.user.infrastructure.persistence.entity.AgentJpaEntity;
import com.identitymodulith.user.infrastructure.persistence.repository.AgentJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RBAC N+1 최적화 성능 벤치마크 — Before vs After 실측
 *
 * <h2>측정 방식</h2>
 * <ul>
 *   <li><b>Before</b>: N+1 패턴을 직접 재현 (반복문 내 findByRoleId + findById)</li>
 *   <li><b>After</b>:  최적화된 단일 JOIN 쿼리 사용</li>
 * </ul>
 *
 * <h2>실행 방법</h2>
 * <pre>
 * ./gradlew test --tests "*.RbacPerformanceBenchmarkTest" --info
 * </pre>
 * 콘솔에서 [BENCHMARK] 키워드로 결과 확인
 *
 * <h2>주의</h2>
 * 이 테스트는 수동으로 실행할 때만 사용합니다. CI/CD에서는 @Disabled로 비활성화됩니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("RBAC N+1 최적화 성능 벤치마크")
@Disabled("수동 벤치마크 테스트 - CI/CD에서는 실행하지 않음")
class RbacPerformanceBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(RbacPerformanceBenchmarkTest.class);

    // ── Service & Repository ─────────────────────────────────────────────────
    @Autowired private RbacQueryService           rbacQueryService;
    @Autowired private RbacManagementService       rbacManagementService;
    @Autowired private RoleJpaRepository           roleRepository;
    @Autowired private PermissionJpaRepository     permissionRepository;
    @Autowired private RolePermissionJpaRepository rolePermissionRepository;
    @Autowired private AgentRoleJpaRepository      agentRoleRepository;
    @Autowired private AgentJpaRepository          agentJpaRepository;
    @Autowired private JdbcTemplate                jdbc; // Hibernate 캐시 우회용 직접 SQL

    @PersistenceContext
    private EntityManager entityManager; // JDBC 직접 삽입 후 JPA 1차 캐시 초기화용

    // ── 벤치마크 파라미터 ────────────────────────────────────────────────────
    private static final String TENANT_ID     = "benchmark-tenant";
    private static final String AGENT_ID      = "benchmark-agent-001";
    private static final int    ROLE_COUNT    = 5;
    private static final int    PERM_PER_ROLE = 4;  // 총 20개
    private static final int    WARMUP_ROUNDS = 3;
    private static final int    MEASURE_ROUNDS = 10;

    // ────────────────────────────────────────────────────────────────────────
    //  Setup / Teardown
    // ────────────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        cleanupViaJdbc(); // 이전 실행 잔여 데이터 제거

        // SecurityContext 설정 (TenantContextHolder가 principal에서 tenantId 추출)
        var auth = new UsernamePasswordAuthenticationToken(
                TENANT_ID + ":" + AGENT_ID,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        log.info("[BENCHMARK] ===== 벤치마크 데이터 생성 시작 =====");
        log.info("[BENCHMARK] 역할: {}개 / 역할당 권한: {}개 / 총 권한: {}개",
                ROLE_COUNT, PERM_PER_ROLE, ROLE_COUNT * PERM_PER_ROLE);

        // 0. user_agents FK 충족 — rbac_agent_roles.agent_id → user_agents.agent_id
        jdbc.update("""
            INSERT INTO user_agents (agent_id, tenant_id, login_id, name, password, status,
                                     password_must_change, version)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (agent_id) DO NOTHING
            """,
            AGENT_ID, TENANT_ID, "bench_agent_001", "벤치마크 상담사",
            "bench-pw-hash", "ACTIVE", false, 0
        );

        int totalPerms = ROLE_COUNT * PERM_PER_ROLE;

        // 1. 권한 생성
        for (int i = 1; i <= totalPerms; i++) {
            final int idx = i;
            jdbc.update("""
                INSERT INTO rbac_permissions (permission_id, tenant_id, code, name, description)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (permission_id) DO NOTHING
                """,
                "bench-perm-" + idx, TENANT_ID,
                "bench:action" + idx, "벤치마크 권한 " + idx, "벤치마크용 권한 " + idx
            );
        }

        // 2. 역할 생성 + 역할-권한 매핑 + 상담사-역할 매핑
        for (int r = 1; r <= ROLE_COUNT; r++) {
            String roleId = "bench-role-" + r;
            jdbc.update("""
                INSERT INTO rbac_roles (role_id, tenant_id, name, type, is_active, version)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (role_id) DO NOTHING
                """,
                roleId, TENANT_ID, "BENCH_ROLE_" + r, "POSITION", true, 0L
            );

            for (int p = (r - 1) * PERM_PER_ROLE + 1; p <= r * PERM_PER_ROLE; p++) {
                String permId = "bench-perm-" + p;
                jdbc.update("""
                    INSERT INTO rbac_role_permissions (role_id, permission_id)
                    VALUES (?, ?)
                    ON CONFLICT DO NOTHING
                    """,
                    roleId, permId
                );
            }

            jdbc.update("""
                INSERT INTO rbac_agent_roles (agent_id, role_id)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """,
                AGENT_ID, roleId
            );
        }

        log.info("[BENCHMARK] 데이터 생성 완료");

        // JDBC 직접 삽입 후 JPA 1차 캐시 초기화 — simulateNPlusOne()의 JPA Repository 조회가
        // 캐시가 아닌 DB에서 새 데이터를 읽도록 보장
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        cleanupViaJdbc();
        SecurityContextHolder.clearContext();
        log.info("[BENCHMARK] 테스트 데이터 정리 완료");
    }

    /** JdbcTemplate으로 직접 삭제 — Hibernate 1차 캐시/낙관적 잠금 완전 우회 */
    private void cleanupViaJdbc() {
        jdbc.update("DELETE FROM rbac_agent_roles WHERE agent_id = ?", AGENT_ID);
        for (int r = 1; r <= ROLE_COUNT; r++) {
            jdbc.update("DELETE FROM rbac_role_permissions WHERE role_id = ?", "bench-role-" + r);
        }
        for (int r = 1; r <= ROLE_COUNT; r++) {
            jdbc.update("DELETE FROM rbac_roles WHERE role_id = ?", "bench-role-" + r);
        }
        int totalPerms = ROLE_COUNT * PERM_PER_ROLE;
        for (int i = 1; i <= totalPerms; i++) {
            jdbc.update("DELETE FROM rbac_permissions WHERE permission_id = ?", "bench-perm-" + i);
        }
        jdbc.update("DELETE FROM user_agents WHERE agent_id = ?", AGENT_ID);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  벤치마크 1: getEffectivePermissions — Before vs After
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("[벤치마크 1] getEffectivePermissions — N+1 vs 3-JOIN 실측 비교")
    void benchmark_getEffectivePermissions_beforeVsAfter() {
        int totalPerms = ROLE_COUNT * PERM_PER_ROLE;

        log.info("[BENCHMARK] ===================================================");
        log.info("[BENCHMARK] 벤치마크 1: getEffectivePermissions");
        log.info("[BENCHMARK] 환경: 역할 {}개 / 권한 {}개 / 상담사 1명", ROLE_COUNT, totalPerms);
        log.info("[BENCHMARK] ===================================================");

        // ── BEFORE: N+1 패턴 직접 재현 ─────────────────────────────────────
        for (int i = 0; i < WARMUP_ROUNDS; i++) simulateNPlusOne();

        long[] beforeMs = new long[MEASURE_ROUNDS];
        StopWatch swBefore = new StopWatch("N+1");
        for (int i = 0; i < MEASURE_ROUNDS; i++) {
            swBefore.start("r" + i);
            Set<String> r = simulateNPlusOne();
            swBefore.stop();
            beforeMs[i] = swBefore.getLastTaskTimeMillis();
            assertThat(r).hasSize(totalPerms);
        }

        // ── AFTER: 최적화 3-JOIN 단일 쿼리 ────────────────────────────────
        for (int i = 0; i < WARMUP_ROUNDS; i++) rbacManagementService.getEffectivePermissions(AGENT_ID);

        long[] afterMs = new long[MEASURE_ROUNDS];
        StopWatch swAfter = new StopWatch("3-JOIN");
        for (int i = 0; i < MEASURE_ROUNDS; i++) {
            swAfter.start("r" + i);
            Set<String> r = rbacManagementService.getEffectivePermissions(AGENT_ID);
            swAfter.stop();
            afterMs[i] = swAfter.getLastTaskTimeMillis();
            assertThat(r).hasSize(totalPerms);
        }

        printComparison("getEffectivePermissions",
                "반복문 내 findByRoleId + findById  (N+1)",
                "단일 3-JOIN  (agent_roles → role_permissions → permissions)",
                String.format("1 + %d + %d = %d queries", ROLE_COUNT, totalPerms, 1 + ROLE_COUNT + totalPerms),
                "1 query (고정)",
                beforeMs, afterMs);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  벤치마크 2: permissionsOfRoles — Before vs After
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("[벤치마크 2] permissionsOfRoles — N+1 vs JOIN 2-query 실측 비교")
    void benchmark_permissionsOfRoles_beforeVsAfter() {
        int totalPerms = ROLE_COUNT * PERM_PER_ROLE;
        Set<String> roleNames = new HashSet<>();
        for (int r = 1; r <= ROLE_COUNT; r++) roleNames.add("BENCH_ROLE_" + r);

        log.info("[BENCHMARK] ===================================================");
        log.info("[BENCHMARK] 벤치마크 2: permissionsOfRoles");
        log.info("[BENCHMARK] 환경: 역할 {}개 / 권한 {}개", ROLE_COUNT, totalPerms);
        log.info("[BENCHMARK] ===================================================");

        // ── BEFORE: N+1 패턴 직접 재현 ─────────────────────────────────────
        for (int i = 0; i < WARMUP_ROUNDS; i++) simulateNPlusOneForRoles(roleNames);

        long[] beforeMs = new long[MEASURE_ROUNDS];
        StopWatch swBefore = new StopWatch("N+1");
        for (int i = 0; i < MEASURE_ROUNDS; i++) {
            swBefore.start("r" + i);
            Set<String> r = simulateNPlusOneForRoles(roleNames);
            swBefore.stop();
            beforeMs[i] = swBefore.getLastTaskTimeMillis();
            assertThat(r).hasSize(totalPerms);
        }

        // ── AFTER: 최적화 2-query ──────────────────────────────────────────
        for (int i = 0; i < WARMUP_ROUNDS; i++) rbacQueryService.permissionsOfRoles(TENANT_ID, roleNames);

        long[] afterMs = new long[MEASURE_ROUNDS];
        StopWatch swAfter = new StopWatch("2-query");
        for (int i = 0; i < MEASURE_ROUNDS; i++) {
            swAfter.start("r" + i);
            Set<String> r = rbacQueryService.permissionsOfRoles(TENANT_ID, roleNames);
            swAfter.stop();
            afterMs[i] = swAfter.getLastTaskTimeMillis();
            assertThat(r).hasSize(totalPerms);
        }

        printComparison("permissionsOfRoles",
                "반복문 내 findByRoleId + findById  (N+1)",
                "roles 조회 1회 + JOIN으로 권한 코드 일괄 조회 1회",
                String.format("1 + %d + %d = %d queries", ROLE_COUNT, totalPerms, 1 + ROLE_COUNT + totalPerms),
                "2 queries (고정)",
                beforeMs, afterMs);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  N+1 패턴 재현 (Before 측정용)
    // ────────────────────────────────────────────────────────────────────────

    private Set<String> simulateNPlusOne() {
        List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByAgentId(AGENT_ID); // Q1
        Set<String> codes = new HashSet<>();
        for (AgentRoleJpaEntity ar : agentRoles) {
            List<RolePermissionJpaEntity> rps = rolePermissionRepository.findByRoleId(ar.getRoleId()); // QN
            for (RolePermissionJpaEntity rp : rps) {
                permissionRepository.findById(rp.getPermissionId())             // QM
                        .ifPresent(p -> codes.add(p.getCode()));
            }
        }
        return codes;
    }

    private Set<String> simulateNPlusOneForRoles(Set<String> roleNames) {
        List<RoleJpaEntity> roles = roleRepository.findByTenantIdAndNameIn(TENANT_ID, roleNames); // Q1
        Set<String> codes = new HashSet<>();
        for (RoleJpaEntity role : roles) {
            List<RolePermissionJpaEntity> rps = rolePermissionRepository.findByRoleId(role.getRoleId()); // QN
            for (RolePermissionJpaEntity rp : rps) {
                permissionRepository.findById(rp.getPermissionId())             // QM
                        .ifPresent(p -> codes.add(p.getCode()));
            }
        }
        return codes;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  결과 출력
    // ────────────────────────────────────────────────────────────────────────

    private void printComparison(String methodName,
                                 String beforeStrategy, String afterStrategy,
                                 String beforeQCount, String afterQCount,
                                 long[] beforeMs, long[] afterMs) {

        long bAvg = (long) Arrays.stream(beforeMs).average().orElse(0);
        long bMin = Arrays.stream(beforeMs).min().orElse(0);
        long bMax = Arrays.stream(beforeMs).max().orElse(0);
        long aAvg = (long) Arrays.stream(afterMs).average().orElse(0);
        long aMin = Arrays.stream(afterMs).min().orElse(0);
        long aMax = Arrays.stream(afterMs).max().orElse(0);
        double pct = bAvg > 0 ? (double)(bAvg - aAvg) / bAvg * 100 : 0;

        log.info("[BENCHMARK] ╔══════════════════════════════════════════════════╗");
        log.info("[BENCHMARK] ║  {} 결과", methodName);
        log.info("[BENCHMARK] ╠══════════════════════════════════════════════════╣");
        log.info("[BENCHMARK] ║  [BEFORE] {}", beforeStrategy);
        log.info("[BENCHMARK] ║  쿼리 수  : {}", beforeQCount);
        log.info("[BENCHMARK] ║  평균/min/max : {} ms / {} ms / {} ms", bAvg, bMin, bMax);
        log.info("[BENCHMARK] ║  각 라운드 : {}", Arrays.toString(beforeMs));
        log.info("[BENCHMARK] ╠══════════════════════════════════════════════════╣");
        log.info("[BENCHMARK] ║  [AFTER]  {}", afterStrategy);
        log.info("[BENCHMARK] ║  쿼리 수  : {}", afterQCount);
        log.info("[BENCHMARK] ║  평균/min/max : {} ms / {} ms / {} ms", aAvg, aMin, aMax);
        log.info("[BENCHMARK] ║  각 라운드 : {}", Arrays.toString(afterMs));
        log.info("[BENCHMARK] ╠══════════════════════════════════════════════════╣");
        log.info("[BENCHMARK] ║  응답시간 개선 : {} ms  →  {} ms  ({})",
                bAvg, aAvg, String.format("%.1f%% 단축", pct));
        log.info("[BENCHMARK] ║  쿼리 수 변화  : {}  →  {}", beforeQCount, afterQCount);
        log.info("[BENCHMARK] ╚══════════════════════════════════════════════════╝");
    }
}

