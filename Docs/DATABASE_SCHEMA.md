# Database Schema - 데이터베이스 스키마 설계

Identity Modulith의 데이터베이스 스키마 정의

---

## 1. 개요

### 1.1 데이터베이스
- **DBMS**: PostgreSQL 15+
- **스키마**: public (단일 스키마, 모듈별 테이블 접두어)

### 1.2 테이블 명명 규칙
```
{module}_{entity}

예시:
- user_agents          (User 모듈 - Agent 테이블)
- org_departments      (Organization 모듈 - Department 테이블)
- rbac_roles           (RBAC 모듈 - Role 테이블)
```

### 1.3 공통 컬럼
```sql
-- 모든 테이블에 포함
tenant_id       VARCHAR(50)  NOT NULL    -- Multi-tenancy
created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
```

---

## 2. ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Identity Modulith ERD                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐         ┌─────────────────┐                           │
│  │  user_agents    │         │ org_departments │                           │
│  ├─────────────────┤         ├─────────────────┤                           │
│  │ PK id           │    ┌───▶│ PK dept_id      │◀──┐                       │
│  │    tenant_id    │    │    │    tenant_id    │   │                       │
│  │    login_id     │    │    │    code         │   │ self-ref              │
│  │    password     │    │    │    name         │   │ (parent)              │
│  │    name         │    │    │    type         │   │                       │
│  │    employee_id  │    │    │    custom_type  │   │                       │
│  │ FK department_id│────┘    │ FK parent_id    │───┘                       │
│  │    email        │         │    status       │                           │
│  │    phone        │         │    deactivated_at│                          │
│  │    status       │         │    created_at   │                           │
│  │    pwd_must_chg │         │    updated_at   │                           │
│  │    suspended_at │         └─────────────────┘                           │
│  │    retired_at   │                                                       │
│  │    scheduled_del│                                                       │
│  │    created_at   │                                                       │
│  │    updated_at   │                                                       │
│  └────────┬────────┘                                                       │
│           │                                                                 │
│           │ M:N                                                             │
│           ▼                                                                 │
│  ┌─────────────────┐         ┌─────────────────┐                           │
│  │ user_agent_roles│         │   rbac_roles    │                           │
│  ├─────────────────┤         ├─────────────────┤                           │
│  │ FK agent_id     │────────▶│ PK id           │◀──┐                       │
│  │ FK role_id      │────────▶│    name (UK)    │   │                       │
│  │    assigned_at  │         │    type         │   │                       │
│  │    assigned_by  │         │    data_scope   │   │                       │
│  └─────────────────┘         │    description  │   │                       │
│                              │    created_at   │   │                       │
│                              │    updated_at   │   │                       │
│                              └────────┬────────┘   │                       │
│                                       │            │                       │
│                                       │ M:N        │                       │
│                                       ▼            │                       │
│  ┌─────────────────┐         ┌─────────────────┐   │                       │
│  │rbac_permissions │         │rbac_role_perms  │   │                       │
│  ├─────────────────┤         ├─────────────────┤   │                       │
│  │ PK id           │◀────────│ FK role_id      │───┘                       │
│  │    code (UK)    │◀────────│ FK permission_id│                           │
│  │    name         │         │    assigned_at  │                           │
│  │    description  │         │    assigned_by  │                           │
│  │    category     │         └─────────────────┘                           │
│  │    created_at   │                                                       │
│  └─────────────────┘                                                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. User 모듈 테이블

### 3.1 user_agents (상담사)

