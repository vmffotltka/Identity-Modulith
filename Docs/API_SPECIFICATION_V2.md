# API Specification v2.0 - REST API 명세

Identity Modulith REST API 명세서 (실제 구현 기반)

**버전**: 2.0.2  
**최종 업데이트**: 2026-02-04  
**상태**: 완전 구현 반영 + 아키텍처 개선

---

## 📋 변경 이력

### v2.0.2 (2026-02-04) - 아키텍처 개선
- ✅ **Port/Adapter 패턴 적용** (모듈러 모놀리스 + DDD 원칙)
  - `RbacPort` 인터페이스 추가 (User 모듈)
  - `RbacAdapter` 구현체 추가 (Infrastructure)
  - User 모듈이 RBAC 모듈을 직접 의존하지 않음
  - Presentation/Application layer는 Port만 의존
  - Infrastructure layer에서만 외부 모듈 의존
- ✅ 모듈 간 결합도 감소 및 테스트 용이성 향상

### v2.0.1 (2026-02-04) - 추가 구현 완료
- ✅ Agent API 개별 역할 추가/제거 구현 완료
  - `POST /api/v1/agents/{id}/roles/{name}`
  - `DELETE /api/v1/agents/{id}/roles/{name}`
- ✅ RBAC 서비스와 통합하여 실제 동작

### v2.0.0 (2026-02-04)
- ✅ 실제 구현된 모든 API 반영
- ✅ Base URL 수정: `/api/v1/departmentEntities` → `/api/org/departments`
- ✅ 추가 구현된 API 20개 문서화
- ✅ HTTP Method 일치 확인 (PUT, DELETE 등)
- ✅ RBAC 모듈 전체 API 문서화
- ⚠️ 부서 비활성화/활성화 API 제거 (미구현)

---

## 1. 개요

### 1.1 아키텍처 원칙

본 API는 **모듈러 모놀리스(Modular Monolith)** 아키텍처와 **DDD(Domain-Driven Design)** 원칙을 따릅니다.

#### 모듈 구조
```
┌─────────────────────────────────────────────┐
│           Presentation Layer                │
│  (Controllers - REST API Endpoints)         │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│          Application Layer                  │
│  (Use Cases, Services, Ports)               │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         Infrastructure Layer                │
│  (Adapters, Repositories, External APIs)    │
└─────────────────────────────────────────────┘
```

#### Port/Adapter 패턴 적용

모듈 간 직접 의존을 피하기 위해 **Port/Adapter 패턴**을 사용합니다:

- **Port**: Application layer에 정의된 인터페이스
  - 예: `RbacPort` (User 모듈)
- **Adapter**: Infrastructure layer에 구현된 실제 구현체
  - 예: `RbacAdapter` (User → RBAC 연동)

**효과**:
- ✅ 모듈 간 결합도 감소
- ✅ 테스트 용이성 향상 (Mock 주입 가능)
- ✅ 향후 모듈 교체 가능성 확보

### 1.2 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL (User/Agent) | `/api/v1` |
| Base URL (Organization) | `/api/org` |
| Base URL (RBAC) | `/api/rbac` |
| Content-Type | `application/json` |
| 인증 | Bearer Token (JWT) |

### 1.2 공통 헤더

