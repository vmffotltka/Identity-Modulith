# Event Storming - Identity Modulith

콜센터 상담사 관리 시스템의 비즈니스 정의

---

## 1. 시스템 개요

### 1.1 핵심 도메인
**콜센터 상담사 관리 (Call Center Agent Management)**

### 1.2 주요 액터
| Actor | 설명 | 권한 수준 |
|-------|------|----------|
| **Admin** | 전체 시스템 관리자 | 모든 조직/상담사 접근 |
| **Team Lead** | 팀장/부서장 | 소속 부서 및 하위 부서 접근 |
| **Agent** | 일반 상담사 | 본인 정보만 접근 |

### 1.3 모듈 구조
```
identity-modulith/
├── user/           # 상담사(Agent) 관리
├── organization/   # 조직(Department) 관리
└── rbac/           # 역할/권한(Role/Permission) 관리
```

---

## 2. User 모듈 - 상담사 관리

### 2.1 Aggregate: Agent

```
┌─────────────────────────────────────────────────────────┐
│  Agent (Aggregate Root)                                 │
├─────────────────────────────────────────────────────────┤
│  - id: UUID                                             │
│  - tenantId: String                                     │
│  - loginId: String (unique per tenant)                  │
│  - password: String (encrypted)                         │
│  - name: String                                         │
│  - departmentId: Long                                   │
│  - status: AgentStatus                                  │
│  - passwordMustChange: Boolean                          │
│  - roles: Set<Role>                                     │
│  - createdAt: DateTime                                  │
│  - retiredAt: DateTime?                                 │
└─────────────────────────────────────────────────────────┘
```

### 2.2 Value Objects

```
┌──────────────────────────┐    ┌──────────────────────────┐
│  AgentStatus (Enum)      │    │  Role (VO)               │
├──────────────────────────┤    ├──────────────────────────┤
│  - ACTIVE                │    │  - name: String          │
│  - SUSPENDED             │    │  - type: RoleType        │
│  - RETIRED               │    │                          │
└──────────────────────────┘    └──────────────────────────┘

┌──────────────────────────┐
│  RoleType (Enum)         │
├──────────────────────────┤
│  - POSITION (직급)       │
│  - CHANNEL (채널권한)    │
└──────────────────────────┘
```

### 2.3 Commands & Events

#### 상담사 생성
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────────┐
│   CreateAgent   │────▶│     Agent       │────▶│   AgentCreated      │
│   (Command)     │     │   (Aggregate)   │     │   (Event)           │
└─────────────────┘     └─────────────────┘     └─────────────────────┘
                                                         │
                                                         ▼
                                               ┌─────────────────────┐
                                               │ 임시 비밀번호 생성   │
                                               │ passwordMustChange  │
                                               │ = true              │
                                               └─────────────────────┘
```

| Command | Event | 비즈니스 규칙 |
|---------|-------|--------------|
| `CreateAgent` | `AgentCreated` | 임시 비밀번호 자동 생성, passwordMustChange=true |
| `RetireAgent` | `AgentRetired` | status=RETIRED, retiredAt 기록, 물리삭제 금지 |
| `TransferAgent` | `AgentTransferred` | 다른 부서로 이동, 대상 부서 존재 확인 필요 |
| `SuspendAgent` | `AgentSuspended` | status=SUSPENDED, 로그인 차단 |
| `ActivateAgent` | `AgentActivated` | status=ACTIVE, 로그인 허용 |
| `ResetPassword` | `PasswordReset` | 임시 비밀번호 생성, passwordMustChange=true |
| `ChangePassword` | `PasswordChanged` | 본인이 변경, passwordMustChange=false |
| `AssignRole` | `RoleAssigned` | 역할 추가 |
| `RevokeRole` | `RoleRevoked` | 역할 제거 |

### 2.4 상태 전이 다이어그램

```
                    CreateAgent
                         │
                         ▼
    ┌──────────────────────────────────────┐
    │               ACTIVE                  │◀──────────┐
    └──────────────────────────────────────┘           │
         │                    │                         │
         │ SuspendAgent       │ RetireAgent    ActivateAgent
         ▼                    ▼                         │
    ┌──────────────┐    ┌──────────────┐               │
    │  SUSPENDED   │    │   RETIRED    │               │
    └──────────────┘    └──────────────┘               │
         │                    │                         │
         │                    │ (종료상태, 복구불가)     │
         └────────────────────┴─────────────────────────┘
