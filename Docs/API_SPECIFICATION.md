# API Specification - REST API 명세

Identity Modulith REST API 명세서

---

## 1. 개요

### 1.1 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL (User) | `/api/v1` |
| Base URL (Organization) | `/api/org` |
| Base URL (RBAC) | `/api/rbac` |
| Content-Type | `application/json` |
| 인증 | Bearer Token (JWT) |

### 1.2 공통 헤더

```http
Authorization: Bearer {jwt_token}
X-Tenant-Id: {tenant_id}
Content-Type: application/json
```

### 1.3 공통 응답 형식

**성공 응답**
```json
{
  "data": { ... },
  "timestamp": "2026-01-14T10:00:00Z"
}
```

**에러 응답**
```json
{
  "error": {
    "code": "AGENT_NOT_FOUND",
    "message": "상담사를 찾을 수 없습니다.",
    "details": { ... }
  },
  "timestamp": "2026-01-14T10:00:00Z"
}
```

### 1.4 페이지네이션

```json
{
  "data": [ ... ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

## 2. Agent API (상담사 관리)

### 2.1 상담사 생성

```http
POST /api/v1/agents
```

**Request Body**
```json
{
  "loginId": "john.doe",
  "name": "홍길동",
  "employeeId": "EMP001",
  "email": "john@example.com",
  "phone": "010-1234-5678",
  "departmentId": 5,
  "roles": ["AGENT", "VOICE_INBOUND"]
}
```

**Response (201 Created)**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "loginId": "john.doe",
    "name": "홍길동",
    "tempPassword": "Abc12345",
    "passwordMustChange": true,
    "status": "ACTIVE",
    "departmentId": 5,
    "roles": ["AGENT", "VOICE_INBOUND"],
    "createdAt": "2026-01-14T10:00:00Z"
  }
}
```

**에러**
| Code | HTTP | 설명 |
|------|------|------|
| DUPLICATE_LOGIN_ID | 409 | 이미 존재하는 로그인ID |
| DEPT_NOT_FOUND | 404 | 부서를 찾을 수 없음 |
| DEPT_INACTIVE | 400 | 비활성 부서에 배치 불가 |
| ROLE_NOT_FOUND | 404 | 역할을 찾을 수 없음 |

---

### 2.2 상담사 목록 조회

```http
GET /api/v1/agents
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| page | int | N | 페이지 번호 (기본: 0) |
| size | int | N | 페이지 크기 (기본: 20, 최대: 100) |
| status | string | N | 상태 필터 (ACTIVE, SUSPENDED, RETIRED) |
| departmentId | long | N | 부서 필터 |
| search | string | N | 이름/로그인ID 검색 |

**Response (200 OK)**
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "loginId": "john.doe",
      "name": "홍길동",
      "employeeId": "EMP001",
      "email": "john@example.com",
      "status": "ACTIVE",
      "departmentId": 5,
      "departmentName": "인바운드팀",
      "roles": ["AGENT", "VOICE_INBOUND"],
      "createdAt": "2026-01-14T10:00:00Z"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3
  }
}
```

---

### 2.3 상담사 상세 조회

```http
GET /api/v1/agents/{agentId}
```

**Response (200 OK)**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "loginId": "john.doe",
    "name": "홍길동",
    "employeeId": "EMP001",
    "email": "john@example.com",
    "phone": "010-1234-5678",
    "status": "ACTIVE",
    "passwordMustChange": false,
    "departmentId": 5,
    "departmentName": "인바운드팀",
    "departmentPath": "넥스프론 > 고객서비스본부 > 인바운드팀",
    "roles": [
      { "name": "AGENT", "type": "POSITION" },
      { "name": "VOICE_INBOUND", "type": "CHANNEL" }
    ],
    "createdAt": "2026-01-14T10:00:00Z",
    "updatedAt": "2026-01-14T12:00:00Z"
  }
}
```

---

### 2.4 상담사 정보 수정

```http
PATCH /api/v1/agents/{agentId}
```

**Request Body**
```json
{
  "name": "홍길동",
  "email": "new@example.com",
  "phone": "010-9999-8888"
}
```

**Response (200 OK)**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "홍길동",
    "email": "new@example.com",
    "phone": "010-9999-8888",
    "updatedAt": "2026-01-14T12:00:00Z"
  }
}
```

