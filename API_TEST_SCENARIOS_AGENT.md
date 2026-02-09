# API 테스트 시나리오 - Agent Management

## 🎯 테스트 환경
- **Base URL**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **기본 인증**: Mock 인증 (자동)
- **테넌트**: `default-tenant`

---

## 📋 초기 데이터 (SQL 자동 삽입)

### 사용자 (3명)
| agent_id | login_id | name | employee_id | email | dept_id | status | role |
|----------|----------|------|-------------|-------|---------|--------|------|
| 10000000-0000-0000-0000-000000000001 | admin | 시스템관리자 | EMP-0001 | admin@nexfron.com | 넥스프론 | ACTIVE | ADMIN |
| 10000000-0000-0000-0000-000000000002 | dev.lead | 김팀장 | EMP-0002 | dev.lead@nexfron.com | 백엔드팀 | ACTIVE | TEAM_LEAD |
| 10000000-0000-0000-0000-000000000003 | dev.member | 이개발 | EMP-0003 | dev.member@nexfron.com | 백엔드팀 | ACTIVE | MEMBER |

### 🔑 로그인 정보
**모든 계정의 비밀번호**: `Admin123!`

| login_id | password |
|----------|----------|
| admin | Admin123! |
| dev.lead | Admin123! |
| dev.member | Admin123! |

⚠️ **중요**: 비밀번호 변경 테스트 시 `currentPassword`에 `Admin123!`를 사용하세요!

### 부서 (5개)
| dept_id | name | code |
|---------|------|------|
| 00000000-0000-0000-0000-000000000001 | 넥스프론 | NEXFRON |
| 00000000-0000-0000-0000-000000000002 | 개발본부 | DEV-DIV |
| 00000000-0000-0000-0000-000000000003 | 영업본부 | SALES-DIV |
| 00000000-0000-0000-0000-000000000004 | 백엔드팀 | DEV-BE |
| 00000000-0000-0000-0000-000000000005 | 프론트엔드팀 | DEV-FE |

---

## ⚠️ 실제 API 응답 형태

### 필드명 차이
- **문서**: `agentId` → **실제 API**: `id`
- 모든 응답 예제는 실제 API에 맞춰 `id`로 작성되었습니다.

### 응답 구조
- **목록 조회**: 배열 형태로 반환 (페이징 정보 없음)
- **단건 조회**: 객체 형태로 반환

### 기본 포함 필드
모든 상담사 응답에는 다음 필드가 포함됩니다:
- `id`, `loginId`, `name`
- `organizationId`, `departmentName`, `departmentPath`
- `employeeId`, `email`, `phone` (null 가능)
- `status`, `passwordMustChange`, `createdAt`, `retiredAt`
- `roles` (배열, 빈 배열 가능)

### 초기 데이터의 특징
- `employeeId`, `email`, `phone`은 대부분 **null**
- `roles`는 초기에 **빈 배열** (별도 할당 필요)
- `departmentName`은 "백엔드개발팀" (문서의 "백엔드팀"과 다름)

---

## 🧪 테스트 시나리오

### Scenario 1: 상담사 목록 조회 ✅

**권한**: 모든 사용자 (인증 불필요)

**GET** `/api/v1/agents`

**Headers**: (불필요)

**Query Parameters** (필수: tenantId):
```
tenantId=default-tenant
status=ACTIVE
organizationId=00000000-0000-0000-0000-000000000004
page=0
size=20
```

