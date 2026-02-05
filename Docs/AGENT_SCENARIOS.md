# Agent Scenarios - 상담사 시나리오 상세 정의

상담사(Agent) 라이프사이클 전체 플로우 및 비즈니스 규칙 상세 정의

---

## 1. Agent 데이터 모델 (확장)

### 1.1 Agent Aggregate (상세)

```
┌─────────────────────────────────────────────────────────────────────┐
│  Agent (Aggregate Root)                                             │
├─────────────────────────────────────────────────────────────────────┤
│  [식별]                                                             │
│  - id: UUID (PK)                                                    │
│  - tenantId: String                                                 │
│  - loginId: String (UK per tenant)                                  │
│  - employeeId: String? (사번, 선택)                                 │
│                                                                     │
│  [인증]                                                             │
│  - password: String (encrypted)                                     │
│  - passwordMustChange: Boolean                                      │
│                                                                     │
│  [기본 정보]                                                        │
│  - name: String                                                     │
│  - email: String?                                                   │
│  - phone: String?                                                   │
│                                                                     │
│  [조직]                                                             │
│  - departmentId: Long                                               │
│  - roles: Set<Role>                                                 │
│                                                                     │
│  [상태]                                                             │
│  - status: AgentStatus                                              │
│  - suspendedAt: DateTime?                                           │
│  - retiredAt: DateTime?                                             │
│  - scheduledDeleteAt: DateTime? (예약 삭제 일시)                    │
│                                                                     │
│  [감사]                                                             │
│  - createdAt: DateTime                                              │
│  - updatedAt: DateTime                                              │
│  - createdBy: UUID?                                                 │
│  - updatedBy: UUID?                                                 │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Value Objects

```java
// 역할
public record Role(String name, RoleType type) {}

public enum RoleType {
    POSITION,   // 직급 (사원, 대리, 과장, 팀장 등)
    CHANNEL     // 채널 권한 (전화, 채팅, 이메일 등)
}

// 상태
public enum AgentStatus {
    ACTIVE,     // 활성 (정상 근무)
    SUSPENDED,  // 정지 (임시 차단)
    RETIRED     // 퇴사 (종료 상태)
}

// 퇴사 삭제 정책
public enum RetireDeletePolicy {
    IMMEDIATE,      // 즉시 익명화
    SCHEDULED,      // 예약 삭제 (retentionDays 후)
    PRESERVE        // 영구 보존
}
```

---

## 2. 시나리오: 상담사 생성 (입사)

### 2.1 Command & Event

```
┌───────────────────────┐         ┌───────────────────────┐
│     CreateAgent       │         │     AgentCreated      │
│     (Command)         │────────▶│     (Event)           │
├───────────────────────┤         ├───────────────────────┤
│ tenantId: String      │         │ agentId: UUID         │
│ loginId: String       │         │ tenantId: String      │
│ name: String          │         │ loginId: String       │
│ employeeId: String?   │         │ name: String          │
│ email: String?        │         │ departmentId: Long    │
│ phone: String?        │         │ tempPassword: String  │
│ departmentId: Long    │         │ roles: Set<Role>      │
│ roles: Set<Role>      │         │ createdAt: DateTime   │
└───────────────────────┘         └───────────────────────┘
```

### 2.2 플로우

```
요청 수신
    │
    ▼
┌────────────────────────────────────────────────────────┐
│  1. 검증                                               │
│     - loginId 형식 검증 (영문+숫자, 4-20자)            │
│     - loginId 중복 검사 (테넌트 내)                    │
│     - departmentId 존재 및 ACTIVE 상태 확인            │
│     - roles 최소 1개 이상 확인                         │
│     - email 형식 검증 (있는 경우)                      │
│     - phone 형식 검증 (있는 경우)                      │
├────────────────────────────────────────────────────────┤
│  2. 임시 비밀번호 생성                                 │
│     - 8-12자, 영문 대소문자 + 숫자 조합                │
│     - 특수문자 포함 옵션 (설정 가능)                   │
├────────────────────────────────────────────────────────┤
│  3. Agent 생성                                         │
│     - status = ACTIVE                                  │
│     - passwordMustChange = true                        │
│     - createdAt = now()                                │
├────────────────────────────────────────────────────────┤
│  4. 이벤트 발행                                        │
│     - AgentCreated 이벤트 발행                         │
│     - KeyCloak 동기화 트리거                           │
├────────────────────────────────────────────────────────┤
│  5. 응답 반환                                          │
│     - agentId, 임시 비밀번호 포함                      │
│     - 임시 비밀번호는 이 응답에서만 확인 가능!         │
└────────────────────────────────────────────────────────┘
```

### 2.3 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| C-001 | loginId는 테넌트 내 유일 | DUPLICATE_LOGIN_ID |
| C-002 | departmentId는 존재하고 ACTIVE | DEPT_NOT_FOUND / DEPT_INACTIVE |
| C-003 | roles는 최소 1개 이상 | INVALID_REQUEST |
| C-004 | 임시 비밀번호는 응답에서 1회만 제공 | - |
| C-005 | INACTIVE 부서에 배치 불가 | DEPT_INACTIVE |

### 2.4 API 요청/응답

```json
// POST /api/agents

