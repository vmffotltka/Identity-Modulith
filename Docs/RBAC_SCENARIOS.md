# RBAC Scenarios - 역할/권한 시나리오 상세 정의

역할 기반 접근 제어(RBAC) 및 데이터 접근 범위(DataScope) 상세 정의

---

## 1. RBAC 아키텍처 개요

### 1.1 2차원 역할 체계

```
┌─────────────────────────────────────────────────────────────────────┐
│                    2차원 역할 체계                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   [축1: POSITION - 직급/권한 레벨] (필수, 1개)                      │
│   ├── ADMIN         → DataScope: ADMIN (전체 조직)                 │
│   ├── TEAM_LEAD     → DataScope: TEAM_LEAD (팀+하위)               │
│   └── AGENT         → DataScope: MEMBER (본인 팀)                  │
│                                                                     │
│   [축2: CHANNEL - 업무 채널] (선택, 여러 개 가능)                   │
│   ├── VOICE_INBOUND     (인바운드 전화)                             │
│   ├── VOICE_OUTBOUND    (아웃바운드 전화)                           │
│   ├── CHAT              (채팅 상담)                                 │
│   ├── EMAIL             (이메일 상담)                               │
│   └── CALLBACK          (콜백 관리)                                 │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 역할 할당 예시

```
[ 일반 상담사 - 인바운드 전담 ]
roles: [AGENT, VOICE_INBOUND]

[ 멀티채널 상담사 ]
roles: [AGENT, VOICE_INBOUND, CHAT]

[ 아웃바운드 전담 상담사 ]
roles: [AGENT, VOICE_OUTBOUND, CALLBACK]

[ 팀장 - 전체 채널 ]
roles: [TEAM_LEAD, VOICE_INBOUND, VOICE_OUTBOUND, CHAT, EMAIL, CALLBACK]

[ 관리자 ]
roles: [ADMIN]  // 채널 권한 불필요 (관리 업무만)

[ 관리자 + 상담 업무 ]
roles: [ADMIN, VOICE_INBOUND, CHAT]  // 관리 + 상담 가능
```

---

## 2. 데이터 모델

### 2.1 Role (역할)

```
┌─────────────────────────────────────────────────────────────────────┐
│  Role                                                               │
├─────────────────────────────────────────────────────────────────────┤
│  - id: Long (PK)                                                    │
│  - name: String (UK)                                                │
│  - type: RoleType (POSITION | CHANNEL)                              │
│  - dataScope: DataScopeLevel? (POSITION일 때만)                     │
│  - description: String                                              │
│  - createdAt: DateTime                                              │
│  - updatedAt: DateTime                                              │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 Permission (권한)

```
┌─────────────────────────────────────────────────────────────────────┐
│  Permission                                                         │
├─────────────────────────────────────────────────────────────────────┤
│  - id: Long (PK)                                                    │
│  - code: String (UK, 예: "agent:create")                            │
│  - name: String (표시명)                                            │
│  - description: String                                              │
│  - category: String (그룹핑: AGENT, DEPARTMENT, CALL, CHAT 등)      │
│  - createdAt: DateTime                                              │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.3 RolePermission (M:N 관계)

```
┌─────────────────────────────────────────────────────────────────────┐
│  RolePermission                                                     │
├─────────────────────────────────────────────────────────────────────┤
│  - roleId: Long (FK → Role)                                         │
│  - permissionId: Long (FK → Permission)                             │
│  - assignedAt: DateTime                                             │
│  - assignedBy: UUID                                                 │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.4 Enums

```java
// 역할 타입
public enum RoleType {
    POSITION,   // 직급 (ADMIN, TEAM_LEAD, AGENT)
    CHANNEL     // 채널 (VOICE_INBOUND, CHAT 등)
}

// 데이터 접근 범위
public enum DataScopeLevel {
    ADMIN,      // 전체 조직 접근
    TEAM_LEAD,  // 본인 부서 + 하위 부서
    MEMBER      // 본인 부서만
}

// 채널 타입 (고정 Enum)
public enum ChannelType {
    VOICE_INBOUND,      // 인바운드 전화
    VOICE_OUTBOUND,     // 아웃바운드 전화
    CHAT,               // 채팅
    EMAIL,              // 이메일
    CALLBACK            // 콜백
}
```