**예상 응답 (200 OK)**:
```json
[
  {
    "id": "10000000-0000-0000-0000-000000000001",
    "loginId": "admin",
    "name": "시스템관리자",
    "organizationId": "00000000-0000-0000-0000-000000000001",
    "departmentName": "넥스프론",
    "departmentPath": "넥스프론",
    "employeeId": "EMP-0001",
    "email": "admin@nexfron.com",
    "phone": null,
    "status": "ACTIVE",
    "passwordMustChange": false,
    "createdAt": "2026-02-08T01:16:33.042785",
    "retiredAt": null,
    "roles": []
  },
  {
    "id": "10000000-0000-0000-0000-000000000002",
    "loginId": "dev.lead",
    "name": "김팀장",
    "organizationId": "00000000-0000-0000-0000-000000000004",
    "departmentName": "백엔드개발팀",
    "departmentPath": "넥스프론 > 개발본부 > 백엔드개발팀",
    "employeeId": null,
    "email": null,
    "phone": null,
    "status": "ACTIVE",
    "passwordMustChange": false,
    "createdAt": "2026-02-08T01:16:33.042785",
    "retiredAt": null,
    "roles": []
  },
  {
    "id": "10000000-0000-0000-0000-000000000003",
    "loginId": "dev.member",
    "name": "이개발",
    "organizationId": "00000000-0000-0000-0000-000000000004",
    "departmentName": "백엔드개발팀",
    "departmentPath": "넥스프론 > 개발본부 > 백엔드개발팀",
    "employeeId": null,
    "email": null,
    "phone": null,
    "status": "ACTIVE",
    "passwordMustChange": false,
    "createdAt": "2026-02-08T01:16:33.042785",
    "retiredAt": null,
    "roles": []
  }
]
```

**검증 항목**:
- ✅ 전체 상담사 목록 조회됨 (배열 형태)
- ✅ 부서명 및 부서 경로(departmentPath) 표시됨
- ✅ roles 배열 포함 (빈 배열일 수 있음)
- ✅ passwordMustChange, retiredAt 필드 포함
- ⚠️ **주의**: 페이징 정보는 별도 API 또는 헤더로 제공될 수 있음

---

### Scenario 2: 상담사 단건 조회 ✅

**권한**: 모든 사용자 (인증 불필요)

**GET** `/api/v1/agents/{agentId}`

**Headers**: (불필요)

**사용할 agentId**: `10000000-0000-0000-0000-000000000002` (김팀장)

**예상 응답 (200 OK)**:
```json
{
  "id": "10000000-0000-0000-0000-000000000002",
  "loginId": "dev.lead",
  "name": "김팀장",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "departmentName": "백엔드개발팀",
  "departmentPath": "넥스프론 > 개발본부 > 백엔드개발팀",
  "employeeId": null,
  "email": null,
  "phone": null,
  "status": "ACTIVE",
  "passwordMustChange": false,
  "createdAt": "2026-02-08T01:16:33.042785",
  "retiredAt": null,
  "roles": []
}
```

**검증 항목**:
- ✅ 상세 정보 조회됨
- ✅ 부서 경로(departmentPath) 표시
- ✅ roles 배열 포함 (빈 배열일 수 있음)
- ✅ passwordMustChange, retiredAt 필드 포함
- ⚠️ **주의**: 역할이 할당되지 않은 경우 빈 배열

---

### Scenario 3: 로그인 아이디 중복 체크 ✅

**권한**: 모든 사용자 (공개 API)

**GET** `/api/v1/agents/check-login-id?loginId={loginId}`

**Headers**: (불필요)

#### 3-1. 사용 가능한 아이디

**Query Parameters**:
```
loginId=new.user
```

**예상 응답 (200 OK)**:
```json
{
  "available": true,
  "loginId": "new.user",
  "message": "사용 가능한 로그인 아이디입니다"
}
```

#### 3-2. 이미 사용 중인 아이디

**Query Parameters**:
```
loginId=admin
```

**예상 응답 (200 OK)**:
```json
{
  "available": false,
  "loginId": "admin",
  "message": "이미 사용 중인 로그인 아이디입니다"
}
```

**검증 항목**:
- ✅ 중복 검사 정확
- ✅ 사용 가능 여부 명확

---

### Scenario 4: 상담사 생성 ✅

**권한**: 모든 사용자 (인증 불필요)

**POST** `/api/v1/agents`

**Headers**: 
```
Content-Type: application/json
```

⚠️ **주의**: X-User-Id 헤더는 **불필요**합니다. 생성 API도 조회 API와 동일하게 공개 API입니다.

#### 4-1. 백엔드 개발자 생성

**Request Body** (필수 필드):
```json
{
  "tenantId": "default-tenant",
  "loginId": "backend.dev",
  "name": "박개발",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "roles": ["MEMBER"]
}
```

