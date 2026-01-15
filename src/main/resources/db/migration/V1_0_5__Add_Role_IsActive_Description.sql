-- ============================================================
-- V1_0_5: roles 테이블에 is_active, description, version 컬럼 추가
-- 날짜: 2026-01-15
-- 목적: 역할 논리적 삭제(Soft Delete) 및 설명, 낙관적 잠금 기능 추가
-- ============================================================

-- 1. description 컬럼 추가 (역할 설명)
ALTER TABLE roles
ADD COLUMN IF NOT EXISTS description VARCHAR(255);

COMMENT ON COLUMN roles.description IS '역할 설명 (목적 및 권한 범위)';

-- 2. is_active 컬럼 추가 (활성화 상태)
ALTER TABLE roles
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;

COMMENT ON COLUMN roles.is_active IS '활성화 상태 (true=활성, false=비활성/논리적 삭제)';

-- 3. version 컬럼 추가 (낙관적 잠금)
ALTER TABLE roles
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN roles.version IS '낙관적 잠금 버전 (동시성 제어용, JPA @Version)';

-- 4. updated_at 컬럼 추가 (마지막 수정 일시)
ALTER TABLE roles
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

COMMENT ON COLUMN roles.updated_at IS '마지막 수정 일시 (권한 변경 추적용)';

-- 5. 인덱스 추가 (활성 역할 빠른 조회)
CREATE INDEX IF NOT EXISTS idx_roles_is_active ON roles(is_active);

-- 6. 기존 데이터 업데이트 (모두 활성화 상태로)
UPDATE roles SET is_active = true WHERE is_active IS NULL;

-- 7. version 초기화 (기존 데이터는 0)
UPDATE roles SET version = 0 WHERE version IS NULL;

-- 8. 기존 역할에 기본 설명 추가 (선택 사항)
UPDATE roles SET description = '시스템 전체를 관리하는 최고 관리자 권한'
WHERE name = 'ADMIN' AND description IS NULL;

UPDATE roles SET description = '팀을 관리하고 팀원의 업무를 지원하는 권한'
WHERE name = 'TEAM_LEADER' AND description IS NULL;

UPDATE roles SET description = '전화 상담을 수행하는 상담사 권한'
WHERE name = 'PHONE_AGENT' AND description IS NULL;

UPDATE roles SET description = '채팅 상담을 수행하는 상담사 권한'
WHERE name = 'CHAT_AGENT' AND description IS NULL;

UPDATE roles SET description = '이메일 상담을 수행하는 상담사 권한'
WHERE name = 'EMAIL_AGENT' AND description IS NULL;

-- ============================================================
-- 결과 확인
-- ============================================================
SELECT
    '✅ roles 테이블 컬럼 추가 완료!' as result,
    COUNT(*) as total_roles,
    COUNT(CASE WHEN is_active = true THEN 1 END) as active_roles,
    COUNT(CASE WHEN is_active = false THEN 1 END) as inactive_roles,
    AVG(version) as avg_version
FROM roles;

