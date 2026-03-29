# 성능 최적화 가이드

> **최종 업데이트**: 2026-03-29  
> **상태**: ✅ 실측/회귀 검증 완료 — RBAC + Organization(Fetch Join + DTO Projection)

---

## 📊 핵심 수치 요약

| 메서드 | Before | After | 개선율 | 쿼리 변화 |
|--------|:------:|:-----:|:------:|:---------:|
| `getEffectivePermissions` | **255 ms** | **10 ms** | **96.1% ↓** | 26 → 1 |
| `permissionsOfRoles` | **230 ms** | **12 ms** | **94.8% ↓** | 26 → 2 |
| `getDepartmentsByDepth`(dept=1, parent 접근) | **27 ms** | **31 ms** | 로컬 소규모는 유사 | 1 → 1 |
| `Department 목록(TREE)` | **135 ms** | **58 ms** | **57.0% ↓** | 1 → 1 |
| `Department 목록(SEARCH)` | **35 ms** | **22 ms** | **37.1% ↓** | 1 → 1 |
| `Department 목록(SUBTREE)` | **18 ms** | **8 ms** | **55.6% ↓** | 1 → 1 |
| `Department 목록(DEPTH)` | **133 ms** | **66 ms** | **50.4% ↓** | 1 → 1 |
| `Department 목록(TYPE)` | **146 ms** | **65 ms** | **55.5% ↓** | 1 → 1 |
| `Department 목록(SCOPE_IN)` | **165 ms** | **10 ms** | **93.9% ↓** | 1 → 1 |

> 측정 환경: 로컬 PostgreSQL / 워밍업 3회 + 측정 10회

> 참고: Organization Fetch Join은 소규모 데이터에서 쿼리 수가 동일할 수 있으나,
> parent fan-out이 커지는 실제 운영 규모에서 N+1 회귀를 방지하는 목적이 핵심입니다.

---

## 1. 문제 배경

RBAC 권한 조회는 **인증된 모든 API 요청에서 호출**됩니다. 권한이 없으면 요청 자체가 차단되므로, 이 경로의 성능이 전체 API 응답 시간에 직결됩니다.

### 데이터 구조
```
agents (User 모듈)
  └── rbac_agent_roles      (N) : 상담사 ↔ 역할 매핑
        └── rbac_roles       (1)
              └── rbac_role_permissions  (N) : 역할 ↔ 권한 매핑
                    └── rbac_permissions  (1)
```

---

## 2. 개선된 메서드 및 변경 파일

### 2-1. `getEffectivePermissions` — 96.1% 개선

**변경 파일**: `RbacManagementServiceImpl.java`  
**추가 Repository 메서드**: `AgentRoleJpaRepository.findPermissionCodesByAgentIdAndTenant()`

```java
// ❌ Before: 반복문 내 N+1 (26 queries, 255ms 평균)
List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByAgentId(agentId);
for (AgentRoleJpaEntity ar : agentRoles) {
    List<RolePermissionJpaEntity> rps = rolePermissionRepository.findByRoleId(ar.getRoleId()); // QN
    for (RolePermissionJpaEntity rp : rps) {
        permissionRepository.findById(rp.getPermissionId())  // QM
                .ifPresent(p -> codes.add(p.getCode()));
    }
}

// ✅ After: 3-JOIN 단일 쿼리 (1 query, 10ms 평균)
List<String> permissionCodes =
    agentRoleRepository.findPermissionCodesByAgentIdAndTenant(agentId, tenantId);
```

**추가된 JPQL** (`AgentRoleJpaRepository.java`):
```sql
SELECT DISTINCT p.code
FROM AgentRoleJpaEntity ar
JOIN RolePermissionJpaEntity rp ON ar.roleId = rp.roleId
JOIN PermissionJpaEntity p      ON rp.permissionId = p.permissionId
WHERE ar.agentId = :agentId AND p.tenantId = :tenantId
```

---

### 2-2. `permissionsOfRoles` — 94.8% 개선

**변경 파일**: `RbacQueryServiceImpl.java`  
**추가 Repository 메서드**: `RolePermissionJpaRepository.findPermissionCodesByRoleIdsAndTenant()`

```java
// ❌ Before: 반복문 내 N+1 (26 queries, 230ms 평균)
List<RoleJpaEntity> roles = roleRepository.findByTenantIdAndNameIn(tenantId, roleNames);
for (RoleJpaEntity role : roles) {
    List<RolePermissionJpaEntity> rps = rolePermissionRepository.findByRoleId(role.getRoleId()); // QN
    for (RolePermissionJpaEntity rp : rps) {
        permissionRepository.findById(rp.getPermissionId())  // QM
                .ifPresent(p -> codes.add(p.getCode()));
    }
}

// ✅ After: 2-query (고정, 12ms 평균)
List<RoleJpaEntity> roles = roleRepository.findByTenantIdAndNameIn(tenantId, roleNames); // Q1
Set<String> roleIds = roles.stream().map(RoleJpaEntity::getRoleId).collect(toSet());
List<String> permissionCodes =
    rolePermissionRepository.findPermissionCodesByRoleIdsAndTenant(roleIds, tenantId);  // Q2
```

