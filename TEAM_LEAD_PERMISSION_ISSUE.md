# ⚠️ TEAM_LEAD 권한 문제 및 해결 방안

## 🎯 문제 상황

**현상**: TEAM_LEAD가 부서 이동(`moveDepartment`)을 할 수 있음
**예상**: ADMIN만 부서 이동 가능해야 함

---

## 🔍 원인 분석

### 현재 권한 검증 로직
```java
// DepartmentServiceImpl.validateMoveDepartment()
Set<String> accessibleDeptIds = getAccessibleDepartmentIds(tenantId, actorUserId);
if (!accessibleDeptIds.contains(deptId)) {
    throw new OrganizationException(OrganizationErrorCode.INSUFFICIENT_PERMISSION);
}
```

**문제점**:
- ✅ DataScopeLevel만 확인 (접근 가능한 부서인지)
- ❌ **실제 RBAC 권한(org:update) 확인 안 함**

### 결과
- TEAM_LEAD가 자신의 부서에 **접근은 가능**하므로 (`TEAM_LEAD` DataScopeLevel)
- 부서 이동도 가능하게 됨 ❌

---

## ✅ SQL 수정 완료 (1차 해결)

### 수정된 파일 (3개)
1. ✅ `reset_database.sql`
2. ✅ `V1_0_0__Complete_Schema_With_Code.sql`
3. ✅ `V2_0_0__Fixed_Schema.sql`

### 변경 내용

**TEAM_LEAD 권한에서 제거**:
- ❌ `user:update` (사용자 수정 권한 제거)

**TEAM_LEAD가 가진 권한 (최종)**:
```sql
INSERT INTO rbac_role_permissions (role_id, permission_id, ...)
VALUES
    (team_lead_role_id, perm_user_read, ...),      -- 사용자 조회
    (team_lead_role_id, perm_org_read, ...),       -- 조직 조회
    (team_lead_role_id, perm_report_view, ...),    -- 보고서 조회
    (team_lead_role_id, perm_report_export, ...);  -- 보고서 내보내기
```

**TEAM_LEAD가 없는 권한**:
- ❌ `user:create` (사용자 생성)
- ❌ `user:update` (사용자 수정)
- ❌ `user:delete` (사용자 삭제)
- ❌ `org:create` (조직 생성)
- ❌ `org:update` (조직 수정)
- ❌ `rbac:manage` (RBAC 관리)

---

## ⚠️ 근본적 해결을 위한 코드 수정 필요

### 문제: 권한 확인 로직 누락

현재 코드는 **DataScopeLevel만 확인**하고 **RBAC 권한은 확인하지 않습니다**.

### 필요한 코드 수정 (참고용)

**DepartmentServiceImpl.validateMoveDepartment()**에 추가 필요:

```java
// 권한 검증 추가 필요
private void validateMoveDepartment(...) {
    // 기존: DataScopeLevel 확인
    Set<String> accessibleDeptIds = getAccessibleDepartmentIds(tenantId, actorUserId);
    if (!accessibleDeptIds.contains(deptId)) {
        throw new OrganizationException(OrganizationErrorCode.INSUFFICIENT_PERMISSION);
    }
    
    // 추가 필요: RBAC 권한 확인
    if (!rbacService.hasPermission(actorUserId, "org:update")) {
        log.warn("[ORG] org:update 권한 없음 - userId={}", actorUserId);
        throw new OrganizationException(OrganizationErrorCode.INSUFFICIENT_PERMISSION);
    }
}
```

**동일하게 수정 필요한 메서드**:
- `createDepartment()` → `org:create` 권한 확인
- `updateDepartment()` → `org:update` 권한 확인
- `deleteDepartment()` → `org:delete` 권한 확인
- `deactivateDepartment()` → `org:update` 권한 확인
- `activateDepartment()` → `org:update` 권한 확인

---

## 📊 현재 상태

