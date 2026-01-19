-- ============================================================
-- Plan B: 동시성 제어 (Optimistic Locking)
-- Version: 1.1.0
-- Description: roles, permissions, departments 테이블에 version 컬럼 추가
-- ============================================================

-- 1. roles 테이블에 version 컬럼 추가
ALTER TABLE roles ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
COMMENT ON COLUMN roles.version IS '낙관적 락 버전 (동시성 제어)';

-- 2. permissions 테이블에 version 컬럼 추가
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
COMMENT ON COLUMN permissions.version IS '낙관적 락 버전 (동시성 제어)';

-- 3. departments 테이블에 version 컬럼 추가
ALTER TABLE departments ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
COMMENT ON COLUMN departments.version IS '낙관적 락 버전 (동시성 제어)';

-- 4. 기존 데이터의 version을 0으로 초기화 (이미 DEFAULT로 설정되어 있지만 명시적으로)
UPDATE roles SET version = 0 WHERE version IS NULL;
UPDATE permissions SET version = 0 WHERE version IS NULL;
UPDATE departments SET version = 0 WHERE version IS NULL;

-- ============================================================
-- 검증 쿼리
-- ============================================================
SELECT '✅ version 컬럼 추가 완료!' as result;

SELECT 'roles' as table_name,
       COUNT(*) as total_rows,
       COUNT(version) as version_column_count,
       '동시성 제어 준비 완료' as status
FROM roles
UNION ALL
SELECT 'permissions',
       COUNT(*),
       COUNT(version),
       '동시성 제어 준비 완료'
FROM permissions
UNION ALL
SELECT 'departments',
       COUNT(*),
       COUNT(version),
       '동시성 제어 준비 완료'
FROM departments;