**선택 필드 포함 예시**:
```json
{
  "tenantId": "default-tenant",
  "loginId": "backend.dev",
  "name": "박개발",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "roles": ["MEMBER"],
  "email": "backend.dev@nexfron.com",
  "phone": "010-1234-5678",
  "employeeId": "EMP-0004"
}
```

**예상 응답 (201 Created)**:
```json
{
  "agentId": "uuid-generated",
  "loginId": "backend.dev",
  "tempPassword": "Temp1234!@#$"
}
```

**검증 항목**:
- ✅ agentId 자동 생성됨
- ✅ **tempPassword 자동 생성됨** (일회성, 재조회 불가)
- ✅ password는 Request Body에 포함 안 함
- ✅ **roles 필수**: 최소 1개 이상 (ADMIN, TEAM_LEAD, MEMBER 중 선택)
- ❌ 응답에는 agentId, loginId, tempPassword만 포함

#### 4-2. 프론트엔드 팀장 생성

**Request Body**:
```json
{
  "tenantId": "default-tenant",
  "loginId": "frontend.lead",
  "name": "최팀장",
  "organizationId": "00000000-0000-0000-0000-000000000005",
  "roles": ["TEAM_LEAD"],
  "email": "frontend.lead@nexfron.com",
  "employeeId": "EMP-0005"
}
```

**예상 응답 (201 Created)**:
```json
{
  "agentId": "uuid-generated",
  "loginId": "frontend.lead",
  "tempPassword": "Auto1234!@#$"
}
```

**검증 항목**:
- ✅ tempPassword 자동 생성됨
- ✅ 프론트엔드팀 소속
- ✅ **TEAM_LEAD 역할 할당됨**
- ❌ 역할은 응답에 포함 안 됨 (조회 API로 확인 필요)

#### 4-3. 중복 로그인 아이디로 생성 시도 ❌

**Request Body**:
```json
{
  "tenantId": "default-tenant",
  "loginId": "admin",
  "name": "테스트",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "roles": ["MEMBER"]
}
```

**예상 응답 (409 Conflict)**:
```json
{
  "code": "A002",
  "message": "이미 사용 중인 아이디입니다."
}
```

#### 4-4. roles 없이 생성 시도 ❌

**Request Body**:
```json
{
  "tenantId": "default-tenant",
  "loginId": "test.user",
  "name": "테스트",
  "organizationId": "00000000-0000-0000-0000-000000000004"
}
```

**예상 응답 (400 Bad Request)**:
```json
{
  "code": "INVALID_INPUT_VALUE",
  "message": "역할은 최소 1개 이상이어야 합니다"
}
```

**검증 항목**:
- ✅ HTTP 400 Bad Request 반환
- ✅ Bean Validation 메시지 정확
- ✅ roles 필드 누락 시 자동 검증


---

### Scenario 5: 상담사 정보 수정 ✅

**⚠️ 필수 헤더**: `X-User-Id` (본인 또는 ADMIN만 수정 가능)

**PATCH** `/api/v1/agents/{agentId}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000003
Content-Type: application/json
```

**사용할 agentId**: `10000000-0000-0000-0000-000000000003` (이개발)

#### 5-1. 본인이 이름과 이메일 변경

**Request Body**:
```json
{
  "name": "이시니어",
  "email": "lee.senior@nexfron.com"
}
```

**예상 응답 (200 OK)**:
```json
{
  "id": "10000000-0000-0000-0000-000000000003",
  "loginId": "dev.member",
  "name": "이시니어",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "departmentName": "백엔드개발팀",
  "departmentPath": "넥스프론 > 개발본부 > 백엔드개발팀",
  "employeeId": null,
  "email": "lee.senior@nexfron.com",
  "phone": null,
  "status": "ACTIVE",
  "passwordMustChange": false,
  "createdAt": "2026-02-08T01:16:33.042785",
  "retiredAt": null,
  "roles": []
}
```

**검증 항목**:
- ✅ 변경된 필드만 업데이트됨 (name)
- ✅ 다른 필드는 유지됨
- ✅ 전체 객체 반환 안 됨 (204 No Content 응답)
- ✅ 본인이 수정 가능