```

### 2.5 비즈니스 규칙 (Invariants)

| ID | 규칙 | 설명 |
|----|------|------|
| U-001 | loginId는 테넌트 내 유일 | 중복 로그인ID 생성 불가 |
| U-002 | RETIRED 상태는 복구 불가 | 퇴사자는 재활성화 불가 |
| U-003 | RETIRED/SUSPENDED는 로그인 불가 | ACTIVE만 로그인 허용 |
| U-004 | 비밀번호는 조회 응답에 포함 금지 | 보안 규칙 |
| U-005 | 임시 비밀번호는 일회성 | 생성/초기화 시에만 반환 |
| U-006 | 부서 이동 시 대상 부서 존재 확인 | 존재하지 않는 부서로 이동 불가 |

---

## 3. Organization 모듈 - 조직 관리

### 3.1 Aggregate: Department

```
┌─────────────────────────────────────────────────────────┐
│  Department (Aggregate Root)                            │
├─────────────────────────────────────────────────────────┤
│  - id: Long                                             │
│  - tenantId: String                                     │
│  - name: String                                         │
│  - code: String (unique per tenant)                     │
│  - type: DepartmentType                                 │
│  - parentId: Long? (self-referential)                   │
│  - status: DepartmentStatus                             │
│  - createdAt: DateTime                                  │
│  - deactivatedAt: DateTime?                             │
└─────────────────────────────────────────────────────────┘
```

### 3.2 Value Objects

```
┌──────────────────────────┐    ┌──────────────────────────┐
│  DepartmentType (Enum)   │    │  DepartmentStatus (Enum) │
├──────────────────────────┤    ├──────────────────────────┤
│  - COMPANY (회사)        │    │  - ACTIVE                │
│  - DIVISION (본부)       │    │  - INACTIVE              │
│  - TEAM (팀)             │    │                          │
│  - GROUP (그룹)          │    │                          │
│  - CUSTOM (커스텀)       │    │                          │
└──────────────────────────┘    └──────────────────────────┘
```

**하이브리드 타입 설계**:
- 기본 타입(COMPANY/DIVISION/TEAM/GROUP)으로 표준화
- CUSTOM 타입으로 유동적인 조직 구조 지원
- type=CUSTOM일 때 `customTypeName` 필드 필수

```
┌─────────────────────────────────────────────────────────┐
│  Department (수정된 구조)                               │
├─────────────────────────────────────────────────────────┤
│  - id: Long                                             │
│  - tenantId: String                                     │
│  - name: String                                         │
│  - code: String (unique per tenant)                     │
│  - type: DepartmentType                                 │
│  - customTypeName: String? (type=CUSTOM일 때만)         │
│  - parentId: Long? (self-referential)                   │
│  - status: DepartmentStatus                             │
│  - createdAt: DateTime                                  │
│  - deactivatedAt: DateTime?                             │
└─────────────────────────────────────────────────────────┘
```

**사용 예시**:
```
ABC금융 (COMPANY)
  ├── 서울센터 (CUSTOM: "센터")
  │       ├── 인바운드팀 (TEAM)
  │       └── 아웃바운드팀 (TEAM)
  ├── 부산센터 (CUSTOM: "센터")
  │       └── 통합상담팀 (TEAM)
  └── 기술본부 (DIVISION)
          └── 개발팀 (TEAM)
