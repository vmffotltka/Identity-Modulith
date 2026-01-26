# Spring Cache 동작 원리 상세 가이드

## 🎯 핵심 개념

### 캐시란?
**자주 조회하는 데이터를 메모리(RAM)에 임시 저장**해서, 같은 요청이 들어올 때 DB 조회 없이 빠르게 응답하는 기술입니다.

```
일반 요청 흐름:
클라이언트 → API → Service → DB 조회 (느림 💤) → 응답

캐시 적용 후:
클라이언트 → API → Service → 메모리 조회 (빠름 ⚡) → 응답
                            ↓ (캐시 없을 때만)
                            → DB 조회 → 메모리 저장
```

---

## 📚 @Cacheable - 조회 결과 캐싱

### 동작 방식

```java
@Cacheable(
    value = "userPermissions",
    key = "T(CacheKeyGenerator).userPermissions(#tenantId, #agentId.toString())"
)
public Set<String> permissionsOf(String tenantId, UUID agentId) {
    // DB 조회 로직
    return repository.findPermissionsByAgentId(agentId);
}
```

### 🔄 실행 흐름 (첫 번째 호출)

```
1. permissionsOf("tenant-001", UUID("abc-123")) 호출
   ↓
2. 캐시 키 생성: "userPermissions::tenant-001::abc-123"
   ↓
3. 메모리에서 해당 키 검색
   ↓
4. 캐시 없음 (Cache Miss)
   ↓
5. 메서드 실행 → DB 조회
   ↓
6. 결과: ["user:read", "user:create"]
   ↓
7. 메모리에 저장 (캐싱)
   [키] userPermissions::tenant-001::abc-123
   [값] ["user:read", "user:create"]
   ↓
8. 결과 반환
```

### ⚡ 실행 흐름 (두 번째 호출 - 같은 파라미터)

```
1. permissionsOf("tenant-001", UUID("abc-123")) 호출
   ↓
2. 캐시 키 생성: "userPermissions::tenant-001::abc-123"
   ↓
3. 메모리에서 해당 키 검색
   ↓
4. 캐시 있음 (Cache Hit) ✅
   ↓
5. 메서드 실행 SKIP (DB 조회 안 함!)
   ↓
6. 메모리에서 바로 반환: ["user:read", "user:create"]
```

### 📊 성능 비교

| 구분 | DB 조회 | 캐시 조회 | 성능 차이 |
|------|---------|----------|----------|
| 응답 시간 | 50-100ms | 1-5ms | **10~100배 빠름** |
| DB 부하 | 있음 | 없음 | **DB 부담 감소** |

---

## 🗑️ @CacheEvict - 캐시 무효화

### 동작 방식

```java
@CacheEvict(
    value = "userPermissions",
    key = "T(CacheKeyGenerator).userPermissions(#tenantId, #agentId)"
)
public void assignRoleToAgent(String tenantId, UUID agentId, String roleName) {
    // 역할 할당 로직
    agentRoleRepository.save(...);
}
```

### 🔄 실행 흐름

```
1. assignRoleToAgent("tenant-001", UUID("abc-123"), "ADMIN") 호출
   ↓
2. 캐시 키 생성: "userPermissions::tenant-001::abc-123"
   ↓
3. 메서드 실행 (역할 할당 - DB 업데이트)
   ↓
4. 메모리에서 해당 키의 캐시 삭제
   [삭제됨] userPermissions::tenant-001::abc-123
   ↓
5. 다음 permissionsOf() 호출 시 DB에서 최신 데이터 조회
```

### 왜 필요한가?

**예시 상황:**
```
1. 사용자 A의 권한 조회 → ["user:read"] (캐시 저장)
2. 관리자가 사용자 A에게 "ADMIN" 역할 할당
3. 사용자 A의 권한 조회 → ???
```

**@CacheEvict 없으면:**
```
3. 사용자 A의 권한 조회 → ["user:read"] (캐시에서 조회)
   ❌ 잘못된 정보! (실제로는 ADMIN 권한도 있음)
```

**@CacheEvict 있으면:**
```
2. 역할 할당 + 캐시 삭제
3. 사용자 A의 권한 조회 → DB 조회 → ["user:read", "admin:*"]
   ✅ 최신 정보!
```

---

## 💾 메모리에 저장되는 구조

### 내부 저장 방식 (ConcurrentHashMap)

```
메모리 (RAM)
├─ userPermissions::tenant-001::user-123
│  └─ ["user:read", "user:create", "user:delete"]
│
├─ userPermissions::tenant-001::user-456
│  └─ ["user:read"]
│
├─ accessibleDepts::tenant-001::user-123
│  └─ ["dept-001", "dept-002", "dept-003"]
│
└─ accessibleDepts::tenant-002::user-789
   └─ ["dept-100", "dept-101"]
```

