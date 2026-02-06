# Database Schema - 데이터베이스 스키마 설계 (v3.0)

Identity Modulith의 실제 구현된 데이터베이스 스키마 정의

> ✅ **이 문서는 실제 구현을 반영합니다**  
> 기준: `V1_0_0__Complete_Init.sql` (2026-02-05)  
> 프로젝트의 실제 엔티티 구조 및 시나리오 문서와 완전히 일치합니다.

---

## 1. 개요

### 1.1 데이터베이스
- **DBMS**: PostgreSQL 15+
- **스키마**: public (단일 스키마, 모듈별 테이블 접두어)
- **문자 인코딩**: UTF-8
- **타임존**: UTC (CURRENT_TIMESTAMP 사용)

### 1.2 테이블 명명 규칙
```
{module}_{entity}

예시:
- user_agents           (User 모듈 - Agent 테이블)
- org_departments       (Organization 모듈 - Department 테이블)
- rbac_roles            (RBAC 모듈 - Role 테이블)
```

### 1.3 ID 전략
- **모든 PK**: VARCHAR(50) (UUID 형식, 애플리케이션 생성)
- **UUID 생성**: Java `UUID.randomUUID().toString()`
- **형식 예시**: `agent-admin-001`, `dept-root-001`, `role-admin-001`
- **장점**: 분산 환경 안전, 예측 불가능, 멀티테넌시 친화적

### 1.4 공통 컬럼
```sql
-- 거의 모든 테이블에 포함
tenant_id       VARCHAR(50)  NOT NULL    -- Multi-tenancy 지원
created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
```

---

## 2. 테이블 목록 및 구조

### 2.1 전체 테이블 (6개)

| # | 테이블명 | 모듈 | 설명 | PK | FK |
|---|---------|------|------|----|----|
| 1 | **org_departments** | Organization | 조직/부서 (트리) | dept_id | parent_dept_id → self |
| 2 | **user_agents** | User | 사용자/상담사 | agent_id | dept_id → org_departments |
| 3 | **rbac_roles** | RBAC | 역할 정의 | role_id | - |
| 4 | **rbac_permissions** | RBAC | 권한 정의 | permission_id | - |
| 5 | **rbac_role_permissions** | RBAC | 역할-권한 M:N | (role_id, permission_id) | → roles, permissions |
| 6 | **user_agent_roles** | RBAC | 사용자-역할 M:N | (agent_id, role_id) | → user_agents, roles |

---

## 3. ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Identity Modulith ERD (v3.0)                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐         ┌─────────────────┐                           │
│  │  user_agents    │         │org_departments  │                           │
│  ├─────────────────┤         ├─────────────────┤                           │
│  │ PK agent_id (V) │    ┌───▶│ PK dept_id (V)  │◀──┐                       │
│  │    tenant_id    │    │    │    tenant_id    │   │                       │
│  │    login_id (U) │    │    │    name         │   │ self-ref              │
│  │    password     │    │    │    type         │   │ (parent)              │
│  │    name         │    │    │ FK parent_dept  │───┘                       │
│  │ FK dept_id      │────┘    │    org_path (M) │                           │
│  │    status       │         │    depth        │                           │
│  │    version      │         │    is_active    │                           │
│  └────────┬────────┘         └─────────────────┘                           │
│           │                                                                 │
│           │ M:N                                                             │
│           ▼                                                                 │
│  ┌─────────────────┐         ┌─────────────────┐                           │
│  │user_agent_roles │         │   rbac_roles    │                           │
│  ├─────────────────┤         ├─────────────────┤                           │
│  │ FK agent_id     │────────▶│ PK role_id (V)  │◀──┐                       │
│  │ FK role_id      │────────▶│    tenant_id    │   │                       │
│  │    assigned_at  │         │    name (UK)    │   │                       │
│  └─────────────────┘         │    type         │   │                       │
│                              │    data_scope   │   │                       │
│                              │    is_active    │   │                       │
│                              └────────┬────────┘   │                       │
│                                       │            │                       │
│                                       │ M:N        │                       │
│                                       ▼            │                       │
│  ┌─────────────────┐         ┌─────────────────┐   │                       │
│  │rbac_permissions │         │rbac_role_perms  │   │                       │
│  ├─────────────────┤         ├─────────────────┤   │                       │
│  │ PK permission_id│◀────────│ FK role_id      │───┘                       │
│  │    tenant_id    │◀────────│ FK permission_id│                           │
│  │    code (UK)    │         │    assigned_at  │                           │
│  │    category     │         └─────────────────┘                           │
│  └─────────────────┘                                                       │
│                                                                             │
│  (V) = VARCHAR(50)                                                          │
│  (U) = UNIQUE with tenant_id                                                │
│  (M) = Materialized Path                                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Organization 모듈

### 4.1 org_departments (조직/부서)

**목적**: 조직 계층 구조 관리 (Materialized Path 패턴)