---

## 3. 기본 역할 정의

### 3.1 POSITION 역할 (시스템 기본)

| Role Name | Type | DataScope | 설명 |
|-----------|------|-----------|------|
| `ADMIN` | POSITION | ADMIN | 시스템 관리자, 전체 조직 접근 |
| `TEAM_LEAD` | POSITION | TEAM_LEAD | 팀장/부서장, 팀+하위 접근 |
| `AGENT` | POSITION | MEMBER | 일반 상담사, 본인 팀만 접근 |

### 3.2 CHANNEL 역할 (시스템 기본)

| Role Name | Type | DataScope | 설명 |
|-----------|------|-----------|------|
| `VOICE_INBOUND` | CHANNEL | - | 인바운드 전화 상담 |
| `VOICE_OUTBOUND` | CHANNEL | - | 아웃바운드 전화 상담 |
| `CHAT` | CHANNEL | - | 채팅 상담 |
| `EMAIL` | CHANNEL | - | 이메일 상담 |
| `CALLBACK` | CHANNEL | - | 콜백 관리 |

---

## 4. Permission 정의

### 4.1 Permission 명명 규칙

```
{resource}:{action}

예시:
- agent:create      (상담사 생성)
- agent:read        (상담사 조회)
- agent:update      (상담사 수정)
- agent:delete      (상담사 삭제/퇴사)
- dept:create       (부서 생성)
- call:receive      (전화 수신)
- chat:send         (채팅 발송)
```

### 4.2 Permission 목록

#### Agent 관련

| Code | Name | Category | 설명 |
|------|------|----------|------|
| `agent:create` | 상담사 생성 | AGENT | 새 상담사 등록 |
| `agent:read` | 상담사 조회 | AGENT | 상담사 정보 조회 |
| `agent:read:self` | 본인 정보 조회 | AGENT | 본인 정보만 조회 |
| `agent:update` | 상담사 수정 | AGENT | 상담사 정보 수정 |
| `agent:update:self` | 본인 정보 수정 | AGENT | 본인 정보만 수정 |
| `agent:delete` | 상담사 퇴사 | AGENT | 퇴사 처리 |
| `agent:suspend` | 상담사 정지 | AGENT | 정지/활성화 |
| `agent:transfer` | 부서 이동 | AGENT | 다른 부서로 이동 |
| `agent:role:assign` | 역할 할당 | AGENT | 역할 부여/제거 |
| `agent:password:reset` | 비밀번호 초기화 | AGENT | 비밀번호 리셋 |

#### Department 관련

| Code | Name | Category | 설명 |
|------|------|----------|------|
| `dept:create` | 부서 생성 | DEPARTMENT | 새 부서 생성 |
| `dept:read` | 부서 조회 | DEPARTMENT | 부서 정보 조회 |
| `dept:update` | 부서 수정 | DEPARTMENT | 부서 정보 수정 |
| `dept:delete` | 부서 삭제 | DEPARTMENT | 부서 삭제 |
| `dept:move` | 부서 이동 | DEPARTMENT | 부서 위치 이동 |
| `dept:deactivate` | 부서 비활성화 | DEPARTMENT | 비활성화/활성화 |

#### Role/Permission 관련

| Code | Name | Category | 설명 |
|------|------|----------|------|
| `role:create` | 역할 생성 | RBAC | 새 역할 생성 |
| `role:read` | 역할 조회 | RBAC | 역할 목록 조회 |
| `role:update` | 역할 수정 | RBAC | 역할 정보 수정 |
| `role:delete` | 역할 삭제 | RBAC | 역할 삭제 |
| `permission:read` | 권한 조회 | RBAC | 권한 목록 조회 |
| `permission:assign` | 권한 할당 | RBAC | 역할에 권한 부여 |

