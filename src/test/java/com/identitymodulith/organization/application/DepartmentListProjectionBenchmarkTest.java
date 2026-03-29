package com.identitymodulith.organization.application;

import com.identitymodulith.organization.domain.model.DepartmentType;
import com.identitymodulith.organization.infrastructure.persistence.entity.DepartmentEntity;
import com.identitymodulith.organization.infrastructure.persistence.repository.DepartmentListProjection;
import com.identitymodulith.organization.infrastructure.persistence.repository.JpaDepartmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Organization 목록성 조회에서 Fetch Join 엔티티 로딩 vs DTO Projection을 비교한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DepartmentListProjectionBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(DepartmentListProjectionBenchmarkTest.class);

    private static final String TENANT_ID = "benchmark-tenant-projection";
    private static final int ROOT_COUNT = 40;
    private static final int CHILD_PER_ROOT = 20;
    private static final int BENCHMARK_DEPTH = 1;
    private static final String BENCHMARK_KEYWORD = "CHILD_1_";
    private static final int WARMUP_ROUNDS = 3;
    private static final int MEASURE_ROUNDS = 10;

    @Autowired
    private JpaDepartmentRepository departmentRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;
    private TransactionTemplate readOnlyTx;

    private String subtreePrefix;
    private Set<String> scopeDeptIds;

    @BeforeEach
    void setUp() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);

        readOnlyTx = new TransactionTemplate(transactionManager);
        readOnlyTx.setReadOnly(true);

        cleanupBenchmarkData();
        seedBenchmarkData();
        prepareScenarioData();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        cleanupBenchmarkData();
    }

    @Test
    @Order(1)
    @DisplayName("Department 목록 조회 - Fetch Join 엔티티 vs DTO Projection")
    void benchmark_listQueries_fetchJoinVsProjection() {
        assertScenario("TREE", this::measureFetchTree, this::measureProjectionTree);
        assertScenario("SEARCH", this::measureFetchSearch, this::measureProjectionSearch);
        assertScenario("SUBTREE", this::measureFetchSubtree, this::measureProjectionSubtree);
        assertScenario("DEPTH", this::measureFetchDepth, this::measureProjectionDepth);
        assertScenario("TYPE", this::measureFetchType, this::measureProjectionType);
        assertScenario("SCOPE_IN", this::measureFetchScopeInMemory, this::measureProjectionScopeIn);
    }

    private void assertScenario(String label, Measure before, Measure after) {
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            before.run();
            after.run();
        }

        long[] beforeMs = new long[MEASURE_ROUNDS];
        long[] beforeQueries = new long[MEASURE_ROUNDS];
        long[] beforeRows = new long[MEASURE_ROUNDS];
        long[] afterMs = new long[MEASURE_ROUNDS];
        long[] afterQueries = new long[MEASURE_ROUNDS];
        long[] afterRows = new long[MEASURE_ROUNDS];

        for (int i = 0; i < MEASURE_ROUNDS; i++) {
            Result b = before.run();
            beforeMs[i] = b.elapsedMs;
            beforeQueries[i] = b.queryCount;
            beforeRows[i] = b.rowCount;

            Result a = after.run();
            afterMs[i] = a.elapsedMs;
            afterQueries[i] = a.queryCount;
            afterRows[i] = a.rowCount;
        }

        long beforeAvgMs = avg(beforeMs);
        long afterAvgMs = avg(afterMs);
        long beforeAvgQueries = avg(beforeQueries);
        long afterAvgQueries = avg(afterQueries);
        long beforeAvgRows = avg(beforeRows);
        long afterAvgRows = avg(afterRows);

        log.info("[BENCHMARK][DEPT][{}] BEFORE(FETCH_ENTITY) avg={}ms, avgQueries={}, avgRows={}, roundsMs={}, roundsQueries={}",
                label, beforeAvgMs, beforeAvgQueries, beforeAvgRows, Arrays.toString(beforeMs), Arrays.toString(beforeQueries));
        log.info("[BENCHMARK][DEPT][{}] AFTER(PROJECTION)    avg={}ms, avgQueries={}, avgRows={}, roundsMs={}, roundsQueries={}",
                label, afterAvgMs, afterAvgQueries, afterAvgRows, Arrays.toString(afterMs), Arrays.toString(afterQueries));

        assertThat(afterAvgRows).isEqualTo(beforeAvgRows);
        assertThat(afterAvgQueries).isLessThanOrEqualTo(beforeAvgQueries);
    }

    private Result measureFetchTree() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();
            long start = System.currentTimeMillis();
            List<DepartmentEntity> rows = departmentRepository.findAllByTenantIdWithParent(TENANT_ID);
            long elapsed = System.currentTimeMillis() - start;
            return new Result(elapsed, statistics.getPrepareStatementCount(), rows.size());
        });
    }

    private Result measureProjectionTree() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();
            long start = System.currentTimeMillis();
            List<DepartmentListProjection> rows = departmentRepository.findAllProjectedByTenantId(TENANT_ID);
            long elapsed = System.currentTimeMillis() - start;
            return new Result(elapsed, statistics.getPrepareStatementCount(), rows.size());
        });
    }

    private Result measureFetchSearch() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();
            long start = System.currentTimeMillis();
            List<DepartmentEntity> rows = departmentRepository
                    .findByTenantIdAndNameContainingIgnoreCaseWithParent(TENANT_ID, BENCHMARK_KEYWORD);
            long elapsed = System.currentTimeMillis() - start;
            return new Result(elapsed, statistics.getPrepareStatementCount(), rows.size());
        });
    }

    private Result measureProjectionSearch() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();
            long start = System.currentTimeMillis();
            List<DepartmentListProjection> rows = departmentRepository
                    .findProjectedByTenantIdAndNameContainingIgnoreCase(TENANT_ID, BENCHMARK_KEYWORD);
            long elapsed = System.currentTimeMillis() - start;
            return new Result(elapsed, statistics.getPrepareStatementCount(), rows.size());
        });
    }

    private Result measureFetchSubtree() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();
            long start = System.currentTimeMillis();
            List<DepartmentEntity> rows = departmentRepository
                    .findByTenantIdAndOrgPathStartsWithWithParent(TENANT_ID, subtreePrefix);
            long elapsed = System.currentTimeMillis() - start;
            return new Result(elapsed, statistics.getPrepareStatementCount(), rows.size());
        });
    }

    private Result measureProjectionSubtree() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();
            long start = System.currentTimeMillis();
            List<DepartmentListProjection> rows = departmentRepository
                    .findProjectedByTenantIdAndOrgPathStartsWith(TENANT_ID, subtreePrefix);
            long elapsed = System.currentTimeMillis() - start;
            return new Result(elapsed, statistics.getPrepareStatementCount(), rows.size());
        });
    }

    private Result measureFetchDepth() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();
            long start = System.currentTimeMillis();
            List<DepartmentEntity> rows = departmentRepository
                    .findByTenantIdAndDepthWithParent(TENANT_ID, BENCHMARK_DEPTH);
            long elapsed = System.currentTimeMillis() - start;
            return new Result(elapsed, statistics.getPrepareStatementCount(), rows.size());
        });
    }

    private Result measureProjectionDepth() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();
            long start = System.currentTimeMillis();
            List<DepartmentListProjection> rows = departmentRepository
                    .findProjectedByTenantIdAndDepth(TENANT_ID, BENCHMARK_DEPTH);
            long elapsed = System.currentTimeMillis() - start;
            return new Result(elapsed, statistics.getPrepareStatementCount(), rows.size());
        });
    }

    private Result measureFetchType() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();
            long start = System.currentTimeMillis();
            List<DepartmentEntity> rows = departmentRepository
                    .findByTenantIdAndTypeWithParent(TENANT_ID, DepartmentType.TEAM);
            long elapsed = System.currentTimeMillis() - start;
            return new Result(elapsed, statistics.getPrepareStatementCount(), rows.size());
        });
    }

    private Result measureProjectionType() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();
            long start = System.currentTimeMillis();
            List<DepartmentListProjection> rows = departmentRepository
                    .findProjectedByTenantIdAndType(TENANT_ID, DepartmentType.TEAM);
            long elapsed = System.currentTimeMillis() - start;
            return new Result(elapsed, statistics.getPrepareStatementCount(), rows.size());
        });
    }

    private Result measureFetchScopeInMemory() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();
            long start = System.currentTimeMillis();
            List<DepartmentEntity> allRows = departmentRepository.findAllByTenantIdWithParent(TENANT_ID);
            long count = allRows.stream().filter(d -> scopeDeptIds.contains(d.getDeptId())).count();
            long elapsed = System.currentTimeMillis() - start;
            return new Result(elapsed, statistics.getPrepareStatementCount(), count);
        });
    }

    private Result measureProjectionScopeIn() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();
            long start = System.currentTimeMillis();
            List<DepartmentListProjection> rows = departmentRepository
                    .findProjectedByTenantIdAndDeptIdIn(TENANT_ID, scopeDeptIds);
            long elapsed = System.currentTimeMillis() - start;
            return new Result(elapsed, statistics.getPrepareStatementCount(), rows.size());
        });
    }

    private void prepareScenarioData() {
        List<DepartmentEntity> roots = departmentRepository.findByTenantIdAndDepth(TENANT_ID, 0);
        DepartmentEntity root = roots.getFirst();
        subtreePrefix = root.getOrgPath();

        Set<String> scoped = new HashSet<>();
        for (int i = 0; i < Math.min(3, roots.size()); i++) {
            String prefix = roots.get(i).getOrgPath();
            List<DepartmentEntity> inSubtree = departmentRepository.findByTenantIdAndOrgPathStartsWith(TENANT_ID, prefix);
            for (DepartmentEntity dept : inSubtree) {
                scoped.add(dept.getDeptId());
            }
        }
        scopeDeptIds = scoped;
    }

    private void seedBenchmarkData() {
        List<DepartmentEntity> toSave = new ArrayList<>();

        for (int i = 1; i <= ROOT_COUNT; i++) {
            DepartmentEntity root = DepartmentEntity.create(
                    TENANT_ID,
                    "ROOT_" + i,
                    DepartmentType.DIVISION,
                    "ROOT-CODE-" + i,
                    null,
                    null
            );
            toSave.add(root);

            for (int j = 1; j <= CHILD_PER_ROOT; j++) {
                DepartmentEntity child = DepartmentEntity.create(
                        TENANT_ID,
                        "CHILD_" + i + "_" + j,
                        DepartmentType.TEAM,
                        "CHILD-CODE-" + i + "-" + j,
                        null,
                        root
                );
                toSave.add(child);
            }
        }

        departmentRepository.saveAll(toSave);
        departmentRepository.flush();
    }

    private void cleanupBenchmarkData() {
        jdbc.update("DELETE FROM org_departments WHERE tenant_id = ?", TENANT_ID);
    }

    private long avg(long[] values) {
        return (long) Arrays.stream(values).average().orElse(0);
    }

    private interface Measure {
        Result run();
    }

    private record Result(long elapsedMs, long queryCount, long rowCount) { }
}


