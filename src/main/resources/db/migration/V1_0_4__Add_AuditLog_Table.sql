-- ============================================================
-- RBAC 감사 로그 테이블 생성 마이그레이션 스크립트
-- 버전: 1.0.4
-- 설명: audit_logs 테이블 생성 및 관련 인덱스 추가
-- ============================================================

-- audit_logs 테이블 생성
CREATE TABLE IF NOT EXISTS audit_logs (
    audit_id VARCHAR(36) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    action VARCHAR(32) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id VARCHAR(36) NOT NULL,
    operator_id VARCHAR(36),
    changes TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks TEXT,
    ip_address VARCHAR(45)
);

-- 테이블 주석
COMMENT ON TABLE audit_logs IS 'RBAC 감사 로그 테이블 - 모든 권한 관리 작업 기록';
COMMENT ON COLUMN audit_logs.audit_id IS '감사 로그 고유 ID (UUID)';
COMMENT ON COLUMN audit_logs.tenant_id IS '테넌트 ID (멀티테넌시 격리)';
COMMENT ON COLUMN audit_logs.action IS '수행된 작업 (CREATE, UPDATE, DELETE, ASSIGN, REVOKE)';
COMMENT ON COLUMN audit_logs.resource_type IS '대상 리소스 타입 (ROLE, PERMISSION, ROLE_PERMISSION, AGENT_ROLE)';
COMMENT ON COLUMN audit_logs.resource_id IS '대상 리소스 ID';
COMMENT ON COLUMN audit_logs.operator_id IS '작업 수행자 ID (사용자 ID)';
COMMENT ON COLUMN audit_logs.changes IS '변경 내용 (JSON 형식)';
COMMENT ON COLUMN audit_logs.timestamp IS '작업 수행 일시';
COMMENT ON COLUMN audit_logs.remarks IS '추가 정보 (메모, 실패 원인 등)';
COMMENT ON COLUMN audit_logs.ip_address IS '클라이언트 IP 주소';

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_id ON audit_logs(tenant_id);
COMMENT ON INDEX idx_audit_logs_tenant_id IS '테넌트 기반 빠른 조회';

CREATE INDEX IF NOT EXISTS idx_audit_logs_resource_type ON audit_logs(resource_type);
COMMENT ON INDEX idx_audit_logs_resource_type IS '리소스 타입 기반 빠른 조회';

CREATE INDEX IF NOT EXISTS idx_audit_logs_operator_id ON audit_logs(operator_id);
COMMENT ON INDEX idx_audit_logs_operator_id IS '작업자 기반 빠른 조회';

CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);
COMMENT ON INDEX idx_audit_logs_timestamp IS '시간 범위 조회 최적화';

CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_timestamp ON audit_logs(tenant_id, timestamp);
COMMENT ON INDEX idx_audit_logs_tenant_timestamp IS '테넌트별 시간순 조회 최적화';

-- ============================================================
-- 샘플 데이터 (선택사항 - 테스트용)
-- ============================================================

-- 역할 생성 감사 로그 예시
INSERT INTO audit_logs (
    audit_id,
    tenant_id,
    action,
    resource_type,
    resource_id,
    operator_id,
    changes,
    timestamp
) VALUES (
    UUID(),
    'default-tenant',
    'CREATE',
    'ROLE',
    'role-001',
    'admin-user',
    '{"name": "ADMIN", "type": "POSITION"}',
    CURRENT_TIMESTAMP
);

-- 역할-권한 할당 감사 로그 예시
INSERT INTO audit_logs (
    audit_id,
    tenant_id,
    action,
    resource_type,
    resource_id,
    operator_id,
    changes,
    timestamp
) VALUES (
    UUID(),
    'default-tenant',
    'ASSIGN',
    'ROLE_PERMISSION',
    'role-001',
    'admin-user',
    '{"role": "ADMIN", "permission": "user:manage"}',
    CURRENT_TIMESTAMP
);

-- 사용자-역할 할당 감사 로그 예시
INSERT INTO audit_logs (
    audit_id,
    tenant_id,
    action,
    resource_type,
    resource_id,
    operator_id,
    changes,
    timestamp,
    ip_address
) VALUES (
    UUID(),
    'default-tenant',
    'ASSIGN',
    'AGENT_ROLE',
    'user-123',
    'admin-user',
    '{"agent": "user-123", "role": "TEAM_LEADER"}',
    CURRENT_TIMESTAMP,
    '192.168.1.100'
);

-- ============================================================
-- 마이그레이션 완료 메시지
-- ============================================================
SELECT '✅ audit_logs 테이블 생성 완료!' as result;
SELECT
    'audit_logs' as table_name,
    COUNT(*) as row_count,
    '감사 로그' as description
FROM audit_logs;

