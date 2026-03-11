# API 테스트 시나리오 - RBAC Management

## 🎯 테스트 환경
- **Base URL**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **기본 인증**: Mock 인증 (자동)
- **테넌트**: `default-tenant`

---

## 📋 초기 데이터 (SQL 자동 삽입)

### 역할 (3개)
| role_id | name | type | data_scope_level | is_active |
|---------|------|------|------------------|-----------|
| 20000000-0000-0000-0000-000000000001 | ADMIN | POSITION | ADMIN | true |
| 20000000-0000-0000-0000-000000000002 | TEAM_LEAD | POSITION | TEAM_LEAD | true |
| 20000000-0000-0000-0000-000000000003 | MEMBER | POSITION | MEMBER | true |

### 권한 (10개)
| code | category | description |
|------|----------|-------------|
| user:create | USER_MANAGEMENT | 사용자 생성 |
| user:read | USER_MANAGEMENT | 사용자 조회 |
| user:update | USER_MANAGEMENT | 사용자 수정 |
| user:delete | USER_MANAGEMENT | 사용자 삭제 |
| org:create | ORG_MANAGEMENT | 조직 생성 |
| org:read | ORG_MANAGEMENT | 조직 조회 |
| org:update | ORG_MANAGEMENT | 조직 수정 |
| org:delete | ORG_MANAGEMENT | 조직 삭제 |
| report:view | REPORT | 보고서 조회 |
| report:export | REPORT | 보고서 내보내기 |

### 역할-권한 매핑
**ADMIN**: 모든 권한 (10개)
**TEAM_LEAD**: user:read, org:read, report:view, report:export (4개)
**MEMBER**: user:read, org:read, report:view (3개)

### 사용자-역할 매핑
| agent_id | roles |
|----------|-------|
| 10000000-0000-0000-0000-000000000001 | ADMIN |
| 10000000-0000-0000-0000-000000000002 | TEAM_LEAD |
| 10000000-0000-0000-0000-000000000003 | MEMBER |

---

## 🧪 테스트 시나리오

### Scenario 1: 모든 역할 조회 ✅

**권한**: 모든 사용자 (인증 불필요)

**GET** `/api/rbac/roles`

**Headers**: (불필요)

**Query Parameters** (선택):
```
isActive=true
type=POSITION
```

**예상 응답 (200 OK)**:
```json
[
  {
    "roleId": "20000000-0000-0000-0000-000000000001",
    "name": "ADMIN",
    "type": "POSITION",
    "dataScopeLevel": "ADMIN",
    "description": "시스템 관리자",
    "isActive": true,
    "permissionCount": 10,
    "userCount": 1,
    "createdAt": "2026-02-08T00:00:00",
    "updatedAt": "2026-02-08T00:00:00"
  },
  {
    "roleId": "20000000-0000-0000-0000-000000000002",
    "name": "TEAM_LEAD",
    "type": "POSITION",
    "dataScopeLevel": "TEAM_LEAD",
    "description": "팀장",
    "isActive": true,
    "permissionCount": 4,
    "userCount": 1
  },
  {
    "roleId": "20000000-0000-0000-0000-000000000003",
    "name": "MEMBER",
    "type": "POSITION",
    "dataScopeLevel": "MEMBER",
    "description": "일반 멤버",
    "isActive": true,
    "permissionCount": 3,
    "userCount": 1
  }
]
```

**검증 항목**:
- ✅ 3개 역할 모두 조회됨
- ✅ 권한 개수 정확
- ✅ 사용자 수 정확
- ✅ dataScopeLevel 표시

---

### Scenario 2: 특정 역할 조회 ✅

**권한**: 모든 사용자 (인증 불필요)

**GET** `/api/rbac/roles/{roleName}`

**Headers**: (불필요)

**사용할 roleName**: `TEAM_LEAD`

**예상 응답 (200 OK)**:
```json
{
  "roleId": "20000000-0000-0000-0000-000000000002",
  "name": "TEAM_LEAD",
  "type": "POSITION",
  "dataScopeLevel": "TEAM_LEAD",
  "description": "팀장",
  "isActive": true,
  "permissions": [
    {
      "permissionId": "30000000-0000-0000-0000-000000000002",
      "code": "user:read",
      "category": "USER_MANAGEMENT",
      "description": "사용자 조회"
    },
    {
      "permissionId": "30000000-0000-0000-0000-000000000006",
      "code": "org:read",
      "category": "ORG_MANAGEMENT",
      "description": "조직 조회"
    },
    {
      "permissionId": "30000000-0000-0000-0000-000000000009",
      "code": "report:view",
      "category": "REPORT",
      "description": "보고서 조회"
    },
    {
      "permissionId": "30000000-0000-0000-0000-000000000010",
      "code": "report:export",
      "category": "REPORT",
      "description": "보고서 내보내기"
    }
  ],
  "userCount": 1,
  "createdAt": "2026-02-08T00:00:00"
}
```

