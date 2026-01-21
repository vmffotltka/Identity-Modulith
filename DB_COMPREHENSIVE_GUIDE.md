# Identity Modulith - 데이터베이스 가이드

> 📅 최종 업데이트: 2026-01-21  
> 🗄️ DB: PostgreSQL 18+  
> ⚠️ 주요 변경: PermissionGroup 기능 제거 (v2.0.0)

---

## 📊 전체 테이블 구조 (8개)

| 테이블명 | 모듈 | PK 타입 | 설명 |
|---------|------|---------|------|
| **departments** | Organization | UUID | 조직(부서) 계층 구조 |
| **agents** | User | UUID | 사용자(상담사) 정보 |
| **roles** | RBAC | UUID | 역할 정의 |
| **permissions** | RBAC | UUID | 권한 정의 |
| **role_permissions** | RBAC | BIGSERIAL | 역할-권한 매핑 (M:N) |
| **agent_roles** | RBAC | BIGSERIAL | 사용자-역할 매핑 (M:N) |
| **audit_logs** | RBAC | UUID | 감사 로그 (권한 변경 이력) |
| **audit_logs_archive** | RBAC | UUID | 감사 로그 아카이브 (6개월+) |

---

## 🏢 1. departments (조직/부서)

**목적**: 조직 계층 구조 관리 (트리)

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식/예시 |
|--------|------|------|------|----------------|
| **dept_id** | VARCHAR(36) | ✖ | 부서 ID (PK) | `d0000000-0000-0000-0000-000000000001` |
| tenant_id | VARCHAR(50) | ✖ | 테넌트 ID | `tenant-001` |
| parent_id | VARCHAR(36) | ✓ | 상위 부서 ID (FK) | NULL=최상위, UUID=하위 |
| name | VARCHAR(100) | ✖ | 부서명 | `경영지원본부`, `인사팀` |
| org_path | VARCHAR(500) | ✖ | 조직 경로 | `/루트ID/자식ID` |
| depth | INTEGER | ✖ | 트리 깊이 | 0(최상위) ~ 10 |
| type | VARCHAR(50) | ✓ | 부서 타입 | `본부`, `팀`, `파트`, `실` |
| created_at | TIMESTAMP | ✖ | 생성 일시 | `2026-01-21 10:00:00` |

**인덱스**: `(tenant_id, org_path)` UK, `tenant_id`, `parent_id`, `org_path`  
**FK**: `parent_id` → `departments(dept_id)` ON DELETE RESTRICT

**데이터 예시**:
```sql
-- 본부 (최상위)
('d0000000-0000-0000-0000-000000000001', 'tenant-001', NULL, 
 '경영지원본부', '/d0000000-0000-0000-0000-000000000001', 0, '본부', NOW())

-- 팀 (하위)
('d0000000-0000-0000-0000-000000000011', 'tenant-001', 
 'd0000000-0000-0000-0000-000000000001', '인사팀', 
 '/d0000000-0000-0000-0000-000000000001/d0000000-0000-0000-0000-000000000011', 
 1, '팀', NOW())
```

---

## 👤 2. agents (사용자/상담사)

**목적**: 시스템 사용자 정보 관리

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식/예시 |
|--------|------|------|------|----------------|
| **agent_id** | VARCHAR(36) | ✖ | 사용자 ID (PK) | UUID |
| tenant_id | VARCHAR(50) | ✖ | 테넌트 ID | `tenant-001` |
| login_id | VARCHAR(100) | ✖ | 로그인 ID (UK) | `admin`, `agent01` (영문+숫자, 4-20자) |
| password | VARCHAR(255) | ✖ | 비밀번호 | BCrypt (`$2a$10$...`) |
| name | VARCHAR(100) | ✖ | 사용자명 | `홍길동`, `Kim Admin` (2-50자) |
| dept_id | VARCHAR(36) | ✓ | 소속 부서 ID (FK) | UUID 또는 NULL |
| status | VARCHAR(20) | ✖ | 상태 | **`ACTIVE`** (활성), **`RETIRED`** (퇴직) |
| password_must_change | BOOLEAN | ✓ | 비밀번호 변경 필요 | `true`, `false` |
| created_at | TIMESTAMP | ✖ | 생성 일시 | `2026-01-21 10:00:00` |
| updated_at | TIMESTAMP | ✓ | 수정 일시 | `2026-01-21 15:00:00` |
| retired_at | TIMESTAMP | ✓ | 퇴직 일시 | `2025-12-31 23:59:59` |
| job_title | VARCHAR(100) | ✓ | 직책 | `대리`, `과장`, `팀장` |
| sync_status | VARCHAR(20) | ✓ | 동기화 상태 | `SYNCED`, `PENDING` (Keycloak용) |
| role_id | VARCHAR(36) | ✓ | ⚠️ 사용 안 함 | NULL (agent_roles 테이블 사용) |

**인덱스**: `login_id` UK, `tenant_id`, `dept_id`, `status`  
**FK**: `dept_id` → `departments(dept_id)` ON DELETE SET NULL

**데이터 예시**:
```sql
('a0000000-0000-0000-0000-000000000001', 'tenant-001', 'admin', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 
 'System Admin', NULL, 'ACTIVE', false, NOW(), NULL, NULL, 'Admin', NULL, NULL)
```

---

## 🎭 3. roles (역할)

**목적**: 역할 정의 및 관리

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식/예시 |
|--------|------|------|------|----------------|
| **role_id** | VARCHAR(36) | ✖ | 역할 ID (PK) | UUID |
| tenant_id | VARCHAR(50) | ✖ | 테넌트 ID | `tenant-001` |
| name | VARCHAR(64) | ✖ | 역할명 (UK) | **`ADMIN`**, **`MANAGER`**, **`TEAM_LEAD`**, **`MEMBER`** (직책) <br> **`PHONE_AGENT`**, **`CHAT_AGENT`**, **`EMAIL_AGENT`** (채널) |
| type | VARCHAR(32) | ✖ | 역할 타입 | **`POSITION`** (직책 기반), **`CHANNEL`** (채널 기반), **`SKILL`** (스킬 기반) |
| description | VARCHAR(255) | ✓ | 역할 설명 | `시스템 전체 관리자 - 모든 권한 보유` |
| is_active | BOOLEAN | ✖ | 활성화 상태 | **`true`** (활성), **`false`** (비활성/논리 삭제) |
| version | BIGINT | ✖ | 낙관적 잠금 버전 | 0, 1, 2... (동시성 제어용) |
| created_at | TIMESTAMP | ✖ | 생성 일시 | `2026-01-21 10:00:00` |
| updated_at | TIMESTAMP | ✖ | 수정 일시 | `2026-01-21 15:00:00` |

**인덱스**: `(tenant_id, name)` UK, `tenant_id`, `is_active`

**표준 역할 (8개)**:

### 직책 기반 (POSITION)
1. **ADMIN** - 시스템 전체 관리자 (35개 전체 권한)
2. **MANAGER** - 부서 관리자 (12개 권한)
3. **TEAM_LEAD** - 팀 리더 (5개 권한)
4. **MEMBER** - 일반 사용자 (4개 권한)

### 채널 기반 (CHANNEL)
5. **PHONE_AGENT** - 전화 상담사 (3개 권한)
6. **CHAT_AGENT** - 채팅 상담사 (2개 권한)
7. **EMAIL_AGENT** - 이메일 상담사 (1개 권한)
8. **SUPERVISOR** - 슈퍼바이저 (큐 관리)

**데이터 예시**:
```sql
('660e8400-e29b-41d4-a716-446655440001', 'tenant-001', 'ADMIN', 'POSITION', 
 '시스템 전체 관리자 - 모든 권한 보유', true, 0, NOW(), NOW())
```

---

## 🔑 4. permissions (권한)

**목적**: 세분화된 권한 정의

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식/예시 |
|--------|------|------|------|----------------|
| **permission_id** | VARCHAR(36) | ✖ | 권한 ID (PK) | UUID |
| tenant_id | VARCHAR(50) | ✖ | 테넌트 ID | `tenant-001` |
| code | VARCHAR(128) | ✖ | 권한 코드 (UK) | **`domain:action`** 형식 |
| created_at | TIMESTAMP | ✖ | 생성 일시 | `2026-01-21 10:00:00` |

**인덱스**: `(tenant_id, code)` UK, `tenant_id`

**표준 권한 코드 (35개)**:

### 사용자 관리 (user)
- `user:create` - 사용자 생성
- `user:read` - 사용자 조회 (전체)
- `user:read:self` - 본인 정보 조회
- `user:update` - 사용자 수정
- `user:update:self` - 본인 정보 수정
- `user:delete` - 사용자 삭제
- `user:manage` - 사용자 전체 관리
- `user:assign:role` - 역할 할당
- `user:reset:password` - 비밀번호 초기화

### 조직 관리 (org)
- `org:view` - 조직도 조회
- `org:create` - 부서 생성
- `org:update` - 부서 수정
- `org:move` - 부서 이동
- `org:delete` - 부서 삭제
- `org:manage` - 조직 전체 관리

