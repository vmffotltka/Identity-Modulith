-- =============================================================================
-- Identity Modulith - 완전한 데이터베이스 스키마 및 초기 데이터
-- 버전: V1.0.0
-- 설명: 엔티티 구조를 정확히 반영한 통합 스크립트
-- 작성일: 2026-02-06
-- =============================================================================

-- 기존 테이블 삭제 (역순으로)
DROP TABLE IF EXISTS rbac_agent_roles CASCADE;
DROP TABLE IF EXISTS rbac_role_permissions CASCADE;
DROP TABLE IF EXISTS rbac_permissions CASCADE;
DROP TABLE IF EXISTS rbac_roles CASCADE;
DROP TABLE IF EXISTS user_agents CASCADE;
DROP TABLE IF EXISTS org_departments CASCADE;

-- =============================================================================
-- 1. Organization 모듈 - 부서(Department) 테이블
-- =============================================================================

CREATE TABLE org_departments (
    -- 기본 식별자
    dept_id             VARCHAR(36)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,

    -- 계층 구조
    parent_id           VARCHAR(36),
    org_path            TEXT            NOT NULL,
    depth               INTEGER         NOT NULL DEFAULT 0,

    -- 부서 정보
    name                VARCHAR(100)    NOT NULL,
    type                VARCHAR(20),
    code                VARCHAR(30)     NOT NULL,
    custom_type_name    VARCHAR(50),

    -- 상태 관리
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    deactivated_at      TIMESTAMP,

    -- 감사 추적
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),

    -- 낙관적 잠금
    version             BIGINT          DEFAULT 0,

    -- 제약 조건
    CONSTRAINT fk_dept_parent FOREIGN KEY (parent_id)
        REFERENCES org_departments(dept_id) ON DELETE RESTRICT,
    CONSTRAINT uk_dept_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_dept_type CHECK (type IN ('COMPANY', 'DIVISION', 'TEAM', 'GROUP', 'CUSTOM')),
    CONSTRAINT chk_dept_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- 인덱스
CREATE INDEX idx_dept_tenant ON org_departments(tenant_id);
CREATE INDEX idx_dept_parent ON org_departments(parent_id);
CREATE INDEX idx_dept_org_path ON org_departments(org_path);
CREATE INDEX idx_dept_status ON org_departments(status);
CREATE INDEX idx_dept_type ON org_departments(type);

COMMENT ON TABLE org_departments IS '조직 부서 테이블';
COMMENT ON COLUMN org_departments.dept_id IS '부서 ID (UUID)';
COMMENT ON COLUMN org_departments.org_path IS 'Materialized Path 방식 경로';
COMMENT ON COLUMN org_departments.code IS '사용자 친화적 부서 코드';
COMMENT ON COLUMN org_departments.custom_type_name IS 'CUSTOM 타입일 때 사용자 정의 타입명';

-- =============================================================================
-- 2. User 모듈 - 상담사(Agent) 테이블
-- =============================================================================

CREATE TABLE user_agents (
    -- 기본 식별자
    agent_id            VARCHAR(36)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,

    -- 인증 정보
    login_id            VARCHAR(100)    NOT NULL UNIQUE,
    password            VARCHAR(255)    NOT NULL,
    password_must_change BOOLEAN        DEFAULT FALSE,

    -- 기본 정보
    name                VARCHAR(100)    NOT NULL,
    employee_id         VARCHAR(30),
    email               VARCHAR(255),
    phone               VARCHAR(20),

    -- 부서 정보
    dept_id             VARCHAR(36),

    -- 상태 관리
    status              VARCHAR(20)     DEFAULT 'ACTIVE',
    suspended_at        TIMESTAMP,
    retired_at          TIMESTAMP,
    scheduled_delete_at TIMESTAMP,

    -- 감사 추적
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),

    -- 낙관적 잠금
    version             INTEGER         DEFAULT 0,

    -- 임시 역할 (Deprecated)
    role_id             VARCHAR(50),

    -- 제약 조건
    CONSTRAINT fk_agent_dept FOREIGN KEY (dept_id)
        REFERENCES org_departments(dept_id) ON DELETE SET NULL,
    CONSTRAINT chk_agent_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'RETIRED'))
);

