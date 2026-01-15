# 📚 프로젝트 문서 가이드

## 📂 생성된 주요 문서

### 1. **DB_STRUCTURE.md** 
   - 데이터베이스 전체 구조
   - 테이블 관계도 (ASCII 다이어그램)
   - 각 테이블의 필드 및 컬럼 설명
   - 마이그레이션 구조

### 2. **STANDARD_DATA_GUIDE.md** ⭐
   - 권한(Permission) 표준 가이드
   - 역할(Role) 표준 정의
   - 부서(Department) 추가 규칙
   - 사용자(Agent) 추가 방법
   - 권한 그룹(Permission Group) 설명
   - 새로운 데이터 추가 예시

### 3. **STANDARD_DATA_SUMMARY.md**
   - 삽입된 표준 데이터 현황
   - 테스트 사용자 정보
   - 마이그레이션 실행 내역
   - 다음 단계 가이드

### 4. **AUDIT_LOGS_GUIDE.md** ⭐⭐ (신규)
   - 감사 로그 테이블 구조
   - ACTION 타입 표준 (CREATE, UPDATE, DELETE, ASSIGN, REVOKE)
   - RESOURCE_TYPE 표준
   - Changes 필드 JSON 형식
   - 감사 로그 조회 예시 (7가지)
   - 아카이빙 전략
   - 보안 고려사항
   - 모니터링 및 감시 방법

---

## 🚀 빠른 시작 (팀원들을 위한)

### 1️⃣ 테스트 계정으로 로그인
```
ID: admin-user
PASSWORD: admin123
ROLE: ADMIN (모든 권한)
```

### 2️⃣ 새로운 권한 추가하기
```sql
-- 1. 권한 코드 결정: {도메인}:{액션}
-- 예: reporting:schedule

-- 2. 권한 삽입
INSERT INTO permissions (permission_id, tenant_id, code, created_at)
VALUES (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'reporting:schedule', CURRENT_TIMESTAMP);

-- 3. 역할에 권한 할당 (필요시)
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT r.role_id, p.permission_id, CURRENT_TIMESTAMP
FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.code = 'reporting:schedule';
```

### 3️⃣ 새로운 역할 추가하기
```sql
-- 1. 역할 생성
INSERT INTO roles (role_id, tenant_id, name, type, description, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid()::VARCHAR(36),
    'tenant-001',
    'MANAGER',      -- 역할명
    'POSITION',     -- 역할 타입
    '매니저 권한',   -- 설명
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 2. 역할에 권한 할당
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT r.role_id, p.permission_id, CURRENT_TIMESTAMP
FROM roles r, permissions p
WHERE r.name = 'MANAGER' AND p.code IN ('user:read', 'org:read', 'report:view');
```

### 4️⃣ 새로운 사용자 추가하기
```sql
-- 1. 사용자 생성 (비밀번호는 BCrypt 해싱 필요)
INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at)
SELECT 
    gen_random_uuid()::VARCHAR(36),
    'tenant-001',
    'new-user-01',
    '$2a$10$...', -- BCrypt 해시된 비밀번호
    '새 사용자',
    d.dept_id,
    'ACTIVE',
    false,
    CURRENT_TIMESTAMP
FROM departments d
WHERE d.name = '영업부';

-- 2. 사용자에게 역할 할당
INSERT INTO agent_roles (agent_id, role_id, assigned_at)
SELECT a.agent_id, r.role_id, CURRENT_TIMESTAMP
FROM agents a, roles r
WHERE a.login_id = 'new-user-01' AND r.name = 'MANAGER';
```

---

## 📊 데이터베이스 구조 한눈에

```
👤 Agent (사용자)
  │
  ├─ agent_roles ──→ 👔 Role (역할)
  │                    │
  │                    ├─ role_permissions ──→ 🔐 Permission (권한)
  │                    │
  │                    └─ role_permission_groups ──→ 📦 Permission Group
  │                                                   └─ group_permissions ──→ 🔐 Permission
  │
  └─ 🏢 Department (부서)
```

---

