-- ============================================================
-- V1_0_10: 표준 데이터 확장 (팀원들을 위한 예시 데이터)
-- 날짜: 2026-01-15
-- DB: PostgreSQL
-- 목적: RBAC, Organization 모듈의 상세한 표준 데이터 삽입
--       팀원들이 참고할 수 있는 예시 데이터 제공
-- ============================================================

-- ============================================================
-- Phase 1: 권한 데이터 확인 및 추가
-- ============================================================
-- 기존 권한 확인 (V1_0_9에서 삽입됨)
-- 추가 권한 정의
INSERT INTO permissions (permission_id, tenant_id, code, created_at)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'role:create', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'role:update', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'role:delete', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'permission:manage', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'agent:create', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'agent:update', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'agent:delete', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'department:create', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'department:update', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'department:delete', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'audit:view', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'audit:export', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'cdr:view', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'dashboard:view', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'quality:manage', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- ============================================================
-- Phase 2: 역할-권한 매핑 (권한 할당)
-- ============================================================

-- ADMIN 역할에 모든 권한 할당
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT
    r.role_id,
    p.permission_id,
    CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND r.tenant_id = 'tenant-001'
  AND p.tenant_id = 'tenant-001'
ON CONFLICT DO NOTHING;

-- TEAM_LEADER 역할 - 팀 관리 권한
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT
    r.role_id,
    p.permission_id,
    CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'TEAM_LEADER'
  AND r.tenant_id = 'tenant-001'
  AND p.tenant_id = 'tenant-001'
  AND p.code IN (
    'user:read', 'user:update',
    'org:read',
    'report:view', 'report:export',
    'audit:view',
    'cdr:view',
    'dashboard:view'
  )
ON CONFLICT DO NOTHING;

-- MEMBER 역할 - 기본 조회 권한
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT
    r.role_id,
    p.permission_id,
    CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MEMBER'
  AND r.tenant_id = 'tenant-001'
  AND p.tenant_id = 'tenant-001'
  AND p.code IN (
    'user:read',
    'org:read',
    'report:view',
    'cdr:view',
    'dashboard:view'
  )
ON CONFLICT DO NOTHING;

-- PHONE_AGENT 역할 - 전화 상담 권한
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT
    r.role_id,
    p.permission_id,
    CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'PHONE_AGENT'
  AND r.tenant_id = 'tenant-001'
  AND p.tenant_id = 'tenant-001'
  AND p.code IN (
    'user:read',
    'org:read',
    'report:view',
    'cdr:view',
    'dashboard:view'
  )
ON CONFLICT DO NOTHING;

-- CHAT_AGENT 역할 - 채팅 상담 권한
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT
    r.role_id,
    p.permission_id,
    CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'CHAT_AGENT'
  AND r.tenant_id = 'tenant-001'
  AND p.tenant_id = 'tenant-001'
  AND p.code IN (
    'user:read',
    'org:read',
    'report:view',
    'cdr:view',
    'dashboard:view'
  )
ON CONFLICT DO NOTHING;

-- EMAIL_AGENT 역할 - 이메일 상담 권한
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT
    r.role_id,
    p.permission_id,
    CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'EMAIL_AGENT'
  AND r.tenant_id = 'tenant-001'
  AND p.tenant_id = 'tenant-001'
  AND p.code IN (
    'user:read',
    'org:read',
    'report:view',
    'dashboard:view'
  )
ON CONFLICT DO NOTHING;

-- ============================================================
-- Phase 3: 권한 그룹-권한 매핑
-- ============================================================

-- USER_FULL_ACCESS 그룹에 사용자 관련 권한 추가
INSERT INTO permission_group_permissions (permission_group_id, permission_id, added_at)
SELECT
    pg.permission_group_id,
    p.permission_id,
    CURRENT_TIMESTAMP
FROM permission_groups pg
CROSS JOIN permissions p
WHERE pg.name = 'USER_FULL_ACCESS'
  AND pg.tenant_id = 'tenant-001'
  AND p.tenant_id = 'tenant-001'
  AND p.code IN (
    'user:create', 'user:read', 'user:update', 'user:delete',
    'agent:create', 'agent:update', 'agent:delete'
  )
ON CONFLICT DO NOTHING;

-- ORGANIZATION_FULL_ACCESS 그룹에 조직 관련 권한 추가
INSERT INTO permission_group_permissions (permission_group_id, permission_id, added_at)
SELECT
    pg.permission_group_id,
    p.permission_id,
    CURRENT_TIMESTAMP