```sql
CREATE TABLE user_agents (
    -- 식별
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(50)     NOT NULL,
    login_id            VARCHAR(50)     NOT NULL,
    employee_id         VARCHAR(30),                    -- 사번 (선택)

    -- 인증
    password            VARCHAR(255)    NOT NULL,       -- BCrypt 암호화
    password_must_change BOOLEAN        NOT NULL DEFAULT TRUE,

    -- 기본 정보
    name                VARCHAR(100)    NOT NULL,
    email               VARCHAR(255),
    phone               VARCHAR(20),

    -- 조직
    department_id       BIGINT          NOT NULL,       -- FK → org_departments

    -- 상태
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
                                                        -- ACTIVE, SUSPENDED, RETIRED
    suspended_at        TIMESTAMP,
    retired_at          TIMESTAMP,
    scheduled_delete_at TIMESTAMP,                      -- 예약 삭제 일시

    -- 감사
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,

    -- 낙관적 잠금
    version             INTEGER         NOT NULL DEFAULT 0,

    -- 제약조건
    CONSTRAINT uk_user_agents_tenant_login UNIQUE (tenant_id, login_id),
    CONSTRAINT chk_user_agents_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'RETIRED'))
);

-- 인덱스
CREATE INDEX idx_user_agents_tenant ON user_agents (tenant_id);
CREATE INDEX idx_user_agents_department ON user_agents (tenant_id, department_id);
CREATE INDEX idx_user_agents_status ON user_agents (tenant_id, status);
CREATE INDEX idx_user_agents_scheduled_delete ON user_agents (scheduled_delete_at)
    WHERE scheduled_delete_at IS NOT NULL;

-- 코멘트
COMMENT ON TABLE user_agents IS '상담사 정보';
COMMENT ON COLUMN user_agents.status IS 'ACTIVE: 활성, SUSPENDED: 정지, RETIRED: 퇴사';
COMMENT ON COLUMN user_agents.scheduled_delete_at IS '예약 삭제 일시 (RETIRED 후 데이터 보존 기간 종료 시점)';
```

### 3.2 user_agent_roles (상담사-역할 매핑)

```sql
CREATE TABLE user_agent_roles (
    agent_id            UUID            NOT NULL,       -- FK → user_agents
    role_id             BIGINT          NOT NULL,       -- FK → rbac_roles
    assigned_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by         UUID,

    PRIMARY KEY (agent_id, role_id),
    CONSTRAINT fk_agent_roles_agent FOREIGN KEY (agent_id)
        REFERENCES user_agents (id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_roles_role FOREIGN KEY (role_id)
        REFERENCES rbac_roles (id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX idx_agent_roles_role ON user_agent_roles (role_id);

-- 코멘트
COMMENT ON TABLE user_agent_roles IS '상담사-역할 매핑 (M:N)';
```

---

## 4. Organization 모듈 테이블

### 4.1 org_departments (부서)

```sql
CREATE TABLE org_departments (
    -- 식별
    dept_id             BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    code                VARCHAR(30)     NOT NULL,       -- 부서 코드

    -- 기본 정보
    name                VARCHAR(100)    NOT NULL,
    type                VARCHAR(20)     NOT NULL,       -- COMPANY, DIVISION, TEAM, GROUP, CUSTOM
    custom_type_name    VARCHAR(50),                    -- type=CUSTOM일 때 사용

    -- 계층
    parent_id           BIGINT,                         -- FK → self, NULL이면 루트

    -- 상태
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
                                                        -- ACTIVE, INACTIVE
    deactivated_at      TIMESTAMP,

    -- 감사
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          UUID,
    updated_by          UUID,

    -- 낙관적 잠금
    version             INTEGER         NOT NULL DEFAULT 0,

    -- 제약조건
    CONSTRAINT uk_org_depts_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_org_depts_parent FOREIGN KEY (parent_id)
        REFERENCES org_departments (dept_id) ON DELETE RESTRICT,
    CONSTRAINT chk_org_depts_type CHECK (type IN ('COMPANY', 'DIVISION', 'TEAM', 'GROUP', 'CUSTOM')),
    CONSTRAINT chk_org_depts_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_org_depts_custom_type CHECK (
        (type = 'CUSTOM' AND custom_type_name IS NOT NULL) OR
        (type != 'CUSTOM' AND custom_type_name IS NULL)
    )
);

-- 인덱스
CREATE INDEX idx_org_depts_tenant ON org_departments (tenant_id);
CREATE INDEX idx_org_depts_parent ON org_departments (tenant_id, parent_id);
CREATE INDEX idx_org_depts_status ON org_departments (tenant_id, status);

-- 코멘트
COMMENT ON TABLE org_departments IS '부서 정보 (트리 구조)';
COMMENT ON COLUMN org_departments.type IS 'COMPANY: 회사, DIVISION: 본부, TEAM: 팀, GROUP: 그룹, CUSTOM: 커스텀';
COMMENT ON COLUMN org_departments.parent_id IS '부모 부서 ID (NULL이면 루트)';
```