// Request
{
  "loginId": "john.doe",
  "name": "홍길동",
  "employeeId": "EMP001",        // 선택
  "email": "john@example.com",  // 선택
  "phone": "010-1234-5678",     // 선택
  "departmentId": 5,
  "roles": [
    { "name": "AGENT", "type": "POSITION" },
    { "name": "VOICE", "type": "CHANNEL" }
  ]
}

// Response (201 Created)
{
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "loginId": "john.doe",
  "name": "홍길동",
  "tempPassword": "Abc12345",   // 이 응답에서만 확인 가능!
  "passwordMustChange": true,
  "status": "ACTIVE",
  "createdAt": "2026-01-13T10:00:00"
}
```

---

## 3. 시나리오: 상담사 정지 (Suspend)

### 3.1 Command & Event

```
┌───────────────────────┐         ┌───────────────────────┐
│     SuspendAgent      │         │    AgentSuspended     │
│     (Command)         │────────▶│     (Event)           │
├───────────────────────┤         ├───────────────────────┤
│ tenantId: String      │         │ agentId: UUID         │
│ agentId: UUID         │         │ tenantId: String      │
│ actorId: UUID         │         │ suspendedAt: DateTime │
└───────────────────────┘         │ suspendedBy: UUID     │
                                  └───────────────────────┘
```

### 3.2 플로우

```
요청 수신
    │
    ▼
┌────────────────────────────────────────────────────────┐
│  1. 검증                                               │
│     - Agent 존재 확인                                  │
│     - 현재 상태 확인 (ACTIVE만 정지 가능)              │
│     - 권한 확인 (본인 정지 불가, DataScope 검증)       │
├────────────────────────────────────────────────────────┤
│  2. 상태 변경                                          │
│     - status = SUSPENDED                               │
│     - suspendedAt = now()                              │
├────────────────────────────────────────────────────────┤
│  3. 이벤트 발행                                        │
│     - AgentSuspended 이벤트                            │
│     - KeyCloak: Disable User                           │
│     - 세션 강제 종료 트리거                            │
└────────────────────────────────────────────────────────┘
```

### 3.3 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| S-001 | ACTIVE 상태만 정지 가능 | INVALID_STATUS_TRANSITION |
| S-002 | 본인 정지 불가 | CANNOT_SUSPEND_SELF |
| S-003 | 정지 시 모든 활성 세션 종료 | - |
| S-004 | 정지된 상담사는 로그인 불가 | AGENT_SUSPENDED |

### 3.4 상태 전이

```
        SuspendAgent
ACTIVE ────────────────▶ SUSPENDED
  ▲                           │
  │                           │
  └───────────────────────────┘
        ActivateAgent
```

---

## 4. 시나리오: 상담사 활성화 (Activate)

### 4.1 Command & Event

```
┌───────────────────────┐         ┌───────────────────────┐
│    ActivateAgent      │         │    AgentActivated     │
│     (Command)         │────────▶│     (Event)           │
├───────────────────────┤         ├───────────────────────┤
│ tenantId: String      │         │ agentId: UUID         │
│ agentId: UUID         │         │ tenantId: String      │
│ actorId: UUID         │         │ activatedAt: DateTime │
└───────────────────────┘         │ activatedBy: UUID     │
                                  └───────────────────────┘
```

### 4.2 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| A-001 | SUSPENDED 상태만 활성화 가능 | INVALID_STATUS_TRANSITION |
| A-002 | RETIRED는 활성화 불가 (복구 불가) | AGENT_ALREADY_RETIRED |
| A-003 | 소속 부서가 INACTIVE면 활성화 불가 | DEPT_INACTIVE |

---

## 5. 시나리오: 상담사 퇴사 (Retire)

### 5.1 Command & Event

```
┌───────────────────────┐         ┌───────────────────────┐
│     RetireAgent       │         │     AgentRetired      │
│     (Command)         │────────▶│     (Event)           │
├───────────────────────┤         ├───────────────────────┤
│ tenantId: String      │         │ agentId: UUID         │
│ agentId: UUID         │         │ tenantId: String      │
│ actorId: UUID         │         │ retiredAt: DateTime   │
│ deletePolicy: Enum    │         │ deletePolicy: Enum    │
│ retentionDays: Int?   │         │ scheduledDeleteAt:    │
└───────────────────────┘         │   DateTime?           │
                                  └───────────────────────┘
