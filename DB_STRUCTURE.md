# 🏗️ Identity Modulith - 데이터베이스 구조

## 📊 전체 테이블 관계도

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         RBAC (Role-Based Access Control)                 │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐      ┌──────────────────────┐
│   permissions        │      │   permission_groups  │
├──────────────────────┤      ├──────────────────────┤
│ permission_id (PK)   │◄────►│ permission_group_id  │
│ tenant_id            │      │ tenant_id            │
│ code                 │      │ name                 │
│ created_at           │      │ description          │
│                      │      │ is_active            │
│                      │      │ version              │
└──────────────────────┘      │ created_at           │
         ▲                     │ updated_at           │
         │                     └──────────────────────┘
         │                              │
         │ (1:N)                        │
         │                   ┌──────────▼─────────────────┐
         │                   │permission_group_permissions│
         │                   ├──────────────────────────────┤
         │                   │ id (PK)                      │
         │                   │ permission_group_id (FK)     │
         │                   │ permission_id (FK)           │
         │                   │ added_at                     │
         │                   └──────────────────────────────┘
         │
      ┌──┴──────────────────┐
      │    role_permissions │
      ├──────────────────────┤
      │ id (PK)              │
      │ role_id (FK)         │
      │ permission_id (FK)   │
      │ assigned_at          │
      └──────────────────────┘
         ▲                │
         │ (1:N)          │
         │                └─► 권한 할당 (역할 ─→ 권한)
         │
      ┌──┴───────────────────┐
      │      roles           │
      ├──────────────────────┤
      │ role_id (PK)         │
      │ tenant_id            │
      │ name                 │
      │ type                 │
      │ description          │
      │ is_active            │
      │ version              │
      │ created_at           │
      │ updated_at           │
      └──────────────────────┘
         ▲                 ▲
         │ (1:N)           │ (1:N)
         │                 │
      ┌──┴──────────────┬──┴────────────────────┐
      │  agent_roles    │  role_permission_groups│
      ├──────────────────┼──────────────────────────┤
      │ id (PK)          │ id (PK)                  │
      │ agent_id (FK)    │ role_id (FK)             │
      │ role_id (FK)     │ permission_group_id (FK) │
      │ assigned_at      │ assigned_at              │
      └──────────────────┴──────────────────────────┘
         ▲
         │ (1:N)
         │
      ┌──┴───────────────────┐
      │      agents          │
      ├──────────────────────┤
      │ agent_id (PK)        │
      │ tenant_id            │
      │ login_id             │
      │ password             │
      │ name                 │
      │ dept_id (FK)         │
      │ status               │
      │ password_must_change │
      │ created_at           │
      │ retired_at           │
      └──────────────────────┘
         ▲
         │ (N:1)
         │
      ┌──┴───────────────────┐
      │    departments       │
      ├──────────────────────┤
      │ dept_id (PK)         │
      │ tenant_id            │
      │ parent_id (FK - self)│
      │ name                 │
      │ org_path             │
      │ depth                │
      │ type                 │
      │ created_at           │
      └──────────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                          Audit & Archive                                 │
└─────────────────────────────────────────────────────────────────────────┘

      ┌──────────────────────┐
      │   audit_logs         │
      ├──────────────────────┤
      │ audit_id (PK)        │
      │ tenant_id            │
      │ action               │
      │ resource_type        │
      │ resource_id          │
      │ operator_id          │
      │ changes (JSON)       │
      │ timestamp            │
      │ remarks              │
      │ ip_address           │
      └──────────────────────┘
                │
                │ (아카이빙)
                ▼
      ┌──────────────────────┐
      │  audit_logs_archive  │
      ├──────────────────────┤
      │ id (PK)              │
      │ tenant_id            │
      │ action               │
      │ resource_type        │
      │ resource_id          │
      │ operator_id          │
      │ changes (JSON)       │
      │ timestamp            │
      │ archived_at          │
      └──────────────────────┘
