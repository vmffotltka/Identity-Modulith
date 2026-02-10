# Identity Modulith - 데이터베이스 가이드

> 📅 최종 업데이트: **2026-02-10 (v3.1.0)**  
> 🗄️ 지원 DB: PostgreSQL 18+  
> 🔧 주요 변경: **비밀번호 암호화 BCrypt로 통일**, **RBAC 권한 검증 완전 구현**

---

## 📋 v3.1.0 주요 변경 사항 (2026-02-10)

### 1️⃣ 비밀번호 암호화
- ✅ **알고리즘 변경**: SHA-256 → **BCrypt**
- ✅ **초기 데이터 비밀번호**: 모든 계정 `Admin123!`
- ✅ **BCrypt 해시**: `$2a$10$o6GJICIhlWnRqm3wJNLzx.DtRHtISvJXgBZ.7YKjGJoCXZ27eoBB2`

### 2️⃣ RBAC 권한 검증
- ✅ **user_agents.role_id (JSON) 컬럼**: deprecated → **rbac_agent_roles 테이블 사용**
- ✅ **권한 검증 로직**: RBAC 모듈 연동 (RbacPort/RbacAdapter)
- ✅ **hasRole() 메서드**: rbac_agent_roles 테이블에서 조회

### 3️⃣ 에러 코드 추가
- ✅ **P001**: PASSWORD_MISMATCH - 현재 비밀번호 불일치
- ✅ **P002**: PASSWORD_CONFIRMATION_MISMATCH - 확인 비밀번호 불일치
- ✅ **P003**: SAME_AS_CURRENT_PASSWORD - 동일한 비밀번호

### 4️⃣ API 테스트 진행 상황
- ✅ **Organization API**: 전체 완료 (Scenario 1-12)
- 🔄 **Agent API**: Scenario 8까지 완료, **Scenario 9 진행 중** (역할 관리)
- ⏳ **RBAC API**: 대기 중

---

## 📊 전체 테이블 구조 (6개)

