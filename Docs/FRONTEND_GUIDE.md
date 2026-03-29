# Identity Modulith - Frontend Development Guide

> **대상**: 이 시스템의 프론트엔드를 개발하는 AI Agent 및 개발자
> **최종 업데이트**: 2026-03-12
> **백엔드 서버**: `http://localhost:8080`
> **프론트엔드 서버**: `http://localhost:3000`

---

## 1. 시스템 아키텍처 개요

### 1.1 전체 구조

```
┌─────────────────────────────────────────────────────────┐
│           Frontend (localhost:3000)                      │
│  React / Vue / Next.js 등                                │
│  - JSESSIONID 쿠키 자동 전송 (withCredentials: true)     │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTP (CORS 허용)
┌──────────────────────────▼──────────────────────────────┐
│                 identity-modulith (Spring Boot :8080)    │
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
프론트엔드(:3000)          백엔드(:8080)              Keycloak
    │                          │                          │
    │ 1. GET /api/me/status     │                          │
    │─────────────────────────►│                          │
    │◄── { isAuthenticated: false, loginUrl: "/saml2/authenticate/keycloak" }
    │                          │                          │
    │ 2. 브라우저를 loginUrl로 이동 (window.location.href)  │
    │─────────────────────────►│                          │
    │◄── 302 redirect ────────────────────────────────────►
    │                          │                          │
    │ 3. Keycloak 로그인 폼                                 │
    │◄─────────────────────────────────────────────────────│
    │                          │                          │
    │ 4. 사용자 아이디/비밀번호 입력                         │
    │──────────────────────────────────────────────────────►
    │                          │                          │
    │ 5. SAML Assertion POST /login/saml2/sso/keycloak     │
    │                  ◄────────────────────────────────────│
    │                          │                          │
    │                          │ 6. DB Agent 매핑          │
    │                          │ 7. RBAC 권한 로드         │
    │                          │ 8. SecurityContext 설정   │
    │                          │                          │
    │ 9. 302 redirect → http://localhost:3000             │
    │◄─────────────────────────│                          │
    │                          │                          │
    │ 10. JSESSIONID 쿠키 자동 설정 (httpOnly, SameSite)   │
    │                          │                          │
    │ 이후 모든 API 요청에 JSESSIONID 쿠키 자동 포함        │
```

### 2.2 핵심 포인트

1. **로그인 진입점**: `GET /saml2/authenticate/keycloak` → 브라우저가 Keycloak으로 리디렉션
2. **인증 성공 후**: `http://localhost:3000` 으로 리디렉션 (백엔드 `app.frontend.login-success-url` 설정)
3. **세션**: 로그인 성공 후 `JSESSIONID` 쿠키가 자동 설정됨 (httpOnly)
4. **tenantId**: 백엔드가 SecurityContext에서 자동 추출 → **프론트엔드에서 tenantId 전송 불필요**
5. **userId**: 백엔드가 SecurityContext에서 자동 추출 → **프론트엔드에서 userId 헤더 전송 불필요**
6. **Keycloak 사용자 ↔ DB Agent 매핑**: Keycloak의 `username` 속성 = DB `user_agents.login_id`

### 2.3 에러 리디렉션

로그인 실패 시 프론트엔드로 쿼리 파라미터와 함께 리디렉션됩니다:

| 상황 | 리디렉션 URL |
|------|-------------|
| DB에 Agent 미등록 | `http://localhost:3000?error=not_registered` |
| Agent 비활성 상태 | `http://localhost:3000?error=inactive` |
| SAML 인증 자체 실패 | `http://localhost:3000?error=saml_failed` |

프론트엔드에서 URL 파라미터를 확인하여 적절한 에러 메시지를 표시하세요.

```javascript
// 예시: 로그인 후 에러 파라미터 처리
const params = new URLSearchParams(window.location.search);
const error = params.get('error');
if (error === 'not_registered') {
  showError('등록되지 않은 사용자입니다. 관리자에게 문의하세요.');
} else if (error === 'inactive') {
  showError('비활성화된 계정입니다. 관리자에게 문의하세요.');
}
```

### 2.4 로그아웃

| 방식 | 엔드포인트 | 결과 |
|------|-----------|------|
| 로컬 로그아웃 | `POST /logout` | 서버 세션 무효화 → `http://localhost:3000/login` 리디렉션 |
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
  "timestamp": "2026-03-12T12:00:00",
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
앱 시작 (localhost:3000)
  │
  ▼
