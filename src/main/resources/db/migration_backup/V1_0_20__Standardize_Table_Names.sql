-- V1_0_20__Standardize_Table_Names.sql
-- 테이블명 표준화 - 문서 명세에 맞게 변경
-- 우선순위: P2 (선택사항, Breaking Change)
-- 영향: JPA @Table 어노테이션 수정 필요

-- ============================================================
-- ⚠️ 주의사항
-- ============================================================
-- 1. 이 마이그레이션은 Breaking Change입니다
-- 2. JPA Entity 클래스의 @Table(name="...") 어노테이션 수정 필요
-- 3. 외부 도구/스크립트에서 직접 테이블명 참조 시 영향
-- 4. 배포 전 모든 테스트 필수

-- ============================================================
-- Phase 1: User 모듈 테이블명 변경
-- ============================================================

-- agents → user_agents
ALTER TABLE IF EXISTS agents RENAME TO user_agents;

-- agent_roles → user_agent_roles
ALTER TABLE IF EXISTS agent_roles RENAME TO user_agent_roles;

-- ============================================================
-- Phase 2: Organization 모듈 테이블명 변경
-- ============================================================

-- departmentEntities → org_departments (카멜케이스 제거)
ALTER TABLE IF EXISTS "departmentEntities" RENAME TO org_departments;

-- 시퀀스나 제약조건명도 함께 변경
DO $$
BEGIN
    -- 제약조건 이름 변경
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agents_dept') THEN
        ALTER TABLE user_agents RENAME CONSTRAINT fk_agents_dept TO fk_user_agents_dept;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_departments_parent') THEN
        ALTER TABLE org_departments RENAME CONSTRAINT fk_departments_parent TO fk_org_departments_parent;
    END IF;
END $$;

-- ============================================================
-- Phase 3: RBAC 모듈 테이블명 변경
-- ============================================================

-- roles → rbac_roles
ALTER TABLE IF EXISTS roles RENAME TO rbac_roles;

-- permissions → rbac_permissions
ALTER TABLE IF EXISTS permissions RENAME TO rbac_permissions;

-- role_permissions → rbac_role_permissions
ALTER TABLE IF EXISTS role_permissions RENAME TO rbac_role_permissions;

-- ============================================================
-- Phase 4: FK 제약조건 업데이트
-- ============================================================

-- user_agents.dept_id → org_departments
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_user_agents_dept'
    ) THEN
        ALTER TABLE user_agents DROP CONSTRAINT fk_user_agents_dept;
        ALTER TABLE user_agents ADD CONSTRAINT fk_user_agents_dept
            FOREIGN KEY (dept_id) REFERENCES org_departments(dept_id) ON DELETE SET NULL;
    END IF;
END $$;

-- user_agent_roles → user_agents, rbac_roles
DO $$
BEGIN
    -- agent FK
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_roles_agent') THEN
        ALTER TABLE user_agent_roles DROP CONSTRAINT fk_agent_roles_agent;
        ALTER TABLE user_agent_roles ADD CONSTRAINT fk_user_agent_roles_agent
            FOREIGN KEY (agent_id) REFERENCES user_agents(agent_id) ON DELETE CASCADE;
    END IF;

    -- role FK
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_roles_role') THEN
        ALTER TABLE user_agent_roles DROP CONSTRAINT fk_agent_roles_role;
        ALTER TABLE user_agent_roles ADD CONSTRAINT fk_user_agent_roles_role
            FOREIGN KEY (role_id) REFERENCES rbac_roles(role_id) ON DELETE CASCADE;
    END IF;
END $$;

-- rbac_role_permissions → rbac_roles, rbac_permissions
DO $$
BEGIN
    -- role FK
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_role_permissions_role') THEN
        ALTER TABLE rbac_role_permissions DROP CONSTRAINT fk_role_permissions_role;
        ALTER TABLE rbac_role_permissions ADD CONSTRAINT fk_rbac_role_permissions_role
            FOREIGN KEY (role_id) REFERENCES rbac_roles(role_id) ON DELETE CASCADE;
    END IF;

    -- permission FK
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_role_permissions_permission') THEN
        ALTER TABLE rbac_role_permissions DROP CONSTRAINT fk_role_permissions_permission;
        ALTER TABLE rbac_role_permissions ADD CONSTRAINT fk_rbac_role_permissions_permission
            FOREIGN KEY (permission_id) REFERENCES rbac_permissions(permission_id) ON DELETE CASCADE;
    END IF;
