-- 1. 현재 저장된 비밀번호 해시 확인
SELECT
    agent_id,
    login_id,
    name,
    password,
    LENGTH(password) as hash_length
FROM user_agents
WHERE agent_id = '10000000-0000-0000-0000-000000000003';

-- 2. 모든 사용자의 비밀번호 해시 확인
SELECT
    agent_id,
    login_id,
    LEFT(password, 30) || '...' as password_preview,
    LENGTH(password) as hash_length
FROM user_agents
ORDER BY login_id;