### 4.2 하위 부서 조회 함수 (재귀)

```sql
-- 하위 부서 조회 (자신 포함)
CREATE OR REPLACE FUNCTION get_department_subtree(p_tenant_id VARCHAR, p_dept_id BIGINT)
RETURNS TABLE (
    dept_id BIGINT,
    name VARCHAR,
    parent_id BIGINT,
    depth INTEGER
) AS $$
BEGIN
    RETURN QUERY
    WITH RECURSIVE subtree AS (
        -- 기준 부서
        SELECT d.dept_id, d.name, d.parent_id, 0 AS depth
        FROM org_departments d
        WHERE d.dept_id = p_dept_id AND d.tenant_id = p_tenant_id

        UNION ALL

        -- 하위 부서
        SELECT d.dept_id, d.name, d.parent_id, s.depth + 1
        FROM org_departments d
        JOIN subtree s ON d.parent_id = s.dept_id
        WHERE d.tenant_id = p_tenant_id
    )
    SELECT * FROM subtree;
END;
$$ LANGUAGE plpgsql;

-- 조상 부서 ID 조회 (자신 제외)
CREATE OR REPLACE FUNCTION get_department_ancestors(p_tenant_id VARCHAR, p_dept_id BIGINT)
RETURNS TABLE (ancestor_id BIGINT) AS $$
BEGIN
    RETURN QUERY
    WITH RECURSIVE ancestors AS (
        -- 시작: 부모 부서
        SELECT d.parent_id
        FROM org_departments d
        WHERE d.dept_id = p_dept_id AND d.tenant_id = p_tenant_id

        UNION ALL

        -- 재귀: 조상 부서
        SELECT d.parent_id
        FROM org_departments d
        JOIN ancestors a ON d.dept_id = a.parent_id
        WHERE d.parent_id IS NOT NULL AND d.tenant_id = p_tenant_id
    )
    SELECT parent_id FROM ancestors WHERE parent_id IS NOT NULL;
END;
$$ LANGUAGE plpgsql;
```

---

## 5. RBAC 모듈 테이블

### 5.1 rbac_roles (역할)

```sql
CREATE TABLE rbac_roles (
    id                  BIGSERIAL       PRIMARY KEY,
    name                VARCHAR(50)     NOT NULL UNIQUE,
    type                VARCHAR(20)     NOT NULL,       -- POSITION, CHANNEL
    data_scope          VARCHAR(20),                    -- ADMIN, TEAM_LEAD, MEMBER (POSITION일 때만)
    description         VARCHAR(255),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 제약조건
    CONSTRAINT chk_rbac_roles_type CHECK (type IN ('POSITION', 'CHANNEL')),
    CONSTRAINT chk_rbac_roles_data_scope CHECK (
        (type = 'POSITION' AND data_scope IN ('ADMIN', 'TEAM_LEAD', 'MEMBER')) OR
        (type = 'CHANNEL' AND data_scope IS NULL)
    )
);

-- 코멘트
COMMENT ON TABLE rbac_roles IS '역할 정의';
COMMENT ON COLUMN rbac_roles.type IS 'POSITION: 직급, CHANNEL: 채널';
COMMENT ON COLUMN rbac_roles.data_scope IS 'DataScope 레벨 (POSITION 타입일 때만)';
```

### 5.2 rbac_permissions (권한)

```sql
CREATE TABLE rbac_permissions (
    id                  BIGSERIAL       PRIMARY KEY,
    code                VARCHAR(50)     NOT NULL UNIQUE,    -- 예: agent:create
    name                VARCHAR(100)    NOT NULL,           -- 표시명
    description         VARCHAR(255),
    category            VARCHAR(30)     NOT NULL,           -- AGENT, DEPARTMENT, RBAC, VOICE, CHAT 등
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_rbac_permissions_category ON rbac_permissions (category);

-- 코멘트
COMMENT ON TABLE rbac_permissions IS '권한 정의';
COMMENT ON COLUMN rbac_permissions.code IS '권한 코드 (resource:action 형식)';
```

### 5.3 rbac_role_permissions (역할-권한 매핑)

