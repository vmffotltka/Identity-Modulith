-- =============================================================================
-- 데이터베이스 스키마 검증 스크립트
-- 목적: 엔티티와 SQL 스키마 일치 확인
-- 작성일: 2026-02-07
-- =============================================================================

-- 1. 테이블 존재 확인
SELECT
    '테이블 존재 확인' AS 검증항목,
    table_name AS 테이블명,
    CASE
        WHEN table_name = 'rbac_agent_roles' THEN '✅ 엔티티와 일치 (RBAC 모듈)'
        WHEN table_name = 'user_agent_roles' THEN '❌ 불일치 (rbac_agent_roles 여야 함)'
        ELSE '✅ 정상'
    END AS 상태
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
    'org_departments',
    'user_agents',
    'rbac_roles',
    'rbac_permissions',
    'rbac_role_permissions',
    'rbac_agent_roles',
    'user_agent_roles'  -- 이 테이블이 있으면 안됨
  )
ORDER BY table_name;

-- 2. rbac_agent_roles 테이블 컬럼 확인
SELECT
    'rbac_agent_roles 컬럼' AS 검증항목,
    column_name AS 컬럼명,
    data_type AS 데이터타입,
    is_nullable AS NULL가능,
    column_default AS 기본값
FROM information_schema.columns
WHERE table_name = 'rbac_agent_roles'
ORDER BY ordinal_position;

-- 3. rbac_role_permissions 테이블 컬럼 확인
SELECT
    'rbac_role_permissions 컬럼' AS 검증항목,
    column_name AS 컬럼명,
    data_type AS 데이터타입,
    is_nullable AS NULL가능,
    column_default AS 기본값
FROM information_schema.columns
WHERE table_name = 'rbac_role_permissions'
ORDER BY ordinal_position;

-- 4. rbac_agent_roles assigned_at 컬럼 존재 확인
SELECT
    'assigned_at 컬럼 확인' AS 검증항목,
    table_name AS 테이블명,
    column_name AS 컬럼명,
    CASE
        WHEN column_name = 'assigned_at' THEN '✅ 존재함'
        ELSE '❌ 없음'
    END AS 상태
FROM information_schema.columns
WHERE table_name IN ('rbac_agent_roles', 'rbac_role_permissions')
  AND column_name = 'assigned_at';

-- 5. rbac_agent_roles 제약 조건 확인
SELECT
    'rbac_agent_roles 제약조건' AS 검증항목,
    constraint_name AS 제약조건명,
    constraint_type AS 타입
FROM information_schema.table_constraints
WHERE table_name = 'rbac_agent_roles'
ORDER BY constraint_type, constraint_name;

-- 6. rbac_agent_roles 인덱스 확인
SELECT
    'rbac_agent_roles 인덱스' AS 검증항목,
    indexname AS 인덱스명,
    indexdef AS 정의
FROM pg_indexes
WHERE tablename = 'rbac_agent_roles'
ORDER BY indexname;

-- 7. rbac_role_permissions 인덱스 확인
SELECT
    'rbac_role_permissions 인덱스' AS 검증항목,
    indexname AS 인덱스명,
    indexdef AS 정의
FROM pg_indexes
WHERE tablename = 'rbac_role_permissions'
ORDER BY indexname;

-- 8. 외래 키 관계 확인
SELECT
    '외래키 확인' AS 검증항목,
    tc.table_name AS 테이블명,
    kcu.column_name AS 컬럼명,
    ccu.table_name AS 참조테이블,
    ccu.column_name AS 참조컬럼,
    tc.constraint_name AS 제약조건명
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
  AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
  AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_name IN ('rbac_agent_roles', 'rbac_role_permissions')
ORDER BY tc.table_name, kcu.column_name;

-- 9. 데이터 확인 (샘플)
SELECT
    '데이터 샘플' AS 검증항목,
    COUNT(*) AS 레코드수
FROM rbac_agent_roles;

SELECT
    '역할-권한 매핑 데이터' AS 검증항목,
    COUNT(*) AS 레코드수
FROM rbac_role_permissions;

-- 10. 최종 검증 요약
SELECT
    '=== 최종 검증 요약 ===' AS 구분,
    '' AS 내용
UNION ALL
SELECT
    '1. rbac_agent_roles 테이블',
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'rbac_agent_roles')
        THEN '✅ 존재'
        ELSE '❌ 없음'
    END
UNION ALL
SELECT
    '2. assigned_at 컬럼 (rbac_agent_roles)',
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'rbac_agent_roles' AND column_name = 'assigned_at')
        THEN '✅ 존재'
        ELSE '❌ 없음'
    END
UNION ALL
SELECT
    '3. assigned_at 컬럼 (rbac_role_permissions)',
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'rbac_role_permissions' AND column_name = 'assigned_at')
        THEN '✅ 존재'
        ELSE '❌ 없음'
    END
UNION ALL
SELECT
    '4. user_agent_roles 테이블 (있으면 안됨)',
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_agent_roles')
        THEN '❌ 아직 존재함 (마이그레이션 필요)'
        ELSE '✅ 없음 (정상)'
    END
UNION ALL
SELECT
    '5. FK 제약조건 (rbac_agent_roles)',
    CASE
        WHEN (SELECT COUNT(*) FROM information_schema.table_constraints
              WHERE table_name = 'rbac_agent_roles' AND constraint_type = 'FOREIGN KEY') >= 2
        THEN '✅ 정상 (2개 이상)'
        ELSE '❌ 부족'
    END
UNION ALL
SELECT
    '6. UNIQUE 제약조건 (rbac_agent_roles)',
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.table_constraints
                     WHERE table_name = 'rbac_agent_roles'
                     AND constraint_type = 'UNIQUE'
                     AND constraint_name LIKE '%agent%role%')
        THEN '✅ 정상'
        ELSE '❌ 없음'
    END;

-- 끝