---

### 2.5 상담사 정지

```http
POST /api/v1/agents/{agentId}/suspend
```

**Request Body**
```json
{}
```

**Response (200 OK)**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "SUSPENDED",
    "suspendedAt": "2026-01-14T12:00:00Z"
  }
}
```

**에러**
| Code | HTTP | 설명 |
|------|------|------|
| INVALID_STATUS_TRANSITION | 400 | ACTIVE 상태만 정지 가능 |
| CANNOT_SUSPEND_SELF | 400 | 본인 정지 불가 |

---

### 2.6 상담사 활성화

```http
POST /api/v1/agents/{agentId}/activate
```

**Response (200 OK)**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "ACTIVE",
    "activatedAt": "2026-01-14T12:00:00Z"
  }
}
```

**에러**
| Code | HTTP | 설명 |
|------|------|------|
| INVALID_STATUS_TRANSITION | 400 | SUSPENDED 상태만 활성화 가능 |
| AGENT_ALREADY_RETIRED | 400 | 퇴사자는 활성화 불가 |

---

### 2.7 상담사 퇴사

```http
POST /api/v1/agents/{agentId}/retire
```

**Request Body**
```json
{
  "deletePolicy": "SCHEDULED",
  "retentionDays": 365
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| deletePolicy | enum | Y | IMMEDIATE, SCHEDULED, PRESERVE |
| retentionDays | int | * | SCHEDULED일 때 필수 (1-3650) |

**Response (200 OK)**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "RETIRED",
    "retiredAt": "2026-01-14T12:00:00Z",
    "scheduledDeleteAt": "2027-01-14T12:00:00Z",
    "deletePolicy": "SCHEDULED"
  }
}
```

---

### 2.8 부서 이동

```http
POST /api/v1/agents/{agentId}/transfer
```

**Request Body**
```json
{
  "departmentId": 10
}
```

**Response (200 OK)**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "fromDepartmentId": 5,
    "toDepartmentId": 10,
    "transferredAt": "2026-01-14T12:00:00Z"
  }
}
```

---

### 2.9 비밀번호 초기화

```http
POST /api/v1/agents/{agentId}/reset-password
```

**Response (200 OK)**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "tempPassword": "NewPass123",
    "passwordMustChange": true,
    "resetAt": "2026-01-14T12:00:00Z"
  }
}
```

---

### 2.10 비밀번호 변경 (본인)

```http
POST /api/v1/agents/me/change-password
```

**Request Body**
```json
{
  "currentPassword": "OldPass123",
  "newPassword": "NewPass456"
}
```

**Response (200 OK)**
```json
{
  "data": {
    "passwordMustChange": false,
    "changedAt": "2026-01-14T12:00:00Z"
  }
}
```

---

### 2.11 역할 할당

```http
POST /api/v1/agents/{agentId}/roles
```

**Request Body**
```json
{
  "roleName": "CHAT"
}
```

**Response (200 OK)**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "roles": ["AGENT", "VOICE_INBOUND", "CHAT"],
    "assignedAt": "2026-01-14T12:00:00Z"
  }
}
```

---

### 2.12 역할 제거

```http
DELETE /api/v1/agents/{agentId}/roles/{roleName}
```

**Response (200 OK)**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "roles": ["AGENT", "VOICE_INBOUND"],
    "revokedAt": "2026-01-14T12:00:00Z"
  }
}
```

---

### 2.13 역할 전체 교체

```http
PUT /api/v1/agents/{agentId}/roles
```

**Request Body**
```json
{
  "roles": ["TEAM_LEAD", "VOICE_INBOUND", "CHAT"]
}
```