```

### 3.3 Commands & Events

| Command | Event | 비즈니스 규칙 |
|---------|-------|--------------|
| `CreateDepartment` | `DepartmentCreated` | 부모 부서 존재 확인, 루트면 parent=null |
| `RenameDepartment` | `DepartmentRenamed` | 이름 변경 |
| `MoveDepartment` | `DepartmentMoved` | 순환 참조 검사 필수 |
| `DeactivateDepartment` | `DepartmentDeactivated` | 하위부서/활성사용자 없어야 함 |
| `ActivateDepartment` | `DepartmentActivated` | 비활성 부서 재활성화 |
| `DeleteDepartment` | `DepartmentDeleted` | 물리삭제, 엄격한 조건 필요 |

### 3.4 트리 구조

```
      ┌─────────────┐
      │   회사(ROOT)  │
      └──────┬──────┘
             │
    ┌────────┼────────┐
    ▼        ▼        ▼
┌──────┐ ┌──────┐ ┌──────┐
│본부 A │ │본부 B │ │본부 C │
└──┬───┘ └──────┘ └──────┘
   │
   ├──────────┐
   ▼          ▼
┌──────┐  ┌──────┐
│ 팀 1  │  │ 팀 2  │
└──────┘  └──────┘
```

### 3.5 비즈니스 규칙 (Invariants)

| ID | 규칙 | 설명 |
|----|------|------|
| O-001 | 순환 참조 금지 | 자기 자신/하위 부서를 부모로 설정 불가 |
| O-002 | 하위 부서 존재 시 삭제 불가 | 삭제 전 하위 부서 먼저 처리 |
| O-003 | 활성 사용자 존재 시 삭제 불가 | 소속 사용자 먼저 이동/퇴사 처리 |
| O-004 | code는 테넌트 내 유일 | 부서 코드 중복 불가 |
| O-005 | INACTIVE 부서에 사용자 배치 불가 | 비활성 부서로 이동 불가 |

---

## 4. RBAC 모듈 - 역할/권한 관리

### 4.1 Aggregates

```
┌─────────────────────────┐      ┌─────────────────────────┐
│  Role (Aggregate)       │      │  Permission (Aggregate) │
├─────────────────────────┤      ├─────────────────────────┤
│  - id: Long             │      │  - id: Long             │
│  - name: String (UK)    │      │  - code: String (UK)    │
│  - type: RoleType       │      │  - description: String  │
│  - description: String  │      └─────────────────────────┘
│  - permissions: Set     │
└─────────────────────────┘

        ┌────────────────────────────────┐
        │  Role ◀────── M:N ──────▶ Permission  │
        └────────────────────────────────┘
```

### 4.2 Commands & Events

| Command | Event | 비즈니스 규칙 |
|---------|-------|--------------|
| `CreateRole` | `RoleCreated` | 역할 생성 |
| `DeleteRole` | `RoleDeleted` | 사용 중인 역할 삭제 주의 |
| `CreatePermission` | `PermissionCreated` | 권한 생성 |
| `DeletePermission` | `PermissionDeleted` | 사용 중인 권한 삭제 주의 |
| `AssignPermissionToRole` | `PermissionAssignedToRole` | 역할에 권한 부여 |
| `RevokePermissionFromRole` | `PermissionRevokedFromRole` | 역할에서 권한 제거 |

### 4.3 데이터 접근 범위 (Data Scope)

```
┌──────────────────────────────────────────────────────────────┐
│                    DataScopeLevel                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   ADMIN      ─────────▶  전체 조직 접근 가능                 │
│                                                              │
│   TEAM_LEAD  ─────────▶  본인 부서 + 하위 부서 접근          │
│                                                              │
│   MEMBER     ─────────▶  본인 부서만 접근                    │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 4.4 비즈니스 규칙 (Invariants)

| ID | 규칙 | 설명 |
|----|------|------|
| R-001 | Role name 유일 | 동일 이름 역할 중복 불가 |
| R-002 | Permission code 유일 | 동일 코드 권한 중복 불가 |
| R-003 | DataScope는 역할에서 결정 | Agent의 역할에 따라 접근 범위 자동 계산 |

---

## 5. 모듈 간 통신

### 5.1 의존성 방향

```
         ┌─────────────┐
         │   RBAC      │
         └──────┬──────┘
                │ (조회)
                ▼
         ┌─────────────┐
         │    User     │◀─────────────┐
         └──────┬──────┘              │
                │                     │ (Port-Adapter)
                │                     │
                ▼                     │
         ┌─────────────┐              │
         │ Organization │─────────────┘
         └─────────────┘
```

