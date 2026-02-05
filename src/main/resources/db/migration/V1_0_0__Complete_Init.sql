-- =============================================================================
-- Identity Modulith - 통합 초기화 스크립트
-- 버전: V1.0.0
-- 설명: 전체 스키마 및 초기 데이터 생성 (통합본)
-- 작성일: 2026-02-04
-- =============================================================================

-- =============================================================================
-- 1. 조직(Organization) 모듈 테이블
-- =============================================================================

-- 부서(Department) 테이블
CREATE TABLE IF NOT EXISTS org_departments (
    dept_id             VARCHAR(50)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    type                VARCHAR(20)     NOT NULL,  -- COMPANY, DIVISION, TEAM, GROUP, CUSTOM
    custom_type_name    VARCHAR(50),
    parent_dept_id      VARCHAR(50),
    org_path            TEXT            NOT NULL,
    depth               INTEGER         NOT NULL DEFAULT 0,
    display_order       INTEGER         NOT NULL DEFAULT 0,
    manager_id          VARCHAR(50),
    description         TEXT,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50),

    CONSTRAINT fk_parent_dept FOREIGN KEY (parent_dept_id)
        REFERENCES org_departments(dept_id) ON DELETE RESTRICT,
    CONSTRAINT chk_dept_type CHECK (type IN ('COMPANY', 'DIVISION', 'TEAM', 'GROUP', 'CUSTOM')),
    CONSTRAINT chk_custom_type CHECK (
        (type = 'CUSTOM' AND custom_type_name IS NOT NULL) OR
        (type != 'CUSTOM')
    )
);

CREATE INDEX IF NOT EXISTS idx_dept_tenant ON org_departments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dept_parent ON org_departments(parent_dept_id);
CREATE INDEX IF NOT EXISTS idx_dept_org_path ON org_departments(org_path);
CREATE INDEX IF NOT EXISTS idx_dept_active ON org_departments(is_active);

COMMENT ON TABLE org_departments IS '조직 부서 테이블';
COMMENT ON COLUMN org_departments.type IS '부서 타입: COMPANY(회사), DIVISION(본부), TEAM(팀), GROUP(그룹), CUSTOM(사용자정의)';
COMMENT ON COLUMN org_departments.org_path IS 'Materialized Path 방식 경로 (예: /root/dept1/dept2/)';

-- =============================================================================
-- 2. RBAC 모듈 테이블
-- =============================================================================

