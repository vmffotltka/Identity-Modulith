-- 비밀번호 확인 쿼리
SELECT
    agent_id,
    login_id,
    name,
    password,
    LENGTH(password) as password_length,
    CASE
        WHEN password LIKE '$2a$%' THEN '✅ BCrypt 형식'
        ELSE '❌ 잘못된 형식'
    END as hash_format
FROM user_agents
WHERE agent_id = '10000000-0000-0000-0000-000000000003';

-- 실제 비밀번호가 Admin123!인지 확인하려면:
-- 1. 온라인 BCrypt 검증기 사용
-- 2. 또는 애플리케이션에서 테스트

