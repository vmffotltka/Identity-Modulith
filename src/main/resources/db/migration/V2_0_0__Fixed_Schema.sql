-- =============================================================================
-- Identity Modulith - 완전한 데이터베이스 초기화 스크립트
-- 버전: V2.0.0
-- 엔티티 구조 100% 반영 (updated_at 포함)
-- 작성일: 2026-02-06
-- =============================================================================

-- 기존 테이블 삭제 (역순)
DROP TABLE IF EXISTS rbac_agent_roles CASCADE;
DROP TABLE IF EXISTS rbac_role_permissions CASCADE;
DROP TABLE IF EXISTS rbac_permissions CASCADE;
DROP TABLE IF EXISTS rbac_roles CASCADE;
DROP TABLE IF EXISTS user_agents CASCADE;
DROP TABLE IF EXISTS org_departments CASCADE;

-- =============================================================================
-- 1. Organization - 부서(Department) 테이블
-- =============================================================================

CREATE TABLE org_departments (
    dept_id             VARCHAR(36)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    parent_id           VARCHAR(36),
    org_path            TEXT            NOT NULL,
    depth               INTEGER         NOT NULL DEFAULT 0,
    name                VARCHAR(100)    NOT NULL,
    type                VARCHAR(20),
    code                VARCHAR(30)     NOT NULL,
    custom_type_name    VARCHAR(50),
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    deactivated_at      TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),
    version             BIGINT          DEFAULT 0,

    CONSTRAINT fk_dept_parent FOREIGN KEY (parent_id)
        REFERENCES org_departments(dept_id) ON DELETE RESTRICT,
    CONSTRAINT uk_dept_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_dept_type CHECK (type IN ('COMPANY', 'DIVISION', 'TEAM', 'GROUP', 'CUSTOM')),
    CONSTRAINT chk_dept_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_dept_tenant ON org_departments(tenant_id);
CREATE INDEX idx_dept_parent ON org_departments(parent_id);
CREATE INDEX idx_dept_org_path ON org_departments(org_path);
CREATE INDEX idx_dept_status ON org_departments(status);

COMMENT ON TABLE org_departments IS '조직 부서 테이블';

-- =============================================================================
-- 2. User - 상담사(Agent) 테이블
-- =============================================================================

CREATE TABLE user_agents (
    agent_id            VARCHAR(36)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    login_id            VARCHAR(100)    NOT NULL UNIQUE,
    password            VARCHAR(255)    NOT NULL,
    password_must_change BOOLEAN        DEFAULT FALSE,
    name                VARCHAR(100)    NOT NULL,
    employee_id         VARCHAR(30),
    email               VARCHAR(255),
    phone               VARCHAR(20),
    dept_id             VARCHAR(36),
    status              VARCHAR(20)     DEFAULT 'ACTIVE',
    suspended_at        TIMESTAMP,
    retired_at          TIMESTAMP,
    scheduled_delete_at TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),
    version             INTEGER         DEFAULT 0,
    role_id             VARCHAR(50),

    CONSTRAINT fk_agent_dept FOREIGN KEY (dept_id)
        REFERENCES org_departments(dept_id) ON DELETE SET NULL,
    CONSTRAINT chk_agent_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'RETIRED'))
);

CREATE INDEX idx_agent_tenant ON user_agents(tenant_id);
CREATE INDEX idx_agent_dept ON user_agents(dept_id);
CREATE INDEX idx_agent_status ON user_agents(status);
CREATE INDEX idx_agent_login_id ON user_agents(login_id);

COMMENT ON TABLE user_agents IS '상담사/사용자 테이블';

-- =============================================================================
-- 3. RBAC - 역할(Role) 테이블
-- =============================================================================

CREATE TABLE rbac_roles (
    role_id             VARCHAR(36)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    type                VARCHAR(20)     NOT NULL,
    data_scope_level    VARCHAR(20),
    description         VARCHAR(255),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),
    version             INTEGER         DEFAULT 0,

    CONSTRAINT uk_roles_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT chk_role_type CHECK (type IN ('POSITION', 'CHANNEL', 'SKILL', 'CUSTOM')),
    CONSTRAINT chk_data_scope CHECK (data_scope_level IN ('ADMIN', 'TEAM_LEAD', 'MEMBER', 'CUSTOM'))
);

CREATE INDEX idx_role_tenant ON rbac_roles(tenant_id);
CREATE INDEX idx_role_type ON rbac_roles(type);

COMMENT ON TABLE rbac_roles IS 'RBAC 역할 테이블';

-- =============================================================================
-- 4. RBAC - 권한(Permission) 테이블
-- =============================================================================