```http
Authorization: Bearer {jwt_token}
X-Tenant-Id: {tenant_id}
X-User-Id: {user_id}
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

---

## 2. Agent API (상담사 관리)

Base URL: `/api/v1/agents`

### 2.1 상담사 생성

```http
POST /api/v1/agents
```

**Request Body**
```json
{
  "tenantId": "tenant-001",
  "loginId": "john.doe",
  "name": "홍길동",
  "organizationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (201 Created)**
```json
{
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "loginId": "john.doe",
  "tempPassword": "Abc12345"
}
```

---

### 2.2 아이디 중복 체크

```http
GET /api/v1/agents/check-login-id?loginId={loginId}
```

**Response (200 OK)**
```json
{
  "isUnique": true
}
```

---

### 2.3 상담사 목록 조회

```http
GET /api/v1/agents
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| tenantId | string | Y | 테넌트 ID |
| organizationId | string | N | 조직 ID 필터 |
| status | enum | N | ACTIVE, SUSPENDED, RETIRED |
| nameKeyword | string | N | 이름 검색 키워드 |
| loginIdKeyword | string | N | 로그인 ID 검색 키워드 |
| includeRetired | boolean | N | 퇴사자 포함 여부 (기본: false) |

**Response (200 OK)**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "loginId": "john.doe",
    "name": "홍길동",
    "organizationId": "550e8400-e29b-41d4-a716-446655440001",
    "status": "ACTIVE",
    "passwordMustChange": false,
    "createdAt": "2026-01-14T10:00:00Z",
    "retiredAt": null,
    "roles": [
      { "name": "AGENT", "type": "POSITION" }
    ]
  }
]
```

---

### 2.4 상담사 단건 조회

```http
GET /api/v1/agents/{agentId}
```

**Response (200 OK)**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "loginId": "john.doe",
  "name": "홍길동",
  "organizationId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "ACTIVE",
  "passwordMustChange": false,
  "createdAt": "2026-01-14T10:00:00Z",
  "retiredAt": null,
  "roles": [
    { "name": "AGENT", "type": "POSITION" },
    { "name": "VOICE_INBOUND", "type": "CHANNEL" }
  ]
}
```

---

### 2.5 상담사 정보 수정

```http
PATCH /api/v1/agents/{agentId}
```

**Request Body**
```json
{
  "name": "홍길동"
}
```

**Response (204 No Content)**

---

### 2.6 상담사 조직 이동

```http
PATCH /api/v1/agents/{agentId}/organization
```

**Request Body**
```json
{
  "organizationId": "550e8400-e29b-41d4-a716-446655440002"
}
```

**Response (204 No Content)**

---

### 2.7 비밀번호 초기화 (관리자용)

```http
POST /api/v1/agents/{agentId}/reset-password
```

**Response (200 OK)**
```json
{
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "tempPassword": "NewPass123"
}
```

---

### 2.8 비밀번호 변경 (본인용 - ID 방식)

```http
POST /api/v1/agents/{agentId}/change-password
```

**Request Body**
```json
{
  "currentPassword": "OldPass123",
  "newPassword": "NewPass456"
}
```

**Response (204 No Content)**

---

### 2.9 비밀번호 변경 (본인용 - /me 방식) ✨ 추가됨

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

**Response (204 No Content)**

**설명**: 현재 로그인한 사용자의 비밀번호를 변경합니다.

---

### 2.10 상담사 정지

```http
POST /api/v1/agents/{agentId}/suspend
```

**Response (204 No Content)**

**설명**: ACTIVE → SUSPENDED 상태 전환

---

### 2.11 상담사 활성화

```http
POST /api/v1/agents/{agentId}/activate
```

**Response (204 No Content)**

**설명**: SUSPENDED → ACTIVE 상태 전환

---

### 2.12 상담사 퇴사 처리 (Soft Delete)

```http
DELETE /api/v1/agents/{agentId}
```

**Response (204 No Content)**

**설명**: 
- 실제 삭제가 아닌 status를 RETIRED로 변경
- retiredAt에 퇴사 일시 기록
- 즉시 로그인 차단 및 상담 배정 제외

**변경사항**: API 명세서의 `POST /retire`에서 `DELETE`로 변경 (RESTful 원칙)

---

### 2.13 상담사 부서 이동

```http
POST /api/v1/agents/{agentId}/transfer
```

**Request Body**
```json
{
  "newOrganizationId": "550e8400-e29b-41d4-a716-446655440002"
}
```

**Response (200 OK)**
```json
{
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "fromOrganizationId": "550e8400-e29b-41d4-a716-446655440001",
  "toOrganizationId": "550e8400-e29b-41d4-a716-446655440002",
  "transferredAt": "2026-01-14T12:00:00Z"
}
```

---

### 2.14 역할 전체 교체

```http
PUT /api/v1/agents/{agentId}/roles
```

**Request Body**
```json
{
  "roles": [
    { "name": "AGENT", "type": "POSITION" },
    { "name": "CHAT", "type": "CHANNEL" }
  ]
}
```

**Response (204 No Content)**

**설명**: 기존 역할을 모두 대체하고 새로운 역할들로 교체합니다.

---

### 2.15 개별 역할 추가 ✅ 구현됨

```http
POST /api/v1/agents/{agentId}/roles/{roleName}
```

**Response (201 Created)**

**설명**: 
- 기존 역할은 유지되며 새 역할만 추가됨
- 이미 할당된 역할인 경우 무시됨 (멱등성)
- 내부적으로 RBAC 서비스 호출
- RBAC API(`POST /api/rbac/agents/{agentId}/roles/{roleName}`)와 동일한 기능

---

### 2.16 개별 역할 제거 ✅ 구현됨

```http
DELETE /api/v1/agents/{agentId}/roles/{roleName}
```

**Response (204 No Content)**

**설명**: 
- 다른 역할은 유지되며 지정된 역할만 제거됨
- 할당되지 않은 역할인 경우 404 에러 반환
- 내부적으로 RBAC 서비스 호출
- RBAC API(`DELETE /api/rbac/agents/{agentId}/roles/{roleName}`)와 동일한 기능

---

### 2.17 테넌트별 상담사 통계 조회 ✨ 추가됨

```http
GET /api/v1/agents/statistics?tenantId={tenantId}
```

**Response (200 OK)**
```json
{
  "totalCount": 100,
  "activeCount": 80,
  "suspendedCount": 15,
  "retiredCount": 5,
  "passwordChangeRequired": 10,
  "byOrganization": {
    "550e8400-...": 50,
    "660e8400-...": 30
  },
  "byStatus": {
    "ACTIVE": 80,
    "SUSPENDED": 15,
    "RETIRED": 5
  }
}
```

---

### 2.18 조직별 상담사 통계 조회 ✨ 추가됨

```http
GET /api/v1/agents/statistics/organization/{organizationId}?tenantId={tenantId}
```

**Response (200 OK)**
```json
{
  "totalCount": 50,
  "activeCount": 45,
  "suspendedCount": 5,
  "retiredCount": 0,
  "passwordChangeRequired": 3,
  "byOrganization": {
    "550e8400-...": 50
  },
  "byStatus": {
    "ACTIVE": 45,
    "SUSPENDED": 5
  }
}
```

---

## 3. Department API (부서 관리)

Base URL: `/api/org/departments`

### 3.1 부서 생성

```http
POST /api/org/departments
```

**Request Body**
```json
{
  "name": "개발본부",
  "type": "본부",
  "parentId": null
}
```

**Response (201 Created)**
```json
{
  "deptId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "개발본부",
  "type": "본부",
  "orgPath": "/550e8400-e29b-41d4-a716-446655440000",
  "depth": 0,
  "parent": null,
  "children": [],
  "createdAt": "2026-01-14T10:00:00Z"
}
```

---

### 3.2 부서 정보 수정

```http
PATCH /api/org/departments/{deptId}
```

**Request Body**
```json
{
  "name": "개발본부",
  "type": "본부"
}
```

**Response (200 OK)**
```json
{
  "deptId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "개발본부",
  "type": "본부",
  "orgPath": "/550e8400-e29b-41d4-a716-446655440000",
  "depth": 0
}
```

---

### 3.3 전체 조직도 조회

```http
GET /api/org/departments
```

**Response (200 OK)**
```json
[
  {
    "deptId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "총무부",
    "type": "본부",
    "depth": 0,
    "orgPath": "/550e8400-e29b-41d4-a716-446655440000",
    "children": [
      {
        "deptId": "660e8400-e29b-41d4-a716-446655440000",
        "name": "HR팀",
        "type": "팀",
        "depth": 1,
        "orgPath": "/550e8400-.../660e8400-...",
        "children": []
      }
    ]
  }
]
```

---

### 3.4 스코프 기반 조직도 조회 ✨ 추가됨

```http
GET /api/org/departments/scoped
Headers: X-User-Id: {userId}
```

**Response (200 OK)**
```json
[
  {
    "deptId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "개발본부",
    "type": "본부",
    "depth": 0,
    "children": [ ... ]
  }
]
```

**설명**: 현재 사용자의 접근 권한에 따라 조회 가능한 부서만 반환
- ADMIN: 전체 부서
- TEAM_LEAD: 자신 부서 + 하위 부서
- MEMBER: 자신 부서만

---

### 3.5 부서 검색 ✨ 추가됨

```http
GET /api/org/departments/search?keyword={keyword}
```

**Response (200 OK)**
```json
[
  {
    "deptId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "개발팀",
    "type": "팀",
    "depth": 1
  }
]
```

---

### 3.6 깊이별 부서 조회 ✨ 추가됨

```http
GET /api/org/departments/by-depth?depth={depth}
```

**Response (200 OK)**
```json
[
  {
    "deptId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "총무부",
    "type": "본부",
    "depth": 0
  }
]
```

---

### 3.7 타입별 부서 조회 ✨ 추가됨

```http
GET /api/org/departments/by-type?type={type}
```

**Response (200 OK)**
```json
[
  {
    "deptId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "개발팀",
    "type": "팀",
    "depth": 1
  }
]
```

---

### 3.8 부서 통계 조회 ✨ 추가됨

```http
GET /api/org/departments/{deptId}/statistics
```

**Response (200 OK)**
```json
{
  "deptId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "개발본부",
  "type": "본부",
  "depth": 0,
  "totalEmployees": 50,
  "activeEmployees": 45,
  "childDeptCount": 3,
  "descendantDeptCount": 10
}
```

---

### 3.9 부서별 사용자 목록 조회 ✨ 추가됨

```http
GET /api/org/departments/{deptId}/members?includeSubDepartments={boolean}
```

**Response (200 OK)**
```json
{
  "deptId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "개발본부",
  "includeSubDepartments": true,
  "totalMembers": 50,
  "activeCount": 45,
  "retiredCount": 5,
  "members": [
    {
      "agentId": "a0000000-0000-0000-0000-000000000001",
      "name": "홍길동",
      "loginId": "hong.gd",
      "status": "ACTIVE",
      "departmentId": "550e8400-...",
      "departmentName": "개발팀"
    }
  ]
}
```

---

### 3.10 부서 이동

```http
PUT /api/org/departments/{deptId}/move
Headers: X-User-Id: {userId}
```

**Request Body**
```json
{
  "newParentId": "660e8400-e29b-41d4-a716-446655440000"
}
```

**Response (204 No Content)**

**변경사항**: API 명세서의 `POST`에서 `PUT`으로 변경 (더 적절한 HTTP Method)

---

### 3.11 부서 삭제

```http
DELETE /api/org/departments/{deptId}
Headers: X-User-Id: {userId}
```

**Response (204 No Content)**

**제약 조건**:
- 하위 부서가 없어야 함
- 소속 활성 사용자가 없어야 함

---

### ❌ 3.12 부서 비활성화 (미구현)

```http
POST /api/org/departments/{deptId}/deactivate
```

**상태**: 명세서에만 존재, 실제 구현되지 않음

**이유**: Department 엔티티에 status 필드가 없으며, DB 스키마 변경 필요

---

### ❌ 3.13 부서 활성화 (미구현)

```http
POST /api/org/departments/{deptId}/activate
```

**상태**: 명세서에만 존재, 실제 구현되지 않음

**이유**: Department 엔티티에 status 필드가 없으며, DB 스키마 변경 필요

---

## 4. RBAC API (역할 및 권한 관리)

Base URL: `/api/rbac`

### 4.1 역할 관리

#### 4.1.1 모든 역할 조회

```http
GET /api/rbac/roles
```

**Response (200 OK)**
```json
[
  {
    "name": "ADMIN",
    "type": "POSITION",
    "description": "시스템 관리자",
    "isActive": true,
    "createdAt": "2026-01-14T10:00:00Z"
  }
]
```

---

#### 4.1.2 특정 역할 조회

```http
GET /api/rbac/roles/{roleName}
```

**Response (200 OK)**
```json
{
  "name": "ADMIN",
  "type": "POSITION",
  "description": "시스템 관리자",
  "isActive": true,
  "createdAt": "2026-01-14T10:00:00Z"
}
```

---

#### 4.1.3 역할 생성

```http
POST /api/rbac/roles
```

**Request Body**
```json
{
  "name": "TEAM_LEAD",
  "type": "POSITION",
  "description": "팀장"
}
```

**Response (201 Created)**
```json
{
  "name": "TEAM_LEAD",
  "type": "POSITION",
  "description": "팀장",
  "isActive": true,
  "createdAt": "2026-01-14T10:00:00Z"
}
```

---

#### 4.1.4 역할 수정

```http
PATCH /api/rbac/roles/{roleName}
```

**Request Body**
```json
{
  "type": "POSITION",
  "description": "팀장 (수정됨)",
  "isActive": true
}
```

**Response (200 OK)**
```json
{
  "name": "TEAM_LEAD",
  "type": "POSITION",
  "description": "팀장 (수정됨)",
  "isActive": true
}
```

---

#### 4.1.5 역할 삭제 ✅ 구현됨

```http
DELETE /api/rbac/roles/{roleName}?forceDelete={boolean}
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| forceDelete | boolean | N | true: 사용자가 있어도 강제 삭제 (기본: false) |