FROM permission_groups pg
CROSS JOIN permissions p
WHERE pg.name = 'ORGANIZATION_FULL_ACCESS'
  AND pg.tenant_id = 'tenant-001'
  AND p.tenant_id = 'tenant-001'
  AND p.code IN (
    'org:create', 'org:read', 'org:update', 'org:delete',
    'department:create', 'department:update', 'department:delete'
  )
ON CONFLICT DO NOTHING;

-- REPORTING_ACCESS 그룹에 보고서 관련 권한 추가
INSERT INTO permission_group_permissions (permission_group_id, permission_id, added_at)
SELECT
    pg.permission_group_id,
    p.permission_id,
    CURRENT_TIMESTAMP
FROM permission_groups pg
CROSS JOIN permissions p
WHERE pg.name = 'REPORTING_ACCESS'
  AND pg.tenant_id = 'tenant-001'
  AND p.tenant_id = 'tenant-001'
  AND p.code IN (
    'report:view', 'report:export',
    'audit:view', 'audit:export',
    'cdr:view',
    'dashboard:view'
  )
ON CONFLICT DO NOTHING;

-- ============================================================
-- Phase 4: 역할-권한 그룹 매핑
-- ============================================================

-- ADMIN 역할에 모든 권한 그룹 할당
INSERT INTO role_permission_groups (role_id, permission_group_id, assigned_at)
SELECT
    r.role_id,
    pg.permission_group_id,
    CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permission_groups pg
WHERE r.name = 'ADMIN'
  AND r.tenant_id = 'tenant-001'
  AND pg.tenant_id = 'tenant-001'
ON CONFLICT DO NOTHING;

-- TEAM_LEADER 역할에 USER와 REPORTING 그룹 할당
INSERT INTO role_permission_groups (role_id, permission_group_id, assigned_at)
SELECT
    r.role_id,
    pg.permission_group_id,
    CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permission_groups pg
WHERE r.name = 'TEAM_LEADER'
  AND r.tenant_id = 'tenant-001'
  AND pg.tenant_id = 'tenant-001'
  AND pg.name IN ('USER_FULL_ACCESS', 'REPORTING_ACCESS')
ON CONFLICT DO NOTHING;

-- ============================================================
-- Phase 5: 예시 사용자 데이터 (선택사항)
-- ============================================================

-- 예시 사용자 생성 (암호는 실제로는 BCrypt 해시되어야 함)
-- 참고: login_id는 UNIQUE 제약이 있으므로 중복 주의
INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at)
SELECT
    gen_random_uuid()::VARCHAR(36),
    'tenant-001',
    'admin-user',
    '$2a$10$slYQmyNdGzin7olVN3p5HOpsvhjUefTWGQT1qfJiXlQ8DfXWa7j8G', -- password: admin123 (BCrypt)
    '관리자',
    d.dept_id,
    'ACTIVE',
    false,
    CURRENT_TIMESTAMP
FROM departments d
WHERE d.name = '본사' AND d.tenant_id = 'tenant-001'
AND NOT EXISTS (
    SELECT 1 FROM agents WHERE login_id = 'admin-user' AND tenant_id = 'tenant-001'
)
ON CONFLICT DO NOTHING;

INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at)
SELECT
    gen_random_uuid()::VARCHAR(36),
    'tenant-001',
    'team-leader-01',
    '$2a$10$slYQmyNdGzin7olVN3p5HOpsvhjUefTWGQT1qfJiXlQ8DfXWa7j8G', -- password: admin123 (BCrypt)
    '팀장 01',
    d.dept_id,
    'ACTIVE',
    false,
    CURRENT_TIMESTAMP
FROM departments d
WHERE d.name = '영업부' AND d.tenant_id = 'tenant-001'
AND NOT EXISTS (
    SELECT 1 FROM agents WHERE login_id = 'team-leader-01' AND tenant_id = 'tenant-001'
)
ON CONFLICT DO NOTHING;

INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at)
SELECT
    gen_random_uuid()::VARCHAR(36),
    'tenant-001',
    'phone-agent-01',
    '$2a$10$slYQmyNdGzin7olVN3p5HOpsvhjUefTWGQT1qfJiXlQ8DfXWa7j8G', -- password: admin123 (BCrypt)
    '전화 상담사 01',
    d.dept_id,
    'ACTIVE',
    false,
    CURRENT_TIMESTAMP