## 🔐 권한 코드 표준

### 규칙: `{도메인}:{액션}`

| 도메인 | 액션 | 예시 | 의미 |
|--------|------|------|------|
| user | create, read, update, delete | user:create | 사용자 생성 |
| org | create, read, update, delete | org:read | 조직 조회 |
| role | create, update, delete | role:create | 역할 생성 |
| permission | manage | permission:manage | 권한 관리 |
| report | view, export | report:view | 보고서 조회 |
| audit | view, export | audit:view | 감사 로그 조회 |
| cdr | view | cdr:view | CDR 조회 |
| dashboard | view | dashboard:view | 대시보드 조회 |

---

## 👔 역할 타입

| 타입 | 설명 | 예시 |
|------|------|------|
| **POSITION** | 조직상 직위 | ADMIN, TEAM_LEADER, MEMBER |
| **CHANNEL** | 상담 채널 | PHONE_AGENT, CHAT_AGENT, EMAIL_AGENT |
| **SKILL** | 기술/스킬 (향후 확장) | - |

---

## 📋 표준 데이터 현황

### 삽입된 데이터

```
✅ 권한: 15개
✅ 역할: 6개
✅ 권한 그룹: 3개
✅ 부서: 3개
✅ 예시 사용자: 4명
✅ 역할-권한 매핑: 30+개
✅ 권한 그룹-권한 매핑: 20+개
✅ 사용자-역할 매핑: 4개
```

---

## 🎯 팀원들의 체크리스트

### 개발 시작 전
- [ ] 이 문서 읽기
- [ ] DB_STRUCTURE.md 확인 (DB 구조 이해)
- [ ] STANDARD_DATA_GUIDE.md 숙독 (표준 규칙 학습)
- [ ] admin-user로 로그인해서 기본 데이터 확인

### 새 기능 개발 시
- [ ] 필요한 권한이 있는지 확인
- [ ] 없으면 표준 규칙에 따라 추가
- [ ] 역할에 권한 매핑
- [ ] 테스트 사용자로 권한 확인

### 새 사용자 추가 시
- [ ] 부서 확인
- [ ] login_id 중복 확인
- [ ] 비밀번호 BCrypt 해싱
- [ ] 역할 할당

---

## 🔗 마이그레이션 버전

| 버전 | 파일명 | 내용 |
|------|--------|------|
| V1_0_0 | Complete_Init.sql | 기본 테이블 생성 |
| V1_0_4 | Add_AuditLog_Table.sql | 감사 로그 테이블 추가 |
| V1_0_5 | Add_Role_IsActive_Description.sql | 역할 상태 추가 |
| V1_0_6 | Add_Permission_Groups.sql | 권한 그룹 기능 추가 |
| V1_0_7 | Add_Audit_Log_Archiving.sql | 감사 로그 아카이빙 추가 |
| V1_0_8 | RBAC_Complete_Integration.sql | (비어있음 - 히스토리용) |
| V1_0_9 | Insert_Standard_Data.sql | 기본 표준 데이터 삽입 |
| V1_0_10 | Extend_Standard_Data.sql | 표준 데이터 확장 |
| V1_0_11 | Insert_Audit_Log_Examples.sql | ⭐ 감사 로그 예시 데이터 (현재) |

---

## 📞 문의 및 수정

### 권한 추가 전
1. STANDARD_DATA_GUIDE.md의 "권한 코드 규칙" 확인
2. 도메인과 액션이 표준을 따르는지 검증
3. 팀 내 합의 후 추가

### 역할 추가 전
1. 역할 타입 결정 (POSITION, CHANNEL, SKILL)
2. 포함할 권한 목록 검토
3. 기존 역할과 중복되지 않는지 확인

### 새 도메인 추가 시
1. 팀 회의를 통해 도메인 정의
2. 관련 권한 나열
3. 새 마이그레이션 파일 작성
4. 이 문서 및 STANDARD_DATA_GUIDE.md 업데이트

---

**작성일**: 2026-01-15
**최종 업데이트**: V1_0_10 (표준 데이터 확장)

