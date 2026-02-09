# ✅ 비밀번호 변경 API 에러 메시지 개선 완료!

## 🚨 문제 상황

### 에러 응답
```json
{
  "code": "C001",
  "message": "잘못된 입력값입니다."
}
```

**문제**: 
- ❌ 어떤 필드가 잘못되었는지 알 수 없음
- ❌ 정규식 검증 실패인지, 필수 필드 누락인지 불명확
- ❌ 디버깅 어려움

---

## 🔍 원인 분석

### 1. GlobalExceptionHandler 불완전
```java
// ❌ Before: BusinessException만 처리
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        // ...
    }
    // Bean Validation 에러 처리 없음!
}
```

**문제점**:
- `MethodArgumentNotValidException` 처리 안 함
- Spring의 기본 에러 응답만 반환
- 상세한 에러 메시지 부재

### 2. 데이터베이스 비밀번호 확인
```csv
agent_id,password
10000000-0000-0000-0000-000000000001,$2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm
```

**확인**: BCrypt 해시 형식 정상 ✅

### 3. Bean Validation 검증 항목
```java
@NotBlank(message = "현재 비밀번호는 필수입니다")
private String currentPassword;

@NotBlank(message = "새 비밀번호는 필수입니다")
@Size(min = 8, max = 20, message = "비밀번호는 8-20자여야 합니다")
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,20}$",
    message = "비밀번호는 영문 대소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다"
)
private String newPassword;

@NotBlank(message = "비밀번호 확인은 필수입니다")
private String confirmPassword;
```

---

## ✅ 해결 방법

### GlobalExceptionHandler 개선
```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode, e.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * ✅ Bean Validation 에러 처리 추가
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        // 모든 필드 에러를 수집하여 하나의 메시지로
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("[Validation Error] {}", errorMessage);
        
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * ✅ 기타 예외 처리 추가
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[Unexpected Error] {}", e.getMessage(), e);
        
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR, 
                "서버 내부 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

---

## 🎯 개선 효과

### Before (불명확)
```json
{
  "code": "C001",
  "message": "잘못된 입력값입니다."
}
```
- ❌ 어떤 필드가 문제인지 모름
- ❌ 어떻게 수정해야 하는지 모름

### After (명확)

#### Case 1: 비밀번호 형식 오류
```json
{
  "code": "C001",
  "message": "비밀번호는 영문 대소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다"
}
```

#### Case 2: 비밀번호 길이 오류
```json
{
  "code": "C001",
  "message": "비밀번호는 8-20자여야 합니다"
}
```

#### Case 3: 필수 필드 누락
```json
{
  "code": "C001",
  "message": "새 비밀번호는 필수입니다, 비밀번호 확인은 필수입니다"
}
```

#### Case 4: 현재 비밀번호 불일치 (Service 검증)
```json
{
  "code": "C001",
  "message": "현재 비밀번호가 일치하지 않습니다."
}
```

---

## 🧪 테스트 시나리오

### 1. 정상 케이스
```bash
curl -X POST "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/change-password" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000003" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Admin123!",
    "newPassword": "MyNewPassword456!",
    "confirmPassword": "MyNewPassword456!"
  }'

예상: 204 No Content ✅
```

### 2. 비밀번호 형식 오류
```bash
curl -X POST "..." \
  -d '{
    "currentPassword": "Admin123!",
    "newPassword": "weakpass",
    "confirmPassword": "weakpass"
  }'

예상: 400 Bad Request
{
  "code": "C001",
  "message": "비밀번호는 영문 대소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다"
}
```

### 3. 비밀번호 확인 불일치
```bash
curl -X POST "..." \
  -d '{
    "currentPassword": "Admin123!",
    "newPassword": "MyNewPassword456!",
    "confirmPassword": "Different789!"
  }'

예상: 400 Bad Request
{
  "code": "C001",
  "message": "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."
}
```

### 4. 현재 비밀번호 불일치
```bash
curl -X POST "..." \
  -d '{
    "currentPassword": "WrongPassword123!",
    "newPassword": "MyNewPassword456!",
    "confirmPassword": "MyNewPassword456!"
  }'

예상: 400 Bad Request
{
  "code": "C001",
  "message": "현재 비밀번호가 일치하지 않습니다."
}
```

---

## 📊 수정된 파일

1. **GlobalExceptionHandler.java**
   - ✅ `@ExceptionHandler(MethodArgumentNotValidException.class)` 추가
   - ✅ `@ExceptionHandler(Exception.class)` 추가
   - ✅ 로깅 추가 (`@Slf4j`)
   - ✅ 상세한 에러 메시지 반환

---

## ✅ 컴파일 확인

```bash
.\gradlew compileJava

BUILD SUCCESSFUL
```

---

## 🎉 완료!

이제 **Bean Validation 에러가 상세하게 표시**됩니다!

### 핵심 개선
1. ✅ **명확한 에러 메시지**: 어떤 필드가 문제인지 명시
2. ✅ **디버깅 용이**: 로그에 Validation 에러 기록
3. ✅ **사용자 친화적**: 수정 방법을 알 수 있음

### 다음 단계
1. ✅ 코드 수정 완료
2. ✅ 컴파일 성공
3. ⏳ 애플리케이션 재시작
4. ⏳ API 테스트
5. ⏳ 에러 메시지 확인

---

## 💡 추가 정보

### 데이터베이스 비밀번호
```
모든 계정: Admin123!
BCrypt 해시: $2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm
```

### 비밀번호 정책
- 길이: 8-20자
- 영문 소문자 1개 이상
- 영문 대문자 1개 이상
- 숫자 1개 이상
- 특수문자 1개 이상

### 유효한 비밀번호 예시
- ✅ `Admin123!`
- ✅ `MyNewPassword456!`
- ✅ `SecurePass@2026`
- ❌ `password` (조건 미충족)
- ❌ `Pass123` (길이 부족, 특수문자 없음)

이제 애플리케이션을 재시작하고 **정확한 에러 메시지**를 확인할 수 있습니다! 🚀