### 2-3. Organization 부서 조회 경로 — Fetch Join 회귀 방지

**변경 파일**: `DepartmentServiceImpl.java`, `AgentOrgUserAdapter.java`  
**추가 Repository 메서드**: `JpaDepartmentRepository.*WithParent` 계열 6개

적용 메서드:
- `findAllByTenantIdWithParent`
- `findByDeptIdAndTenantIdWithParent`
- `findByTenantIdAndNameContainingIgnoreCaseWithParent`
- `findByTenantIdAndDepthWithParent`
- `findByTenantIdAndTypeWithParent`
- `findByTenantIdAndOrgPathStartsWithWithParent`

핵심 의도:
- DTO 변환 시 `parent` 접근이 반복되는 경로에서 LAZY 추가 쿼리(N+1) 가능성 제거
- 트리/검색/깊이/타입 조회 등 공통 진입점에서 동일 전략 적용

벤치마크(`DepartmentFetchJoinBenchmarkTest`) 관찰값:
- depth=1, 자식 200개, 루트 20개(부모 공유 구조)
- Before(LAZY): 평균 27ms, 평균 1 query
- After(FETCH): 평균 31ms, 평균 1 query

해석:
- 이번 데이터셋은 다수 자식이 동일 parent를 참조해 영속성 컨텍스트 캐시 효과로 LAZY도 1 query로 수렴 가능
- Fetch Join의 가치는 "항상 더 빠른 시간"보다 "데이터 분포 변화 시 쿼리 급증 방지"에 있음

### 2-4. Organization 목록성 조회 경로 — DTO Projection 병행 적용

**변경 파일**: `DepartmentServiceImpl.java`, `JpaDepartmentRepository.java`, `DepartmentListProjection.java`

적용 서비스 메서드:
- `getDepartmentTree`
- `getDepartmentTreeWithinScope`
- `searchDepartments`
- `getSubtree`
- `getDepartmentsByDepth`
- `getDepartmentsByType`

검증 테스트:
- 벤치마크: `DepartmentListProjectionBenchmarkTest`
  - 비교: Fetch Join 엔티티 조회 vs DTO Projection 조회
  - 시나리오: TREE/SEARCH/SUBTREE/DEPTH/TYPE/SCOPE_IN
- 회귀 테스트: `DepartmentQueryBudgetRegressionTest`
  - 쿼리 예산: `getDepartmentTree<=1`, `search<=1`, `subtree<=2`, `depth<=1`, `type<=1`

**추가된 JPQL** (`RolePermissionJpaRepository.java`):
```sql
SELECT DISTINCT p.code
FROM RolePermissionJpaEntity rp
JOIN PermissionJpaEntity p ON rp.permissionId = p.permissionId
WHERE rp.roleId IN :roleIds AND p.tenantId = :tenantId
```

---

## 3. 실측 결과 상세

### `getEffectivePermissions` (10 rounds)

```
[BEFORE] N+1 패턴  — 26 queries
  라운드: [244, 235, 275, 240, 225, 248, 241, 270, 305, 267] ms
  평균: 255ms  |  min: 225ms  |  max: 305ms

[AFTER]  3-JOIN   —  1 query
  라운드: [15, 9, 9, 11, 11, 13, 9, 11, 9, 10] ms
  평균:  10ms  |  min:   9ms  |  max:  15ms

→ 255ms → 10ms (96.1% 단축, 쿼리 26개 → 1개)
```

### `permissionsOfRoles` (10 rounds)

```
[BEFORE] N+1 패턴  — 26 queries
  라운드: [249, 215, 247, 207, 238, 227, 231, 244, 229, 219] ms
  평균: 230ms  |  min: 207ms  |  max: 249ms

[AFTER]  2-query  —  2 queries
  라운드: [14, 12, 11, 13, 13, 12, 12, 14, 12, 13] ms
  평균:  12ms  |  min:  11ms  |  max:  14ms

→ 230ms → 12ms (94.8% 단축, 쿼리 26개 → 2개)
```

### `DepartmentListProjectionBenchmarkTest` (10 rounds)

```
[TREE] Fetch Entity vs Projection
  BEFORE: 135ms, 1 query, rows=840
  AFTER :  58ms, 1 query, rows=840

[SEARCH] Fetch Entity vs Projection
  BEFORE: 35ms, 1 query, rows=220
  AFTER : 22ms, 1 query, rows=220

[SUBTREE] Fetch Entity vs Projection
  BEFORE: 18ms, 1 query, rows=21
  AFTER :  8ms, 1 query, rows=21

[DEPTH] Fetch Entity vs Projection
  BEFORE: 133ms, 1 query, rows=800
  AFTER :  66ms, 1 query, rows=800

[TYPE] Fetch Entity vs Projection
  BEFORE: 146ms, 1 query, rows=800
  AFTER :  65ms, 1 query, rows=800

[SCOPE_IN] Fetch Entity(전체 조회 후 메모리 필터) vs Projection(IN 조회)
  BEFORE: 165ms, 1 query, rows=63
  AFTER :  10ms, 1 query, rows=63
```

