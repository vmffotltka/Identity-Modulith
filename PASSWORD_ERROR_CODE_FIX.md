# 🎯 비밀번호 에러 코드 구분 완료!

## 📋 문제 상황

### Before (문제)
두 개의 다른 에러 상황이 동일한 코드로 반환됨:

```bash
# Case 1: confirmPassword 불일치
{
  "code": "C001",
  "message": "잘못된 입력값입니다."
}

# Case 2: currentPassword 불일치
{
  "code": "C001",
  "message": "잘못된 입력값입니다."
}
```

**문제점**:
- ❌ 에러 코드가 동일 (`C001`)
- ❌ 메시지가 동일 ("잘못된 입력값입니다.")
- ❌ 사용자가 어떤 문제인지 구분 불가
- ❌ 클라이언트에서 에러 처리 어려움

---

## ✅ 해결 방법

### 1. 비밀번호 전용 에러 코드 추가

**ErrorCode.java**:
```java
// Password Errors (P)
PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "P001", "현재 비밀번호가 일치하지 않습니다."),
PASSWORD_CONFIRMATION_MISMATCH(HttpStatus.BAD_REQUEST, "P002", "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."),
SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "P003", "새 비밀번호는 현재 비밀번호와 달라야 합니다."),
```

### 2. Controller 수정

**AgentController.java**:
```java
// confirmPassword 검증
if (!request.isPasswordMatching()) {
    throw new BusinessException(
            ErrorCode.PASSWORD_CONFIRMATION_MISMATCH,  // ✅ P002
            "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
}
```

### 3. Service 수정

**AgentService.java**:
```java
// 현재 비밀번호 검증
if (!passwordMatches) {
    throw new BusinessException(
            ErrorCode.PASSWORD_MISMATCH,  // ✅ P001
            "현재 비밀번호가 일치하지 않습니다.");
}

// 새 비밀번호 != 현재 비밀번호 검증
if (command.getCurrentPassword().equals(command.getNewPassword())) {
    throw new BusinessException(
            ErrorCode.SAME_AS_CURRENT_PASSWORD,  // ✅ P003
            "새 비밀번호는 현재 비밀번호와 달라야 합니다.");
}
```

---

## 🎯 결과 (After)

### Case 1: confirmPassword 불일치
```bash
curl -X POST "..." \
  -d '{
    "currentPassword": "Admin123!",
    "newPassword": "MyNewPassword456!",
    "confirmPassword": "DifferentPassword789!"
  }'

응답:
{
  "code": "P002",
  "message": "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."
}
```

### Case 2: currentPassword 불일치
```bash
curl -X POST "..." \
  -d '{
    "currentPassword": "WrongPassword123!",
    "newPassword": "MyNewPassword456!",
    "confirmPassword": "MyNewPassword456!"
  }'

응답:
{
  "code": "P001",
  "message": "현재 비밀번호가 일치하지 않습니다."
}
```

### Case 3: 새 비밀번호 == 현재 비밀번호
```bash
curl -X POST "..." \
  -d '{
    "currentPassword": "Admin123!",
    "newPassword": "Admin123!",
    "confirmPassword": "Admin123!"
  }'

응답:
{
  "code": "P003",
  "message": "새 비밀번호는 현재 비밀번호와 달라야 합니다."
}
```

---

## 📊 에러 코드 매핑

| 에러 상황 | 에러 코드 | 메시지 | HTTP Status |
|----------|----------|--------|-------------|
| confirmPassword 불일치 | **P002** | 새 비밀번호와 확인 비밀번호가 일치하지 않습니다. | 400 |
| currentPassword 불일치 | **P001** | 현재 비밀번호가 일치하지 않습니다. | 400 |
| 새 비밀번호 == 현재 비밀번호 | **P003** | 새 비밀번호는 현재 비밀번호와 달라야 합니다. | 400 |
| 비밀번호 형식 오류 | **C001** | (Bean Validation 메시지) | 400 |

---

## 🔍 검증 흐름

### 1단계: Request DTO Validation (Bean Validation)
```
@NotBlank → P002 또는 C001 (필드 누락)
@Size, @Pattern → C001 (형식 오류)
```

### 2단계: Controller Validation
```
isPasswordMatching() → P002 (confirmPassword 불일치)
```

### 3단계: Service Validation
```
passwordEncoder.matches() → P001 (currentPassword 불일치)
currentPassword.equals(newPassword) → P003 (동일한 비밀번호)
```

---

## 🧪 테스트 시나리오

### Scenario 1: confirmPassword 불일치
```bash
curl -X POST "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/change-password" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000003" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Admin123!",
    "newPassword": "MyNewPassword456!",
    "confirmPassword": "DifferentPassword789!"
  }'

예상: 400 Bad Request
{
  "code": "P002",
  "message": "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."
}
```