| ?�이블명 | 모듈 | PK ?�??| ?�명 |
|---------|------|---------|------|
| **org_departments** | Organization | VARCHAR(50) | 조직(부?? 계층 구조 |
| **agents** | User | VARCHAR(50) | ?�용???�담?? ?�보 |
| **rbac_roles** | RBAC | VARCHAR(50) | ??�� ?�의 (POSITION, CHANNEL) |
| **rbac_permissions** | RBAC | VARCHAR(50) | 권한 ?�의 |
| **rbac_role_permissions** | RBAC | Composite PK | ??��-권한 매핑 (M:N) |
| **user_agent_roles** | RBAC | Composite PK | ?�용????�� 매핑 (M:N) |

**변�??�항 (v3.0.0)**:
- ??`departmentEntities` ??`org_departments`
- ??`roles` ??`rbac_roles`
- ??`permissions` ??`rbac_permissions`
- ??`role_permissions` ??`rbac_role_permissions`
- ??`agent_roles` ??`user_agent_roles`
- ??PK ?�?? VARCHAR(36) ??VARCHAR(50) (UUID + ?�유 공간)
- ??`org_departments`??`type` (DepartmentType Enum), `is_active` 컬럼 추�?

---

## ?�� 1. org_departments (조직/부??

**목적**: 조직 계층 구조 관�?(?�리 구조 - Materialized Path ?�턴)

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식/?�시 |
|--------|------|------|------|----------------|
| **dept_id** | VARCHAR(50) | ??| 부??ID (PK) | `dept-root-001`, UUID |
| tenant_id | VARCHAR(50) | ??| ?�넌??ID | `tenant-001` |
| name | VARCHAR(100) | ??| 부?�명 | `?�스?�론`, `고객?�비?�본부` |
| **type** | VARCHAR(20) | ??| 부???�??(Enum) | `COMPANY`, `DIVISION`, `TEAM`, `GROUP`, `CUSTOM` |
| custom_type_name | VARCHAR(50) | ??| ?�용???�의 ?�???�름 | `?�구??, `지?? (type=CUSTOM???? |
| parent_dept_id | VARCHAR(50) | ??| ?�위 부??ID (FK) | NULL=최상?? UUID=?�위 |
| org_path | TEXT | ??| 조직 경로 (Materialized Path) | `/dept-root-001/dept-div-001/` |
| depth | INTEGER | ??| ?�리 깊이 | 0(최상?? ~ 10 |
| display_order | INTEGER | ??| ?�시 ?�서 | 1, 2, 3... |
| manager_id | VARCHAR(50) | ??| 부?�장 ID | Agent ID |
| description | TEXT | ??| 부???�명 | `고객 ?�비??�??�담 ?�무 총괄` |
| **is_active** | BOOLEAN | ??| ?�성???�태 | TRUE (?�성), FALSE (비활?? |
| created_at | TIMESTAMP | ??| ?�성 ?�시 | `2026-01-21 10:00:00` |
| updated_at | TIMESTAMP | ??| ?�정 ?�시 | `2026-02-05 15:00:00` |
| created_by | VARCHAR(50) | ??| ?�성??ID | Agent ID |
| updated_by | VARCHAR(50) | ??| ?�정??ID | Agent ID |

**?�덱??*: 
- `idx_dept_tenant`: `(tenant_id)`
- `idx_dept_parent`: `(parent_dept_id)`
- `idx_dept_org_path`: `(org_path)`
- `idx_dept_active`: `(is_active)`

**FK**: 
- `parent_dept_id` ??`org_departments(dept_id)` ON DELETE RESTRICT

**체크 ?�약**:
- `chk_dept_type`: type IN ('COMPANY', 'DIVISION', 'TEAM', 'GROUP', 'CUSTOM')
- `chk_custom_type`: type='CUSTOM'???�만 custom_type_name ?�수

**Department Type ?�명**:
| ?�??| ?�명 | ?�용 ?�시 |
|------|------|----------|
| `COMPANY` | 최상??조직 | ?�사, 계열??|
| `DIVISION` | 본�?�?조직 | 고객?�비?�본부, ?�업본�? |
| `TEAM` | ?��?조직 | ?�바?�드?�, ?�웃바운?��? |
| `GROUP` | 그룹/?�트 | 개발그룹, 기획?�트 |
| `CUSTOM` | ?�용???�의 | custom_type_name?�로 ?�름 지??|

**?�이???�시**:
```sql
-- 최상??조직 (COMPANY)
('dept-root-001', 'tenant-001', '?�스?�론', 'COMPANY', NULL, NULL, 
 '/dept-root-001/', 0, 1, NULL, '?�스?�론 주식?�사', TRUE, NOW(), NOW(), NULL, NULL)

-- 본�? (DIVISION)
('dept-div-001', 'tenant-001', '고객?�비?�본부', 'DIVISION', NULL, 'dept-root-001', 
 '/dept-root-001/dept-div-001/', 1, 1, NULL, '고객 ?�비??총괄', TRUE, NOW(), NOW(), NULL, NULL)

-- ?� (TEAM)
('dept-team-001', 'tenant-001', '?�바?�드?�', 'TEAM', NULL, 'dept-div-001', 
 '/dept-root-001/dept-div-001/dept-team-001/', 2, 1, NULL, '?�바?�드 ?�화 ?�담', TRUE, NOW(), NOW(), NULL, NULL)
```

---

## ?�� 2. user_agents (?�용???�담??


**목적**: ?�스???�용???�보 관�?

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식/?�시 |
|--------|------|------|------|----------------|
| **agent_id** | VARCHAR(50) | ??| ?�용??ID (PK) | `agent-admin-001`, UUID |
| tenant_id | VARCHAR(50) | ??| ?�넌??ID | `tenant-001` |
| login_id | VARCHAR(50) | ??| 로그??ID (UK) | `admin`, `agent01` (?�문+?�자, 4-20?? |
| password | VARCHAR(255) | ??| 비�?번호 (BCrypt) | `$2a$10$...` (BCrypt ?�시) |
| name | VARCHAR(100) | ??| ?�용?�명 | `관리자`, `?�길?? (2-50?? |
| employee_id | VARCHAR(50) | ??| ?�원 번호 | `EMP001`, `2024001` |
| email | VARCHAR(100) | ??| ?�메??| `admin@nexfron.com` |
| phone | VARCHAR(20) | ??| ?�화번호 | `010-1234-5678` |
| dept_id | VARCHAR(50) | ??| ?�속 부??ID (FK) | org_departments(dept_id) |
| status | VARCHAR(20) | ??| ?�태 | **`ACTIVE`** (?�성), **`SUSPENDED`** (?��?), **`RETIRED`** (?�사) |
| password_must_change | BOOLEAN | ??| 비�?번호 변�??�요 | `FALSE` (기본�? |
| created_at | TIMESTAMP | ??| ?�성 ?�시 | `2026-01-21 10:00:00` |
| updated_at | TIMESTAMP | ??| ?�정 ?�시 | `2026-02-05 15:00:00` |
| suspended_at | TIMESTAMP | ??| ?��? ?�시 | `2025-12-31 23:59:59` |
| retired_at | TIMESTAMP | ??| ?�사 ?�시 | `2025-12-31 23:59:59` |
| scheduled_delete_at | TIMESTAMP | ??| ??�� ?�정 ?�시 | ?�사 ??90??|
| created_by | VARCHAR(50) | ??| ?�성??ID | Agent ID |
| updated_by | VARCHAR(50) | ??| ?�정??ID | Agent ID |
| suspended_by | VARCHAR(50) | ??| ?��? 처리??ID | Agent ID |
| retired_by | VARCHAR(50) | ??| ?�사 처리??ID | Agent ID |
| version | BIGINT | ??| ?��????�금 버전 | 0 (기본�? |

**?�덱??*: 
- UK: `(tenant_id, login_id)` (복합 ?�니??
- `idx_agent_tenant`: `(tenant_id)`
- `idx_agent_login`: `(login_id)`
- `idx_agent_dept`: `(dept_id)`
- `idx_agent_status`: `(status)`
- `idx_agent_scheduled_delete`: `(scheduled_delete_at)` WHERE scheduled_delete_at IS NOT NULL

**FK**: 
- `dept_id` ??`org_departments(dept_id)` ON DELETE SET NULL

**체크 ?�약**:
- `chk_agent_status`: status IN ('ACTIVE', 'SUSPENDED', 'RETIRED')

**데이터 예시**:
```sql
-- 관리자 (비밀번호: Admin123!)
('agent-admin-001', 'tenant-001', 'admin', 
 '$2a$10$o6GJICIhlWnRqm3wJNLzx.DtRHtISvJXgBZ.7YKjGJoCXZ27eoBB2', 
 '관리자', 'EMP001', 'admin@nexfron.com', '010-1234-5678', 
 'dept-root-001', 'ACTIVE', FALSE, NOW(), NOW(), NULL, NULL, NULL, 
 NULL, NULL, NULL, NULL, 0)

-- 팀장
('agent-lead-001', 'tenant-001', 'teamlead01', 
 '$2a$10$o6GJICIhlWnRqm3wJNLzx.DtRHtISvJXgBZ.7YKjGJoCXZ27eoBB2', 
 '김팀장', 'EMP002', 'teamlead@nexfron.com', '010-2345-6789', 
 'dept-div-001', 'ACTIVE', FALSE, NOW(), NOW(), NULL, NULL, NULL, 
 NULL, NULL, NULL, NULL, 0)
```

**비밀번호 해시**:
- **알고리즘**: **BCrypt** (Spring Security 권장)
- **강도**: 10 rounds (기본값)
- **테스트 비밀번호**: `Admin123!`
- **해시 값**: `$2a$10$o6GJICIhlWnRqm3wJNLzx.DtRHtISvJXgBZ.7YKjGJoCXZ27eoBB2`
- **특징**: 
  - Salt 포함 (매번 다른 해시 생성)
  - 60자 고정 길이
  - `$2a$` (알고리즘 버전) + `10$` (cost factor) + salt(22자) + hash(31자)

**⚠️ v3.1.0 변경사항**:
- ❌ Before: SHA-256 + Base64 (보안 취약)
- ✅ After: BCrypt (업계 표준, 안전)

---

## ?�� 3. rbac_roles (??��)

**목적**: ??�� ?�의 �?관�?(POSITION, CHANNEL ?�??

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식/?�시 |
|--------|------|------|------|----------------|
| **role_id** | VARCHAR(50) | ??| ??�� ID (PK) | `role-admin-001`, UUID |
| tenant_id | VARCHAR(50) | ??| ?�넌??ID | `tenant-001` |
| name | VARCHAR(50) | ??| ??���?(UK) | **`ADMIN`**, **`TEAM_LEAD`**, **`AGENT`** (직급) <br> **`INBOUND_AGENT`**, **`CHAT_AGENT`** (채널) |
| **type** | VARCHAR(20) | ??| ??�� ?�??| **`POSITION`** (직급 기반), **`CHANNEL`** (채널 기반) |
| **data_scope** | VARCHAR(20) | ??| ?�이???�코???�벨 | **`ADMIN`**, **`TEAM_LEAD`**, **`MEMBER`** (POSITION???�만) |
| description | VARCHAR(255) | ??| ??�� ?�명 | `?�스???�체 관리자 - 모든 권한 보유` |
| is_active | BOOLEAN | ??| ?�성???�태 | TRUE (기본�? |
| created_at | TIMESTAMP | ??| ?�성 ?�시 | `2026-01-21 10:00:00` |
| updated_at | TIMESTAMP | ??| ?�정 ?�시 | `2026-02-05 15:00:00` |

**?�덱??*: 
- UK: `(tenant_id, name)` (복합 ?�니??
- `idx_role_tenant`: `(tenant_id)`
- `idx_role_type`: `(type)`
- `idx_role_active`: `(is_active)`

**체크 ?�약**:
- `chk_role_type`: type IN ('POSITION', 'CHANNEL')
- `chk_role_data_scope`: 
  - POSITION???? data_scope IN ('ADMIN', 'TEAM_LEAD', 'MEMBER')
  - CHANNEL???? data_scope IS NULL

**??�� ?�???�명**:
| ?�??| ?�명 | ?�시 | data_scope |
|------|------|------|------------|
| `POSITION` | 직급 기반 ??�� | ADMIN, TEAM_LEAD, AGENT | ?�수 (ADMIN, TEAM_LEAD, MEMBER) |
| `CHANNEL` | 채널 기반 ??�� | INBOUND_AGENT, CHAT_AGENT | NULL |

**?�이???�코???�벨**:
| ?�벨 | ?�명 | 조회 범위 |
|------|------|----------|
| `ADMIN` | ?�체 ?�이???�근 | ?�넌????모든 부??|
| `TEAM_LEAD` | 본인 부??+ ?�위 | 본인 부?��? ?�위 부???�체 |
| `MEMBER` | 본인 부?�만 | 본인???�속??부?�만 |

**?�이???�시**:
```sql
-- POSITION ??��
('role-admin-001', 'tenant-001', 'ADMIN', 'POSITION', 'ADMIN', 
 '?�스??관리자 (?�체 조직 ?�근)', TRUE, NOW(), NOW()),
('role-teamlead-001', 'tenant-001', 'TEAM_LEAD', 'POSITION', 'TEAM_LEAD', 
 '?�??(본인 ?� + ?�위 부???�근)', TRUE, NOW(), NOW()),
('role-agent-001', 'tenant-001', 'AGENT', 'POSITION', 'MEMBER', 
 '?�반 ?�담??(본인 ?��??�근)', TRUE, NOW(), NOW())

-- CHANNEL ??��
('role-ch-inbound', 'tenant-001', 'INBOUND_AGENT', 'CHANNEL', NULL, 
 '?�바?�드 ?�화 ?�담', TRUE, NOW(), NOW()),
('role-ch-chat', 'tenant-001', 'CHAT_AGENT', 'CHANNEL', NULL, 
 '채팅 ?�담', TRUE, NOW(), NOW())
```

---

## ?�� 4. rbac_permissions (권한)

**목적**: 권한 ?�의 �?관�?

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식/?�시 |
|--------|------|------|------|----------------|
| **permission_id** | VARCHAR(50) | ??| 권한 ID (PK) | `perm-agent-001`, UUID |
| tenant_id | VARCHAR(50) | ??| ?�넌??ID | `tenant-001` |
| code | VARCHAR(100) | ??| 권한 코드 (UK) | `agent:create`, `dept:read`, `role:manage` |
| name | VARCHAR(100) | ??| 권한 ?�름 | `?�담???�성`, `부??조회`, `??�� 관�? |
| description | VARCHAR(255) | ??| 권한 ?�명 | `?�로???�담??계정 ?�성` |
| category | VARCHAR(50) | ??| 권한 카테고리 | `AGENT`, `DEPARTMENT`, `RBAC`, `CHANNEL` |
| created_at | TIMESTAMP | ??| ?�성 ?�시 | `2026-01-21 10:00:00` |

**?�덱??*: 
- UK: `(tenant_id, code)` (복합 ?�니??
- `idx_permission_tenant`: `(tenant_id)`
- `idx_permission_category`: `(category)`

**권한 코드 ?�식**: `?�메???�션`
- ?�메?? agent, dept, role, permission, channel
- ?�션: create, read, update, delete, suspend, activate, transfer ??

**권한 카테고리**:
| 카테고리 | ?�명 | 권한 ?�시 |
|----------|------|----------|
| `AGENT` | ?�담??관�?| agent:create, agent:read, agent:update, agent:delete |
| `DEPARTMENT` | 부??관�?| dept:create, dept:read, dept:update, dept:delete, dept:move |
| `RBAC` | ??��/권한 관�?| role:create, role:delete, permission:assign |
| `CHANNEL` | 채널�?권한 | channel:inbound:receive, channel:chat:message |

**?�이???�시**:
```sql
-- AGENT 카테고리
('perm-agent-001', 'tenant-001', 'agent:create', '?�담???�성', 
 '?�로???�담??계정 ?�성', 'AGENT', NOW()),
('perm-agent-002', 'tenant-001', 'agent:read', '?�담??조회', 
 '?�담???�보 조회', 'AGENT', NOW()),

-- DEPARTMENT 카테고리
('perm-dept-001', 'tenant-001', 'dept:create', '부???�성', 
 '?�로??부???�성', 'DEPARTMENT', NOW()),
('perm-dept-002', 'tenant-001', 'dept:read', '부??조회', 
 '부???�보 조회', 'DEPARTMENT', NOW()),

-- CHANNEL 카테고리
('perm-ch-in-001', 'tenant-001', 'channel:inbound:receive', '?�바?�드 ?�신', 
 '?�바?�드 ?�화 ?�신', 'CHANNEL', NOW()),
('perm-ch-chat-001', 'tenant-001', 'channel:chat:message', '채팅 메시지', 
 '채팅 메시지 ?�수??, 'CHANNEL', NOW())
```

---

**목적**: ??�� ?�의 �?관�?

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식/?�시 |
|--------|------|------|------|----------------|
| **role_id** | VARCHAR(36) | ??| ??�� ID (PK) | UUID |
| tenant_id | VARCHAR(50) | ??| ?�넌??ID | `tenant-001` |
| name | VARCHAR(64) | ??| ??���?(UK) | **`ADMIN`**, **`MANAGER`**, **`TEAM_LEAD`**, **`MEMBER`** (직책) <br> **`PHONE_AGENT`**, **`CHAT_AGENT`**, **`EMAIL_AGENT`** (채널) |
| type | VARCHAR(32) | ??| ??�� ?�??| **`POSITION`** (직책 기반), **`CHANNEL`** (채널 기반), **`SKILL`** (?�킬 기반) |
| description | VARCHAR(255) | ??| ??�� ?�명 | `?�스???�체 관리자 - 모든 권한 보유` |
| is_active | BOOLEAN | ??| ?�성???�태 | **`true`** (?�성), **`false`** (비활???�리 ??��) |
| version | BIGINT | ??| ?��????�금 버전 | 0, 1, 2... (?�시???�어?? |
| created_at | TIMESTAMP | ??| ?�성 ?�시 | `2026-01-21 10:00:00` |
| updated_at | TIMESTAMP | ??| ?�정 ?�시 | `2026-01-21 15:00:00` |

**?�덱??*: `(tenant_id, name)` UK, `tenant_id`, `is_active`

**?��? ??�� (8�?**:

### 직책 기반 (POSITION)
1. **ADMIN** - ?�스???�체 관리자 (35�??�체 권한)
2. **MANAGER** - 부??관리자 (12�?권한)
3. **TEAM_LEAD** - ?� 리더 (5�?권한)
4. **MEMBER** - ?�반 ?�용??(4�?권한)

### 채널 기반 (CHANNEL)
5. **PHONE_AGENT** - ?�화 ?�담??(3�?권한)
6. **CHAT_AGENT** - 채팅 ?�담??(2�?권한)
7. **EMAIL_AGENT** - ?�메???�담??(1�?권한)
8. **SUPERVISOR** - ?�퍼바이?� (??관�?

**?�이???�시**:
```sql
('660e8400-e29b-41d4-a716-446655440001', 'tenant-001', 'ADMIN', 'POSITION', 
 '?�스???�체 관리자 - 모든 권한 보유', true, 0, NOW(), NOW())
```

---

## ?�� 4. permissions (권한)

**목적**: ?�분?�된 권한 ?�의

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식/?�시 |
|--------|------|------|------|----------------|
| **permission_id** | VARCHAR(36) | ??| 권한 ID (PK) | UUID |
| tenant_id | VARCHAR(50) | ??| ?�넌??ID | `tenant-001` |
| code | VARCHAR(128) | ??| 권한 코드 (UK) | **`domain:action`** ?�식 |
| created_at | TIMESTAMP | ??| ?�성 ?�시 | `2026-01-21 10:00:00` |

**?�덱??*: `(tenant_id, code)` UK, `tenant_id`

**?��? 권한 코드 (35�?**:

### ?�용??관�?(user)
- `user:create` - ?�용???�성
- `user:read` - ?�용??조회 (?�체)
- `user:read:self` - 본인 ?�보 조회
- `user:update` - ?�용???�정
- `user:update:self` - 본인 ?�보 ?�정
- `user:delete` - ?�용????��
- `user:manage` - ?�용???�체 관�?
- `user:assign:role` - ??�� ?�당
- `user:reset:password` - 비�?번호 초기??

### 조직 관�?(org)
- `org:view` - 조직??조회
- `org:create` - 부???�성
- `org:update` - 부???�정
- `org:move` - 부???�동
- `org:delete` - 부????��
- `org:manage` - 조직 ?�체 관�?

### RBAC 관�?(rbac)
- `rbac:view` - ??��/권한 조회
- `rbac:create:role` - ??�� ?�성
- `rbac:update:role` - ??�� ?�정
- `rbac:delete:role` - ??�� ??��
- `rbac:create:permission` - 권한 ?�성
- `rbac:update:permission` - 권한 ?�정
- `rbac:delete:permission` - 권한 ??��
- `rbac:assign:permission` - 권한 ?�당
- `rbac:configure` - RBAC ?�체 ?�정

### 보고??(report)
- `report:view` - 보고??조회
- `report:read` - 보고???�기
- `report:export` - 보고???�보?�기
- `report:manage` - 보고??관�?

### 채널 (phone, chat, email)
- `phone:accept` - ?�화 ?�신
- `phone:hold` - ?�화 보류
- `phone:transfer` - ?�화 ?�환
- `chat:send` - 채팅 ?�송
- `chat:read` - 채팅 ?�기
- `email:send` - ?�메???�송
- `queue:manage` - ??관�?

**?�이???�시**:
```sql
('550e8400-e29b-41d4-a716-446655440001', 'tenant-001', 'user:create', NOW())
```

---

## ?�� 5. rbac_role_permissions (??��-권한 매핑)

**목적**: ??���?권한???��???관�?(M:N)

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식/?�시 |
|--------|------|------|------|----------------|
| **role_id** | VARCHAR(50) | ??| ??�� ID (PK, FK) | `role-admin-001` |
| **permission_id** | VARCHAR(50) | ??| 권한 ID (PK, FK) | `perm-agent-001` |
| assigned_at | TIMESTAMP | ??| ?�당 ?�시 | `2026-01-21 10:00:00` |
| assigned_by | VARCHAR(50) | ??| ?�당??ID | Agent ID |

**PK**: `(role_id, permission_id)` (복합 PK)

**?�덱??*: 
- `idx_rp_role`: `(role_id)`
- `idx_rp_permission`: `(permission_id)`

**FK**: 
- `role_id` ??`rbac_roles(role_id)` ON DELETE CASCADE
- `permission_id` ??`rbac_permissions(permission_id)` ON DELETE CASCADE

**초기 매핑 ??*:
- **ADMIN**: 35�?권한 (?�체 권한)
- **TEAM_LEAD**: 6�?권한 (agent:read, agent:update, agent:transfer, dept:read, role:read, permission:read)
- **AGENT**: 3�?권한 (agent:read, dept:read, role:read)
- **INBOUND_AGENT**: 3�?권한 (channel:inbound:receive, channel:inbound:hold, channel:inbound:transfer)
- **CHAT_AGENT**: 3�?권한 (channel:chat:message, channel:chat:file, channel:chat:emoji)
- **MULTI_CHANNEL_AGENT**: 14�?권한 (모든 채널 권한)

**?�이???�시**:
```sql
-- ADMIN ??��??모든 권한 ?�당
('role-admin-001', 'perm-agent-001', NOW(), NULL),
('role-admin-001', 'perm-agent-002', NOW(), NULL),
('role-admin-001', 'perm-dept-001', NOW(), NULL),
...

-- TEAM_LEAD ??��???��? 권한 ?�당
('role-teamlead-001', 'perm-agent-002', NOW(), NULL),  -- agent:read
('role-teamlead-001', 'perm-agent-003', NOW(), NULL),  -- agent:update
('role-teamlead-001', 'perm-dept-002', NOW(), NULL),   -- dept:read
...
```

---

## ?�� 6. user_agent_roles (?�용????�� 매핑)

**목적**: ?�용?��? ??��???��???관�?(M:N) - ?�나???�용?�에�??�러 ??�� ?�당 가??

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식/?�시 |
|--------|------|------|------|----------------|
| **agent_id** | VARCHAR(50) | ??| ?�용??ID (PK, FK) | `agent-admin-001` |
| **role_id** | VARCHAR(50) | ??| ??�� ID (PK, FK) | `role-admin-001` |
| assigned_at | TIMESTAMP | ??| ?�당 ?�시 | `2026-01-21 10:00:00` |
| assigned_by | VARCHAR(50) | ??| ?�당??ID | Agent ID |

**PK**: `(agent_id, role_id)` (복합 PK)

**?�덱??*: 
- `idx_ar_agent`: `(agent_id)`
- `idx_ar_role`: `(role_id)`

**FK**: 
- `agent_id` ??`agents(agent_id)` ON DELETE CASCADE
- `role_id` ??`rbac_roles(role_id)` ON DELETE CASCADE

**?�용 ?�시**:
???�용?��? ?�러 ??��???�시??가�????�습?�다:
- POSITION ??�� 1�?+ CHANNEL ??�� N�?
- ?? `TEAM_LEAD` (직급) + `INBOUND_AGENT` (채널) + `CHAT_AGENT` (채널)

**?�이???�시**:
```sql
-- 관리자: ADMIN ??���?
('agent-admin-001', 'role-admin-001', NOW(), NULL),

-- ?�?? TEAM_LEAD + INBOUND_AGENT
('agent-lead-001', 'role-teamlead-001', NOW(), NULL),
('agent-lead-001', 'role-ch-inbound', NOW(), NULL),

-- ?�반 ?�담?? AGENT + INBOUND_AGENT + CHAT_AGENT (멀??채널)
('agent-001', 'role-agent-001', NOW(), NULL),
('agent-001', 'role-ch-inbound', NOW(), NULL),
('agent-001', 'role-ch-chat', NOW(), NULL)
```

---

## ?�� 초기 ?�이???�약

### ??�� (8�?
| ??���?| ?�??| ?�이???�코??| ?�명 |
|--------|------|---------------|------|
| ADMIN | POSITION | ADMIN | ?�스??관리자 (?�체 권한) |
| TEAM_LEAD | POSITION | TEAM_LEAD | ?�??(?� + ?�위 ?�근) |
| AGENT | POSITION | MEMBER | ?�반 ?�담??(본인 ?��? |
| INBOUND_AGENT | CHANNEL | NULL | ?�바?�드 ?�화 ?�담 |
| OUTBOUND_AGENT | CHANNEL | NULL | ?�웃바운???�화 ?�담 |
| CHAT_AGENT | CHANNEL | NULL | 채팅 ?�담 |
| EMAIL_AGENT | CHANNEL | NULL | ?�메???�담 |
| MULTI_CHANNEL_AGENT | CHANNEL | NULL | 멀?�채???�담 (모든 채널) |

### 권한 (31�?
| 카테고리 | 권한 ??| ?�시 |
|----------|---------|------|
| AGENT | 9�?| agent:create, agent:read, agent:update, agent:delete, agent:suspend, agent:activate, agent:transfer, agent:role:assign, agent:password:reset |
| DEPARTMENT | 6�?| dept:create, dept:read, dept:update, dept:delete, dept:move, dept:deactivate |
| RBAC | 6�?| role:create, role:read, role:update, role:delete, permission:read, permission:assign |
| CHANNEL | 10�?| channel:inbound:receive/hold/transfer (3), channel:outbound:call/campaign (2), channel:chat:message/file/emoji (3), channel:email:send/receive (2) |

### ?�플 ?�이??
**부??(4�?**:
```
?�스?�론 (COMPANY)
?��??� 고객?�비?�본부 (DIVISION)
    ?��??� ?�바?�드?� (TEAM)
    ?��??� ?�웃바운?��? (TEAM)
```

**?�용??(3�?**:
| 로그??ID | ?�름 | 부??| ??�� | 비�?번호 |
|-----------|------|------|------|----------|
| admin | 관리자 | ?�스?�론 | ADMIN | password123 |
| teamlead01 | 김?�??| 고객?�비?�본부 | TEAM_LEAD, INBOUND_AGENT | password123 |
| agent01 | ?�길??| ?�바?�드?� | AGENT, INBOUND_AGENT, CHAT_AGENT | password123 |

---

## ?�� 주요 쿼리 ?�시

### 1. ?�용?�의 모든 권한 조회 (계산??권한)
```sql
SELECT DISTINCT p.code, p.name, p.category
FROM user_agent_roles ar
JOIN rbac_role_permissions rp ON ar.role_id = rp.role_id
JOIN rbac_permissions p ON rp.permission_id = p.permission_id
WHERE ar.agent_id = 'agent-admin-001'
  AND ar.tenant_id = 'tenant-001'
ORDER BY p.category, p.code;
```

### 2. ??���?권한 ???�인
```sql
SELECT r.name AS role_name, r.type, COUNT(rp.permission_id) AS permission_count
FROM rbac_roles r
LEFT JOIN rbac_role_permissions rp ON r.role_id = rp.role_id
WHERE r.tenant_id = 'tenant-001'
GROUP BY r.role_id, r.name, r.type
ORDER BY r.type, r.name;
```

### 3. 부?�별 ?�용????(?�성 ?�용?�만)
```sql
SELECT d.name AS dept_name, COUNT(a.agent_id) AS agent_count
FROM org_departments d
LEFT JOIN agents a ON d.dept_id = a.dept_id AND a.status = 'ACTIVE'
WHERE d.tenant_id = 'tenant-001' AND d.is_active = TRUE
GROUP BY d.dept_id, d.name
ORDER BY d.org_path;
```

### 4. ?�위 부??조회 (Materialized Path ?�용)
```sql
SELECT dept_id, name, type, depth, org_path
FROM org_departments
WHERE tenant_id = 'tenant-001'
  AND org_path LIKE '/dept-root-001/%'
ORDER BY org_path;
```

### 5. ?�정 권한??가�??�용??찾기
```sql
SELECT DISTINCT a.login_id, a.name, a.email
FROM agents a
JOIN user_agent_roles ar ON a.agent_id = ar.agent_id
JOIN rbac_role_permissions rp ON ar.role_id = rp.role_id
JOIN rbac_permissions p ON rp.permission_id = p.permission_id
WHERE p.code = 'agent:delete'
  AND a.tenant_id = 'tenant-001'
  AND a.status = 'ACTIVE'
ORDER BY a.name;
```

---

## ?? ?�능 최적??

### ?�덱???�략
1. **복합 ?�니???�덱??*: ?�넌??격리 �?중복 방�?
   - `(tenant_id, login_id)` - agents
   - `(tenant_id, name)` - rbac_roles
   - `(tenant_id, code)` - rbac_permissions

2. **조회 ?�능 ?�덱??*:
   - `org_path` - ?�위 부??조회 최적??
   - `status` - ?�성 ?�용???�터�?
   - `type`, `is_active` - ??��/부???�?�별 조회

3. **FK ?�덱??*: JOIN ?�능 ?�상
   - `parent_dept_id`, `dept_id`, `role_id`, `permission_id`

### 쿼리 최적????
1. **Materialized Path**: `LIKE '/parent/%'`�??�위 부??빠르�?조회
2. **복합 PK**: 매핑 ?�이블에??중복 방�? �?빠른 조회
3. **ON DELETE CASCADE**: ??��/권한 ??�� ??매핑 ?�동 ?�리
4. **?��????�금**: `version` 컬럼?�로 ?�시???�어

---

## ?�� 마이그레?�션 가?�드

### v2.x ??v3.0.0 마이그레?�션

**1. ?�이블명 변�?*:
```sql
ALTER TABLE departmentEntities RENAME TO org_departments;
ALTER TABLE roles RENAME TO rbac_roles;
ALTER TABLE permissions RENAME TO rbac_permissions;
ALTER TABLE role_permissions RENAME TO rbac_role_permissions;
ALTER TABLE agent_roles RENAME TO user_agent_roles;
```

**2. 부???�??�??�태 컬럼 추�?**:
```sql
ALTER TABLE org_departments ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'TEAM';
ALTER TABLE org_departments ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE org_departments ADD CONSTRAINT chk_dept_type 
  CHECK (type IN ('COMPANY', 'DIVISION', 'TEAM', 'GROUP', 'CUSTOM'));
```

**3. ??�� ?�이블에 data_scope 추�?**:
```sql
ALTER TABLE rbac_roles ADD COLUMN data_scope VARCHAR(20);
UPDATE rbac_roles SET data_scope = 'ADMIN' WHERE type = 'POSITION' AND name = 'ADMIN';
UPDATE rbac_roles SET data_scope = 'TEAM_LEAD' WHERE type = 'POSITION' AND name = 'TEAM_LEAD';
UPDATE rbac_roles SET data_scope = 'MEMBER' WHERE type = 'POSITION' AND name IN ('AGENT', 'MEMBER');
```

---

**최종 ?�데?�트**: 2026-02-05  
**버전**: v3.0.0  
**?�성??*: Identity Modulith Development Team
- PHONE_AGENT: 3�?
- CHAT_AGENT: 2�?
- EMAIL_AGENT: 1�?
- SUPERVISOR: 15�?

---

## ?�� 6. agent_roles (?�용????�� 매핑)

**목적**: ?�용?��? ??��???��???관�?

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식/?�시 |
|--------|------|------|------|----------------|
| **id** | BIGSERIAL | ??| 매핑 ID (PK) | 1, 2, 3... |
| agent_id | VARCHAR(36) | ??| ?�용??ID (FK) | UUID |
| role_id | VARCHAR(36) | ??| ??�� ID (FK) | UUID |
| assigned_at | TIMESTAMP | ??| ?�당 ?�시 | `2026-01-21 10:00:00` |

**?�덱??*: `(agent_id, role_id)` UK, `agent_id`, `role_id`  
**FK**: `agent_id` ??`agents(agent_id)` ON DELETE CASCADE  
**FK**: `role_id` ??`roles(role_id)` ON DELETE CASCADE

**?�� ?�용?�는 ?�러 ??��???�시??가�????�습?�다**:
- ?? `TEAM_LEAD` + `INBOUND_AGENT` = ?�?�이면서 ?�바?�드 ?�화 ?�담??가??
- ?? `AGENT` + `INBOUND_AGENT` + `CHAT_AGENT` = 멀??채널 ?�담??

---

## ?�� ?�이�?�?관계도

```
org_departments (부??
    ??1:N (parent_dept_id)
org_departments (?�위 부??
    ??1:N (dept_id)
user_agents (?�용??
    ??M:N (user_agent_roles)
rbac_roles (??��)
    ??M:N (rbac_role_permissions)
rbac_permissions (권한)
```

---

## ?? 초기??방법

### 1. ?�플리�??�션 ?�행 (권장)
```bash
./gradlew bootRun
```

Flyway가 ?�동?�로 `V1_0_0__Complete_Init.sql` ?�행 ??6�??�이�?+ ?��? ?�이???�성

### 2. ?�동 초기??(?�요 ??
```bash
# PostgreSQL ?�라?�언?�에??
psql -U your_user -d your_database -f src/main/resources/db/migration/V1_0_0__Complete_Init.sql
```

### 3. ?�인
```sql
SELECT 'org_departments' as table_name, COUNT(*) FROM org_departments
UNION ALL SELECT 'agents', COUNT(*) FROM agents
UNION ALL SELECT 'rbac_roles', COUNT(*) FROM rbac_roles
UNION ALL SELECT 'rbac_permissions', COUNT(*) FROM rbac_permissions
UNION ALL SELECT 'role_permissions', COUNT(*) FROM role_permissions
UNION ALL SELECT 'agent_roles', COUNT(*) FROM agent_roles;
```

**?�상 결과**: 16부?? 16?�용?? 8??��, 35권한, 77매핑, 22?�당

---

**문서 버전**: 2.0.0 CLEAN  
**최종 ?�데?�트**: 2026-01-21


### ?�� ?�심 ?�계 ?�칙

1. **UUID 기반 ?�별??*: 모든 ?�티?�는 UUID 문자??(VARCHAR(36)) ?�용
2. **멀?�테?�시**: 모든 ?�이블에 `tenant_id` 컬럼 ?�함
3. **Soft Delete**: ??��(`roles`)?� `is_active` ?�래그로 ?�리????��
4. **감사 추적**: 모든 권한 변경사??? `audit_logs`??기록
5. **계층 구조**: 부?�는 ?�기참조 + org_path�??�리 구현

---

## 2. ?�이�??�세 명세

### ?�� 2.1 departmentEntities (조직/부??

**목적**: 조직 계층 구조 관�?(?�리 구조)

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식 |
|--------|------|------|------|-----------|
| **dept_id** | VARCHAR(36) | NOT NULL | 부??ID (PK) | UUID ?�식 (`550e8400-...`) |
| tenant_id | VARCHAR(50) | NOT NULL | ?�넌??ID | `tenant-001` ~ `tenant-999` |
| parent_id | VARCHAR(36) | NULL | ?�위 부??ID (FK) | NULL = 최상?? UUID = ?�위 부??|
| name | VARCHAR(100) | NOT NULL | 부?�명 | ?��?/?�문, 2-100??|
| org_path | VARCHAR(500) | NOT NULL | 조직 경로 | `/루트ID/부?�ID` ?�식 |
| depth | INTEGER | NOT NULL | ?�리 깊이 | 0(최상?? ~ 10(최�?) |
| type | VARCHAR(50) | NULL | 부???�??| `본�?`, `?�`, `?�트`, `?? ??|
| created_at | TIMESTAMP | NOT NULL | ?�성 ?�시 | `2026-01-20 10:30:00` |

**?�덱??*:
- UK: `(tenant_id, org_path)` - 경로 중복 방�?
- IDX: `tenant_id`, `parent_id`, `org_path`

**FK**:
- `parent_id` ??`departmentEntities(dept_id)` ON DELETE RESTRICT

**?�이???�시**:
```sql
-- 본�? (최상??
('d0000000-0000-0000-0000-000000000001', 'tenant-001', NULL, 
 '경영지?�본부', '/d0000000-0000-0000-0000-000000000001', 0, '본�?', NOW())

-- ?� (?�위)
('d0000000-0000-0000-0000-000000000011', 'tenant-001', 
 'd0000000-0000-0000-0000-000000000001', '?�사?�', 
 '/d0000000-0000-0000-0000-000000000001/d0000000-0000-0000-0000-000000000011', 
 1, '?�', NOW())
```

---

### ?�� 2.2 user_agents (?�용???�담??

**목적**: ?�스???�용???�보 관�?

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식 |
|--------|------|------|------|-----------|
| **agent_id** | VARCHAR(36) | NOT NULL | ?�용??ID (PK) | UUID ?�식 |
| tenant_id | VARCHAR(50) | NOT NULL | ?�넌??ID | `tenant-001` |
| login_id | VARCHAR(100) | NOT NULL | 로그??ID (UK) | ?�문+?�자, 4-20??|
| password | VARCHAR(255) | NOT NULL | 비�?번호 | BCrypt ?�시 (`$2a$10$...`) |
| name | VARCHAR(100) | NOT NULL | ?�용?�명 | ?��?/?�문, 2-50??|
| dept_id | VARCHAR(36) | NULL | ?�속 부??ID (FK) | UUID ?�는 NULL |
| status | VARCHAR(20) | NOT NULL | ?�태 | `ACTIVE`, `RETIRED` |
| password_must_change | BOOLEAN | NULL | 비�?번호 변�??�요 | `true`, `false` |
| created_at | TIMESTAMP | NOT NULL | ?�성 ?�시 | `2026-01-20 10:30:00` |
| updated_at | TIMESTAMP | NULL | ?�정 ?�시 | `2026-01-20 15:00:00` |
| retired_at | TIMESTAMP | NULL | ?�직 ?�시 | `2025-12-31 23:59:59` |
| job_title | VARCHAR(100) | NULL | 직책 | `?��?, `과장`, `?�?? ??|
| sync_status | VARCHAR(20) | NULL | ?�기???�태 | `SYNCED`, `PENDING` (Keycloak ?�동?? |
| role_id | VARCHAR(50) | NULL | ??�� ID (?�거?? | ?�용 중단 ?�정 |

**?�덱??*:
- UK: `login_id`
- IDX: `tenant_id`, `dept_id`, `status`, `login_id`

**FK**:
- `dept_id` ??`departmentEntities(dept_id)` ON DELETE SET NULL

**?�이???��?**:
- **login_id**: ?�문??+ ?�자 조합 (`admin`, `hong123`, `kim_gd`)
- **password**: BCrypt ?�시�??�??(?�문 ?�??금�?)
- **status**: `ACTIVE`(?�성), `RETIRED`(?�직) �??�용
- **name**: ?�명 ?�용 권장

---

### ?�� 2.3 roles (??��)

**목적**: RBAC ??�� ?�의

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식 |
|--------|------|------|------|-----------|
| **role_id** | VARCHAR(36) | NOT NULL | ??�� ID (PK) | UUID ?�식 |
| tenant_id | VARCHAR(50) | NOT NULL | ?�넌??ID | `tenant-001` |
| name | VARCHAR(64) | NOT NULL | ??���?(UK) | ?�문자+?�더?�코?? 2-64??|
| type | VARCHAR(32) | NOT NULL | ??�� ?�??| `POSITION`, `CHANNEL`, `SKILL` |
| description | VARCHAR(255) | NULL | ??�� ?�명 | 목적 �?권한 범위 ?�명 |
| is_active | BOOLEAN | NOT NULL | ?�성???�태 | `true`(?�성), `false`(비활?? |
| version | BIGINT | NOT NULL | ?��????�금 버전 | 0부???�작, ?�정 ??+1 |
| created_at | TIMESTAMP | NOT NULL | ?�성 ?�시 | `2026-01-20 10:30:00` |
| updated_at | TIMESTAMP | NOT NULL | ?�정 ?�시 | `2026-01-20 15:00:00` |

**?�덱??*:
- UK: `(tenant_id, name)`
- IDX: `tenant_id`, `is_active`

**??�� ?�??(type)**:
- **POSITION**: 직급 기반 (?? `ADMIN`, `TEAM_LEADER`, `MEMBER`)
- **CHANNEL**: 채널 기반 (?? `INBOUND`, `OUTBOUND`, `CHAT`)
- **SKILL**: ?�킬 기반 (?? `VIP_SUPPORT`, `TECHNICAL_SUPPORT`)

**??���?(name) ?��?**:
```
- ?�체 관리자: ADMIN
- ?�?? TEAM_LEADER
- ?�반 ?�담?? AGENT
- ?�바?�드 ?�담: INBOUND_AGENT
- ?�웃바운???�담: OUTBOUND_AGENT
- 채팅 ?�담: CHAT_AGENT
- VIP ?�담: VIP_AGENT
- 기술 지?? TECH_SUPPORT
```

---

### ?�� 2.4 permissions (권한)

**목적**: RBAC 권한 ?�의

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식 |
|--------|------|------|------|-----------|
| **permission_id** | VARCHAR(36) | NOT NULL | 권한 ID (PK) | UUID ?�식 |
| tenant_id | VARCHAR(50) | NOT NULL | ?�넌??ID | `tenant-001` |
| code | VARCHAR(128) | NOT NULL | 권한 코드 (UK) | `domain:action` ?�식 |
| created_at | TIMESTAMP | NOT NULL | ?�성 ?�시 | `2026-01-20 10:30:00` |

**?�덱??*:
- UK: `(tenant_id, code)`
- IDX: `tenant_id`

**권한 코드 (code) ?��?**:

?�식: `{domain}:{action}`

**?�메??(domain)**:
- `user`: ?�용??관�?
- `org`: 조직 관�?
- `role`: ??�� 관�?
- `permission`: 권한 관�?
- `agent_role`: ?�용????�� ?�당 관�?
- `audit`: 감사 로그 조회

**?�션 (action)**:
- `create`: ?�성
- `read`: 조회
- `read:self`: 본인�?조회
- `update`: ?�정
- `update:self`: 본인�??�정
- `delete`: ??��
- `manage`: ?�체 관�?
- `assign`: ?�당
- `view`: 보기

**?��? 권한 코드 ?�시**:
```
user:create          - ?�용???�성
user:read            - 모든 ?�용??조회
user:read:self       - 본인 ?�보�?조회
user:update          - ?�용???�보 ?�정
user:delete          - ?�용????��
user:manage          - ?�용???�체 관�?
user:assign:role     - ?�용?�에�???�� ?�당
org:view             - 조직??보기
org:create           - 부???�성
org:update           - 부???�보 ?�정
org:move             - 부???�동
org:delete           - 부????��
role:create          - ??�� ?�성
role:read            - ??�� 조회
role:update          - ??�� ?�정
role:delete          - ??�� ??��
role:assign          - ??��??권한 ?�당
permission:create    - 권한 ?�성
permission:read      - 권한 조회
audit:view           - 감사 로그 조회
```

---

### ?�� 2.5 role_permissions (??��-권한 매핑)

**목적**: ??���?권한???��???관�?

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식 |
|--------|------|------|------|-----------|
| **id** | BIGSERIAL | NOT NULL | 매핑 ID (PK) | ?�동 증�? |
| role_id | VARCHAR(36) | NOT NULL | ??�� ID (FK) | UUID ?�식 |
| permission_id | VARCHAR(36) | NOT NULL | 권한 ID (FK) | UUID ?�식 |
| assigned_at | TIMESTAMP | NOT NULL | ?�당 ?�시 | `2026-01-20 10:30:00` |

**?�덱??*:
- UK: `(role_id, permission_id)` - 중복 ?�당 방�?

**FK**:
- `role_id` ??`roles(role_id)` ON DELETE CASCADE
- `permission_id` ??`permissions(permission_id)` ON DELETE CASCADE

---

### ?�� 2.6 agent_roles (?�용????�� 매핑)

**목적**: ?�용?��? ??��???��???관�?

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식 |
|--------|------|------|------|-----------|
| **id** | BIGSERIAL | NOT NULL | 매핑 ID (PK) | ?�동 증�? |
| agent_id | VARCHAR(36) | NOT NULL | ?�용??ID (FK) | UUID ?�식 |
| role_id | VARCHAR(36) | NOT NULL | ??�� ID (FK) | UUID ?�식 |
| assigned_at | TIMESTAMP | NOT NULL | ?�당 ?�시 | `2026-01-20 10:30:00` |

**?�덱??*:
- UK: `(agent_id, role_id)` - 중복 ?�당 방�?
- IDX: `agent_id`, `role_id`

**FK**:
- `agent_id` ??`agents(agent_id)` ON DELETE CASCADE
- `role_id` ??`roles(role_id)` ON DELETE CASCADE

---

### ?�� 2.7 audit_logs (감사 로그)

**목적**: 권한 관??모든 변경사??추적

| 컬럼�?| ?�??| NULL | ?�명 | ?��? ?�식 |
|--------|------|------|------|-----------|
| **audit_id** | VARCHAR(36) | NOT NULL | 감사 로그 ID (PK) | UUID ?�식 |
| tenant_id | VARCHAR(50) | NOT NULL | ?�넌??ID | `tenant-001` |
| action | VARCHAR(32) | NOT NULL | ?�업 ?�형 | `CREATE`, `UPDATE`, `DELETE`, `ASSIGN`, `REVOKE` |
| resource_type | VARCHAR(64) | NOT NULL | ?�??리소???�??| `ROLE`, `PERMISSION`, `AGENT_ROLE` |
| resource_id | VARCHAR(255) | NOT NULL | ?�??리소??ID | UUID ?�는 복합 ID |
| operator_id | VARCHAR(255) | NOT NULL | ?�업 ?�행??ID | ?�용??UUID |
| changes | TEXT | NULL | 변�??�용 | JSON ?�식 |
| timestamp | TIMESTAMP | NOT NULL | ?�업 ?�시 | `2026-01-20 10:30:00.123` |
| remarks | TEXT | NULL | 추�? ?�보 | 메모, ?�패 ?�인 ??|
| ip_address | VARCHAR(45) | NULL | ?�라?�언??IP | `192.168.1.100`, IPv6 ?�함 |

**?�덱??*:
- IDX: `tenant_id`, `resource_type`, `operator_id`, `timestamp DESC`

**?�업 ?�형 (action) ?��?**:
- `CREATE`: ?�성 (??��, 권한)
- `UPDATE`: ?�정
- `DELETE`: ??��
- `ASSIGN`: ?�당 (??��-권한, ?�용????��)
- `REVOKE`: ?�수

**리소???�??(resource_type) ?��?**:
- `ROLE`: ??��
- `PERMISSION`: 권한
- `ROLE_PERMISSION`: ??��-권한 매핑
- `AGENT_ROLE`: ?�용????�� 매핑

**변�??�용 (changes) JSON ?�식**:
```json
// ??�� ?�성
{"roleName": "TEAM_LEADER", "roleType": "POSITION"}

// ??�� ?�정
{"old": {"isActive": true}, "new": {"isActive": false}}

// ??��-권한 ?�당
{"roleId": "uuid-role", "permissionId": "uuid-perm", "permissionCode": "user:create"}

// ?�용????�� ?�당
{"agentId": "uuid-agent", "roleId": "uuid-role", "roleName": "ADMIN"}
```

---

### ?���?2.8 audit_logs_archive (감사 로그 ?�카?�브)

**목적**: 6개월 ?�상 ?�래??감사 로그 보�?

| 컬럼�?| ?�??| NULL | ?�명 |
|--------|------|------|------|
| audit_id ~ ip_address | (audit_logs?� ?�일) | | |
| archived_at | TIMESTAMP | NOT NULL | ?�카?�브 ?�시 |

**?�이???�동**:
- 매월 1???�정 ?�동 ?�동 (AuditLogArchivingBatchService)
- 6개월 ?�전 ?�이???�??

---

## 3. ?�이???��???규칙

### ?�� 3.1 UUID ?�성 규칙

**?�식**: `8-4-4-4-12` (�?36?? ?�이???�함)
**?�시**: `550e8400-e29b-41d4-a716-446655440001`

**?�성 방법**:
```java
// Java
UUID.randomUUID().toString()

// PostgreSQL
gen_random_uuid()::text
```

### ?���?3.2 ?�넌??ID 규칙

**?�식**: `tenant-{?�자 3?�리}`
**?�시**: `tenant-001`, `tenant-002`
**범위**: `tenant-001` ~ `tenant-999`

### ?�� 3.3 ?�용??로그??ID 규칙

**?�식**: ?�문 ?�문??+ ?�자 + ?�더?�코??
**길이**: 4-20??
**?�시**: `admin`, `hong123`, `kim_gd`, `team_leader`
**금�?**: ?�수문자 (@, #, $ ??, 공백, ?��?

### ?�� 3.4 비�?번호 규칙

**?�??*: BCrypt ?�시�??�??
**?�식**: `$2a$10$...` (60??
**Java ?�성**:
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashed = encoder.encode("?�본비�?번호");
```

### ?�� 3.5 ??���?규칙

**?�식**: ?�문자 + ?�더?�코??
**길이**: 2-64??
**?�시**: `ADMIN`, `TEAM_LEADER`, `INBOUND_AGENT`
**금�?**: ?�문?? 공백, ?�수문자

### ?�� 3.6 권한 코드 규칙

**?�식**: `{domain}:{action}`
**domain**: ?�문?? ?�더?�코???�용
**action**: ?�문?? ?�더?�코???�용, 콜론(`:`) ?�중 ?�용
**?�시**: `user:create`, `org:read:team`, `role:assign`

### ?�� 3.7 ?�짜/?�간 규칙

**?�??*: `TIMESTAMP WITHOUT TIME ZONE`
**?�식**: `YYYY-MM-DD HH:MI:SS`
**?�시**: `2026-01-20 10:30:00`
**기본�?*: `NOW()` ?�는 `CURRENT_TIMESTAMP`

---

## 4. ?�이�?�?관계도

```
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
?? departmentEntities    ???��??�?�?�?�??
?? (조직 계층)     ??      ???�기참조 (parent_id)
?��??�?�?�?�?�?�?�?��??�?�?�?�?�?�?�??      ??
         ??               ??
         ??FK: dept_id    ??
         ??               ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??      ??
??    agents      ??      ??
??  (?�용??       ??      ??
?��??�?�?�?�?�?�?�?��??�?�?�?�?�?�?�??      ??
         ??               ??
         ??FK: agent_id   ??
         ??               ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??      ??
?? agent_roles    ?�◄?�?�?�?�?�?�??
?? (?��???매핑)   ??
?��??�?�?�?�?�?�?�?��??�?�?�?�?�?�?�??
         ??FK: role_id
         ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
??    roles       ??
??  (??��)        ??
?��??�?�?�?�?�?�?�?��??�?�?�?�?�?�?�??
         ??FK: role_id
         ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
?�role_permissions ??
?? (?��???매핑)   ??
?��??�?�?�?�?�?�?�?��??�?�?�?�?�?�?�??
         ??FK: permission_id
         ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
?? permissions    ??
??  (권한)        ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??

?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
?? audit_logs     ???�?�6개월 ?��??�??audit_logs_archive
?? (감사 로그)     ??              (?�카?�브)
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
```

**CASCADE 규칙**:
- `role_permissions`: role ??�� ??매핑????��
- `agent_roles`: agent ?�는 role ??�� ??매핑????��

**SET NULL 규칙**:
- `agents.dept_id`: departmentEntity ??�� ??NULL�?변�?

**RESTRICT 규칙**:
- `departmentEntities.parent_id`: ?�위 부??존재 ????�� 불�?

---

## 5. ?��? ?�이???�시

### ?�� 5.1 초기 ?�이?�셋 구성

**마이그레?�션 ?�크립트**: `V1_0_9__Insert_Standard_Data.sql`

```
??조직 구조 (3?�계 계층):
   - 본�? 3�?
   - ?� 9�? 
   - ?�트 6�?
   - �?18�?부??

???�용??(16�?:
   - ?�성 ?�용??15�?
   - ?�직 ?�용??1�?

??권한 (35�?:
   - user: 9�?
   - org: 5�?
   - role: 7�?
   - permission: 4�?
   - agent_role: 4�?
   - audit: 6�?

????�� (8�?:
   - ADMIN (최고 관리자)
   - TEAM_LEADER (?�??
   - AGENT (?�반 ?�담??
   - INBOUND_AGENT (?�바?�드)
   - OUTBOUND_AGENT (?�웃바운??
   - CHAT_AGENT (채팅 ?�담)
   - VIP_AGENT (VIP ?�담)
   - TECH_SUPPORT (기술 지??

????��-권한 매핑 (77�?
???�용????�� 매핑 (18�?
```

### ?�� 5.2 조직 구조 ?�시

```sql
-- 최상??(본�?)
('d0000000-0000-0000-0000-000000000001', 'tenant-001', NULL,
 '경영지?�본부', '/d0000000-0000-0000-0000-000000000001', 0, '본�?', NOW())

-- 2?�계 (?�)
('d0000000-0000-0000-0000-000000000011', 'tenant-001',
 'd0000000-0000-0000-0000-000000000001',
 '?�사?�', '/d0000000-0000-0000-0000-000000000001/d0000000-0000-0000-0000-000000000011',
 1, '?�', NOW())

-- 3?�계 (?�트)
('d0000000-0000-0000-0000-000000000111', 'tenant-001',
 'd0000000-0000-0000-0000-000000000011',
 '채용?�트', '/d0000000-0000-0000-0000-000000000001/d0000000-0000-0000-0000-000000000011/d0000000-0000-0000-0000-000000000111',
 2, '?�트', NOW())
```

### ?�� 5.3 ?�용???�이???�시

```sql
INSERT INTO agents VALUES
-- 최고 관리자
('a0000000-0000-0000-0000-000000000001', 'tenant-001', 'admin',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password: admin123
 '?�스??관리자', NULL, 'ACTIVE', false, NOW(), NULL, NULL, '?�스??관리자', NULL, NULL),

-- ?�??
('a0000000-0000-0000-0000-000000000002', 'tenant-001', 'teamlead01',
 '$2a$10$...', '김?�??, 'd0000000-0000-0000-0000-000000000011',
 'ACTIVE', false, NOW(), NULL, NULL, '?�??, NULL, NULL),

-- ?�반 ?�담??
('a0000000-0000-0000-0000-000000000003', 'tenant-001', 'agent01',
 '$2a$10$...', '?�상??, 'd0000000-0000-0000-0000-000000000021',
 'ACTIVE', false, NOW(), NULL, NULL, '?��?, NULL, NULL);
```

### ?�� 5.4 ??��-권한 매핑 ?�시

```sql
-- ADMIN ??��??모든 권한 ?�당
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT 'r0000000-0000-0000-0000-000000000001', permission_id, NOW()
FROM permissions WHERE tenant_id = 'tenant-001';

-- TEAM_LEADER ??��???� 관�?권한 ?�당
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT 'r0000000-0000-0000-0000-000000000002', permission_id, NOW()
FROM permissions 
WHERE tenant_id = 'tenant-001'
  AND code IN ('user:read', 'org:view', 'org:update');
```

### ?�� 5.5 ?�용????�� 매핑 ?�시

```sql
-- admin ?�용?�에�?ADMIN ??�� ?�당
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('a0000000-0000-0000-0000-000000000001', 'r0000000-0000-0000-0000-000000000001', NOW());

-- teamlead01 ?�용?�에�?TEAM_LEADER ??�� ?�당
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('a0000000-0000-0000-0000-000000000002', 'r0000000-0000-0000-0000-000000000002', NOW());

-- ?�중 ??�� ?�당 ?�시 (?�담??+ VIP ?�담)
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('a0000000-0000-0000-0000-000000000003', 'r0000000-0000-0000-0000-000000000003', NOW()),
('a0000000-0000-0000-0000-000000000003', 'r0000000-0000-0000-0000-000000000007', NOW());
```

---

## ?�� 부�? ?�용??SQL 쿼리

### A. 조직???�체 조회 (계층 구조)
```sql
WITH RECURSIVE org_tree AS (
  SELECT dept_id, name, parent_id, 0 AS level, name AS path
  FROM departmentEntities
  WHERE tenant_id = 'tenant-001' AND parent_id IS NULL
  
  UNION ALL
  
  SELECT d.dept_id, d.name, d.parent_id, o.level + 1,
         o.path || ' > ' || d.name
  FROM departmentEntities d
  INNER JOIN org_tree o ON d.parent_id = o.dept_id
)
SELECT * FROM org_tree ORDER BY path;
```

### B. ?�용?�별 권한 조회
```sql
SELECT a.login_id, a.name, r.name AS role_name, p.code AS permission_code
FROM agents a
JOIN agent_roles ar ON a.agent_id = ar.agent_id
JOIN roles r ON ar.role_id = r.role_id
JOIN role_permissions rp ON r.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE a.tenant_id = 'tenant-001'
  AND a.status = 'ACTIVE'
ORDER BY a.login_id, r.name, p.code;
```

### C. 감사 로그 조회 (최근 7??
```sql
SELECT audit_id, action, resource_type, operator_id, timestamp, changes
FROM audit_logs
WHERE tenant_id = 'tenant-001'
  AND timestamp >= NOW() - INTERVAL '7 days'
ORDER BY timestamp DESC
LIMIT 100;
```

### D. 부?�별 ?�원 집계
```sql
SELECT d.name AS dept_name, COUNT(a.agent_id) AS agent_count
FROM departmentEntities d
LEFT JOIN agents a ON d.dept_id = a.dept_id AND a.status = 'ACTIVE'
WHERE d.tenant_id = 'tenant-001'
GROUP BY d.dept_id, d.name
ORDER BY d.org_path;
```

---

## ?�� 중요 참고?�항

### ?�️ 주의?�항

1. **UUID ?��???*: 모든 ?�티??ID??UUID (VARCHAR(36)) ?�용
2. **?�넌??격리**: 모든 쿼리??`tenant_id` 조건 ?�수
3. **Soft Delete**: ??��?� `is_active = false`�??�리????��
4. **CASCADE 주의**: ??��/권한 ??�� ??매핑 ?�이�??�동 ??��??
5. **감사 로그**: 모든 권한 변경�? ?�동?�로 `audit_logs`??기록

### ?�� 체크리스??

?�로?�션 배포 ???�인:
- [ ] 모든 FK ?�약조건 ?�인
- [ ] ?�덱???�능 ?�스??
- [ ] ?�넌??격리 검�?
- [ ] 감사 로그 ?�카?�빙 ?��?�??�정
- [ ] 백업 ?�책 ?�립

---

## ?�� 관??문서

- [AUDIT_AND_CONSTANTS_ANALYSIS.md](./AUDIT_AND_CONSTANTS_ANALYSIS.md) - 감사 로그 & ?�수 분석
- [V1_0_0__Complete_Init.sql](./src/main/resources/db/migration/V1_0_0__Complete_Init.sql) - DB 초기???�크립트
- [V1_0_9__Insert_Standard_Data.sql](./src/main/resources/db/migration/V1_0_9__Insert_Standard_Data.sql) - ?��? ?�이???�입

---

**문서 버전**: 2.0
**최종 검증일**: 2026-01-20
**?�성??*: Identity Modulith Team

> **목적**: ?�이?�베?�스 ?�계, ?�이�?구조, ?��? ?�이?��? ??곳에???�인  
> **?�??*: 개발?�, ?�영?�  
> **버전**: 2.0  
> **최종 ?�정??*: 2026-01-16

---

## ?�� 목차
1. [?�이?�베?�스 개요](#?�이?�베?�스-개요)
2. [?�이�?구조](#?�이�?구조)
3. [?�이�??�세 명세](#?�이�??�세-명세)
4. [?�이�?�??��?관�?(#?�이�?�??��?관�?
5. [컬럼 ?�이???�식 ?��?](#컬럼-?�이???�식-?��?)
6. [?��? ?�이??가?�드](#?��?-?�이??가?�드)
7. [권한 �???�� ?��?](#권한-�???��-?��?)

---

## ?�이?�베?�스 개요

### ?�계 목표
- **멀?�테?�시(Multi-Tenancy)**: �??�이블에 tenant_id�??�이??격리
- **UUID ?�일**: 모든 ?�티??ID??UUID (VARCHAR(36))�??�일
- **조직 ?�리**: ?�기참조�??�용??부??계층 구조
- **RBAC**: ??�� 기반 ?�근 ?�어 (Role-Based Access Control)
- **감사 추적**: 모든 ?�이블에 created_at, 주요 ?�업?� audit_logs�?기록

### ?�심 ?�칙
```
??ID ?�?? UUID (VARCHAR(36)) ?�일
???��???관�? 중간 ?�이블로 명시??관�?
???�기참조: departments??parent_id
??Soft Delete: agents??status?� retired_at
???�이??격리: 모든 ?�이블에 tenant_id (NOT NULL)
```

---

## ?�이�?구조

### ?�체 ?�이�?목록 (6�?+ 2�?

| ?�이�?| 모듈 | ?�도 | PK ?�??| 참고 |
|--------|------|------|---------|------|
| **departmentEntities** | Organization | 조직/부??계층 | VARCHAR(36) | ?�기참조 ?�리 |
| **agents** | User | ?�용??직원 | VARCHAR(36) | Soft Delete |
| **roles** | RBAC | ??�� ?�의 | VARCHAR(36) | 권한 묶음 |
| **permissions** | RBAC | 권한 ?�의 | VARCHAR(36) | 최소 ?�위 권한 |
| **role_permissions** | RBAC | ??��-권한 매핑 | BIGSERIAL | N:M 중간 ?�이�?|
| **agent_roles** | RBAC | ?�용????�� 매핑 | BIGSERIAL | N:M 중간 ?�이�?|
| **audit_logs** | Audit | 감사 로그 | BIGSERIAL | 변�??�력 추적 |
| **audit_archive** | Audit | 감사 로그 ?�카?�브 | BIGSERIAL | 90???�상 로그 |

### ERD (Entity Relationship Diagram)

```
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
??              Identity Modulith Database ERD                 ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??

?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
??  departmentEntities    ??(?�기참조)
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
??PK: dept_id (U)  ??
??    tenant_id    ??
??FK: parent_id ?�?�?�?��??�?�?�?�??
??    name         ??    ??
??    org_path     ??    ??
??    depth        ??    ??
??    type         ??    ??
??    created_at   ??    ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??    ??
         ??              ??
         ??1:N (?�기참조)|
         ?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�??
         ??
         ??1:N (?�속)
         ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
??    agents       ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
??PK: agent_id (U) ??
??    tenant_id    ??
??    login_id (U) ??
??    password     ??
??    name         ??
??FK: dept_id ?�?�?�?�?�??
??    status       ??
??    ...etc       ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
         ??
         ??N:M (??�� ?�당)
         ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??      ?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
??  agent_roles    ??      ??     roles       ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??      ?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
??PK: id           ??      ??PK: role_id (U)  ??
??FK: agent_id ?�?�?�?�?��??�?�?�?�?�?�│     tenant_id    ??
??FK: role_id ?�?�?�?�?�?��??�?�?�?�?�?�│     name (U)     ??
??    assigned_at  ??      ??    type         ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??      ??    created_at   ??
                           ?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
                                   ??
                                   ??N:M
                                   ??
                           ?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
                           ??role_permissions ??
                           ?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
                           ??PK: id           ??
                           ??FK: role_id ?�?�?�?�?�??
                           ??FK: permission_id??
                           ??    assigned_at  ??
                           ?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
                                   ??
                                   ??
                           ?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
                           ??  permissions    ??
                           ?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??
                           ??PK: permission_id??
                           ??    tenant_id    ??
                           ??    code (U)     ??
                           ??    created_at   ??
                           ?��??�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�??

범�?: PK=Primary Key, FK=Foreign Key, U=UUID, N:M=?��???
```

---

## ?�이�??�세 명세

### 1. departmentEntities (조직/부???�이�?

**목적**: ?�사 조직 계층 구조 관�?(?�리 구조)

| 컬럼�?| ?�??| ?�약 | ?�명 |
|--------|------|------|------|
| dept_id | VARCHAR(36) | PK | 부??ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | ?�넌??ID (멀?�테?�시) |
| parent_id | VARCHAR(36) | FK (?�기참조) | ?�위 부??ID (NULL?�면 최상?? |
| name | VARCHAR(100) | NOT NULL | 부?�명 |
| org_path | VARCHAR(500) | NOT NULL, UNIQUE | 조직 경로 (?? /dept1/dept2/dept3) |
| depth | INTEGER | NOT NULL | ?�리 깊이 (0부???�작) |
| type | VARCHAR(50) | | 부???�??(HEADQUARTERS, DIVISION, TEAM) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ?�성 ?�시 |

**?�덱??*:
- PK: dept_id
- UK: (tenant_id, org_path)
- FK: parent_id ??dept_id (?�기참조, ON DELETE RESTRICT)
- IDX: (tenant_id), (parent_id), (org_path)

**?�징**:
- **?�기참조 (Self-Join)**: parent_id�??�하 관�??�현
- **Closure Table ?�??*: org_path�?계층 ?�색 최적??
- **??�� ?�약**: RESTRICT�??�위 부???�으�???�� 불�?

**?�시 ?�이??*:
```sql
dept_id              | name        | parent_id | org_path          | depth | type
---------------------|-------------|-----------|-------------------|-------|-------------
d50e8400-e29b-...001 | ?�스?�론본�? | NULL      | /d50e...001       | 0     | HEADQUARTERS
d50e8400-e29b-...002 | 고객지?�사부 | ...001    | /d50e...001/002   | 1     | DIVISION
d50e8400-e29b-...005 | ?�화?�담?�  | ...002    | /d50e...001/002/005 | 2   | TEAM
```

---

### 2. user_agents (?�용??직원 ?�이�?

**목적**: ?�스???�용???�보 관�?

| 컬럼�?| ?�??| ?�약 | ?�명 |
|--------|------|------|------|
| agent_id | VARCHAR(36) | PK | ?�용??ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | ?�넌??ID |
| login_id | VARCHAR(100) | NOT NULL, UNIQUE | 로그??ID |
| password | VARCHAR(255) | NOT NULL | 비�?번호 (BCrypt ?�시) |
| name | VARCHAR(100) | NOT NULL | ?�용?�명 |
| dept_id | VARCHAR(36) | FK | ?�속 부??ID (NULL 가?? |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ?�태 (ACTIVE, RETIRED) |
| password_must_change | BOOLEAN | DEFAULT false | 비�?번호 변�??�요 ?��? |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ?�성 ?�시 |
| updated_at | TIMESTAMP | | ?�정 ?�시 |
| retired_at | TIMESTAMP | | ?�직 ?�시 |
| job_title | VARCHAR(100) | | 직책 |
| sync_status | VARCHAR(20) | | ?�기 ?�태 |

**?�덱??*:
- PK: agent_id
- UK: login_id
- FK: dept_id ??departmentEntities.dept_id (ON DELETE SET NULL)
- IDX: (tenant_id), (dept_id), (status), (login_id)

**?�징**:
- **Soft Delete**: status='RETIRED'�??�리????�� (물리????�� X)
- **?�중 ??��**: agent_roles ?�이블로 ?�러 ??�� ?�당 가??
- **부???�결**: dept_id�?조직 구조?� ?�결

**?�시 ?�이??*:
```sql
agent_id             | login_id    | name      | dept_id      | status
---------------------|-------------|-----------|--------------|--------
550e8400-e29b-...101 | admin       | ?�스?��?리자 | d50e...001  | ACTIVE
550e8400-e29b-...104 | phone_ag01 | 박상??    | d50e...005  | ACTIVE
550e8400-e29b-...199 | retired_usr | ?�직??   | d50e...005  | RETIRED
```

---

### 3. roles (??�� ?�이�?

**목적**: RBAC ??�� ?�의 (권한 묶음)

| 컬럼�?| ?�??| ?�약 | ?�명 |
|--------|------|------|------|
| role_id | VARCHAR(36) | PK | ??�� ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | ?�넌??ID |
| name | VARCHAR(64) | NOT NULL, UNIQUE | ??���?(ADMIN, MANAGER ?? |
| type | VARCHAR(32) | NOT NULL | ??�� ?�??(POSITION, CHANNEL, SKILL) |
| description | VARCHAR(255) | | ??�� ?�명 |
| is_active | BOOLEAN | DEFAULT true | ?�성???��? |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ?�성 ?�시 |

**?�덱??*:
- PK: role_id
- UK: (tenant_id, name)
- IDX: (tenant_id)

**??�� 분류**:

| ?�??| ?�명 | ?�시 |
|------|------|------|
| POSITION | 직급 기반 (직책) | ADMIN, MANAGER, TEAM_LEAD, MEMBER |
| CHANNEL | 채널 기반 (?�무 채널) | PHONE_AGENT, CHAT_AGENT, EMAIL_AGENT, SUPERVISOR |
| SKILL | ??�� 기반 | (?�장 가?? |

**?�징**:
- **?�중 ??�� 조합**: ?�용?�는 POSITION + CHANNEL 조합 가??
- ?? 박상??= MEMBER (직급) + PHONE_AGENT (채널)

**?�시 ?�이??*:
```sql
role_id              | name         | type      | is_active
---------------------|--------------|-----------|----------
660e8400-e29b-...001 | ADMIN        | POSITION  | true
660e8400-e29b-...005 | PHONE_AGENT  | CHANNEL   | true
```

---

### 4. permissions (권한 ?�이�?

**목적**: ?�스??권한 ?�의 (최소 ?�위 권한)

| 컬럼�?| ?�??| ?�약 | ?�명 |
|--------|------|------|------|
| permission_id | VARCHAR(36) | PK | 권한 ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | ?�넌??ID |
| code | VARCHAR(128) | NOT NULL, UNIQUE | 권한 코드 (domain:action ?�식) |
| description | VARCHAR(255) | | 권한 ?�명 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ?�성 ?�시 |

**?�덱??*:
- PK: permission_id
- UK: (tenant_id, code)
- IDX: (tenant_id)

**권한 코드 ?�식**:
```
{domain}:{action}[:{resource}]

?�메??(8�?:
?��? user:      ?�용??관�?(9�?
?��? org:       조직 관�?(6�?
?��? rbac:      RBAC 관�?(9�?
?��? report:    보고??(4�?
?��? phone:     ?�화 채널 (3�?
?��? chat:      채팅 채널 (2�?
?��? email:     ?�메??채널 (1�?
?��? queue:     ??관�?(1�?

�?35�?권한
```

**?�시 ?�이??*:
```sql
permission_id        | code                | description
---------------------|---------------------|------------------
550e8400-e29b-...001 | user:create         | ?�용???�성
550e8400-e29b-...029 | phone:accept        | ?�화 ?�락
550e8400-e29b-...032 | chat:send           | 채팅 ?�송
```

---

### 5. role_permissions (??��-권한 매핑 ?�이�?

**목적**: ??��??권한 ?�당 (N:M 관�?

| 컬럼�?| ?�??| ?�약 | ?�명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 매핑 ID (?�동 증�?) |
| role_id | VARCHAR(36) | FK, NOT NULL | ??�� ID |
| permission_id | VARCHAR(36) | FK, NOT NULL | 권한 ID |
| assigned_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ?�당 ?�시 |

**?�덱??*:
- PK: id
- UK: (role_id, permission_id)
- FK: role_id ??roles.role_id (ON DELETE CASCADE)
- FK: permission_id ??permissions.permission_id (ON DELETE CASCADE)

**?�징**:
- **?��???관�?*: ????��???�러 권한 ?�당 가??
- **?�적 권한 관�?*: ??�� 변�????�동 반영
- **CASCADE ??��**: ??��/권한 ??�� ??매핑???�동 ??��

**권한 배분 ?�시**:
```sql
ADMIN:     35�?(?�체)
MANAGER:   12�?(?�용?? 조직, 보고??
TEAM_LEAD:  5�?(?�기, 조직 �? 보고??
MEMBER:     4�?(본인 ?�기, 조직 �? 보고??

PHONE_AGENT:  3�?(?�화 관??
CHAT_AGENT:   2�?(채팅 관??
EMAIL_AGENT:  1�?(?�메??관??
SUPERVISOR:   7�?(모든 채널 + ??

�?77�?매핑
```

---

### 6. agent_roles (?�용????�� 매핑 ?�이�?

**목적**: ?�용?�에�???�� ?�당 (N:M 관�?

| 컬럼�?| ?�??| ?�약 | ?�명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 매핑 ID (?�동 증�?) |
| agent_id | VARCHAR(36) | FK, NOT NULL | ?�용??ID |
| role_id | VARCHAR(36) | FK, NOT NULL | ??�� ID |
| assigned_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ?�당 ?�시 |

**?�덱??*:
- PK: id
- UK: (agent_id, role_id) - 중복 방�?
- FK: agent_id ??agents.agent_id (ON DELETE CASCADE)
- FK: role_id ??roles.role_id (ON DELETE CASCADE)
- IDX: (agent_id), (role_id)

**?�징**:
- **?�중 ??��**: ?�용?�는 ?�러 ??�� 보유 가??(?? MEMBER + PHONE_AGENT + SUPERVISOR)
- **?�적 ?�당**: ??�� 추�?/?�거 ???�동 반영
- **권한 계산**: 모든 ??��??권한 ?�집??= ?�용?�의 최종 권한

**?�시 ?�이??*:
```sql
agent_id (박상??    | role_id (??��)
---------------------|----------------------
550e8400-e29b-...104 | 660e8400-e29b-...004 (MEMBER)
550e8400-e29b-...104 | 660e8400-e29b-...005 (PHONE_AGENT)
```

---

### 7. audit_logs (감사 로그 ?�이�?

**목적**: ?�스??주요 ?�업 ?�력 추적

| 컬럼�?| ?�??| ?�약 | ?�명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 로그 ID (?�동 증�?) |
| tenant_id | VARCHAR(50) | NOT NULL | ?�넌??ID |
| action | VARCHAR(100) | NOT NULL | ?�업 (ROLE_ASSIGNED, PERMISSION_CREATED ?? |
| target_type | VARCHAR(50) | NOT NULL | ?�???�??(ROLE, PERMISSION, USER ?? |
| target_id | VARCHAR(100) | NOT NULL | ?�??ID |
| actor_id | VARCHAR(36) | NOT NULL | ?�업??ID |
| details | TEXT | | ?�세 ?�보 (JSON ?�식) |
| ip_address | VARCHAR(45) | | ?�업??IP |
| timestamp | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 발생 ?�시 |

**?�덱??*:
- PK: id
- IDX: (tenant_id, timestamp), (actor_id), (target_type, target_id)

**?�징**:
- **불�? 로그**: ?�성 ???�정/??�� 불�?
- **90???�동 ?�카?�빙**: audit_archive�??�동
- **JSON ?�세 ?�보**: 변�??�후 �??�??

---

### 8. audit_archive (감사 로그 ?�카?�브 ?�이�?

**목적**: 90???�상 ?�래??감사 로그 보�?

| 컬럼�?| ?�??| ?�약 | ?�명 |
|--------|------|------|------|
| (audit_logs?� ?�일) | | | |
| archived_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | ?�카?�빙 ?�시 |

**?�징**:
- **?�동 ?�카?�빙**: 배치 ?�업?�로 90??초과 로그 ?�동
- **?�기 보�?**: 법적 ?�구?�항 ?�??
- **검??최적??*: 최근 로그??audit_logs?�서�?검??

---

## ?�이�?�??��?관�?

### 1. ?��???(One-to-Many) 관�?

#### departmentEntities (1) ??departmentEntities (N) - ?�기참조
```
?�위 부??(parent) ???�위 부?�들 (?�식)

관�? 부�??�식
FK: parent_id ??dept_id
?�징: ?�기참조, ?�리 구조
??�� ?�책: ON DELETE RESTRICT (?�위 부???�으�???�� 불�?)

?�시:
?�스?�론 본�? (root)
?��? 고객지?�사?��?
?? ?��? ?�화?�담?�
?? ?��? 채팅?�담?�
?��? 기술개발본�?
   ?��? Backend개발?�
```

#### departmentEntities (1) ??user_agents (N)
```
부??(departmentEntity) ???�속 직원??(employees)

관�? 조직 ?�함 관�?
FK: agents.dept_id ??departmentEntities.dept_id
?�징: ?�나??부?�에 ?�러 직원
??�� ?�책: ON DELETE SET NULL (부????�� ??직원??dept_id = NULL)

?�시:
?�화?�담?� (1�?
?��? ?��???(1�?
?��? 박상??(1�?
?��? 최상??(1�?
```

---

### 2. ?��???(Many-to-Many) 관�?

#### user_agents (N) ??roles (M) via agent_roles
```
?�용???�→ ??��

구조:
agents ??agent_roles ??roles

?�징:
- ???�용?��? ?�러 ??�� 보유
- ????��???�러 ?�용?�에�??�당
- 중간 ?�이�? agent_roles

?�시:
박상??(1�?
?��? MEMBER (직급)
?��? PHONE_AGENT (채널)

MEMBER ??��
?��? 박상??
?��? ?�상??
?��? 강상??
?��? ... (7�?

??�� ?�책: ON DELETE CASCADE (?�쪽 모두)
```

#### roles (N) ??permissions (M) via role_permissions
```
??�� ?�→ 권한

구조:
roles ??role_permissions ??permissions

?�징:
- ????��???�러 권한 ?�함
- ??권한???�러 ??��???�당 가??
- 중간 ?�이�? role_permissions

?�시:
ADMIN ??�� (1�? ??35�?권한 (모두)
MEMBER ??�� (1�? ??4�?권한 (최소)

??�� ?�책: ON DELETE CASCADE (?�쪽 모두)
```

---

### 3. 권한 체크 ?�름

**?�용?�의 최종 권한 계산**:

```
1?�계: ?�용??조회
?��??�?�?�?�?�?�?�?�?�?�?�?�?�??
??agents       ??(agent_id�?조회)
??agent_id=... ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�??
        ??

2?�계: ?�용?�의 모든 ??�� 조회
?��??�?�?�?�?�?�?�?�?�?�?�?�?�??
??agent_roles  ??(WHERE agent_id = ?)
??role_id=... ??
??role_id=... ??(?�중 ??��)
?��??�?�?�?�?�?�?�?�?�?�?�?�?�??
        ??

3?�계: �???��??모든 권한 조회
?��??�?�?�?�?�?�?�?�?�?�?�?�?�??
??role_permissions ??(WHERE role_id IN (...))
??permission_id=... ??
??permission_id=... ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�??
        ??

4?�계: 모든 권한 코드 조회
?��??�?�?�?�?�?�?�?�?�?�?�?�?�??
??permissions  ??
??code='user:create' ??
??code='phone:accept' ??
?��??�?�?�?�?�?�?�?�?�?�?�?�?�??
        ??

5?�계: 권한 ?�인
최종 권한 = 모든 ??��??권한 ?�집??(Union)
```

**SQL ?�시**:
```sql
-- ?�정 ?�용?�의 모든 권한 조회
SELECT DISTINCT p.code
FROM agents a
JOIN agent_roles ar ON a.agent_id = ar.agent_id
JOIN role_permissions rp ON ar.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE a.agent_id = ? 
  AND a.status = 'ACTIVE'
  AND a.tenant_id = ?;
```

---

## 컬럼 ?�이???�식 ?��?

### 1. ID 컬럼 (모두 UUID�??�일)

| 컬럼�?| ?�??| ?�기 | ?�식 | ?�시 |
|--------|------|------|------|------|
| dept_id | VARCHAR | 36 | UUID | d50e8400-e29b-41d4-a716-446655440001 |
| agent_id | VARCHAR | 36 | UUID | 550e8400-e29b-41d4-a716-446655440101 |
| role_id | VARCHAR | 36 | UUID | 660e8400-e29b-41d4-a716-446655440001 |
| permission_id | VARCHAR | 36 | UUID | 550e8400-e29b-41d4-a716-446655440001 |

**UUID ?�식**:
```
xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  8??  - 4??- 4??- 4??-    12??
  �?36??(?�이???�함)
```

---

### 2. 문자??컬럼 ?��?

| 컬럼�?| 최�?길이 | ?�명 | ?�시 |
|--------|---------|------|------|
| tenant_id | 50 | ?�넌??ID (고정) | tenant-001 |
| login_id | 100 | 로그??ID (?�숫?? -, _) | phone_agent01 |
| password | 255 | BCrypt ?�시 | $2a$10$N9qo8... |
| name | 100 | ?�용??부?�명 | 박상?? ?�화?�담?� |
| org_path | 500 | 조직 경로 (UUID 기반) | /d50e8400.../d50e8400.../... |
| job_title | 100 | 직책 | ?�?? 과장 |
| type (departmentEntities) | 50 | 부???�??| HEADQUARTERS, DIVISION, TEAM |
| type (roles) | 32 | ??�� ?�??| POSITION, CHANNEL, SKILL |
| status (agents) | 20 | ?�태 | ACTIVE, RETIRED |
| name (roles) | 64 | ??���?(?�문자, _) | ADMIN, TEAM_LEAD, PHONE_AGENT |
| code (permissions) | 128 | 권한 코드 (?�메???�션) | user:create, phone:accept |

---

### 3. ?�간 컬럼 ?��?

| 컬럼�?| ?�??| ?�식 | ?�명 | ?�시 |
|--------|------|------|------|------|
| created_at | TIMESTAMP | ISO 8601 | ?�성 ?�시 (?�동) | 2026-01-16 10:00:00 |
| updated_at | TIMESTAMP | ISO 8601 | ?�정 ?�시 (?�동) | 2026-01-16 10:05:00 |
| assigned_at | TIMESTAMP | ISO 8601 | ?�당 ?�시 | 2026-01-16 10:00:00 |
| retired_at | TIMESTAMP | ISO 8601 | ?�직 ?�시 (NULL 가?? | 2025-12-14 17:00:00 |
| timestamp | TIMESTAMP | ISO 8601 | 감사 로그 발생 ?�시 | 2026-01-16 10:00:00 |

---

### 4. NULL ?�책

| 컬럼�?| ?�이�?| NULL ?�용 | ?�유 | 비고 |
|--------|--------|----------|------|------|
| parent_id | departmentEntities | YES | 최상??부?�일 ???�음 | 루트??NULL |
| dept_id | agents | YES | 부??미정 직원 가??| ON DELETE SET NULL |
| updated_at | agents | YES | ?�성 ???�정 ?�을 ???�음 | ?�택?�항 |
| retired_at | agents | YES | ?�성 직원?� NULL | Soft Delete |
| job_title | agents | YES | 직책 미정 가??| ?�택?�항 |
| description | roles, permissions | YES | ?�명 ?�택?�항 | |
| ip_address | audit_logs | YES | IP 추적 불�??�할 ???�음 | |

---

## ?��? ?�이??가?�드

### ?��? ?�이?�셋

| ??�� | ?�량 | ?�명 |
|------|------|------|
| **Departments** | 13�?| 본�?(1) + ?�업부(3) + ?�(9) |
| **Agents** | 16�?| ?�성(15) + ?�직(1) |
| **Roles** | 8�?| POSITION(4) + CHANNEL(4) |
| **Permissions** | 35�?| 8�??�메??|
| **Role-Permissions** | 77�?| ??���?권한 매핑 |
| **Agent-Roles** | ~30�?| ?�용?�별 ?�중 ??�� |

### 조직 구조 ?�시

```
?�스?�론 본�? (HEADQUARTERS)
?��? 고객지?�사?��? (DIVISION)
?? ?��? ?�화?�담?� (TEAM)
?? ?? ?��? ?��???(TEAM_LEAD + SUPERVISOR)
?? ?? ?��? 박상??(MEMBER + PHONE_AGENT)
?? ?? ?��? 최상??(MEMBER + PHONE_AGENT)
?? ?��? 채팅?�담?� (TEAM)
?? ?��? VIP고객지?��? (TEAM)
?��? ?�업?�업부 (DIVISION)
?��? 기술개발본�? (DIVISION)
   ?��? Backend개발?� (TEAM)
   ?��? Frontend개발?� (TEAM)
   ?��? DevOps?� (TEAM)
```

---

## 권한 �???�� ?��?

### 권한(Permission) 코드 규칙

**?�식**: `{domain}:{action}[:{resource}]`

### ?�메?�별 권한 목록 (�?35�?

#### 1. ?�용??관�?(user, agent) - 9�?
```
- user:create          ?�용???�성
- user:read            ?�용??조회
- user:update          ?�용???�정
- user:delete          ?�용????��
- user:read:self       본인 ?�보 조회
- user:update:self     본인 ?�보 ?�정
- user:assign:role     ??�� ?�당
- user:reset:password  비�?번호 ?�설??
- agent:manage         ?�이?�트 ?�체 관�?
```

#### 2. 조직 관�?(org, departmentEntity) - 6�?
```
- org:view             조직 조회
- org:create           조직 ?�성
- org:update           조직 ?�정
- org:move             조직 ?�동
- org:delete           조직 ??��
- org:manage           조직 ?�체 관�?
```

#### 3. RBAC 관�?(rbac, role, permission) - 9�?
```
- rbac:view            RBAC 조회
- rbac:create:role     ??�� ?�성
- rbac:update:role     ??�� ?�정
- rbac:delete:role     ??�� ??��
- rbac:create:permission 권한 ?�성
- rbac:update:permission 권한 ?�정
- rbac:delete:permission 권한 ??��
- rbac:assign:permission 권한 ?�당
- rbac:configure       RBAC ?�체 ?�정
```

#### 4. 보고??�?감시 (report, audit, cdr) - 7�?
```
- report:view          보고??조회
- report:read          보고???�기
- report:export        보고???�보?�기
- report:manage        보고??관�?
- audit:view           감사 로그 조회
- audit:export         감사 로그 ?�보?�기
- cdr:view             CDR 조회
```

#### 5. 채널 관�?(phone, chat, email, queue) - 7�?
```
- phone:accept         ?�화 ?�락
- phone:hold           ?�화 보류
- phone:transfer       ?�화 ?�환
- chat:send            채팅 ?�송
- chat:receive         채팅 ?�신
- email:send           ?�메???�송
- queue:manage         ??관�?
```

#### 6. 기�? (dashboard, quality) - 2�?
```
- dashboard:view       ?�?�보??조회
- quality:manage       ?�질 관�?
```

---

### ??��(Role) ?�의

#### ??�� ?�??
- **POSITION**: 조직??직위 (ADMIN, MANAGER, TEAM_LEAD, MEMBER)
- **CHANNEL**: ?�담 채널 (PHONE_AGENT, CHAT_AGENT, EMAIL_AGENT, SUPERVISOR)
- **SKILL**: 기술/?�킬 (?�후 ?�장??

#### 기본 ??�� �?권한 ?�당

| ??�� | ?�??| 권한 ??| 주요 권한 |
|------|------|---------|-----------|
| **ADMIN** | POSITION | 35�?(?�체) | user:*, org:*, rbac:*, report:*, audit:*, 모든 채널 |
| **MANAGER** | POSITION | 12�?| user ?�성/?�정, org ?�성/?�정/?�동, report ?�체 |
| **TEAM_LEAD** | POSITION | 5�?| user:read, org:view, report:view/read/export |
| **MEMBER** | POSITION | 4�?| user:read:self, user:update:self, org:view, report:view |
| **PHONE_AGENT** | CHANNEL | 3�?| phone:accept, phone:hold, phone:transfer |
| **CHAT_AGENT** | CHANNEL | 2�?| chat:send, chat:receive |
| **EMAIL_AGENT** | CHANNEL | 1�?| email:send |
| **SUPERVISOR** | CHANNEL | 7�?| 모든 채널 + queue:manage |

---

### 권한 계층 ?�시

```
ADMIN (35�?권한 - ?�체)
?��? user:* (9�?
?��? org:* (6�?
?��? rbac:* (9�?
?��? report:* (4�?
?��? audit:* (2�?
?��? 채널 ?�체 (7�?
?��? dashboard, quality (2�?

MANAGER (12�?권한)
?��? user: create, read, update, assign:role, reset:password
?��? org: view, create, update, move
?��? report: view, read, export

MEMBER (4�?권한 - 최소)
?��? user:read:self
?��? user:update:self
?��? org:view
?��? report:view

PHONE_AGENT (3�?권한)
?��? phone:accept
?��? phone:hold
?��? phone:transfer
```

---

## ?�계 ?�칙 �??�유

### 1. UUID�??�일???�유
- ??분산 ?�경 지??(ID 충돌 ?�음)
- ??멀?�테?�시 ?�전??(?�넌??�?ID 충돌 불�?)
- ??보안 (?�차 ID ?�출 방�?)
- ???��???(모든 ?�티???�일???�식)

### 2. ?�기참조 FK ?�용 ?�유
- ??계층 구조 ?�현 최적??
- ??org_path�?경로 ?�색 빠름
- ??depth�??�벨 ?�게 ?�악
- ???�연??부??추�?/?�거

### 3. 중간 ?�이�??�용 ?�유
- ??N:M 관계�? 명시?�으�?관�?
- ???�당 ?�시 ??메�??�이???�??가??
- ??감사 추적 ?�이
- ???�능 최적??(조인 명확??

### 4. Soft Delete ?�용 ?�유
- ???�스?�리 ?��?
- ??감사 추적 (?�제 ?�직?�는지)
- ???�이??복구 가??
- ??참조 무결???��?

### 5. 멀?�테?�시 구현 ?�유
- ???�이??격리 (tenant_id ?�수)
- ??SaaS ?�장??
- ??보안 (?�넌??�??�이???�근 불�?)

---

## 부�? 빠른 참조

### 주요 쿼리 ?�턴

#### 1. ?�용?�의 모든 권한 조회
```sql
SELECT DISTINCT p.code
FROM agents a
JOIN agent_roles ar ON a.agent_id = ar.agent_id
JOIN role_permissions rp ON ar.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE a.agent_id = :agentId
  AND a.status = 'ACTIVE'
  AND a.tenant_id = :tenantId;
```

#### 2. 부?�의 ?�체 ?�위 부??조회 (?�리)
```sql
SELECT *
FROM departmentEntities
WHERE org_path LIKE CONCAT(:targetOrgPath, '%')
  AND tenant_id = :tenantId
ORDER BY depth, name;
```

#### 3. ??��???�당??모든 권한 조회
```sql
SELECT p.code, p.description
FROM role_permissions rp
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE rp.role_id = :roleId
  AND p.tenant_id = :tenantId;
```

#### 4. ?�용?��? ?�정 권한??보유?�는지 ?�인
```sql
SELECT EXISTS (
    SELECT 1
    FROM agents a
    JOIN agent_roles ar ON a.agent_id = ar.agent_id
    JOIN role_permissions rp ON ar.role_id = rp.role_id
    JOIN permissions p ON rp.permission_id = p.permission_id
    WHERE a.agent_id = :agentId
      AND p.code = :permissionCode
      AND a.status = 'ACTIVE'
      AND a.tenant_id = :tenantId
) AS has_permission;
```

---

## 6. ?�이?�베?�스 초기??방법

### ?�� ?�전 초기??(권장)

**?�️ 주의**: 모든 ?�이?��? ??��?�니??

#### 방법 1: SQL ?�크립트 직접 ?�행
```bash
# 1. PostgreSQL ?�라?�언?�에???�행
psql -U nexfron -d nexfron -f reset_database_clean.sql

# 2. ?�플리�??�션 ?�시??(Flyway ?�동 마이그레?�션)
./gradlew bootRun
```

#### 방법 2: DBeaver/DataGrip ??GUI ?�구
1. `reset_database_clean.sql` ?�일 ?�기
2. ?�체 ?�택 ???�행 (Ctrl+Enter)
3. 결과 ?�인: `???�이?�베?�스 ?�전 초기???�료!`
4. ?�플리�??�션 ?�시??

### ?�� Flyway 마이그레?�션

?�플리�??�션 ?�작 ???�동?�로:
1. `V1_0_0__Complete_Init.sql` ?�키�??�성
2. ?��? ?�이???�동 ?�입 (35권한 + 8??�� + 16?�용??

### ?�� 초기?????�인

```sql
-- ?�이�?목록 ?�인
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- ?�이??건수 ?�인
SELECT 'departmentEntities' as table_name, COUNT(*) as count FROM departmentEntities
UNION ALL SELECT 'agents', COUNT(*) FROM agents
UNION ALL SELECT 'roles', COUNT(*) FROM roles
UNION ALL SELECT 'permissions', COUNT(*) FROM permissions
UNION ALL SELECT 'role_permissions', COUNT(*) FROM role_permissions
UNION ALL SELECT 'agent_roles', COUNT(*) FROM agent_roles;
```

**?�상 결과**:
- departmentEntities: 16�?
- agents: 16�?(admin ?�함)
- roles: 8�?
- permissions: 35�?
- role_permissions: 77�?
- agent_roles: 22�?

---

**문서 ?�성??*: 2026-01-21  
**?�성??*: Identity System Team  
**버전**: 2.0.0 CLEAN  
**?�태**: 최종 ?�인 ??
---

> ?�️ **주의?�항**  
> - 모든 ?�이블�? tenant_id�?격리?�어???�니?? 
> - ID??반드??UUID (VARCHAR(36)) ?�식???�용?�야 ?�니?? 
> - ??�� ?�책(ON DELETE)?� 반드??문서?��??�정?�야 ?�니?? 
> - 권한 코드??`domain:action` ?�식???�격??준?�해???�니??

