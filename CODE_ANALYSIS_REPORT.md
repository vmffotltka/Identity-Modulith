# 🔍 코드 심층 분석 보고서

**날짜**: 2026-01-19  
**대상**: Organization & RBAC 모듈  
**분석 수준**: 코드 레벨 + 아키텍처

---

## 📋 목차
1. [조직 모듈 분석](#조직-모듈-분석)
2. [RBAC 모듈 분석](#rbac-모듈-분석)
3. [공통 개선 사항](#공통-개선-사항)
4. [우선순위별 개선 계획](#우선순위별-개선-계획)

---

## 🏢 조직 모듈 분석

### 1. 코드 레벨 개선점

#### 🔴 P0: 트랜잭션 범위 문제
**현재 구조**:
```java
@Service
@Transactional(readOnly = true)  // 클래스 레벨
public class DepartmentService {
    
    @Transactional  // 메서드 레벨
    public Department createDepartment(...) {
        // 비즈니스 로직
    }
}
```

**문제점**:
1. **readOnly=true가 기본** → 쓰기 작업에서 명시적으로 override 필요
2. **혼란 가능성**: 새로운 메서드 추가 시 @Transactional 누락 위험

**개선 방안**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
// ✅ 클래스 레벨 트랜잭션 제거, 메서드별로 명시적 설정
public class DepartmentService {
    
    @Transactional  // 쓰기 작업
    public Department createDepartment(...) { }
    
    @Transactional(readOnly = true)  // 읽기 작업
    public List<Department> getAllDepartments(...) { }
}
```

**효과**:
- ✅ 명확성 향상
- ✅ 실수 방지
- ✅ 트랜잭션 의도 명확화

---

#### 🟡 P1: 메서드 복잡도 과다

**문제 메서드**: `getOrganizationTree()` (Line 543-696, 153줄)

**현재 구조**:
```java
public DepartmentDto.TreeResponse getOrganizationTree(...) {
    // 1. 접근 가능 부서 ID 조회 (20줄)
    // 2. 부서 조회 및 필터링 (30줄)
    // 3. Map 변환 (20줄)
    // 4. 트리 구조 생성 (50줄)
    // 5. 정렬 및 반환 (30줄)
    return tree;  // 총 150줄+
}
```

**문제점**:
1. **단일 책임 원칙(SRP) 위반**: 여러 책임이 섞임
2. **테스트 어려움**: 각 로직 단위 테스트 불가
3. **가독성 저하**: 로직 흐름 파악 어려움

**개선 방안**:
```java
public DepartmentDto.TreeResponse getOrganizationTree(...) {
    // 1. 접근 가능 부서 조회
    Set<String> accessibleDeptIds = getAccessibleDepartmentIds(tenantId, userId);
    
    // 2. 부서 데이터 로드
    List<Department> departments = loadDepartments(tenantId, accessibleDeptIds);
    
    // 3. DTO 변환
    List<DepartmentDto.Response> dtoList = convertToDtoList(departments);
    
    // 4. 트리 구조 생성
    List<DepartmentDto.Response> tree = buildTree(dtoList);
    
    // 5. 응답 생성
    return createTreeResponse(tree);
}

// ✅ 각 단계를 private 메서드로 분리
private Set<String> getAccessibleDepartmentIds(...) { }
private List<Department> loadDepartments(...) { }
private List<DepartmentDto.Response> convertToDtoList(...) { }
private List<DepartmentDto.Response> buildTree(...) { }
private TreeResponse createTreeResponse(...) { }
```

**효과**:
- ✅ 각 단계별 단위 테스트 가능
- ✅ 가독성 향상
- ✅ 유지보수 용이

---

#### 🟡 P1: 순환 참조 검증 로직 개선

**현재 구조** (Line 329-365):
```java
public void moveDepartment(...) {
    // ...검증 로직
    if (newParent.getOrgPath().startsWith(target.getOrgPath())) {
        throw new OrganizationException(CIRCULAR_REFERENCE);
    }
    // ...이동 로직 (100줄+)
}
```

**문제점**:
1. **검증과 실행이 혼재**: 트랜잭션 범위 비대화
2. **재사용 불가**: 다른 곳에서 순환 참조 검증 불가

**개선 방안**:
```java
// ✅ 검증 로직 분리
@Transactional(readOnly = true)
public void validateMoveDepartment(String deptId, String newParentId) {
    Department target = findDepartment(deptId);
    Department newParent = findDepartment(newParentId);
    
    // 순환 참조 검증
    if (isCircularReference(target, newParent)) {
        throw new OrganizationException(CIRCULAR_REFERENCE);
    }
}

// ✅ 실행 로직 분리
@Transactional
public void executeMoveDepartment(String deptId, String newParentId) {
    // 실제 이동 로직
}

// ✅ 검증 메서드 추출
private boolean isCircularReference(Department target, Department newParent) {
    return newParent.getOrgPath().startsWith(target.getOrgPath());
}
```

**효과**:
- ✅ 트랜잭션 범위 최소화
- ✅ 검증 로직 재사용 가능
- ✅ 테스트 용이

---

#### 🟢 P2: 매직 넘버 제거

**현재 코드**:
```java
if (dept.getDepth() > 5) {  // ❌ 매직 넘버
    throw new OrganizationException(DEPTH_EXCEEDED);
}

if (dept.getName().length() > 50) {  // ❌ 매직 넘버
    throw new OrganizationException(NAME_TOO_LONG);
}
```

**개선 방안**:
```java
// ✅ 상수 클래스 생성
public final class OrganizationConstants {
    public static final int MAX_DEPARTMENT_DEPTH = 5;
    public static final int MAX_DEPARTMENT_NAME_LENGTH = 50;
    public static final int MAX_ORG_PATH_LENGTH = 500;
    
    private OrganizationConstants() {}
}

// ✅ 사용
if (dept.getDepth() > OrganizationConstants.MAX_DEPARTMENT_DEPTH) {
    throw new OrganizationException(DEPTH_EXCEEDED);
}
```

---

### 2. 구조적 개선점

#### 🟡 P1: Repository 메서드 이름 개선

**현재**:
```java
List<Department> findByTenantIdAndOrgPathStartingWith(String tenantId, String orgPath);
```

**문제점**: 이름이 너무 길고 Spring Data JPA 자동 생성에 의존

**개선 방안**:
```java
@Query("""
    SELECT d FROM Department d
    WHERE d.tenantId = :tenantId
      AND d.orgPath LIKE CONCAT(:orgPath, '%')
    ORDER BY d.orgPath
""")
List<Department> findSubtree(@Param("tenantId") String tenantId,
                               @Param("orgPath") String orgPath);
```

**효과**:
- ✅ 의도 명확화
- ✅ 쿼리 최적화 가능
- ✅ 가독성 향상

---

#### 🟢 P2: DTO 변환 로직 분리

**현재**: Service 계층에 DTO 변환 로직 산재

**개선 방안**:
```java
// ✅ Mapper 클래스 생성
@Component
public class DepartmentMapper {
    
    public DepartmentDto.Response toDto(Department department) {
        return DepartmentDto.Response.builder()
            .deptId(department.id())
            .name(department.name())
            .type(department.type())
            .orgPath(department.orgPath())
            .depth(department.depth())
            .parentId(department.parent() != null ? 
                department.parent().id() : null)
            .build();
    }
    
    public List<DepartmentDto.Response> toDtoList(List<Department> departments) {
        return departments.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
}
```

**효과**:
- ✅ Service 로직 간결화
- ✅ 변환 로직 재사용
- ✅ 테스트 용이

---

## 🔐 RBAC 모듈 분석

### 1. 코드 레벨 개선점

#### 🔴 P0: 동시성 제어 부재

**현재 구조**:
```java
@Transactional
public void assignRoleToAgent(String agentId, String roleName) {
    // 1. 역할 조회
    RoleJpaEntity role = roleRepository.findByTenantIdAndName(...);
    
    // 2. 중복 체크
    if (agentRoleRepository.existsByAgentIdAndRoleId(...)) {
        throw new RbacException(ALREADY_ASSIGNED);
    }
    
    // 3. 할당
    agentRoleRepository.save(...);
}
```

**문제점**:
1. **Race Condition**: 동시 요청 시 중복 체크를 통과할 수 있음
2. **DB 제약 위반 가능**: UNIQUE 제약 위반 시 500 에러

**개선 방안**:
```java
@Transactional
public void assignRoleToAgent(String agentId, String roleName) {
    try {
        // ✅ DB UNIQUE 제약에 의존
        agentRoleRepository.save(...);
        
    } catch (DataIntegrityViolationException e) {
        // ✅ 중복 시 명확한 예외
        throw new RbacException(ALREADY_ASSIGNED);
    }
}

// 또는 Pessimistic Lock 사용
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT ar FROM AgentRoleJpaEntity ar WHERE ar.agentId = :agentId")
Optional<AgentRoleJpaEntity> findByAgentIdForUpdate(@Param("agentId") String agentId);
```

**효과**:
- ✅ Race Condition 방지
- ✅ 동시성 안전성 확보

---

#### 🟡 P1: 권한 검증 로직 중복

**현재**: 여러 곳에 산재된 권한 검증

**문제 코드**:
```java
// RbacManagementServiceImpl
public void assignRoleToAgent(...) {
    // ❌ 로깅 없음
    if (!hasPermission(userId, "role:assign")) {
        throw new RbacException(ACCESS_DENIED);
    }
}

public void revokeRoleFromAgent(...) {
    // ❌ 로깅 없음
    if (!hasPermission(userId, "role:revoke")) {
        throw new RbacException(ACCESS_DENIED);
    }
}
```

**개선 방안**:
```java
// ✅ AOP로 권한 검증 추출
@Aspect
@Component
@Slf4j
public class RbacAuthorizationAspect {
    
    @Before("@annotation(requiresPermission)")
    public void checkPermission(JoinPoint joinPoint, 
                                 RequiresPermission requiresPermission) {
        String userId = getCurrentUserId();
        String permission = requiresPermission.value();
        
        // ✅ P0-4: 권한 검증 로깅
        log.info("[RBAC 권한 검증] userId={}, permission={}, method={}", 
            userId, permission, joinPoint.getSignature().getName());
        
        if (!rbacQueryService.hasPermission(userId, permission)) {
            log.warn("[RBAC 권한 거부] userId={}, permission={}", userId, permission);
            throw new RbacException(ACCESS_DENIED);
        }
        
        log.debug("[RBAC 권한 승인] userId={}, permission={}", userId, permission);
    }
}

// ✅ 사용
@RequiresPermission("role:assign")
public void assignRoleToAgent(...) {
    // 비즈니스 로직만 집중
}
```

**효과**:
- ✅ 중복 코드 제거
- ✅ 로깅 일관성
- ✅ 권한 검증 추적 용이

---

#### 🟡 P1: 캐시 키 전략 개선

**현재 구조**:
```java
@Cacheable(value = "userPermissions", key = "#tenantId + ':' + #userId")
public Set<String> permissionsOf(String tenantId, String userId) {
    // ...
}
```

**문제점**:
1. **문자열 연결**: 가독성 저하, 오타 위험
2. **일관성 부족**: 각 메서드마다 다른 키 생성 방식

**개선 방안**:
```java
// ✅ 캐시 키 생성 유틸 클래스
public class CacheKeyGenerator {
    private static final String DELIMITER = "::";
    
    public static String userPermissions(String tenantId, String userId) {
        return "userPermissions" + DELIMITER + tenantId + DELIMITER + userId;
    }
    
    public static String rolePermissions(String tenantId, String roleName) {
        return "rolePermissions" + DELIMITER + tenantId + DELIMITER + roleName;
    }
}

// ✅ 사용
@Cacheable(value = "userPermissions", 
           keyGenerator = "cacheKeyGenerator")
public Set<String> permissionsOf(String tenantId, String userId) {
    // ...
}

@Component("cacheKeyGenerator")
public class RbacCacheKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (method.getName().equals("permissionsOf")) {
            return CacheKeyGenerator.userPermissions(
                (String) params[0], (String) params[1]);
        }
        // ...
    }
}
```

**효과**:
- ✅ 키 생성 로직 중앙화
- ✅ 오타 방지
- ✅ 테스트 용이

---

#### 🟢 P2: 예외 메시지 국제화

**현재**:
```java
public enum RbacErrorCode {
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "역할을 찾을 수 없습니다"),
    PERMISSION_DENIED("PERMISSION_DENIED", "권한이 없습니다")
}
```

**문제점**: 하드코딩된 한글 메시지

**개선 방안**:
```java
// ✅ messages.properties
rbac.error.role_not_found=역할을 찾을 수 없습니다
rbac.error.permission_denied=권한이 없습니다

// ✅ messages_en.properties
rbac.error.role_not_found=Role not found
rbac.error.permission_denied=Permission denied

// ✅ ErrorCode
public enum RbacErrorCode {
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "rbac.error.role_not_found"),
    PERMISSION_DENIED("PERMISSION_DENIED", "rbac.error.permission_denied");
    
    private final String code;
    private final String messageKey;  // i18n 키
}
```

---

### 2. 구조적 개선점

#### 🟡 P1: Command/Query 분리 (CQRS 도입)

**현재 구조**: RbacManagementService가 Read/Write 모두 처리

**개선 방안**:
```java
// ✅ Command (쓰기)
@Service
@Transactional
public class RbacCommandService {
    public void createRole(...) { }
    public void assignRoleToAgent(...) { }
    public void revokeRoleFromAgent(...) { }
}

// ✅ Query (읽기)
@Service
@Transactional(readOnly = true)
public class RbacQueryService {
    @Cacheable
    public Set<String> getUserPermissions(...) { }
    
    @Cacheable
    public List<RoleDto> getAllRoles(...) { }
}
```

**효과**:
- ✅ 책임 명확화
- ✅ 캐싱 전략 명확화
- ✅ 읽기 성능 최적화

---

#### 🟢 P2: 이벤트 기반 캐시 무효화

**현재**: @CacheEvict 수동 관리

**개선 방안**:
```java
// ✅ 도메인 이벤트 발행
@Service
public class RbacCommandService {
    private final ApplicationEventPublisher eventPublisher;
    
    public void assignRoleToAgent(...) {
        // 비즈니스 로직
        agentRoleRepository.save(...);
        
        // ✅ 이벤트 발행
        eventPublisher.publishEvent(
            new RoleAssignedEvent(agentId, roleId, tenantId)
        );
    }
}

// ✅ 이벤트 리스너에서 캐시 무효화
@Component
public class RbacCacheInvalidationListener {
    private final CacheManager cacheManager;
    
    @EventListener
    public void onRoleAssigned(RoleAssignedEvent event) {
        // ✅ 캐시 무효화 로직 중앙화
        Cache cache = cacheManager.getCache("userPermissions");
        cache.evict(event.getTenantId() + ":" + event.getAgentId());
        
        log.info("[캐시 무효화] agentId={}, reason=RoleAssigned", 
            event.getAgentId());
    }
}
```

**효과**:
- ✅ 캐시 무효화 로직 중앙화
- ✅ 느슨한 결합
- ✅ 확장 용이

---

## 🔄 공통 개선 사항

### 1. 예외 처리 전략 통일

**현재**: 각 모듈마다 다른 예외 처리 방식

**개선 방안**:
```java
// ✅ 공통 예외 계층
public abstract class BusinessException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    protected BusinessException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = status;
    }
}