**검증 항목**:
- ✅ 역할 상세 정보 조회
- ✅ 권한 목록 포함
- ✅ 사용자 수 표시

---

### Scenario 3: 역할 생성 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**POST** `/api/rbac/roles`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

#### 3-1. 프로젝트 매니저 역할 생성

**Request Body**:
```json
{
  "name": "PROJECT_MANAGER",
  "type": "POSITION",
  "dataScopeLevel": "TEAM_LEAD",
  "description": "프로젝트 매니저",
  "permissionCodes": [
    "user:read",
    "user:update",
    "org:read",
    "org:update",
    "report:view",
    "report:export"
  ]
}
```

**예상 응답 (201 Created)**:
```json
{
  "roleId": "uuid-generated",
  "name": "PROJECT_MANAGER",
  "type": "POSITION",
  "dataScopeLevel": "TEAM_LEAD",
  "description": "프로젝트 매니저",
  "isActive": true,
  "permissions": [
    {"code": "user:read"},
    {"code": "user:update"},
    {"code": "org:read"},
    {"code": "org:update"},
    {"code": "report:view"},
    {"code": "report:export"}
  ],
  "createdAt": "2026-02-08T10:00:00"
}
```

**검증 항목**:
- ✅ roleId 자동 생성
- ✅ isActive = true (기본값)
- ✅ 권한 자동 할당
- ✅ dataScopeLevel 설정

#### 3-2. 인사팀 역할 생성 (HR)

**Request Body**:
```json
{
  "name": "HR",
  "type": "POSITION",
  "dataScopeLevel": "ADMIN",
  "description": "인사팀",
  "permissionCodes": [
    "user:create",
    "user:read",
    "user:update",
    "user:delete",
    "org:read"
  ]
}
```

#### 3-3. 중복 역할명으로 생성 시도 ❌

**Request Body**:
```json
{
  "name": "ADMIN",
  "type": "POSITION",
  "description": "테스트"
}
```

**예상 응답 (400 Bad Request)**:
```json
{
  "code": "DUPLICATE_ROLE_NAME",
  "message": "이미 존재하는 역할명입니다"
}
```

#### 3-4. 권한 없는 사용자로 생성 시도 ❌

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000002
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "NEW_ROLE",
  "type": "POSITION"
}
```

**예상 응답 (403 Forbidden)**:
```json
{
  "code": "INSUFFICIENT_PERMISSION",
  "message": "역할 생성은(는) ADMIN 역할이 필요합니다."
}
```

---

### Scenario 4: 역할 수정 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**PATCH** `/api/rbac/roles/{roleName}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 roleName**: `PROJECT_MANAGER`

#### 4-1. 설명 변경

**Request Body**:
```json
{
  "description": "프로젝트 관리자 (수정됨)"
}
```

**예상 응답 (200 OK)**:
```json
{
  "roleId": "uuid",
  "name": "PROJECT_MANAGER",
  "description": "프로젝트 관리자 (수정됨)",
  "updatedAt": "2026-02-08T10:30:00"
}
```

#### 4-2. dataScopeLevel 변경

**Request Body**:
```json
{
  "dataScopeLevel": "ADMIN"
}
```

**검증 항목**:
- ✅ 변경된 필드만 업데이트
- ✅ updatedAt 갱신

---

### Scenario 5: 역할에 권한 할당/제거 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

#### 5-1. 단일 권한 할당