#### 5-2. 전화번호 추가

**Request Body**:
```json
{
  "phone": "010-9876-5432"
}
```

**예상 응답 (204 No Content)**: 응답 본문 없음

#### 5-3. 권한 없는 사용자로 다른 사람 수정 시도 (MEMBER가 다른 사람 수정) ❌

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000003
Content-Type: application/json
```

**사용할 agentId**: `10000000-0000-0000-0000-000000000002` (김팀장 - 다른 사람)

**Request Body**:
```json
{
  "name": "김매니저"
}
```

**예상 응답 (400 Bad Request)**:
```json
{
  "code": "A005",
  "message": "본인 또는 관리자만 상담사 정보를 수정할 수 있습니다."
}
```

**검증 항목**:
- ✅ 본인이 아닌 경우 수정 불가
- ✅ ADMIN이 아닌 경우 다른 사람 수정 불가
- ✅ HTTP 400 Bad Request 반환
- ✅ 적절한 에러 메시지

---

### Scenario 6: 상담사 부서 이동 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**POST** `/api/v1/agents/{agentId}/transfer`

**또는**

**PATCH** `/api/v1/agents/{agentId}/organization`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 agentId**: `10000000-0000-0000-0000-000000000003` (이시니어)

#### 6-1. 백엔드팀 → 프론트엔드팀 이동

**Request Body** (transfer):
```json
{
  "newOrganizationId": "00000000-0000-0000-0000-000000000005",
  "transferReason": "업무 재배치"
}
```

**또는 Request Body** (organization):
```json
{
  "organizationId": "00000000-0000-0000-0000-000000000005"
}
```

**예상 응답 (200 OK)**:
```json
{
  "id": "10000000-0000-0000-0000-000000000003",
  "loginId": "dev.member",
  "name": "이시니어",
  "organizationId": "00000000-0000-0000-0000-000000000005",
  "departmentName": "프론트엔드팀",
  "departmentPath": "넥스프론 > 개발본부 > 프론트엔드팀",
  "employeeId": null,
  "email": "lee.senior@nexfron.com",
  "phone": null,
  "status": "ACTIVE",
  "passwordMustChange": false,
  "createdAt": "2026-02-08T01:16:33.042785",
  "retiredAt": null,
  "roles": []
}
```

**검증 항목**:
- ✅ organizationId 변경됨
- ✅ departmentName 업데이트됨
- ✅ departmentPath 업데이트됨

#### 6-2. 존재하지 않는 부서로 이동 시도 ❌

**Request Body**:
```json
{
  "newOrganizationId": "99999999-9999-9999-9999-999999999999"
}
```

**예상 응답 (404 Not Found)**:
```json
{
  "code": "A006",
  "message": "이동할 부서를 찾을 수 없습니다."
}
```

**검증 항목**:
- ✅ HTTP 404 Not Found 반환
- ✅ 부서 존재 여부 검증 수행
- ✅ 적절한 에러 메시지

---

### Scenario 7: 비밀번호 초기화 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**POST** `/api/v1/agents/{agentId}/reset-password`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 agentId**: `10000000-0000-0000-0000-000000000003`

**Request Body** (선택):
```json
{
  "newPassword": "TempPassword123!"
}
```

**예상 응답 (200 OK)**:
```json
{
  "id": "10000000-0000-0000-0000-000000000003",
  "loginId": "dev.member",
  "temporaryPassword": "TempPassword123!",
  "passwordMustChange": true,
  "message": "비밀번호가 초기화되었습니다. 다음 로그인 시 비밀번호를 변경해야 합니다."
}
```

**검증 항목**:
- ✅ passwordMustChange = true 설정됨
- ✅ 임시 비밀번호 반환됨 (또는 자동 생성)

---

### Scenario 8: 비밀번호 변경 (본인) ✅

**⚠️ 필수 헤더**: `X-User-Id` (본인만 가능)

**POST** `/api/v1/agents/me/change-password`

**또는**

**POST** `/api/v1/agents/{agentId}/change-password`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000003
Content-Type: application/json
```

#### 8-1. 본인이 비밀번호 변경

