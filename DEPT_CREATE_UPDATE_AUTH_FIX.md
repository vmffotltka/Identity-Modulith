# ✅ 부서 생성/수정 API 권한 검증 추가 완료!

## 🎯 문제

**부서 생성 및 수정 API에서 X-User-Id 헤더를 받지 않고, RBAC 권한 검증을 하지 않았습니다!**

### 기존 상태

| API | X-User-Id 헤더 | RBAC 권한 검증 | 상태 |
|-----|---------------|---------------|------|
| POST `/departments` (생성) | ❌ 없음 | ❌ 없음 | 🔴 문제 |
| PATCH `/departments/{id}` (수정) | ❌ 없음 | ❌ 없음 | 🔴 문제 |
| PUT `/departments/{id}/move` (이동) | ✅ 있음 | ✅ 있음 | ✅ 정상 |
| DELETE `/departments/{id}` (삭제) | ✅ 있음 | ✅ 있음 | ✅ 정상 |

**결과**: TEAM_LEAD나 MEMBER도 부서를 생성/수정할 수 있었음!

---

## ✅ 수정 완료 (3개 파일)

### 1. DepartmentService.java (인터페이스)

**변경 사항**: `actorUserId` 파라미터 추가

```java
// createDepartment
DepartmentDto.Response createDepartment(
    String tenantId,
    UUID actorUserId,        // ✅ 추가
    String name,
    DepartmentType type,
    String code,
    String customTypeName,
    String parentId
);

// updateDepartment
DepartmentDto.Response updateDepartment(
    String tenantId,
    UUID actorUserId,        // ✅ 추가
    String deptId,
    String name,
    DepartmentType type
);
```

---

### 2. DepartmentServiceImpl.java (구현체)

**변경 사항**: RBAC 권한 검증 추가

#### A. createDepartment
```java
@Override
@Transactional
public DepartmentDto.Response createDepartment(
        String tenantId,
        UUID actorUserId,        // ✅ 추가
        String name,
        DepartmentType type,
        String code,
        String customTypeName,
        String parentId) {

    Objects.requireNonNull(actorUserId, "actorUserId는 null일 수 없습니다");

    // ✅ RBAC 권한 검증: org:create 권한 필요
    Set<String> permissions = rbacModuleApi.getPermissionsByAgentId(tenantId, actorUserId.toString());
    if (!permissions.contains("org:create")) {
        log.warn("[ORG] org:create 권한 없음 - userId={}, permissions={}", actorUserId, permissions);
        throw new OrganizationException(
                OrganizationErrorCode.INSUFFICIENT_PERMISSION
        );
    }

    log.info("[ORG] 부서 생성 - tenantId={}, userId={}, name={}, type={}, code={}, parentId={}",
             tenantId, actorUserId, name, type, code, parentId);

    // ...existing code...
}
```

#### B. updateDepartment
```java
@Override
@Transactional
public DepartmentDto.Response updateDepartment(
        String tenantId,
        UUID actorUserId,        // ✅ 추가
        String deptId,
        String name,
        DepartmentType type) {

    Objects.requireNonNull(actorUserId, "actorUserId는 null일 수 없습니다");

    // ✅ RBAC 권한 검증: org:update 권한 필요
    Set<String> permissions = rbacModuleApi.getPermissionsByAgentId(tenantId, actorUserId.toString());
    if (!permissions.contains("org:update")) {
        log.warn("[ORG] org:update 권한 없음 - userId={}, permissions={}", actorUserId, permissions);
        throw new OrganizationException(
                OrganizationErrorCode.INSUFFICIENT_PERMISSION
        );
    }

    log.info("[ORG] 부서 수정 - tenantId={}, userId={}, deptId={}, name={}, type={}",
             tenantId, actorUserId, deptId, name, type);

    // ...existing code...
}
```

---

### 3. DepartmentController.java

**변경 사항**: X-User-Id 헤더 받기