FROM departments d
WHERE d.name = '영업부' AND d.tenant_id = 'tenant-001'
AND NOT EXISTS (
    SELECT 1 FROM agents WHERE login_id = 'phone-agent-01' AND tenant_id = 'tenant-001'
)
ON CONFLICT DO NOTHING;

INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at)
SELECT
    gen_random_uuid()::VARCHAR(36),
    'tenant-001',
    'chat-agent-01',
    '$2a$10$slYQmyNdGzin7olVN3p5HOpsvhjUefTWGQT1qfJiXlQ8DfXWa7j8G', -- password: admin123 (BCrypt)
    '채팅 상담사 01',
    d.dept_id,
    'ACTIVE',
    false,
    CURRENT_TIMESTAMP
FROM departments d
WHERE d.name = '기술부' AND d.tenant_id = 'tenant-001'
AND NOT EXISTS (
    SELECT 1 FROM agents WHERE login_id = 'chat-agent-01' AND tenant_id = 'tenant-001'
)
ON CONFLICT DO NOTHING;

-- ============================================================
-- Phase 6: 사용자-역할 매핑
-- ============================================================

-- 관리자에게 ADMIN 역할 할당
INSERT INTO agent_roles (agent_id, role_id, assigned_at)
SELECT
    a.agent_id,
    r.role_id,
    CURRENT_TIMESTAMP
FROM agents a
CROSS JOIN roles r
WHERE a.login_id = 'admin-user'
  AND a.tenant_id = 'tenant-001'
  AND r.name = 'ADMIN'
  AND r.tenant_id = 'tenant-001'
ON CONFLICT DO NOTHING;

-- 팀장에게 TEAM_LEADER 역할 할당
INSERT INTO agent_roles (agent_id, role_id, assigned_at)
SELECT
    a.agent_id,
    r.role_id,
    CURRENT_TIMESTAMP
FROM agents a
CROSS JOIN roles r
WHERE a.login_id = 'team-leader-01'
  AND a.tenant_id = 'tenant-001'
  AND r.name = 'TEAM_LEADER'
  AND r.tenant_id = 'tenant-001'
ON CONFLICT DO NOTHING;

-- 전화 상담사에게 PHONE_AGENT 역할 할당
INSERT INTO agent_roles (agent_id, role_id, assigned_at)
SELECT
    a.agent_id,
    r.role_id,
    CURRENT_TIMESTAMP
FROM agents a
CROSS JOIN roles r
WHERE a.login_id = 'phone-agent-01'
  AND a.tenant_id = 'tenant-001'
  AND r.name = 'PHONE_AGENT'
  AND r.tenant_id = 'tenant-001'
ON CONFLICT DO NOTHING;

-- 채팅 상담사에게 CHAT_AGENT 역할 할당
INSERT INTO agent_roles (agent_id, role_id, assigned_at)
SELECT
    a.agent_id,
    r.role_id,
    CURRENT_TIMESTAMP
FROM agents a
CROSS JOIN roles r
WHERE a.login_id = 'chat-agent-01'
  AND a.tenant_id = 'tenant-001'
  AND r.name = 'CHAT_AGENT'
  AND r.tenant_id = 'tenant-001'
ON CONFLICT DO NOTHING;

-- ============================================================
-- Phase 7: 완료 메시지 및 통계
-- ============================================================

SELECT '✅ V1_0_10: 표준 데이터 확장 완료!' as result;

-- 삽입된 데이터 통계
SELECT
    '📊 데이터 통계' as category,
    (SELECT COUNT(*) FROM permissions WHERE tenant_id = 'tenant-001') as permission_count,
    (SELECT COUNT(*) FROM roles WHERE tenant_id = 'tenant-001') as role_count,
    (SELECT COUNT(*) FROM role_permissions) as role_permission_mapping,
    (SELECT COUNT(*) FROM permission_groups WHERE tenant_id = 'tenant-001') as permission_group_count,
    (SELECT COUNT(*) FROM permission_group_permissions) as group_permission_mapping,
    (SELECT COUNT(*) FROM role_permission_groups) as role_group_mapping,
    (SELECT COUNT(*) FROM departments WHERE tenant_id = 'tenant-001') as department_count,
    (SELECT COUNT(*) FROM agents WHERE tenant_id = 'tenant-001') as agent_count,
    (SELECT COUNT(*) FROM agent_roles) as agent_role_mapping;

