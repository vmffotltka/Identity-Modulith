-- V1_0_14__Add_Department_Status.sql
-- 부서 상태 관리 기능 추가

-- departments 테이블에 status 컬럼 추가
ALTER TABLE departments
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- status 컬럼에 CHECK 제약조건 추가
ALTER TABLE departments
ADD CONSTRAINT chk_department_status CHECK (status IN ('ACTIVE', 'INACTIVE'));

-- status 컬럼에 인덱스 추가 (성능 최적화)
CREATE INDEX idx_departments_status ON departments(status);

-- 복합 인덱스 추가 (tenant_id + status) - 활성 부서 조회 최적화
CREATE INDEX idx_departments_tenant_status ON departments(tenant_id, status);

COMMENT ON COLUMN departments.status IS '부서 상태 (ACTIVE: 활성, INACTIVE: 비활성)';
