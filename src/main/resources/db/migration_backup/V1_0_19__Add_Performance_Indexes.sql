-- V1_0_19__Add_Performance_Indexes.sql
-- 성능 최적화를 위한 추가 인덱스
-- 우선순위: P1 (조만간 권장)

-- ============================================================
-- Phase 1: roles 테이블 추가 인덱스
-- ============================================================

-- 복합 인덱스: tenant + type (채널 역할 조회 등)
CREATE INDEX IF NOT EXISTS idx_roles_tenant_type
    ON roles(tenant_id, type);

-- 복합 인덱스: tenant + type + is_active (활성 채널 역할 조회)
CREATE INDEX IF NOT EXISTS idx_roles_tenant_type_active
    ON roles(tenant_id, type, is_active);

COMMENT ON INDEX idx_roles_tenant_type IS '테넌트별 역할 타입 조회 최적화';
COMMENT ON INDEX idx_roles_tenant_type_active IS '테넌트별 활성 역할 타입 조회 최적화';

-- ============================================================
-- Phase 2: agent_roles 테이블 추가 인덱스
-- ============================================================

-- 복합 인덱스: role_id + assigned_at (역할별 최근 할당 조회)
CREATE INDEX IF NOT EXISTS idx_agent_roles_role_assigned
    ON agent_roles(role_id, assigned_at DESC);

-- 복합 인덱스: agent_id + assigned_at (사용자별 역할 할당 이력)
CREATE INDEX IF NOT EXISTS idx_agent_roles_agent_assigned
    ON agent_roles(agent_id, assigned_at DESC);

COMMENT ON INDEX idx_agent_roles_role_assigned IS '역할별 최근 할당 이력 조회 최적화';
COMMENT ON INDEX idx_agent_roles_agent_assigned IS '사용자별 역할 할당 이력 조회 최적화';

-- ============================================================
-- Phase 3: role_permissions 테이블 추가 인덱스
-- ============================================================

-- 복합 인덱스: permission_id + role_id (권한별 역할 조회)
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission
    ON role_permissions(permission_id, role_id);

COMMENT ON INDEX idx_role_permissions_permission IS '권한별 역할 조회 최적화 (역방향 검색)';

-- ============================================================
-- Phase 4: audit_logs 테이블 추가 인덱스
-- ============================================================

-- 복합 인덱스: tenant + timestamp (시간순 조회)
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_timestamp
    ON audit_logs(tenant_id, timestamp DESC);

-- 복합 인덱스: resource_type + resource_id (리소스별 이력)
CREATE INDEX IF NOT EXISTS idx_audit_logs_resource
    ON audit_logs(resource_type, resource_id, timestamp DESC);

-- 복합 인덱스: tenant + action (작업 유형별 조회)
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_action
    ON audit_logs(tenant_id, action);

COMMENT ON INDEX idx_audit_logs_tenant_timestamp IS '테넌트별 시간순 감사 로그 조회';
COMMENT ON INDEX idx_audit_logs_resource IS '리소스별 감사 로그 이력 조회';
COMMENT ON INDEX idx_audit_logs_tenant_action IS '테넌트별 작업 유형 필터링';

-- ============================================================
-- Phase 5: 통계 및 분석용 인덱스
-- ============================================================

-- agents: 테넌트별 생성일자 (가입 추이 분석)
CREATE INDEX IF NOT EXISTS idx_agents_tenant_created
    ON agents(tenant_id, created_at DESC);

-- departmentEntities: 테넌트별 생성일자 (조직 변화 추이)
CREATE INDEX IF NOT EXISTS idx_departments_tenant_created
    ON departmentEntities(tenant_id, created_at DESC);

COMMENT ON INDEX idx_agents_tenant_created IS '사용자 가입 추이 분석용';
COMMENT ON INDEX idx_departments_tenant_created IS '조직 변화 추이 분석용';

-- ============================================================
-- Phase 6: 검색 성능 최적화
-- ============================================================

-- agents: 이름 검색 (LIKE 최적화 - 한국어 대응)
CREATE INDEX IF NOT EXISTS idx_agents_name_pattern
    ON agents(name text_pattern_ops);

-- agents: 로그인ID 검색 (LIKE 최적화)
CREATE INDEX IF NOT EXISTS idx_agents_login_pattern
    ON agents(login_id text_pattern_ops);

-- departmentEntities: 부서명 검색 (LIKE 최적화)
CREATE INDEX IF NOT EXISTS idx_departments_name_pattern
    ON departmentEntities(name text_pattern_ops);

COMMENT ON INDEX idx_agents_name_pattern IS '사용자 이름 부분 검색 최적화 (LIKE)';
COMMENT ON INDEX idx_agents_login_pattern IS '로그인ID 부분 검색 최적화 (LIKE)';
COMMENT ON INDEX idx_departments_name_pattern IS '부서명 부분 검색 최적화 (LIKE)';

-- ============================================================
-- Phase 7: 분석 함수 (선택)
-- ============================================================

-- 테넌트별 통계 함수
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
        (SELECT COUNT(*) FROM agents WHERE tenant_id = p_tenant_id),
        (SELECT COUNT(*) FROM agents WHERE tenant_id = p_tenant_id AND status = 'ACTIVE'),
        (SELECT COUNT(*) FROM agents WHERE tenant_id = p_tenant_id AND status = 'SUSPENDED'),
        (SELECT COUNT(*) FROM agents WHERE tenant_id = p_tenant_id AND status = 'RETIRED'),
        (SELECT COUNT(*) FROM departmentEntities WHERE tenant_id = p_tenant_id),
        (SELECT COUNT(*) FROM departmentEntities WHERE tenant_id = p_tenant_id AND status = 'ACTIVE'),
        (SELECT COUNT(*) FROM roles WHERE tenant_id = p_tenant_id),
        (SELECT COUNT(*) FROM permissions WHERE tenant_id = p_tenant_id);
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_tenant_statistics IS '테넌트별 전체 통계 조회 (대시보드용)';

-- ============================================================
-- Phase 8: 인덱스 사용 통계 뷰 (모니터링용)
-- ============================================================

CREATE OR REPLACE VIEW v_index_usage_stats AS
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched,
    ROUND(100.0 * idx_scan / GREATEST(seq_scan + idx_scan, 1), 2) AS index_usage_percent
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

COMMENT ON VIEW v_index_usage_stats IS '인덱스 사용 통계 (성능 모니터링용)';

-- ============================================================
-- 완료 메시지
-- ============================================================

DO $$
DECLARE
    total_indexes INT;
BEGIN
    SELECT COUNT(*) INTO total_indexes
    FROM pg_indexes
    WHERE schemaname = 'public';

    RAISE NOTICE '✅ V1_0_19 마이그레이션 완료: 성능 최적화 인덱스 추가';
    RAISE NOTICE '  - roles 테이블: 2개 인덱스';
    RAISE NOTICE '  - agent_roles 테이블: 2개 인덱스';
    RAISE NOTICE '  - role_permissions 테이블: 1개 인덱스';
    RAISE NOTICE '  - audit_logs 테이블: 3개 인덱스';
    RAISE NOTICE '  - 통계 분석용: 2개 인덱스';
    RAISE NOTICE '  - 검색 최적화: 3개 인덱스';
    RAISE NOTICE '  - 통계 함수 1개, 모니터링 뷰 1개 생성';
    RAISE NOTICE '  - 현재 전체 인덱스 수: %', total_indexes;
END $$;
