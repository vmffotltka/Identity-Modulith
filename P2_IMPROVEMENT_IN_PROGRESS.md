# 🟢 P2 코드 품질 개선 진행 중

**날짜**: 2026-01-19  
**예상 시간**: 2시간 30분  
**우선순위**: P2 (중간)

---

## 🎯 P2 개선 목표

### 코드 품질 향상
1. ✅ **매직 넘버 제거** (30분) - 상수 클래스 생성
2. ✅ **DTO Mapper 분리** (1시간) - Service 계층 단순화
3. ✅ **로깅 전략 표준화** (1시간) - 일관된 로그 포맷

---

## 📋 P2-1: 매직 넘버 제거 (30분)

### 현재 문제점
```java
// ❌ 하드코딩된 숫자들
if (dept.getDepth() > 5) { }
if (dept.getName().length() > 50) { }
if (orgPath.length() > 500) { }
```

### 개선 방안
```java
// ✅ 상수 클래스 생성
public final class OrganizationConstants {
    public static final int MAX_DEPARTMENT_DEPTH = 5;
    public static final int MAX_DEPARTMENT_NAME_LENGTH = 50;
    public static final int MAX_ORG_PATH_LENGTH = 500;
}

public final class RbacConstants {
    public static final int MAX_ROLE_NAME_LENGTH = 64;
    public static final int MAX_PERMISSION_CODE_LENGTH = 128;
}
```

---

## 📋 P2-2: DTO Mapper 분리 (1시간)

### 현재 문제점
```java
// ❌ Service에 DTO 변환 로직 산재
public List<DepartmentDto.Response> getDepartments(...) {
    List<Department> depts = repository.findAll(...);
    return depts.stream()
        .map(d -> DepartmentDto.Response.builder()
            .deptId(d.id())
            .name(d.name())
            // ... 10줄 이상의 변환 로직
        )
        .toList();
}
```

### 개선 방안
```java
// ✅ Mapper 클래스 생성
@Component
public class DepartmentMapper {
    public DepartmentDto.Response toDto(Department dept) { }
    public List<DepartmentDto.Response> toDtoList(List<Department> depts) { }
}

// ✅ Service 간결화
public List<DepartmentDto.Response> getDepartments(...) {
    List<Department> depts = repository.findAll(...);
    return departmentMapper.toDtoList(depts);  // 한 줄!
}
```

---

## 📋 P2-3: 로깅 전략 표준화 (1시간)

### 현재 문제점
```java
// ❌ 비일관적인 로그 포맷
log.info("부서 생성: {}", name);
log.debug("[ORG] 부서 조회 - tenantId: {}", tenantId);
log.warn("권한 없음");
```

### 개선 방안
```java
// ✅ 표준 포맷 정의
log.info("[ORG] 부서 생성 성공: tenantId={}, deptId={}, name={}", ...);
log.debug("[ORG] 부서 조회 시작: tenantId={}, userId={}", ...);
log.warn("[ORG] 권한 부족: userId={}, requiredPermission={}", ...);
log.error("[ORG] 부서 생성 실패: tenantId={}, error={}", ..., e);

// 포맷 규칙:
// [모듈명] 동작 상태: key1=value1, key2=value2
```

---

## 🚀 진행 순서

1. **P2-1: 매직 넘버 제거** (30분)
   - OrganizationConstants.java 생성
   - RbacConstants.java 생성
   - 기존 하드코딩 수정

2. **P2-2: DTO Mapper 분리** (1시간)
   - DepartmentMapper.java 생성
   - RbacMapper.java 생성
   - Service 계층 리팩토링

3. **P2-3: 로깅 전략 표준화** (1시간)
   - 로깅 가이드 문서화
   - 기존 로그 표준화

---

**시작합니다!** 🚀