-- 인덱스
CREATE INDEX idx_agent_tenant ON user_agents(tenant_id);
CREATE INDEX idx_agent_dept ON user_agents(dept_id);
CREATE INDEX idx_agent_status ON user_agents(status);
CREATE INDEX idx_agent_login_id ON user_agents(login_id);
CREATE INDEX idx_agent_scheduled_delete ON user_agents(scheduled_delete_at)
    WHERE scheduled_delete_at IS NOT NULL;

COMMENT ON TABLE user_agents IS '상담사/사용자 테이블';
COMMENT ON COLUMN user_agents.agent_id IS '상담사 ID (UUID)';
COMMENT ON COLUMN user_agents.login_id IS '로그인 ID (고유)';
COMMENT ON COLUMN user_agents.employee_id IS '사번';
COMMENT ON COLUMN user_agents.status IS '상태: ACTIVE, INACTIVE, SUSPENDED, RETIRED';

-- =============================================================================
-- 3. RBAC 모듈 - 역할(Role) 테이블
-- =============================================================================

CREATE TABLE rbac_roles (
    -- 기본 식별자
    role_id             VARCHAR(36)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,

    -- 역할 정보
    name                VARCHAR(100)    NOT NULL,
    type                VARCHAR(20)     NOT NULL,
    data_scope_level    VARCHAR(20),

    -- 감사 추적
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),

    -- 낙관적 잠금
    version             INTEGER         DEFAULT 0,

    -- 제약 조건
    CONSTRAINT uk_roles_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT chk_role_type CHECK (type IN ('POSITION', 'CHANNEL', 'SKILL', 'CUSTOM')),
    CONSTRAINT chk_data_scope CHECK (data_scope_level IN ('SELF', 'DEPARTMENT', 'ALL', 'CUSTOM'))
);

-- 인덱스
CREATE INDEX idx_role_tenant ON rbac_roles(tenant_id);
CREATE INDEX idx_role_type ON rbac_roles(type);

COMMENT ON TABLE rbac_roles IS 'RBAC 역할 테이블';
COMMENT ON COLUMN rbac_roles.role_id IS '역할 ID (UUID)';
COMMENT ON COLUMN rbac_roles.type IS '역할 타입: POSITION(직책), CHANNEL(채널), SKILL(능력)';
COMMENT ON COLUMN rbac_roles.data_scope_level IS '데이터 범위: SELF, DEPARTMENT, ALL';

-- =============================================================================
-- 4. RBAC 모듈 - 권한(Permission) 테이블
-- =============================================================================

CREATE TABLE rbac_permissions (
    -- 기본 식별자
    permission_id       VARCHAR(36)     PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL,

    -- 권한 정보
    code                VARCHAR(100)    NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    resource            VARCHAR(100),
    action              VARCHAR(50),

    -- 감사 추적
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),

    -- 낙관적 잠금
    version             INTEGER         DEFAULT 0,

    -- 제약 조건
    CONSTRAINT uk_perm_tenant_code UNIQUE (tenant_id, code)
);

-- 인덱스
CREATE INDEX idx_perm_tenant ON rbac_permissions(tenant_id);
CREATE INDEX idx_perm_code ON rbac_permissions(code);
CREATE INDEX idx_perm_resource ON rbac_permissions(resource);

COMMENT ON TABLE rbac_permissions IS 'RBAC 권한 테이블';
COMMENT ON COLUMN rbac_permissions.permission_id IS '권한 ID (UUID)';
COMMENT ON COLUMN rbac_permissions.code IS '권한 코드 (예: user:create)';