```sql
CREATE TABLE rbac_role_permissions (
    role_id             BIGINT          NOT NULL,       -- FK → rbac_roles
    permission_id       BIGINT          NOT NULL,       -- FK → rbac_permissions
    assigned_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by         UUID,

    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_perms_role FOREIGN KEY (role_id)
        REFERENCES rbac_roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_perms_perm FOREIGN KEY (permission_id)
        REFERENCES rbac_permissions (id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX idx_role_perms_permission ON rbac_role_permissions (permission_id);

-- 코멘트
COMMENT ON TABLE rbac_role_permissions IS '역할-권한 매핑 (M:N)';
```

---

## 6. Audit 모듈 테이블 (감사 로그)

### 6.1 audit_logs (감사 로그)

```sql
CREATE TABLE audit_logs (
    -- 식별
    audit_id            VARCHAR(36)     PRIMARY KEY,    -- UUID
    tenant_id           VARCHAR(50)     NOT NULL,

    -- 작업 정보
    action              VARCHAR(32)     NOT NULL,       -- CREATE, UPDATE, DELETE, ASSIGN, REVOKE
    resource_type       VARCHAR(64)     NOT NULL,       -- ROLE, PERMISSION, AGENT_ROLE, DEPARTMENT 등
    resource_id         VARCHAR(255)    NOT NULL,

    -- 작업자
    operator_id         VARCHAR(255)    NOT NULL,       -- 작업 수행자 ID

    -- 변경 내용
    changes             TEXT,                           -- JSON 형식 (변경 전후 값)

    -- 메타데이터
    timestamp           TIMESTAMP       NOT NULL,       -- 작업 발생 일시
    remarks             TEXT,                           -- 추가 정보 (메모, 실패 원인 등)
    ip_address          VARCHAR(45),                    -- 클라이언트 IP

    -- 제약조건
    CONSTRAINT chk_audit_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'ASSIGN', 'REVOKE', 'ACTIVATE', 'DEACTIVATE'))
);

-- 인덱스
CREATE INDEX idx_audit_logs_tenant_id ON audit_logs (tenant_id);
CREATE INDEX idx_audit_logs_resource_type ON audit_logs (resource_type);
CREATE INDEX idx_audit_logs_operator_id ON audit_logs (operator_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs (timestamp DESC);
CREATE INDEX idx_audit_logs_resource ON audit_logs (resource_type, resource_id);

-- 코멘트
COMMENT ON TABLE audit_logs IS '감사 로그 - 권한 관련 모든 변경사항 추적';
COMMENT ON COLUMN audit_logs.action IS '작업 유형 (CREATE: 생성, UPDATE: 수정, DELETE: 삭제, ASSIGN: 할당, REVOKE: 회수)';
COMMENT ON COLUMN audit_logs.resource_type IS '대상 리소스 타입 (ROLE, PERMISSION, AGENT_ROLE, DEPARTMENT 등)';
COMMENT ON COLUMN audit_logs.changes IS '변경 내용 (JSON 형식) - 예: {"before": {...}, "after": {...}}';
COMMENT ON COLUMN audit_logs.ip_address IS 'IPv4/IPv6 주소 (최대 45자)';
```

### 6.2 audit_logs_archive (감사 로그 아카이브)

```sql
CREATE TABLE audit_logs_archive (
    -- audit_logs와 동일한 구조
    audit_id            VARCHAR(36)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    action              VARCHAR(32)     NOT NULL,
    resource_type       VARCHAR(64)     NOT NULL,
    resource_id         VARCHAR(255)    NOT NULL,
    operator_id         VARCHAR(255)    NOT NULL,
    changes             TEXT,
    timestamp           TIMESTAMP       NOT NULL,
    remarks             TEXT,
    ip_address          VARCHAR(45),

    -- 아카이브 정보
    archived_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 제약조건
    CONSTRAINT chk_audit_archive_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'ASSIGN', 'REVOKE', 'ACTIVATE', 'DEACTIVATE'))
);

-- 인덱스
CREATE INDEX idx_audit_logs_archive_tenant ON audit_logs_archive (tenant_id);
CREATE INDEX idx_audit_logs_archive_timestamp ON audit_logs_archive (timestamp DESC);
CREATE INDEX idx_audit_logs_archive_resource ON audit_logs_archive (resource_type, resource_id);
CREATE INDEX idx_audit_logs_archive_operator ON audit_logs_archive (operator_id);

-- 코멘트
COMMENT ON TABLE audit_logs_archive IS '감사 로그 아카이브 - 6개월 이상 경과한 로그';
COMMENT ON COLUMN audit_logs_archive.archived_at IS '아카이브 일시';
```

