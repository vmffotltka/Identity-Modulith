-- 현재 비밀번호 해시 확인
SELECT
    agent_id,
    login_id,
    name,
    password,
    LENGTH(password) as hash_length,
    CASE
        WHEN password = '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm' THEN '✅ Admin123!'
        WHEN password = '$2a$10$dummyhash' THEN '❌ 더미 해시 (업데이트 안됨)'
        ELSE '❓ 다른 비밀번호'
    END as password_status
FROM user_agents
WHERE agent_id = '10000000-0000-0000-0000-000000000003';