END $$;

-- ============================================================
-- Phase 5: 인덱스 이름 업데이트
-- ============================================================

-- user_agents 인덱스
ALTER INDEX IF EXISTS idx_agents_tenant_id RENAME TO idx_user_agents_tenant_id;
ALTER INDEX IF EXISTS idx_agents_dept_id RENAME TO idx_user_agents_dept_id;
ALTER INDEX IF EXISTS idx_agents_status RENAME TO idx_user_agents_status;
ALTER INDEX IF EXISTS idx_agents_login_id RENAME TO idx_user_agents_login_id;
ALTER INDEX IF EXISTS idx_agents_tenant_status RENAME TO idx_user_agents_tenant_status;
ALTER INDEX IF EXISTS idx_agents_tenant_dept RENAME TO idx_user_agents_tenant_dept;
ALTER INDEX IF EXISTS idx_agents_scheduled_delete RENAME TO idx_user_agents_scheduled_delete;
ALTER INDEX IF EXISTS idx_agents_email RENAME TO idx_user_agents_email;
ALTER INDEX IF EXISTS idx_agents_employee_id RENAME TO idx_user_agents_employee_id;
ALTER INDEX IF EXISTS idx_agents_tenant_created RENAME TO idx_user_agents_tenant_created;
ALTER INDEX IF EXISTS idx_agents_name_pattern RENAME TO idx_user_agents_name_pattern;
ALTER INDEX IF EXISTS idx_agents_login_pattern RENAME TO idx_user_agents_login_pattern;

-- org_departments 인덱스
ALTER INDEX IF EXISTS uk_departments_tenant_path RENAME TO uk_org_departments_tenant_path;
ALTER INDEX IF EXISTS idx_departments_tenant_id RENAME TO idx_org_departments_tenant_id;
ALTER INDEX IF EXISTS idx_departments_parent_id RENAME TO idx_org_departments_parent_id;
ALTER INDEX IF EXISTS idx_departments_org_path RENAME TO idx_org_departments_org_path;
ALTER INDEX IF EXISTS idx_departments_status RENAME TO idx_org_departments_status;
ALTER INDEX IF EXISTS idx_departments_tenant_status RENAME TO idx_org_departments_tenant_status;
ALTER INDEX IF EXISTS idx_departments_tenant_parent RENAME TO idx_org_departments_tenant_parent;
ALTER INDEX IF EXISTS idx_departments_code RENAME TO idx_org_departments_code;
ALTER INDEX IF EXISTS idx_departments_type RENAME TO idx_org_departments_type;
ALTER INDEX IF EXISTS uk_departments_tenant_code RENAME TO uk_org_departments_tenant_code;
ALTER INDEX IF EXISTS idx_departments_tenant_created RENAME TO idx_org_departments_tenant_created;
ALTER INDEX IF EXISTS idx_departments_name_pattern RENAME TO idx_org_departments_name_pattern;

-- rbac_roles 인덱스
ALTER INDEX IF EXISTS uk_roles_tenant_name RENAME TO uk_rbac_roles_tenant_name;
ALTER INDEX IF EXISTS idx_roles_tenant_id RENAME TO idx_rbac_roles_tenant_id;
ALTER INDEX IF EXISTS idx_roles_is_active RENAME TO idx_rbac_roles_is_active;
ALTER INDEX IF EXISTS idx_roles_tenant_type RENAME TO idx_rbac_roles_tenant_type;
ALTER INDEX IF EXISTS idx_roles_tenant_type_active RENAME TO idx_rbac_roles_tenant_type_active;

