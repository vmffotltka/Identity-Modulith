-- ============================================================
-- V1_0_6: 권한 그룹화 기능 추가
-- 날짜: 2026-01-15
-- 목적: 관련된 권한들을 그룹화하여 역할 할당 간소화
-- ============================================================

-- ============================================================
-- Phase 1: 권한 그룹 테이블
-- ============================================================

CREATE TABLE IF NOT EXISTS permission_groups (
    permission_group_id VARCHAR(36) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE permission_groups IS '권한 그룹 관리 테이블';
COMMENT ON COLUMN permission_groups.permission_group_id IS '권한 그룹 ID (UUID)';
COMMENT ON COLUMN permission_groups.tenant_id IS '테넌트 ID (멀티테넌시)';
COMMENT ON COLUMN permission_groups.name IS '권한 그룹명 (USER_FULL_ACCESS, ORG_ADMIN 등)';
COMMENT ON COLUMN permission_groups.description IS '권한 그룹 설명';
COMMENT ON COLUMN permission_groups.is_active IS '활성화 상태 (true=활성, false=비활성)';
COMMENT ON COLUMN permission_groups.version IS '낙관적 잠금 버전';
COMMENT ON COLUMN permission_groups.created_at IS '생성 일시';
COMMENT ON COLUMN permission_groups.updated_at IS '마지막 수정 일시';

CREATE UNIQUE INDEX IF NOT EXISTS uk_permission_groups_tenant_name
    ON permission_groups(tenant_id, name);
CREATE INDEX IF NOT EXISTS idx_permission_groups_tenant_id
    ON permission_groups(tenant_id);
CREATE INDEX IF NOT EXISTS idx_permission_groups_is_active
    ON permission_groups(is_active);

-- ============================================================
-- Phase 2: 권한 그룹-권한 매핑 테이블
-- ============================================================

CREATE TABLE IF NOT EXISTS permission_group_permissions (
    id BIGSERIAL PRIMARY KEY,
    permission_group_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE permission_group_permissions IS '권한 그룹-권한 매핑 테이블';
COMMENT ON COLUMN permission_group_permissions.id IS '매핑 ID';
COMMENT ON COLUMN permission_group_permissions.permission_group_id IS '권한 그룹 ID (FK)';
COMMENT ON COLUMN permission_group_permissions.permission_id IS '권한 ID (FK)';
COMMENT ON COLUMN permission_group_permissions.added_at IS '추가 일시';

CREATE UNIQUE INDEX IF NOT EXISTS uk_permission_group_permissions
    ON permission_group_permissions(permission_group_id, permission_id);
CREATE INDEX IF NOT EXISTS idx_permission_group_permissions_group_id
    ON permission_group_permissions(permission_group_id);
CREATE INDEX IF NOT EXISTS idx_permission_group_permissions_permission_id
    ON permission_group_permissions(permission_id);

ALTER TABLE permission_group_permissions
    ADD CONSTRAINT fk_permission_group_permissions_group
    FOREIGN KEY (permission_group_id) REFERENCES permission_groups(permission_group_id)
    ON DELETE CASCADE;

ALTER TABLE permission_group_permissions
    ADD CONSTRAINT fk_permission_group_permissions_permission
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id)
    ON DELETE CASCADE;

-- ============================================================
-- Phase 3: 역할-권한 그룹 매핑 테이블
-- ============================================================

CREATE TABLE IF NOT EXISTS role_permission_groups (
    id BIGSERIAL PRIMARY KEY,
    role_id VARCHAR(36) NOT NULL,
    permission_group_id VARCHAR(36) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE role_permission_groups IS '역할-권한 그룹 매핑 테이블';
COMMENT ON COLUMN role_permission_groups.id IS '매핑 ID';
COMMENT ON COLUMN role_permission_groups.role_id IS '역할 ID (FK)';
COMMENT ON COLUMN role_permission_groups.permission_group_id IS '권한 그룹 ID (FK)';
COMMENT ON COLUMN role_permission_groups.assigned_at IS '할당 일시';

CREATE UNIQUE INDEX IF NOT EXISTS uk_role_permission_groups
    ON role_permission_groups(role_id, permission_group_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_groups_role_id
    ON role_permission_groups(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_groups_group_id
    ON role_permission_groups(permission_group_id);

ALTER TABLE role_permission_groups
    ADD CONSTRAINT fk_role_permission_groups_role
    FOREIGN KEY (role_id) REFERENCES roles(role_id)
    ON DELETE CASCADE;

ALTER TABLE role_permission_groups
    ADD CONSTRAINT fk_role_permission_groups_group
    FOREIGN KEY (permission_group_id) REFERENCES permission_groups(permission_group_id)
    ON DELETE CASCADE;

-- ============================================================
-- Phase 4: 표준 권한 그룹 데이터 삽입 (선택사항)
-- ============================================================

-- 사용자 관리 권한 그룹
INSERT INTO permission_groups (permission_group_id, tenant_id, name, description, is_active, created_at, updated_at)
VALUES (
    '650e8400-e29b-41d4-a716-446655440001',
    'tenant-001',
    'USER_FULL_ACCESS',
    '사용자 생성, 조회, 수정, 삭제 권한',
    true,
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- 조직 관리 권한 그룹
INSERT INTO permission_groups (permission_group_id, tenant_id, name, description, is_active, created_at, updated_at)
VALUES (
    '650e8400-e29b-41d4-a716-446655440002',
    'tenant-001',
    'ORGANIZATION_FULL_ACCESS',
    '조직 생성, 조회, 수정, 삭제, 이동 권한',
    true,
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- 보고서 생성 권한 그룹
INSERT INTO permission_groups (permission_group_id, tenant_id, name, description, is_active, created_at, updated_at)
VALUES (
    '650e8400-e29b-41d4-a716-446655440003',
    'tenant-001',
    'REPORTING_ACCESS',
    '보고서 조회 및 내보내기 권한',
    true,
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- ============================================================
-- 결과 확인
-- ============================================================

SELECT
    '✅ 권한 그룹화 기능 추가 완료!' as result,
    (SELECT COUNT(*) FROM permission_groups WHERE tenant_id = 'tenant-001' AND is_active = true) as active_groups,
    (SELECT COUNT(*) FROM permission_group_permissions) as total_group_permissions,
    (SELECT COUNT(*) FROM role_permission_groups) as total_role_groups;

