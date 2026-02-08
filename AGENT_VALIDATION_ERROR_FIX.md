# ✅ Agent 생성 API - Bean Validation 에러 처리 수정 완료

## 🔍 문제 분석

### 에러 현상
```
2026-02-08T23:52:04.723+09:00 ERROR 4840 --- [identity-modulith] [nio-8080-exec-7] 
c.n.i.o.p.OrganizationExceptionHandler   : [ORG] 예상치 못한 오류 발생: 
Validation failed for argument [0] ... Field error in object 'createAgentRequest' 
on field 'roles': rejected value [null] ... default message [역할은 최소 1개 이상이어야 합니다]

org.springframework.web.bind.MethodArgumentNotValidException: ...
```

### 문제
- **예상**: 400 Bad Request
- **실제**: 500 Internal Server Error

### 원인
1. **CreateAgentRequest**의 `@NotEmpty(message = "역할은 최소 1개 이상이어야 합니다")` 검증 실패 ✅
2. **MethodArgumentNotValidException** 발생 ✅
3. **OrganizationExceptionHandler**에 `MethodArgumentNotValidException` 핸들러가 없음 ❌
4. **Exception 핸들러**로 넘어가서 500 반환 ❌

---

## ✅ 해결 방법

### OrganizationExceptionHandler에 MethodArgumentNotValidException 핸들러 추가

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException e) {

    // 첫 번째 필드 에러 추출
    FieldError fieldError = e.getBindingResult().getFieldError();
    String message = fieldError != null ? fieldError.getDefaultMessage() : "입력값이 올바르지 않습니다";

    ErrorResponse response = ErrorResponse.builder()
            .code("INVALID_INPUT_VALUE")
            .message(message)
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
}
```

---

## 📊 처리 흐름

### Before ❌
```
POST /api/v1/agents (roles 누락)
  ↓
AgentController.createAgent()
  ↓
@Valid 검증 실패
  ↓
throw MethodArgumentNotValidException
  ↓
OrganizationExceptionHandler.handleGeneralException(Exception e)
  ↓
500 Internal Server Error
```

### After ✅
```
POST /api/v1/agents (roles 누락)
  ↓
AgentController.createAgent()
  ↓
@Valid 검증 실패
  ↓
throw MethodArgumentNotValidException
  ↓
OrganizationExceptionHandler.handleMethodArgumentNotValidException()
  ↓
FieldError 추출: roles
  ↓
DefaultMessage 추출: "역할은 최소 1개 이상이어야 합니다"
  ↓
ResponseEntity<ErrorResponse>
  {
    "code": "INVALID_INPUT_VALUE",
    "message": "역할은 최소 1개 이상이어야 합니다"
  }
  ↓
400 Bad Request ✅
```

---

## 📋 수정 항목

### 1. OrganizationExceptionHandler.java ✅

#### 추가된 import
```java
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
```

#### 추가된 메서드
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException e)
```

#### 처리 로직
1. **BindingResult에서 첫 번째 필드 에러 추출**
2. **DefaultMessage 추출**: `@NotEmpty(message = "...")` 메시지
3. **ErrorResponse 생성**: code = "INVALID_INPUT_VALUE", message = 추출된 메시지
4. **400 Bad Request 반환**

### 2. API_TEST_SCENARIOS_AGENT.md ✅

#### Scenario 4-4 검증 항목 추가
```markdown
**검증 항목**:
- ✅ HTTP 400 Bad Request 반환
- ✅ Bean Validation 메시지 정확
- ✅ roles 필드 누락 시 자동 검증
```

---

## 🎯 Bean Validation 검증 항목

### CreateAgentRequest 필드별 검증

| 필드 | 검증 어노테이션 | 에러 메시지 | HTTP Status |
|------|----------------|-------------|-------------|
| tenantId | `@NotBlank` | 테넌트 ID는 필수입니다 | 400 |
| loginId | `@NotBlank`<br/>`@Pattern` | 로그인 ID는 필수입니다<br/>4-20자, 영문/숫자/특수문자(_.-) | 400 |
| name | `@NotBlank` | 상담사 이름은 필수입니다 | 400 |
| organizationId | `@NotBlank` | 소속 조직 ID는 필수입니다 | 400 |
| **roles** | **@NotEmpty** | **역할은 최소 1개 이상이어야 합니다** | **400** |
| email | `@Email` (선택) | 유효한 이메일 형식이 아닙니다 | 400 |
| phone | (선택) | - | - |
| employeeId | (선택) | - | - |

---

## 🎉 테스트

### Scenario 4-4: roles 없이 생성 시도 ❌

**Request**:
```bash
POST /api/v1/agents
Content-Type: application/json

{
  "tenantId": "default-tenant",
  "loginId": "test.user",
  "name": "테스트",
  "organizationId": "00000000-0000-0000-0000-000000000004"
}
```

**예상 응답 (400 Bad Request)** ✅:
```json
{
  "code": "INVALID_INPUT_VALUE",
  "message": "역할은 최소 1개 이상이어야 합니다"
}
```

### Scenario 4-4-2: roles 빈 배열로 생성 시도 ❌

**Request**:
```bash
POST /api/v1/agents
Content-Type: application/json

{
  "tenantId": "default-tenant",
  "loginId": "test.user2",
  "name": "테스트2",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "roles": []
}
```