**DDL**:
```sql
CREATE TABLE IF NOT EXISTS org_departments (
    -- 식별자
    dept_id             VARCHAR(50)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    
    -- 기본 정보
    name                VARCHAR(100)    NOT NULL,
    type                VARCHAR(20)     NOT NULL,          -- COMPANY, DIVISION, TEAM, GROUP, CUSTOM
    custom_type_name    VARCHAR(50),                       -- type=CUSTOM일 때만
    
    -- 계층 구조 (Materialized Path)
    parent_dept_id      VARCHAR(50),                       -- FK → self, NULL=루트
    org_path            TEXT            NOT NULL,          -- 예: /dept-root/dept-child/
    depth               INTEGER         NOT NULL DEFAULT 0,
    display_order       INTEGER         NOT NULL DEFAULT 0,
    
    -- 추가 정보
    manager_id          VARCHAR(50),                       -- 부서장 ID
    description         TEXT,
    
    -- 상태
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    
    -- 감사
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50),
    
    -- 제약조건
    CONSTRAINT fk_parent_dept FOREIGN KEY (parent_dept_id)
        REFERENCES org_departments(dept_id) ON DELETE RESTRICT,
    CONSTRAINT chk_dept_type CHECK (type IN ('COMPANY', 'DIVISION', 'TEAM', 'GROUP', 'CUSTOM')),
    CONSTRAINT chk_custom_type CHECK (
        (type = 'CUSTOM' AND custom_type_name IS NOT NULL) OR
        (type != 'CUSTOM')
    )
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_dept_tenant ON org_departments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dept_parent ON org_departments(parent_dept_id);
CREATE INDEX IF NOT EXISTS idx_dept_org_path ON org_departments(org_path);
CREATE INDEX IF NOT EXISTS idx_dept_active ON org_departments(is_active);
```

**컬럼 설명**:

| 컬럼명 | 타입 | NULL | 설명 | 예시 |
|--------|------|------|------|------|
| dept_id | VARCHAR(50) | ✖ | 부서 ID (PK) | `dept-root-001` |
| tenant_id | VARCHAR(50) | ✖ | 테넌트 ID | `tenant-001` |
| name | VARCHAR(100) | ✖ | 부서명 | `넥스프론`, `고객서비스본부` |
| type | VARCHAR(20) | ✖ | 부서 타입 | `COMPANY`, `DIVISION`, `TEAM` |
| custom_type_name | VARCHAR(50) | ✓ | 사용자 정의 타입 | `센터`, `지사` (type=CUSTOM) |
| parent_dept_id | VARCHAR(50) | ✓ | 상위 부서 ID (FK) | NULL=루트, UUID=하위 |
| org_path | TEXT | ✖ | 조직 경로 | `/dept-root-001/dept-div-001/` |
| depth | INTEGER | ✖ | 트리 깊이 | 0(루트), 1, 2... |
| display_order | INTEGER | ✖ | 표시 순서 | 1, 2, 3... |
| manager_id | VARCHAR(50) | ✓ | 부서장 ID | Agent ID |
| description | TEXT | ✓ | 부서 설명 | `고객 서비스 총괄` |
| is_active | BOOLEAN | ✖ | 활성화 상태 | TRUE, FALSE |
| created_at | TIMESTAMP | ✖ | 생성 일시 | `2026-02-05 10:00:00` |
| updated_at | TIMESTAMP | ✖ | 수정 일시 | `2026-02-05 15:00:00` |
| created_by | VARCHAR(50) | ✓ | 생성자 ID | Agent ID |
| updated_by | VARCHAR(50) | ✓ | 수정자 ID | Agent ID |

**DepartmentType Enum**:
```java
public enum DepartmentType {
    COMPANY,    // 회사 (루트 전용)
    DIVISION,   // 본부/사업부
    TEAM,       // 팀
    GROUP,      // 그룹/파트
    CUSTOM      // 커스텀 (customTypeName 필수)
}
```

**샘플 데이터 (4개)**:
```sql
-- 루트
INSERT INTO org_departments (dept_id, tenant_id, name, type, parent_dept_id, org_path, depth, display_order, is_active)
VALUES
    ('dept-root-001', 'tenant-001', '넥스프론', 'COMPANY', NULL, '/dept-root-001/', 0, 1, TRUE),
    ('dept-div-001', 'tenant-001', '고객서비스본부', 'DIVISION', 'dept-root-001', '/dept-root-001/dept-div-001/', 1, 1, TRUE),
    ('dept-team-001', 'tenant-001', '인바운드팀', 'TEAM', 'dept-div-001', '/dept-root-001/dept-div-001/dept-team-001/', 2, 1, TRUE),
    ('dept-team-002', 'tenant-001', '아웃바운드팀', 'TEAM', 'dept-div-001', '/dept-root-001/dept-div-001/dept-team-002/', 2, 2, TRUE);
```

**조직도 예시**:
```
넥스프론 (dept-root-001, COMPANY)
└── 고객서비스본부 (dept-div-001, DIVISION)
    ├── 인바운드팀 (dept-team-001, TEAM)
    └── 아웃바운드팀 (dept-team-002, TEAM)
```

---

## 5. User 모듈

### 5.1 user_agents (사용자/상담사)

**목적**: 시스템 사용자 정보 관리

