# ✅ DataScopeLevel 오류 수정 완료

## 🎯 문제 원인

**오류**: `No enum constant com.nexfron.identitymodulith.rbac.domain.DataScopeLevel.ALL`

### 원인 분석
- **Enum 정의**: `ADMIN`, `TEAM_LEAD`, `MEMBER`
- **SQL에서 사용**: `'ALL'`, `'DEPARTMENT'`, `'SELF'`
- **결과**: Hibernate가 DB 값을 enum으로 변환할 수 없음

---

## ✅ 해결 방법

**원칙**: **SQL 파일만 수정, Java 코드는 수정하지 않음**

### 수정된 파일 (3개)

#### 1. `reset_database.sql`

**A. CHECK 제약조건 수정**
```sql
-- 변경 전
CONSTRAINT chk_data_scope CHECK (data_scope_level IN ('SELF', 'DEPARTMENT', 'ALL', 'CUSTOM'))

-- 변경 후
CONSTRAINT chk_data_scope CHECK (data_scope_level IN ('ADMIN', 'TEAM_LEAD', 'MEMBER', 'CUSTOM'))
```

**B. INSERT 값 수정**
```sql
-- 변경 전
(admin_role_id, std_tenant, 'ADMIN', 'POSITION', 'ALL', TRUE, ...)
(team_lead_role_id, std_tenant, 'TEAM_LEAD', 'POSITION', 'DEPARTMENT', TRUE, ...)
(member_role_id, std_tenant, 'MEMBER', 'POSITION', 'SELF', TRUE, ...)

-- 변경 후
(admin_role_id, std_tenant, 'ADMIN', 'POSITION', 'ADMIN', TRUE, ...)
(team_lead_role_id, std_tenant, 'TEAM_LEAD', 'POSITION', 'TEAM_LEAD', TRUE, ...)
(member_role_id, std_tenant, 'MEMBER', 'POSITION', 'MEMBER', TRUE, ...)
```

#### 2. `V1_0_0__Complete_Schema_With_Code.sql`
- CHECK 제약조건: `'SELF', 'DEPARTMENT', 'ALL'` → `'ADMIN', 'TEAM_LEAD', 'MEMBER'`
- INSERT 값: `'ALL'` → `'ADMIN'`, `'DEPARTMENT'` → `'TEAM_LEAD'`, `'SELF'` → `'MEMBER'`

#### 3. `V2_0_0__Fixed_Schema.sql`
- CHECK 제약조건: `'SELF', 'DEPARTMENT', 'ALL'` → `'ADMIN', 'TEAM_LEAD', 'MEMBER'`
- INSERT 값: `'ALL'` → `'ADMIN'`, `'DEPARTMENT'` → `'TEAM_LEAD'`, `'SELF'` → `'MEMBER'`

---

## 🗺️ DataScopeLevel 매핑 (최종)

| Enum 값 | 의미 | 접근 범위 |
|---------|------|----------|
| `ADMIN` | 시스템 관리자 | 전체 조직 |
| `TEAM_LEAD` | 팀장/부서장 | 본인 부서 + 하위 부서 |
| `MEMBER` | 일반 사원 | 본인 부서만 |

---

## 🚀 적용 방법

### 방법 1: 기존 DB가 있는 경우

```sql
-- PostgreSQL에 직접 접속
psql -U postgres -d nexfron

-- 기존 데이터 수정
UPDATE rbac_roles 
SET data_scope_level = 'ADMIN' 
WHERE data_scope_level = 'ALL';

UPDATE rbac_roles 
SET data_scope_level = 'TEAM_LEAD' 
WHERE data_scope_level = 'DEPARTMENT';

UPDATE rbac_roles 
SET data_scope_level = 'MEMBER' 
WHERE data_scope_level = 'SELF';
```

### 방법 2: 깨끗하게 재설치 (추천)

```bash
# PostgreSQL에 접속하여 reset_database.sql 실행
psql -U postgres -d nexfron -f reset_database.sql

# 애플리케이션 실행
./gradlew bootRun
```

---

## 🔍 검증

### 1. DB에서 확인
```sql
SELECT role_id, name, data_scope_level 
FROM rbac_roles 
WHERE tenant_id = 'default-tenant';

-- 예상 결과:
-- role_id | name       | data_scope_level
-- --------+------------+-----------------
-- ...     | ADMIN      | ADMIN
-- ...     | TEAM_LEAD  | TEAM_LEAD
-- ...     | MEMBER     | MEMBER
```

### 2. API 호출 테스트
```http
PUT /api/org/departments/00000000-0000-0000-0000-000000000004/move
X-User-Id: 10000000-0000-0000-0000-000000000001

{
  "newParentId": "00000000-0000-0000-0000-000000000003"
}

-- 예상 결과: 200 OK 또는 204 No Content (오류 없음)
```

---

## 📊 수정 요약

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| **파일 수정** | Java + SQL | **SQL만** |
| **CHECK 제약조건** | `'SELF', 'DEPARTMENT', 'ALL'` | `'ADMIN', 'TEAM_LEAD', 'MEMBER'` ✅ |
| **ADMIN 역할** | `'ALL'` | `'ADMIN'` ✅ |
| **TEAM_LEAD 역할** | `'DEPARTMENT'` | `'TEAM_LEAD'` ✅ |
| **MEMBER 역할** | `'SELF'` | `'MEMBER'` ✅ |

---

## 🎉 완료!

- ✅ **모든 SQL 파일 수정 완료** (3개)
- ✅ **Java 코드 수정 없음**
- ✅ **reset_database.sql 한 번 실행으로 해결**
- ✅ **enum과 DB 값 완벽히 일치**

**이제 reset_database.sql만 실행하면 모든 오류가 해결됩니다!** 🚀

---

**작성일**: 2026-02-07  
**수정 방식**: SQL 파일만 수정 (Java 코드 수정 없음)  
**파일**: reset_database.sql, V1_0_0, V2_0_0