**POST** `/api/rbac/roles/{roleName}/permissions/{permissionCode}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 roleName**: `MEMBER`
**사용할 permissionCode**: `report:export`

**예상 응답 (200 OK)**:
```json
{
  "roleId": "20000000-0000-0000-0000-000000000003",
  "name": "MEMBER",
  "permissions": [
    {"code": "user:read"},
    {"code": "org:read"},
    {"code": "report:view"},
    {"code": "report:export"}
  ],
  "message": "권한이 할당되었습니다"
}
```

**검증 항목**:
- ✅ 권한 추가됨
- ✅ 기존 권한 유지

#### 5-2. 단일 권한 제거

**DELETE** `/api/rbac/roles/{roleName}/permissions/{permissionCode}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
```

**사용할 roleName**: `MEMBER`
**사용할 permissionCode**: `report:export`

**예상 응답 (200 OK)**:
```json
{
  "roleId": "20000000-0000-0000-0000-000000000003",
  "name": "MEMBER",
  "permissions": [
    {"code": "user:read"},
    {"code": "org:read"},
    {"code": "report:view"}
  ],
  "message": "권한이 제거되었습니다"
}
```

#### 5-3. 여러 권한 일괄 할당

**POST** `/api/rbac/roles/{roleName}/permissions/batch`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 roleName**: `MEMBER`

**Request Body**:
```json
{
  "permissionCodes": [
    "user:update",
    "org:update"
  ]
}
```

**예상 응답 (200 OK)**:
```json
{
  "roleId": "20000000-0000-0000-0000-000000000003",
  "name": "MEMBER",
  "addedPermissions": [
    {"code": "user:update"},
    {"code": "org:update"}
  ],
  "totalPermissions": 5,
  "message": "2개의 권한이 할당되었습니다"
}
```

#### 5-4. 여러 권한 일괄 제거

**DELETE** `/api/rbac/roles/{roleName}/permissions/batch`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**Request Body**:
```json
{
  "permissionCodes": [
    "user:update",
    "org:update"
  ]
}
```

**예상 응답 (200 OK)**:
```json
{
  "roleId": "20000000-0000-0000-0000-000000000003",
  "name": "MEMBER",
  "removedPermissions": [
    {"code": "user:update"},
    {"code": "org:update"}
  ],
  "totalPermissions": 3,
  "message": "2개의 권한이 제거되었습니다"
}
```

**검증 항목**:
- ✅ 일괄 할당/제거 작동
- ✅ 권한 개수 업데이트

---

### Scenario 6: 역할의 권한 조회 ✅

**권한**: 모든 사용자 (인증 불필요)

**GET** `/api/rbac/roles/{roleName}/permissions`

**Headers**: (불필요)

**사용할 roleName**: `TEAM_LEAD`

**예상 응답 (200 OK)**:
```json
{
  "roleName": "TEAM_LEAD",
  "permissions": [
    {
      "permissionId": "30000000-0000-0000-0000-000000000002",
      "code": "user:read",
      "category": "USER_MANAGEMENT",
      "description": "사용자 조회",
      "assignedAt": "2026-02-08T00:00:00"
    },
    {
      "permissionId": "30000000-0000-0000-0000-000000000006",
      "code": "org:read",
      "category": "ORG_MANAGEMENT",
      "description": "조직 조회",
      "assignedAt": "2026-02-08T00:00:00"
    },
    {
      "permissionId": "30000000-0000-0000-0000-000000000009",
      "code": "report:view",
      "category": "REPORT",
      "description": "보고서 조회",
      "assignedAt": "2026-02-08T00:00:00"
    },
    {
      "permissionId": "30000000-0000-0000-0000-000000000010",
      "code": "report:export",
      "category": "REPORT",
      "description": "보고서 내보내기",
      "assignedAt": "2026-02-08T00:00:00"
    }
  ],
  "totalCount": 4
}
```

**검증 항목**:
- ✅ 역할의 모든 권한 조회
- ✅ 카테고리별로 그룹화 가능
- ✅ 할당 일시 표시

---

### Scenario 7: 역할 비활성화/활성화 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

#### 7-1. 역할 비활성화

**POST** `/api/rbac/roles/{roleName}/deactivate`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 roleName**: `PROJECT_MANAGER`

**Request Body** (선택):
```json
{
  "reason": "사용하지 않는 역할"
}
```

**예상 응답 (200 OK)**:
```json
{
  "roleId": "uuid",
  "name": "PROJECT_MANAGER",
  "isActive": false,
  "deactivatedAt": "2026-02-08T11:00:00",
  "message": "역할이 비활성화되었습니다"
}
```

**검증 항목**:
- ✅ isActive = false
- ✅ deactivatedAt 설정
- ✅ 해당 역할의 사용자는 권한 상실

#### 7-2. 역할 활성화

**POST** `/api/rbac/roles/{roleName}/activate`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
```

**사용할 roleName**: `PROJECT_MANAGER`

**예상 응답 (200 OK)**:
```json
{
  "roleId": "uuid",
  "name": "PROJECT_MANAGER",
  "isActive": true,
  "activatedAt": "2026-02-08T11:30:00",
  "message": "역할이 활성화되었습니다"
}
```

