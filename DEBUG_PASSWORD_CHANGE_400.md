# 🔍 비밀번호 변경 API 400 에러 디버깅

## 🚨 현재 상황

### 요청
```bash
curl -X POST "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/change-password" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000003" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Admin123!",
    "newPassword": "MyNewPassword456!",
    "confirmPassword": "MyNewPassword456!"
  }'
```

### 응답
```json
{
  "code": "C001",
  "message": "잘못된 입력값입니다."
}
```

### Hibernate 로그
```sql
SELECT ... FROM user_agents WHERE tenant_id=? AND agent_id=?
```
- ✅ Agent 조회 성공
- ❌ 그 이후 400 에러 발생

---

## 🔍 가능한 원인

### 1. Bean Validation 실패 (가능성 높음)
**정규식 검증**:
```java
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,20}$"
)
private String newPassword;
```

**테스트**:
- `MyNewPassword456!` 검증
  - ✅ 소문자: `y, e, w, a, s, s, o, r, d`
  - ✅ 대문자: `M, P, N`
  - ✅ 숫자: `456`
  - ✅ 특수문자: `!`
  - ✅ 길이: 18자
  
**→ 정규식은 통과해야 함**

### 2. confirmPassword 검증 실패
```java
if (!request.isPasswordMatching()) {
    throw new BusinessException(INVALID_INPUT_VALUE, 
        "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
}
```

**확인**:
- `newPassword`: `MyNewPassword456!`
- `confirmPassword`: `MyNewPassword456!`
- **→ 일치함**

### 3. 애플리케이션 재시작 안 함
- ✅ 코드 수정 완료
- ✅ 컴파일 완료
- ❌ **애플리케이션 재시작 안 함**

**→ 변경사항이 반영되지 않았을 가능성 높음!**

---

## ✅ 해결 방법

### 1. 애플리케이션 재시작 (필수!)
```bash
# 기존 프로세스 종료
Ctrl+C

# 재시작
.\gradlew bootRun
```

### 2. 로그 확인
재시작 후 다시 테스트하면 로그에서 실제 원인을 확인할 수 있습니다:

```
[Controller] 비밀번호 변경 요청 - userId=..., agentId=...
[Controller] 비밀번호 일치 확인: newPassword=..., confirmPassword=..., matching=true
[Controller] tenantId=...
[USER] 본인 확인 - agentId=..., actorId=...
[USER] 현재 비밀번호 검증 시작
[USER] 비밀번호 변경 완료
```

### 3. GlobalExceptionHandler 확인
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
    String errorMessage = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    
    log.warn("[Validation Error] {}", errorMessage);  // ✅ 로그 추가됨
    // ...
}
```

---

## 🧪 디버깅 체크리스트

### Step 1: 애플리케이션 재시작 확인
- [ ] 기존 프로세스 종료
- [ ] `.\gradlew bootRun` 실행
- [ ] "Started IdentityModulithApplication" 로그 확인
- [ ] Swagger UI 접속 확인

### Step 2: 로그 확인
- [ ] `[Controller] 비밀번호 변경 요청` 로그 확인
- [ ] `[Controller] 비밀번호 일치 확인` 로그 확인
- [ ] `[Validation Error]` 로그 확인 (있으면 원인 파악)
- [ ] `[USER] 비밀번호 변경` 로그 확인

### Step 3: Request Body 검증
- [ ] JSON 형식 정확한지 확인
- [ ] 필드명 대소문자 정확한지 확인
- [ ] 특수문자 이스케이프 확인

### Step 4: 데이터베이스 확인
```sql
SELECT agent_id, login_id, password, LENGTH(password)
FROM user_agents
WHERE agent_id = '10000000-0000-0000-0000-000000000003';