### 5.2 Port-Adapter 패턴

```
Organization 모듈                    User 모듈
┌─────────────────┐                ┌─────────────────┐
│  OrgUserPort    │ ◀───────────── │  UserModuleApi  │
│  (Interface)    │   implements   │  (Public API)   │
└─────────────────┘                └─────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│  OrgUserPort 메서드                                      │
├─────────────────────────────────────────────────────────┤
│  + existsActiveUserInDepartment(tenantId, deptId)       │
│  + findUserByIdWithOrgInfo(tenantId, userId)            │
│  + findActiveUsersByDeptIds(tenantId, deptIds)          │
└─────────────────────────────────────────────────────────┘
```

---

## 6. Cross-Cutting Concerns

### 6.1 Multi-Tenancy
- 모든 Aggregate에 `tenantId` 포함
- 모든 쿼리에 tenantId 조건 필수
- 테넌트 간 데이터 격리

### 6.2 Audit Trail (향후 고려)
```
┌─────────────────────────────────────────────────────────┐
│  AuditLog                                               │
├─────────────────────────────────────────────────────────┤
│  - id: UUID                                             │
│  - tenantId: String                                     │
│  - aggregateType: String                                │
│  - aggregateId: String                                  │
│  - eventType: String                                    │
│  - actorId: UUID                                        │
│  - timestamp: DateTime                                  │
│  - payload: JSON                                        │
└─────────────────────────────────────────────────────────┘
```

### 6.3 Database Resilience
- 모든 Repository 호출에 retry 로직 적용
- 연결 실패 시 자동 재시도

---

## 7. 정책 (Policies)

### 7.1 이벤트 반응형 정책

| Trigger Event | Policy | Action |
|---------------|--------|--------|
| `AgentCreated` | 임시 비밀번호 알림 | 관리자에게 임시 비밀번호 전달 |
| `AgentRetired` | 접근 권한 회수 | 모든 세션 종료, 권한 제거 |
| `DepartmentDeactivated` | 소속 사용자 알림 | 소속 사용자에게 부서 변경 안내 |

### 7.2 Level 2 RBAC 정책

```
사용자 요청
    │
    ▼
┌────────────────────────────────────────────────────────┐
│  1. 사용자의 역할에서 DataScopeLevel 결정              │
│     - ADMIN 역할 보유 → ADMIN                         │
│     - TEAM_LEAD 역할 보유 → TEAM_LEAD                 │
│     - 그 외 → MEMBER                                  │
├────────────────────────────────────────────────────────┤
│  2. 접근 가능 부서 ID 계산                             │
│     - ADMIN: 전체 부서                                │
│     - TEAM_LEAD: 본인 부서 + 하위 부서 (재귀)         │
│     - MEMBER: 본인 부서만                             │
├────────────────────────────────────────────────────────┤
│  3. 요청 대상 부서가 접근 가능 범위 내인지 확인        │
│     - 범위 내 → 허용                                  │
│     - 범위 외 → INSUFFICIENT_PERMISSION               │
└────────────────────────────────────────────────────────┘
```

---

## 8. 에러 코드

### User 모듈
| Code | HTTP | 설명 |
|------|------|------|
| AGENT_NOT_FOUND | 404 | 상담사를 찾을 수 없음 |
| DUPLICATE_LOGIN_ID | 409 | 이미 존재하는 로그인ID |
| AGENT_ALREADY_RETIRED | 400 | 이미 퇴사 처리된 상담사 |
| INVALID_PASSWORD | 400 | 잘못된 비밀번호 |
| AGENT_SUSPENDED | 403 | 정지된 상담사 |

### Organization 모듈
| Code | HTTP | 설명 |
|------|------|------|
| DEPT_NOT_FOUND | 404 | 부서를 찾을 수 없음 |
| INVALID_PARENT | 400 | 부모 부서가 존재하지 않음 |
| CIRCULAR_REFERENCE | 400 | 순환 참조 발생 |
| CHILD_DEPT_EXISTS | 409 | 하위 부서 존재로 삭제 불가 |
| ACTIVE_USERS_EXIST | 409 | 활성 사용자 존재로 삭제 불가 |
| INSUFFICIENT_PERMISSION | 403 | 권한 부족 |