**검증 항목**:
- ✅ isActive = true
- ✅ activatedAt 설정

---

### Scenario 8: 역할 삭제 영향도 조회 ✅

**권한**: 모든 사용자 (인증 불필요)

**GET** `/api/rbac/roles/{roleName}/deletion-impact`

**Headers**: (불필요)

**사용할 roleName**: `TEAM_LEAD`

**예상 응답 (200 OK)**:
```json
{
  "roleName": "TEAM_LEAD",
  "canDelete": false,
  "affectedUserCount": 1,
  "affectedUsers": [
    {
      "agentId": "10000000-0000-0000-0000-000000000002",
      "loginId": "dev.lead",
      "name": "김팀장"
    }
  ],
  "warnings": [
    "1명의 사용자가 이 역할을 가지고 있습니다",
    "삭제하기 전에 사용자의 역할을 변경해야 합니다"
  ]
}
```

**검증 항목**:
- ✅ 영향받는 사용자 수 표시
- ✅ 영향받는 사용자 목록
- ✅ 삭제 가능 여부
- ✅ 경고 메시지

---

### Scenario 9: 역할을 사용하는 사용자 수 조회 ✅

**권한**: 모든 사용자 (인증 불필요)

**GET** `/api/rbac/roles/{roleName}/agent-count`

**Headers**: (불필요)

**사용할 roleName**: `ADMIN`

**예상 응답 (200 OK)**:
```json
{
  "roleName": "ADMIN",
  "totalCount": 1,
  "activeCount": 1,
  "suspendedCount": 0,
  "retiredCount": 0
}
```

**검증 항목**:
- ✅ 전체 사용자 수
- ✅ 상태별 집계

---

### Scenario 10: 역할 삭제 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**DELETE** `/api/rbac/roles/{roleName}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
```

**사용할 roleName**: `PROJECT_MANAGER` (사용자가 없는 역할)

**예상 응답 (204 No Content)**

**검증 항목**:
- ✅ 사용자가 없으면 삭제 성공
- ✅ 관련 권한 매핑도 삭제됨

#### 10-1. 사용자가 있는 역할 삭제 시도 ❌

**사용할 roleName**: `ADMIN`

**예상 응답 (409 Conflict)**:
```json
{
  "code": "ROLE_IN_USE",
  "message": "1명의 사용자가 이 역할을 사용 중입니다. 삭제할 수 없습니다"
}
```

---

### Scenario 11: 모든 권한 조회 ✅

**권한**: 모든 사용자 (인증 불필요)

**GET** `/api/rbac/permissions`

**Headers**: (불필요)

**Query Parameters** (선택):
```
category=USER_MANAGEMENT
```

**예상 응답 (200 OK)**:
```json
{
  "permissions": [
    {
      "permissionId": "30000000-0000-0000-0000-000000000001",
      "code": "user:create",
      "category": "USER_MANAGEMENT",
      "description": "사용자 생성",
      "resource": "user",
      "action": "create",
      "roleCount": 2,
      "createdAt": "2026-02-08T00:00:00"
    },
    {
      "permissionId": "30000000-0000-0000-0000-000000000002",
      "code": "user:read",
      "category": "USER_MANAGEMENT",
      "description": "사용자 조회",
      "resource": "user",
      "action": "read",
      "roleCount": 3
    },
    {
      "permissionId": "30000000-0000-0000-0000-000000000003",
      "code": "user:update",
      "category": "USER_MANAGEMENT",
      "description": "사용자 수정",
      "resource": "user",
      "action": "update",
      "roleCount": 2
    },
    {
      "permissionId": "30000000-0000-0000-0000-000000000004",
      "code": "user:delete",
      "category": "USER_MANAGEMENT",
      "description": "사용자 삭제",
      "resource": "user",
      "action": "delete",
      "roleCount": 2
    }
  ],
  "totalCount": 10,
  "categories": [
    {
      "name": "USER_MANAGEMENT",
      "count": 4
    },
    {
      "name": "ORG_MANAGEMENT",
      "count": 4
    },
    {
      "name": "REPORT",
      "count": 2
    }
  ]
}
```

**검증 항목**:
- ✅ 모든 권한 조회
- ✅ 카테고리별 그룹화
- ✅ 역할 개수 표시

---

### Scenario 12: 특정 권한 조회 ✅

**권한**: 모든 사용자 (인증 불필요)

**GET** `/api/rbac/permissions/{code}`

**Headers**: (불필요)

**사용할 code**: `user:create`

**예상 응답 (200 OK)**:
```json
{
  "permissionId": "30000000-0000-0000-0000-000000000001",
  "code": "user:create",
  "name": "사용자 생성",
  "category": "USER_MANAGEMENT",
  "description": "사용자 계정 생성 권한",
  "resource": "user",
  "action": "create",
  "roles": [
    {
      "roleId": "20000000-0000-0000-0000-000000000001",
      "name": "ADMIN"
    },
    {
      "roleId": "uuid",
      "name": "HR"
    }
  ],
  "roleCount": 2,
  "createdAt": "2026-02-08T00:00:00"
}
```

**검증 항목**:
- ✅ 권한 상세 정보
- ✅ 이 권한을 가진 역할 목록

---

### Scenario 13: 권한 생성 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**POST** `/api/rbac/permissions`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

#### 13-1. 새로운 권한 생성

**Request Body**:
```json
{
  "code": "dashboard:view",
  "name": "대시보드 조회",
  "category": "DASHBOARD",
  "description": "대시보드 조회 권한",
  "resource": "dashboard",
  "action": "view"
}
```

**예상 응답 (201 Created)**:
```json
{
  "permissionId": "uuid-generated",
  "code": "dashboard:view",
  "name": "대시보드 조회",
  "category": "DASHBOARD",
  "description": "대시보드 조회 권한",
  "resource": "dashboard",
  "action": "view",
  "createdAt": "2026-02-08T12:00:00"
}
```

**검증 항목**:
- ✅ permissionId 자동 생성
- ✅ code 형식 검증 (resource:action)

#### 13-2. 중복 권한 코드로 생성 시도 ❌

**Request Body**:
```json
{
  "code": "user:create",
  "name": "테스트",
  "category": "TEST"
}
```

**예상 응답 (400 Bad Request)**:
```json
{
  "code": "DUPLICATE_PERMISSION_CODE",
  "message": "이미 존재하는 권한 코드입니다"
}
```

---

### Scenario 14: 권한 수정 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**PATCH** `/api/rbac/permissions/{code}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 code**: `dashboard:view`

