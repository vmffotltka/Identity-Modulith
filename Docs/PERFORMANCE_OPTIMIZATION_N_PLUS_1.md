# N+1 문제 해결 상세 가이드

> **최종 업데이트**: 2026-03-29  
> **적용 범위**: RBAC 모듈 (`RbacQueryServiceImpl`, `RbacManagementServiceImpl`)  
> **상태**: ✅ 실측 완료 — 벤치마크 테스트로 Before/After 수치 검증됨

> 이 문서는 RBAC N+1 최적화 전용입니다.  
> Organization(부서 parent Fetch Join) 성능 결과는 `Docs/PERFORMANCE_OPTIMIZATION.md`를 참고하세요.

---

## 📋 목차
1. [N+1 문제란](#1-n1-문제란)
2. [이 프로젝트에서 발생한 위치](#2-이-프로젝트에서-발생한-위치)
3. [Before — N+1 패턴 코드](#3-before--n1-패턴-코드)
4. [After — 최적화된 코드](#4-after--최적화된-코드)
5. [실측 벤치마크 결과](#5-실측-벤치마크-결과)
6. [측정 방법](#6-측정-방법)

---

## 1. N+1 문제란

JPA에서 **1번의 쿼리로 목록을 조회한 후, 각 항목마다 추가 쿼리가 N번 실행**되는 패턴입니다.

```
역할 5개, 권한 20개 환경:
  Q1:    agent_roles 조회        →  1 query
  Q2~6:  역할마다 role_permissions →  5 queries (N)
  Q7~26: 권한마다 permissions    → 20 queries (M)
  ─────────────────────────────────────────────
  합계:  1 + 5 + 20 = 26 queries
```

---

## 2. 이 프로젝트에서 발생한 위치

| 메서드 | 파일 | 발생 구조 |
|--------|------|-----------|
| `getEffectivePermissions` | `RbacManagementServiceImpl.java` | agent_roles → (N) role_permissions → (M) permissions |
| `permissionsOf` | `RbacQueryServiceImpl.java` | agent_roles → (N) role_permissions → (M) permissions |
| `permissionsOfRoles` | `RbacQueryServiceImpl.java` | roles → (N) role_permissions → (M) permissions |

---

## 3. Before — N+1 패턴 코드

### `getEffectivePermissions` (개선 전)

```java
// ❌ N+1 패턴 — 역할 수(N) + 권한 수(M) 만큼 쿼리 실행
// 파일: RbacManagementServiceImpl.java

// Q1: agent_roles 조회
List<AgentRoleJpaEntity> agentRoles = agentRoleRepository.findByAgentId(agentId);

Set<String> codes = new HashSet<>();
for (AgentRoleJpaEntity ar : agentRoles) {

    // QN: 역할마다 role_permissions 개별 조회
    List<RolePermissionJpaEntity> rps = rolePermissionRepository.findByRoleId(ar.getRoleId());

    for (RolePermissionJpaEntity rp : rps) {
        // QM: 권한마다 permissions 개별 조회
        permissionRepository.findById(rp.getPermissionId())
                .ifPresent(p -> codes.add(p.getCode()));
    }
}
// 역할 5개, 권한 20개 → 1 + 5 + 20 = 26 queries
```

### `permissionsOfRoles` (개선 전)

```java
// ❌ N+1 패턴
// 파일: RbacQueryServiceImpl.java

// Q1: roles 조회
List<RoleJpaEntity> roles = roleRepository.findByTenantIdAndNameIn(tenantId, roleNames);

Set<String> codes = new HashSet<>();
for (RoleJpaEntity role : roles) {

    // QN: 역할마다 role_permissions 개별 조회
    List<RolePermissionJpaEntity> rps = rolePermissionRepository.findByRoleId(role.getRoleId());

    for (RolePermissionJpaEntity rp : rps) {
        // QM: 권한마다 permissions 개별 조회
        permissionRepository.findById(rp.getPermissionId())
                .ifPresent(p -> codes.add(p.getCode()));
    }
}
// 역할 5개, 권한 20개 → 1 + 5 + 20 = 26 queries
```

### `permissionsOf` (개선 전)

```java
// ❌ N+1 패턴
// 파일: RbacQueryServiceImpl.java

// Q1: agent_roles에서 roleId 목록 조회
Set<String> roleIds = agentRoleRepository.findRoleIdsByAgentId(agentId.toString());

// QN + QM: roleId마다 permissions 엔티티 조회 후 code 추출
Set<String> permissionCodes = roleIds.stream()
        .flatMap(roleId -> rolePermissionRepository
                .findPermissionsByRoleIdAndTenant(roleId, tenantId)
                .stream()
                .map(PermissionJpaEntity::getCode))
        .collect(Collectors.toSet());
```

---

## 4. After — 최적화된 코드

### 4-1. `getEffectivePermissions` (개선 후)

```java
// ✅ 단일 3-JOIN 쿼리 — 역할/권한 수에 무관하게 항상 1 query
// 파일: RbacManagementServiceImpl.java (line ~1053)

List<String> permissionCodes =
        agentRoleRepository.findPermissionCodesByAgentIdAndTenant(agentId, tenantId);
// → 1 query (고정)
```

**Repository 쿼리** (`AgentRoleJpaRepository.java`):
```java
@Query("""
    SELECT DISTINCT p.code
    FROM AgentRoleJpaEntity ar
    JOIN RolePermissionJpaEntity rp ON ar.roleId = rp.roleId
    JOIN PermissionJpaEntity p      ON rp.permissionId = p.permissionId
    WHERE ar.agentId  = :agentId
      AND p.tenantId  = :tenantId
""")
List<String> findPermissionCodesByAgentIdAndTenant(
    @Param("agentId")  String agentId,
    @Param("tenantId") String tenantId);
```

### 4-2. `permissionsOfRoles` (개선 후)

```java
// ✅ 2-query — roles 조회 1회 + JOIN으로 권한 코드 일괄 조회 1회
// 파일: RbacQueryServiceImpl.java (line ~95)

// Q1: roles 조회
List<RoleJpaEntity> roles = roleRepository.findByTenantIdAndNameIn(tenantId, roleNames);
Set<String> roleIds = roles.stream().map(RoleJpaEntity::getRoleId).collect(toSet());

// Q2: JOIN으로 권한 코드 일괄 조회 (DTO 프로젝션)
List<String> permissionCodes =
        rolePermissionRepository.findPermissionCodesByRoleIdsAndTenant(roleIds, tenantId);
```

**Repository 쿼리** (`RolePermissionJpaRepository.java`):
```java
@Query("""
    SELECT DISTINCT p.code
    FROM RolePermissionJpaEntity rp
    JOIN PermissionJpaEntity p ON rp.permissionId = p.permissionId
    WHERE rp.roleId IN :roleIds
      AND p.tenantId = :tenantId
""")
List<String> findPermissionCodesByRoleIdsAndTenant(
    @Param("roleIds")  Collection<String> roleIds,
    @Param("tenantId") String tenantId);
```

### 4-3. `permissionsOf` (개선 후)

```java
// ✅ 1-query — agent_roles → role_permissions → permissions 단일 JOIN
// 파일: RbacQueryServiceImpl.java (line ~170)

// Q1: 스칼라 프로젝션으로 권한 코드만 조회
List<String> permissionCodes = agentRoleRepository
        .findPermissionCodesByAgentIdAndTenant(agentId.toString(), tenantId);

// 중복 제거
Set<String> codes = new HashSet<>(permissionCodes);
```

**핵심 포인트**
- 기존: roleId별 반복 조회 + `PermissionJpaEntity` 전체 로딩 후 `code`만 사용 (N+1)
- 개선: `List<String>` 스칼라 프로젝션으로 필요한 컬럼(`p.code`)만 조회
- 효과: 쿼리 수 고정(1 query), 불필요한 엔티티 로딩 제거

---

## 5. 실측 벤치마크 결과

> **측정 환경**: 로컬 PostgreSQL / 역할 5개 / 역할당 권한 4개 (총 20개) / 10 rounds

### `getEffectivePermissions`

| 구분 | 쿼리 수 | 평균 응답 | min | max | 각 라운드 (ms) |
|------|:-------:|:---------:|:---:|:---:|----------------|
| **Before** (N+1) | 26 queries | **255 ms** | 225 ms | 305 ms | [244, 235, 275, 240, 225, 248, 241, 270, 305, 267] |
| **After** (3-JOIN) | 1 query | **10 ms** | 9 ms | 15 ms | [15, 9, 9, 11, 11, 13, 9, 11, 9, 10] |
| **개선** | 26 → 1 | **96.1% 단축** | | | |

### `permissionsOfRoles`

| 구분 | 쿼리 수 | 평균 응답 | min | max | 각 라운드 (ms) |
|------|:-------:|:---------:|:---:|:---:|----------------|
| **Before** (N+1) | 26 queries | **230 ms** | 207 ms | 249 ms | [249, 215, 247, 207, 238, 227, 231, 244, 229, 219] |
| **After** (2-query) | 2 queries | **12 ms** | 11 ms | 14 ms | [14, 12, 11, 13, 13, 12, 12, 14, 12, 13] |
| **개선** | 26 → 2 | **94.8% 단축** | | | |

### 확장성 — 데이터 증가 시 쿼리 수 변화

> **쿼리 수**는 코드 구조에서 계산한 팩트입니다. 응답 시간은 별도 실측하지 않았습니다.

```
Before (N+1):  1 + 역할수(N) + 권한수(M)  — 코드 구조상 계산값
  역할  5개 × 권한  4개 →  26 queries  ← 실측 환경
  역할 10개 × 권한  5개 →  61 queries  ← 계산값 (미실측)
  역할 20개 × 권한 10개 → 221 queries  ← 계산값 (미실측)

After (최적화): 역할/권한 수에 무관하게 고정  — 코드 구조상 확정
  getEffectivePermissions → 항상 1 query   (실측 확인)
  permissionsOf         → 항상 1 query   (구조 확인, 별도 실측 미포함)
  permissionsOfRoles      → 항상 2 queries (실측 확인)
```

---

## 6. 측정 방법

### 6-1. 벤치마크 테스트 파일

```
src/test/java/com/identitymodulith/rbac/application/RbacPerformanceBenchmarkTest.java
```

### 6-2. 측정 전략

```
Before 측정:  N+1 패턴을 simulateNPlusOne() 메서드로 직접 재현
              (실제 개선 전 코드와 동일한 반복문 + findByRoleId + findById 패턴)

After 측정:   최적화된 서비스 메서드 직접 호출
              getEffectivePermissions(agentId) / permissionsOfRoles(tenantId, roleNames)
              * permissionsOf(tenantId, agentId)는 구조 검증 완료, 본 문서의 실측 표에는 미포함

워밍업:       JVM/DB 커넥션 풀 안정화를 위해 3회 사전 실행 후 10회 측정
측정 도구:    Spring의 StopWatch — 각 라운드별 ms 단위 기록
데이터:       JdbcTemplate으로 직접 INSERT (Hibernate 캐시 영향 배제)
정리:         JdbcTemplate으로 직접 DELETE (FK 역순, 낙관적 잠금 우회)
```

### 6-3. 실행 방법

```bash
# 벤치마크만 단독 실행
./gradlew test --tests "*.RbacPerformanceBenchmarkTest" --info

# 결과 확인 (콘솔 필터링)
./gradlew test --tests "*.RbacPerformanceBenchmarkTest" --info 2>&1 | grep "\[BENCHMARK\]"
```

### 6-4. 결과 로그 위치

```
build/reports/tests/test/index.html   ← HTML 리포트
콘솔 로그에서 [BENCHMARK] 키워드 검색
```

### 6-5. 측정 한계 및 주의사항

```
- 로컬 환경 기준 수치 (운영 DB 환경에서는 네트워크 지연 추가)
- 데이터 5개 역할 / 20개 권한 기준 (실제 운영 규모에서 Before는 더 큰 차이)
- JVM 워밍업 3회 후 측정하여 JIT 컴파일 영향 최소화
- PostgreSQL 쿼리 캐시로 인해 After의 수치는 실제 콜드 스타트보다 유리할 수 있음
```
