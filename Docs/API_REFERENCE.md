# Identity Modulith - API Reference

> **Base URL**: `http://localhost:8080`
> **인증 방식**: SAML 2.0 SSO (JSESSIONID 쿠키)
> **공통 응답 Content-Type**: `application/json`

---

## 목차

1. [Me API](#1-me-api)
2. [Agent API](#2-agent-api)
3. [RBAC API](#3-rbac-api)
4. [Organization API](#4-organization-api)
5. [공통 에러 응답](#5-공통-에러-응답)

---

## 1. Me API

현재 로그인한 사용자 정보 조회

### `GET /api/me`

현재 SAML 로그인된 사용자의 Agent 정보, 역할, 권한 목록을 반환합니다.

**인증 필요**: ✅

**응답 예시**
```json
{
  "tenantId": "default-tenant",
  "agent": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "loginId": "test.admin",
    "name": "홍길동",
    "organizationId": "dept-uuid-001",
    "departmentName": "개발팀",
    "departmentPath": "/본사/개발본부/개발팀",
    "employeeId": "EMP001",
    "email": "hong@example.com",
    "phone": "010-1234-5678",
    "status": "ACTIVE",
    "passwordMustChange": false,
    "createdAt": "2026-01-01T09:00:00",
    "retiredAt": null,
    "roles": []
  },
  "roles": ["ADMIN"],
  "permissions": ["user:read", "user:write", "org:read"],
  "isAuthenticated": true
}
```

---

### `GET /api/me/status`

로그인 상태만 확인합니다. **인증 없이도 호출 가능**합니다.

**인증 필요**: ❌

**미인증 응답**
```json
{
  "isAuthenticated": false,
  "loginUrl": "/saml2/authenticate/keycloak"
}
```

**인증된 응답**
```json
{
  "isAuthenticated": true,
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "tenantId": "default-tenant",
  "username": "test.admin"
}
```

---

## 2. Agent API

상담사 관리 (`/api/v1/agents`)

**모든 엔드포인트 인증 필요**: ✅

---

### `POST /api/v1/agents`

새로운 상담사를 생성합니다. 임시 비밀번호가 자동 발급됩니다.

**권한**: ADMIN

**Request Body**
```json
{
  "loginId": "agent001",
  "name": "김상담",
  "organizationId": "dept-uuid-001",
  "roles": ["AGENT"],
  "email": "agent001@example.com",
  "phone": "010-1234-5678",
  "employeeId": "EMP002"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `loginId` | string | ✅ | 로그인 ID (4-20자, 영문/숫자/`_`/`.`/`-`) |
| `name` | string | ✅ | 상담사 이름 (최대 100자) |
| `organizationId` | string (UUID) | ✅ | 소속 부서 ID |
| `roles` | string[] | ✅ | 역할 이름 배열 (최소 1개) |
| `email` | string | ❌ | 이메일 |
| `phone` | string | ❌ | 전화번호 (`010-XXXX-XXXX` 형식) |
| `employeeId` | string | ❌ | 사번 |

**성공 응답** `201 Created`
```json
{
  "agentId": "550e8400-e29b-41d4-a716-446655440002",
  "loginId": "agent001",
  "tempPassword": "Temp!2026Ab"
}
```

> ⚠️ `tempPassword`는 최초 1회만 발급됩니다. 반드시 안전하게 전달하세요.

---

### `GET /api/v1/agents`

상담사 목록을 조회합니다.

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `organizationId` | string | - | 조직 ID 필터 |
| `status` | string | - | `ACTIVE` / `SUSPENDED` / `RETIRED` |
| `nameKeyword` | string | - | 이름 부분 검색 |
| `loginIdKeyword` | string | - | 로그인 ID 부분 검색 |
| `includeRetired` | boolean | `false` | 퇴사자 포함 여부 |

**성공 응답** `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "loginId": "test.admin",
    "name": "홍길동",
    "organizationId": "dept-uuid-001",
    "departmentName": "개발팀",
    "departmentPath": "/본사/개발본부/개발팀",
    "employeeId": "EMP001",
    "email": "hong@example.com",
    "phone": "010-1234-5678",
    "status": "ACTIVE",
    "passwordMustChange": false,
    "createdAt": "2026-01-01T09:00:00",
    "retiredAt": null,
    "roles": []
  }
]
```

---

### `GET /api/v1/agents/{agentId}`

상담사 단건 조회.

**Path Parameter**: `agentId` (UUID)

**성공 응답** `200 OK` — Agent 객체 (위와 동일한 구조)

---

### `GET /api/v1/agents/check-login-id`

로그인 ID 중복 체크.

**Query Parameter**: `loginId` (string)

**성공 응답** `200 OK`
```json
{ "isUnique": true }
```

---

### `PATCH /api/v1/agents/{agentId}`

상담사 기본 정보 수정.

**권한**: 본인 또는 ADMIN

**Request Body**
```json
{ "name": "김상담사" }
```

**성공 응답** `204 No Content`

---

### `PATCH /api/v1/agents/{agentId}/organization`

상담사 소속 조직 변경.

**권한**: ADMIN

**Request Body**
```json
{ "organizationId": "new-dept-uuid" }
```

**성공 응답** `204 No Content`

---

### `POST /api/v1/agents/{agentId}/reset-password`

관리자가 상담사 비밀번호를 초기화하고 임시 비밀번호를 발급합니다.

**권한**: ADMIN

**Request Body**: 없음

**성공 응답** `200 OK`
```json
{
  "agentId": "550e8400-e29b-41d4-a716-446655440002",
  "tempPassword": "Reset!2026Xy"
}
```

---

### `POST /api/v1/agents/{agentId}/change-password`

상담사 본인이 비밀번호를 변경합니다.

**권한**: 본인만

**Request Body**
```json
{
  "currentPassword": "OldPass123!",
  "newPassword": "NewPass456!",
  "confirmPassword": "NewPass456!"
}
```

**성공 응답** `204 No Content`

---

### `POST /api/v1/agents/me/change-password`

현재 로그인한 사용자가 본인 비밀번호를 변경합니다. (`agentId` 없이 호출)

**Request Body** — `change-password`와 동일

**성공 응답** `204 No Content`

---

### `POST /api/v1/agents/{agentId}/suspend`

상담사를 정지합니다. ACTIVE → SUSPENDED 전이만 가능.

**권한**: ADMIN (본인 정지 불가)

**성공 응답** `204 No Content`

---

### `POST /api/v1/agents/{agentId}/activate`

정지된 상담사를 활성화합니다. SUSPENDED → ACTIVE 전이만 가능.

**권한**: ADMIN

**성공 응답** `204 No Content`

---

### `DELETE /api/v1/agents/{agentId}`

상담사를 퇴사 처리합니다. 상태가 RETIRED로 변경되며 **복구 불가**합니다.

**권한**: ADMIN (본인 퇴사 처리 불가)

**성공 응답** `204 No Content`

---

### `POST /api/v1/agents/{agentId}/transfer`

상담사를 다른 부서로 이동합니다.

**권한**: ADMIN

**Request Body**
```json
{ "newOrganizationId": "target-dept-uuid" }
```

**성공 응답** `200 OK`
```json
{
  "agentId": "550e8400-...",
  "fromOrganizationId": "old-dept-uuid",
  "toOrganizationId": "new-dept-uuid",
  "transferredAt": "2026-03-11T10:00:00"
}
```

---

### `PUT /api/v1/agents/{agentId}/roles`

상담사 역할을 일괄 지정합니다. 기존 역할은 **모두 제거**됩니다.

**권한**: ADMIN

**Request Body** (아래 중 하나 사용)
```json
// 방식 1: 역할 이름으로 지정
{ "roleNames": ["ADMIN", "VOICE_INBOUND"] }

// 방식 2: 역할 ID로 지정
{ "roleIds": ["role-uuid-001", "role-uuid-002"] }
```

**성공 응답** `204 No Content`

---

### `POST /api/v1/agents/{agentId}/roles/{roleName}`

상담사에게 역할을 개별 추가합니다. 기존 역할은 **유지**됩니다.

**권한**: ADMIN

**Path Parameter**: `roleName` (예: `SENIOR_AGENT`)

**성공 응답** `201 Created`

---

### `DELETE /api/v1/agents/{agentId}/roles/{roleName}`

상담사에게서 특정 역할을 제거합니다.

**권한**: ADMIN

**성공 응답** `204 No Content`

---

### `GET /api/v1/agents/statistics`

전체 상담사 통계 조회.

**성공 응답** `200 OK`
```json
{
  "totalCount": 50,
  "activeCount": 45,
  "suspendedCount": 3,
  "retiredCount": 2,
  "passwordChangeRequired": 5,
  "byOrganization": {
    "dept-uuid-001": 20,
    "dept-uuid-002": 30
  },
  "byStatus": {
    "ACTIVE": 45,
    "SUSPENDED": 3,
    "RETIRED": 2
  }
}
```

---

### `GET /api/v1/agents/statistics/organization/{organizationId}`

조직별 상담사 통계 조회.

**성공 응답** `200 OK` — 위와 동일한 구조

---

## 3. RBAC API

역할 기반 접근 제어 관리 (`/api/rbac`)

**모든 엔드포인트 인증 필요**: ✅

---

### 역할(Role) 관리

#### `GET /api/rbac/roles`
모든 역할 조회.

**성공 응답** `200 OK`
```json
[
  {
    "roleId": "role-uuid-001",
    "name": "ADMIN",
    "type": "POSITION",
    "dataScopeLevel": "ALL",
    "description": "관리자 역할",
    "isActive": true,
    "permissions": [],
    "userCount": 3,
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-01T00:00:00"
  }
]
```

#### `GET /api/rbac/roles/{roleName}`
특정 역할 조회.

#### `POST /api/rbac/roles`
역할 생성. **권한**: ADMIN

**Request Body**
```json
{
  "name": "SENIOR_AGENT",
  "type": "POSITION",
  "description": "시니어 상담사"
}
```
**성공 응답** `201 Created` — RoleResponse 객체

#### `PATCH /api/rbac/roles/{roleName}`
역할 수정. **권한**: ADMIN

**Request Body**
```json
{
  "type": "POSITION",
  "description": "수정된 설명",
  "isActive": true
}
```

#### `DELETE /api/rbac/roles/{roleName}?forceDelete=false`
역할 삭제. **권한**: ADMIN

| 파라미터 | 기본값 | 설명 |
|---------|--------|------|
| `forceDelete` | `false` | 사용자가 있어도 강제 삭제 |

**성공 응답** `200 OK`
```json
{
  "roleName": "SENIOR_AGENT",
  "affectedAgentCount": 0,
  "success": true,
  "message": "역할이 삭제되었습니다"
}
```

#### `GET /api/rbac/roles/{roleName}/deletion-impact`
역할 삭제 시 영향도 조회.

**성공 응답** `200 OK`
```json
{
  "roleName": "SENIOR_AGENT",
  "affectedAgentCount": 5,
  "affectedPermissionCount": 3
}
```

#### `POST /api/rbac/roles/{roleName}/deactivate`
역할 비활성화. **권한**: ADMIN. **성공 응답** `200 OK`

#### `POST /api/rbac/roles/{roleName}/activate`
역할 활성화. **권한**: ADMIN. **성공 응답** `200 OK`

---

### 권한(Permission) 관리

#### `GET /api/rbac/permissions`
모든 권한 조회.

**성공 응답** `200 OK`
```json
[
  {
    "permissionId": "perm-uuid-001",
    "code": "user:read",
    "name": "사용자 조회",
    "description": "사용자 정보를 조회할 수 있는 권한",
    "category": "READ",
    "resource": "user",
    "action": "read"
  }
]
```

#### `GET /api/rbac/permissions/{code}`
특정 권한 조회. (`code` 예: `user:read`)

#### `POST /api/rbac/permissions`
권한 생성. **권한**: ADMIN

**Request Body**
```json
{
  "code": "report:read",
  "name": "리포트 조회",
  "description": "리포트 조회 권한",
  "category": "READ",
  "resource": "report",
  "action": "read"
}
```
**성공 응답** `201 Created`

#### `PATCH /api/rbac/permissions/{code}`
권한 수정. **권한**: ADMIN

**Request Body**
```json
{
  "description": "수정된 설명",
  "category": "READ"
}
```

#### `DELETE /api/rbac/permissions/{code}`
권한 삭제. **권한**: ADMIN. **성공 응답** `204 No Content`

---

### 역할-권한 할당

#### `GET /api/rbac/roles/{roleName}/permissions`
역할에 할당된 권한 목록 조회.

#### `POST /api/rbac/roles/{roleName}/permissions/{permissionCode}`
역할에 권한 할당. **권한**: ADMIN. **성공 응답** `201 Created`

#### `DELETE /api/rbac/roles/{roleName}/permissions/{permissionCode}`
역할에서 권한 제거. **권한**: ADMIN. **성공 응답** `204 No Content`

#### `POST /api/rbac/roles/{roleName}/permissions/batch`
역할에 여러 권한 일괄 할당. **권한**: ADMIN

**Request Body**
```json
{ "permissionCodes": ["user:read", "user:write", "org:read"] }
```
**성공 응답** `200 OK`
```json
{
  "successCount": 3,
  "failCount": 0,
  "results": []
}
```

#### `DELETE /api/rbac/roles/{roleName}/permissions/batch`
역할에서 여러 권한 일괄 제거. **권한**: ADMIN

**Request Body** — batch 할당과 동일. **성공 응답** `200 OK`

---

### 사용자-역할 할당

#### `GET /api/rbac/agents/{agentId}/roles`
상담사의 역할 목록 조회.

**성공 응답** `200 OK`
```json
["ADMIN", "VOICE_INBOUND"]
```

#### `GET /api/rbac/agents/{agentId}/effective-permissions`
상담사의 실제 유효 권한 목록 조회 (모든 역할의 권한 합산).

**성공 응답** `200 OK`
```json
["user:read", "user:write", "org:read", "org:create"]
```

#### `POST /api/rbac/agents/{agentId}/roles/{roleName}`
상담사에게 역할 할당. **권한**: ADMIN. **성공 응답** `201 Created`

#### `DELETE /api/rbac/agents/{agentId}/roles/{roleName}`
상담사에게서 역할 회수. **권한**: ADMIN. **성공 응답** `204 No Content`

---

### 통계 & 역검색

#### `GET /api/rbac/permissions/{permissionCode}/roles`
특정 권한을 가진 역할 목록 조회.

**성공 응답** `200 OK`
```json
["ADMIN", "MANAGER"]
```

#### `GET /api/rbac/roles/{roleName}/agent-count`
역할이 할당된 상담사 수 조회.

**성공 응답** `200 OK`
```json
5
```

---

## 4. Organization API

조직(부서) 관리 (`/api/org/departments`)

**모든 엔드포인트 인증 필요**: ✅

---

### 부서 생성 & 수정

#### `POST /api/org/departments`
부서 생성. **권한**: ADMIN (헤더 `X-User-Id` 필요)

**Request Header**: `X-User-Id: {agentId-uuid}`

**Request Body**
```json
{
  "name": "플랫폼개발팀",
  "type": "TEAM",
  "code": "DEV-PLT",
  "customTypeName": null,
  "parentId": "parent-dept-uuid"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | string | ✅ | 부서명 |
| `type` | string | ✅ | `COMPANY` / `DIVISION` / `TEAM` / `GROUP` / `CUSTOM` |
| `code` | string | ✅ | 부서 코드 (테넌트 내 고유, 2-30자) |
| `customTypeName` | string | `CUSTOM`일 때 필수 | 커스텀 타입명 |
| `parentId` | string (UUID) | ❌ | 상위 부서 ID (없으면 루트 부서) |

**성공 응답** `201 Created` — DepartmentResponse 객체

#### `PATCH /api/org/departments/{deptId}`
부서 정보 수정.

**Request Header**: `X-User-Id: {agentId-uuid}`

**Request Body**
```json
{
  "name": "수정된 부서명",
  "type": "TEAM"
}
```

**성공 응답** `200 OK` — DepartmentResponse 객체

---

### 부서 조회

**DepartmentResponse 객체 구조**
```json
{
  "deptId": "dept-uuid-001",
  "name": "플랫폼개발팀",
  "type": "TEAM",
  "orgPath": "/본사-uuid/개발본부-uuid/플랫폼개발팀-uuid",
  "depth": 2,
  "parentId": "개발본부-uuid",
  "status": "ACTIVE",
  "children": []
}
```

#### `GET /api/org/departments`
전체 조직도 트리 조회.

**성공 응답** `200 OK` — DepartmentResponse[] (트리 구조, `children` 포함)

#### `GET /api/org/departments/scoped`
현재 사용자의 접근 범위 내 조직도 조회.

**Request Header**: `X-User-Id: {agentId-uuid}`

- ADMIN: 전체 부서
- TEAM_LEAD: 자신 부서 + 하위 부서
- MEMBER: 자신 부서만

**성공 응답** `200 OK` — DepartmentResponse[] (트리 구조)

#### `GET /api/org/departments/search?keyword={keyword}`
부서명으로 검색.

**성공 응답** `200 OK` — DepartmentResponse[]

#### `GET /api/org/departments/{deptId}/subtree`
특정 부서 및 모든 하위 부서 조회.

**성공 응답** `200 OK` — DepartmentResponse[]

#### `GET /api/org/departments/by-depth?depth={depth}`
특정 깊이의 부서 조회 (0 = 루트).

**성공 응답** `200 OK` — DepartmentResponse[]

#### `GET /api/org/departments/by-type?type={type}`
특정 타입의 부서 조회.

**성공 응답** `200 OK` — DepartmentResponse[]

---

### 부서 통계 & 멤버

#### `GET /api/org/departments/{deptId}/statistics`
부서 통계 조회.

**Request Header**: `X-User-Id: {agentId-uuid}`

**성공 응답** `200 OK`
```json
{
  "deptId": "dept-uuid-001",
  "name": "플랫폼개발팀",
  "type": "TEAM",
  "depth": 2,
  "totalEmployees": 15,
  "activeEmployees": 13,
  "childDeptCount": 2,
  "descendantDeptCount": 5
}
```

#### `GET /api/org/departments/{deptId}/members?includeSubDepartments=false`
부서 소속 상담사 목록 조회.

**Request Header**: `X-User-Id: {agentId-uuid}`

**성공 응답** `200 OK`
```json
{
  "deptId": "dept-uuid-001",
  "deptName": "플랫폼개발팀",
  "includeSubDepartments": false,
  "totalCount": 15,
  "activeCount": 13,
  "retiredCount": 2,
  "members": [
    {
      "userId": "agent-uuid-001",
      "loginId": "hong.gildong",
      "name": "홍길동",
      "deptId": "dept-uuid-001",
      "jobTitle": "팀장",
      "status": "ACTIVE"
    }
  ]
}
```

---

### 부서 이동 & 상태 관리

#### `PUT /api/org/departments/{deptId}/move`
부서를 다른 부서 하위로 이동.

**Request Header**: `X-User-Id: {agentId-uuid}`

**Request Body**
```json
{ "newParentId": "new-parent-dept-uuid" }
```

**성공 응답** `204 No Content`

#### `POST /api/org/departments/{deptId}/deactivate`
부서 비활성화. 활성 하위 부서가 없어야 함.

**Request Header**: `X-User-Id: {agentId-uuid}`

**성공 응답** `204 No Content`

#### `POST /api/org/departments/{deptId}/activate`
부서 활성화. 상위 부서가 활성이어야 함.

**Request Header**: `X-User-Id: {agentId-uuid}`

**성공 응답** `204 No Content`

---

### 부서 삭제

#### `DELETE /api/org/departments/{deptId}`
부서 삭제. 하위 부서 없고 활성 소속 직원 없을 때만 가능.

**Request Header**: `X-User-Id: {agentId-uuid}`

**성공 응답** `204 No Content`

---

## 5. 공통 에러 응답

### 에러 응답 형식

```json
{
  "timestamp": "2026-03-11T12:00:00",
  "status": 400,
  "code": "INVALID_INPUT_VALUE",
  "message": "loginId는 4-20자이며 알파벳, 숫자, _, ., -만 포함 가능합니다."
}
```

### HTTP 상태 코드별 에러 코드 목록

#### 400 Bad Request
| code | 설명 |
|------|------|
| `INVALID_INPUT_VALUE` | 요청 필드 형식/값 오류 |
| `BUSINESS_RULE_VIOLATION` | 비즈니스 규칙 위반 |
| `INVALID_STATUS_TRANSITION` | 허용되지 않는 상태 전이 |
| `PASSWORD_MISMATCH` | 현재 비밀번호 불일치 |
| `SAME_AS_CURRENT_PASSWORD` | 새 비밀번호 = 현재 비밀번호 |
| `PASSWORD_CONFIRMATION_MISMATCH` | 새 비밀번호 확인 불일치 |

#### 401 Unauthorized
| code | 설명 |
|------|------|
| `UNAUTHORIZED` | 미인증 사용자 |

#### 404 Not Found
| code | 설명 |
|------|------|
| `AGENT_NOT_FOUND` | 상담사를 찾을 수 없음 |
| `ORGANIZATION_NOT_FOUND` | 부서를 찾을 수 없음 |
| `ROLE_NOT_FOUND` | 역할을 찾을 수 없음 |
| `PERMISSION_NOT_FOUND` | 권한을 찾을 수 없음 |

#### 409 Conflict
| code | 설명 |
|------|------|
| `DUPLICATE_USERNAME` | 이미 존재하는 로그인 ID |
| `DATA_INTEGRITY_VIOLATION` | DB 무결성 제약 위반 |
| `DUPLICATE_DEPT_CODE` | 이미 존재하는 부서 코드 |
| `ROLE_ALREADY_EXISTS` | 이미 존재하는 역할 |

#### 410 Gone
| code | 설명 |
|------|------|
| `AGENT_ALREADY_RETIRED` | 이미 퇴사한 상담사 (복구 불가) |

#### 500 Internal Server Error
| code | 설명 |
|------|------|
| `INTERNAL_ERROR` | 서버 내부 오류 |