**Request Body**:
```json
{
  "description": "대시보드 및 통계 조회 권한 (수정됨)",
  "category": "ANALYTICS"
}
```

**예상 응답 (200 OK)**:
```json
{
  "permissionId": "uuid",
  "code": "dashboard:view",
  "description": "대시보드 및 통계 조회 권한 (수정됨)",
  "category": "ANALYTICS",
  "updatedAt": "2026-02-08T12:30:00"
}
```

**검증 항목**:
- ✅ 설명 변경됨
- ✅ 카테고리 변경됨
- ✅ code는 변경 불가

---

### Scenario 15: 권한 삭제 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**DELETE** `/api/rbac/permissions/{code}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
```

**사용할 code**: `dashboard:view` (역할에 할당되지 않은 권한)

**예상 응답 (204 No Content)**

**검증 항목**:
- ✅ 역할에 할당되지 않으면 삭제 성공

#### 15-1. 역할에 할당된 권한 삭제 시도 ❌

**사용할 code**: `user:create`

**예상 응답 (409 Conflict)**:
```json
{
  "code": "PERMISSION_IN_USE",
  "message": "2개의 역할이 이 권한을 사용 중입니다. 삭제할 수 없습니다",
  "roles": ["ADMIN", "HR"]
}
```

---

### Scenario 16: 특정 권한을 가진 역할 조회 ✅

**권한**: 모든 사용자 (인증 불필요)

**GET** `/api/rbac/permissions/{permissionCode}/roles`

**Headers**: (불필요)

**사용할 permissionCode**: `org:read`

**예상 응답 (200 OK)**:
```json
{
  "permissionCode": "org:read",
  "permissionName": "조직 조회",
  "roles": [
    {
      "roleId": "20000000-0000-0000-0000-000000000001",
      "name": "ADMIN",
      "type": "POSITION",
      "userCount": 1
    },
    {
      "roleId": "20000000-0000-0000-0000-000000000002",
      "name": "TEAM_LEAD",
      "type": "POSITION",
      "userCount": 1
    },
    {
      "roleId": "20000000-0000-0000-0000-000000000003",
      "name": "MEMBER",
      "type": "POSITION",
      "userCount": 1
    },
    {
      "roleId": "uuid",
      "name": "HR",
      "type": "POSITION",
      "userCount": 0
    }
  ],
  "totalRoleCount": 4
}
```

**검증 항목**:
- ✅ 해당 권한을 가진 모든 역할 조회
- ✅ 역할별 사용자 수 표시

---

### Scenario 17: 사용자에게 역할 할당/회수 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

#### 17-1. 사용자에게 역할 할당

**POST** `/api/rbac/agents/{agentId}/roles/{roleName}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 agentId**: `10000000-0000-0000-0000-000000000003` (이개발)
**사용할 roleName**: `HR`