#### A. createDepartment
```java
@PostMapping
public ResponseEntity<DepartmentDto.Response> createDepartment(
        @Parameter(description = "사용자 ID (UUID)", required = true)
        @RequestHeader(value = "X-User-Id") String userIdStr,  // ✅ 추가
        @Valid @RequestBody DepartmentDto.CreateRequest request) {

    String tenantId = TenantContextHolder.getCurrentTenantId();
    UUID userId = UUID.fromString(userIdStr);  // ✅ 추가

    DepartmentDto.Response response = departmentServiceImpl.createDepartment(
            tenantId,
            userId,          // ✅ 추가
            request.getName(),
            request.getType(),
            request.getCode(),
            request.getCustomTypeName(),
            request.getParentId()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

#### B. updateDepartment
```java
@PatchMapping("/{deptId}")
public ResponseEntity<DepartmentDto.Response> updateDepartment(
        @Parameter(description = "사용자 ID (UUID)", required = true)
        @RequestHeader(value = "X-User-Id") String userIdStr,  // ✅ 추가
        @Parameter(description = "부서 ID (UUID)", required = true)
        @PathVariable String deptId,
        @Valid @RequestBody DepartmentDto.UpdateRequest request) {

    String tenantId = TenantContextHolder.getCurrentTenantId();
    UUID userId = UUID.fromString(userIdStr);  // ✅ 추가

    DepartmentDto.Response response = departmentServiceImpl.updateDepartment(
            tenantId,
            userId,          // ✅ 추가
            deptId,
            request.getName(),
            request.getType()
    );

    return ResponseEntity.ok(response);
}
```

---

## 🚀 테스트 방법

### 1. IntelliJ에서 Rebuild

```
Build → Rebuild Project (Ctrl+Shift+F9)
```

---

### 2. 애플리케이션 재시작

```
Run → Stop (Ctrl+F2)
Run → Run (Shift+F10)
```

---

## 🧪 API 테스트

### Test A: ADMIN으로 부서 생성 (성공) ✅

```http
POST /api/org/departments
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json

{
  "name": "DevOps팀",
  "type": "TEAM",
  "code": "DEV-OPS",
  "parentId": "00000000-0000-0000-0000-000000000002"
}
```

**예상 응답**: `201 Created` ✅

**콘솔 로그**:
```
[ORG] 부서 생성 - tenantId=default-tenant, userId=10000000-0000-0000-0000-000000000001, name=DevOps팀
```

---

### Test B: TEAM_LEAD로 부서 생성 (실패) ❌

```http
POST /api/org/departments
X-User-Id: 10000000-0000-0000-0000-000000000002
Content-Type: application/json

{
  "name": "DevOps팀",
  "type": "TEAM",
  "code": "DEV-OPS",
  "parentId": "00000000-0000-0000-0000-000000000002"
}
```

**예상 응답**: `403 Forbidden` ❌

**응답 본문**:
```json
{
  "code": "INSUFFICIENT_PERMISSION",
  "message": "권한이 부족합니다"
}
```

**콘솔 로그**:
```
WARN [ORG] org:create 권한 없음 - userId=10000000-0000-0000-0000-000000000002, 
     permissions=[user:read, org:read, report:view, report:export]
```

---

### Test C: X-User-Id 헤더 누락 (실패) ❌

```http
POST /api/org/departments
Content-Type: application/json

{
  "name": "DevOps팀",
  "type": "TEAM",
  "code": "DEV-OPS"
}
```

**예상 응답**: `400 Bad Request` ❌

**응답 본문**:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Required request header 'X-User-Id' is not present"
}
```

---

### Test D: ADMIN으로 부서 수정 (성공) ✅

```http
PATCH /api/org/departments/00000000-0000-0000-0000-000000000004
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json

{
  "name": "백엔드개발팀"
}
```

**예상 응답**: `200 OK` ✅

---

### Test E: TEAM_LEAD로 부서 수정 (실패) ❌

```http
PATCH /api/org/departments/00000000-0000-0000-0000-000000000004
X-User-Id: 10000000-0000-0000-0000-000000000002
Content-Type: application/json

{
  "name": "백엔드개발팀"
}
```