### RBAC 관리 (rbac)
- `rbac:view` - 역할/권한 조회
- `rbac:create:role` - 역할 생성
- `rbac:update:role` - 역할 수정
- `rbac:delete:role` - 역할 삭제
- `rbac:create:permission` - 권한 생성
- `rbac:update:permission` - 권한 수정
- `rbac:delete:permission` - 권한 삭제
- `rbac:assign:permission` - 권한 할당
- `rbac:configure` - RBAC 전체 설정

### 보고서 (report)
- `report:view` - 보고서 조회
- `report:read` - 보고서 읽기
- `report:export` - 보고서 내보내기
- `report:manage` - 보고서 관리

### 채널 (phone, chat, email)
- `phone:accept` - 전화 수신
- `phone:hold` - 전화 보류
- `phone:transfer` - 전화 전환
- `chat:send` - 채팅 전송
- `chat:read` - 채팅 읽기
- `email:send` - 이메일 전송
- `queue:manage` - 큐 관리

**데이터 예시**:
```sql
('550e8400-e29b-41d4-a716-446655440001', 'tenant-001', 'user:create', NOW())
```

---

## 🔗 5. role_permissions (역할-권한 매핑)

**목적**: 역할과 권한의 다대다 관계

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식/예시 |
|--------|------|------|------|----------------|
| **id** | BIGSERIAL | ✖ | 매핑 ID (PK) | 1, 2, 3... |
| role_id | VARCHAR(36) | ✖ | 역할 ID (FK) | UUID |
| permission_id | VARCHAR(36) | ✖ | 권한 ID (FK) | UUID |
| assigned_at | TIMESTAMP | ✖ | 할당 일시 | `2026-01-21 10:00:00` |

**인덱스**: `(role_id, permission_id)` UK  
**FK**: `role_id` → `roles(role_id)` ON DELETE CASCADE  
**FK**: `permission_id` → `permissions(permission_id)` ON DELETE CASCADE

**매핑 수 (77개)**:
- ADMIN: 35개 (전체)
- MANAGER: 12개
- TEAM_LEAD: 5개
- MEMBER: 4개
- PHONE_AGENT: 3개
- CHAT_AGENT: 2개
- EMAIL_AGENT: 1개
- SUPERVISOR: 15개

---

## 👥 6. agent_roles (사용자-역할 매핑)

**목적**: 사용자와 역할의 다대다 관계

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식/예시 |
|--------|------|------|------|----------------|
| **id** | BIGSERIAL | ✖ | 매핑 ID (PK) | 1, 2, 3... |
| agent_id | VARCHAR(36) | ✖ | 사용자 ID (FK) | UUID |
| role_id | VARCHAR(36) | ✖ | 역할 ID (FK) | UUID |
| assigned_at | TIMESTAMP | ✖ | 할당 일시 | `2026-01-21 10:00:00` |

**인덱스**: `(agent_id, role_id)` UK, `agent_id`, `role_id`  
**FK**: `agent_id` → `agents(agent_id)` ON DELETE CASCADE  
**FK**: `role_id` → `roles(role_id)` ON DELETE CASCADE

**💡 사용자는 여러 역할을 동시에 가질 수 있습니다**:
- 예: `MANAGER` + `PHONE_AGENT` = 관리자이면서 전화 상담도 가능

---

## 📝 7. audit_logs (감사 로그)

**목적**: 권한 관련 모든 변경사항 추적

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식/예시 |
|--------|------|------|------|----------------|
| **audit_id** | VARCHAR(36) | ✖ | 감사 로그 ID (PK) | UUID |
| tenant_id | VARCHAR(50) | ✖ | 테넌트 ID | `tenant-001` |
| action | VARCHAR(32) | ✖ | 작업 유형 | **`CREATE`**, **`UPDATE`**, **`DELETE`**, **`ASSIGN`**, **`REVOKE`** |
| resource_type | VARCHAR(64) | ✖ | 리소스 타입 | **`ROLE`**, **`PERMISSION`**, **`AGENT_ROLE`**, **`ROLE_PERMISSION`** |
| resource_id | VARCHAR(255) | ✖ | 리소스 ID | UUID 또는 복합키 |
| operator_id | VARCHAR(255) | ✖ | 작업자 ID | UUID |
| changes | TEXT | ✓ | 변경 내용 (JSON) | `{"before":"...","after":"..."}` |
| timestamp | TIMESTAMP | ✖ | 작업 일시 | `2026-01-21 10:00:00` |
| remarks | TEXT | ✓ | 추가 정보 | 메모, 실패 원인 등 |
| ip_address | VARCHAR(45) | ✓ | 클라이언트 IP | `192.168.1.100` |

**인덱스**: `tenant_id`, `resource_type`, `operator_id`, `timestamp DESC`

**자동 기록 시점**:
- 역할 생성/수정/삭제
- 권한 생성/수정/삭제
- 역할-권한 할당/회수
- 사용자-역할 할당/회수

---

## 📦 8. audit_logs_archive (감사 로그 아카이브)

**목적**: 6개월 이상 경과한 로그 보관

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| *(audit_logs와 동일)* | | |
| archived_at | TIMESTAMP | 아카이브 일시 |

**인덱스**: `tenant_id`, `timestamp DESC`, `(resource_type, resource_id)`, `operator_id`

**자동 아카이빙 배치**:
- **스케줄**: 매월 1일 00:00:00 (CRON: `0 0 1 * * *`)
- **기준**: **6개월** 이전 로그 자동 이동
- **처리 흐름**:
  1. `audit_logs`에서 6개월 이전 데이터 조회
  2. `audit_logs_archive`로 복사 (`archived_at` = 복사 시각)
  3. 원본 데이터 삭제
  4. 처리 건수 로깅
- **구현**: `AuditLogArchivingBatchService` (네이티브 쿼리 사용)
- **활성화**: `@EnableScheduling` (IdentityModulithApplication)

**💡 데이터 보존 정책**:
- `audit_logs`: 최근 **6개월**
- `audit_logs_archive`: **6개월 이상** (장기 보관)

**로그 예시**:
```
[감사 로그 아카이빙] 배치 시작: 대상=2025-07-21 00:00:00 이전 로그
[감사 로그 아카이빙] 아카이브 테이블 복사 완료: 1,234건
[감사 로그 아카이빙] 배치 완료: 복사=1,234, 삭제=1,234, 소요시간=1,500ms
```

---

## 🔄 테이블 간 관계도

```
departments (부서)
    ↓ 1:N (parent_id)
departments (하위 부서)
    ↓ 1:N (dept_id)
agents (사용자)
    ↓ M:N (agent_roles)
roles (역할)
    ↓ M:N (role_permissions)
permissions (권한)
```

---

## 🚀 초기화 방법

### 1. 데이터베이스 완전 초기화
```bash
# PostgreSQL 클라이언트에서
psql -U nexfron -d nexfron -f reset_database_clean.sql
```

### 2. 애플리케이션 실행
```bash
./gradlew bootRun
```

Flyway가 자동으로 `V1_0_0__Complete_Init.sql` 실행 → 8개 테이블 + 표준 데이터 생성

### 3. 확인
```sql
SELECT 'departments' as table_name, COUNT(*) FROM departments
UNION ALL SELECT 'agents', COUNT(*) FROM agents
UNION ALL SELECT 'roles', COUNT(*) FROM roles
UNION ALL SELECT 'permissions', COUNT(*) FROM permissions
UNION ALL SELECT 'role_permissions', COUNT(*) FROM role_permissions
UNION ALL SELECT 'agent_roles', COUNT(*) FROM agent_roles;
```

**예상 결과**: 16부서, 16사용자, 8역할, 35권한, 77매핑, 22할당

---

**문서 버전**: 2.0.0 CLEAN  
**최종 업데이트**: 2026-01-21


### 🎯 핵심 설계 원칙

1. **UUID 기반 식별자**: 모든 엔티티는 UUID 문자열 (VARCHAR(36)) 사용
2. **멀티테넌시**: 모든 테이블에 `tenant_id` 컬럼 포함
3. **Soft Delete**: 역할(`roles`)은 `is_active` 플래그로 논리적 삭제
4. **감사 추적**: 모든 권한 변경사항은 `audit_logs`에 기록
5. **계층 구조**: 부서는 자기참조 + org_path로 트리 구현

---

## 2. 테이블 상세 명세

### 🏢 2.1 departments (조직/부서)

**목적**: 조직 계층 구조 관리 (트리 구조)

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식 |
|--------|------|------|------|-----------|
| **dept_id** | VARCHAR(36) | NOT NULL | 부서 ID (PK) | UUID 형식 (`550e8400-...`) |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID | `tenant-001` ~ `tenant-999` |
| parent_id | VARCHAR(36) | NULL | 상위 부서 ID (FK) | NULL = 최상위, UUID = 하위 부서 |
| name | VARCHAR(100) | NOT NULL | 부서명 | 한글/영문, 2-100자 |
| org_path | VARCHAR(500) | NOT NULL | 조직 경로 | `/루트ID/부서ID` 형식 |
| depth | INTEGER | NOT NULL | 트리 깊이 | 0(최상위) ~ 10(최대) |
| type | VARCHAR(50) | NULL | 부서 타입 | `본부`, `팀`, `파트`, `실` 등 |
| created_at | TIMESTAMP | NOT NULL | 생성 일시 | `2026-01-20 10:30:00` |

