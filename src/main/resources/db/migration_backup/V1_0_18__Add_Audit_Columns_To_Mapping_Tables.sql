-- V1_0_18__Add_Audit_Columns_To_Mapping_Tables.sql
-- 매핑 테이블에 감사 컬럼 추가
-- 우선순위: P1 (조만간 권장)

-- ============================================================
-- Phase 1: agent_roles 테이블 감사 컬럼 추가
-- ============================================================

-- 1. assigned_by 컬럼 추가 (누가 역할을 할당했는지 추적)
ALTER TABLE agent_roles ADD COLUMN IF NOT EXISTS assigned_by VARCHAR(36);

-- 2. 코멘트 추가
COMMENT ON COLUMN agent_roles.id IS '매핑 ID (대리키)';
COMMENT ON COLUMN agent_roles.agent_id IS '사용자 ID (FK → agents)';
COMMENT ON COLUMN agent_roles.role_id IS '역할 ID (FK → roles)';
COMMENT ON COLUMN agent_roles.assigned_at IS '할당 일시';
COMMENT ON COLUMN agent_roles.assigned_by IS '할당자 ID (감사 추적용, NULL이면 시스템 할당)';

-- 3. 기존 데이터 처리
UPDATE agent_roles
SET assigned_by = 'SYSTEM'
WHERE assigned_by IS NULL;

-- ============================================================
-- Phase 2: role_permissions 테이블 감사 컬럼 추가
-- ============================================================

-- 1. assigned_by 컬럼 추가 (누가 권한을 할당했는지 추적)
ALTER TABLE role_permissions ADD COLUMN IF NOT EXISTS assigned_by VARCHAR(36);

-- 2. 코멘트 추가
COMMENT ON COLUMN role_permissions.id IS '매핑 ID (대리키)';
COMMENT ON COLUMN role_permissions.role_id IS '역할 ID (FK → roles)';
COMMENT ON COLUMN role_permissions.permission_id IS '권한 ID (FK → permissions)';
COMMENT ON COLUMN role_permissions.assigned_at IS '할당 일시';
COMMENT ON COLUMN role_permissions.assigned_by IS '할당자 ID (감사 추적용, NULL이면 시스템 할당)';

-- 3. 기존 데이터 처리
UPDATE role_permissions
SET assigned_by = 'SYSTEM'
WHERE assigned_by IS NULL;

-- ============================================================
-- Phase 3: 인덱스 추가 (감사 추적 조회 성능)
-- ============================================================

-- agent_roles: assigned_by 인덱스
CREATE INDEX IF NOT EXISTS idx_agent_roles_assigned_by
    ON agent_roles(assigned_by)
    WHERE assigned_by IS NOT NULL;

-- role_permissions: assigned_by 인덱스
CREATE INDEX IF NOT EXISTS idx_role_permissions_assigned_by
    ON role_permissions(assigned_by)
    WHERE assigned_by IS NOT NULL;

-- ============================================================
-- Phase 4: 감사 추적 뷰 생성
-- ============================================================

-- 사용자별 역할 할당 이력
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
FROM agent_roles ar
JOIN agents a ON ar.agent_id = a.agent_id
JOIN roles r ON ar.role_id = r.role_id
LEFT JOIN agents a2 ON ar.assigned_by = a2.agent_id
ORDER BY ar.assigned_at DESC;

COMMENT ON VIEW v_agent_role_audit IS '사용자별 역할 할당 이력 (감사 추적용)';

-- 역할별 권한 할당 이력
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
FROM role_permissions rp
JOIN roles r ON rp.role_id = r.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
LEFT JOIN agents a ON rp.assigned_by = a.agent_id
ORDER BY rp.assigned_at DESC;

COMMENT ON VIEW v_role_permission_audit IS '역할별 권한 할당 이력 (감사 추적용)';

-- ============================================================
-- 완료 메시지
-- ============================================================

DO $$
BEGIN
    RAISE NOTICE '✅ V1_0_18 마이그레이션 완료: 매핑 테이블 감사 컬럼 추가';
    RAISE NOTICE '  - agent_roles.assigned_by 추가';
    RAISE NOTICE '  - role_permissions.assigned_by 추가';
    RAISE NOTICE '  - 인덱스 2개 추가';
    RAISE NOTICE '  - 감사 추적 뷰 2개 생성';
END $$;
