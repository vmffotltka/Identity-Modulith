-- V1_0_15__Fix_Agents_Table_Critical.sql
-- agents 테이블 치명적 오류 수정 및 필수 컬럼 추가
-- 우선순위: P0 (즉시 필요)

-- ============================================================
-- Phase 1: 설계 오류 수정
-- ============================================================

-- 1. agents.role_id 컬럼 제거 (M:N 관계 오류)
-- 이미 agent_roles 테이블로 M:N 매핑하고 있으므로 중복
ALTER TABLE agents DROP COLUMN IF EXISTS role_id;

-- 2. 사용하지 않는 컬럼 제거
ALTER TABLE agents DROP COLUMN IF EXISTS job_title;
ALTER TABLE agents DROP COLUMN IF EXISTS sync_status;

COMMENT ON TABLE agents IS '사용자 (에이전트) 테이블 - role_id 제거, 연락처 및 감사 컬럼 추가';

-- ============================================================
-- Phase 2: 필수 컬럼 추가
-- ============================================================

-- 3. 연락처 정보 (HR 연동, 알림 발송 필수)
ALTER TABLE agents ADD COLUMN IF NOT EXISTS employee_id VARCHAR(30);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS phone VARCHAR(20);

-- 4. 상태 추적 컬럼
ALTER TABLE agents ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMP;
ALTER TABLE agents ADD COLUMN IF NOT EXISTS scheduled_delete_at TIMESTAMP;

-- 5. 감사 추적 컬럼
ALTER TABLE agents ADD COLUMN IF NOT EXISTS created_by VARCHAR(36);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS updated_by VARCHAR(36);

-- 6. 낙관적 잠금 (동시성 제어)
ALTER TABLE agents ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;

-- ============================================================
-- Phase 3: 코멘트 추가
-- ============================================================

COMMENT ON COLUMN agents.agent_id IS '사용자 ID (UUID)';
COMMENT ON COLUMN agents.tenant_id IS '테넌트 ID (멀티테넌시)';
COMMENT ON COLUMN agents.login_id IS '로그인 ID (고유)';
COMMENT ON COLUMN agents.password IS '비밀번호 (BCrypt 해시)';
COMMENT ON COLUMN agents.name IS '사용자명';
COMMENT ON COLUMN agents.employee_id IS '사번 (선택, HR 시스템 연동용)';
COMMENT ON COLUMN agents.email IS '이메일 주소 (알림 발송, 계정 복구)';
COMMENT ON COLUMN agents.phone IS '전화번호 (본인 확인, 연락처)';
COMMENT ON COLUMN agents.dept_id IS '소속 부서 ID (UUID, FK → departmentEntities)';
COMMENT ON COLUMN agents.status IS '상태 (ACTIVE: 활성, SUSPENDED: 정지, RETIRED: 퇴사)';
COMMENT ON COLUMN agents.password_must_change IS '비밀번호 변경 필요 여부 (임시 비밀번호 발급 시 true)';
COMMENT ON COLUMN agents.suspended_at IS '정지 일시 (상태 변경 이력 추적)';
COMMENT ON COLUMN agents.retired_at IS '퇴사 일시 (상태 변경 이력 추적)';
COMMENT ON COLUMN agents.scheduled_delete_at IS '예약 삭제 일시 (GDPR 준수, 데이터 보존 기간 관리)';
COMMENT ON COLUMN agents.created_at IS '생성 일시';
COMMENT ON COLUMN agents.updated_at IS '최종 수정 일시';
COMMENT ON COLUMN agents.created_by IS '생성자 ID (감사 추적)';
COMMENT ON COLUMN agents.updated_by IS '최종 수정자 ID (감사 추적)';
COMMENT ON COLUMN agents.version IS '낙관적 잠금 버전 (동시성 제어용, 업데이트마다 증가)';

-- ============================================================
-- Phase 4: 제약조건 추가
-- ============================================================

-- CHECK 제약조건: status 값 검증
ALTER TABLE agents ADD CONSTRAINT IF NOT EXISTS chk_agents_status
    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'RETIRED'));

-- CHECK 제약조건: email 형식 검증 (선택 - 기본 검증)
ALTER TABLE agents ADD CONSTRAINT IF NOT EXISTS chk_agents_email_format
    CHECK (email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$');

-- CHECK 제약조건: phone 형식 검증 (선택)
ALTER TABLE agents ADD CONSTRAINT IF NOT EXISTS chk_agents_phone_format
    CHECK (phone IS NULL OR phone ~ '^[0-9\-\+\(\) ]+$');

-- ============================================================
-- Phase 5: 인덱스 추가 (성능 최적화)
-- ============================================================

-- 복합 인덱스: 테넌트별 상태 조회 (자주 사용)
CREATE INDEX IF NOT EXISTS idx_agents_tenant_status
    ON agents(tenant_id, status);

-- 복합 인덱스: 테넌트별 부서 조회 (자주 사용)
CREATE INDEX IF NOT EXISTS idx_agents_tenant_dept
    ON agents(tenant_id, dept_id);

-- 부분 인덱스: 예약 삭제 대상만 (GDPR 준수 자동화)
CREATE INDEX IF NOT EXISTS idx_agents_scheduled_delete
    ON agents(scheduled_delete_at)
    WHERE scheduled_delete_at IS NOT NULL;

-- 인덱스: 이메일 조회 (계정 복구, 알림 발송)
CREATE INDEX IF NOT EXISTS idx_agents_email
    ON agents(email)
    WHERE email IS NOT NULL;

-- 인덱스: 사번 조회 (HR 연동)
CREATE INDEX IF NOT EXISTS idx_agents_employee_id
    ON agents(employee_id)
    WHERE employee_id IS NOT NULL;

-- ============================================================
-- Phase 6: 기존 데이터 처리
-- ============================================================

-- 기존 agent의 created_by, updated_by를 시스템 계정으로 설정
UPDATE agents
SET created_by = 'SYSTEM',
    updated_by = 'SYSTEM'
WHERE created_by IS NULL;

-- ============================================================
-- 완료 메시지
-- ============================================================

DO $$
BEGIN
    RAISE NOTICE '✅ V1_0_15 마이그레이션 완료: agents 테이블 수정';
    RAISE NOTICE '  - agents.role_id 제거 (설계 오류 수정)';
    RAISE NOTICE '  - email, phone, employee_id 추가';
    RAISE NOTICE '  - suspended_at, scheduled_delete_at 추가';
    RAISE NOTICE '  - created_by, updated_by, version 추가';
    RAISE NOTICE '  - CHECK 제약조건 3개 추가';
    RAISE NOTICE '  - 인덱스 5개 추가';
END $$;