**인덱스**:
- UK: `(tenant_id, org_path)` - 경로 중복 방지
- IDX: `tenant_id`, `parent_id`, `org_path`

**FK**:
- `parent_id` → `departments(dept_id)` ON DELETE RESTRICT

**데이터 예시**:
```sql
-- 본부 (최상위)
('d0000000-0000-0000-0000-000000000001', 'tenant-001', NULL, 
 '경영지원본부', '/d0000000-0000-0000-0000-000000000001', 0, '본부', NOW())

-- 팀 (하위)
('d0000000-0000-0000-0000-000000000011', 'tenant-001', 
 'd0000000-0000-0000-0000-000000000001', '인사팀', 
 '/d0000000-0000-0000-0000-000000000001/d0000000-0000-0000-0000-000000000011', 
 1, '팀', NOW())
```

---

### 👤 2.2 agents (사용자/상담사)

**목적**: 시스템 사용자 정보 관리

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식 |
|--------|------|------|------|-----------|
| **agent_id** | VARCHAR(36) | NOT NULL | 사용자 ID (PK) | UUID 형식 |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID | `tenant-001` |
| login_id | VARCHAR(100) | NOT NULL | 로그인 ID (UK) | 영문+숫자, 4-20자 |
| password | VARCHAR(255) | NOT NULL | 비밀번호 | BCrypt 해시 (`$2a$10$...`) |
| name | VARCHAR(100) | NOT NULL | 사용자명 | 한글/영문, 2-50자 |
| dept_id | VARCHAR(36) | NULL | 소속 부서 ID (FK) | UUID 또는 NULL |
| status | VARCHAR(20) | NOT NULL | 상태 | `ACTIVE`, `RETIRED` |
| password_must_change | BOOLEAN | NULL | 비밀번호 변경 필요 | `true`, `false` |
| created_at | TIMESTAMP | NOT NULL | 생성 일시 | `2026-01-20 10:30:00` |
| updated_at | TIMESTAMP | NULL | 수정 일시 | `2026-01-20 15:00:00` |
| retired_at | TIMESTAMP | NULL | 퇴직 일시 | `2025-12-31 23:59:59` |
| job_title | VARCHAR(100) | NULL | 직책 | `대리`, `과장`, `팀장` 등 |
| sync_status | VARCHAR(20) | NULL | 동기화 상태 | `SYNCED`, `PENDING` (Keycloak 연동용) |
| role_id | VARCHAR(50) | NULL | 역할 ID (레거시) | 사용 중단 예정 |

**인덱스**:
- UK: `login_id`
- IDX: `tenant_id`, `dept_id`, `status`, `login_id`

**FK**:
- `dept_id` → `departments(dept_id)` ON DELETE SET NULL

**데이터 표준**:
- **login_id**: 소문자 + 숫자 조합 (`admin`, `hong123`, `kim_gd`)
- **password**: BCrypt 해시만 저장 (평문 저장 금지)
- **status**: `ACTIVE`(활성), `RETIRED`(퇴직) 만 사용
- **name**: 실명 사용 권장

---

### 🎭 2.3 roles (역할)

**목적**: RBAC 역할 정의

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식 |
|--------|------|------|------|-----------|
| **role_id** | VARCHAR(36) | NOT NULL | 역할 ID (PK) | UUID 형식 |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID | `tenant-001` |
| name | VARCHAR(64) | NOT NULL | 역할명 (UK) | 대문자+언더스코어, 2-64자 |
| type | VARCHAR(32) | NOT NULL | 역할 타입 | `POSITION`, `CHANNEL`, `SKILL` |
| description | VARCHAR(255) | NULL | 역할 설명 | 목적 및 권한 범위 설명 |
| is_active | BOOLEAN | NOT NULL | 활성화 상태 | `true`(활성), `false`(비활성) |
| version | BIGINT | NOT NULL | 낙관적 잠금 버전 | 0부터 시작, 수정 시 +1 |
| created_at | TIMESTAMP | NOT NULL | 생성 일시 | `2026-01-20 10:30:00` |
| updated_at | TIMESTAMP | NOT NULL | 수정 일시 | `2026-01-20 15:00:00` |

**인덱스**:
- UK: `(tenant_id, name)`
- IDX: `tenant_id`, `is_active`

**역할 타입 (type)**:
- **POSITION**: 직급 기반 (예: `ADMIN`, `TEAM_LEADER`, `MEMBER`)
- **CHANNEL**: 채널 기반 (예: `INBOUND`, `OUTBOUND`, `CHAT`)
- **SKILL**: 스킬 기반 (예: `VIP_SUPPORT`, `TECHNICAL_SUPPORT`)

**역할명 (name) 표준**:
```
- 전체 관리자: ADMIN
- 팀장: TEAM_LEADER
- 일반 상담사: AGENT
- 인바운드 상담: INBOUND_AGENT
- 아웃바운드 상담: OUTBOUND_AGENT
- 채팅 상담: CHAT_AGENT
- VIP 전담: VIP_AGENT
- 기술 지원: TECH_SUPPORT
```

---

### 🔑 2.4 permissions (권한)

**목적**: RBAC 권한 정의

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식 |
|--------|------|------|------|-----------|
| **permission_id** | VARCHAR(36) | NOT NULL | 권한 ID (PK) | UUID 형식 |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID | `tenant-001` |
| code | VARCHAR(128) | NOT NULL | 권한 코드 (UK) | `domain:action` 형식 |
| created_at | TIMESTAMP | NOT NULL | 생성 일시 | `2026-01-20 10:30:00` |

**인덱스**:
- UK: `(tenant_id, code)`
- IDX: `tenant_id`

**권한 코드 (code) 표준**:

형식: `{domain}:{action}`

**도메인 (domain)**:
- `user`: 사용자 관리
- `org`: 조직 관리
- `role`: 역할 관리
- `permission`: 권한 관리
- `agent_role`: 사용자-역할 할당 관리
- `audit`: 감사 로그 조회

**액션 (action)**:
- `create`: 생성
- `read`: 조회
- `read:self`: 본인만 조회
- `update`: 수정
- `update:self`: 본인만 수정
- `delete`: 삭제
- `manage`: 전체 관리
- `assign`: 할당
- `view`: 보기

**표준 권한 코드 예시**:
```
user:create          - 사용자 생성
user:read            - 모든 사용자 조회
user:read:self       - 본인 정보만 조회
user:update          - 사용자 정보 수정
user:delete          - 사용자 삭제
user:manage          - 사용자 전체 관리
user:assign:role     - 사용자에게 역할 할당
org:view             - 조직도 보기
org:create           - 부서 생성
org:update           - 부서 정보 수정
org:move             - 부서 이동
org:delete           - 부서 삭제
role:create          - 역할 생성
role:read            - 역할 조회
role:update          - 역할 수정
role:delete          - 역할 삭제
role:assign          - 역할에 권한 할당
permission:create    - 권한 생성
permission:read      - 권한 조회
audit:view           - 감사 로그 조회
```

---

### 🔗 2.5 role_permissions (역할-권한 매핑)

**목적**: 역할과 권한의 다대다 관계

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식 |
|--------|------|------|------|-----------|
| **id** | BIGSERIAL | NOT NULL | 매핑 ID (PK) | 자동 증가 |
| role_id | VARCHAR(36) | NOT NULL | 역할 ID (FK) | UUID 형식 |
| permission_id | VARCHAR(36) | NOT NULL | 권한 ID (FK) | UUID 형식 |
| assigned_at | TIMESTAMP | NOT NULL | 할당 일시 | `2026-01-20 10:30:00` |

**인덱스**:
- UK: `(role_id, permission_id)` - 중복 할당 방지

**FK**:
- `role_id` → `roles(role_id)` ON DELETE CASCADE
- `permission_id` → `permissions(permission_id)` ON DELETE CASCADE

---

### 👥 2.6 agent_roles (사용자-역할 매핑)

**목적**: 사용자와 역할의 다대다 관계

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식 |
|--------|------|------|------|-----------|
| **id** | BIGSERIAL | NOT NULL | 매핑 ID (PK) | 자동 증가 |
| agent_id | VARCHAR(36) | NOT NULL | 사용자 ID (FK) | UUID 형식 |
| role_id | VARCHAR(36) | NOT NULL | 역할 ID (FK) | UUID 형식 |
| assigned_at | TIMESTAMP | NOT NULL | 할당 일시 | `2026-01-20 10:30:00` |

**인덱스**:
- UK: `(agent_id, role_id)` - 중복 할당 방지
- IDX: `agent_id`, `role_id`

**FK**:
- `agent_id` → `agents(agent_id)` ON DELETE CASCADE
- `role_id` → `roles(role_id)` ON DELETE CASCADE