### RBAC 모듈
| Code | HTTP | 설명 |
|------|------|------|
| ROLE_NOT_FOUND | 404 | 역할을 찾을 수 없음 |
| PERMISSION_NOT_FOUND | 404 | 권한을 찾을 수 없음 |
| DUPLICATE_ROLE | 409 | 이미 존재하는 역할 |
| DUPLICATE_PERMISSION | 409 | 이미 존재하는 권한 |

---

## 9. 외부 시스템 연동

### 9.1 KeyCloak 연동 (Identity-first)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Identity-first 동기화                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   Identity Modulith              KeyCloak                          │
│   ┌─────────────┐               ┌─────────────┐                    │
│   │ CreateAgent │ ────────────▶ │ Create User │                    │
│   └─────────────┘   Event       └─────────────┘                    │
│                                                                     │
│   ┌─────────────┐               ┌─────────────┐                    │
│   │ RetireAgent │ ────────────▶ │ Disable User│                    │
│   └─────────────┘   Event       └─────────────┘                    │
│                                                                     │
│   ┌─────────────┐               ┌──────────────┐                   │
│   │SuspendAgent │ ────────────▶ │ Disable User │                   │
│   └─────────────┘   Event       └──────────────┘                   │
│                                                                     │
│   ┌─────────────┐               ┌──────────────┐                   │
│   │ActivateAgent│ ────────────▶ │ Enable User  │                   │
│   └─────────────┘   Event       └──────────────┘                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

| 이벤트 | KeyCloak 동작 | 설명 |
|--------|--------------|------|
| `AgentCreated` | Create User | KeyCloak에 사용자 생성 |
| `AgentRetired` | Disable User | KeyCloak 사용자 비활성화 |
| `AgentSuspended` | Disable User | KeyCloak 사용자 비활성화 |
| `AgentActivated` | Enable User | KeyCloak 사용자 활성화 |
| `PasswordReset` | Reset Password | KeyCloak 비밀번호 초기화 |
| `RoleAssigned` | Assign Role | KeyCloak 역할 할당 |
| `RoleRevoked` | Revoke Role | KeyCloak 역할 제거 |

### 9.2 KeyCloak 연동 Port

```
┌─────────────────────────────────────────────────────────────────────┐
│  KeyCloakPort (Interface)                                          │
├─────────────────────────────────────────────────────────────────────┤
│  + createUser(tenantId, loginId, tempPassword, name)               │
│  + disableUser(tenantId, loginId)                                  │
│  + enableUser(tenantId, loginId)                                   │
│  + resetPassword(tenantId, loginId, tempPassword)                  │
│  + assignRole(tenantId, loginId, roleName)                         │
│  + revokeRole(tenantId, loginId, roleName)                         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 10. 결정 사항 요약

### 10.1 확정된 설계 결정

| 항목 | 결정 | 비고 |
|------|------|------|
| Agent 상태 | ACTIVE / SUSPENDED / RETIRED | 3단계 상태 관리 |
| Department 상태 | ACTIVE / INACTIVE | 2단계 + 물리 삭제 가능 |
| DepartmentType | 하이브리드 (Enum + CUSTOM) | COMPANY/DIVISION/TEAM/GROUP + 커스텀 타입 |
| 부서 코드 | code 필드 포함 | 테넌트 내 유일, ERP 연동 등에 활용 |
| 역할 타입 | POSITION / CHANNEL | 직급과 채널권한 구분 |
| DataScope | ADMIN / TEAM_LEAD / MEMBER | 3단계 접근 제어 |
| 인증 | KeyCloak 외부 시스템 | Identity-first 동기화 |

### 10.2 향후 결정 필요

- [ ] 비밀번호 정책(복잡도, 만료 등) 세부 사항
- [ ] Audit Trail 구현 여부 및 범위

---

*문서 버전: 1.2*
*최종 수정: 2026-01-13*