**DDL**:
```sql
CREATE TABLE IF NOT EXISTS user_agents (
    -- 식별자
    agent_id            VARCHAR(50)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    login_id            VARCHAR(50)     NOT NULL,
    
    -- 인증
    password            VARCHAR(255)    NOT NULL,          -- BCrypt 해시
    password_must_change BOOLEAN        NOT NULL DEFAULT FALSE,
    
    -- 기본 정보
    name                VARCHAR(100)    NOT NULL,
    employee_id         VARCHAR(50),                       -- 사원번호
    email               VARCHAR(100),
    phone               VARCHAR(20),
    
    -- 조직
    dept_id             VARCHAR(50),                       -- FK → org_departments
    
    -- 상태
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, SUSPENDED, RETIRED
    
    -- 상태 추적
    suspended_at        TIMESTAMP,
    retired_at          TIMESTAMP,
    scheduled_delete_at TIMESTAMP,                         -- 예약 삭제 일시 (RETIRED 후 90일)
    
    -- 감사
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50),
    suspended_by        VARCHAR(50),
    retired_by          VARCHAR(50),
    
    -- 낙관적 잠금
    version             BIGINT          NOT NULL DEFAULT 0,
    
    -- 제약조건
    CONSTRAINT uk_agent_tenant_login UNIQUE (tenant_id, login_id),
    CONSTRAINT chk_agent_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'RETIRED')),
    CONSTRAINT fk_agent_dept FOREIGN KEY (dept_id)
        REFERENCES org_departments(dept_id) ON DELETE SET NULL
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_agent_tenant ON user_agents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_login ON user_agents(login_id);
CREATE INDEX IF NOT EXISTS idx_agent_dept ON user_agents(dept_id);
CREATE INDEX IF NOT EXISTS idx_agent_status ON user_agents(status);
CREATE INDEX IF NOT EXISTS idx_agent_scheduled_delete ON user_agents(scheduled_delete_at)
    WHERE scheduled_delete_at IS NOT NULL;
```

**컬럼 설명**:

| 컬럼명 | 타입 | NULL | 설명 | 예시 |
|--------|------|------|------|------|
| agent_id | VARCHAR(50) | ✖ | 사용자 ID (PK) | `agent-admin-001` |
| tenant_id | VARCHAR(50) | ✖ | 테넌트 ID | `tenant-001` |
| login_id | VARCHAR(50) | ✖ | 로그인 ID (UK) | `admin`, `agent01` |
| password | VARCHAR(255) | ✖ | 비밀번호 (BCrypt) | `$2a$10$...` |
| password_must_change | BOOLEAN | ✖ | 비밀번호 변경 필요 | FALSE |
| name | VARCHAR(100) | ✖ | 사용자명 | `관리자`, `홍길동` |
| employee_id | VARCHAR(50) | ✓ | 사원번호 | `EMP001` |
| email | VARCHAR(100) | ✓ | 이메일 | `admin@nexfron.com` |
| phone | VARCHAR(20) | ✓ | 전화번호 | `010-1234-5678` |
| dept_id | VARCHAR(50) | ✓ | 소속 부서 ID | org_departments(dept_id) |
| status | VARCHAR(20) | ✖ | 상태 | ACTIVE, SUSPENDED, RETIRED |
| suspended_at | TIMESTAMP | ✓ | 정지 일시 | `2025-12-31 23:59:59` |
| retired_at | TIMESTAMP | ✓ | 퇴사 일시 | `2025-12-31 23:59:59` |
| scheduled_delete_at | TIMESTAMP | ✓ | 삭제 예정 일시 | 퇴사 후 90일 |
| created_at | TIMESTAMP | ✖ | 생성 일시 | `2026-02-05 10:00:00` |
| updated_at | TIMESTAMP | ✖ | 수정 일시 | `2026-02-05 15:00:00` |
| created_by | VARCHAR(50) | ✓ | 생성자 ID | Agent ID |
| updated_by | VARCHAR(50) | ✓ | 수정자 ID | Agent ID |
| suspended_by | VARCHAR(50) | ✓ | 정지 처리자 ID | Agent ID |
| retired_by | VARCHAR(50) | ✓ | 퇴사 처리자 ID | Agent ID |
| version | BIGINT | ✖ | 낙관적 잠금 버전 | 0 (기본값) |

**AgentStatus Enum**:
```java
public enum AgentStatus {
    ACTIVE,     // 활성 (정상 근무)
    SUSPENDED,  // 정지 (임시 차단)
    RETIRED     // 퇴사 (종료)
}
```

**비밀번호 표준**:
- **알고리즘**: BCrypt
- **강도**: 10 rounds
- **형식**: `$2a$10$` (60자)
- **테스트 비밀번호**: `password123`
- **해시**: `$2a$10$8K1p/a0dL3.W6ba/xH88su7pUdyJNgI3Jy0FsYqKOdw7tWpVKSzSy`