---

### 📝 2.7 audit_logs (감사 로그)

**목적**: 권한 관련 모든 변경사항 추적

| 컬럼명 | 타입 | NULL | 설명 | 표준 형식 |
|--------|------|------|------|-----------|
| **audit_id** | VARCHAR(36) | NOT NULL | 감사 로그 ID (PK) | UUID 형식 |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID | `tenant-001` |
| action | VARCHAR(32) | NOT NULL | 작업 유형 | `CREATE`, `UPDATE`, `DELETE`, `ASSIGN`, `REVOKE` |
| resource_type | VARCHAR(64) | NOT NULL | 대상 리소스 타입 | `ROLE`, `PERMISSION`, `AGENT_ROLE` |
| resource_id | VARCHAR(255) | NOT NULL | 대상 리소스 ID | UUID 또는 복합 ID |
| operator_id | VARCHAR(255) | NOT NULL | 작업 수행자 ID | 사용자 UUID |
| changes | TEXT | NULL | 변경 내용 | JSON 형식 |
| timestamp | TIMESTAMP | NOT NULL | 작업 일시 | `2026-01-20 10:30:00.123` |
| remarks | TEXT | NULL | 추가 정보 | 메모, 실패 원인 등 |
| ip_address | VARCHAR(45) | NULL | 클라이언트 IP | `192.168.1.100`, IPv6 포함 |

**인덱스**:
- IDX: `tenant_id`, `resource_type`, `operator_id`, `timestamp DESC`

**작업 유형 (action) 표준**:
- `CREATE`: 생성 (역할, 권한)
- `UPDATE`: 수정
- `DELETE`: 삭제
- `ASSIGN`: 할당 (역할-권한, 사용자-역할)
- `REVOKE`: 회수

**리소스 타입 (resource_type) 표준**:
- `ROLE`: 역할
- `PERMISSION`: 권한
- `ROLE_PERMISSION`: 역할-권한 매핑
- `AGENT_ROLE`: 사용자-역할 매핑

**변경 내용 (changes) JSON 형식**:
```json
// 역할 생성
{"roleName": "TEAM_LEADER", "roleType": "POSITION"}

// 역할 수정
{"old": {"isActive": true}, "new": {"isActive": false}}

// 역할-권한 할당
{"roleId": "uuid-role", "permissionId": "uuid-perm", "permissionCode": "user:create"}

// 사용자-역할 할당
{"agentId": "uuid-agent", "roleId": "uuid-role", "roleName": "ADMIN"}
```

---

### 🗄️ 2.8 audit_logs_archive (감사 로그 아카이브)

**목적**: 6개월 이상 오래된 감사 로그 보관

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| audit_id ~ ip_address | (audit_logs와 동일) | | |
| archived_at | TIMESTAMP | NOT NULL | 아카이브 일시 |

**데이터 이동**:
- 매월 1일 자정 자동 이동 (AuditLogArchivingBatchService)
- 6개월 이전 데이터 대상

---

## 3. 데이터 표준화 규칙

### 🎯 3.1 UUID 생성 규칙

**형식**: `8-4-4-4-12` (총 36자, 하이픈 포함)
**예시**: `550e8400-e29b-41d4-a716-446655440001`

**생성 방법**:
```java
// Java
UUID.randomUUID().toString()

// PostgreSQL
gen_random_uuid()::text
```

### 🏷️ 3.2 테넌트 ID 규칙

**형식**: `tenant-{숫자 3자리}`
**예시**: `tenant-001`, `tenant-002`
**범위**: `tenant-001` ~ `tenant-999`

### 👤 3.3 사용자 로그인 ID 규칙

