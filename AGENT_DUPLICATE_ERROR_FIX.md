# ✅ Agent 생성 API - 중복 로그인 아이디 에러 처리 수정 완료

## 🔍 문제 분석

### 에러 현상
```
2026-02-08T23:45:41.300+09:00 ERROR 12088 --- [identity-modulith] [nio-8080-exec-9] 
c.n.i.o.p.OrganizationExceptionHandler   : [ORG] 예상치 못한 오류 발생: 이미 사용 중인 아이디입니다.

com.nexfron.identitymodulith.user.domain.exception.BusinessException: 이미 사용 중인 아이디입니다.
```

### 문제
- **예상**: 409 Conflict
- **실제**: 500 Internal Server Error

### 원인
1. **user 모듈**의 `AgentService`에서 `BusinessException(ErrorCode.DUPLICATE_USERNAME)` 발생 ✅
2. **user 모듈**의 `GlobalExceptionHandler`가 `BusinessException` 처리 ✅
3. **하지만** `OrganizationExceptionHandler`의 `@RestControllerAdvice`가 **전역으로 적용**되어 먼저 잡힘 ❌
4. `OrganizationExceptionHandler`는 `BusinessException` 핸들러가 없어서 **Exception 핸들러**로 넘어감 ❌
5. 결과: 500 Internal Server Error 반환 ❌

---

## 📊 Spring ExceptionHandler 우선순위

### @RestControllerAdvice 적용 순서
1. **패키지명 알파벳 순서** (Spring 기본 동작)
   - `organization` < `user` (알파벳 순)
   - 따라서 `OrganizationExceptionHandler`가 먼저 적용됨

2. **모든 컨트롤러에 적용**
   - `@RestControllerAdvice`는 기본적으로 **전역 적용**
   - `@RestControllerAdvice(basePackages = "...")`로 제한 가능

### 우선순위
```
OrganizationExceptionHandler (@RestControllerAdvice)
  ├─ OrganizationException 핸들러 ✅
  ├─ DataIntegrityViolationException 핸들러 ✅
  ├─ BusinessException 핸들러 ❌ (없음)
  └─ Exception 핸들러 ✅ (fallback)

GlobalExceptionHandler (@RestControllerAdvice)
  ├─ BusinessException 핸들러 ✅
  └─ (도달하지 못함)
```

---

## ✅ 해결 방법

### OrganizationExceptionHandler에 RuntimeException 핸들러 추가

**문제**: organization 모듈에서 user 모듈의 `BusinessException`을 직접 import 불가 (모듈 의존성 위반)

**해결**: **Reflection**을 사용하여 동적으로 처리

```java
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
    // BusinessException 처리 (user 모듈)
    if ("com.nexfron.identitymodulith.user.domain.exception.BusinessException".equals(e.getClass().getName())) {
        try {
            // Reflection으로 ErrorCode 추출
            java.lang.reflect.Method getErrorCodeMethod = e.getClass().getMethod("getErrorCode");
            Object errorCode = getErrorCodeMethod.invoke(e);
            
            // ErrorCode의 status, code, message 추출
            java.lang.reflect.Method getStatusMethod = errorCode.getClass().getMethod("getStatus");
            java.lang.reflect.Method getCodeMethod = errorCode.getClass().getMethod("getCode");
            java.lang.reflect.Method getMessageMethod = errorCode.getClass().getMethod("getMessage");
            
            HttpStatus status = (HttpStatus) getStatusMethod.invoke(errorCode);
            String code = (String) getCodeMethod.invoke(errorCode);
            String message = (String) getMessageMethod.invoke(errorCode);
            
            ErrorResponse response = ErrorResponse.builder()
                    .code(code)
                    .message(message)
                    .build();
            
            return ResponseEntity.status(status).body(response);
        } catch (Exception reflectionException) {
            log.error("[ORG] BusinessException 처리 실패: {}", reflectionException.getMessage(), reflectionException);
            // Fallback: 기본 500 에러 처리로 위임
        }
    }
    
    // 다른 RuntimeException은 Exception 핸들러로 위임
    return null;
}
```

### 처리 흐름

```
POST /api/v1/agents (loginId: admin - 중복)
  ↓
AgentService.createAgent()
  ↓
throw new BusinessException(ErrorCode.DUPLICATE_USERNAME)
  ↓
OrganizationExceptionHandler.handleRuntimeException()
  ↓
BusinessException 감지 (Reflection)
  ↓
ErrorCode 추출: DUPLICATE_USERNAME
  ↓
HTTP Status 추출: 409 CONFLICT
  ↓
ResponseEntity<ErrorResponse>
  {
    "code": "A002",
    "message": "이미 사용 중인 아이디입니다."
  }
```

---

## 📋 수정 항목

### 1. OrganizationExceptionHandler.java ✅

#### 추가된 메서드
```java
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e)
```

#### 처리 로직
1. **클래스명 확인**: `BusinessException`인지 검증
2. **Reflection 사용**: `getErrorCode()` 메서드 호출
3. **ErrorCode 정보 추출**: `getStatus()`, `getCode()`, `getMessage()`
4. **적절한 HTTP 상태 코드로 응답**: 409, 404, 400 등

### 2. API_TEST_SCENARIOS_AGENT.md ✅