```

### 5.2 삭제 정책 (DeletePolicy)

```
┌────────────────────────────────────────────────────────────────────┐
│                     RetireDeletePolicy                             │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  IMMEDIATE (즉시 익명화)                                           │
│  ├── loginId → "deleted_" + UUID                                   │
│  ├── name → "탈퇴회원"                                             │
│  ├── email → null                                                  │
│  ├── phone → null                                                  │
│  ├── employeeId → null                                             │
│  └── status = RETIRED, retiredAt = now()                           │
│                                                                    │
│  SCHEDULED (예약 삭제)                                              │
│  ├── status = RETIRED, retiredAt = now()                           │
│  ├── scheduledDeleteAt = now() + retentionDays                     │
│  ├── 스케줄러가 scheduledDeleteAt에 도달하면:                       │
│  │   └── IMMEDIATE와 동일하게 익명화 처리                           │
│  └── 또는 물리 삭제 (설정에 따라)                                   │
│                                                                    │
│  PRESERVE (영구 보존)                                               │
│  ├── status = RETIRED, retiredAt = now()                           │
│  └── 데이터 그대로 보존 (감사 목적)                                 │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 5.3 플로우

```
요청 수신
    │
    ▼
┌────────────────────────────────────────────────────────┐
│  1. 검증                                               │
│     - Agent 존재 확인                                  │
│     - 이미 RETIRED가 아닌지 확인                       │
│     - 권한 확인 (DataScope 검증)                       │
│     - 본인 퇴사 처리 불가                              │
├────────────────────────────────────────────────────────┤
│  2. deletePolicy에 따른 처리                           │
│                                                        │
│     [IMMEDIATE]                                        │
│     - 즉시 개인정보 익명화                             │
│     - status = RETIRED, retiredAt = now()              │
│                                                        │
│     [SCHEDULED]                                        │
│     - status = RETIRED, retiredAt = now()              │
│     - scheduledDeleteAt = now() + retentionDays        │
│                                                        │
│     [PRESERVE]                                         │
│     - status = RETIRED, retiredAt = now()              │
│     - 데이터 변경 없음                                 │
├────────────────────────────────────────────────────────┤
│  3. 이벤트 발행                                        │
│     - AgentRetired 이벤트                              │
│     - KeyCloak: Disable User                           │
│     - 세션 강제 종료                                   │
├────────────────────────────────────────────────────────┤
│  4. 후속 처리 (비동기)                                 │
│     - 모든 역할 제거                                   │
│     - 관련 권한 회수                                   │
└────────────────────────────────────────────────────────┘
```

### 5.4 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| R-001 | 이미 RETIRED인 경우 재퇴사 불가 | AGENT_ALREADY_RETIRED |
| R-002 | RETIRED 상태는 복구 불가 | CANNOT_REACTIVATE_RETIRED |
| R-003 | 본인 퇴사 처리 불가 | CANNOT_RETIRE_SELF |
| R-004 | SCHEDULED 시 retentionDays 필수 | INVALID_REQUEST |
| R-005 | 퇴사자는 로그인 불가 | AGENT_RETIRED |

### 5.5 API 요청/응답

```json
// POST /api/agents/{agentId}/retire

// Request
{
  "deletePolicy": "SCHEDULED",
  "retentionDays": 365  // SCHEDULED일 때 필수
}

// Response (200 OK)
{
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RETIRED",
  "retiredAt": "2026-01-13T10:00:00",
  "scheduledDeleteAt": "2027-01-13T10:00:00",
  "deletePolicy": "SCHEDULED"
}
```

---

## 6. 시나리오: 부서 이동 (Transfer)

### 6.1 Command & Event

```
┌───────────────────────┐         ┌───────────────────────┐
│    TransferAgent      │         │   AgentTransferred    │
│     (Command)         │────────▶│     (Event)           │
├───────────────────────┤         ├───────────────────────┤
│ tenantId: String      │         │ agentId: UUID         │
│ agentId: UUID         │         │ tenantId: String      │
│ newDepartmentId: Long │         │ fromDeptId: Long      │
│ actorId: UUID         │         │ toDeptId: Long        │
└───────────────────────┘         │ transferredAt:        │
                                  │   DateTime            │
                                  └───────────────────────┘
```