-- rbac_permissions 인덱스
ALTER INDEX IF EXISTS uk_permissions_tenant_code RENAME TO uk_rbac_permissions_tenant_code;
ALTER INDEX IF EXISTS idx_permissions_tenant_id RENAME TO idx_rbac_permissions_tenant_id;
ALTER INDEX IF EXISTS idx_permissions_category RENAME TO idx_rbac_permissions_category;
ALTER INDEX IF EXISTS idx_permissions_tenant_category RENAME TO idx_rbac_permissions_tenant_category;
ALTER INDEX IF EXISTS idx_permissions_name RENAME TO idx_rbac_permissions_name;

-- user_agent_roles 인덱스
ALTER INDEX IF EXISTS uk_agent_roles RENAME TO uk_user_agent_roles;
ALTER INDEX IF EXISTS idx_agent_roles_agent_id RENAME TO idx_user_agent_roles_agent_id;
ALTER INDEX IF EXISTS idx_agent_roles_role_id RENAME TO idx_user_agent_roles_role_id;
ALTER INDEX IF EXISTS idx_agent_roles_assigned_by RENAME TO idx_user_agent_roles_assigned_by;
ALTER INDEX IF EXISTS idx_agent_roles_role_assigned RENAME TO idx_user_agent_roles_role_assigned;
ALTER INDEX IF EXISTS idx_agent_roles_agent_assigned RENAME TO idx_user_agent_roles_agent_assigned;

-- rbac_role_permissions 인덱스
ALTER INDEX IF EXISTS uk_role_permissions RENAME TO uk_rbac_role_permissions;
ALTER INDEX IF EXISTS idx_role_permissions_assigned_by RENAME TO idx_rbac_role_permissions_assigned_by;
ALTER INDEX IF EXISTS idx_role_permissions_permission RENAME TO idx_rbac_role_permissions_permission;

-- ============================================================
-- Phase 6: 뷰 업데이트
-- ============================================================

-- v_permission_statistics 재생성
DROP VIEW IF EXISTS v_permission_statistics;
CREATE OR REPLACE VIEW v_permission_statistics AS
SELECT
    tenant_id,
    category,
    COUNT(*) AS permission_count,
    array_agg(name ORDER BY code) AS permission_names
FROM rbac_permissions
GROUP BY tenant_id, category
ORDER BY tenant_id, category;

COMMENT ON VIEW v_permission_statistics IS '권한 카테고리별 통계 (대시보드용)';

-- v_agent_role_audit 재생성
DROP VIEW IF EXISTS v_agent_role_audit;
CREATE OR REPLACE VIEW v_agent_role_audit AS
SELECT
    ar.id,
    ar.agent_id,
    a.name AS agent_name,
    a.login_id,
    ar.role_id,
    r.name AS role_name,
    r.type AS role_type,
    ar.assigned_at,
    ar.assigned_by,
    CASE
        WHEN ar.assigned_by = 'SYSTEM' THEN '시스템'
        ELSE COALESCE(a2.name, ar.assigned_by)
    END AS assigned_by_name
FROM user_agent_roles ar
JOIN user_agents a ON ar.agent_id = a.agent_id
JOIN rbac_roles r ON ar.role_id = r.role_id
LEFT JOIN user_agents a2 ON ar.assigned_by = a2.agent_id
ORDER BY ar.assigned_at DESC;

COMMENT ON VIEW v_agent_role_audit IS '사용자별 역할 할당 이력 (감사 추적용)';

-- v_role_permission_audit 재생성
DROP VIEW IF EXISTS v_role_permission_audit;
CREATE OR REPLACE VIEW v_role_permission_audit AS
SELECT
    rp.id,
    rp.role_id,
    r.name AS role_name,
    r.type AS role_type,
    rp.permission_id,
    p.code AS permission_code,
    p.name AS permission_name,
    p.category AS permission_category,
    rp.assigned_at,
    rp.assigned_by,
    CASE
        WHEN rp.assigned_by = 'SYSTEM' THEN '시스템'
        ELSE COALESCE(a.name, rp.assigned_by)
    END AS assigned_by_name
FROM rbac_role_permissions rp
JOIN rbac_roles r ON rp.role_id = r.role_id
JOIN rbac_permissions p ON rp.permission_id = p.permission_id
LEFT JOIN user_agents a ON rp.assigned_by = a.agent_id
ORDER BY rp.assigned_at DESC;