### 실제 코드 내부 (Spring Framework)

```java
// Spring이 내부적으로 이렇게 관리
Map<String, Object> cache = new ConcurrentHashMap<>();

// @Cacheable 동작
public Set<String> permissionsOf(String tenantId, UUID agentId) {
    String cacheKey = "userPermissions::tenant-001::abc-123";
    
    // 캐시 확인
    if (cache.containsKey(cacheKey)) {
        return (Set<String>) cache.get(cacheKey); // 캐시에서 바로 반환
    }
    
    // 캐시 없으면 DB 조회
    Set<String> result = repository.findPermissions(...);
    
    // 캐시 저장
    cache.put(cacheKey, result);
    
    return result;
}

// @CacheEvict 동작
public void assignRole(...) {
    // 비즈니스 로직 실행
    repository.save(...);
    
    // 캐시 삭제
    String cacheKey = "userPermissions::tenant-001::abc-123";
    cache.remove(cacheKey);
}
```

---

## 🔑 캐시 키 생성 원리

### SpEL (Spring Expression Language) 사용

```java
@Cacheable(
    value = "userPermissions",
    key = "T(CacheKeyGenerator).userPermissions(#tenantId, #agentId.toString())"
    //     ↑ T(클래스명)           ↑ 파라미터 참조
)
public Set<String> permissionsOf(String tenantId, UUID agentId)
```

**SpEL 문법 해석:**
- `T(...)` : 클래스의 static 메서드 호출
- `#tenantId` : 메서드 파라미터 `tenantId` 값 참조
- `#agentId.toString()` : 파라미터 `agentId`의 `toString()` 호출

**실행 예시:**
```java
// 호출: permissionsOf("tenant-001", UUID.fromString("abc-123"))
// 캐시 키 생성 과정:
CacheKeyGenerator.userPermissions("tenant-001", "abc-123")
→ return "userPermissions::tenant-001::abc-123"
```

---

## 🛡️ 테넌트 격리가 중요한 이유

### 잘못된 예시 (tenantId 없음)

```java
@Cacheable(
    value = "userPermissions",
    key = "#agentId.toString()" // ❌ tenantId 없음!
)
```

**문제 발생:**
```
1. Tenant A의 user-123 권한 조회 → ["user:read"] (캐시 저장)
   캐시 키: "user-123"

2. Tenant B의 user-123 권한 조회 → ???
   캐시 키: "user-123" (같음!)
   
3. Tenant B의 user-123이 Tenant A의 권한을 받게 됨!
   ❌ 보안 취약점!
```

### 올바른 예시 (tenantId 포함)

```java
@Cacheable(
    value = "userPermissions",
    key = "T(CacheKeyGenerator).userPermissions(#tenantId, #agentId.toString())"
)
```

**안전한 동작:**
```
1. Tenant A의 user-123 권한 조회
   캐시 키: "userPermissions::tenant-A::user-123"

2. Tenant B의 user-123 권한 조회
   캐시 키: "userPermissions::tenant-B::user-123"
   
3. 완전히 다른 캐시이므로 안전 ✅
```

---

## 🔄 실제 사용 시나리오

### 시나리오 1: 권한 확인 API 호출

```
사용자 A가 5번 연속 "내 권한 조회" 버튼 클릭

[1번 클릭] 
→ DB 조회 (100ms)
→ 캐시 저장
→ 응답

[2~5번 클릭]
→ 캐시 조회 (2ms)
→ 응답

결과: DB 부하 80% 감소, 응답 속도 50배 향상
```

### 시나리오 2: 권한 변경

```
1. 사용자 A 권한 조회 (캐시 저장)
   → ["user:read"]

2. 관리자가 사용자 A에게 "ADMIN" 역할 할당
   → @CacheEvict 실행
   → 캐시 삭제

3. 사용자 A 권한 조회 (캐시 없음)
   → DB 조회
   → ["user:read", "admin:*"]
   → 캐시 저장

4. 사용자 A 권한 조회 (캐시 Hit)
   → ["user:read", "admin:*"]
```

---

## ⚙️ 캐시 무효화 옵션

### 1. 특정 키만 삭제

```java
@CacheEvict(
    value = "userPermissions",
    key = "T(CacheKeyGenerator).userPermissions(#tenantId, #agentId)"
)
public void assignRoleToAgent(...)
```

**삭제 범위:** `userPermissions::tenant-001::abc-123` 하나만

