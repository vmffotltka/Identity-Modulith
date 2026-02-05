-- V1_0_21__Convert_Department_Type_To_Enum.sql
-- DepartmentType을 String에서 Enum으로 변환
-- EVENT_STORMING.md 명세 준수: COMPANY, DIVISION, TEAM, GROUP, CUSTOM

-- ============================================================
-- 1. 기존 데이터 검증 및 표준화
-- ============================================================

-- 현재 type 값 확인 (마이그레이션 전 검증용)
DO $$
DECLARE
    invalid_count INTEGER;
    distinct_types TEXT;
BEGIN
    -- 현재 존재하는 distinct type 값 확인
    SELECT string_agg(DISTINCT COALESCE(type, 'NULL'), ', ')
    INTO distinct_types
    FROM departmentEntities;

    RAISE NOTICE '현재 type 값들: %', distinct_types;

    -- NULL이거나 빈 문자열인 경우 기본값 설정
    UPDATE departmentEntities
    SET type = 'TEAM'
    WHERE type IS NULL OR TRIM(type) = '';

    -- 표준 타입으로 변환 (대소문자 무시)
    UPDATE departmentEntities
    SET type = CASE
        -- 회사 관련
        WHEN UPPER(type) = 'COMPANY' THEN 'COMPANY'
        WHEN UPPER(type) IN ('본사', '회사', 'HEADQUARTERS', 'HEAD_OFFICE') THEN 'COMPANY'

        -- 본부/사업부 관련
        WHEN UPPER(type) = 'DIVISION' THEN 'DIVISION'
        WHEN UPPER(type) IN ('본부', '사업부', '사업본부', 'DEPT', 'DEPARTMENT') THEN 'DIVISION'

        -- 팀 관련
        WHEN UPPER(type) = 'TEAM' THEN 'TEAM'
        WHEN UPPER(type) IN ('팀', 'UNIT') THEN 'TEAM'

        -- 그룹/파트 관련
        WHEN UPPER(type) = 'GROUP' THEN 'GROUP'
        WHEN UPPER(type) IN ('그룹', '파트', 'PART', 'SECTION') THEN 'GROUP'

        -- 커스텀 관련
        WHEN UPPER(type) = 'CUSTOM' THEN 'CUSTOM'
        WHEN UPPER(type) IN ('센터', '지점', '지사', '사무소', 'CENTER', 'BRANCH', 'OFFICE') THEN 'CUSTOM'

        -- 기타 (CUSTOM으로 분류)
        ELSE 'CUSTOM'
    END;

    -- CUSTOM으로 변환된 경우 customTypeName 설정
    UPDATE departmentEntities
    SET custom_type_name = CASE
        WHEN type = 'CUSTOM' AND (custom_type_name IS NULL OR TRIM(custom_type_name) = '')
        THEN '기타'  -- 기본값
        ELSE custom_type_name
    END
    WHERE type = 'CUSTOM';

    -- 유효하지 않은 값 확인
    SELECT COUNT(*)
    INTO invalid_count
    FROM departmentEntities
    WHERE type NOT IN ('COMPANY', 'DIVISION', 'TEAM', 'GROUP', 'CUSTOM');

    IF invalid_count > 0 THEN
        RAISE EXCEPTION '유효하지 않은 type 값이 % 건 존재합니다. 마이그레이션을 중단합니다.', invalid_count;
    END IF;

    RAISE NOTICE '부서 타입 표준화 완료. CUSTOM 타입: % 건',
        (SELECT COUNT(*) FROM departmentEntities WHERE type = 'CUSTOM');
END $$;

-- ============================================================
-- 2. 컬럼 타입 변경 및 제약조건 추가
-- ============================================================

-- type 컬럼에 CHECK 제약조건 추가
ALTER TABLE departmentEntities
DROP CONSTRAINT IF EXISTS chk_department_type;

ALTER TABLE departmentEntities
ADD CONSTRAINT chk_department_type CHECK (
    type IN ('COMPANY', 'DIVISION', 'TEAM', 'GROUP', 'CUSTOM')
);

