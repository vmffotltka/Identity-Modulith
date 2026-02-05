-- ==============================================================================
-- V1_0_13__Add_DataScope_And_Insert_Standard_Roles.sql
--
-- 목적:
-- 1. roles 테이블에 data_scope 컬럼 추가
-- 2. RBAC_SCENARIOS 문서대로 기본 역할 8개 삽입
--    - POSITION 역할 3개: ADMIN, TEAM_LEAD, AGENT
--    - CHANNEL 역할 5개: VOICE_INBOUND, VOICE_OUTBOUND, CHAT, EMAIL, CALLBACK
-- 3. 기본 Permission 삽입 (agent, dept, role, permission, channel 관련)
-- 4. Role-Permission 매핑
--
-- 실행 조건:
-- - V1_0_0__Complete_Init.sql 실행 후
-- - roles 테이블 존재
-- - permissions 테이블 존재
-- ==============================================================================

-- ==============================================================================
-- 1. roles 테이블에 data_scope 컬럼 추가
-- ==============================================================================

-- data_scope 컬럼 추가 (POSITION 역할용)
ALTER TABLE roles ADD COLUMN IF NOT EXISTS data_scope VARCHAR(32);

-- 컬럼 설명 추가
COMMENT ON COLUMN roles.data_scope IS '데이터 접근 범위 (POSITION 역할만 사용: ADMIN, TEAM_LEAD, MEMBER)';

-- ==============================================================================
-- 2. 기존 데이터 정리 (Clean Slate)
-- ==============================================================================

-- role_permissions, agent_roles 먼저 삭제 (FK 제약 조건)
DELETE FROM role_permissions;
DELETE FROM agent_roles WHERE role_id IN (
    SELECT role_id FROM roles WHERE tenant_id = 'tenant-001'
);

-- 기존 역할 삭제
DELETE FROM roles WHERE tenant_id = 'tenant-001';

-- 기존 권한 삭제
DELETE FROM permissions WHERE tenant_id = 'tenant-001';

-- ==============================================================================
-- 3. 기본 역할 삽입 (POSITION 3개 + CHANNEL 5개)
-- ==============================================================================