### 6.3 아카이빙 배치 작업

```sql
-- 6개월 이상 오래된 감사 로그를 아카이브로 이동
-- 스프링 배치에서 실행 (매월 1일 오전 3시)
INSERT INTO audit_logs_archive (
    audit_id, tenant_id, action, resource_type, resource_id,
    operator_id, changes, timestamp, remarks, ip_address, archived_at
)
SELECT
    audit_id, tenant_id, action, resource_type, resource_id,
    operator_id, changes, timestamp, remarks, ip_address, CURRENT_TIMESTAMP
FROM audit_logs
WHERE timestamp < CURRENT_TIMESTAMP - INTERVAL '6 months';

-- 이동 완료 후 삭제
DELETE FROM audit_logs
WHERE timestamp < CURRENT_TIMESTAMP - INTERVAL '6 months';
```

### 6.4 감사 로그 조회 예시

```sql
-- 특정 역할의 변경 이력 조회
SELECT
    audit_id,
    action,
    operator_id,
    changes,
    timestamp,
    remarks
FROM audit_logs
WHERE tenant_id = 'tenant-001'
  AND resource_type = 'ROLE'
  AND resource_id = 'role-uuid-here'
ORDER BY timestamp DESC;

-- 특정 사용자의 모든 작업 이력
SELECT
    audit_id,
    action,
    resource_type,
    resource_id,
    timestamp
FROM audit_logs
WHERE tenant_id = 'tenant-001'
  AND operator_id = 'operator-uuid'
ORDER BY timestamp DESC
LIMIT 100;

-- 특정 기간 동안의 권한 변경 통계
SELECT
    action,
    resource_type,
    COUNT(*) as count
FROM audit_logs
WHERE tenant_id = 'tenant-001'
  AND timestamp BETWEEN '2026-01-01' AND '2026-01-31'
  AND resource_type IN ('ROLE', 'PERMISSION', 'AGENT_ROLE')
GROUP BY action, resource_type
ORDER BY count DESC;
```

---

## 7. 초기 데이터 (Seed Data)

### 6.1 기본 역할

```sql
-- POSITION 역할
INSERT INTO rbac_roles (name, type, data_scope, description) VALUES
('ADMIN', 'POSITION', 'ADMIN', '시스템 관리자'),
('TEAM_LEAD', 'POSITION', 'TEAM_LEAD', '팀장/부서장'),
('AGENT', 'POSITION', 'MEMBER', '일반 상담사');

-- CHANNEL 역할
INSERT INTO rbac_roles (name, type, data_scope, description) VALUES
('VOICE_INBOUND', 'CHANNEL', NULL, '인바운드 전화 상담'),
('VOICE_OUTBOUND', 'CHANNEL', NULL, '아웃바운드 전화 상담'),
('CHAT', 'CHANNEL', NULL, '채팅 상담'),
('EMAIL', 'CHANNEL', NULL, '이메일 상담'),
('CALLBACK', 'CHANNEL', NULL, '콜백 관리');
```

### 6.2 기본 권한

