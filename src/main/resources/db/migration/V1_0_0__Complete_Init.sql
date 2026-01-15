-- ============================================================
-- Identity Modulith 데이터베이스 통합 초기화 스크립트
-- 버전: FINAL (PostgreSQL 정확 문법)
-- 날짜: 2026-01-14
-- DB: PostgreSQL
-- 설명: RBAC, Organization, User 모듈의 전체 데이터 초기화 및 표준 데이터 삽입
-- ============================================================

-- ============================================================
-- Phase 1: 전체 데이터 정리 (존재하면 삭제)
-- ============================================================

DROP TABLE IF EXISTS agent_roles CASCADE;
DROP TABLE IF EXISTS role_permissions CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;
DROP TABLE IF EXISTS agents CASCADE;
DROP TABLE IF EXISTS departments CASCADE;

-- ============================================================
-- Phase 2: 모든 테이블 생성 (UUID 기반)
-- ============================================================

-- Departments 테이블 (조직 구조)
CREATE TABLE departments (
    dept_id VARCHAR(36) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    parent_id VARCHAR(36),
    name VARCHAR(100) NOT NULL,
    org_path VARCHAR(500) NOT NULL,
    depth INTEGER NOT NULL,
    type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE departments IS '조직 (부서) 관리 테이블';
COMMENT ON COLUMN departments.dept_id IS '부서 ID (UUID)';
COMMENT ON COLUMN departments.tenant_id IS '테넌트 ID (멀티테넌시)';
COMMENT ON COLUMN departments.parent_id IS '상위 부서 ID (자기참조)';
COMMENT ON COLUMN departments.name IS '부서명';
COMMENT ON COLUMN departments.org_path IS '조직 경로 (트리 탐색용)';
COMMENT ON COLUMN departments.depth IS '트리 깊이';
COMMENT ON COLUMN departments.type IS '부서 타입';
COMMENT ON COLUMN departments.created_at IS '생성 일시';

CREATE UNIQUE INDEX IF NOT EXISTS uk_departments_tenant_path ON departments(tenant_id, org_path);
CREATE INDEX IF NOT EXISTS idx_departments_tenant_id ON departments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_departments_parent_id ON departments(parent_id);
CREATE INDEX IF NOT EXISTS idx_departments_org_path ON departments(org_path);

-- 자기참조 FK (부모-자식)
ALTER TABLE departments ADD CONSTRAINT fk_departments_parent
    FOREIGN KEY (parent_id) REFERENCES departments(dept_id) ON DELETE RESTRICT;

-- Agents 테이블 (사용자)
CREATE TABLE agents (
    agent_id VARCHAR(36) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    login_id VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    dept_id VARCHAR(36),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    password_must_change BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    retired_at TIMESTAMP,
    job_title VARCHAR(100),
    sync_status VARCHAR(20),
    role_id VARCHAR(50)
);

COMMENT ON TABLE agents IS '사용자 (에이전트) 관리 테이블';
COMMENT ON COLUMN agents.agent_id IS '사용자 ID (UUID)';
COMMENT ON COLUMN agents.tenant_id IS '테넌트 ID (멀티테넌시)';
COMMENT ON COLUMN agents.login_id IS '로그인 ID';
COMMENT ON COLUMN agents.password IS '비밀번호 (BCrypt)';
COMMENT ON COLUMN agents.name IS '사용자명';
COMMENT ON COLUMN agents.dept_id IS '소속 부서 ID (UUID, FK)';
COMMENT ON COLUMN agents.status IS '상태 (ACTIVE, RETIRED)';
COMMENT ON COLUMN agents.password_must_change IS '비밀번호 변경 필요 여부';
COMMENT ON COLUMN agents.created_at IS '생성 일시';
COMMENT ON COLUMN agents.retired_at IS '퇴직 일시';

CREATE INDEX IF NOT EXISTS idx_agents_tenant_id ON agents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agents_dept_id ON agents(dept_id);
CREATE INDEX IF NOT EXISTS idx_agents_status ON agents(status);
CREATE INDEX IF NOT EXISTS idx_agents_login_id ON agents(login_id);

-- FK: agents.dept_id → departments.dept_id
ALTER TABLE agents ADD CONSTRAINT fk_agents_dept
    FOREIGN KEY (dept_id) REFERENCES departments(dept_id) ON DELETE SET NULL;

-- Permissions 테이블 (권한)
CREATE TABLE permissions (
    permission_id VARCHAR(36) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    code VARCHAR(128) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE permissions IS '권한 관리 테이블';
COMMENT ON COLUMN permissions.permission_id IS '권한 ID (UUID)';
COMMENT ON COLUMN permissions.tenant_id IS '테넌트 ID (멀티테넌시)';
COMMENT ON COLUMN permissions.code IS '권한 코드 (domain:action)';
COMMENT ON COLUMN permissions.created_at IS '생성 일시';

CREATE UNIQUE INDEX IF NOT EXISTS uk_permissions_tenant_code ON permissions(tenant_id, code);
CREATE INDEX IF NOT EXISTS idx_permissions_tenant_id ON permissions(tenant_id);

-- Roles 테이블 (역할)
CREATE TABLE roles (
    role_id VARCHAR(36) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    name VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE roles IS '역할 관리 테이블';
COMMENT ON COLUMN roles.role_id IS '역할 ID (UUID)';
COMMENT ON COLUMN roles.tenant_id IS '테넌트 ID (멀티테넌시)';
COMMENT ON COLUMN roles.name IS '역할명 (ADMIN, TEAM_LEADER 등)';
COMMENT ON COLUMN roles.type IS '역할 타입 (POSITION, CHANNEL, SKILL)';
COMMENT ON COLUMN roles.description IS '역할 설명 (목적 및 권한 범위)';
COMMENT ON COLUMN roles.is_active IS '활성화 상태 (true=활성, false=비활성/논리적 삭제)';
COMMENT ON COLUMN roles.version IS '낙관적 잠금 버전 (동시성 제어용)';
COMMENT ON COLUMN roles.created_at IS '생성 일시';
COMMENT ON COLUMN roles.updated_at IS '마지막 수정 일시';

CREATE UNIQUE INDEX IF NOT EXISTS uk_roles_tenant_name ON roles(tenant_id, name);
CREATE INDEX IF NOT EXISTS idx_roles_tenant_id ON roles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_roles_is_active ON roles(is_active);

-- Role-Permission 매핑 테이블
CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE role_permissions IS '역할-권한 매핑 테이블';
COMMENT ON COLUMN role_permissions.id IS '매핑 ID';
COMMENT ON COLUMN role_permissions.role_id IS '역할 ID (FK)';
COMMENT ON COLUMN role_permissions.permission_id IS '권한 ID (FK)';
COMMENT ON COLUMN role_permissions.assigned_at IS '할당 일시';

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_permissions ON role_permissions(role_id, permission_id);
ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_role
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE;
ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_permission
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE;

-- Agent-Role 매핑 테이블
CREATE TABLE agent_roles (
    id BIGSERIAL PRIMARY KEY,
    agent_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE agent_roles IS '사용자-역할 매핑 테이블 (다대다 관계)';
COMMENT ON COLUMN agent_roles.id IS '매핑 ID';
COMMENT ON COLUMN agent_roles.agent_id IS '사용자 ID (FK to agents)';
COMMENT ON COLUMN agent_roles.role_id IS '역할 ID (FK to roles)';
COMMENT ON COLUMN agent_roles.assigned_at IS '할당 일시';

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_roles ON agent_roles(agent_id, role_id);
CREATE INDEX IF NOT EXISTS idx_agent_roles_agent_id ON agent_roles(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_roles_role_id ON agent_roles(role_id);

ALTER TABLE agent_roles ADD CONSTRAINT fk_agent_roles_agent
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE;
ALTER TABLE agent_roles ADD CONSTRAINT fk_agent_roles_role
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE;

-- ============================================================
-- Phase 3: RBAC 표준 데이터 삽입 (35권한 + 8역할 + 77매핑)
-- ============================================================

-- Permissions (35개)
INSERT INTO permissions (permission_id, tenant_id, code, created_at) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'tenant-001', 'user:create', NOW()),
('550e8400-e29b-41d4-a716-446655440002', 'tenant-001', 'user:read', NOW()),
('550e8400-e29b-41d4-a716-446655440003', 'tenant-001', 'user:read:self', NOW()),
('550e8400-e29b-41d4-a716-446655440004', 'tenant-001', 'user:update', NOW()),
('550e8400-e29b-41d4-a716-446655440005', 'tenant-001', 'user:update:self', NOW()),
('550e8400-e29b-41d4-a716-446655440006', 'tenant-001', 'user:delete', NOW()),
('550e8400-e29b-41d4-a716-446655440007', 'tenant-001', 'user:manage', NOW()),
('550e8400-e29b-41d4-a716-446655440008', 'tenant-001', 'user:assign:role', NOW()),
('550e8400-e29b-41d4-a716-446655440009', 'tenant-001', 'user:reset:password', NOW()),
('550e8400-e29b-41d4-a716-446655440010', 'tenant-001', 'org:view', NOW()),
('550e8400-e29b-41d4-a716-446655440011', 'tenant-001', 'org:create', NOW()),
('550e8400-e29b-41d4-a716-446655440012', 'tenant-001', 'org:update', NOW()),
('550e8400-e29b-41d4-a716-446655440013', 'tenant-001', 'org:move', NOW()),
('550e8400-e29b-41d4-a716-446655440014', 'tenant-001', 'org:delete', NOW()),
('550e8400-e29b-41d4-a716-446655440015', 'tenant-001', 'org:manage', NOW()),
('550e8400-e29b-41d4-a716-446655440016', 'tenant-001', 'rbac:view', NOW()),
('550e8400-e29b-41d4-a716-446655440017', 'tenant-001', 'rbac:create:role', NOW()),
('550e8400-e29b-41d4-a716-446655440018', 'tenant-001', 'rbac:update:role', NOW()),
('550e8400-e29b-41d4-a716-446655440019', 'tenant-001', 'rbac:delete:role', NOW()),
('550e8400-e29b-41d4-a716-446655440020', 'tenant-001', 'rbac:create:permission', NOW()),
('550e8400-e29b-41d4-a716-446655440021', 'tenant-001', 'rbac:update:permission', NOW()),
('550e8400-e29b-41d4-a716-446655440022', 'tenant-001', 'rbac:delete:permission', NOW()),
('550e8400-e29b-41d4-a716-446655440023', 'tenant-001', 'rbac:assign:permission', NOW()),
('550e8400-e29b-41d4-a716-446655440024', 'tenant-001', 'rbac:configure', NOW()),
('550e8400-e29b-41d4-a716-446655440025', 'tenant-001', 'report:view', NOW()),
('550e8400-e29b-41d4-a716-446655440026', 'tenant-001', 'report:read', NOW()),
('550e8400-e29b-41d4-a716-446655440027', 'tenant-001', 'report:export', NOW()),
('550e8400-e29b-41d4-a716-446655440028', 'tenant-001', 'report:manage', NOW()),
('550e8400-e29b-41d4-a716-446655440029', 'tenant-001', 'phone:accept', NOW()),
('550e8400-e29b-41d4-a716-446655440030', 'tenant-001', 'phone:hold', NOW()),
('550e8400-e29b-41d4-a716-446655440031', 'tenant-001', 'phone:transfer', NOW()),
('550e8400-e29b-41d4-a716-446655440032', 'tenant-001', 'chat:send', NOW()),
('550e8400-e29b-41d4-a716-446655440033', 'tenant-001', 'chat:read', NOW()),
('550e8400-e29b-41d4-a716-446655440034', 'tenant-001', 'email:send', NOW()),
('550e8400-e29b-41d4-a716-446655440035', 'tenant-001', 'queue:manage', NOW())
ON CONFLICT DO NOTHING;

-- Roles (8개)
INSERT INTO roles (role_id, tenant_id, name, type, created_at) VALUES
('660e8400-e29b-41d4-a716-446655440001', 'tenant-001', 'ADMIN', 'POSITION', NOW()),
('660e8400-e29b-41d4-a716-446655440002', 'tenant-001', 'MANAGER', 'POSITION', NOW()),
('660e8400-e29b-41d4-a716-446655440003', 'tenant-001', 'TEAM_LEAD', 'POSITION', NOW()),
('660e8400-e29b-41d4-a716-446655440004', 'tenant-001', 'MEMBER', 'POSITION', NOW()),
('660e8400-e29b-41d4-a716-446655440005', 'tenant-001', 'PHONE_AGENT', 'CHANNEL', NOW()),
('660e8400-e29b-41d4-a716-446655440006', 'tenant-001', 'CHAT_AGENT', 'CHANNEL', NOW()),
('660e8400-e29b-41d4-a716-446655440007', 'tenant-001', 'EMAIL_AGENT', 'CHANNEL', NOW()),
('660e8400-e29b-41d4-a716-446655440008', 'tenant-001', 'SUPERVISOR', 'CHANNEL', NOW())
ON CONFLICT DO NOTHING;

-- Role-Permission 매핑 (77개)
-- ADMIN: 35개 (전체)
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT '660e8400-e29b-41d4-a716-446655440001', permission_id, NOW()
FROM permissions WHERE tenant_id = 'tenant-001'
ON CONFLICT DO NOTHING;

-- MANAGER: 12개
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', NOW()),
('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440002', NOW()),
('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440004', NOW()),
('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440008', NOW()),
('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440009', NOW()),
('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440010', NOW()),
('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440011', NOW()),
('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440012', NOW()),
('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440013', NOW()),
('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440025', NOW()),
('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440026', NOW()),
('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440027', NOW())
ON CONFLICT DO NOTHING;

-- TEAM_LEAD: 5개
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
('660e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440002', NOW()),
('660e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440005', NOW()),
('660e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440010', NOW()),
('660e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440025', NOW()),
('660e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440026', NOW())
ON CONFLICT DO NOTHING;

-- MEMBER: 4개
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
('660e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440003', NOW()),
('660e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440005', NOW()),
('660e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440010', NOW()),
('660e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440025', NOW())
ON CONFLICT DO NOTHING;

-- PHONE_AGENT: 3개
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
('660e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440029', NOW()),
('660e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440030', NOW()),
('660e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440031', NOW())
ON CONFLICT DO NOTHING;

-- CHAT_AGENT: 2개
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
('660e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440032', NOW()),
('660e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440033', NOW())
ON CONFLICT DO NOTHING;

-- EMAIL_AGENT: 1개
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
('660e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440034', NOW())
ON CONFLICT DO NOTHING;

-- SUPERVISOR: 7개
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
('660e8400-e29b-41d4-a716-446655440008', '550e8400-e29b-41d4-a716-446655440029', NOW()),
('660e8400-e29b-41d4-a716-446655440008', '550e8400-e29b-41d4-a716-446655440030', NOW()),
('660e8400-e29b-41d4-a716-446655440008', '550e8400-e29b-41d4-a716-446655440031', NOW()),
('660e8400-e29b-41d4-a716-446655440008', '550e8400-e29b-41d4-a716-446655440032', NOW()),
('660e8400-e29b-41d4-a716-446655440008', '550e8400-e29b-41d4-a716-446655440033', NOW()),
('660e8400-e29b-41d4-a716-446655440008', '550e8400-e29b-41d4-a716-446655440034', NOW()),
('660e8400-e29b-41d4-a716-446655440008', '550e8400-e29b-41d4-a716-446655440035', NOW())
ON CONFLICT DO NOTHING;

-- ============================================================
-- Phase 4: Organization 표준 데이터 삽입 (13개 부서)
-- ============================================================

-- 최상위 조직
INSERT INTO departments (dept_id, tenant_id, parent_id, name, org_path, depth, type, created_at) VALUES
('d50e8400-e29b-41d4-a716-446655440001', 'tenant-001', NULL, '넥스프론 본부', '/d50e8400-e29b-41d4-a716-446655440001', 0, 'HEADQUARTERS', NOW())
ON CONFLICT DO NOTHING;

-- 1차 사업부
INSERT INTO departments (dept_id, tenant_id, parent_id, name, org_path, depth, type, created_at) VALUES
('d50e8400-e29b-41d4-a716-446655440002', 'tenant-001', 'd50e8400-e29b-41d4-a716-446655440001', '고객지원사업부', '/d50e8400-e29b-41d4-a716-446655440001/d50e8400-e29b-41d4-a716-446655440002', 1, 'DIVISION', NOW()),
('d50e8400-e29b-41d4-a716-446655440003', 'tenant-001', 'd50e8400-e29b-41d4-a716-446655440001', '영업사업부', '/d50e8400-e29b-41d4-a716-446655440001/d50e8400-e29b-41d4-a716-446655440003', 1, 'DIVISION', NOW()),
('d50e8400-e29b-41d4-a716-446655440004', 'tenant-001', 'd50e8400-e29b-41d4-a716-446655440001', '기술개발본부', '/d50e8400-e29b-41d4-a716-446655440001/d50e8400-e29b-41d4-a716-446655440004', 1, 'DIVISION', NOW())
ON CONFLICT DO NOTHING;

-- 2차 팀 - 고객지원사업부
INSERT INTO departments (dept_id, tenant_id, parent_id, name, org_path, depth, type, created_at) VALUES
('d50e8400-e29b-41d4-a716-446655440005', 'tenant-001', 'd50e8400-e29b-41d4-a716-446655440002', '전화상담팀', '/d50e8400-e29b-41d4-a716-446655440001/d50e8400-e29b-41d4-a716-446655440002/d50e8400-e29b-41d4-a716-446655440005', 2, 'TEAM', NOW()),
('d50e8400-e29b-41d4-a716-446655440006', 'tenant-001', 'd50e8400-e29b-41d4-a716-446655440002', '채팅상담팀', '/d50e8400-e29b-41d4-a716-446655440001/d50e8400-e29b-41d4-a716-446655440002/d50e8400-e29b-41d4-a716-446655440006', 2, 'TEAM', NOW()),
('d50e8400-e29b-41d4-a716-446655440007', 'tenant-001', 'd50e8400-e29b-41d4-a716-446655440002', '이메일상담팀', '/d50e8400-e29b-41d4-a716-446655440001/d50e8400-e29b-41d4-a716-446655440002/d50e8400-e29b-41d4-a716-446655440007', 2, 'TEAM', NOW()),
('d50e8400-e29b-41d4-a716-446655440008', 'tenant-001', 'd50e8400-e29b-41d4-a716-446655440002', 'VIP고객지원팀', '/d50e8400-e29b-41d4-a716-446655440001/d50e8400-e29b-41d4-a716-446655440002/d50e8400-e29b-41d4-a716-446655440008', 2, 'TEAM', NOW())
ON CONFLICT DO NOTHING;

-- 2차 팀 - 영업사업부
INSERT INTO departments (dept_id, tenant_id, parent_id, name, org_path, depth, type, created_at) VALUES
('d50e8400-e29b-41d4-a716-446655440009', 'tenant-001', 'd50e8400-e29b-41d4-a716-446655440003', '기업영업팀', '/d50e8400-e29b-41d4-a716-446655440001/d50e8400-e29b-41d4-a716-446655440003/d50e8400-e29b-41d4-a716-446655440009', 2, 'TEAM', NOW()),
('d50e8400-e29b-41d4-a716-446655440010', 'tenant-001', 'd50e8400-e29b-41d4-a716-446655440003', '소비자영업팀', '/d50e8400-e29b-41d4-a716-446655440001/d50e8400-e29b-41d4-a716-446655440003/d50e8400-e29b-41d4-a716-446655440010', 2, 'TEAM', NOW())
ON CONFLICT DO NOTHING;

-- 2차 팀 - 기술개발본부
INSERT INTO departments (dept_id, tenant_id, parent_id, name, org_path, depth, type, created_at) VALUES
('d50e8400-e29b-41d4-a716-446655440011', 'tenant-001', 'd50e8400-e29b-41d4-a716-446655440004', 'Backend개발팀', '/d50e8400-e29b-41d4-a716-446655440001/d50e8400-e29b-41d4-a716-446655440004/d50e8400-e29b-41d4-a716-446655440011', 2, 'TEAM', NOW()),
('d50e8400-e29b-41d4-a716-446655440012', 'tenant-001', 'd50e8400-e29b-41d4-a716-446655440004', 'Frontend개발팀', '/d50e8400-e29b-41d4-a716-446655440001/d50e8400-e29b-41d4-a716-446655440004/d50e8400-e29b-41d4-a716-446655440012', 2, 'TEAM', NOW()),
('d50e8400-e29b-41d4-a716-446655440013', 'tenant-001', 'd50e8400-e29b-41d4-a716-446655440004', 'DevOps팀', '/d50e8400-e29b-41d4-a716-446655440001/d50e8400-e29b-41d4-a716-446655440004/d50e8400-e29b-41d4-a716-446655440013', 2, 'TEAM', NOW())
ON CONFLICT DO NOTHING;

-- ============================================================
-- Phase 5: User 표준 데이터 삽입 (16명)
-- ============================================================

-- 관리자
INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at, retired_at) VALUES
('550e8400-e29b-41d4-a716-446655440101', 'tenant-001', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '시스템관리자', 'd50e8400-e29b-41d4-a716-446655440001', 'ACTIVE', false, NOW(), NULL),
('550e8400-e29b-41d4-a716-446655440102', 'tenant-001', 'manager01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '김부장', 'd50e8400-e29b-41d4-a716-446655440002', 'ACTIVE', false, NOW(), NULL)
ON CONFLICT DO NOTHING;

-- 전화상담팀
INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at, retired_at) VALUES
('550e8400-e29b-41d4-a716-446655440103', 'tenant-001', 'phone_supervisor', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '이팀장', 'd50e8400-e29b-41d4-a716-446655440005', 'ACTIVE', false, NOW(), NULL),
('550e8400-e29b-41d4-a716-446655440104', 'tenant-001', 'phone_agent01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '박상담', 'd50e8400-e29b-41d4-a716-446655440005', 'ACTIVE', true, NOW(), NULL),
('550e8400-e29b-41d4-a716-446655440105', 'tenant-001', 'phone_agent02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '최상담', 'd50e8400-e29b-41d4-a716-446655440005', 'ACTIVE', true, NOW(), NULL)
ON CONFLICT DO NOTHING;

-- 채팅상담팀
INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at, retired_at) VALUES
('550e8400-e29b-41d4-a716-446655440106', 'tenant-001', 'chat_agent01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '정상담', 'd50e8400-e29b-41d4-a716-446655440006', 'ACTIVE', true, NOW(), NULL),
('550e8400-e29b-41d4-a716-446655440107', 'tenant-001', 'chat_agent02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '강상담', 'd50e8400-e29b-41d4-a716-446655440006', 'ACTIVE', true, NOW(), NULL)
ON CONFLICT DO NOTHING;

-- 이메일상담팀
INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at, retired_at) VALUES
('550e8400-e29b-41d4-a716-446655440108', 'tenant-001', 'email_agent01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '한상담', 'd50e8400-e29b-41d4-a716-446655440007', 'ACTIVE', true, NOW(), NULL)
ON CONFLICT DO NOTHING;

-- VIP지원팀
INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at, retired_at) VALUES
('550e8400-e29b-41d4-a716-446655440109', 'tenant-001', 'vip_supervisor', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '윤팀장', 'd50e8400-e29b-41d4-a716-446655440008', 'ACTIVE', false, NOW(), NULL),
('550e8400-e29b-41d4-a716-446655440110', 'tenant-001', 'vip_agent01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '송상담', 'd50e8400-e29b-41d4-a716-446655440008', 'ACTIVE', true, NOW(), NULL)
ON CONFLICT DO NOTHING;

-- 영업팀
INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at, retired_at) VALUES
('550e8400-e29b-41d4-a716-446655440111', 'tenant-001', 'sales_manager', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '조부장', 'd50e8400-e29b-41d4-a716-446655440009', 'ACTIVE', false, NOW(), NULL),
('550e8400-e29b-41d4-a716-446655440112', 'tenant-001', 'sales_member01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '임사원', 'd50e8400-e29b-41d4-a716-446655440010', 'ACTIVE', true, NOW(), NULL)
ON CONFLICT DO NOTHING;

-- 개발팀
INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at, retired_at) VALUES
('550e8400-e29b-41d4-a716-446655440113', 'tenant-001', 'dev_lead', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '전팀장', 'd50e8400-e29b-41d4-a716-446655440011', 'ACTIVE', false, NOW(), NULL),
('550e8400-e29b-41d4-a716-446655440114', 'tenant-001', 'dev_member01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '신개발', 'd50e8400-e29b-41d4-a716-446655440011', 'ACTIVE', true, NOW(), NULL),
('550e8400-e29b-41d4-a716-446655440115', 'tenant-001', 'frontend_dev', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '유개발', 'd50e8400-e29b-41d4-a716-446655440012', 'ACTIVE', true, NOW(), NULL)
ON CONFLICT DO NOTHING;

-- 퇴직자
INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at, retired_at) VALUES
('550e8400-e29b-41d4-a716-446655440199', 'tenant-001', 'retired_user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '퇴직자', 'd50e8400-e29b-41d4-a716-446655440005', 'RETIRED', true, NOW() - INTERVAL '1 year', NOW() - INTERVAL '1 month')
ON CONFLICT DO NOTHING;

-- ============================================================
-- Phase 6: Agent-Role 매핑 (약 30개)
-- ============================================================

-- 관리자
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('550e8400-e29b-41d4-a716-446655440101', '660e8400-e29b-41d4-a716-446655440001', NOW())
ON CONFLICT DO NOTHING;

-- 부장급
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('550e8400-e29b-41d4-a716-446655440102', '660e8400-e29b-41d4-a716-446655440002', NOW())
ON CONFLICT DO NOTHING;

-- 팀장급
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('550e8400-e29b-41d4-a716-446655440103', '660e8400-e29b-41d4-a716-446655440003', NOW()),
('550e8400-e29b-41d4-a716-446655440103', '660e8400-e29b-41d4-a716-446655440008', NOW()),
('550e8400-e29b-41d4-a716-446655440109', '660e8400-e29b-41d4-a716-446655440003', NOW()),
('550e8400-e29b-41d4-a716-446655440109', '660e8400-e29b-41d4-a716-446655440008', NOW()),
('550e8400-e29b-41d4-a716-446655440111', '660e8400-e29b-41d4-a716-446655440002', NOW()),
('550e8400-e29b-41d4-a716-446655440113', '660e8400-e29b-41d4-a716-446655440003', NOW())
ON CONFLICT DO NOTHING;

-- 전화상담사
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('550e8400-e29b-41d4-a716-446655440104', '660e8400-e29b-41d4-a716-446655440004', NOW()),
('550e8400-e29b-41d4-a716-446655440104', '660e8400-e29b-41d4-a716-446655440005', NOW()),
('550e8400-e29b-41d4-a716-446655440105', '660e8400-e29b-41d4-a716-446655440004', NOW()),
('550e8400-e29b-41d4-a716-446655440105', '660e8400-e29b-41d4-a716-446655440005', NOW())
ON CONFLICT DO NOTHING;

-- 채팅상담사
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('550e8400-e29b-41d4-a716-446655440106', '660e8400-e29b-41d4-a716-446655440004', NOW()),
('550e8400-e29b-41d4-a716-446655440106', '660e8400-e29b-41d4-a716-446655440006', NOW()),
('550e8400-e29b-41d4-a716-446655440107', '660e8400-e29b-41d4-a716-446655440004', NOW()),
('550e8400-e29b-41d4-a716-446655440107', '660e8400-e29b-41d4-a716-446655440006', NOW())
ON CONFLICT DO NOTHING;

-- 이메일상담사
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('550e8400-e29b-41d4-a716-446655440108', '660e8400-e29b-41d4-a716-446655440004', NOW()),
('550e8400-e29b-41d4-a716-446655440108', '660e8400-e29b-41d4-a716-446655440007', NOW())
ON CONFLICT DO NOTHING;

-- VIP상담사
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('550e8400-e29b-41d4-a716-446655440110', '660e8400-e29b-41d4-a716-446655440004', NOW()),
('550e8400-e29b-41d4-a716-446655440110', '660e8400-e29b-41d4-a716-446655440005', NOW()),
('550e8400-e29b-41d4-a716-446655440110', '660e8400-e29b-41d4-a716-446655440006', NOW()),
('550e8400-e29b-41d4-a716-446655440110', '660e8400-e29b-41d4-a716-446655440007', NOW())
ON CONFLICT DO NOTHING;

-- 기타팀
INSERT INTO agent_roles (agent_id, role_id, assigned_at) VALUES
('550e8400-e29b-41d4-a716-446655440112', '660e8400-e29b-41d4-a716-446655440004', NOW()),
('550e8400-e29b-41d4-a716-446655440114', '660e8400-e29b-41d4-a716-446655440004', NOW()),
('550e8400-e29b-41d4-a716-446655440115', '660e8400-e29b-41d4-a716-446655440004', NOW())
ON CONFLICT DO NOTHING;

-- ============================================================
-- Phase 7: 검증 및 완료
-- ============================================================

SELECT '✅ 데이터베이스 초기화 완료!' as result;

SELECT
    'departments' as table_name,
    COUNT(*) as row_count
FROM departments
UNION ALL
SELECT 'agents', COUNT(*) FROM agents
UNION ALL
SELECT 'permissions', COUNT(*) FROM permissions
UNION ALL
SELECT 'roles', COUNT(*) FROM roles
UNION ALL
SELECT 'role_permissions', COUNT(*) FROM role_permissions
UNION ALL
SELECT 'agent_roles', COUNT(*) FROM agent_roles
ORDER BY table_name;