**예상 응답**: `403 Forbidden` ❌

**콘솔 로그**:
```
WARN [ORG] org:update 권한 없음 - userId=10000000-0000-0000-0000-000000000002, 
     permissions=[user:read, org:read, report:view, report:export]
```

---

### Test F: MEMBER로 부서 수정 (실패) ❌

```http
PATCH /api/org/departments/00000000-0000-0000-0000-000000000004
X-User-Id: 10000000-0000-0000-0000-000000000003
Content-Type: application/json

{
  "name": "백엔드개발팀"
}
```

**예상 응답**: `403 Forbidden` ❌

---

## 📊 최종 API 권한 검증 상태

| API | X-User-Id 헤더 | RBAC 권한 | 필요 권한 | 상태 |
|-----|---------------|----------|----------|------|
| POST `/departments` | ✅ 있음 | ✅ 있음 | `org:create` | ✅ 완료 |
| PATCH `/departments/{id}` | ✅ 있음 | ✅ 있음 | `org:update` | ✅ 완료 |
| PUT `/departments/{id}/move` | ✅ 있음 | ✅ 있음 | `org:update` | ✅ 정상 |
| DELETE `/departments/{id}` | ✅ 있음 | ✅ 있음 | `org:delete` | ✅ 정상 |
| POST `/departments/{id}/deactivate` | ✅ 있음 | ❓ 미확인 | `org:update` | ⚠️ 확인 필요 |
| POST `/departments/{id}/activate` | ✅ 있음 | ❓ 미확인 | `org:update` | ⚠️ 확인 필요 |
| GET `/departments` | ❌ 불필요 | ❌ 불필요 | `org:read` | ✅ 정상 |

---

## 🔐 역할별 부서 관리 권한 (최종)

### ADMIN (전체 권한)
- ✅ **생성** (org:create)
- ✅ **조회** (org:read)
- ✅ **수정** (org:update)
- ✅ **이동** (org:update)
- ✅ **삭제** (org:delete)
- ✅ **비활성화/활성화** (org:update)

### TEAM_LEAD (조회만)
- ❌ 생성
- ✅ **조회** (org:read)
- ❌ 수정
- ❌ 이동
- ❌ 삭제
- ❌ 비활성화/활성화

### MEMBER (조회만)
- ❌ 생성
- ✅ **조회** (org:read)
- ❌ 수정
- ❌ 이동
- ❌ 삭제
- ❌ 비활성화/활성화

---

## 🎯 정리

### ✅ 수정 완료

| 구분 | 수정 내용 |
|------|----------|
| **DepartmentService.java** | actorUserId 파라미터 추가 (2개 메서드) |
| **DepartmentServiceImpl.java** | RBAC 권한 검증 추가 (org:create, org:update) |
| **DepartmentController.java** | X-User-Id 헤더 받기 (2개 메서드) |

---

### 🚀 다음 단계

1. ✅ **IntelliJ에서 Rebuild** (Ctrl+Shift+F9)
2. ✅ **애플리케이션 재시작** (Shift+F10)
3. ✅ **API 테스트**:
   - ADMIN: 생성/수정 성공 ✅
   - TEAM_LEAD: 생성/수정 실패 (403 Forbidden) ❌
   - MEMBER: 생성/수정 실패 (403 Forbidden) ❌
   - X-User-Id 누락: 400 Bad Request ❌

---

## 🎉 완료!

**이제 부서 생성/수정 API가 제대로 권한을 검증합니다!** 🚀

- ✅ X-User-Id 헤더 필수
- ✅ RBAC 권한 검증 (org:create, org:update)
- ✅ ADMIN만 부서 생성/수정 가능
- ✅ TEAM_LEAD, MEMBER는 403 Forbidden

**IntelliJ에서 Rebuild 후 재시작하세요!**

---

**작성일**: 2026-02-08  
**수정 파일**: 3개 (DepartmentService, DepartmentServiceImpl, DepartmentController)  
**핵심 수정**: 부서 생성/수정 API에 X-User-Id 헤더 및 RBAC 권한 검증 추가