-- =============================================================================
-- 5. RBAC 모듈 - 역할-권한 매핑 테이블
-- =============================================================================

CREATE TABLE rbac_role_permissions (
    -- 매핑 ID
    id                  BIGSERIAL       PRIMARY KEY,

    -- 외래 키
    role_id             VARCHAR(36)     NOT NULL,
    permission_id       VARCHAR(36)     NOT NULL,

    -- 감사 추적
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(36),

    -- 제약 조건
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id)
        REFERENCES rbac_roles(role_id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id)
        REFERENCES rbac_permissions(permission_id) ON DELETE CASCADE,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

-- 인덱스
CREATE INDEX idx_rp_role ON rbac_role_permissions(role_id);
CREATE INDEX idx_rp_permission ON rbac_role_permissions(permission_id);

COMMENT ON TABLE rbac_role_permissions IS '역할-권한 매핑 테이블';

-- =============================================================================
-- 6. RBAC 모듈 - 사용자-역할 매핑 테이블
-- =============================================================================

CREATE TABLE rbac_agent_roles (
    -- 매핑 ID
    id                  BIGSERIAL       PRIMARY KEY,

    -- 외래 키
    agent_id            VARCHAR(36)     NOT NULL,
    role_id             VARCHAR(36)     NOT NULL,

    -- 감사 추적
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(36),

    -- 제약 조건
    CONSTRAINT fk_ar_agent FOREIGN KEY (agent_id)
        REFERENCES user_agents(agent_id) ON DELETE CASCADE,
    CONSTRAINT fk_ar_role FOREIGN KEY (role_id)
        REFERENCES rbac_roles(role_id) ON DELETE CASCADE,
    CONSTRAINT uk_agent_role UNIQUE (agent_id, role_id)
);

-- 인덱스
CREATE INDEX idx_ar_agent ON rbac_agent_roles(agent_id);
CREATE INDEX idx_ar_role ON rbac_agent_roles(role_id);

COMMENT ON TABLE rbac_agent_roles IS '사용자-역할 매핑 테이블';

-- =============================================================================
-- 표준 데이터 삽입
-- =============================================================================