### 6.2 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| T-001 | 대상 부서 존재 및 ACTIVE 확인 | DEPT_NOT_FOUND / DEPT_INACTIVE |
| T-002 | 동일 부서로 이동 불가 | SAME_DEPARTMENT |
| T-003 | RETIRED 상담사 이동 불가 | AGENT_ALREADY_RETIRED |
| T-004 | 행위자의 DataScope에 양쪽 부서 포함 필요 | INSUFFICIENT_PERMISSION |

---

## 7. 시나리오: 비밀번호 초기화 (Reset Password)

### 7.1 Command & Event

```
┌───────────────────────┐         ┌───────────────────────┐
│    ResetPassword      │         │    PasswordReset      │
│     (Command)         │────────▶│     (Event)           │
├───────────────────────┤         ├───────────────────────┤
│ tenantId: String      │         │ agentId: UUID         │
│ agentId: UUID         │         │ tenantId: String      │
│ actorId: UUID         │         │ tempPassword: String  │
└───────────────────────┘         │ resetAt: DateTime     │
                                  │ resetBy: UUID         │
                                  └───────────────────────┘
```

### 7.2 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| P-001 | RETIRED 상담사 비밀번호 초기화 불가 | AGENT_ALREADY_RETIRED |
| P-002 | 초기화 후 passwordMustChange = true | - |
| P-003 | 임시 비밀번호는 응답에서 1회만 제공 | - |
| P-004 | KeyCloak 동기화 필수 | - |

---

## 8. 시나리오: 비밀번호 변경 (Change Password)

### 8.1 Command & Event

```
┌───────────────────────┐         ┌───────────────────────┐
│   ChangePassword      │         │   PasswordChanged     │
│     (Command)         │────────▶│     (Event)           │
├───────────────────────┤         ├───────────────────────┤
│ tenantId: String      │         │ agentId: UUID         │
│ agentId: UUID         │         │ tenantId: String      │
│ currentPassword: Str  │         │ changedAt: DateTime   │
│ newPassword: String   │         └───────────────────────┘
└───────────────────────┘
```

### 8.2 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| PC-001 | 현재 비밀번호 검증 필수 | INVALID_PASSWORD |
| PC-002 | 새 비밀번호 != 현재 비밀번호 | SAME_PASSWORD |
| PC-003 | 변경 후 passwordMustChange = false | - |
| PC-004 | 본인만 변경 가능 | CANNOT_CHANGE_OTHERS_PASSWORD |

---

## 9. 시나리오: 역할 관리 (Role Management)

### 9.1 역할 할당

```
┌───────────────────────┐         ┌───────────────────────┐
│     AssignRole        │         │     RoleAssigned      │
│     (Command)         │────────▶│     (Event)           │
├───────────────────────┤         ├───────────────────────┤
│ tenantId: String      │         │ agentId: UUID         │
│ agentId: UUID         │         │ tenantId: String      │
│ role: Role            │         │ role: Role            │
│ actorId: UUID         │         │ assignedAt: DateTime  │
└───────────────────────┘         └───────────────────────┘
```

### 9.2 역할 제거

```
┌───────────────────────┐         ┌───────────────────────┐
│     RevokeRole        │         │     RoleRevoked       │
│     (Command)         │────────▶│     (Event)           │
├───────────────────────┤         ├───────────────────────┤
│ tenantId: String      │         │ agentId: UUID         │
│ agentId: UUID         │         │ tenantId: String      │
│ role: Role            │         │ role: Role            │
│ actorId: UUID         │         │ revokedAt: DateTime   │
└───────────────────────┘         └───────────────────────┘
```

### 9.3 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| RL-001 | 역할이 존재해야 함 | ROLE_NOT_FOUND |
| RL-002 | 중복 역할 할당 불가 | ROLE_ALREADY_ASSIGNED |
| RL-003 | 최소 1개 역할 유지 필요 | CANNOT_REMOVE_LAST_ROLE |
| RL-004 | RETIRED 상담사 역할 변경 불가 | AGENT_ALREADY_RETIRED |

---

## 10. 예약 삭제 스케줄러

### 10.1 처리 흐름