#### Channel 업무 관련

| Code | Name | Category | 설명 |
|------|------|----------|------|
| `call:receive` | 전화 수신 | VOICE | 인바운드 전화 받기 |
| `call:dial` | 전화 발신 | VOICE | 아웃바운드 전화 걸기 |
| `call:transfer` | 전화 전환 | VOICE | 다른 상담사에게 전환 |
| `call:hold` | 전화 보류 | VOICE | 통화 보류 |
| `chat:receive` | 채팅 수신 | CHAT | 채팅 상담 받기 |
| `chat:send` | 채팅 발송 | CHAT | 채팅 메시지 발송 |
| `chat:transfer` | 채팅 전환 | CHAT | 다른 상담사에게 전환 |
| `email:receive` | 이메일 수신 | EMAIL | 이메일 상담 받기 |
| `email:send` | 이메일 발송 | EMAIL | 이메일 답변 발송 |
| `callback:create` | 콜백 등록 | CALLBACK | 콜백 예약 등록 |
| `callback:manage` | 콜백 관리 | CALLBACK | 콜백 수정/삭제 |

#### 리포트 관련

| Code | Name | Category | 설명 |
|------|------|----------|------|
| `report:view` | 리포트 조회 | REPORT | 리포트 보기 |
| `report:export` | 리포트 내보내기 | REPORT | 엑셀/PDF 내보내기 |
| `report:create` | 리포트 생성 | REPORT | 커스텀 리포트 생성 |

---

## 5. Role-Permission 매핑

### 5.1 POSITION 역할별 Permission

#### ADMIN

```
agent:create, agent:read, agent:update, agent:delete
agent:suspend, agent:transfer, agent:role:assign, agent:password:reset
dept:create, dept:read, dept:update, dept:delete, dept:move, dept:deactivate
role:create, role:read, role:update, role:delete
permission:read, permission:assign
report:view, report:export, report:create
```

#### TEAM_LEAD

```
agent:read, agent:update (팀원)
agent:suspend, agent:transfer (팀원)
agent:password:reset (팀원)
dept:read
report:view, report:export
```

#### AGENT

```
agent:read:self, agent:update:self
```

### 5.2 CHANNEL 역할별 Permission

#### VOICE_INBOUND

```
call:receive, call:transfer, call:hold
```

#### VOICE_OUTBOUND

```
call:dial, call:receive, call:transfer, call:hold
callback:create
```

#### CHAT

```
chat:receive, chat:send, chat:transfer
```

#### EMAIL

```
email:receive, email:send
```

#### CALLBACK

```
callback:create, callback:manage
```

---

## 6. DataScope 로직

### 6.1 DataScope 결정

```
Agent의 DataScope = POSITION 역할의 dataScope

예시:
- roles: [ADMIN, VOICE_INBOUND] → DataScope = ADMIN
- roles: [TEAM_LEAD, CHAT] → DataScope = TEAM_LEAD
- roles: [AGENT, VOICE_INBOUND, CHAT] → DataScope = MEMBER
```

### 6.2 DataScope 적용 범위

```
┌────────────────────────────────────────────────────────────────────┐
│                    DataScope 적용                                  │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ADMIN                                                             │
│  └── SELECT * FROM agents WHERE tenant_id = :tenantId             │
│      (테넌트 전체)                                                 │
│                                                                    │
│  TEAM_LEAD (부서ID: 5)                                             │
│  └── SELECT * FROM agents                                          │
│      WHERE tenant_id = :tenantId                                   │
│      AND department_id IN (                                        │
│          SELECT dept_id FROM dept_subtree(5)  -- 5번 부서+하위    │
│      )                                                             │
│                                                                    │
│  MEMBER (부서ID: 10)                                               │
│  └── SELECT * FROM agents                                          │
│      WHERE tenant_id = :tenantId                                   │
│      AND department_id = 10  -- 10번 부서만                        │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 6.3 DataScope 계산 서비스

```java
public class DataScopeService {

