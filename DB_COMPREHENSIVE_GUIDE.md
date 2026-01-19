# 📊 Identity Modulith 데이터베이스 종합 가이드

> **목적**: 데이터베이스 설계, 테이블 구조, 표준 데이터를 한 곳에서 확인  
> **대상**: 개발팀, 운영팀  
> **버전**: 2.0  
> **최종 수정일**: 2026-01-16

---

## 📋 목차
1. [데이터베이스 개요](#데이터베이스-개요)
2. [테이블 구조](#테이블-구조)
3. [테이블 상세 명세](#테이블-상세-명세)
4. [테이블 간 연관관계](#테이블-간-연관관계)
5. [컬럼 데이터 형식 표준](#컬럼-데이터-형식-표준)
6. [표준 데이터 가이드](#표준-데이터-가이드)
7. [권한 및 역할 표준](#권한-및-역할-표준)

---

## 데이터베이스 개요

### 설계 목표
- **멀티테넌시(Multi-Tenancy)**: 각 테이블에 tenant_id로 데이터 격리
- **UUID 통일**: 모든 엔티티 ID는 UUID (VARCHAR(36))로 통일
- **조직 트리**: 자기참조를 이용한 부서 계층 구조
- **RBAC**: 역할 기반 접근 제어 (Role-Based Access Control)
- **감사 추적**: 모든 테이블에 created_at, 주요 작업은 audit_logs로 기록

### 핵심 원칙
```
✅ ID 타입: UUID (VARCHAR(36)) 통일
✅ 다대다 관계: 중간 테이블로 명시적 관리
✅ 자기참조: departments의 parent_id
✅ Soft Delete: agents의 status와 retired_at
✅ 데이터 격리: 모든 테이블에 tenant_id (NOT NULL)
```

---

## 테이블 구조

### 전체 테이블 목록 (6개 + 2개)

| 테이블 | 모듈 | 용도 | PK 타입 | 참고 |
|--------|------|------|---------|------|
| **departments** | Organization | 조직/부서 계층 | VARCHAR(36) | 자기참조 트리 |
| **agents** | User | 사용자/직원 | VARCHAR(36) | Soft Delete |
| **roles** | RBAC | 역할 정의 | VARCHAR(36) | 권한 묶음 |
| **permissions** | RBAC | 권한 정의 | VARCHAR(36) | 최소 단위 권한 |
| **role_permissions** | RBAC | 역할-권한 매핑 | BIGSERIAL | N:M 중간 테이블 |
| **agent_roles** | RBAC | 사용자-역할 매핑 | BIGSERIAL | N:M 중간 테이블 |
| **audit_logs** | Audit | 감사 로그 | BIGSERIAL | 변경 이력 추적 |
| **audit_archive** | Audit | 감사 로그 아카이브 | BIGSERIAL | 90일 이상 로그 |

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
                                   ▼
                           ┌──────────────────┐
                           │   permissions    │
                           ├──────────────────┤
                           │ PK: permission_id│
                           │     tenant_id    │
                           │     code (U)     │
                           │     created_at   │
                           └──────────────────┘

범례: PK=Primary Key, FK=Foreign Key, U=UUID, N:M=다대다
```

---

## 테이블 상세 명세

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
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |

**인덱스**:
- PK: dept_id
- UK: (tenant_id, org_path)
- FK: parent_id → dept_id (자기참조, ON DELETE RESTRICT)
- IDX: (tenant_id), (parent_id), (org_path)

**특징**:
- **자기참조 (Self-Join)**: parent_id로 상하 관계 표현
- **Closure Table 대안**: org_path로 계층 탐색 최적화
- **삭제 제약**: RESTRICT로 하위 부서 있으면 삭제 불가

**예시 데이터**:
```sql
dept_id              | name        | parent_id | org_path          | depth | type
---------------------|-------------|-----------|-------------------|-------|-------------
d50e8400-e29b-...001 | 넥스프론본부 | NULL      | /d50e...001       | 0     | HEADQUARTERS
d50e8400-e29b-...002 | 고객지원사부 | ...001    | /d50e...001/002   | 1     | DIVISION
d50e8400-e29b-...005 | 전화상담팀  | ...002    | /d50e...001/002/005 | 2   | TEAM
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
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMP | | 수정 일시 |
| retired_at | TIMESTAMP | | 퇴직 일시 |
| job_title | VARCHAR(100) | | 직책 |
| sync_status | VARCHAR(20) | | 동기 상태 |

**인덱스**:
- PK: agent_id
- UK: login_id
- FK: dept_id → departments.dept_id (ON DELETE SET NULL)
- IDX: (tenant_id), (dept_id), (status), (login_id)

**특징**:
- **Soft Delete**: status='RETIRED'로 논리적 삭제 (물리적 삭제 X)
- **다중 역할**: agent_roles 테이블로 여러 역할 할당 가능
- **부서 연결**: dept_id로 조직 구조와 연결

**예시 데이터**:
```sql
agent_id             | login_id    | name      | dept_id      | status
---------------------|-------------|-----------|--------------|--------
550e8400-e29b-...101 | admin       | 시스템관리자 | d50e...001  | ACTIVE
550e8400-e29b-...104 | phone_ag01 | 박상담     | d50e...005  | ACTIVE
550e8400-e29b-...199 | retired_usr | 퇴직자    | d50e...005  | RETIRED
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
| description | VARCHAR(255) | | 역할 설명 |
| is_active | BOOLEAN | DEFAULT true | 활성화 여부 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |

**인덱스**:
- PK: role_id
- UK: (tenant_id, name)
- IDX: (tenant_id)

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
```sql
role_id              | name         | type      | is_active
---------------------|--------------|-----------|----------
660e8400-e29b-...001 | ADMIN        | POSITION  | true
660e8400-e29b-...005 | PHONE_AGENT  | CHANNEL   | true
```

---

### 4. permissions (권한 테이블)

**목적**: 시스템 권한 정의 (최소 단위 권한)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| permission_id | VARCHAR(36) | PK | 권한 ID (UUID) |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID |
| code | VARCHAR(128) | NOT NULL, UNIQUE | 권한 코드 (domain:action 형식) |
| description | VARCHAR(255) | | 권한 설명 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |

**인덱스**:
- PK: permission_id
- UK: (tenant_id, code)
- IDX: (tenant_id)

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
```sql
permission_id        | code                | description
---------------------|---------------------|------------------
550e8400-e29b-...001 | user:create         | 사용자 생성
550e8400-e29b-...029 | phone:accept        | 전화 수락
550e8400-e29b-...032 | chat:send           | 채팅 전송
```

---

### 5. role_permissions (역할-권한 매핑 테이블)

**목적**: 역할에 권한 할당 (N:M 관계)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 매핑 ID (자동 증가) |
| role_id | VARCHAR(36) | FK, NOT NULL | 역할 ID |
| permission_id | VARCHAR(36) | FK, NOT NULL | 권한 ID |
| assigned_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 할당 일시 |

**인덱스**:
- PK: id
- UK: (role_id, permission_id)
- FK: role_id → roles.role_id (ON DELETE CASCADE)
- FK: permission_id → permissions.permission_id (ON DELETE CASCADE)

**특징**:
- **다대다 관계**: 한 역할에 여러 권한 할당 가능
- **동적 권한 관리**: 역할 변경 시 자동 반영
- **CASCADE 삭제**: 역할/권한 삭제 시 매핑도 자동 삭제

**권한 배분 예시**:
```sql
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
| agent_id | VARCHAR(36) | FK, NOT NULL | 사용자 ID |
| role_id | VARCHAR(36) | FK, NOT NULL | 역할 ID |
| assigned_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 할당 일시 |

**인덱스**:
- PK: id
- UK: (agent_id, role_id) - 중복 방지
- FK: agent_id → agents.agent_id (ON DELETE CASCADE)
- FK: role_id → roles.role_id (ON DELETE CASCADE)
- IDX: (agent_id), (role_id)

**특징**:
- **다중 역할**: 사용자는 여러 역할 보유 가능 (예: MEMBER + PHONE_AGENT + SUPERVISOR)
- **동적 할당**: 역할 추가/제거 시 자동 반영
- **권한 계산**: 모든 역할의 권한 합집합 = 사용자의 최종 권한

**예시 데이터**:
```sql
agent_id (박상담)    | role_id (역할)
---------------------|----------------------
550e8400-e29b-...104 | 660e8400-e29b-...004 (MEMBER)
550e8400-e29b-...104 | 660e8400-e29b-...005 (PHONE_AGENT)
```

---

### 7. audit_logs (감사 로그 테이블)

**목적**: 시스템 주요 작업 이력 추적

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 로그 ID (자동 증가) |
| tenant_id | VARCHAR(50) | NOT NULL | 테넌트 ID |
| action | VARCHAR(100) | NOT NULL | 작업 (ROLE_ASSIGNED, PERMISSION_CREATED 등) |
| target_type | VARCHAR(50) | NOT NULL | 대상 타입 (ROLE, PERMISSION, USER 등) |
| target_id | VARCHAR(100) | NOT NULL | 대상 ID |
| actor_id | VARCHAR(36) | NOT NULL | 작업자 ID |
| details | TEXT | | 상세 정보 (JSON 형식) |
| ip_address | VARCHAR(45) | | 작업자 IP |
| timestamp | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 발생 일시 |

**인덱스**:
- PK: id
- IDX: (tenant_id, timestamp), (actor_id), (target_type, target_id)

**특징**:
- **불변 로그**: 생성 후 수정/삭제 불가
- **90일 자동 아카이빙**: audit_archive로 이동
- **JSON 상세 정보**: 변경 전후 값 저장

---

### 8. audit_archive (감사 로그 아카이브 테이블)

**목적**: 90일 이상 오래된 감사 로그 보관

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| (audit_logs와 동일) | | | |
| archived_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 아카이빙 일시 |

**특징**:
- **자동 아카이빙**: 배치 작업으로 90일 초과 로그 이동
- **장기 보관**: 법적 요구사항 대응
- **검색 최적화**: 최근 로그는 audit_logs에서만 검색

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
ADMIN 역할 (1개) → 35개 권한 (모두)
MEMBER 역할 (1개) → 4개 권한 (최소)

삭제 정책: ON DELETE CASCADE (양쪽 모두)
```

---

### 3. 권한 체크 흐름

**사용자의 최종 권한 계산**:

```
1단계: 사용자 조회
┌──────────────┐
│ agents       │ (agent_id로 조회)
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
└──────────────┘
        ↓

4단계: 모든 권한 코드 조회
┌──────────────┐
│ permissions  │
│ code='user:create' │
│ code='phone:accept' │
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
| type (departments) | 50 | 부서 타입 | HEADQUARTERS, DIVISION, TEAM |
| type (roles) | 32 | 역할 타입 | POSITION, CHANNEL, SKILL |
| status (agents) | 20 | 상태 | ACTIVE, RETIRED |
| name (roles) | 64 | 역할명 (대문자, _) | ADMIN, TEAM_LEAD, PHONE_AGENT |
| code (permissions) | 128 | 권한 코드 (도메인:액션) | user:create, phone:accept |

---

### 3. 시간 컬럼 표준

| 컬럼명 | 타입 | 형식 | 설명 | 예시 |
|--------|------|------|------|------|
| created_at | TIMESTAMP | ISO 8601 | 생성 일시 (자동) | 2026-01-16 10:00:00 |
| updated_at | TIMESTAMP | ISO 8601 | 수정 일시 (자동) | 2026-01-16 10:05:00 |
| assigned_at | TIMESTAMP | ISO 8601 | 할당 일시 | 2026-01-16 10:00:00 |
| retired_at | TIMESTAMP | ISO 8601 | 퇴직 일시 (NULL 가능) | 2025-12-14 17:00:00 |
| timestamp | TIMESTAMP | ISO 8601 | 감사 로그 발생 일시 | 2026-01-16 10:00:00 |

---

### 4. NULL 정책

| 컬럼명 | 테이블 | NULL 허용 | 이유 | 비고 |
|--------|--------|----------|------|------|
| parent_id | departments | YES | 최상위 부서일 수 있음 | 루트는 NULL |
| dept_id | agents | YES | 부서 미정 직원 가능 | ON DELETE SET NULL |
| updated_at | agents | YES | 생성 후 수정 없을 수 있음 | 선택사항 |
| retired_at | agents | YES | 활성 직원은 NULL | Soft Delete |
| job_title | agents | YES | 직책 미정 가능 | 선택사항 |
| description | roles, permissions | YES | 설명 선택사항 | |
| ip_address | audit_logs | YES | IP 추적 불가능할 수 있음 | |

---

## 표준 데이터 가이드

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
│  └─ VIP고객지원팀 (TEAM)
├─ 영업사업부 (DIVISION)
└─ 기술개발본부 (DIVISION)
   ├─ Backend개발팀 (TEAM)
   ├─ Frontend개발팀 (TEAM)
   └─ DevOps팀 (TEAM)
```

---

## 권한 및 역할 표준

### 권한(Permission) 코드 규칙

**형식**: `{domain}:{action}[:{resource}]`

### 도메인별 권한 목록 (총 35개)

#### 1. 사용자 관리 (user, agent) - 9개
```
- user:create          사용자 생성
- user:read            사용자 조회
- user:update          사용자 수정
- user:delete          사용자 삭제
- user:read:self       본인 정보 조회
- user:update:self     본인 정보 수정
- user:assign:role     역할 할당
- user:reset:password  비밀번호 재설정
- agent:manage         에이전트 전체 관리
```

#### 2. 조직 관리 (org, department) - 6개
```
- org:view             조직 조회
- org:create           조직 생성
- org:update           조직 수정
- org:move             조직 이동
- org:delete           조직 삭제
- org:manage           조직 전체 관리
```

#### 3. RBAC 관리 (rbac, role, permission) - 9개
```
- rbac:view            RBAC 조회
- rbac:create:role     역할 생성
- rbac:update:role     역할 수정
- rbac:delete:role     역할 삭제
- rbac:create:permission 권한 생성
- rbac:update:permission 권한 수정
- rbac:delete:permission 권한 삭제
- rbac:assign:permission 권한 할당
- rbac:configure       RBAC 전체 설정
```

#### 4. 보고서 및 감시 (report, audit, cdr) - 7개
```
- report:view          보고서 조회
- report:read          보고서 읽기
- report:export        보고서 내보내기
- report:manage        보고서 관리
- audit:view           감사 로그 조회
- audit:export         감사 로그 내보내기
- cdr:view             CDR 조회
```

#### 5. 채널 관리 (phone, chat, email, queue) - 7개
```
- phone:accept         전화 수락
- phone:hold           전화 보류
- phone:transfer       전화 전환
- chat:send            채팅 전송
- chat:receive         채팅 수신
- email:send           이메일 전송
- queue:manage         큐 관리
```

#### 6. 기타 (dashboard, quality) - 2개
```
- dashboard:view       대시보드 조회
- quality:manage       품질 관리
```

---

### 역할(Role) 정의

#### 역할 타입
- **POSITION**: 조직상 직위 (ADMIN, MANAGER, TEAM_LEAD, MEMBER)
- **CHANNEL**: 상담 채널 (PHONE_AGENT, CHAT_AGENT, EMAIL_AGENT, SUPERVISOR)
- **SKILL**: 기술/스킬 (향후 확장용)

#### 기본 역할 및 권한 할당

| 역할 | 타입 | 권한 수 | 주요 권한 |
|------|------|---------|-----------|
| **ADMIN** | POSITION | 35개 (전체) | user:*, org:*, rbac:*, report:*, audit:*, 모든 채널 |
| **MANAGER** | POSITION | 12개 | user 생성/수정, org 생성/수정/이동, report 전체 |
| **TEAM_LEAD** | POSITION | 5개 | user:read, org:view, report:view/read/export |
| **MEMBER** | POSITION | 4개 | user:read:self, user:update:self, org:view, report:view |
| **PHONE_AGENT** | CHANNEL | 3개 | phone:accept, phone:hold, phone:transfer |
| **CHAT_AGENT** | CHANNEL | 2개 | chat:send, chat:receive |
| **EMAIL_AGENT** | CHANNEL | 1개 | email:send |
| **SUPERVISOR** | CHANNEL | 7개 | 모든 채널 + queue:manage |

---

### 권한 계층 예시

```
ADMIN (35개 권한 - 전체)
├─ user:* (9개)
├─ org:* (6개)
├─ rbac:* (9개)
├─ report:* (4개)
├─ audit:* (2개)
├─ 채널 전체 (7개)
└─ dashboard, quality (2개)

MANAGER (12개 권한)
├─ user: create, read, update, assign:role, reset:password
├─ org: view, create, update, move
└─ report: view, read, export

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

## 부록: 빠른 참조

### 주요 쿼리 패턴

#### 1. 사용자의 모든 권한 조회
```sql
SELECT DISTINCT p.code
FROM agents a
JOIN agent_roles ar ON a.agent_id = ar.agent_id
JOIN role_permissions rp ON ar.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE a.agent_id = :agentId
  AND a.status = 'ACTIVE'
  AND a.tenant_id = :tenantId;
```

#### 2. 부서의 전체 하위 부서 조회 (트리)
```sql
SELECT *
FROM departments
WHERE org_path LIKE CONCAT(:targetOrgPath, '%')
  AND tenant_id = :tenantId
ORDER BY depth, name;
```

#### 3. 역할에 할당된 모든 권한 조회
```sql
SELECT p.code, p.description
FROM role_permissions rp
JOIN permissions p ON rp.permission_id = p.permission_id
WHERE rp.role_id = :roleId
  AND p.tenant_id = :tenantId;
```

#### 4. 사용자가 특정 권한을 보유했는지 확인
```sql
SELECT EXISTS (
    SELECT 1
    FROM agents a
    JOIN agent_roles ar ON a.agent_id = ar.agent_id
    JOIN role_permissions rp ON ar.role_id = rp.role_id
    JOIN permissions p ON rp.permission_id = p.permission_id
    WHERE a.agent_id = :agentId
      AND p.code = :permissionCode
      AND a.status = 'ACTIVE'
      AND a.tenant_id = :tenantId
) AS has_permission;
```

---

**문서 작성일**: 2026-01-16  
**작성자**: Identity System Team  
**버전**: 2.0  
**상태**: 최종 승인

---

> ⚠️ **주의사항**  
> - 모든 테이블은 tenant_id로 격리되어야 합니다  
> - ID는 반드시 UUID (VARCHAR(36)) 형식을 사용해야 합니다  
> - 삭제 정책(ON DELETE)은 반드시 문서대로 설정해야 합니다  
> - 권한 코드는 `domain:action` 형식을 엄격히 준수해야 합니다

