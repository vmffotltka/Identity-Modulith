-- ============================================================
-- V1_0_9: RBAC 및 Organization 모듈 - 표준 데이터 삽입
-- 날짜: 2026-01-15
-- DB: PostgreSQL
-- 목적: V1_0_0 ~ V1_0_7 마이그레이션 완료 후 표준 데이터만 삽입
-- ============================================================

-- ============================================================
-- Phase 1: 표준 데이터 삽입 (초기 역할 및 권한)
-- ============================================================

-- 기본 권한 정의
INSERT INTO permissions (permission_id, tenant_id, code, created_at)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'user:create', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'user:read', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'user:update', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'user:delete', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'org:create', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'org:read', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'org:update', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'org:delete', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'report:view', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'report:export', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 기본 역할 정의
INSERT INTO roles (role_id, tenant_id, name, type, description, is_active, created_at, updated_at)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'ADMIN', 'POSITION', '시스템 전체를 관리하는 최고 관리자 권한', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'TEAM_LEADER', 'POSITION', '팀을 관리하고 팀원의 업무를 지원하는 권한', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'MEMBER', 'POSITION', '일반 구성원 권한', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'PHONE_AGENT', 'CHANNEL', '전화 상담을 수행하는 상담사 권한', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'CHAT_AGENT', 'CHANNEL', '채팅 상담을 수행하는 상담사 권한', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'EMAIL_AGENT', 'CHANNEL', '이메일 상담을 수행하는 상담사 권한', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 기본 권한 그룹
INSERT INTO permission_groups (permission_group_id, tenant_id, name, description, is_active, created_at, updated_at)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'USER_FULL_ACCESS', '사용자 생성, 조회, 수정, 삭제 권한', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'ORGANIZATION_FULL_ACCESS', '조직 생성, 조회, 수정, 삭제 권한', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'REPORTING_ACCESS', '보고서 조회 및 내보내기 권한', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 기본 부서 구조
INSERT INTO departments (dept_id, tenant_id, parent_id, name, org_path, depth, type, created_at)
VALUES
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', NULL, '본사', '/001', 1, 'COMPANY', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', (SELECT dept_id FROM departments WHERE name='본사' AND tenant_id='tenant-001' LIMIT 1), '영업부', '/001/002', 2, 'DEPARTMENT', CURRENT_TIMESTAMP),
    (gen_random_uuid()::VARCHAR(36), 'tenant-001', (SELECT dept_id FROM departments WHERE name='본사' AND tenant_id='tenant-001' LIMIT 1), '기술부', '/001/003', 2, 'DEPARTMENT', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 마이그레이션 완료
-- ============================================================

SELECT '✅ V1_0_9: 표준 데이터 삽입 완료!' as result;