    public Set<Long> getAccessibleDepartmentIds(String tenantId, UUID agentId) {
        Agent agent = agentRepository.findById(agentId);
        DataScopeLevel scope = getDataScopeLevel(agent);

        return switch (scope) {
            case ADMIN ->
                // 테넌트 전체 부서
                departmentRepository.findAllIdsByTenantId(tenantId);

            case TEAM_LEAD ->
                // 본인 부서 + 하위 부서 (재귀)
                departmentRepository.findSubtreeIds(tenantId, agent.getDepartmentId());

            case MEMBER ->
                // 본인 부서만
                Set.of(agent.getDepartmentId());
        };
    }

    private DataScopeLevel getDataScopeLevel(Agent agent) {
        return agent.getRoles().stream()
            .filter(role -> role.getType() == RoleType.POSITION)
            .findFirst()
            .map(Role::getDataScope)
            .orElse(DataScopeLevel.MEMBER);
    }
}
```

---

## 7. 시나리오: 역할 생성

### 7.1 Command & Event

```
┌─────────────────────────────┐       ┌─────────────────────────────┐
│     CreateRole              │       │     RoleCreated             │
│     (Command)               │──────▶│     (Event)                 │
├─────────────────────────────┤       ├─────────────────────────────┤
│ name: String                │       │ roleId: Long                │
│ type: RoleType              │       │ name: String                │
│ dataScope: DataScopeLevel?  │       │ type: RoleType              │
│ description: String         │       │ createdAt: DateTime         │
│ actorId: UUID               │       └─────────────────────────────┘
└─────────────────────────────┘
```

### 7.2 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| RC-001 | name은 유일 | DUPLICATE_ROLE |
| RC-002 | POSITION 타입이면 dataScope 필수 | DATASCOPE_REQUIRED |
| RC-003 | CHANNEL 타입이면 dataScope는 null | INVALID_DATASCOPE |
| RC-004 | ADMIN만 역할 생성 가능 | INSUFFICIENT_PERMISSION |

---

## 8. 시나리오: 역할에 권한 할당

### 8.1 Command & Event

```
┌─────────────────────────────┐       ┌─────────────────────────────┐
│  AssignPermissionToRole     │       │  PermissionAssignedToRole   │
│     (Command)               │──────▶│     (Event)                 │
├─────────────────────────────┤       ├─────────────────────────────┤
│ roleName: String            │       │ roleId: Long                │
│ permissionCode: String      │       │ permissionId: Long          │
│ actorId: UUID               │       │ assignedAt: DateTime        │
└─────────────────────────────┘       └─────────────────────────────┘
```

### 8.2 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| PA-001 | Role 존재 확인 | ROLE_NOT_FOUND |
| PA-002 | Permission 존재 확인 | PERMISSION_NOT_FOUND |
| PA-003 | 이미 할당된 경우 무시 | - |
| PA-004 | ADMIN만 권한 할당 가능 | INSUFFICIENT_PERMISSION |

---

## 9. 시나리오: 상담사에게 역할 할당

### 9.1 Command & Event

```
┌─────────────────────────────┐       ┌─────────────────────────────┐
│   AssignRoleToAgent         │       │   RoleAssignedToAgent       │
│     (Command)               │──────▶│     (Event)                 │
├─────────────────────────────┤       ├─────────────────────────────┤
│ tenantId: String            │       │ agentId: UUID               │
│ agentId: UUID               │       │ roleName: String            │
│ roleName: String            │       │ roleType: RoleType          │
│ actorId: UUID               │       │ assignedAt: DateTime        │
└─────────────────────────────┘       └─────────────────────────────┘
```

### 9.2 비즈니스 규칙

| ID | 규칙 | 실패 시 에러 |
|----|------|-------------|
| RA-001 | Agent 존재 확인 | AGENT_NOT_FOUND |
| RA-002 | Role 존재 확인 | ROLE_NOT_FOUND |
| RA-003 | POSITION은 1개만 가능 | POSITION_ALREADY_ASSIGNED |
| RA-004 | 이미 할당된 역할 무시 | - |
| RA-005 | RETIRED 상담사 역할 변경 불가 | AGENT_ALREADY_RETIRED |
| RA-006 | DataScope 검증 (대상 상담사 접근 권한) | INSUFFICIENT_PERMISSION |

### 9.3 POSITION 교체 시나리오

```
현재: Agent.roles = [AGENT, VOICE_INBOUND]
요청: TEAM_LEAD 역할 할당

