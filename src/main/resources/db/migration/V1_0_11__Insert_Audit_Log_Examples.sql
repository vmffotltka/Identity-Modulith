-- ============================================================
-- V1_0_11: 감사 로그 예시 데이터 삽입
-- 날짜: 2026-01-15
-- DB: PostgreSQL
-- 목적: audit_logs와 audit_logs_archive에 현실적인 예시 데이터 추가
--       팀원들이 감사 로그 구조를 이해하도록 지원
-- ============================================================

-- ============================================================
-- Phase 1: 활성 감사 로그 예시 데이터 (audit_logs)
-- ============================================================

-- 1. 권한 생성 로그
INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'CREATE', 'PERMISSION', 'user:create', 'admin-user',
     '{"code": "user:create", "created_at": "2026-01-15T10:00:00Z"}',
     CURRENT_TIMESTAMP - INTERVAL '10 days', '사용자 생성 권한 추가', '192.168.1.100');

INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'CREATE', 'PERMISSION', 'org:read', 'admin-user',
     '{"code": "org:read", "created_at": "2026-01-15T10:05:00Z"}',
     CURRENT_TIMESTAMP - INTERVAL '10 days', '조직 조회 권한 추가', '192.168.1.100');

-- 2. 역할 생성 로그
INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'CREATE', 'ROLE',
     (SELECT role_id FROM roles WHERE name='ADMIN' AND tenant_id='tenant-001' LIMIT 1),
     'admin-user',
     '{"name": "ADMIN", "type": "POSITION", "description": "시스템 관리자", "is_active": true}',
     CURRENT_TIMESTAMP - INTERVAL '9 days', '관리자 역할 생성', '192.168.1.100');

INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'CREATE', 'ROLE',
     (SELECT role_id FROM roles WHERE name='TEAM_LEADER' AND tenant_id='tenant-001' LIMIT 1),
     'admin-user',
     '{"name": "TEAM_LEADER", "type": "POSITION", "description": "팀 리더", "is_active": true}',
     CURRENT_TIMESTAMP - INTERVAL '9 days', '팀리더 역할 생성', '192.168.1.100');

-- 3. 역할-권한 할당 로그
INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'ASSIGN', 'ROLE_PERMISSION',
     (SELECT role_id FROM roles WHERE name='ADMIN' AND tenant_id='tenant-001' LIMIT 1),
     'admin-user',
     '{"role": "ADMIN", "permission": "user:create", "action": "assigned"}',
     CURRENT_TIMESTAMP - INTERVAL '8 days', 'ADMIN 역할에 user:create 권한 할당', '192.168.1.100');

INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'ASSIGN', 'ROLE_PERMISSION',
     (SELECT role_id FROM roles WHERE name='TEAM_LEADER' AND tenant_id='tenant-001' LIMIT 1),
     'admin-user',
     '{"role": "TEAM_LEADER", "permission": "user:read", "action": "assigned"}',
     CURRENT_TIMESTAMP - INTERVAL '8 days', 'TEAM_LEADER 역할에 user:read 권한 할당', '192.168.1.100');

-- 4. 사용자 생성 로그
INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'CREATE', 'AGENT',
     (SELECT agent_id FROM agents WHERE login_id='admin-user' AND tenant_id='tenant-001' LIMIT 1),
     'admin-user',
     '{"login_id": "admin-user", "name": "관리자", "dept_id": "본사", "status": "ACTIVE"}',
     CURRENT_TIMESTAMP - INTERVAL '7 days', '관리자 사용자 계정 생성', '192.168.1.100');

INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'CREATE', 'AGENT',
     (SELECT agent_id FROM agents WHERE login_id='team-leader-01' AND tenant_id='tenant-001' LIMIT 1),
     'admin-user',
     '{"login_id": "team-leader-01", "name": "팀장 01", "dept_id": "영업부", "status": "ACTIVE"}',
     CURRENT_TIMESTAMP - INTERVAL '7 days', '팀장 계정 생성', '192.168.1.100');

-- 5. 사용자-역할 할당 로그
INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'ASSIGN', 'AGENT_ROLE',
     (SELECT agent_id FROM agents WHERE login_id='admin-user' AND tenant_id='tenant-001' LIMIT 1),
     'admin-user',
     '{"agent": "admin-user", "role": "ADMIN", "action": "assigned"}',
     CURRENT_TIMESTAMP - INTERVAL '6 days', '관리자 사용자에게 ADMIN 역할 할당', '192.168.1.100');

INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'ASSIGN', 'AGENT_ROLE',
     (SELECT agent_id FROM agents WHERE login_id='team-leader-01' AND tenant_id='tenant-001' LIMIT 1),
     'admin-user',
     '{"agent": "team-leader-01", "role": "TEAM_LEADER", "action": "assigned"}',
     CURRENT_TIMESTAMP - INTERVAL '6 days', '팀장에게 TEAM_LEADER 역할 할당', '192.168.1.100');

-- 6. 부서 생성 로그
INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'CREATE', 'DEPARTMENT',
     (SELECT dept_id FROM departments WHERE name='영업부' AND tenant_id='tenant-001' LIMIT 1),
     'admin-user',
     '{"name": "영업부", "parent": "본사", "org_path": "/001/002"}',
     CURRENT_TIMESTAMP - INTERVAL '5 days', '영업부 부서 생성', '192.168.1.100');