```
┌────────────────────────────────────────────────────────────────────┐
│                    Scheduled Delete Job                            │
│                    (매일 자정 실행)                                 │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  1. 대상 조회                                                      │
│     SELECT * FROM agents                                           │
│     WHERE status = 'RETIRED'                                       │
│     AND scheduledDeleteAt <= now()                                 │
│     AND scheduledDeleteAt IS NOT NULL                              │
│                                                                    │
│  2. 각 대상에 대해:                                                │
│     ├── 개인정보 익명화                                            │
│     │   - loginId → "deleted_" + UUID                              │
│     │   - name → "탈퇴회원"                                        │
│     │   - email → null                                             │
│     │   - phone → null                                             │
│     │   - employeeId → null                                        │
│     │                                                              │
│     └── scheduledDeleteAt = null (처리 완료 표시)                  │
│                                                                    │
│  3. 처리 결과 로깅                                                 │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

---

## 11. 전체 상태 전이 다이어그램

```
                         CreateAgent
                              │
                              ▼
       ┌──────────────────────────────────────────────────┐
       │                    ACTIVE                         │
       │                                                   │
       │  [가능한 작업]                                    │
       │  - TransferAgent (부서 이동)                      │
       │  - ResetPassword (비밀번호 초기화)               │
       │  - ChangePassword (비밀번호 변경)                │
       │  - AssignRole / RevokeRole (역할 관리)           │
       │  - UpdateProfile (정보 수정)                     │
       └──────────────────────────────────────────────────┘
              │                           │
              │ SuspendAgent              │ RetireAgent
              ▼                           ▼
       ┌──────────────┐           ┌──────────────────────┐
       │  SUSPENDED   │           │      RETIRED          │
       │              │           │                       │
       │  [가능한 작업]│           │  [가능한 작업]        │
       │  - 없음      │           │  - 조회만 가능        │
       │              │           │  - 복구 불가 (최종)   │
       └──────────────┘           └──────────────────────┘
              │                           │
              │ ActivateAgent             │
              ▼                           │
       ┌──────────────┐                   │
       │    ACTIVE    │                   │
       │   (복귀)     │                   │
       └──────────────┘                   │
                                          │
                      ┌───────────────────┴───────────────────┐
                      │           DeletePolicy                │
                      ├───────────────────┬───────────────────┤
                      │     IMMEDIATE     │    SCHEDULED      │
                      │   (즉시 익명화)   │  (예약 익명화)    │
                      └───────────────────┴───────────────────┘
```

---

## 12. Edge Cases 및 예외 처리

### 12.1 동시성 이슈

| 시나리오 | 처리 방법 |
|---------|----------|
| 동일 Agent 동시 수정 | Optimistic Locking (version 필드) |
| 퇴사 처리 중 역할 변경 시도 | 상태 검증 후 에러 반환 |
| 부서 삭제 중 해당 부서로 이동 시도 | 부서 상태 검증 |

### 12.2 KeyCloak 동기화 실패

| 시나리오 | 처리 방법 |
|---------|----------|
| 생성 시 KeyCloak 실패 | 트랜잭션 롤백 또는 재시도 큐 |
| 상태 변경 시 KeyCloak 실패 | 재시도 큐, 수동 동기화 옵션 |
| KeyCloak 응답 지연 | 비동기 처리, 타임아웃 설정 |

### 12.3 데이터 정합성

| 시나리오 | 처리 방법 |
|---------|----------|
| loginId 중복 race condition | DB Unique 제약조건으로 보장 |
| 부서 삭제 vs 사용자 이동 | 부서 삭제 시 소속 사용자 검증 |
| 스케줄러 중복 실행 | 분산 락 또는 단일 인스턴스 보장 |

---

## 13. API 엔드포인트 요약

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/agents` | 상담사 생성 |
| GET | `/api/agents/{id}` | 상담사 조회 |
| GET | `/api/agents` | 상담사 목록 조회 |
| PATCH | `/api/agents/{id}` | 상담사 정보 수정 |
| POST | `/api/agents/{id}/suspend` | 정지 |
| POST | `/api/agents/{id}/activate` | 활성화 |
| POST | `/api/agents/{id}/retire` | 퇴사 |
| POST | `/api/agents/{id}/transfer` | 부서 이동 |
| POST | `/api/agents/{id}/reset-password` | 비밀번호 초기화 |
| POST | `/api/agents/{id}/change-password` | 비밀번호 변경 |
| POST | `/api/agents/{id}/roles` | 역할 할당 |
| DELETE | `/api/agents/{id}/roles/{roleName}` | 역할 제거 |

---

*문서 버전: 1.0*
*최종 수정: 2026-01-13*
