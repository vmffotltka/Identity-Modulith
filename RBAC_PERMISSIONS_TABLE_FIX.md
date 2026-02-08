# ✅ rbac_permissions 테이블 수정 완료

## 🎯 문제

**오류**: 
```
ERROR: column pje1_0.category does not exist
ERROR: column pje1_0.description does not exist
```

**원인**:
- **엔티티**: `PermissionJpaEntity`에 `category`, `description` 필드 있음
- **DB 테이블**: `rbac_permissions`에 컬럼 없음
- **결과**: Hibernate가 SELECT 쿼리 실행 시 오류

---

## ✅ 해결 완료

### 수정된 파일 (3개)

#### 1. `reset_database.sql`
#### 2. `V1_0_0__Complete_Schema_With_Code.sql`
#### 3. `V2_0_0__Fixed_Schema.sql`

### 수정 내용

**추가된 컬럼**:
```sql
CREATE TABLE rbac_permissions (
    permission_id       VARCHAR(36)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    code                VARCHAR(128)    NOT NULL,      -- 100 → 128 확장
    name                VARCHAR(100)    NOT NULL,
    description         VARCHAR(500),   -- ✅ 추가
    category            VARCHAR(64),    -- ✅ 추가
    resource            VARCHAR(100),
    action              VARCHAR(50),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT          DEFAULT 0,      -- INTEGER → BIGINT
    CONSTRAINT uk_perm_tenant_code UNIQUE (tenant_id, code)
);
```

**제거된 컬럼**:
```sql
-- ❌ created_by, updated_by 제거 (엔티티에 없음)
```

---

## 📊 엔티티 vs SQL 매핑 (최종)

| 엔티티 필드 | SQL 컬럼 | 타입 | 상태 |
|------------|----------|------|------|
| `permissionId` | `permission_id` | VARCHAR(36) | ✅ |
| `tenantId` | `tenant_id` | VARCHAR(50) | ✅ |
| `code` | `code` | VARCHAR(128) | ✅ |
| `name` | `name` | VARCHAR(100) | ✅ |
| `description` | `description` | VARCHAR(500) | ✅ 추가 |
| `category` | `category` | VARCHAR(64) | ✅ 추가 |
| `createdAt` | `created_at` | TIMESTAMP | ✅ |
| `updatedAt` | `updated_at` | TIMESTAMP | ✅ |
| `version` | `version` | BIGINT | ✅ |

---

## 🚀 적용 방법

### 1. DB 완전 초기화 (추천)

```bash
# PostgreSQL에서 reset_database.sql 실행
psql -U postgres -d nexfron -f reset_database.sql
```

---

### 2. 애플리케이션 재시작

```bash
# IntelliJ에서 Stop → Run
# 또는
./gradlew bootRun
```

---

### 3. 테스트

#### A. ADMIN으로 부서 이동 (성공 예상) ✅

```http
PUT /api/org/departments/00000000-0000-0000-0000-000000000004/move
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json

{
  "newParentId": "00000000-0000-0000-0000-000000000003"
}
```

**예상 결과**: `204 No Content` ✅

**콘솔 로그**:
```
[RBAC] 사용자 권한 조회 - agentId=10000000-0000-0000-0000-000000000001, permissionCount=10
[ORG] 부서 이동 완료 - deptId=00000000-0000-0000-0000-000000000004
```

---

#### B. TEAM_LEAD로 부서 이동 (실패 예상) ❌

```http
PUT /api/org/departments/00000000-0000-0000-0000-000000000004/move
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
[RBAC] 사용자 권한 조회 - agentId=10000000-0000-0000-0000-000000000002, permissionCount=4
WARN [ORG] org:update 권한 없음 - userId=10000000-0000-0000-0000-000000000002, 
     permissions=[user:read, org:read, report:view, report:export]
```

---

## 🎉 완료!

### ✅ 수정 내용 (총 6개 파일)

**SQL 파일 (3개)**:
1. reset_database.sql
2. V1_0_0__Complete_Schema_With_Code.sql
3. V2_0_0__Fixed_Schema.sql

**변경 사항**:
- `rbac_permissions` 테이블에 `category`, `description` 컬럼 추가
- `version` 타입을 BIGINT로 변경
- `code` 길이를 128로 확장

**Java 파일 (3개)**:
1. RbacModuleApi.java - 권한 조회 메서드 추가
2. RbacManagementServiceImpl.java - 메서드 구현
3. DepartmentServiceImpl.java - RBAC 권한 검증 추가

---

## 📋 다음 단계

### 1. reset_database.sql 실행
```bash
psql -U postgres -d nexfron -f reset_database.sql
```

### 2. 애플리케이션 재시작
```bash
./gradlew bootRun
```

### 3. API 테스트
- ADMIN: 부서 이동 성공 ✅
- TEAM_LEAD: 부서 이동 실패 (403 Forbidden) ❌

---

**이제 모든 오류가 해결됩니다!** 🚀

---

**작성일**: 2026-02-07  
**수정 파일**: 6개 (SQL 3개 + Java 3개)  
**핵심 수정**: rbac_permissions 테이블에 category, description 컬럼 추가

