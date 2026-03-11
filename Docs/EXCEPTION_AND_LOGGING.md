# 예외 및 로깅 구조 설계

> **최종 업데이트**: 2026-03-11  
> **상태**: ✅ 구현 완료

---

## 📋 목차
1. [배경 및 문제 인식](#1-배경-및-문제-인식)
2. [예외 처리 계층 구조](#2-예외-처리-계층-구조)
3. [예외 클래스 계층도](#3-예외-클래스-계층도)
4. [공통 에러 응답 포맷](#4-공통-에러-응답-포맷)
5. [모듈별 에러 코드](#5-모듈별-에러-코드)
6. [로그 레벨 규칙](#6-로그-레벨-규칙)
7. [케이스별 처리 흐름](#7-케이스별-처리-흐름)
8. [구현 확인 체크리스트](#8-구현-확인-체크리스트)

---

## 1. 배경 및 문제 인식

### 문제
- 예외 발생 시 모듈마다 응답 형식이 달라 클라이언트 처리 어려움
- 로그에서 어느 모듈/계층에서 발생한 오류인지 파악 불가
- 인증 실패/권한 누락/비즈니스 규칙 위반이 동일한 방식으로 처리됨

### 해결 방향
- **공통 에러 응답 포맷** `ApiErrorResponse` 통일
- **모듈 전용 ExceptionHandler** 분리 (`@RestControllerAdvice(basePackages=...)`)
- **로그 레벨 규칙** 명문화 (INFO / WARN / ERROR)

---

## 2. 예외 처리 계층 구조

예외는 **두 단계**로 처리됩니다.

```
HTTP 요청
    │
    ▼
Controller
    │ 예외 발생
    ▼
┌──────────────────────────────────────────────────────┐
│  모듈 전용 ExceptionHandler (basePackages 지정)       │
│                                                      │
│  GlobalExceptionHandler     (user 패키지)            │
│  RbacExceptionHandler       (rbac 패키지)            │
│  OrganizationExceptionHandler (organization 패키지)  │
│                                                      │
│  처리 대상: 각 모듈의 비즈니스 예외                     │
│  (BusinessException, RbacException, OrgException)   │
└──────────────────────────────────────────────────────┘
    │ 위에서 처리 안 된 예외
    ▼
┌──────────────────────────────────────────────────────┐
│  CommonExceptionHandler (전역 공통)                   │
│                                                      │
│  처리 대상:                                           │
│  - UnauthorizedException (401)                      │
│  - MethodArgumentNotValidException (400)            │
│  - DataIntegrityViolationException (409)            │
│  - NoResourceFoundException (404)                   │
│  - Exception (500 fallback)                         │
└──────────────────────────────────────────────────────┘
    │
    ▼
ApiErrorResponse (통일된 JSON 응답)
```

### 파일 위치

| 핸들러 | 위치 | 담당 범위 |
|--------|------|---------|
| `CommonExceptionHandler` | `common/exception/` | 전역 공통 예외 |
| `GlobalExceptionHandler` | `user/presentation/` | User 모듈 비즈니스 예외 |
| `RbacExceptionHandler` | `rbac/presentation/` | RBAC 모듈 비즈니스 예외 |
| `OrganizationExceptionHandler` | `organization/presentation/` | Organization 모듈 비즈니스 예외 |

---

## 3. 예외 클래스 계층도

```
RuntimeException
    │
    ├── BusinessException             (User 모듈)
    │       └── ErrorCode (enum)
    │           ├── AGENT_NOT_FOUND
    │           ├── DUPLICATE_USERNAME
    │           ├── INVALID_STATUS_TRANSITION
    │           ├── PASSWORD_MISMATCH
    │           └── ...
    │
    ├── RbacException                 (RBAC 모듈)
    │       └── RbacErrorCode (enum)
    │           ├── ROLE_NOT_FOUND
    │           ├── PERMISSION_NOT_FOUND
    │           ├── INSUFFICIENT_PERMISSION
    │           └── ...
    │
    ├── OrganizationException         (Organization 모듈)
    │       └── OrganizationErrorCode (enum)
    │           ├── DEPARTMENT_NOT_FOUND
    │           ├── CIRCULAR_REFERENCE
    │           └── ...
    │
    └── UnauthorizedException         (common/security)
            └── 미인증 요청 (401)
```

**공통 구조** — 모든 모듈 예외가 동일한 패턴을 따릅니다:

```java
// ErrorCode enum — HttpStatus + 코드 + 메시지를 함께 보유
public enum ErrorCode {
    AGENT_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "상담사를 찾을 수 없습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "A002", "이미 사용 중인 아이디입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

// BusinessException — ErrorCode를 래핑
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
}
```

---

## 4. 공통 에러 응답 포맷

모든 모듈에서 **동일한 JSON 구조**로 에러를 반환합니다.

```java
// ApiErrorResponse.java (루트 패키지)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private final LocalDateTime timestamp;
    private final int status;
    private final String code;
    private final String message;
}
```

**응답 예시:**

```json
// 400 Bad Request
{
  "timestamp": "2026-03-11T10:30:00",
  "status": 400,
  "code": "A004",
  "message": "ACTIVE 상태만 정지할 수 있습니다."
}

// 401 Unauthorized
{
  "timestamp": "2026-03-11T10:30:00",
  "status": 401,
  "code": "UNAUTHORIZED",
  "message": "인증이 필요합니다."
}

// 403 Forbidden
{
  "timestamp": "2026-03-11T10:30:00",
  "status": 403,
  "code": "INSUFFICIENT_PERMISSION",
  "message": "권한이 부족합니다."
}

// 409 Conflict
{
  "timestamp": "2026-03-11T10:30:00",
  "status": 409,
  "code": "A002",
  "message": "이미 사용 중인 아이디입니다."
}

// 500 Internal Server Error
{
  "timestamp": "2026-03-11T10:30:00",
  "status": 500,
  "code": "INTERNAL_ERROR",
  "message": "서버 내부 오류가 발생했습니다"
}
```

---

## 5. 모듈별 에러 코드

### User 모듈 (`ErrorCode.java`)

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| `C001` | 400 | 잘못된 입력값 |
| `C003` | 500 | 서버 내부 오류 |
| `C005` | 404 | 리소스 없음 |
| `A001` | 404 | 상담사 없음 |
| `A002` | 409 | 로그인 ID 중복 |
| `A003` | 400 | 이미 퇴사한 상담사 |
| `A004` | 400 | 잘못된 상태 전이 |
| `A005` | 400 | 비즈니스 규칙 위반 |
| `A006` | 404 | 부서 없음 |
| `P001` | 400 | 비밀번호 불일치 |
| `P002` | 400 | 비밀번호 확인 불일치 |
| `P003` | 400 | 현재 비밀번호와 동일 |

### RBAC 모듈 (`RbacException.RbacErrorCode`)

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| `ROLE_NOT_FOUND` | 404 | 역할 없음 |
| `ROLE_ALREADY_EXISTS` | 409 | 역할 중복 |
| `ROLE_NOT_ACTIVE` | 400 | 비활성 역할 |
| `PERMISSION_NOT_FOUND` | 404 | 권한 없음 |
| `PERMISSION_ALREADY_EXISTS` | 409 | 권한 중복 |
| `PERMISSION_ALREADY_ASSIGNED` | 409 | 이미 할당된 권한 |
| `ROLE_HAS_USERS` | 400 | 사용자 있는 역할 삭제 불가 |
| `AGENT_RETIRED` | 422 | 퇴사 상담사에게 역할 할당 불가 |
| `INSUFFICIENT_PERMISSION` | 403 | 권한 부족 |

### Organization 모듈 (`OrganizationException.OrganizationErrorCode`)

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| `DEPARTMENT_NOT_FOUND` | 404 | 부서 없음 |
| `DUPLICATE_DEPT_CODE` | 409 | 부서 코드 중복 |
| `CIRCULAR_REFERENCE` | 400 | 순환 참조 |
| `HAS_CHILDREN` | 400 | 하위 부서 있어 삭제 불가 |
| `HAS_MEMBERS` | 400 | 소속 직원 있어 삭제 불가 |

---

## 6. 로그 레벨 규칙

모든 ExceptionHandler에서 **동일한 기준**으로 로그 레벨을 결정합니다.

```
HTTP 상태 코드         로그 레벨    이유
─────────────────────────────────────────────────────────────
400 Bad Request    →  INFO       클라이언트 실수 (운영자가 즉시 대응 불필요)
404 Not Found      →  INFO       클라이언트 실수
401 Unauthorized   →  WARN       보안 이벤트 (추적 필요)
403 Forbidden      →  WARN       보안 이벤트 (권한 위반 추적 필요)
409 Conflict       →  WARN       데이터 무결성 이슈 (추적 필요)
422 Unprocessable  →  INFO       비즈니스 규칙 위반
500 Server Error   →  ERROR      즉각 대응 필요 (스택 트레이스 포함)
```

**구현 예시** (세 핸들러 모두 동일한 패턴):

```java
@ExceptionHandler(RbacException.class)
public ResponseEntity<ApiErrorResponse> handleRbacException(RbacException e) {
    RbacException.RbacErrorCode errorCode = e.getErrorCode();
    HttpStatus status = errorCode.getHttpStatus();

    if (status == HttpStatus.FORBIDDEN || status == HttpStatus.CONFLICT) {
        log.warn("[RBAC] code={}, status={}, message={}",
                errorCode.getCode(), status.value(), e.getMessage());
    } else if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
        log.error("[RBAC] code={}, status={}, message={}",
                errorCode.getCode(), status.value(), e.getMessage(), e);  // 스택 트레이스 포함
    } else {
        log.info("[RBAC] code={}, status={}, message={}",
                errorCode.getCode(), status.value(), e.getMessage());
    }

    return ResponseEntity.status(status)
            .body(ApiErrorResponse.of(status.value(), errorCode.getCode(), e.getMessage()));
}
```

**로그 포맷 패턴:**

| 위치 태그 | 예시 |
|----------|------|
| `[Common][Auth]` | 공통 인증 예외 |
| `[Common][Validation]` | 공통 유효성 검사 |
| `[Common][DB]` | 공통 DB 무결성 |
| `[Common][Unexpected]` | 공통 fallback |
| `[User]` | User 모듈 비즈니스 예외 |
| `[RBAC]` | RBAC 모듈 비즈니스 예외 |
| `[Org]` | Organization 모듈 비즈니스 예외 |
| `[Org][DB]` | Organization 모듈 DB 예외 |
| `[SAML]` | SAML 인증 관련 |
| `[Keycloak]` | Keycloak 연동 |

---

## 7. 케이스별 처리 흐름

### Case 1: 상담사 없음 (404)
```
AgentController.getAgent(agentId)
    → AgentService.findAgentById()
        → agentRepository.findById() → empty
        → throw new BusinessException(ErrorCode.AGENT_NOT_FOUND)
    → GlobalExceptionHandler.handleBusinessException()
        → log.info("[User] code=A001, status=404, message=상담사를 찾을 수 없습니다.")
        → return 404 { "code": "A001", "message": "상담사를 찾을 수 없습니다." }
```

### Case 2: 잘못된 상태 전이 (400)
```
AgentController.suspendAgent(agentId)
    → AgentService.suspendAgent()
        → agent.suspend()  ← 도메인 모델 내부에서 검증
            → if (status != ACTIVE) throw BusinessException(INVALID_STATUS_TRANSITION)
    → GlobalExceptionHandler.handleBusinessException()
        → log.info("[User] code=A004, status=400, message=...")
        → return 400 { "code": "A004", ... }
```

### Case 3: 권한 부족 (403)
```
RbacController.deleteRole(roleId)
    → RbacManagementService.deleteRole()
        → 권한 검증 실패
        → throw new RbacException(RbacErrorCode.INSUFFICIENT_PERMISSION)
    → RbacExceptionHandler.handleRbacException()
        → log.warn("[RBAC] code=INSUFFICIENT_PERMISSION, status=403, ...")
        → return 403 { "code": "INSUFFICIENT_PERMISSION", ... }
```

### Case 4: Bean Validation 실패 (400)
```
AgentController.createAgent(@Valid @RequestBody request)
    → @Valid 검증 실패
    → MethodArgumentNotValidException 발생
    → CommonExceptionHandler.handleValidationException()
        → log.info("[Common][Validation] loginId: 4자 이상이어야 합니다")
        → return 400 { "code": "INVALID_INPUT_VALUE", "message": "loginId: 4자 이상이어야 합니다" }
```

### Case 5: 예상치 못한 서버 오류 (500)
```
어떤 Controller
    → 예상치 못한 RuntimeException 발생
    → CommonExceptionHandler.handleGeneralException()
        → log.error("[Common][Unexpected] NullPointerException - ...", e)  // 스택 트레이스 포함
        → return 500 { "code": "INTERNAL_ERROR", "message": "서버 내부 오류가 발생했습니다" }
```

---

## 8. 구현 확인 체크리스트

| 항목 | 상태 | 위치 |
|------|:----:|------|
| 공통 에러 응답 포맷 (`ApiErrorResponse`) | ✅ | `com.identitymodulith.ApiErrorResponse` |
| User 모듈 비즈니스 예외 처리 | ✅ | `GlobalExceptionHandler` |
| RBAC 모듈 비즈니스 예외 처리 | ✅ | `RbacExceptionHandler` |
| Organization 모듈 비즈니스 예외 처리 | ✅ | `OrganizationExceptionHandler` |
| 공통 인증/유효성/fallback 처리 | ✅ | `CommonExceptionHandler` |
| 에러 코드에 `HttpStatus` 내장 | ✅ | `ErrorCode`, `RbacException.RbacErrorCode`, `OrganizationException.OrganizationErrorCode` |
| HTTP 상태 코드별 로그 레벨 구분 | ✅ | 3개 모듈 핸들러 모두 동일 패턴 |
| 500 오류 시 스택 트레이스 로그 | ✅ | `log.error(..., e)` |
| 로그 위치 태그 (`[User]`, `[RBAC]`, `[Org]`) | ✅ | 모든 핸들러 |
| Organization DB 무결성 특수 처리 | ✅ | `OrganizationExceptionHandler` (부서코드 중복 별도 처리) |
| SAML 인증 이벤트 로그 (`[SAML]`) | ✅ | `Saml2AuthenticationSuccessHandler`, `Saml2AuthenticationFailureHandler` |
| 모든 핸들러 `@Slf4j` 적용 | ✅ | v4.0.0에서 전체 수정 완료 |
| 감사 로그 (audit_log 테이블 저장) | ❌ | 미구현 — 현재는 콘솔 로그만 (향후 과제)

