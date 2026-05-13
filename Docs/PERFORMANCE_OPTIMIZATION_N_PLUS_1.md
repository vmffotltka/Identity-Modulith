# N+1 문제 해결 상세 가이드

> **최종 업데이트**: 2026-03-29  
> **적용 범위**: RBAC 모듈 (`RbacQueryServiceImpl`, `RbacManagementServiceImpl`)  
> **상태**: ✅ 실측 완료 — 벤치마크 테스트로 Before/After 수치 검증됨


---

## 📋 목차
1. [N+1 문제란](#1-n1-문제란)
2. [이 프로젝트에서 발생한 위치](#2-이-프로젝트에서-발생한-위치)
3. [Before — N+1 패턴 코드](#3-before--n1-패턴-코드)
4. [After — 최적화된 코드](#4-after--최적화된-코드)
5. [실측 벤치마크 결과](#5-실측-벤치마크-결과)
6. [N+1 해결로 얻는 비즈니스/운영 이점](#6-n1-해결로-얻는-비즈니스운영-이점)
7. [측정 방법](#7-측정-방법)

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

## 6. N+1 해결로 얻는 비즈니스/운영 이점

> 아래 항목은 **실측값(5장)** + **코드 구조상 확정된 쿼리 수 변화**를 연결해 해석한 내용입니다.  
> 시간 수치는 로컬 기준이며, 운영 환경에서는 절대값보다 **개선 방향성과 기울기(증가 패턴 억제)**가 더 중요합니다.

### 6-1. 비즈니스 KPI 관점 (응답 지연 감소)

- 핵심 조회의 평균 응답이 `255ms -> 10ms`, `230ms -> 12ms`로 줄어, 동일 트래픽에서 SLA 준수 가능성이 크게 높아집니다.
- 화면/기능 체감 속도가 빨라지면 이탈률 감소, 처리 완료율 증가 같은 운영 KPI에 직접적인 긍정 효과가 납니다.

### 6-2. 인프라 비용 관점 (DB 부하/스케일 비용 완화)

- 쿼리 수가 `26 -> 1`, `26 -> 2`로 감소해, 동일 요청당 DB CPU/IO 소모가 크게 줄어듭니다.
- 트래픽 증가 시에도 수평 확장 시점(리소스 증설 시점)을 뒤로 미뤄 총 소유비용(TCO) 절감에 유리합니다.

### 6-3. 확장성 관점 (데이터 증가 시 성능 기울기 완화)

- Before는 `1 + N + M` 구조라 데이터가 늘수록 쿼리 수가 가파르게 증가합니다 (`61`, `221` queries 계산 사례).
- After는 메서드별 `1` 또는 `2` query 고정이어서, 데이터 증가 구간에서도 성능 예측 가능성이 높습니다.

### 6-4. 안정성 관점 (피크 시간 장애 확률 완화)

- N+1 패턴은 피크 트래픽에서 DB 커넥션/락 대기/타임아웃을 연쇄적으로 유발하기 쉽습니다.
- 쿼리 수를 고정화하면 피크 시간의 변동폭을 줄여, "갑자기 느려짐" 또는 간헐적 타임아웃 리스크를 낮출 수 있습니다.

### 6-5. 사용자 경험 관점 (일관된 체감 성능)

- 평균만 빨라지는 것이 아니라, `min/max` 범위가 작아져 응답 품질의 일관성이 좋아집니다.
- 특히 권한 계산처럼 요청마다 반복되는 경로에서 일관성 개선은 "가끔 느린 시스템" 인식을 줄이는 데 효과적입니다.

### 6-6. 개발/운영 생산성 관점 (디버깅·튜닝 난이도 감소)

- 쿼리 수가 고정되면 병목 포인트가 명확해져 성능 이슈 분석 시간이 짧아집니다.
- "데이터가 늘면 느려지는 이유"를 구조적으로 설명하기 쉬워져, 운영 대응과 커뮤니케이션 비용이 줄어듭니다.

### 6-7. 거버넌스/리스크 관점 (지속 가능한 성능 기준 확보)

- 이번 최적화는 단순 속도 개선이 아니라, "권한 조회 경로는 고정 쿼리"라는 설계 원칙을 만든 사례입니다.
- 향후 기능 추가 시에도 동일 원칙(스칼라 DTO 프로젝션, JOIN 집계)을 유지하면 성능 회귀를 예방하기 쉽습니다.

---

## 7. 측정 방법

### 7-1. 벤치마크 테스트 파일

```
src/test/java/com/identitymodulith/rbac/application/RbacPerformanceBenchmarkTest.java
```

### 7-2. 측정 전략

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

### 7-3. 실행 방법

```bash
# 벤치마크만 단독 실행
./gradlew test --tests "*.RbacPerformanceBenchmarkTest" --info

# 결과 확인 (콘솔 필터링)
./gradlew test --tests "*.RbacPerformanceBenchmarkTest" --info 2>&1 | grep "\[BENCHMARK\]"
```

### 7-4. 결과 로그 위치

```
build/reports/tests/test/index.html   ← HTML 리포트
콘솔 로그에서 [BENCHMARK] 키워드 검색
```

### 7-5. 측정 한계 및 주의사항

```
- 로컬 환경 기준 수치 (운영 DB 환경에서는 네트워크 지연 추가)
- 데이터 5개 역할 / 20개 권한 기준 (실제 운영 규모에서 Before는 더 큰 차이)
- JVM 워밍업 3회 후 측정하여 JIT 컴파일 영향 최소화
- PostgreSQL 쿼리 캐시로 인해 After의 수치는 실제 콜드 스타트보다 유리할 수 있음
```