**Response (200 OK)**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "roles": ["TEAM_LEAD", "VOICE_INBOUND", "CHAT"],
    "updatedAt": "2026-01-14T12:00:00Z"
  }
}
```

---

## 3. Department API (부서 관리)

### 3.1 부서 생성

```http
POST /api/v1/departments
```

**Request Body**
```json
{
  "name": "인바운드팀",
  "code": "INBOUND-01",
  "type": "TEAM",
  "parentId": 5
}
```

**커스텀 타입**
```json
{
  "name": "서울센터",
  "code": "SEOUL-CENTER",
  "type": "CUSTOM",
  "customTypeName": "센터",
  "parentId": 3
}
```

**Response (201 Created)**
```json
{
  "data": {
    "id": 10,
    "name": "인바운드팀",
    "code": "INBOUND-01",
    "type": "TEAM",
    "parentId": 5,
    "status": "ACTIVE",
    "createdAt": "2026-01-14T10:00:00Z"
  }
}
```

---

### 3.2 조직도 트리 조회

```http
GET /api/v1/departments/tree
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| includeInactive | boolean | N | 비활성 부서 포함 (기본: false) |

**Response (200 OK)**
```json
{
  "data": {
    "id": 1,
    "name": "넥스프론",
    "code": "NEXFRON",
    "type": "COMPANY",
    "status": "ACTIVE",
    "children": [
      {
        "id": 2,
        "name": "고객서비스본부",
        "code": "CS-HQ",
        "type": "DIVISION",
        "status": "ACTIVE",
        "children": [
          {
            "id": 5,
            "name": "서울센터",
            "code": "SEOUL-CENTER",
            "type": "CUSTOM",
            "customTypeName": "센터",
            "status": "ACTIVE",
            "children": [
              {
                "id": 10,
                "name": "인바운드팀",
                "code": "INBOUND-01",
                "type": "TEAM",
                "status": "ACTIVE",
                "children": []
              }
            ]
          }
        ]
      }
    ]
  }
}
```

---

### 3.3 부서 상세 조회

```http
GET /api/v1/departments/{deptId}
```

**Response (200 OK)**
```json
{
  "data": {
    "id": 10,
    "name": "인바운드팀",
    "code": "INBOUND-01",
    "type": "TEAM",
    "status": "ACTIVE",
    "parentId": 5,
    "parentName": "서울센터",
    "path": "넥스프론 > 고객서비스본부 > 서울센터 > 인바운드팀",
    "depth": 4,
    "memberCount": 15,
    "createdAt": "2026-01-14T10:00:00Z",
    "updatedAt": "2026-01-14T12:00:00Z"
  }
}
```

---

### 3.4 하위 부서 조회

```http
GET /api/v1/departments/{deptId}/subtree
```

**Response (200 OK)**
```json
{
  "data": [
    {
      "id": 5,
      "name": "서울센터",
      "depth": 0
    },
    {
      "id": 10,
      "name": "인바운드팀",
      "depth": 1
    },
    {
      "id": 11,
      "name": "아웃바운드팀",
      "depth": 1
    }
  ]
}
```

---

### 3.5 부서 정보 수정

```http
PATCH /api/v1/departments/{deptId}
```

**Request Body**
```json
{
  "name": "인바운드상담팀",
  "type": "TEAM"
}
```

**Response (200 OK)**
```json
{
  "data": {
    "id": 10,
    "name": "인바운드상담팀",
    "type": "TEAM",
    "updatedAt": "2026-01-14T12:00:00Z"
  }
}
```

---

### 3.6 부서 이동

```http
POST /api/v1/departments/{deptId}/move
```

**Request Body**
```json
{
  "newParentId": 6
}
```

**Response (200 OK)**
```json
{
  "data": {
    "id": 10,
    "fromParentId": 5,
    "toParentId": 6,
    "movedAt": "2026-01-14T12:00:00Z"
  }
}
```