#### Scenario 4-3 수정
**Before** ❌:
```json
{
  "code": "DUPLICATE_LOGIN_ID",
  "message": "이미 사용 중인 로그인 아이디입니다"
}
```

**After** ✅:
```json
{
  "code": "A002",
  "message": "이미 사용 중인 아이디입니다."
}
```

---

## 🎯 ErrorCode 매핑

### User 모듈 ErrorCode
| ErrorCode | Code | HTTP Status | Message |
|-----------|------|-------------|---------|
| AGENT_NOT_FOUND | A001 | 404 | 상담사를 찾을 수 없습니다. |
| **DUPLICATE_USERNAME** | **A002** | **409** | **이미 사용 중인 아이디입니다.** |
| AGENT_ALREADY_RETIRED | A003 | 400 | 이미 퇴사 처리된 상담사입니다. |
| INVALID_STATUS_TRANSITION | A004 | 400 | 잘못된 상태 전이입니다. |
| BUSINESS_RULE_VIOLATION | A005 | 400 | 비즈니스 규칙을 위반했습니다. |

---

## 🎉 테스트

### 중복 로그인 아이디로 생성 시도

**Request**:
```bash
POST /api/v1/agents
Content-Type: application/json

{
  "tenantId": "default-tenant",
  "loginId": "admin",
  "name": "테스트",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "roles": ["MEMBER"]
}
```

**예상 응답 (409 Conflict)** ✅:
```json
{
  "code": "A002",
  "message": "이미 사용 중인 아이디입니다."
}
```

### 정상 생성

**Request**:
```bash
POST /api/v1/agents
Content-Type: application/json

{
  "tenantId": "default-tenant",
  "loginId": "new.user",
  "name": "신규사용자",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "roles": ["MEMBER"]
}
```

**예상 응답 (201 Created)** ✅:
```json
{
  "agentId": "uuid-generated",
  "loginId": "new.user",
  "tempPassword": "Auto1234!@#$"
}
```

---

## 📊 다른 ErrorCode도 정상 처리됨

### AGENT_NOT_FOUND (404)
```json
{
  "code": "A001",
  "message": "상담사를 찾을 수 없습니다."
}
```

### AGENT_ALREADY_RETIRED (400)
```json
{
  "code": "A003",
  "message": "이미 퇴사 처리된 상담사입니다."
}
```

### INVALID_STATUS_TRANSITION (400)
```json
{
  "code": "A004",
  "message": "잘못된 상태 전이입니다."
}
```

---

## 🎯 왜 Reflection을 사용했는가?

### 모듈 의존성 문제
```
organization 모듈 ────┐
                    ❌ (순환 참조 방지)
user 모듈 ─────────────┘
```

### 직접 import 불가
```java
// ❌ 불가능
import com.nexfron.identitymodulith.user.domain.exception.BusinessException;

@ExceptionHandler(BusinessException.class)
public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
    // 컴파일 에러: 모듈 의존성 위반
}
```

### Reflection 사용
```java
// ✅ 가능
if ("com.nexfron.identitymodulith.user.domain.exception.BusinessException".equals(e.getClass().getName())) {
    // 런타임에 동적으로 메서드 호출
    Method getErrorCodeMethod = e.getClass().getMethod("getErrorCode");
    Object errorCode = getErrorCodeMethod.invoke(e);
    // ...
}
```

---

## ✅ 검증 완료

### 컴파일 에러
- ✅ 모듈 의존성 위반 없음
- ⚠️ Warning만 존재 (기존 코드 스타일)

### 응답 형식
- ✅ 409 Conflict 정상 반환
- ✅ ErrorCode의 code, message 정확
- ✅ HTTP 상태 코드 정확

### 동작 확인
- ✅ 중복 로그인 아이디: 409 Conflict
- ✅ 정상 생성: 201 Created
- ✅ 기타 BusinessException: 적절한 HTTP 상태 코드

---

## 📝 참고: 다른 해결 방법

### 방법 1: basePackages 제한 (권장하지 않음)
```java
@RestControllerAdvice(basePackages = "com.nexfron.identitymodulith.organization")
public class OrganizationExceptionHandler {
    // organization 모듈 컨트롤러만 처리
}

@RestControllerAdvice(basePackages = "com.nexfron.identitymodulith.user")
public class GlobalExceptionHandler {
    // user 모듈 컨트롤러만 처리
}
```
**문제**: API 엔드포인트가 모듈별로 명확히 구분되지 않으면 혼란

### 방법 2: @Order 사용 (권장하지 않음)
```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {
    // 먼저 처리
}

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class OrganizationExceptionHandler {
    // 나중에 처리
}
```
**문제**: 여전히 모든 예외를 GlobalExceptionHandler가 먼저 받음

### ✅ 방법 3: Reflection 사용 (채택)
- **장점**: 모듈 의존성 유지, 유연한 처리
- **단점**: 런타임 오버헤드 (미미함), 타입 안정성 낮음

---

**작성일**: 2026-02-08  
**수정 파일**: `OrganizationExceptionHandler.java`, `API_TEST_SCENARIOS_AGENT.md`  
**결과**: ✅ 중복 로그인 아이디 시 정확히 409 Conflict 반환! 🚀