**샘플 데이터 (3개)**:
```sql
INSERT INTO user_agents (agent_id, tenant_id, login_id, password, name, employee_id, email, phone, dept_id, status, password_must_change)
VALUES
    ('agent-admin-001', 'tenant-001', 'admin', '$2a$10$8K1p/a0dL3.W6ba/xH88su7pUdyJNgI3Jy0FsYqKOdw7tWpVKSzSy', '관리자', 'EMP001', 'admin@nexfron.com', '010-1234-5678', 'dept-root-001', 'ACTIVE', FALSE),
    ('agent-lead-001', 'tenant-001', 'teamlead01', '$2a$10$8K1p/a0dL3.W6ba/xH88su7pUdyJNgI3Jy0FsYqKOdw7tWpVKSzSy', '김팀장', 'EMP002', 'teamlead@nexfron.com', '010-2345-6789', 'dept-div-001', 'ACTIVE', FALSE),
    ('agent-001', 'tenant-001', 'agent01', '$2a$10$8K1p/a0dL3.W6ba/xH88su7pUdyJNgI3Jy0FsYqKOdw7tWpVKSzSy', '홍길동', 'EMP003', 'agent01@nexfron.com', '010-3456-7890', 'dept-team-001', 'ACTIVE', FALSE);
```

---

## 6. RBAC 모듈

### 6.1 rbac_roles (역할)

**목적**: 역할 정의 및 관리

**DDL**:
```sql
CREATE TABLE IF NOT EXISTS rbac_roles (
    role_id             VARCHAR(50)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    name                VARCHAR(50)     NOT NULL,
    type                VARCHAR(20)     NOT NULL,          -- POSITION, CHANNEL
    data_scope          VARCHAR(20),                       -- ADMIN, TEAM_LEAD, MEMBER (POSITION일 때만)
    description         VARCHAR(255),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_role_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT chk_role_type CHECK (type IN ('POSITION', 'CHANNEL')),
    CONSTRAINT chk_role_data_scope CHECK (
        (type = 'POSITION' AND data_scope IN ('ADMIN', 'TEAM_LEAD', 'MEMBER')) OR
        (type = 'CHANNEL' AND data_scope IS NULL)
    )
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_role_tenant ON rbac_roles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_role_type ON rbac_roles(type);
CREATE INDEX IF NOT EXISTS idx_role_active ON rbac_roles(is_active);
```

**컬럼 설명**:

| 컬럼명 | 타입 | NULL | 설명 | 예시 |
|--------|------|------|------|------|
| role_id | VARCHAR(50) | ✖ | 역할 ID (PK) | `role-admin-001` |
| tenant_id | VARCHAR(50) | ✖ | 테넌트 ID | `tenant-001` |
| name | VARCHAR(50) | ✖ | 역할명 (UK) | `ADMIN`, `TEAM_LEAD`, `INBOUND_AGENT` |
| type | VARCHAR(20) | ✖ | 역할 타입 | `POSITION`, `CHANNEL` |
| data_scope | VARCHAR(20) | ✓ | 데이터 스코프 | `ADMIN`, `TEAM_LEAD`, `MEMBER` (POSITION만) |
| description | VARCHAR(255) | ✓ | 역할 설명 | `시스템 관리자 (전체 조직 접근)` |
| is_active | BOOLEAN | ✖ | 활성화 상태 | TRUE, FALSE |
| created_at | TIMESTAMP | ✖ | 생성 일시 | `2026-02-05 10:00:00` |
| updated_at | TIMESTAMP | ✖ | 수정 일시 | `2026-02-05 15:00:00` |

**RoleType Enum**:
```java
public enum RoleType {
    POSITION,   // 직급 (ADMIN, TEAM_LEAD, AGENT)
    CHANNEL     // 채널 (INBOUND_AGENT, CHAT_AGENT 등)
}
```

**DataScopeLevel Enum** (POSITION 전용):
```java
public enum DataScopeLevel {
    ADMIN,      // 전체 조직 접근
    TEAM_LEAD,  // 본인 부서 + 하위 부서
    MEMBER      // 본인 부서만
}
```

**초기 역할 (8개)**:
```sql
-- POSITION 역할 (3개)
INSERT INTO rbac_roles (role_id, tenant_id, name, type, data_scope, description, is_active)
VALUES
    ('role-admin-001', 'tenant-001', 'ADMIN', 'POSITION', 'ADMIN', '시스템 관리자 (전체 조직 접근)', TRUE),
    ('role-teamlead-001', 'tenant-001', 'TEAM_LEAD', 'POSITION', 'TEAM_LEAD', '팀장 (본인 팀 + 하위 부서 접근)', TRUE),
    ('role-agent-001', 'tenant-001', 'AGENT', 'POSITION', 'MEMBER', '일반 상담사 (본인 팀만 접근)', TRUE);

-- CHANNEL 역할 (5개)
INSERT INTO rbac_roles (role_id, tenant_id, name, type, data_scope, description, is_active)
VALUES
    ('role-ch-inbound', 'tenant-001', 'INBOUND_AGENT', 'CHANNEL', NULL, '인바운드 전화 상담', TRUE),
    ('role-ch-outbound', 'tenant-001', 'OUTBOUND_AGENT', 'CHANNEL', NULL, '아웃바운드 전화 상담', TRUE),
    ('role-ch-chat', 'tenant-001', 'CHAT_AGENT', 'CHANNEL', NULL, '채팅 상담', TRUE),
    ('role-ch-email', 'tenant-001', 'EMAIL_AGENT', 'CHANNEL', NULL, '이메일 상담', TRUE),
    ('role-ch-multi', 'tenant-001', 'MULTI_CHANNEL_AGENT', 'CHANNEL', NULL, '멀티채널 상담 (모든 채널)', TRUE);
```