**예상 응답 (400 Bad Request)** ✅:
```json
{
  "code": "INVALID_INPUT_VALUE",
  "message": "역할은 최소 1개 이상이어야 합니다"
}
```

### Scenario 4-1: 정상 생성 ✅

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

## 📊 다른 Bean Validation 실패도 정상 처리됨

### tenantId 누락 (400)
**Request**:
```json
{
  "loginId": "test.user",
  "name": "테스트",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "roles": ["MEMBER"]
}
```

**응답 (400 Bad Request)**:
```json
{
  "code": "INVALID_INPUT_VALUE",
  "message": "테넌트 ID는 필수입니다"
}
```

### loginId 형식 오류 (400)
**Request**:
```json
{
  "tenantId": "default-tenant",
  "loginId": "a",
  "name": "테스트",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "roles": ["MEMBER"]
}
```

**응답 (400 Bad Request)**:
```json
{
  "code": "INVALID_INPUT_VALUE",
  "message": "로그인 ID는 4-20자, 영문/숫자/특수문자(_.-) 가능합니다"
}
```

### email 형식 오류 (400)
**Request**:
```json
{
  "tenantId": "default-tenant",
  "loginId": "test.user",
  "name": "테스트",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "roles": ["MEMBER"],
  "email": "invalid-email"
}
```

**응답 (400 Bad Request)**:
```json
{
  "code": "INVALID_INPUT_VALUE",
  "message": "유효한 이메일 형식이 아닙니다"
}
```

---

## 🎯 Exception Handler 우선순위 (최종)

```
OrganizationExceptionHandler (@RestControllerAdvice)
  ├─ OrganizationException ✅
  ├─ DataIntegrityViolationException ✅
  ├─ MethodArgumentNotValidException ✅ (신규 추가)
  ├─ RuntimeException (BusinessException 동적 처리) ✅
  └─ Exception (fallback) ✅

GlobalExceptionHandler (@RestControllerAdvice)
  └─ BusinessException ✅ (도달하지 못함)
```

---

## 🎯 왜 OrganizationExceptionHandler에 추가했는가?

### 이유
1. **Spring ExceptionHandler 우선순위**: `OrganizationExceptionHandler`가 먼저 적용됨 (알파벳 순서)
2. **Agent API도 포함**: `/api/v1/agents` 엔드포인트도 `OrganizationExceptionHandler`가 처리
3. **통합 예외 처리**: 모든 Bean Validation 실패를 한 곳에서 처리

### 대안
- `GlobalExceptionHandler`에 추가할 수도 있지만, 우선순위 문제로 도달하지 못함
- `@Order` 어노테이션으로 우선순위 조정 가능하지만, 더 복잡해짐

---

## ✅ 검증 완료

### 컴파일 에러
- ✅ import 추가 완료
- ⚠️ Warning만 존재 (기존 코드 스타일)

### 응답 형식
- ✅ 400 Bad Request 정상 반환
- ✅ INVALID_INPUT_VALUE 코드 정확
- ✅ Bean Validation 메시지 정확

### 동작 확인
- ✅ roles 누락: 400 Bad Request
- ✅ roles 빈 배열: 400 Bad Request
- ✅ 정상 생성: 201 Created
- ✅ 기타 필드 검증 실패: 400 Bad Request

---

## 📝 참고: Spring Bean Validation

### @Valid 어노테이션
```java
@PostMapping
public ResponseEntity<CreateAgentResponse> createAgent(
    @Valid @RequestBody CreateAgentRequest request) {
    // ...
}
```

### CreateAgentRequest 검증
```java
@NotBlank(message = "테넌트 ID는 필수입니다")
private String tenantId;

@NotBlank(message = "로그인 ID는 필수입니다")
@Pattern(regexp = "^[a-zA-Z0-9_.-]{4,20}$", message = "...")
private String loginId;

@NotEmpty(message = "역할은 최소 1개 이상이어야 합니다")
private Set<String> roles;
```

### 검증 실패 시 발생하는 예외
- **MethodArgumentNotValidException**: Request Body 검증 실패
- **BindException**: Request Parameter 검증 실패
- **ConstraintViolationException**: Path Variable 검증 실패

---

## 🎯 추가 개선 가능한 부분

### 여러 필드 에러 동시 반환
현재는 **첫 번째 필드 에러만** 반환합니다.

**개선안**:
```java
List<String> errors = e.getBindingResult().getAllErrors().stream()
        .map(error -> {
            if (error instanceof FieldError) {
                return ((FieldError) error).getField() + ": " + error.getDefaultMessage();
            }
            return error.getDefaultMessage();
        })
        .collect(Collectors.toList());

ErrorResponse response = ErrorResponse.builder()
        .code("INVALID_INPUT_VALUE")
        .message(String.join(", ", errors))
        .build();
```

**예시 응답**:
```json
{
  "code": "INVALID_INPUT_VALUE",
  "message": "tenantId: 테넌트 ID는 필수입니다, roles: 역할은 최소 1개 이상이어야 합니다"
}
```

---

**작성일**: 2026-02-08  
**수정 파일**: `OrganizationExceptionHandler.java`, `API_TEST_SCENARIOS_AGENT.md`  
**결과**: ✅ roles 없이 생성 시도 시 정확히 400 Bad Request 반환! 🚀