처리:
1. 기존 POSITION (AGENT) 자동 제거
2. 새 POSITION (TEAM_LEAD) 할당
3. CHANNEL 역할 (VOICE_INBOUND)은 유지

결과: Agent.roles = [TEAM_LEAD, VOICE_INBOUND]
```

---

## 10. 시나리오: 권한 확인

### 10.1 권한 확인 흐름

```
요청: agent:update 권한으로 상담사 수정

┌────────────────────────────────────────────────────────────────────┐
│  1. JWT에서 roles 추출                                             │
│     roles: [TEAM_LEAD, VOICE_INBOUND]                              │
├────────────────────────────────────────────────────────────────────┤
│  2. roles의 permissions 조회 (DB)                                  │
│     TEAM_LEAD → [agent:read, agent:update, ...]                    │
│     VOICE_INBOUND → [call:receive, call:transfer, ...]             │
│     합집합 → [agent:read, agent:update, call:receive, ...]         │
├────────────────────────────────────────────────────────────────────┤
│  3. 요청된 permission 확인                                         │
│     agent:update ∈ permissions? → YES                              │
├────────────────────────────────────────────────────────────────────┤
│  4. DataScope 확인                                                 │
│     TEAM_LEAD → DataScope = TEAM_LEAD                              │
│     대상 상담사가 접근 범위 내? → 확인                              │
├────────────────────────────────────────────────────────────────────┤
│  5. 최종 판단                                                      │
│     Permission OK + DataScope OK → 허용                            │
└────────────────────────────────────────────────────────────────────┘
```

### 10.2 권한 확인 서비스

```java
public class PermissionService {

    public boolean hasPermission(UUID agentId, String permissionCode) {
        Set<String> permissions = getAgentPermissions(agentId);
        return permissions.contains(permissionCode);
    }

    public Set<String> getAgentPermissions(UUID agentId) {
        Agent agent = agentRepository.findById(agentId);

        return agent.getRoles().stream()
            .flatMap(role -> rolePermissionRepository
                .findPermissionsByRole(role.getName())
                .stream())
            .map(Permission::getCode)
            .collect(Collectors.toSet());
    }

    public void checkAccess(UUID actorId, UUID targetAgentId, String permission) {
        // 1. Permission 확인
        if (!hasPermission(actorId, permission)) {
            throw new InsufficientPermissionException();
        }

        // 2. DataScope 확인
        Agent target = agentRepository.findById(targetAgentId);
        Set<Long> accessibleDepts = dataScopeService.getAccessibleDepartmentIds(
            target.getTenantId(), actorId);

        if (!accessibleDepts.contains(target.getDepartmentId())) {
            throw new InsufficientPermissionException();
        }
    }
}
```

---

## 11. KeyCloak 연동

### 11.1 역할 동기화

```
Identity Modulith (Master)           KeyCloak
┌─────────────────────────┐         ┌─────────────────────────┐
│  Agent.roles 변경       │         │  User.roles 업데이트    │
│  [AGENT, VOICE_INBOUND] │ ──────▶ │  [AGENT, VOICE_INBOUND] │
│         ↓               │         │                         │
│  [TEAM_LEAD, VOICE_...] │ ──────▶ │  [TEAM_LEAD, VOICE_...] │
└─────────────────────────┘         └─────────────────────────┘
```

### 11.2 JWT 토큰 구조

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "tenant_id": "nexfron",
  "login_id": "john.doe",
  "roles": ["TEAM_LEAD", "VOICE_INBOUND", "CHAT"],
  "iat": 1704067200,
  "exp": 1704070800
}
```