-- 7. 권한 업데이트 로그 (비활성화)
INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'UPDATE', 'ROLE',
     (SELECT role_id FROM roles WHERE name='MEMBER' AND tenant_id='tenant-001' LIMIT 1),
     'admin-user',
     '{"field": "is_active", "old_value": true, "new_value": false, "reason": "권한 조정 중"}',
     CURRENT_TIMESTAMP - INTERVAL '4 days', 'MEMBER 역할 비활성화', '192.168.1.100');

-- 8. 권한 회수 로그
INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'REVOKE', 'ROLE_PERMISSION',
     (SELECT role_id FROM roles WHERE name='MEMBER' AND tenant_id='tenant-001' LIMIT 1),
     'admin-user',
     '{"role": "MEMBER", "permission": "user:delete", "action": "revoked"}',
     CURRENT_TIMESTAMP - INTERVAL '3 days', 'MEMBER 역할에서 user:delete 권한 회수', '192.168.1.100');

-- 9. 역할 활성화 로그
INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'UPDATE', 'ROLE',
     (SELECT role_id FROM roles WHERE name='MEMBER' AND tenant_id='tenant-001' LIMIT 1),
     'admin-user',
     '{"field": "is_active", "old_value": false, "new_value": true}',
     CURRENT_TIMESTAMP - INTERVAL '2 days', 'MEMBER 역할 다시 활성화', '192.168.1.100');

-- 10. 가장 최근 로그 (어제)
INSERT INTO audit_logs (audit_id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, remarks, ip_address)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'ASSIGN', 'AGENT_ROLE',
     (SELECT agent_id FROM agents WHERE login_id='phone-agent-01' AND tenant_id='tenant-001' LIMIT 1),
     'team-leader-01',
     '{"agent": "phone-agent-01", "role": "PHONE_AGENT", "action": "assigned"}',
     CURRENT_TIMESTAMP - INTERVAL '1 day', '전화 상담사에게 PHONE_AGENT 역할 할당', '192.168.1.50');

-- ============================================================
-- Phase 2: 아카이브 감사 로그 예시 데이터 (audit_logs_archive)
-- ============================================================
-- 6개월 이상 된 로그를 아카이브로 표시

-- 1. 초기 시스템 설정 로그 (6개월 전)
INSERT INTO audit_logs_archive (id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, archived_at)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'CREATE', 'PERMISSION', 'system:init', 'system',
     '{"type": "system_initialization", "version": "1.0"}',
     CURRENT_TIMESTAMP - INTERVAL '200 days', CURRENT_TIMESTAMP - INTERVAL '10 days');

-- 2. 초기 역할 생성 로그 (6개월 전)
INSERT INTO audit_logs_archive (id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, archived_at)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'CREATE', 'ROLE', 'ADMIN', 'system',
     '{"name": "ADMIN", "type": "POSITION", "version": "1.0"}',
     CURRENT_TIMESTAMP - INTERVAL '195 days', CURRENT_TIMESTAMP - INTERVAL '10 days');

-- 3. 초기 사용자 생성 로그 (6개월 전)
INSERT INTO audit_logs_archive (id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, archived_at)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'CREATE', 'AGENT', 'admin-initial', 'system',
     '{"login_id": "admin-initial", "name": "초기 관리자"}',
     CURRENT_TIMESTAMP - INTERVAL '190 days', CURRENT_TIMESTAMP - INTERVAL '10 days');

-- 4. 3개월 전 권한 변경 로그
INSERT INTO audit_logs_archive (id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, archived_at)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'UPDATE', 'ROLE', 'ROLE-001', 'admin',
     '{"field": "description", "old_value": "old description", "new_value": "새로운 설명"}',
     CURRENT_TIMESTAMP - INTERVAL '120 days', CURRENT_TIMESTAMP - INTERVAL '10 days');

-- 5. 2개월 전 사용자 회수 로그
INSERT INTO audit_logs_archive (id, tenant_id, action, resource_type, resource_id, operator_id, changes, timestamp, archived_at)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'REVOKE', 'AGENT_ROLE', 'agent-123', 'admin',
     '{"agent": "agent-123", "role": "OLD_ROLE", "action": "revoked"}',
     CURRENT_TIMESTAMP - INTERVAL '70 days', CURRENT_TIMESTAMP - INTERVAL '10 days');

-- ============================================================
-- Phase 3: 마이그레이션 완료 및 통계
-- ============================================================

SELECT '✅ V1_0_11: 감사 로그 예시 데이터 삽입 완료!' as result;

-- 감사 로그 통계
SELECT
    '📊 감사 로그 통계' as category,
    (SELECT COUNT(*) FROM audit_logs WHERE tenant_id = 'tenant-001') as active_audit_count,
    (SELECT COUNT(*) FROM audit_logs_archive WHERE tenant_id = 'tenant-001') as archived_audit_count,
    (SELECT COUNT(DISTINCT action) FROM audit_logs WHERE tenant_id = 'tenant-001') as action_types,
    (SELECT COUNT(DISTINCT resource_type) FROM audit_logs WHERE tenant_id = 'tenant-001') as resource_types;

-- 감사 로그 액션 타입별 통계
SELECT
    action,
    COUNT(*) as count,
    COUNT(DISTINCT resource_type) as resource_types
FROM audit_logs
WHERE tenant_id = 'tenant-001'
GROUP BY action
ORDER BY count DESC;

-- 감사 로그 리소스 타입별 통계
SELECT
    resource_type,
    COUNT(*) as count,
    COUNT(DISTINCT action) as action_types
FROM audit_logs
WHERE tenant_id = 'tenant-001'
GROUP BY resource_type
ORDER BY count DESC;

