-- =====================================================
-- 비밀번호 해시 업데이트 (BCrypt)
-- 비밀번호: Admin123!
-- =====================================================

UPDATE user_agents
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm'
WHERE agent_id IN (
    '10000000-0000-0000-0000-000000000001',  -- admin
    '10000000-0000-0000-0000-000000000002',  -- dev.lead
    '10000000-0000-0000-0000-000000000003'   -- dev.member
);

-- 업데이트 확인
SELECT
    agent_id,
    login_id,
    name,
    LEFT(password, 20) || '...' as password_hash,
    CASE
        WHEN password = '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm'
        THEN '✅ 정상'
        ELSE '❌ 미업데이트'
    END as status
FROM user_agents
WHERE agent_id IN (
    '10000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000003'
)
ORDER BY login_id;