**Request Body**:
```json
{
  "currentPassword": "Admin123!",
  "newPassword": "MyNewPassword456!",
  "confirmPassword": "MyNewPassword456!"
}
```

**예상 응답 (204 No Content)**: 응답 본문 없음

**검증 항목**:
- ✅ passwordMustChange = false로 변경됨
- ✅ 현재 비밀번호 검증됨 (Admin123!)
- ✅ confirmPassword 일치 확인

#### 8-2. 현재 비밀번호 불일치 ❌

**Request Body**:
```json
{
  "currentPassword": "WrongPassword123!",
  "newPassword": "MyNewPassword456!",
  "confirmPassword": "MyNewPassword456!"
}
```

**예상 응답 (400 Bad Request)**:
```json
{
  "code": "P001",
  "message": "현재 비밀번호가 일치하지 않습니다."
}
```

#### 8-3. 비밀번호 확인 불일치 ❌

**Request Body**:
```json
{
  "currentPassword": "Admin123!",
  "newPassword": "MyNewPassword456!",
  "confirmPassword": "DifferentPassword789!"
}
```

**예상 응답 (400 Bad Request)**:
```json
{
  "code": "P002",
  "message": "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."
}
```

#### 8-4. 새 비밀번호가 현재 비밀번호와 동일 ❌

**Request Body**:
```json
{
  "currentPassword": "Admin123!",
  "newPassword": "Admin123!",
  "confirmPassword": "Admin123!"
}
```

**예상 응답 (400 Bad Request)**:
```json
{
  "code": "P003",
  "message": "새 비밀번호는 현재 비밀번호와 달라야 합니다."
}
```

---

### Scenario 9: 역할 관리 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

#### 9-1. 상담사에게 역할 추가