**예상 응답 (200 OK)**:
```json
{
  "agentId": "10000000-0000-0000-0000-000000000003",
  "loginId": "dev.member",
  "name": "이개발",
  "roles": [
    {
      "roleId": "20000000-0000-0000-0000-000000000003",
      "name": "MEMBER",
      "assignedAt": "2026-02-08T00:00:00"
    },
    {
      "roleId": "uuid",
      "name": "HR",
      "assignedAt": "2026-02-08T13:00:00"
    }
  ],
  "message": "역할이 할당되었습니다"
}
```

**검증 항목**:
- ✅ 역할 추가됨
- ✅ 기존 역할 유지
- ✅ assignedAt 기록

#### 17-2. 사용자에게서 역할 회수

**DELETE** `/api/rbac/agents/{agentId}/roles/{roleName}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
```

**사용할 agentId**: `10000000-0000-0000-0000-000000000003`
**사용할 roleName**: `HR`

**예상 응답 (200 OK)**:
```json
{
  "agentId": "10000000-0000-0000-0000-000000000003",
  "loginId": "dev.member",
  "roles": [
    {
      "name": "MEMBER"
    }
  ],
  "message": "역할이 회수되었습니다"
}
```

---

### Scenario 18: 사용자의 역할 목록 조회 ✅

**권한**: 모든 사용자 (인증 불필요)

**GET** `/api/rbac/agents/{agentId}/roles`

**Headers**: (불필요)

**사용할 agentId**: `10000000-0000-0000-0000-000000000002` (김팀장)

**예상 응답 (200 OK)**:
```json
{
  "agentId": "10000000-0000-0000-0000-000000000002",
  "loginId": "dev.lead",
  "name": "김팀장",
  "roles": [
    {
      "roleId": "20000000-0000-0000-0000-000000000002",
      "name": "TEAM_LEAD",
      "type": "POSITION",
      "dataScopeLevel": "TEAM_LEAD",
      "description": "팀장",
      "assignedAt": "2026-02-08T00:00:00"
    }
  ],
  "totalRoleCount": 1
}
```

**검증 항목**:
- ✅ 사용자의 모든 역할 조회
- ✅ 역할 상세 정보 포함

---

### Scenario 19: 사용자의 실제 권한 조회 ✅

**권한**: 모든 사용자 (인증 불필요)

**GET** `/api/rbac/agents/{agentId}/effective-permissions`

**Headers**: (불필요)

**사용할 agentId**: `10000000-0000-0000-0000-000000000002` (김팀장)

**예상 응답 (200 OK)**:
```json
{
  "agentId": "10000000-0000-0000-0000-000000000002",
  "loginId": "dev.lead",
  "name": "김팀장",
  "roles": ["TEAM_LEAD"],
  "permissions": [
    {
      "code": "user:read",
      "category": "USER_MANAGEMENT",
      "description": "사용자 조회",
      "grantedBy": ["TEAM_LEAD"]
    },
    {
      "code": "org:read",
      "category": "ORG_MANAGEMENT",
      "description": "조직 조회",
      "grantedBy": ["TEAM_LEAD"]
    },
    {
      "code": "report:view",
      "category": "REPORT",
      "description": "보고서 조회",
      "grantedBy": ["TEAM_LEAD"]
    },
    {
      "code": "report:export",
      "category": "REPORT",
      "description": "보고서 내보내기",
      "grantedBy": ["TEAM_LEAD"]
    }
  ],
  "totalPermissionCount": 4,
  "permissionsByCategory": {
    "USER_MANAGEMENT": 1,
    "ORG_MANAGEMENT": 1,
    "REPORT": 2
  }
}
```

**검증 항목**:
- ✅ 모든 역할의 권한 통합
- ✅ 중복 권한 제거
- ✅ 권한 출처(grantedBy) 표시
- ✅ 카테고리별 집계

---

## 🎯 테스트 체크리스트

### 역할 관리
- [x] 역할 목록 조회
- [x] 역할 생성
- [x] 역할 수정 (설명, dataScopeLevel)
- [x] 역할 삭제
- [x] 역할 비활성화/활성화
- [x] 역할 삭제 영향도 조회
- [x] 역할 사용자 수 조회