-- 예상 결과:
-- password: $2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm
-- LENGTH: 60
```

---

## 🎯 예상 시나리오

### 시나리오 A: Bean Validation 실패
**증상**: "잘못된 입력값입니다" + 로그 없음

**원인**: 
- 정규식 불일치
- 필드 누락
- 형식 오류

**로그 (재시작 후)**:
```
[Validation Error] 비밀번호는 영문 대소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다
```

### 시나리오 B: confirmPassword 불일치
**증상**: "새 비밀번호와 확인 비밀번호가 일치하지 않습니다"

**로그 (재시작 후)**:
```
[Controller] 비밀번호 불일치 - newPassword=..., confirmPassword=...
```

### 시나리오 C: 현재 비밀번호 불일치
**증상**: "현재 비밀번호가 일치하지 않습니다"

**로그 (재시작 후)**:
```
[Controller] 비밀번호 변경 요청 - userId=..., agentId=...
[USER] 현재 비밀번호 검증 실패
```

### 시나리오 D: 애플리케이션 재시작 안 함
**증상**: 코드 수정했는데 동작 안 바뀜

**해결**: `.\gradlew bootRun`

---

## 📋 수정 완료 체크리스트

### 코드 수정
- [x] ChangePasswordRequest - confirmPassword 추가
- [x] AgentController - confirmPassword 검증 추가
- [x] AgentController - @Slf4j 추가
- [x] AgentController - 로그 추가
- [x] GlobalExceptionHandler - Bean Validation 처리 추가
- [x] V2_0_0__Fixed_Schema.sql - BCrypt 해시 변경

### 컴파일
- [x] `.\gradlew compileJava`
- [x] BUILD SUCCESSFUL

### 다음 단계
- [ ] **애플리케이션 재시작** ⚠️
- [ ] API 테스트
- [ ] 로그 확인
- [ ] 성공/실패 원인 파악

---

## 🎉 예상 결과 (재시작 후)

### 성공 케이스
```bash
curl -X POST "..." \
  -d '{
    "currentPassword": "Admin123!",
    "newPassword": "MyNewPassword456!",
    "confirmPassword": "MyNewPassword456!"
  }'

응답: 204 No Content

로그:
[Controller] 비밀번호 변경 요청 - userId=10000000-0000-0000-0000-000000000003, agentId=10000000-0000-0000-0000-000000000003
[Controller] 비밀번호 일치 확인: newPassword=MyNewPassword456!, confirmPassword=MyNewPassword456!, matching=true
[USER] 비밀번호 변경 완료 - agentId=10000000-0000-0000-0000-000000000003
```

### 실패 케이스 (상세 에러)
```bash
# confirmPassword 불일치
응답: 400 Bad Request
{
  "code": "C001",
  "message": "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."
}

로그:
[Controller] 비밀번호 불일치 - newPassword=..., confirmPassword=...
```

---

## 💡 핵심 포인트

### 코드 변경 후 반드시 재시작!
```bash
# 1. 코드 수정
# 2. 컴파일
.\gradlew compileJava

# 3. ⚠️ 재시작 필수!
.\gradlew bootRun
```

### GlobalExceptionHandler가 제대로 작동하려면
- ✅ `@RestControllerAdvice` 어노테이션
- ✅ `@ExceptionHandler` 메서드
- ✅ **애플리케이션 재시작** ← 중요!

---

## 🚀 다음 단계

1. **애플리케이션 재시작**
   ```bash
   .\gradlew bootRun
   ```

2. **API 재테스트**
   ```bash
   # 동일한 요청 재시도
   curl -X POST "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/change-password" \
     -H "X-User-Id: 10000000-0000-0000-0000-000000000003" \
     -H "Content-Type: application/json" \
     -d '{
       "currentPassword": "Admin123!",
       "newPassword": "MyNewPassword456!",
       "confirmPassword": "MyNewPassword456!"
     }'
   ```

3. **로그 확인**
   - `[Controller] 비밀번호 변경 요청` 로그 확인
   - `[Validation Error]` 로그 확인 (있으면 원인)
   - 성공 시 `[USER] 비밀번호 변경 완료` 로그

4. **결과에 따라 추가 조치**
   - 성공: ✅ 완료!
   - 실패: 로그 메시지로 원인 파악

---

## 📝 완료 보고서

### 수정된 파일 (6개)
1. ChangePasswordRequest.java - confirmPassword 추가
2. AgentController.java - confirmPassword 검증, 로그 추가, @Slf4j
3. GlobalExceptionHandler.java - Bean Validation 처리
4. V2_0_0__Fixed_Schema.sql - BCrypt 해시
5. RbacPort.java - hasRole() 추가
6. RbacAdapter.java - hasRole() 구현

### 다음 필수 작업
⚠️ **애플리케이션 재시작 후 재테스트**

재시작하고 로그를 확인하면 정확한 원인을 알 수 있습니다! 🔍