-- 역할(Role) 테이블
CREATE TABLE IF NOT EXISTS rbac_roles (
    role_id             VARCHAR(50)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    name                VARCHAR(50)     NOT NULL,
    type                VARCHAR(20)     NOT NULL,  -- POSITION, CHANNEL
    data_scope          VARCHAR(20),               -- ADMIN, TEAM_LEAD, MEMBER (POSITION일 때만)
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

CREATE INDEX IF NOT EXISTS idx_role_tenant ON rbac_roles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_role_type ON rbac_roles(type);
CREATE INDEX IF NOT EXISTS idx_role_active ON rbac_roles(is_active);

COMMENT ON TABLE rbac_roles IS '역할 정의 테이블';
COMMENT ON COLUMN rbac_roles.type IS 'POSITION: 직급(ADMIN, TEAM_LEAD, AGENT), CHANNEL: 채널(VOICE, CHAT 등)';
COMMENT ON COLUMN rbac_roles.data_scope IS 'DataScope 레벨 (POSITION 타입일 때만 사용)';

-- 권한(Permission) 테이블
CREATE TABLE IF NOT EXISTS rbac_permissions (
    permission_id       VARCHAR(50)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    code                VARCHAR(100)    NOT NULL,
    name                VARCHAR(100),
    description         VARCHAR(255),
    category            VARCHAR(50),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_permission_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_permission_tenant ON rbac_permissions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_permission_category ON rbac_permissions(category);

COMMENT ON TABLE rbac_permissions IS '권한 정의 테이블';
COMMENT ON COLUMN rbac_permissions.code IS '권한 코드 (예: agent:create, dept:read)';
COMMENT ON COLUMN rbac_permissions.category IS '권한 카테고리 (예: AGENT, DEPARTMENT, CHANNEL)';

-- 역할-권한 매핑 테이블
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

CREATE INDEX IF NOT EXISTS idx_rp_role ON rbac_role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_rp_permission ON rbac_role_permissions(permission_id);

COMMENT ON TABLE rbac_role_permissions IS '역할-권한 매핑 테이블 (M:N)';

-- =============================================================================
-- 3. User 모듈 테이블
-- =============================================================================

-- 상담사(Agent) 테이블
CREATE TABLE IF NOT EXISTS agents (
    agent_id            VARCHAR(50)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    login_id            VARCHAR(50)     NOT NULL,
    password            VARCHAR(255)    NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    employee_id         VARCHAR(50),
    email               VARCHAR(100),
    phone               VARCHAR(20),
    dept_id             VARCHAR(50),
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    password_must_change BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    suspended_at        TIMESTAMP,
    retired_at          TIMESTAMP,
    scheduled_delete_at TIMESTAMP,
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50),
    suspended_by        VARCHAR(50),
    retired_by          VARCHAR(50),
    version             BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT uk_agent_tenant_login UNIQUE (tenant_id, login_id),
    CONSTRAINT chk_agent_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'RETIRED')),
    CONSTRAINT fk_agent_dept FOREIGN KEY (dept_id)
        REFERENCES org_departments(dept_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_agent_tenant ON agents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_login ON agents(login_id);
CREATE INDEX IF NOT EXISTS idx_agent_dept ON agents(dept_id);
CREATE INDEX IF NOT EXISTS idx_agent_status ON agents(status);
CREATE INDEX IF NOT EXISTS idx_agent_scheduled_delete ON agents(scheduled_delete_at)
    WHERE scheduled_delete_at IS NOT NULL;

COMMENT ON TABLE agents IS '상담사(Agent) 테이블';
COMMENT ON COLUMN agents.status IS 'ACTIVE: 활성, SUSPENDED: 정지, RETIRED: 퇴사';
COMMENT ON COLUMN agents.version IS 'Optimistic Locking용 버전';

-- 상담사-역할 매핑 테이블
CREATE TABLE IF NOT EXISTS rbac_agent_roles (
    agent_id            VARCHAR(50)     NOT NULL,
    role_id             VARCHAR(50)     NOT NULL,
    assigned_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by         VARCHAR(50),

    PRIMARY KEY (agent_id, role_id),
    CONSTRAINT fk_ar_agent FOREIGN KEY (agent_id)
        REFERENCES agents(agent_id) ON DELETE CASCADE,
    CONSTRAINT fk_ar_role FOREIGN KEY (role_id)
        REFERENCES rbac_roles(role_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ar_agent ON rbac_agent_roles(agent_id);
CREATE INDEX IF NOT EXISTS idx_ar_role ON rbac_agent_roles(role_id);

COMMENT ON TABLE rbac_agent_roles IS '상담사-역할 매핑 테이블 (M:N)';

-- =============================================================================
-- 4. 초기 데이터 - 기본 역할(Role)
-- =============================================================================

-- POSITION 역할 (직급)
INSERT INTO rbac_roles (role_id, tenant_id, name, type, data_scope, description, is_active, created_at, updated_at)
VALUES
    ('role-admin-001', 'tenant-001', 'ADMIN', 'POSITION', 'ADMIN', '시스템 관리자 (전체 조직 접근)', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role-teamlead-001', 'tenant-001', 'TEAM_LEAD', 'POSITION', 'TEAM_LEAD', '팀장 (본인 팀 + 하위 부서 접근)', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role-agent-001', 'tenant-001', 'AGENT', 'POSITION', 'MEMBER', '일반 상담사 (본인 팀만 접근)', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id, name) DO NOTHING;

-- CHANNEL 역할 (채널)
INSERT INTO rbac_roles (role_id, tenant_id, name, type, data_scope, description, is_active, created_at, updated_at)
VALUES
    ('role-ch-inbound', 'tenant-001', 'INBOUND_AGENT', 'CHANNEL', NULL, '인바운드 전화 상담', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role-ch-outbound', 'tenant-001', 'OUTBOUND_AGENT', 'CHANNEL', NULL, '아웃바운드 전화 상담', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role-ch-chat', 'tenant-001', 'CHAT_AGENT', 'CHANNEL', NULL, '채팅 상담', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role-ch-email', 'tenant-001', 'EMAIL_AGENT', 'CHANNEL', NULL, '이메일 상담', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role-ch-multi', 'tenant-001', 'MULTI_CHANNEL_AGENT', 'CHANNEL', NULL, '멀티채널 상담 (모든 채널)', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id, name) DO NOTHING;

-- =============================================================================
-- 5. 초기 데이터 - 권한(Permission)
-- =============================================================================

-- AGENT 카테고리 권한
INSERT INTO rbac_permissions (permission_id, tenant_id, code, name, description, category, created_at)
VALUES
    ('perm-agent-001', 'tenant-001', 'agent:create', '상담사 생성', '새로운 상담사 계정 생성', 'AGENT', CURRENT_TIMESTAMP),
    ('perm-agent-002', 'tenant-001', 'agent:read', '상담사 조회', '상담사 정보 조회', 'AGENT', CURRENT_TIMESTAMP),
    ('perm-agent-003', 'tenant-001', 'agent:update', '상담사 수정', '상담사 정보 수정', 'AGENT', CURRENT_TIMESTAMP),
    ('perm-agent-004', 'tenant-001', 'agent:delete', '상담사 삭제', '상담사 계정 삭제', 'AGENT', CURRENT_TIMESTAMP),
    ('perm-agent-005', 'tenant-001', 'agent:suspend', '상담사 정지', '상담사 계정 정지', 'AGENT', CURRENT_TIMESTAMP),
    ('perm-agent-006', 'tenant-001', 'agent:activate', '상담사 활성화', '정지된 상담사 활성화', 'AGENT', CURRENT_TIMESTAMP),
    ('perm-agent-007', 'tenant-001', 'agent:transfer', '상담사 이동', '상담사 부서 이동', 'AGENT', CURRENT_TIMESTAMP),
    ('perm-agent-008', 'tenant-001', 'agent:role:assign', '역할 할당', '상담사에게 역할 할당', 'AGENT', CURRENT_TIMESTAMP),
    ('perm-agent-009', 'tenant-001', 'agent:password:reset', '비밀번호 초기화', '상담사 비밀번호 초기화', 'AGENT', CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id, code) DO NOTHING;

-- DEPARTMENT 카테고리 권한
INSERT INTO rbac_permissions (permission_id, tenant_id, code, name, description, category, created_at)
VALUES
    ('perm-dept-001', 'tenant-001', 'dept:create', '부서 생성', '새로운 부서 생성', 'DEPARTMENT', CURRENT_TIMESTAMP),
    ('perm-dept-002', 'tenant-001', 'dept:read', '부서 조회', '부서 정보 조회', 'DEPARTMENT', CURRENT_TIMESTAMP),
    ('perm-dept-003', 'tenant-001', 'dept:update', '부서 수정', '부서 정보 수정', 'DEPARTMENT', CURRENT_TIMESTAMP),
    ('perm-dept-004', 'tenant-001', 'dept:delete', '부서 삭제', '부서 삭제', 'DEPARTMENT', CURRENT_TIMESTAMP),
    ('perm-dept-005', 'tenant-001', 'dept:move', '부서 이동', '부서 위치 이동', 'DEPARTMENT', CURRENT_TIMESTAMP),
    ('perm-dept-006', 'tenant-001', 'dept:deactivate', '부서 비활성화', '부서 비활성화', 'DEPARTMENT', CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id, code) DO NOTHING;

-- RBAC 카테고리 권한
INSERT INTO rbac_permissions (permission_id, tenant_id, code, name, description, category, created_at)
VALUES
    ('perm-rbac-001', 'tenant-001', 'role:create', '역할 생성', '새로운 역할 생성', 'RBAC', CURRENT_TIMESTAMP),
    ('perm-rbac-002', 'tenant-001', 'role:read', '역할 조회', '역할 정보 조회', 'RBAC', CURRENT_TIMESTAMP),
    ('perm-rbac-003', 'tenant-001', 'role:update', '역할 수정', '역할 정보 수정', 'RBAC', CURRENT_TIMESTAMP),
    ('perm-rbac-004', 'tenant-001', 'role:delete', '역할 삭제', '역할 삭제', 'RBAC', CURRENT_TIMESTAMP),
    ('perm-rbac-005', 'tenant-001', 'permission:read', '권한 조회', '권한 목록 조회', 'RBAC', CURRENT_TIMESTAMP),
    ('perm-rbac-006', 'tenant-001', 'permission:assign', '권한 할당', '역할에 권한 할당', 'RBAC', CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id, code) DO NOTHING;

-- CHANNEL 카테고리 권한
INSERT INTO rbac_permissions (permission_id, tenant_id, code, name, description, category, created_at)
VALUES
    ('perm-ch-in-001', 'tenant-001', 'channel:inbound:receive', '인바운드 수신', '인바운드 전화 수신', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-ch-in-002', 'tenant-001', 'channel:inbound:hold', '통화 대기', '통화 대기 처리', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-ch-in-003', 'tenant-001', 'channel:inbound:transfer', '호 전환', '다른 상담사에게 호 전환', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-ch-out-001', 'tenant-001', 'channel:outbound:call', '아웃바운드 발신', '아웃바운드 전화 발신', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-ch-out-002', 'tenant-001', 'channel:outbound:campaign', '캠페인 관리', '캠페인 관리', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-ch-chat-001', 'tenant-001', 'channel:chat:message', '채팅 메시지', '채팅 메시지 송수신', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-ch-chat-002', 'tenant-001', 'channel:chat:file', '파일 전송', '채팅 파일 전송', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-ch-chat-003', 'tenant-001', 'channel:chat:emoji', '이모티콘', '이모티콘 사용', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-ch-email-001', 'tenant-001', 'channel:email:send', '이메일 발송', '이메일 발송', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-ch-email-002', 'tenant-001', 'channel:email:receive', '이메일 수신', '이메일 수신', 'CHANNEL', CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id, code) DO NOTHING;

-- =============================================================================
-- 6. 초기 데이터 - 역할-권한 매핑
-- =============================================================================

-- ADMIN 역할에 모든 권한 할당
INSERT INTO rbac_role_permissions (role_id, permission_id, assigned_at)
SELECT 'role-admin-001', permission_id, CURRENT_TIMESTAMP
FROM rbac_permissions
WHERE tenant_id = 'tenant-001'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- TEAM_LEAD 역할에 일부 권한 할당
INSERT INTO rbac_role_permissions (role_id, permission_id, assigned_at)
SELECT 'role-teamlead-001', permission_id, CURRENT_TIMESTAMP
FROM rbac_permissions
WHERE tenant_id = 'tenant-001'
  AND code IN ('agent:read', 'agent:update', 'agent:transfer', 'dept:read', 'role:read', 'permission:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- AGENT 역할에 기본 권한 할당
INSERT INTO rbac_role_permissions (role_id, permission_id, assigned_at)
SELECT 'role-agent-001', permission_id, CURRENT_TIMESTAMP
FROM rbac_permissions
WHERE tenant_id = 'tenant-001'
  AND code IN ('agent:read', 'dept:read', 'role:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 채널 역할에 채널별 권한 할당
INSERT INTO rbac_role_permissions (role_id, permission_id, assigned_at)
VALUES
    -- INBOUND_AGENT
    ('role-ch-inbound', 'perm-ch-in-001', CURRENT_TIMESTAMP),
    ('role-ch-inbound', 'perm-ch-in-002', CURRENT_TIMESTAMP),
    ('role-ch-inbound', 'perm-ch-in-003', CURRENT_TIMESTAMP),
    -- OUTBOUND_AGENT
    ('role-ch-outbound', 'perm-ch-out-001', CURRENT_TIMESTAMP),
    ('role-ch-outbound', 'perm-ch-out-002', CURRENT_TIMESTAMP),
    -- CHAT_AGENT
    ('role-ch-chat', 'perm-ch-chat-001', CURRENT_TIMESTAMP),
    ('role-ch-chat', 'perm-ch-chat-002', CURRENT_TIMESTAMP),
    ('role-ch-chat', 'perm-ch-chat-003', CURRENT_TIMESTAMP),
    -- EMAIL_AGENT
    ('role-ch-email', 'perm-ch-email-001', CURRENT_TIMESTAMP),
    ('role-ch-email', 'perm-ch-email-002', CURRENT_TIMESTAMP)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- MULTI_CHANNEL_AGENT에 모든 채널 권한 할당
INSERT INTO rbac_role_permissions (role_id, permission_id, assigned_at)
SELECT 'role-ch-multi', permission_id, CURRENT_TIMESTAMP
FROM rbac_permissions
WHERE tenant_id = 'tenant-001' AND category = 'CHANNEL'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- =============================================================================
-- 7. 초기 샘플 데이터
-- =============================================================================

-- 샘플 부서 생성
INSERT INTO org_departments (dept_id, tenant_id, name, type, parent_dept_id, org_path, depth, display_order, is_active)
VALUES
    ('dept-root-001', 'tenant-001', '넥스프론', 'COMPANY', NULL, '/dept-root-001/', 0, 1, TRUE),
    ('dept-div-001', 'tenant-001', '고객서비스본부', 'DIVISION', 'dept-root-001', '/dept-root-001/dept-div-001/', 1, 1, TRUE),
    ('dept-team-001', 'tenant-001', '인바운드팀', 'TEAM', 'dept-div-001', '/dept-root-001/dept-div-001/dept-team-001/', 2, 1, TRUE),
    ('dept-team-002', 'tenant-001', '아웃바운드팀', 'TEAM', 'dept-div-001', '/dept-root-001/dept-div-001/dept-team-002/', 2, 2, TRUE)
ON CONFLICT (dept_id) DO NOTHING;

-- 샘플 상담사 생성 (비밀번호: password123, BCrypt 인코딩)
INSERT INTO agents (agent_id, tenant_id, login_id, password, name, employee_id, email, phone, dept_id, status, password_must_change)
VALUES
    ('agent-admin-001', 'tenant-001', 'admin', '$2a$10$8K1p/a0dL3.W6ba/xH88su7pUdyJNgI3Jy0FsYqKOdw7tWpVKSzSy', '관리자', 'EMP001', 'admin@nexfron.com', '010-1234-5678', 'dept-root-001', 'ACTIVE', FALSE),
    ('agent-lead-001', 'tenant-001', 'teamlead01', '$2a$10$8K1p/a0dL3.W6ba/xH88su7pUdyJNgI3Jy0FsYqKOdw7tWpVKSzSy', '김팀장', 'EMP002', 'teamlead@nexfron.com', '010-2345-6789', 'dept-div-001', 'ACTIVE', FALSE),
    ('agent-001', 'tenant-001', 'agent01', '$2a$10$8K1p/a0dL3.W6ba/xH88su7pUdyJNgI3Jy0FsYqKOdw7tWpVKSzSy', '홍길동', 'EMP003', 'agent01@nexfron.com', '010-3456-7890', 'dept-team-001', 'ACTIVE', FALSE)
ON CONFLICT (tenant_id, login_id) DO NOTHING;

-- 샘플 상담사 역할 할당
INSERT INTO rbac_agent_roles (agent_id, role_id, assigned_at)
VALUES
    ('agent-admin-001', 'role-admin-001', CURRENT_TIMESTAMP),
    ('agent-lead-001', 'role-teamlead-001', CURRENT_TIMESTAMP),
    ('agent-lead-001', 'role-ch-inbound', CURRENT_TIMESTAMP),
    ('agent-001', 'role-agent-001', CURRENT_TIMESTAMP),
    ('agent-001', 'role-ch-inbound', CURRENT_TIMESTAMP),
    ('agent-001', 'role-ch-chat', CURRENT_TIMESTAMP)
ON CONFLICT (agent_id, role_id) DO NOTHING;

-- =============================================================================
-- 8. 검증 쿼리 (주석 처리 - 필요 시 실행)
-- =============================================================================

-- 생성된 테이블 확인
-- SELECT schemaname, tablename FROM pg_tables
-- WHERE schemaname = 'public'
--   AND (tablename LIKE 'org_%' OR tablename LIKE 'rbac_%' OR tablename = 'agents')
-- ORDER BY tablename;

-- 생성된 역할 확인
-- SELECT r.name, r.type, r.data_scope, COUNT(rp.permission_id) AS permission_count
-- FROM rbac_roles r
-- LEFT JOIN rbac_role_permissions rp ON r.role_id = rp.role_id
-- WHERE r.tenant_id = 'tenant-001'
-- GROUP BY r.role_id, r.name, r.type, r.data_scope
-- ORDER BY r.type, r.name;

-- 생성된 권한 확인
-- SELECT category, COUNT(*) AS permission_count
-- FROM rbac_permissions
-- WHERE tenant_id = 'tenant-001'
-- GROUP BY category
-- ORDER BY category;

-- 샘플 데이터 확인
-- SELECT a.login_id, a.name, d.name AS dept_name, STRING_AGG(r.name, ', ') AS roles
-- FROM agents a
-- LEFT JOIN org_departments d ON a.dept_id = d.dept_id
-- LEFT JOIN rbac_agent_roles ar ON a.agent_id = ar.agent_id
-- LEFT JOIN rbac_roles r ON ar.role_id = r.role_id
-- WHERE a.tenant_id = 'tenant-001'
-- GROUP BY a.agent_id, a.login_id, a.name, d.name
-- ORDER BY a.login_id;

-- =============================================================================
-- 초기화 완료
-- =============================================================================
