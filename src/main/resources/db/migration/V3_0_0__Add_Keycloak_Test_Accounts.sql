-- =============================================================================
-- V3: Keycloak 테스트 계정 추가
--
-- Keycloak에 등록된 사용자(test.admin)를 로컬 DB의 user_agents에 추가합니다.
-- 기존 데이터가 있으면 건너뜁니다 (ON CONFLICT DO NOTHING).
-- =============================================================================

DO $$
DECLARE
    -- test.admin Agent ID (고정값으로 추적 용이)
    test_admin_id    VARCHAR(36) := '10000000-0000-0000-0000-000000000010';
    admin_role_id    VARCHAR(36) := '20000000-0000-0000-0000-000000000001';
    company_dept_id  VARCHAR(36) := '00000000-0000-0000-0000-000000000001';
    std_tenant       VARCHAR(50) := 'default-tenant';
    now_time         TIMESTAMP   := NOW();
BEGIN
    -- ─── test.admin Agent 생성 ──────────────────────────────────────────────
    -- Keycloak username: test.admin / email: admin@example.com
    -- 비밀번호: Admin123! (BCrypt, 로컬 로그인용 - SAML 로그인 시 미사용)
    INSERT INTO user_agents (
        agent_id, tenant_id, login_id, password,
        name, employee_id, email,
        dept_id, status, created_at, updated_at
    )
    VALUES (
        test_admin_id, std_tenant, 'test.admin',
        '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm',
        '테스트관리자', 'EMP-0010', 'admin@example.com',
        company_dept_id, 'ACTIVE', now_time, now_time
    )
    ON CONFLICT (login_id) DO NOTHING;

    -- ─── ADMIN 역할 부여 ────────────────────────────────────────────────────
    INSERT INTO rbac_agent_roles (agent_id, role_id, assigned_at, created_at)
    VALUES (test_admin_id, admin_role_id, now_time, now_time)
    ON CONFLICT (agent_id, role_id) DO NOTHING;

    RAISE NOTICE '✅ Keycloak 테스트 계정 등록 완료: test.admin (ADMIN 역할)';
END $$;