**Response (200 OK)**
```json
{
  "roleName": "TEAM_LEAD",
  "deleted": true,
  "affectedAgents": 5,
  "removedPermissions": 10
}
```

---

#### 4.1.6 역할 삭제 영향도 조회 ✨ 추가됨

```http
GET /api/rbac/roles/{roleName}/deletion-impact
```

**Response (200 OK)**
```json
{
  "roleName": "TEAM_LEAD",
  "agentCount": 5,
  "permissionCount": 10,
  "canDelete": false
}
```

---

#### 4.1.7 역할 비활성화 ✨ 추가됨

```http
POST /api/rbac/roles/{roleName}/deactivate
```

**Response (200 OK)**

---

#### 4.1.8 역할 활성화 ✨ 추가됨

```http
POST /api/rbac/roles/{roleName}/activate
```

**Response (200 OK)**

---

#### 4.1.9 역할 사용자 수 조회 ✨ 추가됨

```http
GET /api/rbac/roles/{roleName}/agent-count
```

**Response (200 OK)**
```json
{
  "count": 15
}
```

---

### 4.2 권한 관리

#### 4.2.1 모든 권한 조회

```http
GET /api/rbac/permissions
```

**Response (200 OK)**
```json
[
  {
    "code": "user:create",
    "description": "사용자 생성 권한",
    "createdAt": "2026-01-14T10:00:00Z"
  }
]
```