**에러**
| Code | HTTP | 설명 |
|------|------|------|
| CANNOT_MOVE_ROOT | 400 | 루트 부서 이동 불가 |
| CIRCULAR_REFERENCE | 400 | 순환 참조 발생 |
| PARENT_DEPT_INACTIVE | 400 | 비활성 부서로 이동 불가 |

---

### 3.7 부서 비활성화

```http
POST /api/v1/departments/{deptId}/deactivate
```

**Response (200 OK)**
```json
{
  "data": {
    "id": 10,
    "status": "INACTIVE",
    "deactivatedAt": "2026-01-14T12:00:00Z"
  }
}
```

**에러**
| Code | HTTP | 설명 |
|------|------|------|
| CHILD_DEPT_ACTIVE | 409 | 활성 하위 부서 존재 |
| ACTIVE_USERS_EXIST | 409 | 활성 상담사 존재 |

---

### 3.8 부서 활성화

```http
POST /api/v1/departments/{deptId}/activate
```

**Response (200 OK)**
```json
{
  "data": {
    "id": 10,
    "status": "ACTIVE",
    "activatedAt": "2026-01-14T12:00:00Z"
  }
}
```

---

### 3.9 부서 삭제

```http
DELETE /api/v1/departments/{deptId}
```

**Response (204 No Content)**

**에러**
| Code | HTTP | 설명 |
|------|------|------|
| CANNOT_DELETE_ROOT | 400 | 루트 부서 삭제 불가 |
| CHILD_DEPT_EXISTS | 409 | 하위 부서 존재 |
| USERS_EXIST | 409 | 소속 상담사 존재 |

---

## 4. Role API (역할 관리)

### 4.1 역할 목록 조회

```http
GET /api/v1/roles
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| type | string | N | POSITION 또는 CHANNEL |

**Response (200 OK)**
```json
{
  "data": [
    {
      "id": 1,
      "name": "ADMIN",
      "type": "POSITION",
      "dataScope": "ADMIN",
      "description": "시스템 관리자",
      "permissionCount": 25
    },
    {
      "id": 4,
      "name": "VOICE_INBOUND",
      "type": "CHANNEL",
      "dataScope": null,
      "description": "인바운드 전화 상담",
      "permissionCount": 4
    }
  ]
}
```

---

### 4.2 역할 상세 조회

```http
GET /api/v1/roles/{roleName}
```

**Response (200 OK)**
```json
{
  "data": {
    "id": 1,
    "name": "ADMIN",
    "type": "POSITION",
    "dataScope": "ADMIN",
    "description": "시스템 관리자",
    "permissions": [
      { "code": "agent:create", "name": "상담사 생성", "category": "AGENT" },
      { "code": "agent:read", "name": "상담사 조회", "category": "AGENT" },
      { "code": "dept:create", "name": "부서 생성", "category": "DEPARTMENT" }
    ],
    "createdAt": "2026-01-14T10:00:00Z"
  }
}
```

---

### 4.3 역할 생성

```http
POST /api/v1/roles
```

**Request Body**
```json
{
  "name": "SUPERVISOR",
  "type": "POSITION",
  "dataScope": "TEAM_LEAD",
  "description": "슈퍼바이저"
}
```

**Response (201 Created)**
```json
{
  "data": {
    "id": 9,
    "name": "SUPERVISOR",
    "type": "POSITION",
    "dataScope": "TEAM_LEAD",
    "description": "슈퍼바이저",
    "createdAt": "2026-01-14T10:00:00Z"
  }
}
```

---

### 4.4 역할 수정

```http
PATCH /api/v1/roles/{roleName}
```

**Request Body**
```json
{
  "description": "수정된 설명"
}
```

**Response (200 OK)**
```json
{
  "data": {
    "name": "SUPERVISOR",
    "description": "수정된 설명",
    "updatedAt": "2026-01-14T12:00:00Z"
  }
}
```

---

### 4.5 역할 삭제

```http
DELETE /api/v1/roles/{roleName}
```

**Response (204 No Content)**

---

### 4.6 역할에 권한 할당

```http
POST /api/v1/roles/{roleName}/permissions
```

**Request Body**
```json
{
  "permissionCode": "report:view"
}
```

**Response (200 OK)**
```json
{
  "data": {
    "roleName": "SUPERVISOR",
    "permissionCode": "report:view",
    "assignedAt": "2026-01-14T12:00:00Z"
  }
}
```

---

### 4.7 역할에서 권한 제거

```http
DELETE /api/v1/roles/{roleName}/permissions/{permissionCode}
```

**Response (204 No Content)**

---

## 5. Permission API (권한 관리)

### 5.1 권한 목록 조회

```http
GET /api/v1/permissions
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| category | string | N | 카테고리 필터 (AGENT, DEPARTMENT, etc.) |

