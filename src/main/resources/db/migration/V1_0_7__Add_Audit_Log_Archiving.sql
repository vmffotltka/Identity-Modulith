-- ============================================================
-- V1_0_7: 감사 로그 아카이빙 기능 추가
-- 날짜: 2026-01-15
-- 목적: 오래된 감사 로그 아카이빙으로 활성 테이블 최적화
-- ============================================================

-- ============================================================
-- Phase 1: 감사 로그 아카이브 테이블 생성
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_logs_archive (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    operator_id VARCHAR(36) NOT NULL,
    changes TEXT,
    timestamp TIMESTAMP NOT NULL,
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE audit_logs_archive IS '감사 로그 아카이브 테이블 (6개월 이상 된 로그)';
COMMENT ON COLUMN audit_logs_archive.id IS '로그 ID (UUID)';
COMMENT ON COLUMN audit_logs_archive.tenant_id IS '테넌트 ID';
COMMENT ON COLUMN audit_logs_archive.action IS '작업 유형 (CREATE, UPDATE, DELETE, ASSIGN, REVOKE 등)';
COMMENT ON COLUMN audit_logs_archive.resource_type IS '리소스 타입 (ROLE, PERMISSION, AGENT_ROLE 등)';
COMMENT ON COLUMN audit_logs_archive.resource_id IS '리소스 ID';
COMMENT ON COLUMN audit_logs_archive.operator_id IS '작업 수행자 ID';
COMMENT ON COLUMN audit_logs_archive.changes IS '변경 내용 (JSON)';
COMMENT ON COLUMN audit_logs_archive.timestamp IS '작업 발생 일시';
COMMENT ON COLUMN audit_logs_archive.archived_at IS '아카이브 일시';

-- 성능 최적화 인덱스
CREATE INDEX IF NOT EXISTS idx_audit_logs_archive_tenant
    ON audit_logs_archive(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_archive_timestamp
    ON audit_logs_archive(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_archive_resource
    ON audit_logs_archive(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_archive_operator
    ON audit_logs_archive(operator_id);

-- ============================================================
-- Phase 2: 감사 로그 테이블에 아카이빙 시간 인덱스 추가
-- ============================================================

-- 활성 감사 로그 테이블의 timestamp 인덱스 (아카이빙 배치 성능 최적화)
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp_desc
    ON audit_logs(timestamp DESC);

-- ============================================================
-- Phase 3: 아카이빙 통계 조회 뷰 (선택사항)
-- ============================================================

CREATE OR REPLACE VIEW audit_logs_statistics AS
SELECT
    'active' as log_type,
    COUNT(*) as log_count,
    MIN(timestamp) as oldest_log,
    MAX(timestamp) as newest_log,
    CURRENT_TIMESTAMP as statistics_time
FROM audit_logs
UNION ALL
SELECT
    'archived' as log_type,
    COUNT(*) as log_count,
    MIN(timestamp) as oldest_log,
    MAX(timestamp) as newest_log,
    CURRENT_TIMESTAMP as statistics_time
FROM audit_logs_archive;

COMMENT ON VIEW audit_logs_statistics IS '감사 로그 활성/아카이브 통계 뷰';

-- ============================================================
-- 결과 확인
-- ============================================================

SELECT
    '✅ 감사 로그 아카이빙 기능 추가 완료!' as result,
    (SELECT COUNT(*) FROM audit_logs) as active_logs,
    (SELECT COUNT(*) FROM audit_logs_archive) as archived_logs,
    (SELECT COUNT(*) FROM information_schema.tables
     WHERE table_schema = 'public' AND table_name LIKE 'audit_logs%') as audit_tables;