INSERT INTO roles (role_id, tenant_id, name, type, data_scope, description, is_active, version, created_at, updated_at) VALUES
-- ========== POSITION 역할 (직급) ==========
(
    'r-00000000-0000-0000-0000-000000000001',
    'tenant-001',
    'ADMIN',
    'POSITION',
    'ADMIN',
    '시스템 관리자 - 전체 조직 접근 및 모든 관리 기능',
    true,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'r-00000000-0000-0000-0000-000000000002',
    'tenant-001',
    'TEAM_LEAD',
    'POSITION',
    'TEAM_LEAD',
    '팀장/부서장 - 본인 부서 및 하위 부서 접근',
    true,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'r-00000000-0000-0000-0000-000000000003',
    'tenant-001',
    'AGENT',
    'POSITION',
    'MEMBER',
    '일반 상담사 - 본인 부서만 접근',
    true,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

-- ========== CHANNEL 역할 (업무 채널) ==========
(
    'r-00000000-0000-0000-0000-000000000011',
    'tenant-001',
    'VOICE_INBOUND',
    'CHANNEL',
    NULL,
    '인바운드 전화 상담',
    true,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'r-00000000-0000-0000-0000-000000000012',
    'tenant-001',
    'VOICE_OUTBOUND',
    'CHANNEL',
    NULL,
    '아웃바운드 전화 상담',
    true,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'r-00000000-0000-0000-0000-000000000013',
    'tenant-001',
    'CHAT',
    'CHANNEL',
    NULL,
    '채팅 상담',
    true,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'r-00000000-0000-0000-0000-000000000014',
    'tenant-001',
    'EMAIL',
    'CHANNEL',
    NULL,
    '이메일 상담',
    true,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'r-00000000-0000-0000-0000-000000000015',
    'tenant-001',
    'CALLBACK',
    'CHANNEL',
    NULL,
    '콜백 관리',
    true,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 4. 기본 Permission 삽입
-- ==============================================================================

INSERT INTO permissions (permission_id, tenant_id, code, created_at) VALUES
-- ========== Agent 관련 권한 ==========
('p-00000000-0000-0000-0000-000000000001', 'tenant-001', 'agent:create', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000002', 'tenant-001', 'agent:read', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000003', 'tenant-001', 'agent:read:self', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000004', 'tenant-001', 'agent:update', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000005', 'tenant-001', 'agent:update:self', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000006', 'tenant-001', 'agent:delete', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000007', 'tenant-001', 'agent:suspend', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000008', 'tenant-001', 'agent:transfer', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000009', 'tenant-001', 'agent:role:assign', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000010', 'tenant-001', 'agent:password:reset', CURRENT_TIMESTAMP),

-- ========== Department 관련 권한 ==========
('p-00000000-0000-0000-0000-000000000011', 'tenant-001', 'dept:create', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000012', 'tenant-001', 'dept:read', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000013', 'tenant-001', 'dept:update', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000014', 'tenant-001', 'dept:delete', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000015', 'tenant-001', 'dept:move', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000016', 'tenant-001', 'dept:deactivate', CURRENT_TIMESTAMP),

-- ========== Role/Permission 관련 권한 ==========
('p-00000000-0000-0000-0000-000000000021', 'tenant-001', 'role:create', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000022', 'tenant-001', 'role:read', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000023', 'tenant-001', 'role:update', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000024', 'tenant-001', 'role:delete', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000025', 'tenant-001', 'permission:read', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000026', 'tenant-001', 'permission:assign', CURRENT_TIMESTAMP),

-- ========== Channel 업무 관련 권한 ==========
-- Voice (전화)
('p-00000000-0000-0000-0000-000000000031', 'tenant-001', 'call:receive', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000032', 'tenant-001', 'call:dial', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000033', 'tenant-001', 'call:transfer', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000034', 'tenant-001', 'call:hold', CURRENT_TIMESTAMP),

-- Chat (채팅)
('p-00000000-0000-0000-0000-000000000041', 'tenant-001', 'chat:receive', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000042', 'tenant-001', 'chat:send', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000043', 'tenant-001', 'chat:transfer', CURRENT_TIMESTAMP),

-- Email (이메일)
('p-00000000-0000-0000-0000-000000000051', 'tenant-001', 'email:receive', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000052', 'tenant-001', 'email:send', CURRENT_TIMESTAMP),

-- Callback (콜백)
('p-00000000-0000-0000-0000-000000000061', 'tenant-001', 'callback:create', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000062', 'tenant-001', 'callback:manage', CURRENT_TIMESTAMP),

-- ========== Report 관련 권한 ==========
('p-00000000-0000-0000-0000-000000000071', 'tenant-001', 'report:view', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000072', 'tenant-001', 'report:export', CURRENT_TIMESTAMP),
('p-00000000-0000-0000-0000-000000000073', 'tenant-001', 'report:create', CURRENT_TIMESTAMP);

-- ==============================================================================
-- 5. Role-Permission 매핑
-- ==============================================================================

-- ========== ADMIN 역할 권한 (모든 관리 권한) ==========
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
-- Agent 관리 (전체)
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP), -- agent:create
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP), -- agent:read
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000004', CURRENT_TIMESTAMP), -- agent:update
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000006', CURRENT_TIMESTAMP), -- agent:delete
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000007', CURRENT_TIMESTAMP), -- agent:suspend
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000008', CURRENT_TIMESTAMP), -- agent:transfer
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000009', CURRENT_TIMESTAMP), -- agent:role:assign
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000010', CURRENT_TIMESTAMP), -- agent:password:reset

-- Department 관리 (전체)
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000011', CURRENT_TIMESTAMP), -- dept:create
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000012', CURRENT_TIMESTAMP), -- dept:read
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000013', CURRENT_TIMESTAMP), -- dept:update
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000014', CURRENT_TIMESTAMP), -- dept:delete
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000015', CURRENT_TIMESTAMP), -- dept:move
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000016', CURRENT_TIMESTAMP), -- dept:deactivate

-- Role/Permission 관리 (전체)
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000021', CURRENT_TIMESTAMP), -- role:create
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000022', CURRENT_TIMESTAMP), -- role:read
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000023', CURRENT_TIMESTAMP), -- role:update
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000024', CURRENT_TIMESTAMP), -- role:delete
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000025', CURRENT_TIMESTAMP), -- permission:read
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000026', CURRENT_TIMESTAMP), -- permission:assign

-- Report 관리
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000071', CURRENT_TIMESTAMP), -- report:view
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000072', CURRENT_TIMESTAMP), -- report:export
('r-00000000-0000-0000-0000-000000000001', 'p-00000000-0000-0000-0000-000000000073', CURRENT_TIMESTAMP); -- report:create

-- ========== TEAM_LEAD 역할 권한 (팀 관리) ==========
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
-- Agent 조회/수정 (팀원만)
('r-00000000-0000-0000-0000-000000000002', 'p-00000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP), -- agent:read
('r-00000000-0000-0000-0000-000000000002', 'p-00000000-0000-0000-0000-000000000004', CURRENT_TIMESTAMP), -- agent:update
('r-00000000-0000-0000-0000-000000000002', 'p-00000000-0000-0000-0000-000000000007', CURRENT_TIMESTAMP), -- agent:suspend
('r-00000000-0000-0000-0000-000000000002', 'p-00000000-0000-0000-0000-000000000008', CURRENT_TIMESTAMP), -- agent:transfer
('r-00000000-0000-0000-0000-000000000002', 'p-00000000-0000-0000-0000-000000000010', CURRENT_TIMESTAMP), -- agent:password:reset

-- Department 조회
('r-00000000-0000-0000-0000-000000000002', 'p-00000000-0000-0000-0000-000000000012', CURRENT_TIMESTAMP), -- dept:read

-- Report 조회
('r-00000000-0000-0000-0000-000000000002', 'p-00000000-0000-0000-0000-000000000071', CURRENT_TIMESTAMP), -- report:view
('r-00000000-0000-0000-0000-000000000002', 'p-00000000-0000-0000-0000-000000000072', CURRENT_TIMESTAMP); -- report:export

-- ========== AGENT 역할 권한 (본인 정보만) ==========
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
-- 본인 정보만 조회/수정
('r-00000000-0000-0000-0000-000000000003', 'p-00000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP), -- agent:read:self
('r-00000000-0000-0000-0000-000000000003', 'p-00000000-0000-0000-0000-000000000005', CURRENT_TIMESTAMP); -- agent:update:self

-- ========== VOICE_INBOUND 역할 권한 ==========
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
('r-00000000-0000-0000-0000-000000000011', 'p-00000000-0000-0000-0000-000000000031', CURRENT_TIMESTAMP), -- call:receive
('r-00000000-0000-0000-0000-000000000011', 'p-00000000-0000-0000-0000-000000000033', CURRENT_TIMESTAMP), -- call:transfer
('r-00000000-0000-0000-0000-000000000011', 'p-00000000-0000-0000-0000-000000000034', CURRENT_TIMESTAMP); -- call:hold

-- ========== VOICE_OUTBOUND 역할 권한 ==========
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
('r-00000000-0000-0000-0000-000000000012', 'p-00000000-0000-0000-0000-000000000032', CURRENT_TIMESTAMP), -- call:dial
('r-00000000-0000-0000-0000-000000000012', 'p-00000000-0000-0000-0000-000000000031', CURRENT_TIMESTAMP), -- call:receive
('r-00000000-0000-0000-0000-000000000012', 'p-00000000-0000-0000-0000-000000000033', CURRENT_TIMESTAMP), -- call:transfer
('r-00000000-0000-0000-0000-000000000012', 'p-00000000-0000-0000-0000-000000000034', CURRENT_TIMESTAMP), -- call:hold
('r-00000000-0000-0000-0000-000000000012', 'p-00000000-0000-0000-0000-000000000061', CURRENT_TIMESTAMP); -- callback:create

-- ========== CHAT 역할 권한 ==========
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
('r-00000000-0000-0000-0000-000000000013', 'p-00000000-0000-0000-0000-000000000041', CURRENT_TIMESTAMP), -- chat:receive
('r-00000000-0000-0000-0000-000000000013', 'p-00000000-0000-0000-0000-000000000042', CURRENT_TIMESTAMP), -- chat:send
('r-00000000-0000-0000-0000-000000000013', 'p-00000000-0000-0000-0000-000000000043', CURRENT_TIMESTAMP); -- chat:transfer

-- ========== EMAIL 역할 권한 ==========
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
('r-00000000-0000-0000-0000-000000000014', 'p-00000000-0000-0000-0000-000000000051', CURRENT_TIMESTAMP), -- email:receive
('r-00000000-0000-0000-0000-000000000014', 'p-00000000-0000-0000-0000-000000000052', CURRENT_TIMESTAMP); -- email:send

-- ========== CALLBACK 역할 권한 ==========
INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES
('r-00000000-0000-0000-0000-000000000015', 'p-00000000-0000-0000-0000-000000000061', CURRENT_TIMESTAMP), -- callback:create
('r-00000000-0000-0000-0000-000000000015', 'p-00000000-0000-0000-0000-000000000062', CURRENT_TIMESTAMP); -- callback:manage

-- ==============================================================================
-- 6. 확인 쿼리
-- ==============================================================================

SELECT '✅ RBAC 기본 데이터 삽입 완료!' as status;

SELECT
    'roles' as table_name,
    COUNT(*) as row_count,
    STRING_AGG(name, ', ') as role_names
FROM roles
WHERE tenant_id = 'tenant-001'
UNION ALL
SELECT
    'permissions',
    COUNT(*),
    COUNT(*)::text || '개 권한'
FROM permissions
WHERE tenant_id = 'tenant-001'
UNION ALL
SELECT
    'role_permissions',
    COUNT(*),
    COUNT(*)::text || '개 매핑'
FROM role_permissions;
