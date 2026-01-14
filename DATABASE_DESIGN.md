# 📊 Identity Modulith 데이터베이스 설계 문서

## 목차
1. [DB 설계 개요](#db-설계-개요)
2. [전체 테이블 구조](#전체-테이블-구조)
3. [테이블 상세 설명](#테이블-상세-설명)
4. [테이블 간 연관관계](#테이블-간-연관관계)
5. [컬럼 데이터 형식 표준](#컬럼-데이터-형식-표준)

---

## DB 설계 개요

### 목표
- **멀티테넌시(Multi-Tenancy)**: 각 테이블에 tenant_id로 데이터 격리
- **UUID 통일**: 모든 엔티티 ID는 UUID (VARCHAR(36))로 통일
- **조직 트리**: 자기참조를 이용한 부서 계층 구조
- **RBAC**: 역할 기반 접근 제어 (Role-Based Access Control)

### 핵심 원칙
```
1. ID 타입: UUID (VARCHAR(36)) 통일
2. 다대다 관계: 중간 테이블로 명시적 관리
3. 자기참조: departments의 parent_id
4. Soft Delete: agents의 status와 retired_at
5. 데이터 격리: 모든 테이블에 tenant_id
```

---

## 전체 테이블 구조

### 테이블 목록 (6개)

| 테이블 | 모듈 | 용도 | 행 수 |
|--------|------|------|-------|
| **departments** | Organization | 조직/부서 | 13 |
| **agents** | User | 사용자/직원 | 16 |
| **roles** | RBAC | 역할 정의 | 8 |
| **permissions** | RBAC | 권한 정의 | 35 |
| **role_permissions** | RBAC | 역할-권한 매핑 | 77 |
| **agent_roles** | RBAC | 사용자-역할 매핑 | ~30 |

### ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────┐
│               Identity Modulith Database ERD                 │
└─────────────────────────────────────────────────────────────┘

┌──────────────────┐
│   departments    │ (자기참조)
├──────────────────┤
│ PK: dept_id (U)  │
│     tenant_id    │
│ FK: parent_id ───┼─────┐
│     name         │     │
│     org_path     │     │
│     depth        │     │
│     type         │     │
│     created_at   │     │
└──────────────────┘     │
         ▲               │
         │ 1:N (자기참조)|
         └───────────────┘
         │
         │ 1:N (소속)
         ▼
┌──────────────────┐
│     agents       │
├──────────────────┤
│ PK: agent_id (U) │
│     tenant_id    │
│     login_id (U) │
│     password     │
│     name         │
│ FK: dept_id ─────┘
│     status       │
│     ...etc       │
└──────────────────┘
         │
         │ N:M (역할 할당)
         ▼
┌──────────────────┐       ┌──────────────────┐
│   agent_roles    │       │      roles       │
├──────────────────┤       ├──────────────────┤
│ PK: id           │       │ PK: role_id (U)  │
│ FK: agent_id ────┼──────→│     tenant_id    │
│ FK: role_id ─────┼──────→│     name (U)     │
│     assigned_at  │       │     type         │
└──────────────────┘       │     created_at   │
                           └──────────────────┘
                                   │
                                   │ N:M
                                   ▼
                           ┌──────────────────┐
                           │ role_permissions │
                           ├──────────────────┤
                           │ PK: id           │
                           │ FK: role_id ─────┘
                           │ FK: permission_id┐
                           │     assigned_at  │
                           └──────────────────┘
                                   │
                                   │
                           ┌──────────────────┐
                           │   permissions    │
                           ├──────────────────┤
                           │ PK: permission_id│
                           │     tenant_id    │
                           │     code (U)     │
                           │     created_at   │
                           └──────────────────┘

범례: PK=Primary Key, FK=Foreign Key, U=Unique, N:M=다대다
```

---

## 테이블 상세 설명

### 1. departments (조직/부서 테이블)

**목적**: 회사 조직 계층 구조 관리 (트리 구조)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| dept_id | VARCHAR(36) | PK | 부서 ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID (멀티테넌시) |
| parent_id | VARCHAR(36) | FK (자기참조) | 상위 부서 ID (NULL이면 최상위) |
| name | VARCHAR(100) | NOT NULL | 부서명 |
| org_path | VARCHAR(500) | NOT NULL, UNIQUE | 조직 경로 (예: /dept1/dept2/dept3) |
| depth | INTEGER | NOT NULL | 트리 깊이 (0부터 시작) |
| type | VARCHAR(50) | | 부서 타입 (HEADQUARTERS, DIVISION, TEAM) |
| created_at | TIMESTAMP | NOT NULL | 생성 일시 |

**인덱스**:
- PK: dept_id
- UK: (tenant_id, org_path)
- FK: parent_id → dept_id (자기참조, ON DELETE RESTRICT)
- IDX: tenant_id, parent_id, org_path

**특징**:
- **자기참조 (Self-Join)**: parent_id로 상하 관계 표현
- **Closure Table 대안**: org_path로 계층 탐색 최적화
- **Soft Constraint**: RESTRICT로 하위 부서 있으면 삭제 불가

**예시 데이터**:
```
dept_id              | name        | parent_id | org_path | depth
---------------------|-------------|-----------|----------|-------
d50e8400-e29b-...001 | 넥스프론본부 | NULL      | /d50e... | 0
d50e8400-e29b-...002 | 고객지원사부 | d50e...001| /d50e.../002 | 1
d50e8400-e29b-...005 | 전화상담팀  | d50e...002| /d50e.../002/005 | 2
```

---

### 2. agents (사용자/직원 테이블)

**목적**: 시스템 사용자 정보 관리

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| agent_id | VARCHAR(36) | PK | 사용자 ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID |
| login_id | VARCHAR(100) | NOT NULL, UNIQUE | 로그인 ID |
| password | VARCHAR(255) | NOT NULL | 비밀번호 (BCrypt 해시) |
| name | VARCHAR(100) | NOT NULL | 사용자명 |
| dept_id | VARCHAR(36) | FK | 소속 부서 ID (NULL 가능) |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | 상태 (ACTIVE, RETIRED) |
| password_must_change | BOOLEAN | DEFAULT false | 비밀번호 변경 필요 여부 |
| created_at | TIMESTAMP | NOT NULL | 생성 일시 |
| updated_at | TIMESTAMP | | 수정 일시 |
| retired_at | TIMESTAMP | | 퇴직 일시 |
| job_title | VARCHAR(100) | | 직책 |
| sync_status | VARCHAR(20) | | 동기 상태 |
| role_id | VARCHAR(50) | | (deprecated) 역할 ID |

**인덱스**:
- PK: agent_id
- UK: login_id
- FK: dept_id → departments.dept_id (ON DELETE SET NULL)
- IDX: tenant_id, dept_id, status, login_id

**특징**:
- **Soft Delete**: status='RETIRED'로 논리적 삭제 (물리적 삭제 X)
- **다중 역할**: agent_roles 테이블로 여러 역할 할당 가능
- **department 참조**: 소속 부서를 통한 조직 구조 연결

**예시 데이터**:
```
agent_id             | login_id    | name      | dept_id      | status
---------------------|-------------|-----------|--------------|--------
550e8400-e29b-...101 | admin       | 시스템관리자 | d50e...001 | ACTIVE
550e8400-e29b-...104 | phone_ag01 | 박상담     | d50e...005   | ACTIVE
550e8400-e29b-...199 | retired_usr | 퇴직자    | d50e...005   | RETIRED
```

---

### 3. roles (역할 테이블)

**목적**: RBAC 역할 정의 (권한 묶음)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| role_id | VARCHAR(36) | PK | 역할 ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID |
| name | VARCHAR(64) | NOT NULL, UNIQUE | 역할명 (ADMIN, MANAGER 등) |
| type | VARCHAR(32) | NOT NULL | 역할 타입 (POSITION, CHANNEL, SKILL) |
| created_at | TIMESTAMP | NOT NULL | 생성 일시 |

**인덱스**:
- PK: role_id
- UK: (tenant_id, name)
- IDX: tenant_id

**역할 분류**:

| 타입 | 설명 | 예시 |
|------|------|------|
| POSITION | 직급 기반 (직책) | ADMIN, MANAGER, TEAM_LEAD, MEMBER |
| CHANNEL | 채널 기반 (업무 채널) | PHONE_AGENT, CHAT_AGENT, EMAIL_AGENT, SUPERVISOR |
| SKILL | 역량 기반 | (확장 가능) |

**특징**:
- **다중 역할 조합**: 사용자는 POSITION + CHANNEL 조합 가능
- 예: 박상담 = MEMBER (직급) + PHONE_AGENT (채널)

**예시 데이터**:
```
role_id              | name         | type
---------------------|--------------|----------
660e8400-e29b-...001 | ADMIN        | POSITION
660e8400-e29b-...005 | PHONE_AGENT  | CHANNEL
```

---

### 4. permissions (권한 테이블)

**목적**: 시스템 권한 정의 (최소 단위 권한)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| permission_id | VARCHAR(36) | PK | 권한 ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID |
| code | VARCHAR(128) | NOT NULL, UNIQUE | 권한 코드 (domain:action 형식) |
| created_at | TIMESTAMP | NOT NULL | 생성 일시 |

**인덱스**:
- PK: permission_id
- UK: (tenant_id, code)
- IDX: tenant_id

**권한 코드 형식**:
```
{domain}:{action}[:{resource}]

도메인 (8개):
├─ user:      사용자 관리 (9개)
├─ org:       조직 관리 (6개)
├─ rbac:      RBAC 관리 (9개)
├─ report:    보고서 (4개)
├─ phone:     전화 채널 (3개)
├─ chat:      채팅 채널 (2개)
├─ email:     이메일 채널 (1개)
└─ queue:     큐 관리 (1개)

총 35개 권한
```

**예시 데이터**:
```
permission_id        | code
---------------------|------------------
550e8400-e29b-...001 | user:create
550e8400-e29b-...029 | phone:accept
550e8400-e29b-...032 | chat:send
```

---

### 5. role_permissions (역할-권한 매핑 테이블)

**목적**: 역할에 권한 할당 (N:M 관계)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 매핑 ID (자동 증가) |
| role_id | VARCHAR(36) | FK | 역할 ID |
| permission_id | VARCHAR(36) | FK | 권한 ID |
| assigned_at | TIMESTAMP | NOT NULL | 할당 일시 |

**인덱스**:
- PK: id
- UK: (role_id, permission_id)
- FK: role_id → roles.role_id (ON DELETE CASCADE)
- FK: permission_id → permissions.permission_id (ON DELETE CASCADE)

**특징**:
- **다대다 관계**: 한 역할에 여러 권한 할당 가능
- **동적 권한 관리**: 역할 변경 시 자동 반영

**권한 배분**:
```
ADMIN:     35개 (전체)
MANAGER:   12개 (사용자, 조직, 보고서)
TEAM_LEAD:  5개 (읽기, 조직 뷰, 보고서)
MEMBER:     4개 (본인 읽기, 조직 뷰, 보고서)

PHONE_AGENT:  3개 (전화 관련)
CHAT_AGENT:   2개 (채팅 관련)
EMAIL_AGENT:  1개 (이메일 관련)
SUPERVISOR:   7개 (모든 채널 + 큐)

총 77개 매핑
```

---

### 6. agent_roles (사용자-역할 매핑 테이블)

**목적**: 사용자에게 역할 할당 (N:M 관계)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 매핑 ID (자동 증가) |
| agent_id | VARCHAR(36) | FK | 사용자 ID |
| role_id | VARCHAR(36) | FK | 역할 ID |
| assigned_at | TIMESTAMP | NOT NULL | 할당 일시 |

**인덱스**:
- PK: id
- UK: (agent_id, role_id) - 중복 방지
- FK: agent_id → agents.agent_id (ON DELETE CASCADE)
- FK: role_id → roles.role_id (ON DELETE CASCADE)

**특징**:
- **다중 역할**: 사용자는 여러 역할 보유 가능 (예: MEMBER + PHONE_AGENT + SUPERVISOR)
- **동적 할당**: 역할 추가/제거 시 자동 반영
- **권한 계산**: 모든 역할의 권한 합집합 = 사용자의 최종 권한

**예시 데이터**:
```
agent_id (박상담)    | role_id (역할)
---------------------|----------------------
550e8400-e29b-...104 | 660e8400-e29b-...004 (MEMBER)
550e8400-e29b-...104 | 660e8400-e29b-...005 (PHONE_AGENT)
```

---

## 테이블 간 연관관계

### 1. 일대다 (One-to-Many) 관계

#### departments (1) → departments (N) - 자기참조
```
상위 부서 (parent) → 하위 부서들 (자식)

관계: 부모-자식
FK: parent_id → dept_id
특징: 자기참조, 트리 구조
삭제 정책: ON DELETE RESTRICT (하위 부서 있으면 삭제 불가)

예시:
넥스프론 본부 (root)
├─ 고객지원사업부
│  ├─ 전화상담팀
│  └─ 채팅상담팀
└─ 기술개발본부
   └─ Backend개발팀
```

#### departments (1) → agents (N)
```
부서 (department) → 소속 직원들 (employees)

관계: 조직 포함 관계
FK: agents.dept_id → departments.dept_id
특징: 하나의 부서에 여러 직원
삭제 정책: ON DELETE SET NULL (부서 삭제 시 직원의 dept_id = NULL)

예시:
전화상담팀 (1개)
├─ 이팀장 (1명)
├─ 박상담 (1명)
└─ 최상담 (1명)
```

---

### 2. 다대다 (Many-to-Many) 관계

#### agents (N) ↔ roles (M) via agent_roles
```
사용자 ←→ 역할

구조:
agents → agent_roles → roles

특징:
- 한 사용자가 여러 역할 보유
- 한 역할이 여러 사용자에게 할당
- 중간 테이블: agent_roles

예시:
박상담 (1명)
├─ MEMBER (직급)
└─ PHONE_AGENT (채널)

MEMBER 역할
├─ 박상담
├─ 정상담
├─ 강상담
└─ ... (7명)

삭제 정책: ON DELETE CASCADE (양쪽 모두)
- 사용자 삭제 → agent_roles 자동 삭제
- 역할 삭제 → agent_roles 자동 삭제
```

#### roles (N) ↔ permissions (M) via role_permissions
```
역할 ←→ 권한

구조:
roles → role_permissions → permissions

특징:
- 한 역할에 여러 권한 포함
- 한 권한이 여러 역할에 할당 가능
- 중간 테이블: role_permissions

예시:
ADMIN 역할 (1개)
├─ user:create
├─ user:delete
├─ org:manage
├─ rbac:configure
└─ ... (35개 모두)

MEMBER 역할 (1개)
├─ user:read:self
├─ user:update:self
├─ org:view
└─ report:view

삭제 정책: ON DELETE CASCADE (양쪽 모두)
- 역할 삭제 → role_permissions 자동 삭제
- 권한 삭제 → role_permissions 자동 삭제
```

---

### 3. 권한 체크 흐름

**사용자의 최종 권한 계산**:

```
1단계: 사용자 조회
┌──────────────┐
│ agents       │ (user_id로 조회)
│ agent_id=... │
└──────────────┘
        ↓

2단계: 사용자의 모든 역할 조회
┌──────────────┐
│ agent_roles  │ (WHERE agent_id = ?)
│ role_id=... │
│ role_id=... │ (다중 역할)
└──────────────┘
        ↓

3단계: 각 역할의 모든 권한 조회
┌──────────────┐
│ role_permissions │ (WHERE role_id IN (...))
│ permission_id=... │
│ permission_id=... │
│ permission_id=... │
└──────────────┘
        ↓

4단계: 모든 권한 코드 조회
┌──────────────┐
│ permissions  │
│ code='user:create' │
│ code='org:view' │
│ code='phone:accept' │
│ ... │
└──────────────┘
        ↓

5단계: 권한 확인
최종 권한 = 모든 역할의 권한 합집합 (Union)
```

**SQL 예시**:
```sql
-- 특정 사용자의 모든 권한 조회
SELECT DISTINCT p.code
FROM agents a
JOIN agent_roles ar ON a.agent_id = ar.agent_id
JOIN role_permissions rp ON ar.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE a.agent_id = ? 
  AND a.status = 'ACTIVE'
  AND a.tenant_id = ?;

-- 특정 권한 보유 확인
SELECT EXISTS (
    SELECT 1
    FROM agents a
    JOIN agent_roles ar ON a.agent_id = ar.agent_id
    JOIN role_permissions rp ON ar.role_id = rp.role_id
    JOIN permissions p ON rp.permission_id = p.permission_id
    WHERE a.agent_id = ?
      AND p.code = 'phone:accept'
      AND a.status = 'ACTIVE'
      AND a.tenant_id = ?
) AS has_permission;
```

---

## 컬럼 데이터 형식 표준

### 1. ID 컬럼 (모두 UUID로 통일)

| 컬럼명 | 타입 | 크기 | 형식 | 예시 |
|--------|------|------|------|------|
| dept_id | VARCHAR | 36 | UUID | d50e8400-e29b-41d4-a716-446655440001 |
| agent_id | VARCHAR | 36 | UUID | 550e8400-e29b-41d4-a716-446655440101 |
| role_id | VARCHAR | 36 | UUID | 660e8400-e29b-41d4-a716-446655440001 |
| permission_id | VARCHAR | 36 | UUID | 550e8400-e29b-41d4-a716-446655440001 |

**UUID 형식**:
```
xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  8자   - 4자 - 4자 - 4자 -    12자
  총 36자 (하이픈 포함)
```

---

### 2. 문자열 컬럼 표준

| 컬럼명 | 최대길이 | 설명 | 예시 |
|--------|---------|------|------|
| tenant_id | 50 | 테넌트 ID (고정) | tenant-001 |
| login_id | 100 | 로그인 ID (영숫자, -, _) | phone_agent01 |
| password | 255 | BCrypt 해시 | $2a$10$N9qo8... |
| name | 100 | 사용자/부서명 | 박상담, 전화상담팀 |
| org_path | 500 | 조직 경로 (UUID 기반) | /d50e8400.../d50e8400.../... |
| job_title | 100 | 직책 | 팀장, 과장 |
| sync_status | 20 | 동기 상태 | PENDING, SUCCESS, FAILED |
| type (departments) | 50 | 부서 타입 | HEADQUARTERS, DIVISION, TEAM |
| type (roles) | 32 | 역할 타입 | POSITION, CHANNEL, SKILL |
| status (agents) | 20 | 상태 | ACTIVE, RETIRED |
| name (roles) | 64 | 역할명 (대문자, _) | ADMIN, TEAM_LEAD, PHONE_AGENT |
| code (permissions) | 128 | 권한 코드 (도메인:액션 형식) | user:create, phone:accept |

---

### 3. 시간 컬럼 표준

| 컬럼명 | 타입 | 형식 | 설명 | 예시 |
|--------|------|------|------|------|
| created_at | TIMESTAMP | ISO 8601 | 생성 일시 (자동) | 2026-01-14 11:05:09 |
| updated_at | TIMESTAMP | ISO 8601 | 수정 일시 (자동) | 2026-01-14 11:05:09 |
| assigned_at | TIMESTAMP | ISO 8601 | 할당 일시 | 2026-01-14 10:00:00 |
| retired_at | TIMESTAMP | ISO 8601 | 퇴직 일시 (NULL 가능) | 2025-12-14 17:00:00 |

**TIMESTAMP 설정**:
```sql
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
-- 자동으로 현재 시간 입력
-- 변경 불가 (updatable = false in JPA)
```

---

### 4. 숫자 컬럼 표준

| 컬럼명 | 타입 | 범위 | 설명 | 예시 |
|--------|------|------|------|------|
| depth | INTEGER | 0~99 | 트리 깊이 (0=최상위) | 0, 1, 2 |
| id (매핑 테이블) | BIGSERIAL | 1~9223372036854775807 | 자동 증가 ID | 1, 2, 3, ... |
| password_must_change | BOOLEAN | TRUE/FALSE | 비밀번호 변경 필요 | false, true |

---

### 5. NULL 정책

| 컬럼명 | 테이블 | NULL 허용 | 이유 | 비고 |
|--------|--------|----------|------|------|
| parent_id | departments | YES | 최상위 부서일 수 있음 | 루트는 NULL |
| dept_id | agents | YES | 부서 미정 직원 가능 | ON DELETE SET NULL |
| updated_at | agents | YES | 생성 후 수정 없을 수 있음 | 선택사항 |
| retired_at | agents | YES | 활성 직원은 NULL | Soft Delete |
| job_title | agents | YES | 직책 미정 가능 | 선택사항 |
| sync_status | agents | YES | 동기 상태 미정 | 선택사항 |
| role_id | agents | YES | deprecated, 미사용 | agent_roles 사용 |

**NOT NULL 컬럼**:
```
- 모든 테이블: tenant_id (멀티테넌시 격리)
- 모든 테이블: created_at (감사 추적)
- 모든 ID PK 컬럼
- 조직관리: name, org_path, depth, type
- 사용자관리: login_id, password, name, status
- RBAC: code (권한), name (역할, 권한)
- 매핑: assigned_at
```

---

### 6. UNIQUE 제약 표준

| 테이블 | 컬럼(들) | 범위 | 설명 |
|--------|---------|------|------|
| agents | login_id | 전사 | 로그인 ID는 고유 (테넌트 내) |
| departments | (tenant_id, org_path) | 테넌트 | 같은 경로 불가 |
| roles | (tenant_id, name) | 테넌트 | 같은 테넌트 내 역할명 고유 |
| permissions | (tenant_id, code) | 테넌트 | 같은 테넌트 내 권한 코드 고유 |
| role_permissions | (role_id, permission_id) | N/A | 중복 매핑 방지 |
| agent_roles | (agent_id, role_id) | N/A | 중복 할당 방지 |

---

## 데이터 예시

### 표준 데이터셋

| 항목 | 수량 | 설명 |
|------|------|------|
| **Departments** | 13개 | 본부(1) + 사업부(3) + 팀(9) |
| **Agents** | 16명 | 활성(15) + 퇴직(1) |
| **Roles** | 8개 | POSITION(4) + CHANNEL(4) |
| **Permissions** | 35개 | 8개 도메인 |
| **Role-Permissions** | 77개 | 역할별 권한 매핑 |
| **Agent-Roles** | ~30개 | 사용자별 다중 역할 |

### 조직 구조 예시

```
넥스프론 본부 (HEADQUARTERS)
├─ 고객지원사업부 (DIVISION)
│  ├─ 전화상담팀 (TEAM)
│  │  ├─ 이팀장 (TEAM_LEAD + SUPERVISOR)
│  │  ├─ 박상담 (MEMBER + PHONE_AGENT)
│  │  └─ 최상담 (MEMBER + PHONE_AGENT)
│  ├─ 채팅상담팀 (TEAM)
│  │  └─ ...
│  └─ VIP고객지원팀 (TEAM)
│     └─ 송상담 (MEMBER + PHONE_AGENT + CHAT_AGENT + EMAIL_AGENT)
├─ 영업사업부 (DIVISION)
│  └─ ...
└─ 기술개발본부 (DIVISION)
   ├─ Backend개발팀 (TEAM)
   ├─ Frontend개발팀 (TEAM)
   └─ DevOps팀 (TEAM)
```

### 권한 계층 예시

```
ADMIN (35개 권한 - 전체)
├─ user:create, user:read, user:update, user:delete, ...
├─ org:view, org:create, org:update, org:move, org:delete, org:manage
├─ rbac:view, rbac:create:role, rbac:configure, ...
├─ report:*, phone:*, chat:*, email:*, queue:*
└─ ... (총 35개)

MANAGER (12개 권한)
├─ user:create, user:read, user:update, user:assign:role, user:reset:password
├─ org:view, org:create, org:update, org:move
├─ report:view, report:read, report:export
└─ ... (총 12개)

MEMBER (4개 권한 - 최소)
├─ user:read:self
├─ user:update:self
├─ org:view
└─ report:view

PHONE_AGENT (3개 권한)
├─ phone:accept
├─ phone:hold
└─ phone:transfer
```

---

## 설계 원칙 및 이유

### 1. UUID로 통일한 이유
- ✅ 분산 환경 지원 (ID 충돌 없음)
- ✅ 멀티테넌시 안전성 (테넌트 간 ID 충돌 불가)
- ✅ 보안 (순차 ID 노출 방지)
- ✅ 일관성 (모든 엔티티 동일한 형식)

### 2. 자기참조 FK 사용 이유
- ✅ 계층 구조 표현 최적화
- ✅ org_path로 경로 탐색 빠름
- ✅ depth로 레벨 쉽게 파악
- ✅ 유연한 부서 추가/제거

### 3. 중간 테이블 사용 이유
- ✅ N:M 관계를 명시적으로 관리
- ✅ 할당 일시 등 메타데이터 저장 가능
- ✅ 감사 추적 용이
- ✅ 성능 최적화 (조인 명확화)

### 4. Soft Delete 사용 이유
- ✅ 히스토리 유지
- ✅ 감사 추적 (언제 퇴직했는지)
- ✅ 데이터 복구 가능
- ✅ 참조 무결성 유지

### 5. 멀티테넌시 구현 이유
- ✅ 데이터 격리 (tenant_id 필수)
- ✅ SaaS 확장성
- ✅ 보안 (테넌트 간 데이터 접근 불가)

---

**문서 작성일**: 2026-01-14  
**최종 수정일**: 2026-01-14  
**버전**: 1.0 (최종)