---

### 2. 전체 캐시 삭제

```java
@CacheEvict(
    value = "userPermissions",
    allEntries = true // ⚠️ 모든 캐시 삭제
)
public void rebuildAllPermissions()
```

**삭제 범위:** `userPermissions::*` 전체

**주의사항:**
- 성능 저하 가능 (모든 사용자가 다시 DB 조회)
- 꼭 필요한 경우만 사용 (예: 권한 시스템 대규모 리팩토링)

---

### 3. 여러 캐시 동시 삭제

```java
@Caching(evict = {
    @CacheEvict(value = "userPermissions", key = "..."),
    @CacheEvict(value = "accessibleDepts", key = "...")
})
public void changeDepartment(...)
```

**사용 예:** 부서 이동 시 권한 캐시 + 접근 가능 부서 캐시 둘 다 삭제

---

## 🎯 언제 캐시를 사용해야 하나?

### ✅ 캐시가 적합한 경우

1. **읽기가 많고, 쓰기가 적은 데이터**
   - 예: 사용자 권한, 부서 구조

2. **계산 비용이 높은 데이터**
   - 예: 조직도 트리 구조 계산

3. **자주 조회되는 데이터**
   - 예: 현재 사용자의 권한 정보

### ❌ 캐시가 부적합한 경우

1. **실시간성이 중요한 데이터**
   - 예: 주식 시세, 실시간 채팅

2. **쓰기가 빈번한 데이터**
   - 예: 로그 데이터, 조회수 카운터

3. **데이터 크기가 큰 경우**
   - 예: 대용량 파일, 이미지

---

## 📊 우리 프로젝트의 캐시 전략

### 현재 캐싱 대상

| 데이터 | 읽기 빈도 | 쓰기 빈도 | 캐시 적합도 |
|--------|----------|----------|------------|
| 사용자 권한 | 매우 높음 | 낮음 | ⭐⭐⭐⭐⭐ |
| 접근 가능 부서 | 높음 | 낮음 | ⭐⭐⭐⭐⭐ |
| 부서 정보 | 보통 | 낮음 | ⭐⭐⭐ |

### 캐싱하지 않은 데이터

| 데이터 | 이유 |
|--------|------|
| 감사 로그 | 쓰기 전용 (조회 거의 없음) |
| 사용자 목록 | 필터링 조건이 다양함 |
| 통계 데이터 | 실시간 정확성 필요 |

---

## 🚀 성능 측정 예시

### 실제 측정 결과

```
테스트: 사용자 권한 조회 API 1000번 호출

[캐시 없음]
- 평균 응답 시간: 85ms
- 총 시간: 85,000ms (85초)
- DB 쿼리 수: 1000개

[캐시 적용]
- 1회 호출: 85ms (DB 조회 + 캐시 저장)
- 2~1000회 호출: 평균 3ms (캐시 조회)
- 총 시간: 85 + (999 × 3) = 3,082ms (3초)
- DB 쿼리 수: 1개

결과: 27배 성능 향상, DB 부하 99.9% 감소
```

---

## 🛠️ 디버깅 팁

### 캐시 동작 확인 방법

**1. 로그 레벨 설정 (application.yml)**
```yaml
logging:
  level:
    org.springframework.cache: DEBUG
```

**2. 로그 출력 예시**
```
Cache hit: userPermissions::tenant-001::abc-123
Cache miss: userPermissions::tenant-002::def-456
Cache evict: userPermissions::tenant-001::abc-123
```

**3. 수동 캐시 확인 (디버깅용)**
```java
@Autowired
private CacheManager cacheManager;

public void debugCache() {
    Cache cache = cacheManager.getCache("userPermissions");
    Object value = cache.get("userPermissions::tenant-001::abc-123");
    System.out.println("캐시 값: " + value);
}
```

---

## 📝 정리

### @Cacheable 요약
```
✅ 메서드 실행 전 캐시 확인
✅ 캐시 있으면 메서드 실행 SKIP
✅ 캐시 없으면 메서드 실행 후 결과 저장
```

### @CacheEvict 요약
```
✅ 메서드 실행 후 캐시 삭제
✅ 데이터 변경 시 사용
✅ 다음 조회 시 최신 데이터 보장
```

### 핵심 원리
```
1. RAM에 데이터 저장 (빠른 조회)
2. 같은 파라미터 → 같은 캐시 키 → 같은 결과
3. 데이터 변경 시 캐시 삭제 → 다음 조회 시 최신 데이터
```

---

**이제 Spring Cache가 어떻게 동작하는지 명확히 이해되셨나요?** 🎓