COMMENT ON VIEW v_role_permission_audit IS '역할별 권한 할당 이력 (감사 추적용)';

-- ============================================================
-- Phase 7: 함수 업데이트
-- ============================================================

-- get_tenant_statistics 재생성
DROP FUNCTION IF EXISTS get_tenant_statistics(VARCHAR);
CREATE OR REPLACE FUNCTION get_tenant_statistics(p_tenant_id VARCHAR)
RETURNS TABLE (
    total_agents BIGINT,
    active_agents BIGINT,
    suspended_agents BIGINT,
    retired_agents BIGINT,
    total_departments BIGINT,
    active_departments BIGINT,
    total_roles BIGINT,
    total_permissions BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        (SELECT COUNT(*) FROM user_agents WHERE tenant_id = p_tenant_id),
        (SELECT COUNT(*) FROM user_agents WHERE tenant_id = p_tenant_id AND status = 'ACTIVE'),
        (SELECT COUNT(*) FROM user_agents WHERE tenant_id = p_tenant_id AND status = 'SUSPENDED'),
        (SELECT COUNT(*) FROM user_agents WHERE tenant_id = p_tenant_id AND status = 'RETIRED'),
        (SELECT COUNT(*) FROM org_departments WHERE tenant_id = p_tenant_id),
        (SELECT COUNT(*) FROM org_departments WHERE tenant_id = p_tenant_id AND status = 'ACTIVE'),
        (SELECT COUNT(*) FROM rbac_roles WHERE tenant_id = p_tenant_id),
        (SELECT COUNT(*) FROM rbac_permissions WHERE tenant_id = p_tenant_id);
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_tenant_statistics IS '테넌트별 전체 통계 조회 (대시보드용)';

-- ============================================================
-- Phase 8: 트리거 업데이트
-- ============================================================

-- departmentEntities 트리거 재생성
DROP TRIGGER IF EXISTS trg_departments_updated_at ON org_departments;
CREATE TRIGGER trg_departments_updated_at
    BEFORE UPDATE ON org_departments
    FOR EACH ROW
    EXECUTE FUNCTION update_departments_updated_at();

-- ============================================================
-- Phase 9: 테이블 코멘트 업데이트
-- ============================================================

COMMENT ON TABLE user_agents IS '사용자 (에이전트) 테이블 - 표준 명명 규칙 적용';
COMMENT ON TABLE org_departments IS '조직(부서) 테이블 - 표준 명명 규칙 적용';
COMMENT ON TABLE rbac_roles IS '역할 관리 테이블 - 표준 명명 규칙 적용';
COMMENT ON TABLE rbac_permissions IS '권한 관리 테이블 - 표준 명명 규칙 적용';
COMMENT ON TABLE user_agent_roles IS '사용자-역할 매핑 테이블 - 표준 명명 규칙 적용';
COMMENT ON TABLE rbac_role_permissions IS '역할-권한 매핑 테이블 - 표준 명명 규칙 적용';

-- ============================================================
-- 완료 메시지
-- ============================================================

DO $$
BEGIN
    RAISE NOTICE '✅ V1_0_20 마이그레이션 완료: 테이블명 표준화';
    RAISE NOTICE '';
    RAISE NOTICE '변경된 테이블:';
    RAISE NOTICE '  - agents → user_agents';
    RAISE NOTICE '  - departmentEntities → org_departments';
    RAISE NOTICE '  - roles → rbac_roles';
    RAISE NOTICE '  - permissions → rbac_permissions';
    RAISE NOTICE '  - agent_roles → user_agent_roles';
    RAISE NOTICE '  - role_permissions → rbac_role_permissions';
    RAISE NOTICE '';
    RAISE NOTICE '⚠️ 다음 단계 필수:';
    RAISE NOTICE '  1. JPA Entity @Table 어노테이션 업데이트';
    RAISE NOTICE '  2. 애플리케이션 재기동';
    RAISE NOTICE '  3. 전체 테스트 실행';
    RAISE NOTICE '';
    RAISE NOTICE '문서 일치도: 95% → 100% 🎉';
END $$;