URL 파라미터 확인 (?error=...)
  │  error 있음 → 에러 메시지 표시
  │
  ▼
GET /api/me/status  ← 항상 200 반환 (인증 여부와 무관)
  │
  ├─ isAuthenticated: false
  │     │
  │     ▼
  │  loginUrl로 이동: window.location.href = "/saml2/authenticate/keycloak"
  │  (브라우저가 백엔드 → Keycloak으로 리디렉션 처리)
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
// axios 전역 설정 (필수!)
axios.defaults.baseURL = 'http://localhost:8080';
axios.defaults.withCredentials = true;  // JSESSIONID 쿠키 자동 전송

// fetch 사용 시
fetch('http://localhost:8080/api/...', {
  credentials: 'include',  // 쿠키 자동 전송 필수
});
```

> ⚠️ **주의**: `withCredentials: true` 또는 `credentials: 'include'` 설정 없이는 JSESSIONID 쿠키가 전송되지 않아 모든 API가 401을 반환합니다.

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
- 불일치 시 로그인은 성공해도 `?error=not_registered`와 함께 프론트엔드로 리디렉션됩니다.
- 새로운 사용자는 먼저 `/api/v1/agents` POST로 Agent를 생성한 후 Keycloak 계정을 만들어야 합니다.

---

## 8. 현재 서버 정보

| 항목 | 값 |
|------|----|
| 백엔드 서버 | `http://localhost:8080` |
| 프론트엔드 서버 | `http://localhost:3000` |
| Keycloak | `http://1.224.162.188:51446` |
| Keycloak Realm | `identity-system` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

---

## 9. 백엔드 URL 설정 변경 방법

운영 환경이나 프론트엔드 URL이 변경될 경우 `application.yml`의 `app.frontend` 섹션을 수정하거나 환경변수로 오버라이드합니다.

```yaml
app:
  frontend:
    url: http://localhost:3000             # 프론트엔드 서버 URL
    login-success-url: http://localhost:3000      # 로그인 성공 후 리디렉션
    logout-success-url: http://localhost:3000/login  # 로그아웃 후 리디렉션
```

환경변수로 오버라이드:
```bash
APP_FRONTEND_URL=https://your-domain.com
APP_FRONTEND_LOGIN-SUCCESS-URL=https://your-domain.com
APP_FRONTEND_LOGOUT-SUCCESS-URL=https://your-domain.com/login
```

---

## 10. 향후 고도화 계획

### 10.1 대용량 트래픽 대응 로드맵

현재 구조는 단일 인스턴스 기준으로 동작합니다. 아래 순서로 확장할 계획입니다.

#### Phase 1: Redis 세션 클러스터링 (Scale-out 기반 마련)

```
현재: JSESSIONID → JVM 메모리 내 세션 저장 (단일 인스턴스만 가능)
개선: JSESSIONID → Redis 세션 저장 (다중 인스턴스 Scale-out 가능)
```

```yaml
# 추가 예정 의존성
implementation 'org.springframework.session:spring-session-data-redis'
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

효과: 로드밸런서 뒤에 다수의 백엔드 인스턴스 운영 가능

#### Phase 2: RBAC 권한 Redis 캐시

```
현재: 매 요청마다 DB에서 권한 조회 (getEffectivePermissions)
개선: Redis @Cacheable로 캐싱 (TTL: 5분)
```

```java
// 적용 예정
@Cacheable(value = "agent-permissions", key = "#agentId")
public Set<String> getEffectivePermissions(String agentId) { ... }
```

효과: RBAC 권한 조회 쿼리 90% 이상 감소

#### Phase 3: 비동기 이벤트 처리

현재 Spring ApplicationEvent 기반으로 이벤트 발행이 준비되어 있습니다 (TODO 상태).
Kafka 또는 RabbitMQ 연동 시 모듈 간 비동기 통신으로 전환하여 처리량을 향상시킬 수 있습니다.

### 10.2 JIT Provisioning (자동 사용자 등록)

현재: 관리자가 사전에 DB에 Agent를 생성해야 함 (2단계)

계획: Keycloak 최초 로그인 시 DB에 Agent가 없으면 자동 생성

```
Keycloak 로그인 성공
  │
  └─ DB에 login_id 없음?
        │
        ▼
     Agent 자동 생성 (기본 역할: AGENT, 상태: ACTIVE)
     → 관리자 알림 이벤트 발행
        │
        ▼
     정상 로그인 진행
```

이 방식으로 "회원가입" 대신 Keycloak 계정 생성만으로 시스템 접근이 가능해집니다.
