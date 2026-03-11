# Identity Modulith - Frontend Development Guide

> **대상**: 이 시스템의 프론트엔드를 개발하는 AI Agent 및 개발자
> **최종 업데이트**: 2026-03-11
> **백엔드 서버**: `http://localhost:8080`

---

## 1. 시스템 아키텍처 개요

### 1.1 전체 구조

```
┌─────────────────────────────────────────────────────────┐
│                 identity-modulith (Spring Boot)          │
│                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌────────────────┐  │
│  │  user 모듈  │  │  rbac 모듈  │  │ organization   │  │
│  │             │  │             │  │    모듈         │  │
│  │ - Agent     │  │ - Role      │  │ - Department   │  │
│  │   (상담사)  │  │ - Permission│  │   (부서)       │  │
│  │ - Password  │  │ - AgentRole │  │ - OrgTree      │  │
│  └──────┬──────┘  └──────┬──────┘  └───────┬────────┘  │
│         │                │                 │            │
│  ┌──────▼─────────────────▼─────────────────▼────────┐  │
│  │              common (공통 인프라)                   │  │
│  │  - SAML2 Security Config                           │  │
│  │  - TenantContextHolder (멀티테넌시)                 │  │
│  │  - JwtUserContext (현재 사용자 정보)                │  │
│  │  - CommonExceptionHandler                          │  │
│  └────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
         │
         │ SAML 2.0 SSO
         ▼
┌──────────────────┐
│  Keycloak IdP    │
│  (외부 인증 서버) │
└──────────────────┘
```

### 1.2 기술 스택

| 항목 | 내용 |
|------|------|
| 프레임워크 | Spring Boot 3.x |
| 인증 | SAML 2.0 (Keycloak) |
| 세션 | Server-side Session (JSESSIONID 쿠키) |
| DB | PostgreSQL |
| API 문서 | Swagger UI (`/swagger-ui.html`) |

---

## 2. 인증 흐름 (SAML 2.0 SSO)

### 2.1 로그인 플로우

```
브라우저/앱                백엔드                    Keycloak
    │                       │                          │
    │ 1. GET /api/me/status  │                          │
    │──────────────────────►│                          │
    │◄── { isAuthenticated: false, loginUrl: "/saml2/authenticate/keycloak" }
    │                       │                          │
    │ 2. GET /saml2/authenticate/keycloak              │
    │──────────────────────►│                          │
    │◄── 302 redirect ───────────────────────────────►│
    │                       │                          │
    │ 3. Keycloak 로그인 폼 표시                        │
    │◄─────────────────────────────────────────────────│
    │                       │                          │
    │ 4. 사용자 아이디/비밀번호 입력                     │
    │─────────────────────────────────────────────────►│
    │                       │                          │
    │ 5. SAML Assertion POST /login/saml2/sso/keycloak  │
    │               ◄───────────────────────────────────│
    │                       │                          │
    │                       │ 6. DB에서 Agent 매핑 (loginId 기준)
    │                       │ 7. RBAC 권한 로드        │
    │                       │ 8. SecurityContext 설정  │
    │                       │                          │
    │ 9. 302 redirect to / ─│                          │
    │◄──────────────────────│                          │
    │                       │                          │
    │ 10. JSESSIONID 쿠키 자동 설정 (httpOnly)         │
    │                       │                          │
    │ 이후 모든 API 요청에 JSESSIONID 쿠키 자동 포함    │
```

### 2.2 핵심 포인트

1. **로그인 진입점**: `GET /saml2/authenticate/keycloak` → 브라우저가 Keycloak으로 리디렉션
2. **세션**: 로그인 성공 후 `JSESSIONID` 쿠키가 자동 설정됨 (httpOnly)
3. **tenantId**: 백엔드가 SecurityContext에서 자동 추출 → **프론트엔드에서 tenantId 전송 불필요**
4. **userId**: 백엔드가 SecurityContext에서 자동 추출 → **프론트엔드에서 userId 헤더 전송 불필요**
5. **Keycloak 사용자 ↔ DB Agent 매핑**: Keycloak의 `username` 속성 = DB `user_agents.login_id`

