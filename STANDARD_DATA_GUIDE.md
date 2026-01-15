# 📊 표준 데이터 가이드

## 개요
이 문서는 Identity Modulith 프로젝트의 표준 데이터 구조와 예시를 설명합니다.
팀원들이 이를 참고하여 새로운 권한, 역할, 사용자를 추가할 때 일관된 구조를 유지할 수 있습니다.

---

## 📋 마이그레이션 구성

### V1_0_9: 기본 표준 데이터
```
- 권한(Permissions): 10개 기본 권한
- 역할(Roles): 6개 기본 역할
- 권한 그룹(Permission Groups): 3개 기본 그룹
- 부서(Departments): 3개 기본 부서
```

### V1_0_10: 표준 데이터 확장 (상세 매핑)
```
- 추가 권한: 5개 (총 15개)
- 역할-권한 매핑: ADMIN, TEAM_LEADER, MEMBER, 상담사들
- 권한 그룹-권한 매핑: 그룹별 권한 구성
- 역할-권한 그룹 매핑: 역할별 그룹 할당
- 예시 사용자: 4명 (admin, team-leader, phone-agent, chat-agent)
- 사용자-역할 매핑: 각 사용자에 역할 할당
```

---

## 🔐 권한(Permission) 표준

### 권한 코드 규칙
**형식**: `{도메인}:{액션}`

### 도메인별 권한 목록

#### 1. 사용자 관리 (user, agent)
```
- user:create      | 사용자 생성
- user:read        | 사용자 조회
- user:update      | 사용자 수정
- user:delete      | 사용자 삭제
- agent:create     | 에이전트 생성
- agent:update     | 에이전트 수정
- agent:delete     | 에이전트 삭제
```

#### 2. 조직 관리 (org, department)
```
- org:create       | 조직 생성
- org:read         | 조직 조회
- org:update       | 조직 수정
- org:delete       | 조직 삭제
- department:create | 부서 생성
- department:update | 부서 수정
- department:delete | 부서 삭제
```

#### 3. 권한 관리 (role, permission)
```
- role:create      | 역할 생성
- role:update      | 역할 수정
- role:delete      | 역할 삭제
- permission:manage | 권한 관리
```

#### 4. 보고서 및 감시 (report, audit, cdr)
```
- report:view      | 보고서 조회
- report:export    | 보고서 내보내기
- audit:view       | 감사 로그 조회
- audit:export     | 감사 로그 내보내기
- cdr:view         | CDR 조회
```

#### 5. 대시보드
```
- dashboard:view   | 대시보드 조회
- quality:manage   | 품질 관리
```

---

## 👥 역할(Role) 표준

### 역할 타입
- **POSITION**: 조직상 직위 (ADMIN, TEAM_LEADER, MEMBER)
- **CHANNEL**: 상담 채널 (PHONE_AGENT, CHAT_AGENT, EMAIL_AGENT)
- **SKILL**: 기술/스킬 (향후 확장용)

### 기본 역할 정의

#### 1. ADMIN (POSITION)
```
설명: 시스템 전체를 관리하는 최고 관리자 권한
권한: 모든 권한 (user, org, role, permission, report, audit 등)
권한 그룹: USER_FULL_ACCESS, ORGANIZATION_FULL_ACCESS, REPORTING_ACCESS
```

#### 2. TEAM_LEADER (POSITION)
```
설명: 팀을 관리하고 팀원의 업무를 지원하는 권한
권한: 팀원 조회/수정, 보고서 조회/내보내기, 감사 로그 조회, CDR 조회
권한 그룹: USER_FULL_ACCESS, REPORTING_ACCESS
```

#### 3. MEMBER (POSITION)
```
설명: 일반 구성원 권한
권한: 사용자 조회, 조직 조회, 보고서 조회, CDR 조회, 대시보드 조회
```

#### 4. PHONE_AGENT (CHANNEL)
```
설명: 전화 상담을 수행하는 상담사 권한
권한: 조회 권한 (user:read, org:read, report:view, cdr:view, dashboard:view)
```

#### 5. CHAT_AGENT (CHANNEL)
```
설명: 채팅 상담을 수행하는 상담사 권한
권한: 조회 권한 (user:read, org:read, report:view, cdr:view, dashboard:view)
```

#### 6. EMAIL_AGENT (CHANNEL)
```
설명: 이메일 상담을 수행하는 상담사 권한
권한: 조회 권한 (user:read, org:read, report:view, dashboard:view)
```

---

## 🏢 부서(Department) 표준

### 부서 구조 (트리 구조)
```
본사 (depth=1, org_path=/001)
├── 영업부 (depth=2, org_path=/001/002)
└── 기술부 (depth=2, org_path=/001/003)
```

### 부서 타입
- **COMPANY**: 회사/최상위 조직
- **DEPARTMENT**: 부서

### 부서 추가 시 규칙
1. **org_path 생성**: 계층 구조를 반영한 경로 (예: `/001/002/003`)
2. **depth 설정**: 루트부터의 깊이 (본사=1, 부서=2, 팀=3)
3. **parent_id 설정**: 상위 부서의 ID (루트는 NULL)
4. **type 지정**: COMPANY 또는 DEPARTMENT

---

## 👤 사용자(Agent) 표준

### 예시 사용자

#### 1. 관리자 (admin-user)
```
Login ID: admin-user
Password: admin123 (BCrypt: $2a$10$slYQmyNdGzin7olVN3p5HOpsvhjUefTWGQT1qfJiXlQ8DfXWa7j8G)
Name: 관리자
Department: 본사
Status: ACTIVE
Role: ADMIN
```

#### 2. 팀장 (team-leader-01)
```
Login ID: team-leader-01
Password: admin123 (BCrypt 동일)
Name: 팀장 01
Department: 영업부
Status: ACTIVE
Role: TEAM_LEADER
```