---

### 6.2 rbac_permissions (권한)

**목적**: 권한 정의

**DDL**:
```sql
CREATE TABLE IF NOT EXISTS rbac_permissions (
    permission_id       VARCHAR(50)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    code                VARCHAR(100)    NOT NULL,          -- 예: agent:create, dept:read
    name                VARCHAR(100),
    description         VARCHAR(255),
    category            VARCHAR(50),                       -- AGENT, DEPARTMENT, RBAC, CHANNEL
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_permission_tenant_code UNIQUE (tenant_id, code)
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_permission_tenant ON rbac_permissions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_permission_category ON rbac_permissions(category);
```

**컬럼 설명**:

| 컬럼명 | 타입 | NULL | 설명 | 예시 |
|--------|------|------|------|------|
| permission_id | VARCHAR(50) | ✖ | 권한 ID (PK) | `perm-agent-001` |
| tenant_id | VARCHAR(50) | ✖ | 테넌트 ID | `tenant-001` |
| code | VARCHAR(100) | ✖ | 권한 코드 (UK) | `agent:create`, `dept:read` |
| name | VARCHAR(100) | ✓ | 권한 이름 | `상담사 생성`, `부서 조회` |
| description | VARCHAR(255) | ✓ | 권한 설명 | `새로운 상담사 계정 생성` |
| category | VARCHAR(50) | ✓ | 권한 카테고리 | `AGENT`, `DEPARTMENT`, `CHANNEL` |
| created_at | TIMESTAMP | ✖ | 생성 일시 | `2026-02-05 10:00:00` |

**Permission 명명 규칙**: `{domain}:{action}` 또는 `{domain}:{channel}:{action}`

**초기 권한 (31개)**:

#### AGENT 카테고리 (9개)
```sql
INSERT INTO rbac_permissions (permission_id, tenant_id, code, name, description, category)
VALUES
    ('perm-agent-001', 'tenant-001', 'agent:create', '상담사 생성', '새로운 상담사 계정 생성', 'AGENT'),
    ('perm-agent-002', 'tenant-001', 'agent:read', '상담사 조회', '상담사 정보 조회', 'AGENT'),
    ('perm-agent-003', 'tenant-001', 'agent:update', '상담사 수정', '상담사 정보 수정', 'AGENT'),
    ('perm-agent-004', 'tenant-001', 'agent:delete', '상담사 삭제', '상담사 계정 삭제', 'AGENT'),
    ('perm-agent-005', 'tenant-001', 'agent:suspend', '상담사 정지', '상담사 계정 정지', 'AGENT'),
    ('perm-agent-006', 'tenant-001', 'agent:activate', '상담사 활성화', '정지된 상담사 활성화', 'AGENT'),
    ('perm-agent-007', 'tenant-001', 'agent:transfer', '상담사 이동', '상담사 부서 이동', 'AGENT'),
    ('perm-agent-008', 'tenant-001', 'agent:role:assign', '역할 할당', '상담사에게 역할 할당', 'AGENT'),
    ('perm-agent-009', 'tenant-001', 'agent:password:reset', '비밀번호 초기화', '상담사 비밀번호 초기화', 'AGENT');
```

#### DEPARTMENT 카테고리 (6개)
```sql
INSERT INTO rbac_permissions (permission_id, tenant_id, code, name, description, category)
VALUES
    ('perm-dept-001', 'tenant-001', 'dept:create', '부서 생성', '새로운 부서 생성', 'DEPARTMENT'),
    ('perm-dept-002', 'tenant-001', 'dept:read', '부서 조회', '부서 정보 조회', 'DEPARTMENT'),
    ('perm-dept-003', 'tenant-001', 'dept:update', '부서 수정', '부서 정보 수정', 'DEPARTMENT'),
    ('perm-dept-004', 'tenant-001', 'dept:delete', '부서 삭제', '부서 삭제', 'DEPARTMENT'),
    ('perm-dept-005', 'tenant-001', 'dept:move', '부서 이동', '부서 위치 이동', 'DEPARTMENT'),
    ('perm-dept-006', 'tenant-001', 'dept:deactivate', '부서 비활성화', '부서 비활성화', 'DEPARTMENT');
```

#### RBAC 카테고리 (6개)
```sql
INSERT INTO rbac_permissions (permission_id, tenant_id, code, name, description, category)
VALUES
    ('perm-rbac-001', 'tenant-001', 'role:create', '역할 생성', '새로운 역할 생성', 'RBAC'),
    ('perm-rbac-002', 'tenant-001', 'role:read', '역할 조회', '역할 정보 조회', 'RBAC'),
    ('perm-rbac-003', 'tenant-001', 'role:update', '역할 수정', '역할 정보 수정', 'RBAC'),
    ('perm-rbac-004', 'tenant-001', 'role:delete', '역할 삭제', '역할 삭제', 'RBAC'),
    ('perm-rbac-005', 'tenant-001', 'permission:read', '권한 조회', '권한 목록 조회', 'RBAC'),
    ('perm-rbac-006', 'tenant-001', 'permission:assign', '권한 할당', '역할에 권한 할당', 'RBAC');
```

