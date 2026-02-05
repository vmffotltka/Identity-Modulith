-- V1_0_17__Enhance_Permissions_Table.sql
-- permissions 테이블 강화 - 메타데이터 컬럼 추가
-- 우선순위: P0 (즉시 필요)

-- ============================================================
-- Phase 1: 누락된 컬럼 추가
-- ============================================================

-- 1. 권한 표시명 (UI 표시용)
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS name VARCHAR(100);

-- 2. 권한 설명 (목적 및 범위 설명)
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS description VARCHAR(255);

-- 3. 권한 분류 (그룹핑용)
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS category VARCHAR(30);

-- ============================================================
-- Phase 2: 기존 데이터 처리
-- ============================================================

-- 기존 권한 데이터에 메타데이터 설정
UPDATE permissions
SET name = CASE
        -- Agent 관련
        WHEN code = 'agent:create' THEN '상담사 생성'
        WHEN code = 'agent:read' THEN '상담사 조회'
        WHEN code = 'agent:read:self' THEN '본인 정보 조회'
        WHEN code = 'agent:update' THEN '상담사 수정'
        WHEN code = 'agent:update:self' THEN '본인 정보 수정'
        WHEN code = 'agent:delete' THEN '상담사 삭제'
        WHEN code = 'agent:suspend' THEN '상담사 정지'
        WHEN code = 'agent:transfer' THEN '상담사 이동'
        WHEN code = 'agent:role:assign' THEN '역할 할당'
        WHEN code = 'agent:password:reset' THEN '비밀번호 초기화'
        -- Department 관련
        WHEN code = 'dept:create' THEN '부서 생성'
        WHEN code = 'dept:read' THEN '부서 조회'
        WHEN code = 'dept:update' THEN '부서 수정'
        WHEN code = 'dept:delete' THEN '부서 삭제'
        WHEN code = 'dept:move' THEN '부서 이동'
        WHEN code = 'dept:deactivate' THEN '부서 비활성화'
        -- Role/Permission 관련
        WHEN code = 'role:create' THEN '역할 생성'
        WHEN code = 'role:read' THEN '역할 조회'
        WHEN code = 'role:update' THEN '역할 수정'
        WHEN code = 'role:delete' THEN '역할 삭제'
        WHEN code = 'permission:read' THEN '권한 조회'
        WHEN code = 'permission:assign' THEN '권한 할당'
        -- Call 관련
        WHEN code = 'call:receive' THEN '전화 수신'
        WHEN code = 'call:dial' THEN '전화 발신'
        WHEN code = 'call:transfer' THEN '전화 전환'
        WHEN code = 'call:hold' THEN '전화 보류'
        -- Chat 관련
        WHEN code = 'chat:receive' THEN '채팅 수신'
        WHEN code = 'chat:send' THEN '채팅 발송'
        WHEN code = 'chat:transfer' THEN '채팅 전환'
        -- Email 관련
        WHEN code = 'email:receive' THEN '이메일 수신'
        WHEN code = 'email:send' THEN '이메일 발송'
        -- Callback 관련
        WHEN code = 'callback:create' THEN '콜백 생성'
        WHEN code = 'callback:manage' THEN '콜백 관리'
        -- Report 관련
        WHEN code = 'report:view' THEN '리포트 조회'
        WHEN code = 'report:export' THEN '리포트 내보내기'
        WHEN code = 'report:create' THEN '리포트 생성'
        ELSE code  -- 기본값: code 그대로
    END,
    description = CASE
        -- Agent 관련
        WHEN code = 'agent:create' THEN '새로운 상담사 계정 생성'
        WHEN code = 'agent:read' THEN '상담사 정보 조회 (관리자용)'
        WHEN code = 'agent:read:self' THEN '본인 정보만 조회'
        WHEN code = 'agent:update' THEN '상담사 정보 수정 (관리자용)'
        WHEN code = 'agent:update:self' THEN '본인 정보만 수정'
        WHEN code = 'agent:delete' THEN '상담사 계정 삭제 또는 퇴사 처리'
        WHEN code = 'agent:suspend' THEN '상담사 계정 일시 정지'
        WHEN code = 'agent:transfer' THEN '상담사 부서 이동'
        WHEN code = 'agent:role:assign' THEN '상담사에게 역할 할당'
        WHEN code = 'agent:password:reset' THEN '비밀번호 강제 초기화'
        -- Department 관련
        WHEN code = 'dept:create' THEN '새로운 부서 생성'
        WHEN code = 'dept:read' THEN '부서 정보 조회'
        WHEN code = 'dept:update' THEN '부서 정보 수정'
        WHEN code = 'dept:delete' THEN '부서 삭제'
        WHEN code = 'dept:move' THEN '부서 계층 구조 변경'
        WHEN code = 'dept:deactivate' THEN '부서 비활성화/활성화'
        -- Role/Permission 관련
        WHEN code = 'role:create' THEN '새로운 역할 생성'
        WHEN code = 'role:read' THEN '역할 정보 조회'
        WHEN code = 'role:update' THEN '역할 정보 수정'
        WHEN code = 'role:delete' THEN '역할 삭제'
        WHEN code = 'permission:read' THEN '권한 목록 조회'
        WHEN code = 'permission:assign' THEN '역할에 권한 할당'
        -- Call 관련
        WHEN code = 'call:receive' THEN '인바운드 전화 수신'
        WHEN code = 'call:dial' THEN '아웃바운드 전화 발신'
        WHEN code = 'call:transfer' THEN '통화 전환 (다른 상담사로)'
        WHEN code = 'call:hold' THEN '통화 보류'
        -- Chat 관련
        WHEN code = 'chat:receive' THEN '채팅 상담 수신'
        WHEN code = 'chat:send' THEN '채팅 메시지 발송'
        WHEN code = 'chat:transfer' THEN '채팅 전환 (다른 상담사로)'
        -- Email 관련
        WHEN code = 'email:receive' THEN '이메일 상담 수신'
        WHEN code = 'email:send' THEN '이메일 회신 발송'
        -- Callback 관련
        WHEN code = 'callback:create' THEN '콜백 요청 생성'
        WHEN code = 'callback:manage' THEN '콜백 목록 관리'
        -- Report 관련
        WHEN code = 'report:view' THEN '리포트 및 통계 조회'
        WHEN code = 'report:export' THEN '리포트 데이터 내보내기'
        WHEN code = 'report:create' THEN '커스텀 리포트 생성'
        ELSE '시스템 권한'
    END,
    category = UPPER(SPLIT_PART(code, ':', 1))  -- 'agent:create' → 'AGENT'
