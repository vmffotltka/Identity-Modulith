# 성능 최적화 가이드

> **최종 업데이트**: 2026-03-11  
> **상태**: ✅ 실측 완료 — `RbacPerformanceBenchmarkTest`로 수치 검증됨

---

## 📊 핵심 수치 요약

| 메서드 | Before | After | 개선율 | 쿼리 변화 |
|--------|:------:|:-----:|:------:|:---------:|
| `getEffectivePermissions` | **255 ms** | **10 ms** | **96.1% ↓** | 26 → 1 |
| `permissionsOfRoles` | **230 ms** | **12 ms** | **94.8% ↓** | 26 → 2 |

> 측정 환경: 로컬 PostgreSQL / 역할 5개 / 역할당 권한 4개 / 워밍업 3회 + 측정 10회

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

# 결과만 필터링 (PowerShell)
.\gradlew.bat test --tests "*.RbacPerformanceBenchmarkTest" --info 2>&1 |
  Select-String "\[BENCHMARK\]"
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