// ✅ 모듈별 예외
public class RbacException extends BusinessException {
    public RbacException(RbacErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus());
    }
}

public class OrganizationException extends BusinessException {
    public OrganizationException(OrgErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus());
    }
}
```

---

### 2. 로깅 전략 표준화

**현재**: 일관성 없는 로그 레벨 및 형식

**개선 방안**:
```java
// ✅ 로깅 가이드
public class LoggingTemplate {
    
    // ✅ 비즈니스 로직 시작
    public static void logBusinessStart(Logger log, String operation, Object... params) {
        log.info("[{}] 시작 - {}", operation, formatParams(params));
    }
    
    // ✅ 비즈니스 로직 완료
    public static void logBusinessSuccess(Logger log, String operation, Object... params) {
        log.info("[{}] 완료 - {}", operation, formatParams(params));
    }
    
    // ✅ 비즈니스 로직 실패
    public static void logBusinessError(Logger log, String operation, Exception e) {
        log.error("[{}] 실패 - {}", operation, e.getMessage(), e);
    }
    
    // ✅ 권한 검증
    public static void logPermissionCheck(Logger log, String userId, String permission, boolean granted) {
        if (granted) {
            log.debug("[권한 승인] userId={}, permission={}", userId, permission);
        } else {
            log.warn("[권한 거부] userId={}, permission={}", userId, permission);
        }
    }
}
```

---

### 3. 테스트 커버리지 전략

**현재**: 일부 테스트만 존재

**개선 방안**:
```
✅ 단위 테스트 (Unit Test)
  - Service 계층: 비즈니스 로직 검증
  - Repository 계층: 쿼리 검증
  - Mapper/Converter: 변환 로직 검증