DO $$
DECLARE
    std_tenant VARCHAR(50) := 'default-tenant';

    -- 부서 ID (UUID 형식: 36자)
    company_id VARCHAR(36) := '00000000-0000-0000-0000-000000000001';
    dev_div_id VARCHAR(36) := '00000000-0000-0000-0000-000000000002';
    sales_div_id VARCHAR(36) := '00000000-0000-0000-0000-000000000003';
    backend_team_id VARCHAR(36) := '00000000-0000-0000-0000-000000000004';
    frontend_team_id VARCHAR(36) := '00000000-0000-0000-0000-000000000005';

    -- 사용자 ID (UUID 형식: 36자)
    admin_id VARCHAR(36) := '10000000-0000-0000-0000-000000000001';
    dev_lead_id VARCHAR(36) := '10000000-0000-0000-0000-000000000002';
    dev_member_id VARCHAR(36) := '10000000-0000-0000-0000-000000000003';

    -- 역할 ID (UUID 형식: 36자)
    admin_role_id VARCHAR(36) := '20000000-0000-0000-0000-000000000001';
    team_lead_role_id VARCHAR(36) := '20000000-0000-0000-0000-000000000002';
    member_role_id VARCHAR(36) := '20000000-0000-0000-0000-000000000003';

    -- 권한 ID (UUID 형식: 36자)
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
    -- =========================================================================
    -- 1. 조직(부서) 데이터
    -- =========================================================================

    -- 최상위: 넥스프론 (회사)
    INSERT INTO org_departments (dept_id, tenant_id, parent_id, name, type, code, org_path, depth, status, created_at, updated_at)
    VALUES (company_id, std_tenant, NULL, '넥스프론', 'COMPANY', 'NEXFRON', '/' || company_id, 0, 'ACTIVE', NOW(), NOW());

    -- 1단계: 개발본부
    INSERT INTO org_departments (dept_id, tenant_id, parent_id, name, type, code, org_path, depth, status, created_at, updated_at)
    VALUES (dev_div_id, std_tenant, company_id, '개발본부', 'DIVISION', 'DEV-DIV',
            '/' || company_id || '/' || dev_div_id, 1, 'ACTIVE', NOW(), NOW());

    -- 1단계: 영업본부
    INSERT INTO org_departments (dept_id, tenant_id, parent_id, name, type, code, org_path, depth, status, created_at, updated_at)
    VALUES (sales_div_id, std_tenant, company_id, '영업본부', 'DIVISION', 'SALES-DIV',
            '/' || company_id || '/' || sales_div_id, 1, 'ACTIVE', NOW(), NOW());

    -- 2단계: 백엔드팀
    INSERT INTO org_departments (dept_id, tenant_id, parent_id, name, type, code, org_path, depth, status, created_at, updated_at)
    VALUES (backend_team_id, std_tenant, dev_div_id, '백엔드팀', 'TEAM', 'DEV-BE',
            '/' || company_id || '/' || dev_div_id || '/' || backend_team_id, 2, 'ACTIVE', NOW(), NOW());

    -- 2단계: 프론트엔드팀
    INSERT INTO org_departments (dept_id, tenant_id, parent_id, name, type, code, org_path, depth, status, created_at, updated_at)
    VALUES (frontend_team_id, std_tenant, dev_div_id, '프론트엔드팀', 'TEAM', 'DEV-FE',
            '/' || company_id || '/' || dev_div_id || '/' || frontend_team_id, 2, 'ACTIVE', NOW(), NOW());

    -- =========================================================================
    -- 2. 사용자(Agent) 데이터
    -- =========================================================================

    -- 시스템 관리자
    INSERT INTO user_agents (agent_id, tenant_id, login_id, password, name, employee_id, email, dept_id, status, created_at, updated_at)
    VALUES (admin_id, std_tenant, 'admin',
            'jGl25bVBBBW96Qi9Te4V37Fnqchz/Eu4qB9vKrRIqRg=',
            '시스템관리자', 'EMP-0001', 'admin@nexfron.com', NULL, 'ACTIVE', NOW(), NOW());

    -- 개발 팀장
    INSERT INTO user_agents (agent_id, tenant_id, login_id, password, name, employee_id, email, dept_id, status, created_at, updated_at)
    VALUES (dev_lead_id, std_tenant, 'dev.lead',
            'jGl25bVBBBW96Qi9Te4V37Fnqchz/Eu4qB9vKrRIqRg=',
            '김팀장', 'EMP-0002', 'dev.lead@nexfron.com', backend_team_id, 'ACTIVE', NOW(), NOW());

    -- 개발자
    INSERT INTO user_agents (agent_id, tenant_id, login_id, password, name, employee_id, email, dept_id, status, created_at, updated_at)
    VALUES (dev_member_id, std_tenant, 'dev.member',
            'jGl25bVBBBW96Qi9Te4V37Fnqchz/Eu4qB9vKrRIqRg=',
            '이개발', 'EMP-0003', 'dev.member@nexfron.com', backend_team_id, 'ACTIVE', NOW(), NOW());

    -- =========================================================================
    -- 3. 역할(Role) 데이터
    -- =========================================================================

    INSERT INTO rbac_roles (role_id, tenant_id, name, type, data_scope_level, created_at, updated_at)
    VALUES
        (admin_role_id, std_tenant, 'ADMIN', 'POSITION', 'ALL', NOW(), NOW()),
        (team_lead_role_id, std_tenant, 'TEAM_LEAD', 'POSITION', 'DEPARTMENT', NOW(), NOW()),
        (member_role_id, std_tenant, 'MEMBER', 'POSITION', 'SELF', NOW(), NOW());

    -- =========================================================================
    -- 4. 권한(Permission) 데이터
    -- =========================================================================

    INSERT INTO rbac_permissions (permission_id, tenant_id, code, name, resource, action, created_at, updated_at)
    VALUES
        (perm_user_create, std_tenant, 'user:create', '사용자 생성', 'user', 'create', NOW(), NOW()),
        (perm_user_read, std_tenant, 'user:read', '사용자 조회', 'user', 'read', NOW(), NOW()),
        (perm_user_update, std_tenant, 'user:update', '사용자 수정', 'user', 'update', NOW(), NOW()),
        (perm_user_delete, std_tenant, 'user:delete', '사용자 삭제', 'user', 'delete', NOW(), NOW()),
        (perm_org_create, std_tenant, 'org:create', '조직 생성', 'organization', 'create', NOW(), NOW()),
        (perm_org_read, std_tenant, 'org:read', '조직 조회', 'organization', 'read', NOW(), NOW()),
        (perm_org_update, std_tenant, 'org:update', '조직 수정', 'organization', 'update', NOW(), NOW()),
        (perm_rbac_manage, std_tenant, 'rbac:manage', 'RBAC 관리', 'rbac', 'manage', NOW(), NOW()),
        (perm_report_view, std_tenant, 'report:view', '보고서 조회', 'report', 'view', NOW(), NOW()),
        (perm_report_export, std_tenant, 'report:export', '보고서 내보내기', 'report', 'export', NOW(), NOW());

    -- =========================================================================
    -- 5. 역할-권한 매핑
    -- =========================================================================

    -- ADMIN: 모든 권한
    INSERT INTO rbac_role_permissions (role_id, permission_id, created_at)
    SELECT admin_role_id, permission_id, NOW()
    FROM rbac_permissions
    WHERE tenant_id = std_tenant;

    -- TEAM_LEAD: 팀 관리 권한
    INSERT INTO rbac_role_permissions (role_id, permission_id, created_at)
    VALUES
        (team_lead_role_id, perm_user_read, NOW()),
        (team_lead_role_id, perm_user_update, NOW()),
        (team_lead_role_id, perm_org_read, NOW()),
        (team_lead_role_id, perm_report_view, NOW()),
        (team_lead_role_id, perm_report_export, NOW());

    -- MEMBER: 기본 권한
    INSERT INTO rbac_role_permissions (role_id, permission_id, created_at)
    VALUES
        (member_role_id, perm_user_read, NOW()),
        (member_role_id, perm_org_read, NOW()),
        (member_role_id, perm_report_view, NOW());

    -- =========================================================================
    -- 6. 사용자-역할 매핑
    -- =========================================================================

    INSERT INTO rbac_agent_roles (agent_id, role_id, created_at)
    VALUES
        (admin_id, admin_role_id, NOW()),
        (dev_lead_id, team_lead_role_id, NOW()),
        (dev_member_id, member_role_id, NOW());

END $$;

-- =============================================================================
-- 완료 메시지
-- =============================================================================

DO $$
BEGIN
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Identity Modulith Database Initialized Successfully!';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE '';
    RAISE NOTICE 'Created Tables:';
    RAISE NOTICE '  - org_departments: 5 departments';
    RAISE NOTICE '  - user_agents: 3 users';
    RAISE NOTICE '  - rbac_roles: 3 roles';
    RAISE NOTICE '  - rbac_permissions: 10 permissions';
    RAISE NOTICE '  - rbac_role_permissions: 18 mappings';
    RAISE NOTICE '  - rbac_agent_roles: 3 mappings';
    RAISE NOTICE '';
    RAISE NOTICE 'Test Accounts:';
    RAISE NOTICE '  - admin / admin123 (password)';
    RAISE NOTICE '  - dev.lead / admin123';
    RAISE NOTICE '  - dev.member / admin123';
    RAISE NOTICE '';
    RAISE NOTICE '=============================================================================';
END $$;