CREATE TABLE rbac_permissions (
    permission_id       VARCHAR(36)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,
    code                VARCHAR(128)    NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    description         VARCHAR(500),
    category            VARCHAR(64),
    resource            VARCHAR(100),
    action              VARCHAR(50),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT          DEFAULT 0,

    CONSTRAINT uk_perm_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_perm_tenant ON rbac_permissions(tenant_id);
CREATE INDEX idx_perm_code ON rbac_permissions(code);

COMMENT ON TABLE rbac_permissions IS 'RBAC 권한 테이블';

-- =============================================================================
-- 5. RBAC - 역할-권한 매핑 테이블
-- =============================================================================

CREATE TABLE rbac_role_permissions (
    id                  BIGSERIAL       PRIMARY KEY,
    role_id             VARCHAR(36)     NOT NULL,
    permission_id       VARCHAR(36)     NOT NULL,
    assigned_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(36),

    CONSTRAINT fk_rp_role FOREIGN KEY (role_id)
        REFERENCES rbac_roles(role_id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id)
        REFERENCES rbac_permissions(permission_id) ON DELETE CASCADE,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_rp_role ON rbac_role_permissions(role_id);
CREATE INDEX idx_rp_permission ON rbac_role_permissions(permission_id);
CREATE INDEX idx_rp_assigned_at ON rbac_role_permissions(assigned_at);

-- =============================================================================
-- 6. RBAC - 사용자-역할 매핑 테이블
-- =============================================================================

CREATE TABLE rbac_agent_roles (
    id                  BIGSERIAL       PRIMARY KEY,
    agent_id            VARCHAR(36)     NOT NULL,
    role_id             VARCHAR(36)     NOT NULL,
    assigned_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(36),

    CONSTRAINT fk_agent_roles_agent FOREIGN KEY (agent_id)
        REFERENCES user_agents(agent_id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_roles_role FOREIGN KEY (role_id)
        REFERENCES rbac_roles(role_id) ON DELETE CASCADE,
    CONSTRAINT uk_agent_roles UNIQUE (agent_id, role_id)
);

CREATE INDEX idx_agent_roles_agent ON rbac_agent_roles(agent_id);
CREATE INDEX idx_agent_roles_role ON rbac_agent_roles(role_id);
CREATE INDEX idx_agent_roles_assigned_at ON rbac_agent_roles(assigned_at);

COMMENT ON TABLE rbac_agent_roles IS 'RBAC 사용자-역할 매핑 테이블';

-- =============================================================================
-- 표준 데이터 삽입
-- =============================================================================

DO $$
DECLARE
    std_tenant VARCHAR(50) := 'default-tenant';
    now_time TIMESTAMP := NOW();

    -- 부서 ID
    company_id VARCHAR(36) := '00000000-0000-0000-0000-000000000001';
    dev_div_id VARCHAR(36) := '00000000-0000-0000-0000-000000000002';
    sales_div_id VARCHAR(36) := '00000000-0000-0000-0000-000000000003';
    backend_team_id VARCHAR(36) := '00000000-0000-0000-0000-000000000004';
    frontend_team_id VARCHAR(36) := '00000000-0000-0000-0000-000000000005';

    -- 사용자 ID
    admin_id VARCHAR(36) := '10000000-0000-0000-0000-000000000001';
    dev_lead_id VARCHAR(36) := '10000000-0000-0000-0000-000000000002';
    dev_member_id VARCHAR(36) := '10000000-0000-0000-0000-000000000003';

    -- 역할 ID
    admin_role_id VARCHAR(36) := '20000000-0000-0000-0000-000000000001';
    team_lead_role_id VARCHAR(36) := '20000000-0000-0000-0000-000000000002';
    member_role_id VARCHAR(36) := '20000000-0000-0000-0000-000000000003';

    -- 권한 ID
    perm_user_create VARCHAR(36) := '30000000-0000-0000-0000-000000000001';
    perm_user_read VARCHAR(36) := '30000000-0000-0000-0000-000000000002';
    perm_user_update VARCHAR(36) := '30000000-0000-0000-0000-000000000003';
    perm_user_delete VARCHAR(36) := '30000000-0000-0000-0000-000000000004';
    perm_org_create VARCHAR(36) := '30000000-0000-0000-0000-000000000005';
    perm_org_read VARCHAR(36) := '30000000-0000-0000-0000-000000000006';
    perm_org_update VARCHAR(36) := '30000000-0000-0000-0000-000000000007';
    perm_rbac_manage VARCHAR(36) := '30000000-0000-0000-0000-000000000008';
    perm_report_view VARCHAR(36) := '30000000-0000-0000-0000-000000000009';
    perm_report_export VARCHAR(36) := '30000000-0000-0000-0000-000000000010';

BEGIN
    -- 부서 생성
    INSERT INTO org_departments (dept_id, tenant_id, parent_id, name, type, code, org_path, depth, status, created_at, updated_at)
    VALUES
        (company_id, std_tenant, NULL, '넥스프론', 'COMPANY', 'NEXFRON', '/' || company_id, 0, 'ACTIVE', now_time, now_time),
        (dev_div_id, std_tenant, company_id, '개발본부', 'DIVISION', 'DEV-DIV', '/' || company_id || '/' || dev_div_id, 1, 'ACTIVE', now_time, now_time),
        (sales_div_id, std_tenant, company_id, '영업본부', 'DIVISION', 'SALES-DIV', '/' || company_id || '/' || sales_div_id, 1, 'ACTIVE', now_time, now_time),
        (backend_team_id, std_tenant, dev_div_id, '백엔드팀', 'TEAM', 'DEV-BE', '/' || company_id || '/' || dev_div_id || '/' || backend_team_id, 2, 'ACTIVE', now_time, now_time),
        (frontend_team_id, std_tenant, dev_div_id, '프론트엔드팀', 'TEAM', 'DEV-FE', '/' || company_id || '/' || dev_div_id || '/' || frontend_team_id, 2, 'ACTIVE', now_time, now_time);

    -- 사용자 생성
    -- 비밀번호: Admin123! (BCrypt 해시)
    INSERT INTO user_agents (agent_id, tenant_id, login_id, password, name, employee_id, email, dept_id, status, created_at, updated_at)
    VALUES
        (admin_id, std_tenant, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm', '시스템관리자', 'EMP-0001', 'admin@nexfron.com', company_id, 'ACTIVE', now_time, now_time),
        (dev_lead_id, std_tenant, 'dev.lead', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm', '김팀장', 'EMP-0002', 'dev.lead@nexfron.com', backend_team_id, 'ACTIVE', now_time, now_time),
        (dev_member_id, std_tenant, 'dev.member', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm', '이개발', 'EMP-0003', 'dev.member@nexfron.com', backend_team_id, 'ACTIVE', now_time, now_time);

    -- 역할 생성
    INSERT INTO rbac_roles (role_id, tenant_id, name, type, data_scope_level, is_active, created_at, updated_at)
    VALUES
        (admin_role_id, std_tenant, 'ADMIN', 'POSITION', 'ADMIN', TRUE, now_time, now_time),
        (team_lead_role_id, std_tenant, 'TEAM_LEAD', 'POSITION', 'TEAM_LEAD', TRUE, now_time, now_time),
        (member_role_id, std_tenant, 'MEMBER', 'POSITION', 'MEMBER', TRUE, now_time, now_time);

    -- 권한 생성
    INSERT INTO rbac_permissions (permission_id, tenant_id, code, name, resource, action, created_at, updated_at)
    VALUES
        (perm_user_create, std_tenant, 'user:create', '사용자 생성', 'user', 'create', now_time, now_time),
        (perm_user_read, std_tenant, 'user:read', '사용자 조회', 'user', 'read', now_time, now_time),
        (perm_user_update, std_tenant, 'user:update', '사용자 수정', 'user', 'update', now_time, now_time),
        (perm_user_delete, std_tenant, 'user:delete', '사용자 삭제', 'user', 'delete', now_time, now_time),
        (perm_org_create, std_tenant, 'org:create', '조직 생성', 'organization', 'create', now_time, now_time),
        (perm_org_read, std_tenant, 'org:read', '조직 조회', 'organization', 'read', now_time, now_time),
        (perm_org_update, std_tenant, 'org:update', '조직 수정', 'organization', 'update', now_time, now_time),
        (perm_rbac_manage, std_tenant, 'rbac:manage', 'RBAC 관리', 'rbac', 'manage', now_time, now_time),
        (perm_report_view, std_tenant, 'report:view', '보고서 조회', 'report', 'view', now_time, now_time),
        (perm_report_export, std_tenant, 'report:export', '보고서 내보내기', 'report', 'export', now_time, now_time);

    -- 역할-권한 매핑 (ADMIN: 모든 권한)
    INSERT INTO rbac_role_permissions (role_id, permission_id, assigned_at, created_at)
    SELECT admin_role_id, permission_id, now_time, now_time
    FROM rbac_permissions WHERE tenant_id = std_tenant;

    -- TEAM_LEAD 권한 (조회만 가능, 수정/생성 불가)
    INSERT INTO rbac_role_permissions (role_id, permission_id, assigned_at, created_at)
    VALUES
        (team_lead_role_id, perm_user_read, now_time, now_time),
        (team_lead_role_id, perm_org_read, now_time, now_time),
        (team_lead_role_id, perm_report_view, now_time, now_time),
        (team_lead_role_id, perm_report_export, now_time, now_time);

    -- MEMBER 권한
    INSERT INTO rbac_role_permissions (role_id, permission_id, assigned_at, created_at)
    VALUES
        (member_role_id, perm_user_read, now_time, now_time),
        (member_role_id, perm_org_read, now_time, now_time),
        (member_role_id, perm_report_view, now_time, now_time);

    -- 사용자-역할 매핑
    INSERT INTO rbac_agent_roles (agent_id, role_id, assigned_at, created_at)
    VALUES
        (admin_id, admin_role_id, now_time, now_time),
        (dev_lead_id, team_lead_role_id, now_time, now_time),
        (dev_member_id, member_role_id, now_time, now_time);

END $$;

-- =============================================================================
-- 완료 메시지
-- =============================================================================

DO $$
BEGIN
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Identity Modulith Database Initialized Successfully!';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Test Accounts: admin/admin123, dev.lead/admin123, dev.member/admin123';
    RAISE NOTICE '=============================================================================';
END $$;