#### CHANNEL 카테고리 (10개)
```sql
INSERT INTO rbac_permissions (permission_id, tenant_id, code, name, description, category)
VALUES
    -- 인바운드 (3개)
    ('perm-ch-in-001', 'tenant-001', 'channel:inbound:receive', '인바운드 수신', '인바운드 전화 수신', 'CHANNEL'),
    ('perm-ch-in-002', 'tenant-001', 'channel:inbound:hold', '통화 대기', '통화 대기 처리', 'CHANNEL'),
    ('perm-ch-in-003', 'tenant-001', 'channel:inbound:transfer', '호 전환', '다른 상담사에게 호 전환', 'CHANNEL'),
    
    -- 아웃바운드 (2개)
    ('perm-ch-out-001', 'tenant-001', 'channel:outbound:call', '아웃바운드 발신', '아웃바운드 전화 발신', 'CHANNEL'),
    ('perm-ch-out-002', 'tenant-001', 'channel:outbound:campaign', '캠페인 관리', '캠페인 관리', 'CHANNEL'),
    
    -- 채팅 (3개)
    ('perm-ch-chat-001', 'tenant-001', 'channel:chat:message', '채팅 메시지', '채팅 메시지 송수신', 'CHANNEL'),
    ('perm-ch-chat-002', 'tenant-001', 'channel:chat:file', '파일 전송', '채팅 파일 전송', 'CHANNEL'),
    ('perm-ch-chat-003', 'tenant-001', 'channel:chat:emoji', '이모티콘', '이모티콘 사용', 'CHANNEL'),
    
    -- 이메일 (2개)
    ('perm-ch-email-001', 'tenant-001', 'channel:email:send', '이메일 발송', '이메일 발송', 'CHANNEL'),
    ('perm-ch-email-002', 'tenant-001', 'channel:email:receive', '이메일 수신', '이메일 수신', 'CHANNEL');
```

---

### 6.3 rbac_role_permissions (역할-권한 매핑)

**목적**: 역할과 권한의 M:N 관계

**DDL**:
```sql
CREATE TABLE IF NOT EXISTS rbac_role_permissions (
    role_id             VARCHAR(50)     NOT NULL,
    permission_id       VARCHAR(50)     NOT NULL,
    assigned_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by         VARCHAR(50),
    
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id)
        REFERENCES rbac_roles(role_id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id)
        REFERENCES rbac_permissions(permission_id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_rp_role ON rbac_role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_rp_permission ON rbac_role_permissions(permission_id);
```

**초기 매핑 (약 60개)**:

| 역할 | 할당 권한 | 개수 |
|------|----------|------|
| ADMIN | 모든 권한 (SELECT 자동) | 31개 |
| TEAM_LEAD | agent:read, agent:update, agent:transfer, dept:read, role:read, permission:read | 6개 |
| AGENT | agent:read, dept:read, role:read | 3개 |
| INBOUND_AGENT | channel:inbound:* | 3개 |
| OUTBOUND_AGENT | channel:outbound:* | 2개 |
| CHAT_AGENT | channel:chat:* | 3개 |
| EMAIL_AGENT | channel:email:* | 2개 |
| MULTI_CHANNEL_AGENT | 모든 CHANNEL 권한 (SELECT 자동) | 10개 |

---

### 6.4 user_agent_roles (사용자-역할 매핑)

**목적**: 사용자와 역할의 M:N 관계

**DDL**:
```sql
CREATE TABLE IF NOT EXISTS user_agent_roles (
    agent_id            VARCHAR(50)     NOT NULL,
    role_id             VARCHAR(50)     NOT NULL,
    assigned_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by         VARCHAR(50),
    
    PRIMARY KEY (agent_id, role_id),
    CONSTRAINT fk_ar_agent FOREIGN KEY (agent_id)
        REFERENCES user_agents(agent_id) ON DELETE CASCADE,
    CONSTRAINT fk_ar_role FOREIGN KEY (role_id)
        REFERENCES rbac_roles(role_id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_ar_agent ON user_agent_roles(agent_id);
CREATE INDEX IF NOT EXISTS idx_ar_role ON user_agent_roles(role_id);
```

**초기 매핑 (6개)**:
```sql
INSERT INTO user_agent_roles (agent_id, role_id)
VALUES
    -- 관리자: ADMIN만
    ('agent-admin-001', 'role-admin-001'),
    
    -- 팀장: TEAM_LEAD + INBOUND_AGENT
    ('agent-lead-001', 'role-teamlead-001'),
    ('agent-lead-001', 'role-ch-inbound'),
    
    -- 일반 상담사: AGENT + INBOUND_AGENT + CHAT_AGENT (멀티채널)
    ('agent-001', 'role-agent-001'),
    ('agent-001', 'role-ch-inbound'),
    ('agent-001', 'role-ch-chat');
```

---

## 7. 2차원 역할 체계 (RBAC_SCENARIOS 기준)

### 7.1 역할 할당 규칙

```
[ POSITION 역할 ] (필수, 1개만)
    ↓
ADMIN, TEAM_LEAD, AGENT 중 1개 선택
    ↓
DataScope 자동 결정 (ADMIN, TEAM_LEAD, MEMBER)

[ CHANNEL 역할 ] (선택, 여러 개 가능)
    ↓
INBOUND_AGENT, OUTBOUND_AGENT, CHAT_AGENT, EMAIL_AGENT, MULTI_CHANNEL_AGENT
    ↓
복수 선택 가능 (예: INBOUND + CHAT)
```