### 권한 관리
- [x] 권한 목록 조회
- [x] 권한 생성
- [x] 권한 수정
- [x] 권한 삭제
- [x] 카테고리별 조회

### 역할-권한 매핑
- [x] 단일 권한 할당/제거
- [x] 여러 권한 일괄 할당/제거
- [x] 역할의 권한 목록 조회
- [x] 권한을 가진 역할 조회

### 사용자-역할 매핑
- [x] 사용자에게 역할 할당/회수
- [x] 사용자의 역할 목록 조회
- [x] 사용자의 실제 권한 조회 (effective permissions)

### 검증 및 제약
- [x] 중복 역할명 방지
- [x] 중복 권한 코드 방지
- [x] 사용 중인 역할 삭제 방지
- [x] 사용 중인 권한 삭제 방지
- [x] 권한 검증 (ADMIN만 가능)

---

## 📊 최종 RBAC 구조 (모든 테스트 완료 후)

### 역할 (4개)
| name | type | dataScopeLevel | isActive | userCount |
|------|------|----------------|----------|-----------|
| ADMIN | POSITION | ADMIN | ✅ | 1 |
| TEAM_LEAD | POSITION | TEAM_LEAD | ✅ | 1 |
| MEMBER | POSITION | MEMBER | ✅ | 1 |
| HR | POSITION | ADMIN | ✅ | 0 |

### 권한 (10개)
| code | category | roleCount |
|------|----------|-----------|
| user:create | USER_MANAGEMENT | 2 |
| user:read | USER_MANAGEMENT | 3 |
| user:update | USER_MANAGEMENT | 2 |
| user:delete | USER_MANAGEMENT | 2 |
| org:create | ORG_MANAGEMENT | 1 |
| org:read | ORG_MANAGEMENT | 4 |
| org:update | ORG_MANAGEMENT | 1 |
| org:delete | ORG_MANAGEMENT | 1 |
| report:view | REPORT | 3 |
| report:export | REPORT | 2 |

### 역할별 권한 매핑
**ADMIN**: 모든 권한 (10개)
**TEAM_LEAD**: user:read, org:read, report:view, report:export (4개)
**MEMBER**: user:read, org:read, report:view (3개)
**HR**: user:create, user:read, user:update, user:delete, org:read (5개)

---

## 🚀 빠른 시작

1. **애플리케이션 실행**
   ```bash
   .\gradlew bootRun
   ```

2. **Swagger UI 접속**
   ```
   http://localhost:8080/swagger-ui/index.html
   ```

3. **Scenario 1부터 순차적으로 테스트**

4. **각 단계마다 응답 확인 및 검증**

---

## 🔑 사용자별 테스트 가이드

### 모든 사용자 (인증 불필요)
**가능한 작업**: 조회만 가능
- ✅ 역할 조회 (Scenario 1-2)
- ✅ 권한 조회 (Scenario 11-12)
- ✅ 역할-권한 매핑 조회 (Scenario 6, 16)
- ✅ 사용자-역할 매핑 조회 (Scenario 18-19)
- ✅ 통계 조회 (Scenario 8-9)
- ❌ 생성, 수정, 삭제

**특징**:
- X-User-Id 헤더 **불필요**
- 권한 검증 없음
- 공개 조회 API

**테스트 순서**:
```
1. Scenario 1-2: 역할 조회 (헤더 불필요) ✅
2. Scenario 6: 역할의 권한 조회 (헤더 불필요) ✅
3. Scenario 8-9: 통계 조회 (헤더 불필요) ✅
4. Scenario 11-12: 권한 조회 (헤더 불필요) ✅
5. Scenario 16: 권한의 역할 조회 (헤더 불필요) ✅
6. Scenario 18-19: 사용자 역할/권한 조회 (헤더 불필요) ✅
```

---

### ADMIN (10000000-0000-0000-0000-000000000001)
**가능한 작업**: 전체 권한
- ✅ 조회 (모든 조회 API)
- ✅ 역할 생성/수정/삭제 (Scenario 3-4, 10)
- ✅ 권한 생성/수정/삭제 (Scenario 13-15)
- ✅ 역할-권한 매핑 (Scenario 5)
- ✅ 사용자-역할 매핑 (Scenario 17)
- ✅ 역할 비활성화/활성화 (Scenario 7)

**특징**:
- 조회 API: X-User-Id **불필요**
- 생성/수정/삭제 API: X-User-Id **필수**