-- type을 NOT NULL로 변경 (이미 기본값이 설정되어 있음)
ALTER TABLE departmentEntities
ALTER COLUMN type SET NOT NULL;

-- type 길이 제한 (VARCHAR(20)으로 제한)
ALTER TABLE departmentEntities
ALTER COLUMN type TYPE VARCHAR(20);

-- ============================================================
-- 3. CUSTOM 타입 검증 강화
-- ============================================================

-- CUSTOM 타입인 경우 customTypeName 필수 (CHECK 제약조건)
ALTER TABLE departmentEntities
DROP CONSTRAINT IF EXISTS chk_custom_type_name;

ALTER TABLE departmentEntities
ADD CONSTRAINT chk_custom_type_name CHECK (
    (type = 'CUSTOM' AND custom_type_name IS NOT NULL AND TRIM(custom_type_name) <> '')
    OR (type <> 'CUSTOM')
);

-- ============================================================
-- 4. 인덱스 추가 (타입별 조회 최적화)
-- ============================================================

-- 타입별 조회 인덱스
CREATE INDEX IF NOT EXISTS idx_departments_type
ON departmentEntities(type);

-- 테넌트+타입 복합 인덱스 (가장 자주 사용)
CREATE INDEX IF NOT EXISTS idx_departments_tenant_type
ON departmentEntities(tenant_id, type);

-- 커스텀 타입 조회 최적화 (부분 인덱스)
CREATE INDEX IF NOT EXISTS idx_departments_custom_type
ON departmentEntities(tenant_id, custom_type_name)
WHERE type = 'CUSTOM';

-- ============================================================
-- 5. 주석 업데이트
-- ============================================================

COMMENT ON COLUMN departmentEntities.type IS
'부서 타입 (COMPANY: 회사, DIVISION: 본부, TEAM: 팀, GROUP: 그룹, CUSTOM: 커스텀)';

COMMENT ON COLUMN departmentEntities.custom_type_name IS
'커스텀 타입명 (type=CUSTOM일 때 필수, 예: 센터, 지점, 사무소)';

COMMENT ON CONSTRAINT chk_department_type ON departmentEntities IS
'부서 타입 제약 (5가지 표준 타입만 허용)';

COMMENT ON CONSTRAINT chk_custom_type_name ON departmentEntities IS
'CUSTOM 타입인 경우 customTypeName 필수';

-- ============================================================
-- 6. 검증 쿼리 (마이그레이션 후 확인용)
-- ============================================================

DO $$
DECLARE
    company_count INTEGER;
    division_count INTEGER;
    team_count INTEGER;
    group_count INTEGER;
    custom_count INTEGER;
BEGIN
    SELECT
        COUNT(*) FILTER (WHERE type = 'COMPANY'),
        COUNT(*) FILTER (WHERE type = 'DIVISION'),
        COUNT(*) FILTER (WHERE type = 'TEAM'),
        COUNT(*) FILTER (WHERE type = 'GROUP'),
        COUNT(*) FILTER (WHERE type = 'CUSTOM')
    INTO company_count, division_count, team_count, group_count, custom_count
    FROM departmentEntities;

    RAISE NOTICE '=== 부서 타입 분포 ===';
    RAISE NOTICE 'COMPANY: % 건', company_count;
    RAISE NOTICE 'DIVISION: % 건', division_count;
    RAISE NOTICE 'TEAM: % 건', team_count;
    RAISE NOTICE 'GROUP: % 건', group_count;
    RAISE NOTICE 'CUSTOM: % 건', custom_count;
    RAISE NOTICE '총 부서: % 건', (company_count + division_count + team_count + group_count + custom_count);

    -- CUSTOM 타입의 customTypeName 확인
    RAISE NOTICE '=== CUSTOM 타입 상세 ===';
    FOR rec IN
        SELECT dept_id, name, custom_type_name
        FROM departmentEntities
        WHERE type = 'CUSTOM'
        LIMIT 10
    LOOP
        RAISE NOTICE 'dept_id: %, name: %, customTypeName: %',
            rec.dept_id, rec.name, rec.custom_type_name;
    END LOOP;
END $$;