### 7.2 역할 조합 예시

| 사용자 타입 | POSITION | CHANNEL | DataScope |
|------------|----------|---------|-----------|
| 시스템 관리자 | ADMIN | - | ADMIN (전체) |
| 팀장 (인바운드 겸임) | TEAM_LEAD | INBOUND_AGENT | TEAM_LEAD (팀+하위) |
| 멀티채널 상담사 | AGENT | INBOUND_AGENT, CHAT_AGENT | MEMBER (본인 팀) |
| 아웃바운드 전문가 | AGENT | OUTBOUND_AGENT | MEMBER (본인 팀) |

---

## 8. 제약조건 및 비즈니스 규칙

### 8.1 Unique 제약

| 테이블 | 컬럼 | 설명 |
|--------|------|------|
| user_agents | (tenant_id, login_id) | 테넌트 내 로그인ID 유일 |
| rbac_roles | (tenant_id, name) | 테넌트 내 역할명 유일 |
| rbac_permissions | (tenant_id, code) | 테넌트 내 권한코드 유일 |

### 8.2 Check 제약

| 테이블 | 컬럼 | 유효값 |
|--------|------|--------|
| user_agents | status | ACTIVE, SUSPENDED, RETIRED |
| org_departments | type | COMPANY, DIVISION, TEAM, GROUP, CUSTOM |
| rbac_roles | type | POSITION, CHANNEL |
| rbac_roles | data_scope | ADMIN, TEAM_LEAD, MEMBER (POSITION일 때), NULL (CHANNEL일 때) |

### 8.3 Foreign Key ON DELETE 정책

| 테이블 | 컬럼 | 참조 | ON DELETE | 이유 |
|--------|------|------|-----------|------|
| user_agents | dept_id | org_departments | SET NULL | 부서 삭제 시 사용자는 유지 (부서 미배정 상태) |
| org_departments | parent_dept_id | org_departments | RESTRICT | 하위 부서가 있으면 삭제 불가 |
| user_agent_roles | agent_id | user_agents | CASCADE | 사용자 삭제 시 역할 매핑도 삭제 |
| user_agent_roles | role_id | rbac_roles | CASCADE | 역할 삭제 시 매핑도 삭제 |
| rbac_role_permissions | role_id | rbac_roles | CASCADE | 역할 삭제 시 매핑도 삭제 |
| rbac_role_permissions | permission_id | rbac_permissions | CASCADE | 권한 삭제 시 매핑도 삭제 |

---

## 9. 마이그레이션 순서

**테이블 생성 순서** (FK 의존성 고려):
```
1. rbac_permissions      (독립 테이블)
2. rbac_roles            (독립 테이블)
3. org_departments       (self-referential, 부모 먼저 생성)
4. user_agents           (org_departments 참조)
5. rbac_role_permissions (rbac_roles, rbac_permissions 참조)
6. user_agent_roles      (user_agents, rbac_roles 참조)
```

**초기 데이터 삽입 순서**:
```
1. rbac_roles            (8개 역할)
2. rbac_permissions      (31개 권한)
3. rbac_role_permissions (역할-권한 매핑)
4. org_departments       (4개 부서, 계층 순서대로)
5. user_agents           (3개 사용자)
6. user_agent_roles      (6개 사용자-역할 매핑)
```

---

## 10. 성능 최적화

### 10.1 인덱스 전략

**복합 유니크 인덱스**:
- `(tenant_id, login_id)` - 로그인 시 빠른 조회
- `(tenant_id, name)` - 역할명으로 조회
- `(tenant_id, code)` - 권한 코드로 조회

**FK 인덱스**:
- `parent_dept_id` - 하위 부서 조회
- `dept_id` (in user_agents) - 부서별 사용자 조회
- `agent_id`, `role_id` (in mapping tables) - 조인 최적화

**특수 인덱스**:
- `org_path` - Materialized Path LIKE 검색 최적화
- `scheduled_delete_at` (Partial) - NULL 제외하여 스토리지 절약

### 10.2 Materialized Path 조회

**하위 부서 조회**:
```sql
-- 특정 부서와 모든 하위 부서
SELECT * FROM org_departments
WHERE tenant_id = 'tenant-001'
  AND org_path LIKE '/dept-root-001/%'
ORDER BY org_path;
```

**상위 부서 조회**:
```sql
-- org_path를 분해하여 상위 경로 추출
SELECT * FROM org_departments
WHERE tenant_id = 'tenant-001'
  AND position(dept_id in '/dept-root-001/dept-div-001/dept-team-001/') > 0
ORDER BY depth;
```

---

## 11. 멀티테넌시 격리

### 11.1 테넌트별 데이터 격리

**모든 쿼리에 tenant_id 조건 필수**:
```sql
-- ✅ 올바른 쿼리
SELECT * FROM user_agents
WHERE tenant_id = :tenantId AND login_id = :loginId;

-- ❌ 잘못된 쿼리 (tenant_id 누락)
SELECT * FROM user_agents WHERE login_id = :loginId;
```