### 11.3 권한 확인 위치

| 항목 | 위치 | 설명 |
|------|------|------|
| 인증 (Authentication) | KeyCloak | JWT 토큰 발급 |
| 역할 정보 | JWT | roles claim |
| Permission 조회 | Identity DB | roles → permissions |
| DataScope 계산 | Identity DB | POSITION → dataScope |

---

## 12. API 엔드포인트

### 12.1 Role 관리

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/roles` | 역할 생성 |
| GET | `/api/roles` | 역할 목록 조회 |
| GET | `/api/roles/{name}` | 역할 상세 조회 |
| PATCH | `/api/roles/{name}` | 역할 수정 |
| DELETE | `/api/roles/{name}` | 역할 삭제 |

### 12.2 Permission 관리

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/permissions` | 권한 목록 조회 |
| GET | `/api/permissions/{code}` | 권한 상세 조회 |
| POST | `/api/roles/{name}/permissions` | 역할에 권한 할당 |
| DELETE | `/api/roles/{name}/permissions/{code}` | 역할에서 권한 제거 |

### 12.3 Agent 역할 관리

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/agents/{id}/roles` | 상담사 역할 조회 |
| POST | `/api/agents/{id}/roles` | 상담사에 역할 할당 |
| DELETE | `/api/agents/{id}/roles/{name}` | 상담사에서 역할 제거 |
| PUT | `/api/agents/{id}/roles` | 상담사 역할 전체 교체 |

---

## 13. 에러 코드

| Code | HTTP | 설명 |
|------|------|------|
| ROLE_NOT_FOUND | 404 | 역할을 찾을 수 없음 |
| PERMISSION_NOT_FOUND | 404 | 권한을 찾을 수 없음 |
| DUPLICATE_ROLE | 409 | 이미 존재하는 역할 |
| DUPLICATE_PERMISSION | 409 | 이미 존재하는 권한 |
| DATASCOPE_REQUIRED | 400 | POSITION 역할에 DataScope 필수 |
| INVALID_DATASCOPE | 400 | CHANNEL 역할에 DataScope 불가 |
| POSITION_ALREADY_ASSIGNED | 400 | 이미 POSITION 역할 보유 |
| CANNOT_REMOVE_POSITION | 400 | POSITION 역할 제거 불가 (교체만 가능) |
| INSUFFICIENT_PERMISSION | 403 | 권한 부족 |

---

## 14. 비즈니스 규칙 요약

### 14.1 역할 할당 규칙

| 규칙 | 설명 |
|------|------|
| POSITION은 필수 | 모든 상담사는 정확히 1개의 POSITION 역할 필요 |
| CHANNEL은 선택 | 0개 이상의 CHANNEL 역할 가능 |
| POSITION 교체 가능 | 새 POSITION 할당 시 기존 POSITION 자동 제거 |
| CHANNEL 추가/제거 자유 | 언제든 CHANNEL 추가/제거 가능 |

### 14.2 DataScope 규칙

| 규칙 | 설명 |
|------|------|
| POSITION에서 결정 | DataScope는 POSITION 역할에서만 결정 |
| 단일 값 | Agent당 하나의 DataScope (POSITION이 1개이므로) |
| 계층적 적용 | ADMIN > TEAM_LEAD > MEMBER 순으로 넓음 |

### 14.3 Permission 규칙

| 규칙 | 설명 |
|------|------|
| 합집합 적용 | Agent의 모든 역할의 Permission 합집합 |
| DataScope 추가 검증 | Permission 있어도 DataScope 범위 확인 필요 |

---

*문서 버전: 1.0*
*최종 수정: 2026-01-14*