---

#### 4.2.2 특정 권한 조회 ✨ 추가됨

```http
GET /api/rbac/permissions/{code}
```

**Response (200 OK)**
```json
{
  "code": "user:create",
  "description": "사용자 생성 권한",
  "createdAt": "2026-01-14T10:00:00Z"
}
```

---

#### 4.2.3 권한 생성

```http
POST /api/rbac/permissions
```

**Request Body**
```json
{
  "code": "user:delete",
  "description": "사용자 삭제 권한"
}
```

**Response (201 Created)**
```json
{
  "code": "user:delete",
  "description": "사용자 삭제 권한",
  "createdAt": "2026-01-14T10:00:00Z"
}
```

---

#### 4.2.4 권한 수정 ✨ 추가됨

```http
PATCH /api/rbac/permissions/{code}
```

**Request Body**
```json
{
  "newCode": "user:remove",
  "description": "사용자 제거 권한"
}
```

**Response (200 OK)**

---

#### 4.2.5 권한 삭제 ✨ 추가됨

```http
DELETE /api/rbac/permissions/{code}
```

**Response (204 No Content)**

---

#### 4.2.6 권한을 가진 역할 조회 ✨ 추가됨

```http
GET /api/rbac/permissions/{code}/roles
```

**Response (200 OK)**
```json
[
  "ADMIN",
  "TEAM_LEAD"
]
```