**테스트 순서**:
```
1. Scenario 1-2: 역할 조회 (헤더 불필요) ✅
2. Scenario 3-4: 역할 생성/수정 (X-User-Id 필요) ✅
3. Scenario 5-6: 권한 할당/조회 (할당은 X-User-Id 필요) ✅
4. Scenario 7: 역할 비활성화/활성화 (X-User-Id 필요) ✅
5. Scenario 8-9: 통계 조회 (헤더 불필요) ✅
6. Scenario 10: 역할 삭제 (X-User-Id 필요) ✅
7. Scenario 11-16: 권한 관리 (조회는 헤더 불필요) ✅
8. Scenario 17-19: 사용자-역할 매핑 (조회는 헤더 불필요) ✅
```

---

### TEAM_LEAD (10000000-0000-0000-0000-000000000002)
**가능한 작업**: 조회만 가능
- ✅ 모든 조회 API
- ❌ 생성, 수정, 삭제

**특징**:
- ADMIN과 동일하게 조회 가능
- X-User-Id 헤더 **불필요**

**테스트 순서**:
```
1. Scenario 1-2: 역할 조회 (헤더 불필요) ✅
2. Scenario 3-4: 역할 생성 시도 (X-User-Id로 시도, 403 Forbidden) ❌
3. Scenario 11-12: 권한 조회 (헤더 불필요) ✅
4. Scenario 18-19: 사용자 역할/권한 조회 (헤더 불필요) ✅
```

---

### MEMBER (10000000-0000-0000-0000-000000000003)
**가능한 작업**: 조회만 가능
- ✅ 모든 조회 API
- ❌ 생성, 수정, 삭제

**특징**:
- TEAM_LEAD와 동일
- X-User-Id 헤더 **불필요**

**테스트 순서**:
```
1. Scenario 1-2: 역할 조회 (헤더 불필요) ✅
2. Scenario 11-12: 권한 조회 (헤더 불필요) ✅
3. Scenario 18-19: 본인 역할/권한 조회 (헤더 불필요) ✅
4. Scenario 3: 역할 생성 시도 (X-User-Id로 시도, 403 Forbidden) ❌
```

---

## 📋 X-User-Id 헤더 사용법

### ⚠️ 중요: X-User-Id 헤더 필요 여부

**조회 API (헤더 불필요)**:
- ❌ Scenario 1-2: 역할 조회
- ❌ Scenario 6: 역할의 권한 조회
- ❌ Scenario 8-9: 통계 조회
- ❌ Scenario 11-12: 권한 조회
- ❌ Scenario 16: 권한의 역할 조회
- ❌ Scenario 18-19: 사용자 역할/권한 조회

**생성/수정/삭제 API (헤더 필수, ADMIN만 가능)**:
- ✅ Scenario 3: 역할 생성
- ✅ Scenario 4: 역할 수정
- ✅ Scenario 5: 권한 할당/제거
- ✅ Scenario 7: 역할 비활성화/활성화
- ✅ Scenario 10: 역할 삭제
- ✅ Scenario 13: 권한 생성
- ✅ Scenario 14: 권한 수정
- ✅ Scenario 15: 권한 삭제
- ✅ Scenario 17: 사용자-역할 할당/회수


---

### Swagger UI에서
**조회 API**: 헤더 추가 불필요 (그냥 실행)

**생성/수정/삭제 API**: 
1. **Parameters 섹션에서 X-User-Id 헤더 입력**
2. 값: `10000000-0000-0000-0000-000000000001` (ADMIN)

### cURL에서
**조회**:
```bash
curl -X GET "http://localhost:8080/api/rbac/roles"
```

**생성/수정/삭제**:
```bash
curl -X POST "http://localhost:8080/api/rbac/roles" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000001" \
  -H "Content-Type: application/json" \
  -d '{"name": "NEW_ROLE", "type": "POSITION"}'
```

### Postman에서
**조회**: 헤더 불필요

**생성/수정/삭제**:
```
Headers 탭:
Key: X-User-Id
Value: 10000000-0000-0000-0000-000000000001
```

---

## ✅ 성공 기준

- 모든 API가 예상된 상태 코드 반환
- 권한 검증이 정확하게 작동
- 역할-권한 매핑이 올바름
- 사용자-역할 매핑이 올바름
- effective permissions 계산이 정확
- 중복 방지 로직이 작동
- 삭제 제약 조건이 작동
- 영향도 분석이 정확

🎉 **Happy Testing!**