### 11.2 복합 유니크 키

**tenant_id 포함**으로 테넌트 간 중복 허용:
```
tenant-001의 'ADMIN' 역할
tenant-002의 'ADMIN' 역할
→ 서로 다른 데이터, 중복 가능
```

---

## 12. 데이터 표준 (시나리오 기준)

### 12.1 Department (DEPARTMENT_SCENARIOS 기준)

**타입별 사용 예시**:
```
COMPANY   → 넥스프론, ABC 주식회사
DIVISION  → 고객서비스본부, 영업본부, 기술본부
TEAM      → 인바운드팀, 개발팀, 기획팀
GROUP     → 백엔드파트, 기획그룹
CUSTOM    → 센터(customTypeName='센터'), 지사(customTypeName='지사')
```

**org_path 형식**:
- 시작: `/`
- 구분자: `/`
- 종료: `/`
- 예시: `/dept-root-001/dept-div-001/dept-team-001/`

### 12.2 Agent (AGENT_SCENARIOS 기준)

**login_id 규칙**:
- 소문자 영문 + 숫자
- 4-50자
- 예: `admin`, `teamlead01`, `agent01`, `hong123`

**employee_id 규칙** (선택):
- 형식 자유
- 예: `EMP001`, `2024001`

**비밀번호 정책**:
- 최소 8자
- 영문, 숫자, 특수문자 조합 권장
- BCrypt로 해시하여 저장

### 12.3 Role & Permission (RBAC_SCENARIOS 기준)

**역할명 규칙**:
- 대문자 영문 + 언더스코어
- 2-50자
- 예: `ADMIN`, `TEAM_LEAD`, `INBOUND_AGENT`

**권한 코드 규칙**:
- `{domain}:{action}` 또는 `{domain}:{channel}:{action}`
- 소문자
- 예: `agent:create`, `channel:inbound:receive`

**카테고리**:
- AGENT: 상담사 관리
- DEPARTMENT: 부서 관리
- RBAC: 역할/권한 관리
- CHANNEL: 채널 업무

---

## 13. 완전한 초기 데이터 세트

### 13.1 테이블별 데이터 개수

| 테이블 | 초기 건수 | 설명 |
|--------|----------|------|
| org_departments | 4개 | 3단계 트리 (1-1-2) |
| user_agents | 3개 | admin, teamlead01, agent01 |
| rbac_roles | 8개 | POSITION 3개 + CHANNEL 5개 |
| rbac_permissions | 31개 | AGENT:9, DEPT:6, RBAC:6, CHANNEL:10 |
| rbac_role_permissions | 약 60개 | 역할별 권한 매핑 |
| user_agent_roles | 6개 | 사용자별 역할 할당 |

### 13.2 사용자 상세

#### admin (관리자)
```
- agent_id: agent-admin-001
- login_id: admin
- password: password123
- name: 관리자
- dept: 넥스프론 (dept-root-001)
- roles: [ADMIN]
- permissions: 31개 (모든 권한)
```

#### teamlead01 (팀장)
```
- agent_id: agent-lead-001
- login_id: teamlead01
- password: password123
- name: 김팀장
- dept: 고객서비스본부 (dept-div-001)
- roles: [TEAM_LEAD, INBOUND_AGENT]
- permissions: 6개 (관리) + 3개 (인바운드) = 9개
```

#### agent01 (일반 상담사)
```
- agent_id: agent-001
- login_id: agent01
- password: password123
- name: 홍길동
- dept: 인바운드팀 (dept-team-001)
- roles: [AGENT, INBOUND_AGENT, CHAT_AGENT]
- permissions: 3개 (기본) + 3개 (인바운드) + 3개 (채팅) = 9개
```

---

## 14. 참고 문서

### 시나리오 문서 (표준 데이터 기준)
- **[RBAC_SCENARIOS.md](./RBAC_SCENARIOS.md)**: 역할/권한 시나리오, 2차원 역할 체계
- **[DEPARTMENT_SCENARIOS.md](./DEPARTMENT_SCENARIOS.md)**: 조직 관리 시나리오, 트리 구조 규칙
- **[AGENT_SCENARIOS.md](./AGENT_SCENARIOS.md)**: 상담사 관리 시나리오, 라이프사이클

### 구현 문서
- **[V1_0_0__Complete_Init.sql](../src/main/resources/db/migration/V1_0_0__Complete_Init.sql)**: 실제 마이그레이션 스크립트
- **[DB_COMPREHENSIVE_GUIDE.md](../DB_COMPREHENSIVE_GUIDE.md)**: 상세 DB 가이드
- **[DATABASE_SCHEMA_COMPARISON.md](../DATABASE_SCHEMA_COMPARISON.md)**: 구버전과의 차이점 분석

---

**문서 버전**: 3.0  
**최종 수정**: 2026-02-05  
**기준**: V1_0_0__Complete_Init.sql (실제 구현)  
**엔티티 일치**: ✅ AgentJpaEntity, DepartmentEntity, RoleJpaEntity  
**시나리오 일치**: ✅ RBAC_SCENARIOS, DEPARTMENT_SCENARIOS, AGENT_SCENARIOS  
**상태**: ✅ 실제 구현과 100% 일치