---

### 4.3 역할-권한 할당

#### 4.3.1 역할의 권한 조회 ✨ 추가됨

```http
GET /api/rbac/roles/{roleName}/permissions
```

**Response (200 OK)**
```json
[
  {
    "code": "user:create",
    "description": "사용자 생성 권한"
  },
  {
    "code": "user:read",
    "description": "사용자 조회 권한"
  }
]
```

---

#### 4.3.2 역할에 권한 할당

```http
POST /api/rbac/roles/{roleName}/permissions/{permissionCode}
```

**Response (201 Created)**

---

#### 4.3.3 역할에서 권한 제거

```http
DELETE /api/rbac/roles/{roleName}/permissions/{permissionCode}
```

**Response (204 No Content)**

---

#### 4.3.4 역할에 여러 권한 일괄 할당 ✨ 추가됨

```http
POST /api/rbac/roles/{roleName}/permissions/batch
```

**Request Body**
```json
[
  "user:create",
  "user:read",
  "user:update"
]
```

**Response (200 OK)**
```json
{
  "roleName": "TEAM_LEAD",
  "successCount": 3,
  "failedCount": 0,
  "alreadyAssigned": []
}
```

---

#### 4.3.5 역할에서 여러 권한 일괄 제거 ✨ 추가됨

```http
DELETE /api/rbac/roles/{roleName}/permissions/batch
```