WHERE name IS NULL;

-- name, category를 NOT NULL로 설정
ALTER TABLE permissions ALTER COLUMN name SET NOT NULL;
ALTER TABLE permissions ALTER COLUMN category SET NOT NULL;

-- ============================================================
-- Phase 3: 코멘트 추가
-- ============================================================

COMMENT ON TABLE permissions IS '권한 관리 테이블 - 시스템의 모든 권한 정의';

COMMENT ON COLUMN permissions.permission_id IS '권한 ID (UUID)';
COMMENT ON COLUMN permissions.tenant_id IS '테넌트 ID (멀티테넌시)';
COMMENT ON COLUMN permissions.code IS '권한 코드 (도메인:액션 형식, 예: agent:create)';
COMMENT ON COLUMN permissions.name IS '권한 표시명 (UI 표시용, 예: 상담사 생성)';
COMMENT ON COLUMN permissions.description IS '권한 설명 (목적 및 범위 설명)';
COMMENT ON COLUMN permissions.category IS '권한 분류 (AGENT, DEPT, ROLE, CALL, CHAT, EMAIL 등)';
COMMENT ON COLUMN permissions.created_at IS '생성 일시';

-- ============================================================
-- Phase 4: 제약조건 추가
-- ============================================================

-- CHECK 제약: category 값 검증
ALTER TABLE permissions ADD CONSTRAINT IF NOT EXISTS chk_permissions_category
    CHECK (category IN (
        'AGENT', 'DEPT', 'ROLE', 'PERMISSION',
        'CALL', 'CHAT', 'EMAIL', 'CALLBACK',
        'REPORT', 'SYSTEM'
    ));

-- CHECK 제약: code 형식 검증 (domain:action)
ALTER TABLE permissions ADD CONSTRAINT IF NOT EXISTS chk_permissions_code_format
    CHECK (code ~* '^[a-z]+:[a-z:]+$');

-- ============================================================
-- Phase 5: 인덱스 추가 (성능 최적화)
-- ============================================================

-- 인덱스: 권한 분류별 조회 (권한 그룹핑)
CREATE INDEX IF NOT EXISTS idx_permissions_category
    ON permissions(category);

-- 복합 인덱스: 테넌트별 분류 조회
CREATE INDEX IF NOT EXISTS idx_permissions_tenant_category
    ON permissions(tenant_id, category);

-- 인덱스: 권한명 검색 (검색 최적화)
CREATE INDEX IF NOT EXISTS idx_permissions_name
    ON permissions(name);

-- ============================================================
-- Phase 6: 권한 카테고리 통계 뷰 생성 (선택)
-- ============================================================

-- 권한 카테고리별 통계 뷰
CREATE OR REPLACE VIEW v_permission_statistics AS
SELECT
    tenant_id,
    category,
    COUNT(*) AS permission_count,
    array_agg(name ORDER BY code) AS permission_names
FROM permissions
GROUP BY tenant_id, category
ORDER BY tenant_id, category;

COMMENT ON VIEW v_permission_statistics IS '권한 카테고리별 통계 (대시보드용)';

-- ============================================================
-- 완료 메시지
-- ============================================================

DO $$
BEGIN
    RAISE NOTICE '✅ V1_0_17 마이그레이션 완료: permissions 테이블 강화';
    RAISE NOTICE '  - name, description, category 추가';
    RAISE NOTICE '  - 기존 36개 권한에 메타데이터 자동 설정';
    RAISE NOTICE '  - CHECK 제약조건 2개 추가';
    RAISE NOTICE '  - 인덱스 3개 추가';
    RAISE NOTICE '  - 권한 통계 뷰 생성';
END $$;
