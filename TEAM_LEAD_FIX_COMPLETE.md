# ✅ TEAM_LEAD 부서 이동 차단 완료!

## 🎯 수정 내용

### 문제
- TEAM_LEAD가 부서 이동(`moveDepartment`)을 할 수 있었음
- 문서(`RBAC_SCENARIOS.md`)에서는 ADMIN만 `dept:move` 가능

### 원인
- **코드가 RBAC 권한을 확인하지 않음**
- DataScopeLevel만 확인 (접근 가능한 부서인지만 체크)

---

## ✅ 수정 완료

### 1. SQL 수정 (3개 파일)

**TEAM_LEAD 권한에서 제거**:
- ❌ `user:update` (사용자 수정)

**TEAM_LEAD가 가진 권한 (최종)**:
```sql
-- reset_database.sql, V1_0_0, V2_0_0
INSERT INTO rbac_role_permissions (role_id, permission_id, ...)
VALUES
    (team_lead_role_id, perm_user_read, ...),      -- 조회
    (team_lead_role_id, perm_org_read, ...),       -- 조회
    (team_lead_role_id, perm_report_view, ...),    -- 조회
    (team_lead_role_id, perm_report_export, ...);  -- 내보내기
```

### 2. Java 코드 수정

#### A. RbacModuleApi.java
```java
// 메서드 추가
Set<String> getPermissionsByAgentId(String tenantId, String agentId);
```

#### B. RbacManagementServiceImpl.java
```java
// RbacQueryService 주입
private final RbacQueryService rbacQueryService;

// 메서드 구현
@Override
public Set<String> getPermissionsByAgentId(String tenantId, String agentId) {
    UUID agentUuid = UUID.fromString(agentId);
    Set<String> permissions = rbacQueryService.permissionsOf(tenantId, agentUuid);
    return permissions;
}
```

#### C. DepartmentServiceImpl.java
```java
// RbacModuleApi 주입
private final RbacModuleApi rbacModuleApi;

// validateMoveDepartment에 권한 검증 추가
private void validateMoveDepartment(...) {
    // ...
    
    // RBAC 권한 검증: org:update 권한 필요
    Set<String> permissions = rbacModuleApi.getPermissionsByAgentId(tenantId, actorUserId.toString());
    if (!permissions.contains("org:update")) {
        log.warn("[ORG] org:update 권한 없음 - userId={}, permissions={}", actorUserId, permissions);
        throw new OrganizationException(INSUFFICIENT_PERMISSION);
    }
    
    // DataScope 권한 검증: 접근 가능한 부서인지
    Set<String> accessibleDeptIds = getAccessibleDepartmentIds(...);
    // ...
}
```

---

## 🚀 테스트 방법

### 1. 애플리케이션 재시작

```bash
# IntelliJ에서 Stop → Run
# 또는
./gradlew bootRun
```

---

### 2. Scenario A: ADMIN으로 부서 이동 (성공) ✅

```http
PUT http://localhost:8080/api/org/departments/00000000-0000-0000-0000-000000000004/move
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json

{
  "newParentId": "00000000-0000-0000-0000-000000000003"
}
```

**예상 결과**: `204 No Content` ✅

**콘솔 로그**:
```
[ORG] 부서 이동 완료 - deptId=00000000-0000-0000-0000-000000000004
```

---

### 3. Scenario B: TEAM_LEAD로 부서 이동 (실패) ❌

```http
PUT http://localhost:8080/api/org/departments/00000000-0000-0000-0000-000000000004/move
X-User-Id: 10000000-0000-0000-0000-000000000002
Content-Type: application/json

{
  "newParentId": "00000000-0000-0000-0000-000000000003"
}
```

**예상 결과**: `403 Forbidden` ❌

**응답 본문**:
```json
{
  "code": "INSUFFICIENT_PERMISSION",
  "message": "권한이 부족합니다"
}
```

**콘솔 로그**:
```
WARN [ORG] org:update 권한 없음 - userId=10000000-0000-0000-0000-000000000002, permissions=[user:read, org:read, report:view, report:export]
```

---

## 📊 권한 검증 흐름

### 이전 (문제)
```
사용자 요청
    ↓
DataScopeLevel만 확인 (접근 가능한 부서?)
    ↓
✅ TEAM_LEAD가 자신의 부서에 접근 가능
    ↓
부서 이동 성공 (잘못됨!) ❌
```

### 수정 후 (올바름)
```
사용자 요청
    ↓
1. RBAC 권한 확인 (org:update 있는가?)
    ├─ ADMIN: ✅ 있음 → 다음 단계
    └─ TEAM_LEAD: ❌ 없음 → 403 Forbidden
    ↓
2. DataScopeLevel 확인 (접근 가능한 부서?)
    ↓
3. 비즈니스 로직 검증 (순환 참조 등)
    ↓
4. 부서 이동 실행
```

---

## 📋 역할별 권한 (최종)

### ADMIN
| 작업 | 권한 | 가능 여부 |
|------|------|----------|
| 부서 조회 | org:read | ✅ |
| 부서 생성 | org:create | ✅ |
| 부서 수정 | org:update | ✅ |
| 부서 이동 | org:update | ✅ |
| 부서 삭제 | org:delete | ✅ |

### TEAM_LEAD
| 작업 | 권한 | 가능 여부 |
|------|------|----------|
| 부서 조회 | org:read | ✅ |
| 부서 생성 | org:create | ❌ 없음 |
| 부서 수정 | org:update | ❌ 없음 |
| 부서 이동 | org:update | ❌ 없음 |
| 부서 삭제 | org:delete | ❌ 없음 |

### MEMBER
| 작업 | 권한 | 가능 여부 |
|------|------|----------|
| 부서 조회 | org:read | ✅ |
| 부서 생성 | org:create | ❌ 없음 |
| 부서 수정 | org:update | ❌ 없음 |
| 부서 이동 | org:update | ❌ 없음 |
| 부서 삭제 | org:delete | ❌ 없음 |

---

## 🎯 적용된 보안 정책

### 2단계 권한 검증

**1단계: RBAC 권한 확인**
- `org:update` 권한이 있는가?
- ADMIN만 통과

**2단계: DataScope 확인**
- 접근 가능한 부서인가?
- DataScopeLevel에 따라 결정

**둘 다 통과해야 작업 수행 가능!**

---

## 🔥 추가로 수정 권장

동일한 권한 검증을 다른 메서드에도 추가:

| 메서드 | 필요 권한 | 수정 필요 |
|--------|----------|----------|
| `createDepartment()` | `org:create` | ⚠️ 권장 |
| `updateDepartment()` | `org:update` | ⚠️ 권장 |
| `deleteDepartment()` | `org:delete` | ⚠️ 권장 |
| `deactivateDepartment()` | `org:update` | ⚠️ 권장 |
| `activateDepartment()` | `org:update` | ⚠️ 권장 |

---

## 🎉 완료!

- ✅ RbacModuleApi에 권한 조회 메서드 추가
- ✅ RbacManagementServiceImpl에 구현
- ✅ DepartmentServiceImpl에 RBAC 권한 검증 추가
- ✅ TEAM_LEAD의 부서 이동 차단
- ✅ 문서(`RBAC_SCENARIOS.md`)와 일치

**이제 TEAM_LEAD는 부서 이동을 할 수 없습니다!** 🚀

---

**작성일**: 2026-02-07  
**수정 파일**: RbacModuleApi, RbacManagementServiceImpl, DepartmentServiceImpl  
**기준 문서**: `Docs/RBAC_SCENARIOS.md`