**Request Body**
```json
[
  "user:delete",
  "user:admin"
]
```

**Response (200 OK)**
```json
{
  "roleName": "TEAM_LEAD",
  "successCount": 2,
  "failedCount": 0,
  "notAssigned": []
}
```

---

### 4.4 사용자-역할 관리

#### 4.4.1 사용자에게 역할 할당

```http
POST /api/rbac/agents/{agentId}/roles/{roleName}
```

**Response (201 Created)**

---

#### 4.4.2 사용자에게서 역할 회수

```http
DELETE /api/rbac/agents/{agentId}/roles/{roleName}
```

**Response (204 No Content)**

---

#### 4.4.3 사용자의 역할 목록 조회

```http
GET /api/rbac/agents/{agentId}/roles
```

**Response (200 OK)**
```json
[
  "AGENT",
  "TEAM_LEAD"
]
```

---

#### 4.4.4 사용자의 실제 권한 조회

```http
GET /api/rbac/agents/{agentId}/effective-permissions
```

**Response (200 OK)**
```json
[
  "user:create",
  "user:read",
  "user:update",
  "department:read"
]
```

**설명**: 사용자가 가진 모든 역할의 권한을 합산하여 반환

**변경사항**: API 명세서의 `/permissions`에서 `/effective-permissions`로 변경 (더 명확한 이름)

---

## 5. 에러 코드

### 5.1 공통 에러

| Code | HTTP | 설명 |
|------|------|------|
| INVALID_REQUEST | 400 | 잘못된 요청 |
| UNAUTHORIZED | 401 | 인증 필요 |
| FORBIDDEN | 403 | 권한 없음 |
| NOT_FOUND | 404 | 리소스를 찾을 수 없음 |
| CONFLICT | 409 | 리소스 충돌 |
| INTERNAL_SERVER_ERROR | 500 | 서버 내부 오류 |

### 5.2 Agent 모듈

| Code | HTTP | 설명 |
|------|------|------|
| AGENT_NOT_FOUND | 404 | 상담사를 찾을 수 없음 |
| DUPLICATE_LOGIN_ID | 409 | 이미 존재하는 로그인ID |
| INVALID_STATUS_TRANSITION | 400 | 잘못된 상태 전환 |
| CANNOT_SUSPEND_SELF | 400 | 본인 정지 불가 |
| AGENT_ALREADY_RETIRED | 400 | 이미 퇴사한 상담사 |
| INVALID_PASSWORD | 400 | 잘못된 비밀번호 |

### 5.3 Organization 모듈