**Response (200 OK)**
```json
{
  "data": [
    {
      "id": 1,
      "code": "agent:create",
      "name": "상담사 생성",
      "description": "새 상담사 등록",
      "category": "AGENT"
    },
    {
      "id": 2,
      "code": "agent:read",
      "name": "상담사 조회",
      "description": "상담사 정보 조회",
      "category": "AGENT"
    }
  ]
}
```

---

### 5.2 권한 상세 조회

```http
GET /api/v1/permissions/{code}
```

**Response (200 OK)**
```json
{
  "data": {
    "id": 1,
    "code": "agent:create",
    "name": "상담사 생성",
    "description": "새 상담사 등록",
    "category": "AGENT",
    "assignedRoles": ["ADMIN"]
  }
}
```

---

## 6. 에러 코드 전체 목록

### 6.1 공통

| Code | HTTP | 설명 |
|------|------|------|
| INVALID_REQUEST | 400 | 잘못된 요청 |
| UNAUTHORIZED | 401 | 인증 필요 |
| INSUFFICIENT_PERMISSION | 403 | 권한 부족 |
| NOT_FOUND | 404 | 리소스를 찾을 수 없음 |
| INTERNAL_ERROR | 500 | 서버 내부 오류 |

### 6.2 Agent

| Code | HTTP | 설명 |
|------|------|------|
| AGENT_NOT_FOUND | 404 | 상담사를 찾을 수 없음 |
| DUPLICATE_LOGIN_ID | 409 | 이미 존재하는 로그인ID |
| AGENT_ALREADY_RETIRED | 400 | 이미 퇴사 처리됨 |
| INVALID_STATUS_TRANSITION | 400 | 잘못된 상태 전이 |
| CANNOT_SUSPEND_SELF | 400 | 본인 정지 불가 |
| CANNOT_RETIRE_SELF | 400 | 본인 퇴사 처리 불가 |
| INVALID_PASSWORD | 400 | 잘못된 비밀번호 |
| SAME_PASSWORD | 400 | 동일한 비밀번호 |

### 6.3 Department

| Code | HTTP | 설명 |
|------|------|------|
| DEPT_NOT_FOUND | 404 | 부서를 찾을 수 없음 |
| DUPLICATE_DEPT_CODE | 409 | 이미 존재하는 부서 코드 |
| ROOT_ALREADY_EXISTS | 409 | 이미 루트 부서 존재 |
| CIRCULAR_REFERENCE | 400 | 순환 참조 발생 |
| CANNOT_MOVE_ROOT | 400 | 루트 부서 이동 불가 |
| CANNOT_DEACTIVATE_ROOT | 400 | 루트 부서 비활성화 불가 |
| CANNOT_DELETE_ROOT | 400 | 루트 부서 삭제 불가 |
| CHILD_DEPT_EXISTS | 409 | 하위 부서 존재 |
| CHILD_DEPT_ACTIVE | 409 | 활성 하위 부서 존재 |
| ACTIVE_USERS_EXIST | 409 | 활성 상담사 존재 |
| PARENT_DEPT_INACTIVE | 400 | 부모 부서가 비활성 |
| DEPT_INACTIVE | 400 | 비활성 부서 |

### 6.4 Role/Permission