✅ 통합 테스트 (Integration Test)
  - Controller → Service → Repository 전체 흐름
  - 트랜잭션 롤백 검증
  - 캐시 동작 검증

✅ E2E 테스트 (추후)
  - Testcontainers + PostgreSQL
  - 실제 HTTP 요청 시나리오
```

---

## 📊 우선순위별 개선 계획

### 🔴 P0 (즉시 적용 - 보안/안정성)

| 구분 | 개선 항목 | 예상 시간 | 효과 |
|------|-----------|-----------|------|
| RBAC | 동시성 제어 추가 | 30분 | 중복 할당 방지 |
| RBAC | 권한 검증 AOP | 1시간 | 로깅 일관성 |
| Org | 트랜잭션 명시적 설정 | 20분 | 의도 명확화 |

**총 예상 시간**: 1시간 50분

---

### 🟡 P1 (단기 - 코드 품질)

| 구분 | 개선 항목 | 예상 시간 | 효과 |
|------|-----------|-----------|------|
| Org | 복잡한 메서드 분리 | 1시간 | 가독성 향상 |
| Org | 순환 참조 검증 분리 | 30분 | 재사용성 향상 |
| RBAC | 캐시 키 전략 개선 | 45분 | 일관성 향상 |
| RBAC | Command/Query 분리 | 2시간 | 책임 명확화 |

**총 예상 시간**: 4시간 15분

---

### 🟢 P2 (중기 - 리팩토링)

| 구분 | 개선 항목 | 예상 시간 | 효과 |
|------|-----------|-----------|------|
| Org | 매직 넘버 제거 | 30분 | 유지보수성 |
| Org | DTO 변환 Mapper 분리 | 1시간 | 재사용성 |
| RBAC | 예외 메시지 국제화 | 1시간 | 확장성 |
| RBAC | 이벤트 기반 캐시 무효화 | 2시간 | 느슨한 결합 |
| 공통 | 로깅 전략 표준화 | 1시간 | 일관성 |

**총 예상 시간**: 5시간 30분

---

## 🎯 실행 가이드

### Step 1: P0 개선 (1시간 50분)
```bash
1. RbacManagementServiceImpl - 동시성 제어
2. RbacAuthorizationAspect 생성
3. DepartmentService 트랜잭션 명시화
```

### Step 2: P1 개선 (4시간 15분)
```bash
1. DepartmentService 메서드 분리
2. RbacCommandService/QueryService 분리
3. CacheKeyGenerator 생성
```

### Step 3: P2 개선 (5시간 30분)
```bash
1. OrganizationConstants 생성
2. DepartmentMapper 생성
3. LoggingTemplate 생성
```

---

## 📈 기대 효과

### 코드 품질
- **Before**: 복잡도 높음, 중복 많음
- **After**: 단일 책임, 재사용성 향상

### 안정성
- **Before**: Race Condition 가능
- **After**: 동시성 안전

### 유지보수성
- **Before**: 로직 분산, 파악 어려움
- **After**: 명확한 구조, 테스트 용이

---

**분석 완료**: 2026-01-19  
**다음 단계**: 우선순위에 따라 개선 진행

더 자세한 분석이 필요한 부분이 있으면 말씀해주세요!

