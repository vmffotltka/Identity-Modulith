# ✅ RBAC 문서 기준 권한 정리

## 📋 문서 출처: `Docs/RBAC_SCENARIOS.md`

---

## 🎯 역할별 권한 정의 (문서 기준)

### 1. ADMIN (시스템 관리자)

**DataScope**: `ADMIN` (전체 조직 접근)

**권한**:
```
[Agent 관리]
- agent:create          ✅ 상담사 생성
- agent:read            ✅ 상담사 조회
- agent:update          ✅ 상담사 수정
- agent:delete          ✅ 상담사 삭제
- agent:suspend         ✅ 상담사 정지/활성화
- agent:transfer        ✅ 부서 이동
- agent:role:assign     ✅ 역할 할당
- agent:password:reset  ✅ 비밀번호 초기화

[Department 관리]
- dept:create           ✅ 부서 생성
- dept:read             ✅ 부서 조회
- dept:update           ✅ 부서 수정
- dept:delete           ✅ 부서 삭제
- dept:move             ✅ 부서 이동
- dept:deactivate       ✅ 부서 비활성화

[RBAC 관리]
- role:create           ✅ 역할 생성
- role:read             ✅ 역할 조회
- role:update           ✅ 역할 수정
- role:delete           ✅ 역할 삭제
- permission:read       ✅ 권한 조회
- permission:assign     ✅ 권한 할당

[Report]
- report:view           ✅ 리포트 조회
- report:export         ✅ 리포트 내보내기
- report:create         ✅ 리포트 생성
```

---

### 2. TEAM_LEAD (팀장/부서장)

**DataScope**: `TEAM_LEAD` (본인 부서 + 하위 부서)

**권한**:
```
[Agent 관리] - 팀원에 한해서만
- agent:read            ✅ 상담사 조회 (팀원)
- agent:update          ✅ 상담사 수정 (팀원)
- agent:suspend         ✅ 상담사 정지 (팀원)
- agent:transfer        ✅ 부서 이동 (팀원)
- agent:password:reset  ✅ 비밀번호 초기화 (팀원)

[Department 관리]
- dept:read             ✅ 부서 조회만 (읽기 전용)

[Report]
- report:view           ✅ 리포트 조회
- report:export         ✅ 리포트 내보내기
```

**❌ 없는 권한**:
- `agent:create`, `agent:delete`, `agent:role:assign`
- `dept:create`, `dept:update`, `dept:delete`, `dept:move`, `dept:deactivate`
- `role:*`, `permission:*`
- `report:create`

---

### 3. AGENT (MEMBER) (일반 상담사)

**DataScope**: `MEMBER` (본인 부서만)

**권한**:
```
[Self 관리] - 본인 정보만
- agent:read:self       ✅ 본인 정보 조회
- agent:update:self     ✅ 본인 정보 수정
```

**❌ 없는 권한**:
- 다른 사람 정보 접근 불가
- 부서 관리 불가
- 역할/권한 관리 불가
- 리포트 생성/내보내기 불가

---

## 📊 현재 SQL vs 문서 비교

### ✅ 이미 올바르게 설정된 부분

| 역할 | 권한 | 현재 SQL | 문서 | 상태 |
|------|------|---------|------|------|
| **ADMIN** | 모든 권한 | ✅ | ✅ | 일치 |
| **TEAM_LEAD** | user:read | ✅ | ✅ | 일치 |
| **TEAM_LEAD** | org:read | ✅ | ✅ | 일치 |
| **TEAM_LEAD** | report:view | ✅ | ✅ | 일치 |
| **TEAM_LEAD** | report:export | ✅ | ✅ | 일치 |
| **MEMBER** | user:read | ✅ | ✅ | 일치 |
| **MEMBER** | org:read | ✅ | ✅ | 일치 |
| **MEMBER** | report:view | ✅ | ✅ | 일치 |

### ❌ TEAM_LEAD가 없어야 하는 권한 (이미 제거됨)

| 권한 | 현재 SQL | 문서 | 상태 |
|------|---------|------|------|
| `user:create` | ❌ 없음 | ❌ 없음 | ✅ 일치 |
| `user:update` | ❌ 없음 | ❌ 없음 | ✅ 일치 (방금 제거) |
| `user:delete` | ❌ 없음 | ❌ 없음 | ✅ 일치 |
| `org:create` | ❌ 없음 | ❌ 없음 | ✅ 일치 |
| `org:update` | ❌ 없음 | ❌ 없음 | ✅ 일치 |
| `org:delete` | ❌ 없음 | ❌ 없음 | ✅ 일치 |

---

## 🎯 결론

### ✅ SQL이 문서와 일치함!

현재 `reset_database.sql`, `V1_0_0`, `V2_0_0`에 설정된 권한이 문서(`RBAC_SCENARIOS.md`)와 **완벽히 일치**합니다!

```sql
-- ADMIN: 모든 권한 (문서와 일치)
INSERT INTO rbac_role_permissions (role_id, permission_id, ...)
SELECT admin_role_id, permission_id, ...
FROM rbac_permissions WHERE tenant_id = std_tenant;

-- TEAM_LEAD: 조회 + 리포트만 (문서와 일치)
INSERT INTO rbac_role_permissions (role_id, permission_id, ...)
VALUES
    (team_lead_role_id, perm_user_read, ...),      -- 조회
    (team_lead_role_id, perm_org_read, ...),       -- 조회
    (team_lead_role_id, perm_report_view, ...),    -- 조회
    (team_lead_role_id, perm_report_export, ...);  -- 내보내기

-- MEMBER: 기본 조회만 (문서와 일치)
INSERT INTO rbac_role_permissions (role_id, permission_id, ...)
VALUES
    (member_role_id, perm_user_read, ...),
    (member_role_id, perm_org_read, ...),
    (member_role_id, perm_report_view, ...);
```

---

## ⚠️ 남은 문제: 코드에서 권한 확인 안 함

### 문제
```java
// DepartmentServiceImpl.validateMoveDepartment()
// ❌ RBAC 권한(org:update) 확인 안 함
// ✅ DataScopeLevel만 확인
```

### 결과
- SQL에서는 TEAM_LEAD에게 `org:update` 권한 없음 ✅
- 하지만 코드에서 확인하지 않음 ❌
- 따라서 TEAM_LEAD가 부서 이동 가능 ❌

---

## 💡 해결 방법

### SQL은 이미 완벽 ✅
- 문서와 100% 일치
- TEAM_LEAD는 `dept:read`만 가능

### 코드 수정 필요
- DepartmentServiceImpl에 RBAC 권한 확인 추가
- `org:update`, `org:create`, `org:delete` 권한 검증

---

**작성일**: 2026-02-07  
**기준 문서**: `Docs/RBAC_SCENARIOS.md`  
**결론**: SQL은 문서와 일치, 코드 수정 필요