**형식**: 영문 소문자 + 숫자 + 언더스코어
**길이**: 4-20자
**예시**: `admin`, `hong123`, `kim_gd`, `team_leader`
**금지**: 특수문자 (@, #, $ 등), 공백, 한글

### 🔐 3.4 비밀번호 규칙

**저장**: BCrypt 해시만 저장
**형식**: `$2a$10$...` (60자)
**Java 생성**:
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashed = encoder.encode("원본비밀번호");
```

### 🎭 3.5 역할명 규칙

**형식**: 대문자 + 언더스코어
**길이**: 2-64자
**예시**: `ADMIN`, `TEAM_LEADER`, `INBOUND_AGENT`
**금지**: 소문자, 공백, 특수문자

### 🔑 3.6 권한 코드 규칙

**형식**: `{domain}:{action}`
**domain**: 소문자, 언더스코어 허용
**action**: 소문자, 언더스코어 허용, 콜론(`:`) 다중 허용
**예시**: `user:create`, `org:read:team`, `role:assign`

### 📅 3.7 날짜/시간 규칙

**타입**: `TIMESTAMP WITHOUT TIME ZONE`
**형식**: `YYYY-MM-DD HH:MI:SS`
**예시**: `2026-01-20 10:30:00`
**기본값**: `NOW()` 또는 `CURRENT_TIMESTAMP`

---

## 4. 테이블 간 관계도

```
┌─────────────────┐
│  departments    │ ◄─────┐
│  (조직 계층)     │       │ 자기참조 (parent_id)
└────────┬────────┘       │
         │                │
         │ FK: dept_id    │
         ▼                │
┌─────────────────┐       │
│     agents      │       │
│   (사용자)       │       │
└────────┬────────┘       │
         │                │
         │ FK: agent_id   │
         ▼                │
┌─────────────────┐       │
│  agent_roles    │◄──────┘
│  (다대다 매핑)   │
└────────┬────────┘
         │ FK: role_id
         ▼
┌─────────────────┐
│     roles       │
│   (역할)        │
└────────┬────────┘
         │ FK: role_id
         ▼
┌─────────────────┐
│role_permissions │
│  (다대다 매핑)   │
└────────┬────────┘
         │ FK: permission_id
         ▼
┌─────────────────┐
│  permissions    │
│   (권한)        │
└─────────────────┘

┌─────────────────┐
│  audit_logs     │ ──6개월 후──► audit_logs_archive
│  (감사 로그)     │               (아카이브)
└─────────────────┘
```

**CASCADE 규칙**:
- `role_permissions`: role 삭제 시 매핑도 삭제
- `agent_roles`: agent 또는 role 삭제 시 매핑도 삭제

**SET NULL 규칙**:
- `agents.dept_id`: department 삭제 시 NULL로 변경

**RESTRICT 규칙**:
- `departments.parent_id`: 하위 부서 존재 시 삭제 불가

---

## 5. 표준 데이터 예시

### 📦 5.1 초기 데이터셋 구성

**마이그레이션 스크립트**: `V1_0_9__Insert_Standard_Data.sql`

```
✅ 조직 구조 (3단계 계층):
   - 본부 3개
   - 팀 9개  
   - 파트 6개
   - 총 18개 부서

✅ 사용자 (16명):
   - 활성 사용자 15명
   - 퇴직 사용자 1명

✅ 권한 (35개):
   - user: 9개
   - org: 5개
   - role: 7개
   - permission: 4개
   - agent_role: 4개
   - audit: 6개

✅ 역할 (8개):
   - ADMIN (최고 관리자)
   - TEAM_LEADER (팀장)
   - AGENT (일반 상담사)
   - INBOUND_AGENT (인바운드)
   - OUTBOUND_AGENT (아웃바운드)
   - CHAT_AGENT (채팅 상담)
   - VIP_AGENT (VIP 전담)
   - TECH_SUPPORT (기술 지원)

✅ 역할-권한 매핑 (77개)
✅ 사용자-역할 매핑 (18개)
```

### 🏢 5.2 조직 구조 예시

```sql
-- 최상위 (본부)
('d0000000-0000-0000-0000-000000000001', 'tenant-001', NULL,
 '경영지원본부', '/d0000000-0000-0000-0000-000000000001', 0, '본부', NOW())

-- 2단계 (팀)
('d0000000-0000-0000-0000-000000000011', 'tenant-001',
 'd0000000-0000-0000-0000-000000000001',
 '인사팀', '/d0000000-0000-0000-0000-000000000001/d0000000-0000-0000-0000-000000000011',
 1, '팀', NOW())

-- 3단계 (파트)
('d0000000-0000-0000-0000-000000000111', 'tenant-001',
 'd0000000-0000-0000-0000-000000000011',
 '채용파트', '/d0000000-0000-0000-0000-000000000001/d0000000-0000-0000-0000-000000000011/d0000000-0000-0000-0000-000000000111',
 2, '파트', NOW())
```

### 👤 5.3 사용자 데이터 예시

```sql
INSERT INTO agents VALUES
-- 최고 관리자
('a0000000-0000-0000-0000-000000000001', 'tenant-001', 'admin',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password: admin123
 '시스템 관리자', NULL, 'ACTIVE', false, NOW(), NULL, NULL, '시스템 관리자', NULL, NULL),

-- 팀장
('a0000000-0000-0000-0000-000000000002', 'tenant-001', 'teamlead01',
 '$2a$10$...', '김팀장', 'd0000000-0000-0000-0000-000000000011',
 'ACTIVE', false, NOW(), NULL, NULL, '팀장', NULL, NULL),

-- 일반 상담사
('a0000000-0000-0000-0000-000000000003', 'tenant-001', 'agent01',
 '$2a$10$...', '이상담', 'd0000000-0000-0000-0000-000000000021',
 'ACTIVE', false, NOW(), NULL, NULL, '대리', NULL, NULL);
```

### 🎭 5.4 역할-권한 매핑 예시

```sql
-- ADMIN 역할에 모든 권한 할당
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT 'r0000000-0000-0000-0000-000000000001', permission_id, NOW()
FROM permissions WHERE tenant_id = 'tenant-001';

-- TEAM_LEADER 역할에 팀 관리 권한 할당
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT 'r0000000-0000-0000-0000-000000000002', permission_id, NOW()
FROM permissions 
WHERE tenant_id = 'tenant-001'
  AND code IN ('user:read', 'org:view', 'org:update');
```

### 👥 5.5 사용자-역할 매핑 예시

```sql
-- admin 사용자에게 ADMIN 역할 할당
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('a0000000-0000-0000-0000-000000000001', 'r0000000-0000-0000-0000-000000000001', NOW());

-- teamlead01 사용자에게 TEAM_LEADER 역할 할당
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('a0000000-0000-0000-0000-000000000002', 'r0000000-0000-0000-0000-000000000002', NOW());

-- 다중 역할 할당 예시 (상담사 + VIP 전담)
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('a0000000-0000-0000-0000-000000000003', 'r0000000-0000-0000-0000-000000000003', NOW()),
('a0000000-0000-0000-0000-000000000003', 'r0000000-0000-0000-0000-000000000007', NOW());
```

---

## 🔍 부록: 유용한 SQL 쿼리

### A. 조직도 전체 조회 (계층 구조)
```sql
WITH RECURSIVE org_tree AS (
  SELECT dept_id, name, parent_id, 0 AS level, name AS path
  FROM departments
  WHERE tenant_id = 'tenant-001' AND parent_id IS NULL
  
  UNION ALL
  
  SELECT d.dept_id, d.name, d.parent_id, o.level + 1,
         o.path || ' > ' || d.name
  FROM departments d
  INNER JOIN org_tree o ON d.parent_id = o.dept_id
)
SELECT * FROM org_tree ORDER BY path;
```

### B. 사용자별 권한 조회
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

### C. 감사 로그 조회 (최근 7일)
```sql
SELECT audit_id, action, resource_type, operator_id, timestamp, changes
FROM audit_logs
WHERE tenant_id = 'tenant-001'
  AND timestamp >= NOW() - INTERVAL '7 days'
ORDER BY timestamp DESC
LIMIT 100;
```

### D. 부서별 인원 집계
```sql
SELECT d.name AS dept_name, COUNT(a.agent_id) AS agent_count
FROM departments d
LEFT JOIN agents a ON d.dept_id = a.dept_id AND a.status = 'ACTIVE'
WHERE d.tenant_id = 'tenant-001'
GROUP BY d.dept_id, d.name
ORDER BY d.org_path;
```

---

## 📌 중요 참고사항

### ⚠️ 주의사항

1. **UUID 일관성**: 모든 엔티티 ID는 UUID (VARCHAR(36)) 사용
2. **테넌트 격리**: 모든 쿼리에 `tenant_id` 조건 필수
3. **Soft Delete**: 역할은 `is_active = false`로 논리적 삭제
4. **CASCADE 주의**: 역할/권한 삭제 시 매핑 테이블 자동 삭제됨
5. **감사 로그**: 모든 권한 변경은 자동으로 `audit_logs`에 기록

### 📋 체크리스트

프로덕션 배포 전 확인:
- [ ] 모든 FK 제약조건 확인
- [ ] 인덱스 성능 테스트
- [ ] 테넌트 격리 검증
- [ ] 감사 로그 아카이빙 스케줄 설정
- [ ] 백업 정책 수립

---

## 📚 관련 문서

- [AUDIT_AND_CONSTANTS_ANALYSIS.md](./AUDIT_AND_CONSTANTS_ANALYSIS.md) - 감사 로그 & 상수 분석
- [V1_0_0__Complete_Init.sql](./src/main/resources/db/migration/V1_0_0__Complete_Init.sql) - DB 초기화 스크립트
- [V1_0_9__Insert_Standard_Data.sql](./src/main/resources/db/migration/V1_0_9__Insert_Standard_Data.sql) - 표준 데이터 삽입

---

**문서 버전**: 2.0
**최종 검증일**: 2026-01-20
**작성자**: Identity Modulith Team

> **목적**: 데이터베이스 설계, 테이블 구조, 표준 데이터를 한 곳에서 확인  
> **대상**: 개발팀, 운영팀  
> **버전**: 2.0  
> **최종 수정일**: 2026-01-16

---

## 📋 목차
1. [데이터베이스 개요](#데이터베이스-개요)
2. [테이블 구조](#테이블-구조)
3. [테이블 상세 명세](#테이블-상세-명세)
4. [테이블 간 연관관계](#테이블-간-연관관계)
5. [컬럼 데이터 형식 표준](#컬럼-데이터-형식-표준)
6. [표준 데이터 가이드](#표준-데이터-가이드)
7. [권한 및 역할 표준](#권한-및-역할-표준)

---

## 데이터베이스 개요

### 설계 목표
- **멀티테넌시(Multi-Tenancy)**: 각 테이블에 tenant_id로 데이터 격리
- **UUID 통일**: 모든 엔티티 ID는 UUID (VARCHAR(36))로 통일
- **조직 트리**: 자기참조를 이용한 부서 계층 구조
- **RBAC**: 역할 기반 접근 제어 (Role-Based Access Control)
- **감사 추적**: 모든 테이블에 created_at, 주요 작업은 audit_logs로 기록

### 핵심 원칙
```
✅ ID 타입: UUID (VARCHAR(36)) 통일
✅ 다대다 관계: 중간 테이블로 명시적 관리
✅ 자기참조: departments의 parent_id
✅ Soft Delete: agents의 status와 retired_at
✅ 데이터 격리: 모든 테이블에 tenant_id (NOT NULL)
```

---

## 테이블 구조

### 전체 테이블 목록 (6개 + 2개)

| 테이블 | 모듈 | 용도 | PK 타입 | 참고 |
|--------|------|------|---------|------|
| **departments** | Organization | 조직/부서 계층 | VARCHAR(36) | 자기참조 트리 |
| **agents** | User | 사용자/직원 | VARCHAR(36) | Soft Delete |
| **roles** | RBAC | 역할 정의 | VARCHAR(36) | 권한 묶음 |
| **permissions** | RBAC | 권한 정의 | VARCHAR(36) | 최소 단위 권한 |
| **role_permissions** | RBAC | 역할-권한 매핑 | BIGSERIAL | N:M 중간 테이블 |
| **agent_roles** | RBAC | 사용자-역할 매핑 | BIGSERIAL | N:M 중간 테이블 |
| **audit_logs** | Audit | 감사 로그 | BIGSERIAL | 변경 이력 추적 |
| **audit_archive** | Audit | 감사 로그 아카이브 | BIGSERIAL | 90일 이상 로그 |

### ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────┐
│               Identity Modulith Database ERD                 │
└─────────────────────────────────────────────────────────────┘

┌──────────────────┐
│   departments    │ (자기참조)
├──────────────────┤
│ PK: dept_id (U)  │
│     tenant_id    │
│ FK: parent_id ───┼─────┐
│     name         │     │
│     org_path     │     │
│     depth        │     │
│     type         │     │
│     created_at   │     │
└──────────────────┘     │
         ▲               │
         │ 1:N (자기참조)|
         └───────────────┘
         │
         │ 1:N (소속)
         ▼
┌──────────────────┐
│     agents       │
├──────────────────┤
│ PK: agent_id (U) │
│     tenant_id    │
│     login_id (U) │
│     password     │
│     name         │
│ FK: dept_id ─────┘
│     status       │
│     ...etc       │
└──────────────────┘
         │
         │ N:M (역할 할당)
         ▼
┌──────────────────┐       ┌──────────────────┐
│   agent_roles    │       │      roles       │
├──────────────────┤       ├──────────────────┤
│ PK: id           │       │ PK: role_id (U)  │
│ FK: agent_id ────┼──────→│     tenant_id    │
│ FK: role_id ─────┼──────→│     name (U)     │
│     assigned_at  │       │     type         │
└──────────────────┘       │     created_at   │
                           └──────────────────┘
                                   │
                                   │ N:M
                                   ▼
                           ┌──────────────────┐
                           │ role_permissions │
                           ├──────────────────┤
                           │ PK: id           │
                           │ FK: role_id ─────┘
                           │ FK: permission_id┐
                           │     assigned_at  │
                           └──────────────────┘
                                   │
                                   ▼
                           ┌──────────────────┐
                           │   permissions    │
                           ├──────────────────┤
                           │ PK: permission_id│
                           │     tenant_id    │
                           │     code (U)     │
                           │     created_at   │
                           └──────────────────┘

범례: PK=Primary Key, FK=Foreign Key, U=UUID, N:M=다대다
```

---

## 테이블 상세 명세

### 1. departments (조직/부서 테이블)

**목적**: 회사 조직 계층 구조 관리 (트리 구조)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| dept_id | VARCHAR(36) | PK | 부서 ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID (멀티테넌시) |
| parent_id | VARCHAR(36) | FK (자기참조) | 상위 부서 ID (NULL이면 최상위) |
| name | VARCHAR(100) | NOT NULL | 부서명 |
| org_path | VARCHAR(500) | NOT NULL, UNIQUE | 조직 경로 (예: /dept1/dept2/dept3) |
| depth | INTEGER | NOT NULL | 트리 깊이 (0부터 시작) |
| type | VARCHAR(50) | | 부서 타입 (HEADQUARTERS, DIVISION, TEAM) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |

**인덱스**:
- PK: dept_id
- UK: (tenant_id, org_path)
- FK: parent_id → dept_id (자기참조, ON DELETE RESTRICT)
- IDX: (tenant_id), (parent_id), (org_path)

**특징**:
- **자기참조 (Self-Join)**: parent_id로 상하 관계 표현
- **Closure Table 대안**: org_path로 계층 탐색 최적화
- **삭제 제약**: RESTRICT로 하위 부서 있으면 삭제 불가

**예시 데이터**:
```sql
dept_id              | name        | parent_id | org_path          | depth | type
---------------------|-------------|-----------|-------------------|-------|-------------
d50e8400-e29b-...001 | 넥스프론본부 | NULL      | /d50e...001       | 0     | HEADQUARTERS
d50e8400-e29b-...002 | 고객지원사부 | ...001    | /d50e...001/002   | 1     | DIVISION
d50e8400-e29b-...005 | 전화상담팀  | ...002    | /d50e...001/002/005 | 2   | TEAM
```

---

### 2. agents (사용자/직원 테이블)

**목적**: 시스템 사용자 정보 관리

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| agent_id | VARCHAR(36) | PK | 사용자 ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID |
| login_id | VARCHAR(100) | NOT NULL, UNIQUE | 로그인 ID |
| password | VARCHAR(255) | NOT NULL | 비밀번호 (BCrypt 해시) |
| name | VARCHAR(100) | NOT NULL | 사용자명 |
| dept_id | VARCHAR(36) | FK | 소속 부서 ID (NULL 가능) |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | 상태 (ACTIVE, RETIRED) |
| password_must_change | BOOLEAN | DEFAULT false | 비밀번호 변경 필요 여부 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMP | | 수정 일시 |
| retired_at | TIMESTAMP | | 퇴직 일시 |
| job_title | VARCHAR(100) | | 직책 |
| sync_status | VARCHAR(20) | | 동기 상태 |

**인덱스**:
- PK: agent_id
- UK: login_id
- FK: dept_id → departments.dept_id (ON DELETE SET NULL)
- IDX: (tenant_id), (dept_id), (status), (login_id)

**특징**:
- **Soft Delete**: status='RETIRED'로 논리적 삭제 (물리적 삭제 X)
- **다중 역할**: agent_roles 테이블로 여러 역할 할당 가능
- **부서 연결**: dept_id로 조직 구조와 연결

**예시 데이터**:
```sql
agent_id             | login_id    | name      | dept_id      | status
---------------------|-------------|-----------|--------------|--------
550e8400-e29b-...101 | admin       | 시스템관리자 | d50e...001  | ACTIVE
550e8400-e29b-...104 | phone_ag01 | 박상담     | d50e...005  | ACTIVE
550e8400-e29b-...199 | retired_usr | 퇴직자    | d50e...005  | RETIRED
```

---

### 3. roles (역할 테이블)

**목적**: RBAC 역할 정의 (권한 묶음)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| role_id | VARCHAR(36) | PK | 역할 ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID |
| name | VARCHAR(64) | NOT NULL, UNIQUE | 역할명 (ADMIN, MANAGER 등) |
| type | VARCHAR(32) | NOT NULL | 역할 타입 (POSITION, CHANNEL, SKILL) |
| description | VARCHAR(255) | | 역할 설명 |
| is_active | BOOLEAN | DEFAULT true | 활성화 여부 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |

**인덱스**:
- PK: role_id
- UK: (tenant_id, name)
- IDX: (tenant_id)

**역할 분류**:

| 타입 | 설명 | 예시 |
|------|------|------|
| POSITION | 직급 기반 (직책) | ADMIN, MANAGER, TEAM_LEAD, MEMBER |
| CHANNEL | 채널 기반 (업무 채널) | PHONE_AGENT, CHAT_AGENT, EMAIL_AGENT, SUPERVISOR |
| SKILL | 역량 기반 | (확장 가능) |

**특징**:
- **다중 역할 조합**: 사용자는 POSITION + CHANNEL 조합 가능
- 예: 박상담 = MEMBER (직급) + PHONE_AGENT (채널)

**예시 데이터**:
```sql
role_id              | name         | type      | is_active
---------------------|--------------|-----------|----------
660e8400-e29b-...001 | ADMIN        | POSITION  | true
660e8400-e29b-...005 | PHONE_AGENT  | CHANNEL   | true
```

---

### 4. permissions (권한 테이블)

**목적**: 시스템 권한 정의 (최소 단위 권한)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| permission_id | VARCHAR(36) | PK | 권한 ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID |
| code | VARCHAR(128) | NOT NULL, UNIQUE | 권한 코드 (domain:action 형식) |
| description | VARCHAR(255) | | 권한 설명 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |

**인덱스**:
- PK: permission_id
- UK: (tenant_id, code)
- IDX: (tenant_id)

**권한 코드 형식**:
```
{domain}:{action}[:{resource}]

도메인 (8개):
├─ user:      사용자 관리 (9개)
├─ org:       조직 관리 (6개)
├─ rbac:      RBAC 관리 (9개)
├─ report:    보고서 (4개)
├─ phone:     전화 채널 (3개)
├─ chat:      채팅 채널 (2개)
├─ email:     이메일 채널 (1개)
└─ queue:     큐 관리 (1개)

총 35개 권한
```

**예시 데이터**:
```sql
permission_id        | code                | description
---------------------|---------------------|------------------
550e8400-e29b-...001 | user:create         | 사용자 생성
550e8400-e29b-...029 | phone:accept        | 전화 수락
550e8400-e29b-...032 | chat:send           | 채팅 전송
```

---

### 5. role_permissions (역할-권한 매핑 테이블)

**목적**: 역할에 권한 할당 (N:M 관계)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 매핑 ID (자동 증가) |
| role_id | VARCHAR(36) | FK, NOT NULL | 역할 ID |
| permission_id | VARCHAR(36) | FK, NOT NULL | 권한 ID |
| assigned_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 할당 일시 |

**인덱스**:
- PK: id
- UK: (role_id, permission_id)
- FK: role_id → roles.role_id (ON DELETE CASCADE)
- FK: permission_id → permissions.permission_id (ON DELETE CASCADE)

**특징**:
- **다대다 관계**: 한 역할에 여러 권한 할당 가능
- **동적 권한 관리**: 역할 변경 시 자동 반영
- **CASCADE 삭제**: 역할/권한 삭제 시 매핑도 자동 삭제

**권한 배분 예시**:
```sql
ADMIN:     35개 (전체)
MANAGER:   12개 (사용자, 조직, 보고서)
TEAM_LEAD:  5개 (읽기, 조직 뷰, 보고서)
MEMBER:     4개 (본인 읽기, 조직 뷰, 보고서)

PHONE_AGENT:  3개 (전화 관련)
CHAT_AGENT:   2개 (채팅 관련)
EMAIL_AGENT:  1개 (이메일 관련)
SUPERVISOR:   7개 (모든 채널 + 큐)

총 77개 매핑
```

---

### 6. agent_roles (사용자-역할 매핑 테이블)

**목적**: 사용자에게 역할 할당 (N:M 관계)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 매핑 ID (자동 증가) |
| agent_id | VARCHAR(36) | FK, NOT NULL | 사용자 ID |
| role_id | VARCHAR(36) | FK, NOT NULL | 역할 ID |
| assigned_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 할당 일시 |

**인덱스**:
- PK: id
- UK: (agent_id, role_id) - 중복 방지
- FK: agent_id → agents.agent_id (ON DELETE CASCADE)
- FK: role_id → roles.role_id (ON DELETE CASCADE)
- IDX: (agent_id), (role_id)

**특징**:
- **다중 역할**: 사용자는 여러 역할 보유 가능 (예: MEMBER + PHONE_AGENT + SUPERVISOR)
- **동적 할당**: 역할 추가/제거 시 자동 반영
- **권한 계산**: 모든 역할의 권한 합집합 = 사용자의 최종 권한

**예시 데이터**:
```sql
agent_id (박상담)    | role_id (역할)
---------------------|----------------------
550e8400-e29b-...104 | 660e8400-e29b-...004 (MEMBER)
550e8400-e29b-...104 | 660e8400-e29b-...005 (PHONE_AGENT)
```

---

### 7. audit_logs (감사 로그 테이블)

**목적**: 시스템 주요 작업 이력 추적

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 로그 ID (자동 증가) |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID |
| action | VARCHAR(100) | NOT NULL | 작업 (ROLE_ASSIGNED, PERMISSION_CREATED 등) |
| target_type | VARCHAR(50) | NOT NULL | 대상 타입 (ROLE, PERMISSION, USER 등) |
| target_id | VARCHAR(100) | NOT NULL | 대상 ID |
| actor_id | VARCHAR(36) | NOT NULL | 작업자 ID |
| details | TEXT | | 상세 정보 (JSON 형식) |
| ip_address | VARCHAR(45) | | 작업자 IP |
| timestamp | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 발생 일시 |

**인덱스**:
- PK: id
- IDX: (tenant_id, timestamp), (actor_id), (target_type, target_id)

**특징**:
- **불변 로그**: 생성 후 수정/삭제 불가
- **90일 자동 아카이빙**: audit_archive로 이동
- **JSON 상세 정보**: 변경 전후 값 저장

---

### 8. audit_archive (감사 로그 아카이브 테이블)

**목적**: 90일 이상 오래된 감사 로그 보관

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| (audit_logs와 동일) | | | |
| archived_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 아카이빙 일시 |

**특징**:
- **자동 아카이빙**: 배치 작업으로 90일 초과 로그 이동
- **장기 보관**: 법적 요구사항 대응
- **검색 최적화**: 최근 로그는 audit_logs에서만 검색

---

## 테이블 간 연관관계

### 1. 일대다 (One-to-Many) 관계

#### departments (1) → departments (N) - 자기참조
```
상위 부서 (parent) → 하위 부서들 (자식)

관계: 부모-자식
FK: parent_id → dept_id
특징: 자기참조, 트리 구조
삭제 정책: ON DELETE RESTRICT (하위 부서 있으면 삭제 불가)

예시:
넥스프론 본부 (root)
├─ 고객지원사업부
│  ├─ 전화상담팀
│  └─ 채팅상담팀
└─ 기술개발본부
   └─ Backend개발팀
```

#### departments (1) → agents (N)
```
부서 (department) → 소속 직원들 (employees)

관계: 조직 포함 관계
FK: agents.dept_id → departments.dept_id
특징: 하나의 부서에 여러 직원
삭제 정책: ON DELETE SET NULL (부서 삭제 시 직원의 dept_id = NULL)

예시:
전화상담팀 (1개)
├─ 이팀장 (1명)
├─ 박상담 (1명)
└─ 최상담 (1명)
```

---

### 2. 다대다 (Many-to-Many) 관계

#### agents (N) ↔ roles (M) via agent_roles
```
사용자 ←→ 역할

구조:
agents → agent_roles → roles

특징:
- 한 사용자가 여러 역할 보유
- 한 역할이 여러 사용자에게 할당
- 중간 테이블: agent_roles

예시:
박상담 (1명)
├─ MEMBER (직급)
└─ PHONE_AGENT (채널)

MEMBER 역할
├─ 박상담
├─ 정상담
├─ 강상담
└─ ... (7명)

삭제 정책: ON DELETE CASCADE (양쪽 모두)
```

#### roles (N) ↔ permissions (M) via role_permissions
```
역할 ←→ 권한

구조:
roles → role_permissions → permissions

특징:
- 한 역할에 여러 권한 포함
- 한 권한이 여러 역할에 할당 가능
- 중간 테이블: role_permissions

예시:
ADMIN 역할 (1개) → 35개 권한 (모두)
MEMBER 역할 (1개) → 4개 권한 (최소)

삭제 정책: ON DELETE CASCADE (양쪽 모두)
```

---

### 3. 권한 체크 흐름

**사용자의 최종 권한 계산**:

```
1단계: 사용자 조회
┌──────────────┐
│ agents       │ (agent_id로 조회)
│ agent_id=... │
└──────────────┘
        ↓

2단계: 사용자의 모든 역할 조회
┌──────────────┐
│ agent_roles  │ (WHERE agent_id = ?)
│ role_id=... │
│ role_id=... │ (다중 역할)
└──────────────┘
        ↓

3단계: 각 역할의 모든 권한 조회
┌──────────────┐
│ role_permissions │ (WHERE role_id IN (...))
│ permission_id=... │
│ permission_id=... │
└──────────────┘
        ↓

4단계: 모든 권한 코드 조회
┌──────────────┐
│ permissions  │
│ code='user:create' │
│ code='phone:accept' │
└──────────────┘
        ↓

5단계: 권한 확인
최종 권한 = 모든 역할의 권한 합집합 (Union)
```

**SQL 예시**:
```sql
-- 특정 사용자의 모든 권한 조회
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

## 컬럼 데이터 형식 표준

### 1. ID 컬럼 (모두 UUID로 통일)

| 컬럼명 | 타입 | 크기 | 형식 | 예시 |
|--------|------|------|------|------|
| dept_id | VARCHAR | 36 | UUID | d50e8400-e29b-41d4-a716-446655440001 |
| agent_id | VARCHAR | 36 | UUID | 550e8400-e29b-41d4-a716-446655440101 |
| role_id | VARCHAR | 36 | UUID | 660e8400-e29b-41d4-a716-446655440001 |
| permission_id | VARCHAR | 36 | UUID | 550e8400-e29b-41d4-a716-446655440001 |

**UUID 형식**:
```
xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  8자   - 4자 - 4자 - 4자 -    12자
  총 36자 (하이픈 포함)
```

---

### 2. 문자열 컬럼 표준

| 컬럼명 | 최대길이 | 설명 | 예시 |
|--------|---------|------|------|
| tenant_id | 50 | 테넌트 ID (고정) | tenant-001 |
| login_id | 100 | 로그인 ID (영숫자, -, _) | phone_agent01 |
| password | 255 | BCrypt 해시 | $2a$10$N9qo8... |
| name | 100 | 사용자/부서명 | 박상담, 전화상담팀 |
| org_path | 500 | 조직 경로 (UUID 기반) | /d50e8400.../d50e8400.../... |
| job_title | 100 | 직책 | 팀장, 과장 |
| type (departments) | 50 | 부서 타입 | HEADQUARTERS, DIVISION, TEAM |
| type (roles) | 32 | 역할 타입 | POSITION, CHANNEL, SKILL |
| status (agents) | 20 | 상태 | ACTIVE, RETIRED |
| name (roles) | 64 | 역할명 (대문자, _) | ADMIN, TEAM_LEAD, PHONE_AGENT |
| code (permissions) | 128 | 권한 코드 (도메인:액션) | user:create, phone:accept |

---

### 3. 시간 컬럼 표준

| 컬럼명 | 타입 | 형식 | 설명 | 예시 |
|--------|------|------|------|------|
| created_at | TIMESTAMP | ISO 8601 | 생성 일시 (자동) | 2026-01-16 10:00:00 |
| updated_at | TIMESTAMP | ISO 8601 | 수정 일시 (자동) | 2026-01-16 10:05:00 |
| assigned_at | TIMESTAMP | ISO 8601 | 할당 일시 | 2026-01-16 10:00:00 |
| retired_at | TIMESTAMP | ISO 8601 | 퇴직 일시 (NULL 가능) | 2025-12-14 17:00:00 |
| timestamp | TIMESTAMP | ISO 8601 | 감사 로그 발생 일시 | 2026-01-16 10:00:00 |

---

### 4. NULL 정책

| 컬럼명 | 테이블 | NULL 허용 | 이유 | 비고 |
|--------|--------|----------|------|------|
| parent_id | departments | YES | 최상위 부서일 수 있음 | 루트는 NULL |
| dept_id | agents | YES | 부서 미정 직원 가능 | ON DELETE SET NULL |
| updated_at | agents | YES | 생성 후 수정 없을 수 있음 | 선택사항 |
| retired_at | agents | YES | 활성 직원은 NULL | Soft Delete |
| job_title | agents | YES | 직책 미정 가능 | 선택사항 |
| description | roles, permissions | YES | 설명 선택사항 | |
| ip_address | audit_logs | YES | IP 추적 불가능할 수 있음 | |

---

## 표준 데이터 가이드

### 표준 데이터셋

| 항목 | 수량 | 설명 |
|------|------|------|
| **Departments** | 13개 | 본부(1) + 사업부(3) + 팀(9) |
| **Agents** | 16명 | 활성(15) + 퇴직(1) |
| **Roles** | 8개 | POSITION(4) + CHANNEL(4) |
| **Permissions** | 35개 | 8개 도메인 |
| **Role-Permissions** | 77개 | 역할별 권한 매핑 |
| **Agent-Roles** | ~30개 | 사용자별 다중 역할 |

### 조직 구조 예시

```
넥스프론 본부 (HEADQUARTERS)
├─ 고객지원사업부 (DIVISION)
│  ├─ 전화상담팀 (TEAM)
│  │  ├─ 이팀장 (TEAM_LEAD + SUPERVISOR)
│  │  ├─ 박상담 (MEMBER + PHONE_AGENT)
│  │  └─ 최상담 (MEMBER + PHONE_AGENT)
│  ├─ 채팅상담팀 (TEAM)
│  └─ VIP고객지원팀 (TEAM)
├─ 영업사업부 (DIVISION)
└─ 기술개발본부 (DIVISION)
   ├─ Backend개발팀 (TEAM)
   ├─ Frontend개발팀 (TEAM)
   └─ DevOps팀 (TEAM)
```

---

## 권한 및 역할 표준

### 권한(Permission) 코드 규칙

**형식**: `{domain}:{action}[:{resource}]`

### 도메인별 권한 목록 (총 35개)

#### 1. 사용자 관리 (user, agent) - 9개
```
- user:create          사용자 생성
- user:read            사용자 조회
- user:update          사용자 수정
- user:delete          사용자 삭제
- user:read:self       본인 정보 조회
- user:update:self     본인 정보 수정
- user:assign:role     역할 할당
- user:reset:password  비밀번호 재설정
- agent:manage         에이전트 전체 관리
```

#### 2. 조직 관리 (org, department) - 6개
```
- org:view             조직 조회
- org:create           조직 생성
- org:update           조직 수정
- org:move             조직 이동
- org:delete           조직 삭제
- org:manage           조직 전체 관리
```

#### 3. RBAC 관리 (rbac, role, permission) - 9개
```
- rbac:view            RBAC 조회
- rbac:create:role     역할 생성
- rbac:update:role     역할 수정
- rbac:delete:role     역할 삭제
- rbac:create:permission 권한 생성
- rbac:update:permission 권한 수정
- rbac:delete:permission 권한 삭제
- rbac:assign:permission 권한 할당
- rbac:configure       RBAC 전체 설정
```

#### 4. 보고서 및 감시 (report, audit, cdr) - 7개
```
- report:view          보고서 조회
- report:read          보고서 읽기
- report:export        보고서 내보내기
- report:manage        보고서 관리
- audit:view           감사 로그 조회
- audit:export         감사 로그 내보내기
- cdr:view             CDR 조회
```

#### 5. 채널 관리 (phone, chat, email, queue) - 7개
```
- phone:accept         전화 수락
- phone:hold           전화 보류
- phone:transfer       전화 전환
- chat:send            채팅 전송
- chat:receive         채팅 수신
- email:send           이메일 전송
- queue:manage         큐 관리
```

#### 6. 기타 (dashboard, quality) - 2개
```
- dashboard:view       대시보드 조회
- quality:manage       품질 관리
```

---

### 역할(Role) 정의

#### 역할 타입
- **POSITION**: 조직상 직위 (ADMIN, MANAGER, TEAM_LEAD, MEMBER)
- **CHANNEL**: 상담 채널 (PHONE_AGENT, CHAT_AGENT, EMAIL_AGENT, SUPERVISOR)
- **SKILL**: 기술/스킬 (향후 확장용)

#### 기본 역할 및 권한 할당

| 역할 | 타입 | 권한 수 | 주요 권한 |
|------|------|---------|-----------|
| **ADMIN** | POSITION | 35개 (전체) | user:*, org:*, rbac:*, report:*, audit:*, 모든 채널 |
| **MANAGER** | POSITION | 12개 | user 생성/수정, org 생성/수정/이동, report 전체 |
| **TEAM_LEAD** | POSITION | 5개 | user:read, org:view, report:view/read/export |
| **MEMBER** | POSITION | 4개 | user:read:self, user:update:self, org:view, report:view |
| **PHONE_AGENT** | CHANNEL | 3개 | phone:accept, phone:hold, phone:transfer |
| **CHAT_AGENT** | CHANNEL | 2개 | chat:send, chat:receive |
| **EMAIL_AGENT** | CHANNEL | 1개 | email:send |
| **SUPERVISOR** | CHANNEL | 7개 | 모든 채널 + queue:manage |

---

### 권한 계층 예시

```
ADMIN (35개 권한 - 전체)
├─ user:* (9개)
├─ org:* (6개)
├─ rbac:* (9개)
├─ report:* (4개)
├─ audit:* (2개)
├─ 채널 전체 (7개)
└─ dashboard, quality (2개)

MANAGER (12개 권한)
├─ user: create, read, update, assign:role, reset:password
├─ org: view, create, update, move
└─ report: view, read, export

MEMBER (4개 권한 - 최소)
├─ user:read:self
├─ user:update:self
├─ org:view
└─ report:view

PHONE_AGENT (3개 권한)
├─ phone:accept
├─ phone:hold
└─ phone:transfer
```

---

## 설계 원칙 및 이유

### 1. UUID로 통일한 이유
- ✅ 분산 환경 지원 (ID 충돌 없음)
- ✅ 멀티테넌시 안전성 (테넌트 간 ID 충돌 불가)
- ✅ 보안 (순차 ID 노출 방지)
- ✅ 일관성 (모든 엔티티 동일한 형식)

### 2. 자기참조 FK 사용 이유
- ✅ 계층 구조 표현 최적화
- ✅ org_path로 경로 탐색 빠름
- ✅ depth로 레벨 쉽게 파악
- ✅ 유연한 부서 추가/제거

### 3. 중간 테이블 사용 이유
- ✅ N:M 관계를 명시적으로 관리
- ✅ 할당 일시 등 메타데이터 저장 가능
- ✅ 감사 추적 용이
- ✅ 성능 최적화 (조인 명확화)

### 4. Soft Delete 사용 이유
- ✅ 히스토리 유지
- ✅ 감사 추적 (언제 퇴직했는지)
- ✅ 데이터 복구 가능
- ✅ 참조 무결성 유지

### 5. 멀티테넌시 구현 이유
- ✅ 데이터 격리 (tenant_id 필수)
- ✅ SaaS 확장성
- ✅ 보안 (테넌트 간 데이터 접근 불가)

---

## 부록: 빠른 참조

### 주요 쿼리 패턴

#### 1. 사용자의 모든 권한 조회
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

#### 2. 부서의 전체 하위 부서 조회 (트리)
```sql
SELECT *
FROM departments
WHERE org_path LIKE CONCAT(:targetOrgPath, '%')
  AND tenant_id = :tenantId
ORDER BY depth, name;
```

#### 3. 역할에 할당된 모든 권한 조회
```sql
SELECT p.code, p.description
FROM role_permissions rp
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE rp.role_id = :roleId
  AND p.tenant_id = :tenantId;
```

#### 4. 사용자가 특정 권한을 보유했는지 확인
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

## 6. 데이터베이스 초기화 방법

### 📌 완전 초기화 (권장)

**⚠️ 주의**: 모든 데이터가 삭제됩니다!

#### 방법 1: SQL 스크립트 직접 실행
```bash
# 1. PostgreSQL 클라이언트에서 실행
psql -U nexfron -d nexfron -f reset_database_clean.sql

# 2. 애플리케이션 재시작 (Flyway 자동 마이그레이션)
./gradlew bootRun
```

#### 방법 2: DBeaver/DataGrip 등 GUI 도구
1. `reset_database_clean.sql` 파일 열기
2. 전체 선택 후 실행 (Ctrl+Enter)
3. 결과 확인: `✅ 데이터베이스 완전 초기화 완료!`
4. 애플리케이션 재시작

### 🔄 Flyway 마이그레이션

애플리케이션 시작 시 자동으로:
1. `V1_0_0__Complete_Init.sql` 스키마 생성
2. 표준 데이터 자동 삽입 (35권한 + 8역할 + 16사용자)

### 📊 초기화 후 확인

```sql
-- 테이블 목록 확인
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- 데이터 건수 확인
SELECT 'departments' as table_name, COUNT(*) as count FROM departments
UNION ALL SELECT 'agents', COUNT(*) FROM agents
UNION ALL SELECT 'roles', COUNT(*) FROM roles
UNION ALL SELECT 'permissions', COUNT(*) FROM permissions
UNION ALL SELECT 'role_permissions', COUNT(*) FROM role_permissions
UNION ALL SELECT 'agent_roles', COUNT(*) FROM agent_roles;
```

**예상 결과**:
- departments: 16개
- agents: 16개 (admin 포함)
- roles: 8개
- permissions: 35개
- role_permissions: 77개
- agent_roles: 22개

---

**문서 작성일**: 2026-01-21  
**작성자**: Identity System Team  
**버전**: 2.0.0 CLEAN  
**상태**: 최종 승인 ✅
---

> ⚠️ **주의사항**  
> - 모든 테이블은 tenant_id로 격리되어야 합니다  
> - ID는 반드시 UUID (VARCHAR(36)) 형식을 사용해야 합니다  
> - 삭제 정책(ON DELETE)은 반드시 문서대로 설정해야 합니다  
> - 권한 코드는 `domain:action` 형식을 엄격히 준수해야 합니다