**POST** `/api/v1/agents/{agentId}/roles/{roleName}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 agentId**: `10000000-0000-0000-0000-000000000003`
**사용할 roleName**: `TEAM_LEAD`

**예상 응답 (200 OK)**:
```json
{
  "id": "10000000-0000-0000-0000-000000000003",
  "loginId": "dev.member",
  "name": "이시니어",
  "roles": [
    {
      "roleId": "20000000-0000-0000-0000-000000000002",
      "name": "TEAM_LEAD",
      "type": "POSITION",
      "assignedAt": "2026-02-08T12:00:00"
    },
    {
      "roleId": "20000000-0000-0000-0000-000000000003",
      "name": "MEMBER",
      "type": "POSITION",
      "assignedAt": "2026-02-08T00:00:00"
    }
  ]
}
```

**검증 항목**:
- ✅ 역할이 추가됨
- ✅ 기존 역할 유지됨
- ✅ assignedAt 기록됨

#### 9-2. 상담사에게서 역할 제거

**DELETE** `/api/v1/agents/{agentId}/roles/{roleName}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
```

**사용할 roleName**: `MEMBER`

**예상 응답 (200 OK)**:
```json
{
  "id": "10000000-0000-0000-0000-000000000003",
  "loginId": "dev.member",
  "roles": [
    {
      "name": "TEAM_LEAD"
    }
  ],
  "message": "역할이 제거되었습니다"
}
```

#### 9-3. 역할 일괄 지정

**PUT** `/api/v1/agents/{agentId}/roles`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**Request Body**:
```json
{
  "roleIds": [
    "20000000-0000-0000-0000-000000000002",
    "20000000-0000-0000-0000-000000000003"
  ]
}
```

**또는**:
```json
{
  "roleNames": ["TEAM_LEAD", "MEMBER"]
}
```

**예상 응답 (200 OK)**:
```json
{
  "id": "10000000-0000-0000-0000-000000000003",
  "roles": [
    {"name": "TEAM_LEAD"},
    {"name": "MEMBER"}
  ],
  "message": "역할이 일괄 지정되었습니다"
}
```

**검증 항목**:
- ✅ 기존 역할 모두 제거됨
- ✅ 새 역할만 할당됨

---

### Scenario 10: 상담사 정지 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**POST** `/api/v1/agents/{agentId}/suspend`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 agentId**: 새로 생성한 `backend.dev`의 agentId

**Request Body** (선택):
```json
{
  "reason": "규정 위반",
  "suspendedUntil": "2026-03-08T00:00:00"
}
```

**예상 응답 (200 OK)**:
```json
{
  "id": "uuid",
  "loginId": "backend.dev",
  "name": "박개발",
  "status": "SUSPENDED",
  "suspendedAt": "2026-02-08T13:00:00",
  "suspendedUntil": "2026-03-08T00:00:00",
  "suspendReason": "규정 위반"
}
```

**검증 항목**:
- ✅ status = SUSPENDED
- ✅ suspendedAt 기록됨
- ✅ 정지 기한 설정됨

---

### Scenario 11: 상담사 활성화 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**POST** `/api/v1/agents/{agentId}/activate`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 agentId**: 정지된 `backend.dev`의 agentId

**예상 응답 (200 OK)**:
```json
{
  "id": "uuid",
  "loginId": "backend.dev",
  "name": "박개발",
  "status": "ACTIVE",
  "activatedAt": "2026-02-08T13:30:00",
  "suspendedAt": null,
  "suspendedUntil": null
}
```

**검증 항목**:
- ✅ status = ACTIVE
- ✅ suspendedAt 제거됨
- ✅ activatedAt 기록됨

---

### Scenario 12: 상담사 퇴사 처리 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

**DELETE** `/api/v1/agents/{agentId}`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**사용할 agentId**: `backend.dev`의 agentId

**Request Body** (선택):
```json
{
  "retireReason": "자진 퇴사",
  "scheduledDeleteAt": "2026-03-08T00:00:00"
}
```

**예상 응답 (200 OK)**:
```json
{
  "id": "uuid",
  "loginId": "backend.dev",
  "name": "박개발",
  "status": "RETIRED",
  "retiredAt": "2026-02-08T14:00:00",
  "scheduledDeleteAt": "2026-03-08T00:00:00",
  "message": "퇴사 처리되었습니다. 30일 후 자동 삭제됩니다."
}
```

**검증 항목**:
- ✅ status = RETIRED
- ✅ retiredAt 기록됨
- ✅ 자동 삭제 예정일 설정됨
- ✅ 로그인 불가 상태

---

### Scenario 13: 상담사 통계 조회 ✅

**권한**: 모든 사용자 (공개 API)

**GET** `/api/v1/agents/statistics`

**Headers**: (불필요)

**Query Parameters**:
```
tenantId=default-tenant
```

**예상 응답 (200 OK)**:
```json
{
  "total": 5,
  "active": 4,
  "suspended": 0,
  "retired": 1,
  "byRole": {
    "ADMIN": 1,
    "TEAM_LEAD": 2,
    "MEMBER": 1
  },
  "byDepartment": {
    "00000000-0000-0000-0000-000000000001": {
      "departmentName": "넥스프론",
      "count": 1
    },
    "00000000-0000-0000-0000-000000000004": {
      "departmentName": "백엔드팀",
      "count": 1
    },
    "00000000-0000-0000-0000-000000000005": {
      "departmentName": "프론트엔드팀",
      "count": 2
    }
  }
}
```

**검증 항목**:
- ✅ 전체 통계 정확
- ✅ 역할별 집계 정확
- ✅ 부서별 집계 정확

---

### Scenario 14: 조직별 상담사 통계 조회 ✅

**권한**: 모든 사용자 (공개 API)

**GET** `/api/v1/agents/statistics/organization/{organizationId}`

**Headers**: (불필요)

**사용할 organizationId**: `00000000-0000-0000-0000-000000000005` (프론트엔드팀)

**예상 응답 (200 OK)**:
```json
{
  "organizationId": "00000000-0000-0000-0000-000000000005",
  "organizationName": "프론트엔드팀",
  "total": 2,
  "active": 2,
  "suspended": 0,
  "retired": 0,
  "members": [
    {
      "id": "10000000-0000-0000-0000-000000000003",
      "name": "이시니어",
      "roles": ["TEAM_LEAD"]
    },
    {
      "id": "uuid",
      "name": "최팀장",
      "roles": ["TEAM_LEAD"]
    }
  ]
}
```

**검증 항목**:
- ✅ 해당 부서 통계만 조회
- ✅ 멤버 목록 포함

---

## 🎯 테스트 체크리스트

### 상담사 생성
- [x] 필수 필드 검증 (loginId, password, name, email)
- [x] 로그인 아이디 중복 체크
- [x] 비밀번호 복잡도 검증
- [x] 역할 자동 할당
- [x] 부서 할당

### 상담사 조회
- [x] 목록 조회 (페이징)
- [x] 단건 조회
- [x] 상태별 필터링 (ACTIVE, SUSPENDED, RETIRED)
- [x] 부서별 필터링
- [x] 역할별 필터링

### 상담사 수정
- [x] 기본 정보 수정 (이름, 이메일, 전화번호)
- [x] 부서 이동
- [x] 역할 추가/제거
- [x] 역할 일괄 지정

### 비밀번호 관리
- [x] 초기화 (ADMIN)
- [x] 변경 (본인)
- [x] 현재 비밀번호 검증
- [x] 비밀번호 확인 검증
- [x] passwordMustChange 플래그

### 상태 관리
- [x] 정지 (SUSPENDED)
- [x] 활성화 (ACTIVE)
- [x] 퇴사 처리 (RETIRED)
- [x] 자동 삭제 예약

### 통계
- [x] 전체 통계
- [x] 역할별 집계
- [x] 부서별 집계
- [x] 조직별 통계

---

## 📊 최종 상담사 현황 (모든 테스트 완료 후)

### 활성 상담사 (ACTIVE)
| loginId | name | dept | roles |
|---------|------|------|-------|
| admin | 시스템관리자 | 넥스프론 | ADMIN |
| dev.lead | 김팀장 | 백엔드팀 | TEAM_LEAD |
| dev.member | 이시니어 | 프론트엔드팀 | TEAM_LEAD |
| frontend.lead | 최팀장 | 프론트엔드팀 | TEAM_LEAD |

### 퇴사 처리 (RETIRED)
| loginId | name | retiredAt |
|---------|------|-----------|
| backend.dev | 박개발 | 2026-02-08 |

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
**가능한 작업**: 조회 및 생성 가능
- ✅ 조회 (Scenario 1-3, 13-14)
- ✅ 생성 (Scenario 4)
- ❌ 수정, 삭제

**특징**:
- X-User-Id 헤더 **불필요**
- tenantId만 있으면 조회/생성 가능
- 권한 검증 없음

**테스트 순서**:
```
1. Scenario 1: 목록 조회 (tenantId 필수) ✅
2. Scenario 2: 단건 조회 ✅
3. Scenario 3: 중복 체크 ✅
4. Scenario 4: 상담사 생성 (tenantId 필수) ✅
5. Scenario 13-14: 통계 조회 ✅
```

---

### ADMIN (10000000-0000-0000-0000-000000000001)
**가능한 작업**: 전체 권한
- ✅ 조회 (Scenario 1-3, 13-14)
- ✅ 생성 (Scenario 4)
- ✅ 수정 (Scenario 5)
- ✅ 부서 이동 (Scenario 6)
- ✅ 비밀번호 관리 (Scenario 7-8)
- ✅ 역할 관리 (Scenario 9)
- ✅ 상태 관리 (Scenario 10-12)

**특징**:
- 조회 API: X-User-Id **불필요**
- 생성/수정/삭제 API: X-User-Id **필요**

**테스트 순서**:
```
1. Scenario 1-3: 조회 및 중복 체크 (헤더 불필요) ✅
2. Scenario 4: 상담사 생성 (X-User-Id 필요) ✅
3. Scenario 5: 정보 수정 (X-User-Id 필요) ✅
4. Scenario 6: 부서 이동 (X-User-Id 필요) ✅
5. Scenario 7-8: 비밀번호 관리 (X-User-Id 필요) ✅
6. Scenario 9: 역할 관리 (X-User-Id 필요) ✅
7. Scenario 10-12: 상태 관리 (X-User-Id 필요) ✅
8. Scenario 13-14: 통계 조회 (헤더 불필요) ✅
```

---

### TEAM_LEAD (10000000-0000-0000-0000-000000000002)
**가능한 작업**: 조회 및 생성 가능
- ✅ 조회 (Scenario 1-3, 13-14)
- ✅ 생성 (Scenario 4)
- ❌ 수정, 삭제

**특징**:
- ADMIN과 동일하게 조회/생성 가능
- X-User-Id 헤더 **불필요**

**테스트 순서**:
```
1. Scenario 1-2: 조회 (헤더 불필요) ✅
2. Scenario 4: 상담사 생성 (헤더 불필요) ✅
3. Scenario 5-3: 수정 시도 (X-User-Id로 시도, 403 Forbidden) ❌
4. Scenario 13-14: 통계 조회 (헤더 불필요) ✅
```

---

### MEMBER (10000000-0000-0000-0000-000000000003)
**가능한 작업**: 조회, 생성 및 본인 비밀번호 변경
- ✅ 조회 (Scenario 1-3, 13-14)
- ✅ 생성 (Scenario 4)
- ✅ 본인 비밀번호 변경 (Scenario 8) **⚠️ X-User-Id 필수**
- ❌ 수정, 삭제

**특징**:
- 조회/생성: X-User-Id **불필요**
- 본인 비밀번호 변경: X-User-Id **필수** (보안상 필수)

**테스트 순서**:
```
1. Scenario 1-2: 조회 (헤더 불필요) ✅
2. Scenario 4: 상담사 생성 (헤더 불필요) ✅
3. Scenario 8: 본인 비밀번호 변경 (X-User-Id 필수) ⚠️ ✅
4. Scenario 5: 수정 시도 (X-User-Id로 시도, 403 Forbidden) ❌
```

---

## 📋 X-User-Id 헤더 사용법

### ⚠️ 중요: X-User-Id 헤더 필요 여부

**조회/생성 API (헤더 불필요)**:
- ❌ Scenario 1: 목록 조회
- ❌ Scenario 2: 단건 조회
- ❌ Scenario 3: 중복 체크
- ❌ Scenario 4: 상담사 생성
- ❌ Scenario 13: 통계 조회
- ❌ Scenario 14: 조직별 통계

**수정/삭제 API (헤더 필요)**:
- ✅ Scenario 5: 수정
- ✅ Scenario 6: 부서 이동
- ✅ Scenario 7: 비밀번호 초기화 (ADMIN만)
- ✅ Scenario 8: 비밀번호 변경 (본인만)
- ✅ Scenario 9: 역할 관리
- ✅ Scenario 10: 정지
- ✅ Scenario 11: 활성화
- ✅ Scenario 12: 퇴사 처리

---

### Swagger UI에서
**조회/생성 API**: 헤더 추가 불필요 (그냥 실행)

**수정/삭제 API**: 
1. **Authorize 버튼** 클릭 또는
2. **각 API 실행 시 Parameters 섹션에 헤더 추가**

### cURL에서
**조회/생성**:
```bash
# 조회
curl -X GET "http://localhost:8080/api/v1/agents?tenantId=default-tenant"

# 생성
curl -X POST "http://localhost:8080/api/v1/agents" \
  -H "Content-Type: application/json" \
  -d '{"tenantId": "default-tenant", "loginId": "new.user", "name": "홍길동", "organizationId": "00000000-0000-0000-0000-000000000004", "roles": ["MEMBER"]}'
```

**수정/삭제**:
```bash
curl -X PATCH "http://localhost:8080/api/v1/agents/{agentId}" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000001" \
  -H "Content-Type: application/json" \
  -d '{"name": "홍길동수정"}'
```

### Postman에서
**조회/생성**: 헤더 불필요

**수정/삭제**:
```
Headers 탭:
Key: X-User-Id
Value: 10000000-0000-0000-0000-000000000001
```

---

## ✅ 성공 기준

- 모든 API가 예상된 상태 코드 반환
- 권한 검증이 정확하게 작동
- 상태 전이가 올바름 (ACTIVE ↔ SUSPENDED ↔ RETIRED)
- 비밀번호 관리가 안전하게 작동
- 역할 관리가 정확함
- 부서 이동이 정상 작동
- 통계가 정확함

🎉 **Happy Testing!**