### ✅ SQL 수정으로 해결된 부분
- TEAM_LEAD에게 `org:update`, `user:update` 권한 없음
- 권한 확인 로직이 **추가되면** 자동으로 차단됨

### ❌ 아직 해결되지 않은 부분
- 코드에서 RBAC 권한을 확인하지 않음
- DataScopeLevel만 확인하므로 TEAM_LEAD가 여전히 부서 이동 가능

---

## 🚀 임시 해결책 (SQL만 사용)

### 방법: TEAM_LEAD의 DataScopeLevel을 MEMBER로 낮춤

**문제점**:
- TEAM_LEAD가 하위 부서를 조회하지 못함
- 원래 의도와 다름

**권장하지 않음** ❌

---

## 💡 최선의 해결책

### 1. SQL 수정 (완료) ✅
```sql
-- TEAM_LEAD에게 org:update 권한 없음
```

### 2. 코드 수정 필요 (미완료)
```java
// DepartmentServiceImpl에 RBAC 권한 확인 추가
if (!rbacService.hasPermission(actorUserId, "org:update")) {
    throw new OrganizationException(INSUFFICIENT_PERMISSION);
}
```

---

## 📋 테스트 시나리오 (현재 상태)

### Scenario A: ADMIN으로 부서 이동
```http
PUT /api/org/departments/{deptId}/move
X-User-Id: 10000000-0000-0000-0000-000000000001  (ADMIN)

{
  "newParentId": "..."
}
```
**결과**: ✅ **성공** (ADMIN은 모든 권한 보유)

---

### Scenario B: TEAM_LEAD로 부서 이동
```http
PUT /api/org/departments/00000000-0000-0000-0000-000000000004/move
X-User-Id: 10000000-0000-0000-0000-000000000002  (TEAM_LEAD)

{
  "newParentId": "00000000-0000-0000-0000-000000000003"
}
```

**현재 결과**: ✅ **성공** (DataScopeLevel만 확인하므로)
**예상 결과**: ❌ **실패** (org:update 권한 없으므로 차단되어야 함)

**문제**: 코드에서 RBAC 권한을 확인하지 않음

---

## 🔧 임시 우회 방법

### 데이터베이스에서 직접 TEAM_LEAD의 부서 접근 제한

```sql
-- TEAM_LEAD의 DataScopeLevel을 MEMBER로 변경 (임시)
UPDATE rbac_roles 
SET data_scope_level = 'MEMBER' 
WHERE name = 'TEAM_LEAD';

-- 단, 이렇게 하면 TEAM_LEAD가 하위 부서를 조회하지 못함
```

**권장하지 않음**: 원래 의도와 다름

---

## 📝 결론

### ✅ 완료된 작업
1. TEAM_LEAD에게서 `user:update` 권한 제거 (SQL 수정)
2. TEAM_LEAD는 이제 조회만 가능 (권한 데이터 수정)

### ⚠️ 남은 문제
- **코드에서 RBAC 권한을 확인하지 않음**
- DataScopeLevel만 확인하므로 TEAM_LEAD가 여전히 부서 이동 가능

### 🎯 해결 방법
1. **SQL 수정** (완료) ✅
2. **코드 수정** (필요) - DepartmentServiceImpl에 RBAC 권한 확인 로직 추가

---

## 🚀 다음 단계

### 1. reset_database.sql 재실행
```bash
psql -U postgres -d nexfron -f reset_database.sql
```

### 2. 애플리케이션 재시작
```bash
./gradlew bootRun
```

### 3. 테스트
```http
PUT /api/org/departments/{deptId}/move
X-User-Id: 10000000-0000-0000-0000-000000000002  (TEAM_LEAD)
```

**현재 예상 결과**: ✅ 성공 (권한 확인 로직이 없으므로)
**원하는 결과**: ❌ 403 Forbidden

---

**결론**: SQL만으로는 완벽한 해결 불가. 코드 수정이 추가로 필요합니다.

**작성일**: 2026-02-07