```

---

## 📋 현재 상태 (V1_0_8 마이그레이션 후)

### ✅ 생성된 테이블 (총 12개)

**Organization 모듈:**
- `departments` - 조직 구조 (부서)

**User 모듈:**
- `agents` - 사용자 (에이전트)

**RBAC 모듈:**
- `permissions` - 권한 정의
- `roles` - 역할 정의
- `role_permissions` - 역할-권한 매핑
- `agent_roles` - 사용자-역할 할당
- `permission_groups` - 권한 그룹
- `permission_group_permissions` - 권한 그룹-권한 매핑
- `role_permission_groups` - 역할-권한 그룹 할당

**Audit 모듈:**
- `audit_logs` - 감사 로그 (활성)
- `audit_logs_archive` - 감사 로그 (아카이브)

---

## 🔍 agent, agent_roles, role_permissions이 비어있는 이유

**정상입니다!** ✅

마이그레이션 파일 구조:
- **V1_0_0 ~ V1_0_7**: 테이블 구조 생성 (각 버전별 점진적 개선)
- **V1_0_8**: 표준 데이터만 삽입 (테이블 재생성 안 함)

V1_0_8에서 삽입되는 데이터:

```sql
-- Phase 2: 표준 데이터 삽입

1. permissions 테이블 - 10개의 기본 권한 삽입 ✅
2. roles 테이블 - 6개의 기본 역할 삽입 ✅
3. permission_groups 테이블 - 3개의 권한 그룹 삽입 ✅
4. departments 테이블 - 3개의 부서 삽입 ✅

⚠️ agents 테이블 - 의도적으로 데이터 미삽입
   (사용자는 API/UI를 통해 동적으로 생성)
⚠️ agent_roles 테이블 - 의도적으로 데이터 미삽입
   (사용자 생성 후 역할 할당)
⚠️ role_permissions 테이블 - 의도적으로 데이터 미삽입
   (권한 그룹을 통해 관리)
⚠️ permission_group_permissions 테이블 - 의도적으로 데이터 미삽입
   (필요시 API를 통해 추가)
⚠️ role_permission_groups 테이블 - 의도적으로 데이터 미삽입
   (필요시 API를 통해 추가)
```

---

## 🛠️ 현재 데이터 구조

```
departments (3개 행)
├── 본사 (root)
├── 영업부
└── 기술부

permissions (10개 행)
├── user:create
├── user:read
├── user:update
├── user:delete
├── org:create
├── org:read
├── org:update
├── org:delete
├── report:view
└── report:export

roles (6개 행)
├── ADMIN (POSITION)
├── TEAM_LEADER (POSITION)
├── MEMBER (POSITION)
├── PHONE_AGENT (CHANNEL)
├── CHAT_AGENT (CHANNEL)
└── EMAIL_AGENT (CHANNEL)

permission_groups (3개 행)
├── USER_FULL_ACCESS
├── ORGANIZATION_FULL_ACCESS
└── REPORTING_ACCESS

⚠️ agents (0개 행) - 사용자 데이터 없음
⚠️ agent_roles (0개 행) - 사용자-역할 할당 없음
⚠️ role_permissions (0개 행) - 역할-권한 할당 없음
⚠️ permission_group_permissions (0개 행) - 그룹-권한 할당 없음
⚠️ role_permission_groups (0개 행) - 역할-그룹 할당 없음
```

---

## 📝 권장사항

테이블 구조는 **완벽하게 정상**입니다:
- ✅ 모든 FK(Foreign Key) 설정 완료
- ✅ 인덱스 최적화 완료
- ✅ 기본 역할 및 권한 정의 완료
- ✅ 주석(COMMENT) 추가 완료

**다음 단계:**
1. 애플리케이션 UI/API를 통해 사용자 생성
2. 사용자에게 역할 할당 (agent_roles)
3. 역할에 권한 할당 (role_permissions)
4. 권한 그룹-권한 연결 (permission_group_permissions)
5. 역할-권한 그룹 연결 (role_permission_groups)

또는 SQL INSERT 쿼리로 수동으로 데이터를 삽입할 수 있습니다.