```sql
-- Agent 관련
INSERT INTO rbac_permissions (code, name, category) VALUES
('agent:create', '상담사 생성', 'AGENT'),
('agent:read', '상담사 조회', 'AGENT'),
('agent:read:self', '본인 정보 조회', 'AGENT'),
('agent:update', '상담사 수정', 'AGENT'),
('agent:update:self', '본인 정보 수정', 'AGENT'),
('agent:delete', '상담사 퇴사', 'AGENT'),
('agent:suspend', '상담사 정지', 'AGENT'),
('agent:transfer', '부서 이동', 'AGENT'),
('agent:role:assign', '역할 할당', 'AGENT'),
('agent:password:reset', '비밀번호 초기화', 'AGENT');

-- Department 관련
INSERT INTO rbac_permissions (code, name, category) VALUES
('dept:create', '부서 생성', 'DEPARTMENT'),
('dept:read', '부서 조회', 'DEPARTMENT'),
('dept:update', '부서 수정', 'DEPARTMENT'),
('dept:delete', '부서 삭제', 'DEPARTMENT'),
('dept:move', '부서 이동', 'DEPARTMENT'),
('dept:deactivate', '부서 비활성화', 'DEPARTMENT');

-- RBAC 관련
INSERT INTO rbac_permissions (code, name, category) VALUES
('role:create', '역할 생성', 'RBAC'),
('role:read', '역할 조회', 'RBAC'),
('role:update', '역할 수정', 'RBAC'),
('role:delete', '역할 삭제', 'RBAC'),
('permission:read', '권한 조회', 'RBAC'),
('permission:assign', '권한 할당', 'RBAC');

-- Voice 관련
INSERT INTO rbac_permissions (code, name, category) VALUES
('call:receive', '전화 수신', 'VOICE'),
('call:dial', '전화 발신', 'VOICE'),
('call:transfer', '전화 전환', 'VOICE'),
('call:hold', '전화 보류', 'VOICE');

-- Chat 관련
INSERT INTO rbac_permissions (code, name, category) VALUES
('chat:receive', '채팅 수신', 'CHAT'),
('chat:send', '채팅 발송', 'CHAT'),
('chat:transfer', '채팅 전환', 'CHAT');

-- Email 관련
INSERT INTO rbac_permissions (code, name, category) VALUES
('email:receive', '이메일 수신', 'EMAIL'),
('email:send', '이메일 발송', 'EMAIL');

-- Callback 관련
INSERT INTO rbac_permissions (code, name, category) VALUES
('callback:create', '콜백 등록', 'CALLBACK'),
('callback:manage', '콜백 관리', 'CALLBACK');

-- Report 관련
INSERT INTO rbac_permissions (code, name, category) VALUES
('report:view', '리포트 조회', 'REPORT'),
('report:export', '리포트 내보내기', 'REPORT'),
('report:create', '리포트 생성', 'REPORT');
```

### 6.3 역할-권한 매핑

```sql
-- ADMIN 역할에 모든 관리 권한
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac_roles r, rbac_permissions p
WHERE r.name = 'ADMIN'
AND p.category IN ('AGENT', 'DEPARTMENT', 'RBAC', 'REPORT');

-- TEAM_LEAD 역할
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac_roles r, rbac_permissions p
WHERE r.name = 'TEAM_LEAD'
AND p.code IN ('agent:read', 'agent:update', 'agent:suspend', 'agent:transfer',
               'agent:password:reset', 'dept:read', 'report:view', 'report:export');

-- AGENT 역할
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac_roles r, rbac_permissions p
WHERE r.name = 'AGENT'
AND p.code IN ('agent:read:self', 'agent:update:self');

-- VOICE_INBOUND 역할
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac_roles r, rbac_permissions p
WHERE r.name = 'VOICE_INBOUND'
AND p.code IN ('call:receive', 'call:transfer', 'call:hold');

-- VOICE_OUTBOUND 역할
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac_roles r, rbac_permissions p
WHERE r.name = 'VOICE_OUTBOUND'
AND p.code IN ('call:dial', 'call:receive', 'call:transfer', 'call:hold', 'callback:create');

-- CHAT 역할
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac_roles r, rbac_permissions p
WHERE r.name = 'CHAT'
AND p.code IN ('chat:receive', 'chat:send', 'chat:transfer');

-- EMAIL 역할
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac_roles r, rbac_permissions p
WHERE r.name = 'EMAIL'
AND p.code IN ('email:receive', 'email:send');

-- CALLBACK 역할
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rbac_roles r, rbac_permissions p
WHERE r.name = 'CALLBACK'
AND p.code IN ('callback:create', 'callback:manage');
```

---

## 8. 테이블 요약

| 모듈 | 테이블명 | 설명 | 주요 관계 |
|------|----------|------|----------|
| User | user_agents | 상담사 | FK → org_departments |
| User | user_agent_roles | 상담사-역할 매핑 | FK → user_agents, rbac_roles |
| Organization | org_departments | 부서 (트리) | Self-referential |
| RBAC | rbac_roles | 역할 | - |
| RBAC | rbac_permissions | 권한 | - |
| RBAC | rbac_role_permissions | 역할-권한 매핑 | FK → rbac_roles, rbac_permissions |
| Audit | audit_logs | 감사 로그 | 독립 (FK 없음) |
| Audit | audit_logs_archive | 감사 로그 아카이브 | 독립 (FK 없음) |

