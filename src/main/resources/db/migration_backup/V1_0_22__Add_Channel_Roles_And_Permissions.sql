-- =============================================================================
-- V1_0_13: CHANNEL 타입 역할 및 권한 추가 (RR-003 완전 구현)
-- =============================================================================
-- 생성일: 2026-02-03
-- 목적: RBAC_SCENARIOS 문서에 명시된 채널 역할 추가
-- 참조: RBAC_SCENARIOS.md - RR-003 (채널 역할)
-- =============================================================================

-- =============================================================================
-- 1단계: 채널 관련 권한 추가
-- =============================================================================

-- 📞 인바운드 채널 권한
INSERT INTO permissions (permission_id, tenant_id, code, description, category, created_at)
VALUES
    ('perm-channel-inbound-001', 'tenant-001', 'channel:inbound:receive', '인바운드 호출 수신', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-channel-inbound-002', 'tenant-001', 'channel:inbound:transfer', '인바운드 호출 전달', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-channel-inbound-003', 'tenant-001', 'channel:inbound:hold', '인바운드 호출 대기', 'CHANNEL', CURRENT_TIMESTAMP);

-- 📱 아웃바운드 채널 권한
INSERT INTO permissions (permission_id, tenant_id, code, description, category, created_at)
VALUES
    ('perm-channel-outbound-001', 'tenant-001', 'channel:outbound:call', '아웃바운드 호출 발신', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-channel-outbound-002', 'tenant-001', 'channel:outbound:campaign', '캠페인 관리', 'CHANNEL', CURRENT_TIMESTAMP);

-- 💬 채팅 채널 권한
INSERT INTO permissions (permission_id, tenant_id, code, description, category, created_at)
VALUES
    ('perm-channel-chat-001', 'tenant-001', 'channel:chat:message', '채팅 메시지 송수신', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-channel-chat-002', 'tenant-001', 'channel:chat:file', '파일 전송', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-channel-chat-003', 'tenant-001', 'channel:chat:emoji', '이모티콘 사용', 'CHANNEL', CURRENT_TIMESTAMP);

-- 📧 이메일 채널 권한
INSERT INTO permissions (permission_id, tenant_id, code, description, category, created_at)
VALUES
    ('perm-channel-email-001', 'tenant-001', 'channel:email:send', '이메일 발송', 'CHANNEL', CURRENT_TIMESTAMP),
    ('perm-channel-email-002', 'tenant-001', 'channel:email:template', '템플릿 관리', 'CHANNEL', CURRENT_TIMESTAMP);

-- =============================================================================
-- 2단계: 채널 역할 추가 (RR-003)
-- =============================================================================

-- 📞 인바운드 상담사 역할
INSERT INTO roles (role_id, tenant_id, name, type, description, is_active, created_at)
VALUES
    ('role-channel-inbound-001', 'tenant-001', 'INBOUND_AGENT', 'CHANNEL',
     '인바운드 전화 상담 처리 (수신/대기/전달)', true, CURRENT_TIMESTAMP);

-- 📱 아웃바운드 상담사 역할
INSERT INTO roles (role_id, tenant_id, name, type, description, is_active, created_at)
VALUES
    ('role-channel-outbound-001', 'tenant-001', 'OUTBOUND_AGENT', 'CHANNEL',
     '아웃바운드 전화 발신 및 캠페인 수행', true, CURRENT_TIMESTAMP);

-- 💬 채팅 상담사 역할
INSERT INTO roles (role_id, tenant_id, name, type, description, is_active, created_at)
VALUES
    ('role-channel-chat-001', 'tenant-001', 'CHAT_AGENT', 'CHANNEL',
     '온라인 채팅 상담 처리 (메시지/파일/이모티콘)', true, CURRENT_TIMESTAMP);

-- 📧 이메일 상담사 역할
INSERT INTO roles (role_id, tenant_id, name, type, description, is_active, created_at)
VALUES
    ('role-channel-email-001', 'tenant-001', 'EMAIL_AGENT', 'CHANNEL',
     '이메일 상담 처리 및 템플릿 관리', true, CURRENT_TIMESTAMP);

-- 🎯 멀티채널 상담사 역할 (모든 채널 지원)
INSERT INTO roles (role_id, tenant_id, name, type, description, is_active, created_at)
VALUES
    ('role-channel-multi-001', 'tenant-001', 'MULTI_CHANNEL_AGENT', 'CHANNEL',
     '모든 채널 상담 가능 (인바운드/아웃바운드/채팅/이메일)', true, CURRENT_TIMESTAMP);

-- =============================================================================
-- 3단계: 역할-권한 매핑 (RolePermission)
-- =============================================================================

-- 📞 INBOUND_AGENT 권한 매핑
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
VALUES
    ('role-channel-inbound-001', 'perm-channel-inbound-001', CURRENT_TIMESTAMP),
    ('role-channel-inbound-001', 'perm-channel-inbound-002', CURRENT_TIMESTAMP),
    ('role-channel-inbound-001', 'perm-channel-inbound-003', CURRENT_TIMESTAMP);

-- 📱 OUTBOUND_AGENT 권한 매핑
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
VALUES
    ('role-channel-outbound-001', 'perm-channel-outbound-001', CURRENT_TIMESTAMP),
    ('role-channel-outbound-001', 'perm-channel-outbound-002', CURRENT_TIMESTAMP);

-- 💬 CHAT_AGENT 권한 매핑
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
VALUES
    ('role-channel-chat-001', 'perm-channel-chat-001', CURRENT_TIMESTAMP),
    ('role-channel-chat-001', 'perm-channel-chat-002', CURRENT_TIMESTAMP),
    ('role-channel-chat-001', 'perm-channel-chat-003', CURRENT_TIMESTAMP);

-- 📧 EMAIL_AGENT 권한 매핑
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
VALUES
    ('role-channel-email-001', 'perm-channel-email-001', CURRENT_TIMESTAMP),
    ('role-channel-email-001', 'perm-channel-email-002', CURRENT_TIMESTAMP);

-- 🎯 MULTI_CHANNEL_AGENT 권한 매핑 (모든 채널 권한 포함)
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT 'role-channel-multi-001', permission_id, CURRENT_TIMESTAMP
FROM permissions
WHERE category = 'CHANNEL';

-- =============================================================================
-- 4단계: 검증 쿼리 (실행 후 확인)
-- =============================================================================

-- ✅ 생성된 채널 역할 확인
SELECT '✅ 채널 역할 생성 완료' AS result;

SELECT
    r.name AS role_name,
    r.type AS role_type,
    r.description,
    COUNT(rp.permission_id) AS permission_count
FROM roles r
LEFT JOIN role_permissions rp ON r.role_id = rp.role_id
WHERE r.type = 'CHANNEL'
GROUP BY r.role_id, r.name, r.type, r.description
ORDER BY r.name;

-- ✅ 채널 권한 카테고리별 통계
SELECT
    'CHANNEL' AS category,
    COUNT(*) AS total_permissions
FROM permissions
WHERE category = 'CHANNEL';

-- =============================================================================
-- 마이그레이션 완료
-- =============================================================================