### 2.3 로그아웃

| 방식 | 엔드포인트 | 설명 |
|------|-----------|------|
| 로컬 로그아웃 | `POST /logout` | 서버 세션만 무효화 (Keycloak 세션 유지) |
| SAML 전체 로그아웃 | `POST /saml2/logout` | Keycloak까지 로그아웃 (SLO) |

> 일반적으로 SAML 전체 로그아웃(`/saml2/logout`)을 사용하는 것을 권장합니다.

---

## 3. 멀티테넌시 구조

### 3.1 개요

- 모든 데이터는 `tenant_id` 컬럼으로 격리됩니다.
- `tenant_id`는 SAML 로그인 시 Keycloak의 `tenant_id` Attribute에서 자동 추출됩니다.
- **프론트엔드에서 별도로 tenantId를 전송할 필요가 없습니다.**

### 3.2 현재 운영 중인 테넌트

| tenant_id | 설명 |
|-----------|------|
| `default-tenant` | 기본 테넌트 (개발/테스트용) |

---

## 4. 공통 에러 응답 형식

### 4.1 에러 응답 구조

모든 API는 에러 발생 시 아래 형식으로 응답합니다.

```json
{
  "timestamp": "2026-03-11T12:00:00",
  "status": 403,
  "code": "INSUFFICIENT_PERMISSION",
  "message": "관리자만 이 작업을 수행할 수 있습니다."
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `timestamp` | `string` (ISO 8601) | 에러 발생 시각 |
| `status` | `number` | HTTP 상태 코드 |
| `code` | `string` | 비즈니스 에러 코드 |
| `message` | `string` | 사람이 읽을 수 있는 에러 메시지 (한국어) |

### 4.2 주요 에러 코드

| HTTP | code | 상황 |
|------|------|------|
| 400 | `INVALID_INPUT_VALUE` | 요청 필드 형식/값 오류 |
| 400 | `SAME_AS_CURRENT_PASSWORD` | 새 비밀번호 = 현재 비밀번호 |
| 400 | `PASSWORD_MISMATCH` | 현재 비밀번호 불일치 |
| 400 | `INVALID_STATUS_TRANSITION` | 허용되지 않는 상태 전이 |
| 400 | `BUSINESS_RULE_VIOLATION` | 비즈니스 규칙 위반 |
| 401 | `UNAUTHORIZED` | 미인증 사용자 |
| 404 | `AGENT_NOT_FOUND` | 상담사를 찾을 수 없음 |
| 404 | `ORGANIZATION_NOT_FOUND` | 부서를 찾을 수 없음 |
| 404 | `ROLE_NOT_FOUND` | 역할을 찾을 수 없음 |
| 409 | `DUPLICATE_USERNAME` | 이미 존재하는 로그인 ID |
| 409 | `DATA_INTEGRITY_VIOLATION` | DB 무결성 제약 위반 |
| 409 | `DUPLICATE_DEPT_CODE` | 이미 존재하는 부서 코드 |
| 410 | `AGENT_ALREADY_RETIRED` | 이미 퇴사한 상담사 |

---

## 5. 앱 초기화 패턴 (권장)

```
앱 시작
  │
  ▼
GET /api/me/status  ← 항상 200 반환 (인증 여부와 무관)
  │
  ├─ isAuthenticated: false
  │     │
  │     ▼
  │  loginUrl로 리디렉션 ("/saml2/authenticate/keycloak")
  │
  └─ isAuthenticated: true
        │
        ▼
     GET /api/me  ← 상세 사용자 정보 (Agent 정보 + 역할 + 권한)
        │
        ▼
     passwordMustChange: true → 비밀번호 변경 화면 강제 이동
     passwordMustChange: false → 정상 앱 진입
