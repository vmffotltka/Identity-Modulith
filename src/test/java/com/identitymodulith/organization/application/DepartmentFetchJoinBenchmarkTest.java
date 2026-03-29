package com.identitymodulith.organization.application;

import com.identitymodulith.organization.domain.model.DepartmentType;
import com.identitymodulith.organization.infrastructure.persistence.entity.DepartmentEntity;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Department parent 조회 경로의 Fetch Join 성능 비교 벤치마크.
 *
 * 비교 대상:
 * - Before: findByTenantIdAndDepth(depth=1) (parent LAZY)
 * - After : findByTenantIdAndDepthWithParent(depth=1) (LEFT JOIN FETCH)
 *
 * 주의:
 * - depth=1처럼 parent 중복 참조가 많은 데이터에서는 LAZY도 1 query로 수렴할 수 있다.
 * - 따라서 이 테스트는 "항상 더 빠름"이 아니라 "쿼리 회귀 방지"를 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DepartmentFetchJoinBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(DepartmentFetchJoinBenchmarkTest.class);

    private static final String TENANT_ID = "benchmark-tenant-fetchjoin";
    private static final int ROOT_COUNT = 20;
    private static final int CHILD_PER_ROOT = 10;
    private static final int BENCHMARK_DEPTH = 1;
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

    @BeforeEach
    void setUp() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);

        readOnlyTx = new TransactionTemplate(transactionManager);
        readOnlyTx.setReadOnly(true);

        cleanupBenchmarkData();
        seedBenchmarkData();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        cleanupBenchmarkData();
    }

    @Test
    @Order(1)
    @DisplayName("Department parent 조회 - LAZY vs FETCH JOIN")
    void benchmark_parentLoading_lazyVsFetchJoin() {
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            measureLazy();
            measureFetchJoin();
        }

        long[] beforeMs = new long[MEASURE_ROUNDS];
        long[] afterMs = new long[MEASURE_ROUNDS];
        long[] beforeQueries = new long[MEASURE_ROUNDS];
        long[] afterQueries = new long[MEASURE_ROUNDS];

        for (int i = 0; i < MEASURE_ROUNDS; i++) {
            Result before = measureLazy();
            beforeMs[i] = before.elapsedMs;
            beforeQueries[i] = before.queryCount;

            Result after = measureFetchJoin();
            afterMs[i] = after.elapsedMs;
            afterQueries[i] = after.queryCount;
        }

        long beforeAvgMs = avg(beforeMs);
        long afterAvgMs = avg(afterMs);
        long beforeAvgQuery = avg(beforeQueries);
        long afterAvgQuery = avg(afterQueries);

        log.info("[BENCHMARK][DEPT][DEPTH={}] BEFORE(LAZY)  avg={}ms, avgQueries={}, roundsMs={}, roundsQueries={}",
                BENCHMARK_DEPTH, beforeAvgMs, beforeAvgQuery, Arrays.toString(beforeMs), Arrays.toString(beforeQueries));
        log.info("[BENCHMARK][DEPT][DEPTH={}] AFTER(FETCH) avg={}ms, avgQueries={}, roundsMs={}, roundsQueries={}",
                BENCHMARK_DEPTH, afterAvgMs, afterAvgQuery, Arrays.toString(afterMs), Arrays.toString(afterQueries));

        // 소규모/중복 parent 데이터에서는 before=after가 가능하므로,
        // Fetch Join 경로가 쿼리 수에서 역행하지 않고(<=), 예산(<=2) 안에 머무는지 검증한다.
        assertThat(afterAvgQuery).isLessThanOrEqualTo(beforeAvgQuery);
        assertThat(afterAvgQuery).isLessThanOrEqualTo(2L);
    }

    private Result measureLazy() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();

            long start = System.currentTimeMillis();
            List<DepartmentEntity> rows = departmentRepository.findByTenantIdAndDepth(TENANT_ID, BENCHMARK_DEPTH);
            forceParentAccess(rows);
            long elapsed = System.currentTimeMillis() - start;

            return new Result(elapsed, statistics.getPrepareStatementCount());
        });
    }

    private Result measureFetchJoin() {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();

            long start = System.currentTimeMillis();
            List<DepartmentEntity> rows = departmentRepository.findByTenantIdAndDepthWithParent(TENANT_ID, BENCHMARK_DEPTH);
            forceParentAccess(rows);
            long elapsed = System.currentTimeMillis() - start;

            return new Result(elapsed, statistics.getPrepareStatementCount());
        });
    }

    private void forceParentAccess(List<DepartmentEntity> rows) {
        // parent 필드 접근을 강제하여 LAZY 경로의 추가 SQL을 유도한다.
        for (DepartmentEntity row : rows) {
            if (row.getParent() != null) {
                row.getParent().getDeptId();
            }
        }
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

    private record Result(long elapsedMs, long queryCount) { }
}





