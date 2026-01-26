# 캐시 구현 현황

## 📊 캐시 적용 위치

현재 프로젝트에서 **Spring Cache**가 적용된 위치는 다음과 같습니다:

### 1️⃣ RBAC 모듈 - 사용자 권한 캐싱

**위치**: `RbacQueryServiceImpl.permissionsOf()`

```java
@Cacheable(
    value = "userPermissions",
    key = "T(com.nexfron.identitymodulith.common.cache.CacheKeyGenerator).userPermissions(#tenantId, #agentId.toString())",
    unless = "#result.isEmpty()"
)
public Set<String> permissionsOf(String tenantId, UUID agentId)
```

**목적**: 
- 사용자가 보유한 모든 권한 코드를 캐싱
- API 호출마다 DB 조회하지 않고 메모리에서 빠르게 조회

**캐시 무효화 시점**:
- 사용자에게 역할 할당/회수 시
- 역할에 권한 할당/회수 시
- `RbacManagementServiceImpl`의 `@CacheEvict` 애노테이션으로 자동 처리

---

### 2️⃣ Organization 모듈 - 접근 가능 부서 캐싱

**위치**: `OrgScopeService.getAccessibleDepartmentIds()`

```java
@Cacheable(
    value = "accessibleDepts",
    key = "T(com.nexfron.identitymodulith.common.cache.CacheKeyGenerator).accessibleDepartments(#tenantId, #userId.toString())",
    unless = "#result == null || #result.isEmpty()"
)
public Set<String> getAccessibleDepartmentIds(String tenantId, UUID userId)
```

**목적**:
- 사용자가 접근 가능한 부서 ID 목록을 캐싱
- 조직도 조회 시 매번 계층 구조를 계산하지 않음

**캐시 무효화 시점**:
- 부서 구조 변경 시 (부서 이동, 삭제 등)
- 사용자의 부서 이동 시
- 역할 변경으로 데이터 스코프가 변경될 시

---

## 🔑 캐시 키 생성 방식

### CacheKeyGenerator 유틸리티

모든 캐시 키는 `CacheKeyGenerator` 클래스를 통해 **중앙화**되어 생성됩니다.

```java
public final class CacheKeyGenerator {
    // 캐시 키 형식: {cacheName}::{tenantId}::{identifier}
    
    public static String userPermissions(String tenantId, String userId)
    public static String accessibleDepartments(String tenantId, String userId)
    public static String rolePermissions(String tenantId, String roleName)
    public static String departmentTree(String tenantId)
    public static String departmentStatistics(String tenantId, String deptId)
}
```

**장점**:
- ✅ 테넌트 격리 보장 (각 테넌트별로 독립된 캐시)
- ✅ 오타 방지 및 일관성 보장
- ✅ 캐시 키 형식 변경 시 한 곳에서만 수정

---

## 🛡️ 테넌트 격리 구현

### 캐시 키에 tenantId 포함

```java
// 예시: tenant-001의 user-123 권한 캐시 키
"userPermissions::tenant-001::user-123"

// 예시: tenant-002의 user-123 권한 캐시 키 (완전히 분리됨)
"userPermissions::tenant-002::user-123"
```

**멀티테넌시 보안**:
- 같은 사용자 ID라도 테넌트가 다르면 **완전히 별도의 캐시**로 관리
- 테넌트 간 데이터 누수 차단

---

## 📦 캐시 설정

### RbacCacheConfig.java

```java
@Configuration
@EnableCaching
public class RbacCacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
            "userPermissions",      // 사용자 권한
            "accessibleDepts",      // 접근 가능 부서
            "rolePermissions"       // 역할 권한 (예약)
        );
        return cacheManager;
    }
}
```

**현재 캐시 구현체**:
- `ConcurrentMapCacheManager` (메모리 기반)
- 단일 서버 환경에 적합

**향후 확장**:
- Redis 도입 시 분산 캐시로 전환 가능
- `RedisCacheManager`로 교체하면 됨

---

## 🚀 성능 향상

### Before (캐시 없음)
```
요청 → DB 조회 (50-100ms) → 응답
```

### After (캐시 적용)
```
1회 요청 → DB 조회 (50-100ms) → 캐시 저장 → 응답
2회 요청 → 캐시 조회 (1-5ms) → 응답  ⚡ 10~100배 향상
```

---

## 📝 추가 캐싱 가능 영역 (옵션)

현재는 **핵심 조회만 캐싱**되어 있으며, 필요시 다음 영역도 캐싱 가능합니다:

| 영역 | 메서드 | 효과 | 우선순위 |
|------|--------|------|---------|
| 역할별 권한 목록 | `getRolePermissions()` | 역할-권한 매핑 조회 최적화 | 중 |
| 전체 조직도 | `getDepartmentTree()` | 조직도 렌더링 속도 향상 | 중 |
| 부서 통계 | `getDepartmentStatistics()` | 대시보드 성능 개선 | 낮 |

**현재 구조가 적절한 이유**:
- ✅ 가장 빈번한 조회만 캐싱 (과도한 캐싱 방지)
- ✅ 메모리 사용량 최소화
- ✅ 캐시 일관성 유지 용이

---

## 🔄 캐시 무효화 전략

### @CacheEvict 애노테이션 사용

```java
// 특정 사용자의 권한 캐시만 무효화
@CacheEvict(
    value = "userPermissions",
    key = "T(com.nexfron.identitymodulith.common.cache.CacheKeyGenerator).userPermissions(#tenantId, #agentId)"
)
public void assignRoleToAgent(...)

// 전체 캐시 무효화 (신중하게 사용)
@CacheEvict(value = "userPermissions", allEntries = true)
public void rebuildAllPermissions()
```

---

## ✅ 체크리스트

- [x] `CacheKeyGenerator` 구현 완료
- [x] `RbacQueryServiceImpl`에 캐싱 적용
- [x] `OrgScopeService`에 캐싱 적용
- [x] 테넌트 격리 확인
- [x] 캐시 무효화 로직 구현
- [x] `AuthPrincipal` 인터페이스 통합

---

## 📚 참고 자료

- Spring Cache Abstraction: https://docs.spring.io/spring-framework/reference/integration/cache.html
- SpEL (Spring Expression Language): https://docs.spring.io/spring-framework/reference/core/expressions.html
- Redis Cache (향후 도입 시): https://redis.io/docs/manual/client-side-caching/