| Code | HTTP | 설명 |
|------|------|------|
| DEPARTMENT_NOT_FOUND | 404 | 부서를 찾을 수 없음 |
| INVALID_PARENT | 400 | 잘못된 상위 부서 |
| CHILD_DEPARTMENT_EXISTS | 400 | 하위 부서 존재 |
| ACTIVE_USERS_EXIST | 400 | 소속 활성 사용자 존재 |
| INSUFFICIENT_PERMISSION | 403 | 권한 부족 |

### 5.4 RBAC 모듈

| Code | HTTP | 설명 |
|------|------|------|
| ROLE_NOT_FOUND | 404 | 역할을 찾을 수 없음 |
| ROLE_ALREADY_EXISTS | 409 | 이미 존재하는 역할 |
| PERMISSION_NOT_FOUND | 404 | 권한을 찾을 수 없음 |
| PERMISSION_ALREADY_EXISTS | 409 | 이미 존재하는 권한 |
| ROLE_ALREADY_ASSIGNED | 409 | 이미 할당된 역할 |
| PERMISSION_ALREADY_ASSIGNED | 409 | 이미 할당된 권한 |
| CANNOT_DELETE_ROLE_WITH_AGENTS | 400 | 사용자가 있는 역할은 삭제 불가 |

---

## 6. 변경 사항 요약

### ✅ 경로 수정
- ~~`/api/v1/departmentEntities`~~ → `/api/org/departments`

### ✅ HTTP Method 수정
- 부서 이동: ~~`POST /move`~~ → `PUT /move`
- 상담사 퇴사: ~~`POST /retire`~~ → `DELETE /{agentId}`
- 역할 삭제: 이미 `DELETE /roles/{name}` 구현됨

### ✅ 추가된 엔드포인트 (20개)

**Agent API (4개)**
- `GET /api/v1/agents/check-login-id`
- `PATCH /api/v1/agents/{id}/organization`
- `GET /api/v1/agents/statistics`
- `GET /api/v1/agents/statistics/organization/{id}`

**Department API (5개)**
- `GET /api/org/departments/scoped`
- `GET /api/org/departments/search`
- `GET /api/org/departments/by-type`
- `GET /api/org/departments/by-depth`
- `GET /api/org/departments/{id}/statistics`
- `GET /api/org/departments/{id}/members`

**RBAC API (11개)**
- `GET /api/rbac/roles/{name}/permissions`
- `GET /api/rbac/roles/{name}/deletion-impact`
- `POST /api/rbac/roles/{name}/deactivate`
- `POST /api/rbac/roles/{name}/activate`
- `POST /api/rbac/roles/{name}/permissions/batch`
- `DELETE /api/rbac/roles/{name}/permissions/batch`
- `GET /api/rbac/roles/{name}/agent-count`
- `GET /api/rbac/permissions/{code}`
- `PATCH /api/rbac/permissions/{code}`
- `DELETE /api/rbac/permissions/{code}`
- `GET /api/rbac/permissions/{code}/roles`

### ❌ 제거된 기능 (2개)
- ~~`POST /api/org/departments/{id}/deactivate`~~ (미구현)
- ~~`POST /api/org/departments/{id}/activate`~~ (미구현)

---

## 7. 권장 사항

### 7.1 역할 관리
- Agent API에서 개별 역할 추가/제거는 **RBAC API 사용 권장**
- `PUT /api/v1/agents/{id}/roles`: 전체 교체
- `POST /api/rbac/agents/{id}/roles/{name}`: 개별 추가
- `DELETE /api/rbac/agents/{id}/roles/{name}`: 개별 제거

### 7.2 비밀번호 변경
- 본인 비밀번호 변경: `POST /api/v1/agents/me/change-password` (권장)
- 특정 사용자 비밀번호 변경: `POST /api/v1/agents/{id}/change-password`

### 7.3 부서 조회
- 전체 조회 (관리자): `GET /api/org/departments`
- 스코프 조회 (일반 사용자): `GET /api/org/departments/scoped`

---

**문서 버전**: 2.0.2  
**마지막 업데이트**: 2026-02-04  
**작성자**: Identity System Team  
**아키텍처**: Modular Monolith + DDD + Port/Adapter Pattern