#### 3. 전화 상담사 (phone-agent-01)
```
Login ID: phone-agent-01
Password: admin123
Name: 전화 상담사 01
Department: 영업부
Status: ACTIVE
Role: PHONE_AGENT
```

#### 4. 채팅 상담사 (chat-agent-01)
```
Login ID: chat-agent-01
Password: admin123
Name: 채팅 상담사 01
Department: 기술부
Status: ACTIVE
Role: CHAT_AGENT
```

### 사용자 추가 시 규칙

#### 1. 기본 정보 설정
- **agent_id**: UUID (자동 생성)
- **tenant_id**: 'tenant-001' (멀티테넌시)
- **login_id**: 중복 없는 고유 ID (영문, 숫자, 하이픈)
- **password**: BCrypt 해시된 비밀번호
- **name**: 사용자 한글 이름
- **status**: ACTIVE, AWAY, CLOSED, RETIRED 중 선택

#### 2. 역할 할당
- agent_roles 테이블에 `(agent_id, role_id)` 매핑 추가
- 한 사용자가 여러 역할을 가질 수 있음 (다대다 관계)

---

## 🔗 권한 그룹(Permission Group) 표준

### 기본 권한 그룹

#### 1. USER_FULL_ACCESS
```
설명: 사용자 생성, 조회, 수정, 삭제 권한
포함 권한:
- user:create, user:read, user:update, user:delete
- agent:create, agent:update, agent:delete
```

#### 2. ORGANIZATION_FULL_ACCESS
```
설명: 조직 생성, 조회, 수정, 삭제 권한
포함 권한:
- org:create, org:read, org:update, org:delete
- department:create, department:update, department:delete
```

#### 3. REPORTING_ACCESS
```
설명: 보고서 조회 및 내보내기 권한
포함 권한:
- report:view, report:export
- audit:view, audit:export
- cdr:view
- dashboard:view
```

---

## 🔄 데이터 관계 흐름

```
Agent (사용자)
├─ agent_roles ──→ Roles (역할)
│                  ├─ role_permissions ──→ Permissions (권한)
│                  │
│                  └─ role_permission_groups ──→ Permission Groups
│                                               ├─ permission_group_permissions ──→ Permissions
└─ departments ──→ Department (부서)
```

### 권한 체크 로직 (의사 코드)
```
1. 사용자의 모든 역할 조회
2. 각 역할의 권한 조회
   - 직접 권한: role_permissions
   - 그룹 권한: role_permission_groups → permission_group_permissions
3. 권한 합침 (최종 권한 = 직접 권한 ∪ 그룹 권한)
4. 요청된 권한이 최종 권한 목록에 있는지 확인
```

---

## 📝 추가 시 체크리스트

### 새로운 권한 추가
- [ ] 권한 코드 규칙 준수 (`{도메인}:{액션}`)
- [ ] permissions 테이블에 권한 삽입
- [ ] 역할-권한 매핑 (role_permissions)
- [ ] 권한 그룹에 포함시킬 지 결정

### 새로운 역할 추가
- [ ] 역할 이름/설명 결정
- [ ] 역할 타입 선택 (POSITION, CHANNEL, SKILL)
- [ ] roles 테이블에 역할 삽입
- [ ] 역할에 필요한 권한 매핑

### 새로운 사용자 추가
- [ ] login_id 중복 확인
- [ ] 비밀번호 BCrypt 해싱
- [ ] agents 테이블에 사용자 삽입
- [ ] 부서 ID 지정
- [ ] 역할 할당 (agent_roles)

---

## 🎯 예시: 새로운 권한/역할/사용자 추가

### 시나리오: QA 담당자 역할 추가

```sql
-- 1. QA 관련 권한 추가
INSERT INTO permissions (permission_id, tenant_id, code, created_at)
VALUES (gen_random_uuid()::VARCHAR(36), 'tenant-001', 'quality:review', CURRENT_TIMESTAMP);

-- 2. QA 역할 생성
INSERT INTO roles (role_id, tenant_id, name, type, description, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid()::VARCHAR(36),
    'tenant-001',
    'QA_REVIEWER',
    'POSITION',
    '통화 품질 검수 담당자',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 3. QA 역할에 권한 할당
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT r.role_id, p.permission_id, CURRENT_TIMESTAMP
FROM roles r, permissions p
WHERE r.name = 'QA_REVIEWER' AND p.code = 'quality:review';

-- 4. QA 사용자 추가
INSERT INTO agents (agent_id, tenant_id, login_id, password, name, dept_id, status, password_must_change, created_at)
SELECT 
    gen_random_uuid()::VARCHAR(36),
    'tenant-001',
    'qa-reviewer-01',
    '$2a$10$...',  -- BCrypt 해시된 비밀번호
    'QA 검수자 01',
    d.dept_id,
    'ACTIVE',
    false,
    CURRENT_TIMESTAMP
FROM departments d
WHERE d.name = '기술부';

-- 5. 사용자에게 역할 할당
INSERT INTO agent_roles (agent_id, role_id, assigned_at)
SELECT a.agent_id, r.role_id, CURRENT_TIMESTAMP
FROM agents a, roles r
WHERE a.login_id = 'qa-reviewer-01' AND r.name = 'QA_REVIEWER';
```

---

## 📞 문의 및 수정

표준 데이터 구조에 대해 변경이 필요하거나 새로운 도메인을 추가할 경우:
1. 팀 회의를 통해 표준 확정
2. 새 마이그레이션 파일 작성 (V1_0_X__...)
3. 이 문서 업데이트
4. 팀원들에게 공유

---

**작성일**: 2026-01-15
**버전**: 1.0