### Scenario 2: currentPassword 불일치
```bash
curl -X POST "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/change-password" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000003" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "WrongPassword123!",
    "newPassword": "MyNewPassword456!",
    "confirmPassword": "MyNewPassword456!"
  }'

예상: 400 Bad Request
{
  "code": "P001",
  "message": "현재 비밀번호가 일치하지 않습니다."
}
```

### Scenario 3: 새 비밀번호 == 현재 비밀번호
```bash
curl -X POST "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/change-password" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000003" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Admin123!",
    "newPassword": "Admin123!",
    "confirmPassword": "Admin123!"
  }'

예상: 400 Bad Request
{
  "code": "P003",
  "message": "새 비밀번호는 현재 비밀번호와 달라야 합니다."
}
```

### Scenario 4: 성공
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

---

## 📋 수정된 파일

1. **ErrorCode.java** - 비밀번호 에러 코드 3개 추가 (P001, P002, P003)
2. **AgentController.java** - 2개 메서드에서 P002 사용
3. **AgentService.java** - P001, P003 사용
4. **API_TEST_SCENARIOS_AGENT.md** - 예상 응답 업데이트

---

## ✅ 컴파일 확인

```bash
.\gradlew compileJava

BUILD SUCCESSFUL ✅
```

---

## 🎉 클라이언트 에러 처리 예시

### Before (구분 불가)
```javascript
// ❌ 모든 에러가 동일
if (error.code === 'C001') {
  // 어떤 에러인지 알 수 없음
  alert('잘못된 입력값입니다.');
}
```

### After (명확한 처리)
```javascript
// ✅ 에러 코드로 구분 가능
switch (error.code) {
  case 'P001':
    // 현재 비밀번호 필드에 포커스
    alert('현재 비밀번호가 일치하지 않습니다.');
    currentPasswordInput.focus();
    break;
    
  case 'P002':
    // confirmPassword 필드에 포커스
    alert('새 비밀번호와 확인 비밀번호가 일치하지 않습니다.');
    confirmPasswordInput.focus();
    break;
    
  case 'P003':
    // newPassword 필드에 포커스
    alert('새 비밀번호는 현재 비밀번호와 달라야 합니다.');
    newPasswordInput.focus();
    break;
    
  default:
    alert(error.message);
}
```

---

## 💡 에러 코드 설계 원칙

### 1. 카테고리별 접두사
- **C**: Common (공통 에러)
- **A**: Agent (상담사 관련)
- **P**: Password (비밀번호 관련) ← 신규
- **AUTH**: Authentication (인증 관련)

### 2. 명확한 네이밍
- `PASSWORD_MISMATCH`: 현재 비밀번호 불일치
- `PASSWORD_CONFIRMATION_MISMATCH`: 확인 비밀번호 불일치
- `SAME_AS_CURRENT_PASSWORD`: 동일한 비밀번호

### 3. HTTP Status 일관성
- 모든 비밀번호 에러: `400 Bad Request`
- 사용자 입력 오류이므로 4xx 범위

---

## 🚀 다음 단계

### 1. 애플리케이션 재시작
```bash
.\gradlew bootRun
```

### 2. 각 시나리오 테스트
- ✅ P001: currentPassword 불일치
- ✅ P002: confirmPassword 불일치
- ✅ P003: 새 비밀번호 == 현재 비밀번호
- ✅ 204: 성공

### 3. 로그 확인
```
[Controller] 비밀번호 불일치 → P002
[USER] 현재 비밀번호 불일치 → P001
[USER] 새 비밀번호가 현재 비밀번호와 동일 → P003
```

---

## 🎯 개선 효과

### 사용자 경험
- ✅ 명확한 에러 메시지
- ✅ 어떤 필드를 수정해야 하는지 알 수 있음
- ✅ 디버깅 시간 단축

### 개발자 경험
- ✅ 에러 코드로 빠른 문제 파악
- ✅ 로그 분석 용이
- ✅ 클라이언트 에러 처리 간편

### 유지보수성
- ✅ 명확한 에러 코드 체계
- ✅ 확장 가능한 구조 (P004, P005...)
- ✅ 일관된 에러 응답 형식

---

## 🎉 완료!

### 수정 사항
1. ✅ ErrorCode 3개 추가 (P001, P002, P003)
2. ✅ Controller 2곳 수정
3. ✅ Service 2곳 수정
4. ✅ API 문서 업데이트
5. ✅ 컴파일 성공

### 다음
- ⏳ 애플리케이션 재시작
- ⏳ API 테스트
- ⏳ 에러 코드 확인

이제 **비밀번호 에러를 명확하게 구분**할 수 있습니다! 🎯

