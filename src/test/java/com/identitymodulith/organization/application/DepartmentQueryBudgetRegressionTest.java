package com.identitymodulith.organization.application;

import com.identitymodulith.organization.application.service.DepartmentServiceImpl;
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
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Department 목록성 조회 경로의 쿼리 수 회귀를 방지한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DepartmentQueryBudgetRegressionTest {

    private static final Logger log = LoggerFactory.getLogger(DepartmentQueryBudgetRegressionTest.class);

    private static final String TENANT_ID = "benchmark-tenant-querybudget";
    private static final int ROOT_COUNT = 15;
    private static final int CHILD_PER_ROOT = 10;

    @Autowired
    private DepartmentServiceImpl departmentService;

    @Autowired
    private JpaDepartmentRepository departmentRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;
    private TransactionTemplate readOnlyTx;
    private String sampleRootId;

    @BeforeEach
    void setUp() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);

        readOnlyTx = new TransactionTemplate(transactionManager);
        readOnlyTx.setReadOnly(true);

        cleanupData();
        seedData();

        sampleRootId = departmentRepository.findByTenantIdAndDepth(TENANT_ID, 0)
                .stream()
                .findFirst()
                .map(DepartmentEntity::getDeptId)
                .orElseThrow();

        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        cleanupData();
    }

    @Test
    @Order(1)
    @DisplayName("getDepartmentTree 쿼리 예산 <= 1")
    void queryBudget_getDepartmentTree() {
        QueryResult<List<?>> result = measure(() -> departmentService.getDepartmentTree(TENANT_ID));
        log.info("[QUERY_BUDGET][DEPT][TREE] queries={}, rows={}", result.queries, result.rows.size());
        assertThat(result.rows).isNotEmpty();
        assertThat(result.queries).isLessThanOrEqualTo(1L);
    }

    @Test
    @Order(2)
    @DisplayName("searchDepartments 쿼리 예산 <= 1")
    void queryBudget_searchDepartments() {
        QueryResult<List<?>> result = measure(() -> departmentService.searchDepartments(TENANT_ID, "CHILD_1_"));
        log.info("[QUERY_BUDGET][DEPT][SEARCH] queries={}, rows={}", result.queries, result.rows.size());
        assertThat(result.rows).isNotEmpty();
        assertThat(result.queries).isLessThanOrEqualTo(1L);
    }

    @Test
    @Order(3)
    @DisplayName("getSubtree 쿼리 예산 <= 2")
    void queryBudget_getSubtree() {
        QueryResult<List<?>> result = measure(() -> departmentService.getSubtree(TENANT_ID, sampleRootId));
        log.info("[QUERY_BUDGET][DEPT][SUBTREE] queries={}, rows={}", result.queries, result.rows.size());
        assertThat(result.rows).isNotEmpty();
        assertThat(result.queries).isLessThanOrEqualTo(2L);
    }

    @Test
    @Order(4)
    @DisplayName("getDepartmentsByDepth 쿼리 예산 <= 1")
    void queryBudget_getDepartmentsByDepth() {
        QueryResult<List<?>> result = measure(() -> departmentService.getDepartmentsByDepth(TENANT_ID, 1));
        log.info("[QUERY_BUDGET][DEPT][DEPTH] queries={}, rows={}", result.queries, result.rows.size());
        assertThat(result.rows).isNotEmpty();
        assertThat(result.queries).isLessThanOrEqualTo(1L);
    }

    @Test
    @Order(5)
    @DisplayName("getDepartmentsByType 쿼리 예산 <= 1")
    void queryBudget_getDepartmentsByType() {
        QueryResult<List<?>> result = measure(() -> departmentService.getDepartmentsByType(TENANT_ID, DepartmentType.TEAM));
        log.info("[QUERY_BUDGET][DEPT][TYPE] queries={}, rows={}", result.queries, result.rows.size());
        assertThat(result.rows).isNotEmpty();
        assertThat(result.queries).isLessThanOrEqualTo(1L);
    }

    private QueryResult<List<?>> measure(Supplier<List<?>> call) {
        return readOnlyTx.execute(status -> {
            statistics.clear();
            entityManager.clear();
            List<?> rows = call.get();
            return new QueryResult<>(statistics.getPrepareStatementCount(), rows);
        });
    }

    private void seedData() {
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

    private void cleanupData() {
        jdbc.update("DELETE FROM org_departments WHERE tenant_id = ?", TENANT_ID);
    }

    private record QueryResult<T>(long queries, T rows) {}
}


