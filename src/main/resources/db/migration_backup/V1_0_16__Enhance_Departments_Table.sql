-- V1_0_16__Enhance_Departments_Table.sql
-- departmentEntities 테이블 강화 - 필수 컬럼 추가
-- 우선순위: P0 (즉시 필요)

-- ============================================================
-- Phase 1: 누락된 컬럼 추가
-- ============================================================

-- 1. 부서 코드 (사용자 친화적 식별자)
ALTER TABLE departmentEntities ADD COLUMN IF NOT EXISTS code VARCHAR(30);

-- 2. 커스텀 타입명 (type='CUSTOM'일 때 사용)
ALTER TABLE departmentEntities ADD COLUMN IF NOT EXISTS custom_type_name VARCHAR(50);

-- 3. 비활성화 일시 추적
ALTER TABLE departmentEntities ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP;

-- 4. 수정 일시 (created_at은 이미 있음)
ALTER TABLE departmentEntities ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 5. 감사 추적 컬럼
ALTER TABLE departmentEntities ADD COLUMN IF NOT EXISTS created_by VARCHAR(36);
ALTER TABLE departmentEntities ADD COLUMN IF NOT EXISTS updated_by VARCHAR(36);

-- 6. 낙관적 잠금 (동시성 제어)
ALTER TABLE departmentEntities ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;

-- ============================================================
-- Phase 2: 기존 데이터 처리
-- ============================================================

-- 기존 부서에 code 생성 (dept_id 앞 8자 사용)
UPDATE departmentEntities
SET code = 'DEPT-' || SUBSTRING(dept_id, 1, 8),
    updated_at = COALESCE(created_at, NOW()),
    created_by = 'SYSTEM',
    updated_by = 'SYSTEM'
WHERE code IS NULL;

-- code를 NOT NULL로 설정
ALTER TABLE departmentEntities ALTER COLUMN code SET NOT NULL;
ALTER TABLE departmentEntities ALTER COLUMN updated_at SET NOT NULL;

-- ============================================================
-- Phase 3: 코멘트 추가
-- ============================================================

COMMENT ON TABLE departmentEntities IS '조직(부서) 관리 테이블 - Materialized Path 패턴 사용';

COMMENT ON COLUMN departmentEntities.dept_id IS '부서 ID (UUID)';
COMMENT ON COLUMN departmentEntities.tenant_id IS '테넌트 ID (멀티테넌시)';
COMMENT ON COLUMN departmentEntities.parent_id IS '상위 부서 ID (자기참조, NULL이면 루트)';
COMMENT ON COLUMN departmentEntities.name IS '부서명';
COMMENT ON COLUMN departmentEntities.code IS '부서 코드 (사용자 친화적 식별자, 예: CS-HQ)';
COMMENT ON COLUMN departmentEntities.org_path IS '조직 경로 (Materialized Path, 예: /parent/child)';
COMMENT ON COLUMN departmentEntities.depth IS '조직 트리 깊이 (루트=0, 자식=1, ...)';
COMMENT ON COLUMN departmentEntities.type IS '부서 타입 (COMPANY, DIVISION, TEAM, GROUP, CUSTOM, HEADQUARTERS 등)';
COMMENT ON COLUMN departmentEntities.custom_type_name IS '커스텀 타입명 (type=CUSTOM일 때 사용, 예: 센터, 파트)';
COMMENT ON COLUMN departmentEntities.status IS '부서 상태 (ACTIVE: 활성, INACTIVE: 비활성)';
COMMENT ON COLUMN departmentEntities.deactivated_at IS '비활성화 일시 (상태 변경 이력 추적)';
COMMENT ON COLUMN departmentEntities.created_at IS '생성 일시';
COMMENT ON COLUMN departmentEntities.updated_at IS '최종 수정 일시';
COMMENT ON COLUMN departmentEntities.created_by IS '생성자 ID (감사 추적)';
COMMENT ON COLUMN departmentEntities.updated_by IS '최종 수정자 ID (감사 추적)';
COMMENT ON COLUMN departmentEntities.version IS '낙관적 잠금 버전 (동시성 제어용)';

-- ============================================================
-- Phase 4: 제약조건 추가
-- ============================================================

-- UNIQUE 제약: 테넌트별 부서 코드 고유
ALTER TABLE departmentEntities ADD CONSTRAINT IF NOT EXISTS uk_departments_tenant_code
    UNIQUE (tenant_id, code);

-- CHECK 제약: type 값 검증
ALTER TABLE departmentEntities ADD CONSTRAINT IF NOT EXISTS chk_departments_type
    CHECK (type IN ('COMPANY', 'DIVISION', 'TEAM', 'GROUP', 'CUSTOM', 'HEADQUARTERS'));

-- CHECK 제약: 커스텀 타입 규칙
-- type='CUSTOM'이면 custom_type_name 필수, 아니면 NULL이어야 함
ALTER TABLE departmentEntities ADD CONSTRAINT IF NOT EXISTS chk_departments_custom_type
    CHECK (
        (type = 'CUSTOM' AND custom_type_name IS NOT NULL) OR
        (type != 'CUSTOM' AND custom_type_name IS NULL)
    );

-- CHECK 제약: status 값 검증 (이미 V1_0_14에서 추가되었을 수 있음)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_department_status'
    ) THEN
        ALTER TABLE departmentEntities ADD CONSTRAINT chk_department_status
            CHECK (status IN ('ACTIVE', 'INACTIVE'));
    END IF;
END $$;

-- ============================================================
-- Phase 5: 인덱스 추가 (성능 최적화)
-- ============================================================

-- 복합 인덱스: 테넌트별 상태 조회
CREATE INDEX IF NOT EXISTS idx_departments_tenant_status
    ON departmentEntities(tenant_id, status);

-- 복합 인덱스: 테넌트별 부모 부서 조회 (조직도 구성)
CREATE INDEX IF NOT EXISTS idx_departments_tenant_parent
    ON departmentEntities(tenant_id, parent_id);

-- 인덱스: 부서 코드 조회 (검색 최적화)
CREATE INDEX IF NOT EXISTS idx_departments_code
    ON departmentEntities(code);

-- 인덱스: 부서 타입별 조회
CREATE INDEX IF NOT EXISTS idx_departments_type
    ON departmentEntities(tenant_id, type);

-- ============================================================
-- Phase 6: updated_at 자동 업데이트 트리거 생성
-- ============================================================

-- 트리거 함수: updated_at 자동 갱신
CREATE OR REPLACE FUNCTION update_departments_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 트리거 생성
DROP TRIGGER IF EXISTS trg_departments_updated_at ON departmentEntities;
CREATE TRIGGER trg_departments_updated_at
    BEFORE UPDATE ON departmentEntities
    FOR EACH ROW
    EXECUTE FUNCTION update_departments_updated_at();

-- ============================================================
-- 완료 메시지
-- ============================================================

DO $$
BEGIN
    RAISE NOTICE '✅ V1_0_16 마이그레이션 완료: departmentEntities 테이블 강화';
    RAISE NOTICE '  - code, custom_type_name 추가';
    RAISE NOTICE '  - deactivated_at, updated_at 추가';
    RAISE NOTICE '  - created_by, updated_by, version 추가';
    RAISE NOTICE '  - CHECK 제약조건 3개 추가';
    RAISE NOTICE '  - 인덱스 4개 추가';
    RAISE NOTICE '  - updated_at 자동 업데이트 트리거 생성';
END $$;