```

---

## 6. 도메인 열거형 (Enum) 정의

### 6.1 AgentStatus (상담사 상태)

| 값 | 설명 |
|----|------|
| `ACTIVE` | 활성 (정상 근무) |
| `SUSPENDED` | 정지 (임시 접근 차단) |
| `RETIRED` | 퇴사 (복구 불가) |

### 6.2 DepartmentType (부서 타입)

| 값 | 설명 |
|----|------|
| `COMPANY` | 회사 (최상위) |
| `DIVISION` | 사업부 |
| `TEAM` | 팀 |
| `GROUP` | 그룹 |
| `CUSTOM` | 커스텀 (customTypeName 필드 필수) |

### 6.3 RoleType (역할 타입)

| 값 | 설명 |
|----|------|
| `POSITION` | 직급 역할 (1인 1개만 가능) |
| `CHANNEL` | 채널 역할 (여러 개 가능) |

### 6.4 Permission Code 패턴

권한 코드는 `domain:action` 형식입니다.

| 예시 코드 | 설명 |
|----------|------|
| `user:read` | 사용자 조회 |
| `user:write` | 사용자 생성/수정 |
| `user:delete` | 사용자 삭제 |
| `org:read` | 조직 조회 |
| `org:create` | 조직 생성 |
| `org:update` | 조직 수정 |
| `rbac:manage` | RBAC 관리 |

---

## 7. 프론트엔드 개발 시 주의사항

### 7.1 CORS & 쿠키 설정

```javascript
// axios 설정 예시
axios.defaults.withCredentials = true;  // 쿠키 자동 전송 필수

// fetch 설정 예시
fetch(url, {
  credentials: 'include',  // 쿠키 자동 전송 필수
});
```

> **주의**: `withCredentials: true` 설정 없이는 JSESSIONID 쿠키가 전송되지 않아 모든 API가 401을 반환합니다.

### 7.2 비밀번호 초기 변경 강제

상담사 생성 또는 비밀번호 초기화 후 최초 로그인 시 `passwordMustChange: true`가 반환됩니다.
이 경우 반드시 비밀번호 변경 화면으로 강제 이동해야 합니다.

```javascript
// GET /api/me 응답 처리
const { agent, roles } = await getMe();
if (agent.passwordMustChange) {
  router.push('/change-password');  // 강제 이동
}
```

### 7.3 공개 엔드포인트 (인증 불필요)

| 엔드포인트 | 설명 |
|-----------|------|
| `GET /api/me/status` | 로그인 상태 확인 |
| `GET /swagger-ui.html` | API 문서 |
| `GET /v3/api-docs/**` | OpenAPI 스펙 |
| `GET /saml2/**` | SAML 인증 흐름 |

그 외 모든 `/api/**` 엔드포인트는 인증이 필요합니다.

### 7.4 권한 체계

- **ADMIN**: 모든 작업 가능 (상담사 생성/수정/삭제, RBAC 관리, 부서 관리)
- **TEAM_LEAD**: 자신의 부서 + 하위 부서 조회, 소속 팀원 관리
- **MEMBER / AGENT**: 자신의 정보 조회 및 수정만 가능

### 7.5 SAML 인증 주의사항

- Keycloak 계정의 `username`이 DB의 `login_id`와 반드시 일치해야 합니다.
- 불일치 시 로그인은 성공해도 `401 UNAUTHORIZED`가 반환됩니다.
- 새로운 사용자는 먼저 `/api/v1/agents` POST로 Agent를 생성한 후 Keycloak 계정을 만들어야 합니다.

---

## 8. 현재 서버 정보

| 항목 | 값 |
|------|----|
| 백엔드 서버 | `http://localhost:8080` |
| Keycloak | `http://1.224.162.188:51446` |
| Keycloak Realm | `identity-system` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