| Code | HTTP | 설명 |
|------|------|------|
| ROLE_NOT_FOUND | 404 | 역할을 찾을 수 없음 |
| PERMISSION_NOT_FOUND | 404 | 권한을 찾을 수 없음 |
| DUPLICATE_ROLE | 409 | 이미 존재하는 역할 |
| POSITION_ALREADY_ASSIGNED | 400 | 이미 POSITION 역할 보유 |
| CANNOT_REMOVE_POSITION | 400 | POSITION 역할 제거 불가 |

---

## 7. API 엔드포인트 요약

### 7.1 Agent API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/agents` | 상담사 생성 |
| GET | `/api/v1/agents` | 상담사 목록 조회 |
| GET | `/api/v1/agents/{id}` | 상담사 상세 조회 |
| PATCH | `/api/v1/agents/{id}` | 상담사 정보 수정 |
| POST | `/api/v1/agents/{id}/suspend` | 정지 |
| POST | `/api/v1/agents/{id}/activate` | 활성화 |
| POST | `/api/v1/agents/{id}/retire` | 퇴사 |
| POST | `/api/v1/agents/{id}/transfer` | 부서 이동 |
| POST | `/api/v1/agents/{id}/reset-password` | 비밀번호 초기화 |
| POST | `/api/v1/agents/me/change-password` | 비밀번호 변경 (본인) |
| POST | `/api/v1/agents/{id}/roles` | 역할 할당 |
| DELETE | `/api/v1/agents/{id}/roles/{name}` | 역할 제거 |
| PUT | `/api/v1/agents/{id}/roles` | 역할 전체 교체 |

### 7.2 Department API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/departments` | 부서 생성 |
| GET | `/api/v1/departments/tree` | 조직도 트리 조회 |
| GET | `/api/v1/departments/{id}` | 부서 상세 조회 |
| GET | `/api/v1/departments/{id}/subtree` | 하위 부서 조회 |
| PATCH | `/api/v1/departments/{id}` | 부서 정보 수정 |
| POST | `/api/v1/departments/{id}/move` | 부서 이동 |
| POST | `/api/v1/departments/{id}/deactivate` | 부서 비활성화 |
| POST | `/api/v1/departments/{id}/activate` | 부서 활성화 |
| DELETE | `/api/v1/departments/{id}` | 부서 삭제 |

### 7.3 Role API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/rbac/roles` | 역할 목록 조회 |
| GET | `/api/rbac/roles/{name}` | 역할 상세 조회 |
| POST | `/api/rbac/roles` | 역할 생성 |
| PATCH | `/api/rbac/roles/{name}` | 역할 수정 |
| DELETE | `/api/rbac/roles/{name}` | 역할 삭제 |
| POST | `/api/rbac/roles/{name}/permissions/{code}` | 역할에 권한 할당 |
| DELETE | `/api/rbac/roles/{name}/permissions/{code}` | 역할에서 권한 제거 |

### 7.4 Permission API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/rbac/permissions` | 권한 목록 조회 |
| POST | `/api/rbac/permissions` | 권한 생성 |

### 7.5 Agent-Role API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/rbac/agents/{id}/roles` | 사용자 역할 목록 조회 |
| GET | `/api/rbac/agents/{id}/permissions` | 사용자 실제 권한 조회 |
| POST | `/api/rbac/agents/{id}/roles/{name}` | 사용자에게 역할 할당 |
| DELETE | `/api/rbac/agents/{id}/roles/{name}` | 사용자에게서 역할 회수 |

### 7.6 Organization API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/org/departments` | 전체 조직도 조회 |
| GET | `/api/org/departments/{id}/statistics` | 부서 통계 조회 |
| POST | `/api/org/departments` | 부서 생성 |
| PATCH | `/api/org/departments/{id}` | 부서 수정 |
| DELETE | `/api/org/departments/{id}` | 부서 삭제 |
| PUT | `/api/org/departments/{id}/move` | 부서 이동 |

---

*문서 버전: 2.0.1*  
*최종 수정: 2026-01-22*  
*실제 구현된 API 기준으로 업데이트*