---

## 9. 마이그레이션 순서

```
1. rbac_permissions     (기초 데이터)
2. rbac_roles           (기초 데이터)
3. rbac_role_permissions (매핑)
4. org_departments      (조직 구조)
5. user_agents          (상담사)
6. user_agent_roles     (상담사-역할 매핑)
7. audit_logs           (감사 로그 - FK 의존성 없음, 어느 시점에나 생성 가능)
8. audit_logs_archive   (감사 로그 아카이브)
```

---

## 10. 성능 고려사항

### 10.1 인덱스 전략

| 테이블 | 인덱스 | 용도 |
|--------|--------|------|
| user_agents | (tenant_id) | 테넌트별 조회 |
| user_agents | (tenant_id, department_id) | 부서별 상담사 조회 |
| user_agents | (tenant_id, status) | 상태별 조회 |
| org_departments | (tenant_id, parent_id) | 하위 부서 조회 |
| audit_logs | (tenant_id) | 테넌트별 감사 로그 조회 |
| audit_logs | (timestamp DESC) | 시간 역순 조회 (최신 이력 우선) |
| audit_logs | (resource_type, resource_id) | 특정 리소스 이력 조회 |

### 10.2 재귀 쿼리 최적화

```sql
-- org_departments에 대한 재귀 쿼리용 인덱스
CREATE INDEX idx_org_depts_tree ON org_departments (tenant_id, dept_id, parent_id);
```


### 10.3 Soft Delete 조회
```sql
-- 퇴사자 제외 조회 (기본)
SELECT * FROM user_agents
WHERE tenant_id = ? AND status != 'RETIRED';

-- 예약 삭제 대상 조회 (스케줄러용)
SELECT * FROM user_agents
WHERE scheduled_delete_at <= CURRENT_TIMESTAMP
AND scheduled_delete_at IS NOT NULL;
```

### 10.4 감사 로그 파티셔닝 (선택)

```sql
-- 대용량 환경에서 월별 파티셔닝 고려
-- PostgreSQL 선언적 파티셔닝 (11+)
CREATE TABLE audit_logs_partitioned (
    -- 컬럼 정의 동일
) PARTITION BY RANGE (timestamp);

-- 월별 파티션 생성 예시
CREATE TABLE audit_logs_2026_01 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
```

---

## 11. 제약조건 요약

### 11.1 Unique 제약

| 테이블 | 컬럼 | 설명 |
|--------|------|------|
| user_agents | (tenant_id, login_id) | 테넌트 내 로그인ID 유일 |
| org_departments | (tenant_id, code) | 테넌트 내 부서코드 유일 |
| rbac_roles | name | 역할명 유일 |
| rbac_permissions | code | 권한코드 유일 |

### 11.2 Check 제약

| 테이블 | 컬럼 | 유효값 |
|--------|------|--------|
| user_agents | status | ACTIVE, SUSPENDED, RETIRED |
| org_departments | type | COMPANY, DIVISION, TEAM, GROUP, CUSTOM |
| org_departments | status | ACTIVE, INACTIVE |
| rbac_roles | type | POSITION, CHANNEL |
| rbac_roles | data_scope | ADMIN, TEAM_LEAD, MEMBER (NULL 가능) |

### 11.3 Foreign Key 제약

| 테이블 | 컬럼 | 참조 | ON DELETE |
|--------|------|------|-----------|
| user_agents | department_id | org_departments | RESTRICT |
| user_agent_roles | agent_id | user_agents | CASCADE |
| user_agent_roles | role_id | rbac_roles | CASCADE |
| org_departments | parent_id | org_departments | RESTRICT |
| rbac_role_permissions | role_id | rbac_roles | CASCADE |
| rbac_role_permissions | permission_id | rbac_permissions | CASCADE |

---

*문서 버전: 2.0*  
*최종 수정: 2026-01-22*  
*변경 사항: 감사 로그(Audit) 섹션 추가 (audit_logs, audit_logs_archive)*