### `DepartmentQueryBudgetRegressionTest` 결과

```
[QUERY_BUDGET][DEPT][TREE]   queries=1, rows=15
[QUERY_BUDGET][DEPT][SEARCH] queries=1, rows=70
[QUERY_BUDGET][DEPT][SUBTREE] queries=2, rows=11
[QUERY_BUDGET][DEPT][DEPTH]  queries=1, rows=150
[QUERY_BUDGET][DEPT][TYPE]   queries=1, rows=150
```

해석:
- Projection 적용 후 모든 목록 경로에서 쿼리 수는 유지(1회)되면서 평균 응답시간이 개선되었습니다.
- 특히 `SCOPE_IN`은 전체 엔티티 로딩 + 메모리 필터를 DB `IN` 조회로 바꾸며 개선 폭이 가장 큽니다.
- 쿼리 예산 회귀 테스트도 모두 상한 내(`TREE<=1`, `SEARCH<=1`, `SUBTREE<=2`, `DEPTH<=1`, `TYPE<=1`)로 통과했습니다.

---

## 4. 확장성 분석

아래 **쿼리 수**는 코드 구조에서 계산한 팩트입니다. 응답 시간은 로컬 환경에서 별도 실측하지 않았으며, 쿼리 수 증가에 비례해 늘어나는 것이 일반적이나 별도 측정 필요합니다.

```
쿼리 수 공식 (Before):  1 + 역할수(N) + 권한수(M)

  역할  5개 × 권한  4개 →  26 queries  ← 실측 환경
  역할 10개 × 권한  5개 →  61 queries  ← 계산값 (미실측)
  역할 20개 × 권한 10개 → 221 queries  ← 계산값 (미실측)

쿼리 수 공식 (After):   고정 (코드 구조상 확정)
  getEffectivePermissions → 1 query   (항상, 실측 확인)
  permissionsOfRoles      → 2 queries (항상, 실측 확인)
```

---

## 5. 벤치마크 재실행 방법

```bash
# 단독 실행
./gradlew test --tests "*.RbacPerformanceBenchmarkTest" --info

# Organization Fetch Join 벤치마크 단독 실행
./gradlew test --tests "*.DepartmentFetchJoinBenchmarkTest" --info

# Organization Projection 벤치마크 단독 실행
./gradlew test --tests "*.DepartmentListProjectionBenchmarkTest" --info

# Organization 쿼리 예산 회귀 테스트 단독 실행
./gradlew test --tests "*.DepartmentQueryBudgetRegressionTest" --info

# 결과만 필터링 (PowerShell)
.\gradlew.bat test --tests "*.RbacPerformanceBenchmarkTest" --info 2>&1 |
  Select-String "\[BENCHMARK\]"

.\gradlew.bat test --tests "*.DepartmentFetchJoinBenchmarkTest" --info 2>&1 |
  Select-String "\[BENCHMARK\]"

.\gradlew.bat test --tests "*.DepartmentListProjectionBenchmarkTest" --info 2>&1 |
  Select-String "\[BENCHMARK\]"

.\gradlew.bat test --tests "*.DepartmentQueryBudgetRegressionTest" --info 2>&1 |
  Select-String "\[QUERY_BUDGET\]"
```

**테스트 파일**: `src/test/java/com/identitymodulith/rbac/application/RbacPerformanceBenchmarkTest.java`

측정 방식:
- **Before**: `simulateNPlusOne()` — 반복문 내 `findByRoleId` + `findById` 직접 재현
- **After**: 최적화된 서비스 메서드 직접 호출
- **데이터 격리**: `JdbcTemplate`으로 직접 INSERT/DELETE (Hibernate 캐시 영향 배제)
- **워밍업**: JVM/DB 커넥션 풀 안정화를 위해 3회 선행 실행

---

## 6. 운영 환경 권장 추가 최적화

> ⚠️ 아래 항목은 **현재 미구현** 상태이며, 효과 수치는 실측값이 아닌 일반적인 기술 특성에 근거한 정성적 설명입니다.

| 항목 | 설명 | 기대 방향 (미실측) |
|------|------|-----------|
| Redis 캐싱 | 동일 agentId의 권한은 세션 동안 불변 → `@Cacheable` 적용 가능 | 반복 호출 시 DB 쿼리 감소 |
| 인덱스 확인 | `rbac_agent_roles(agent_id)`, `rbac_role_permissions(role_id)` 인덱스 존재 여부 확인 | 대용량 데이터 시 쿼리 계획 최적화 |
| 커넥션 풀 튜닝 | HikariCP `maximumPoolSize` 조정 | 동시 요청 처리 성능 관련 (부하 테스트로 확인 필요) |
