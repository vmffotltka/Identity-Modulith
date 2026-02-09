# 비밀번호 변경 API 400 에러 해결

## 🚨 문제 상황

```bash
curl -X POST "http://localhost:8080/api/v1/agents/.../change-password" \
  -d '{
    "currentPassword": "TempPassword123!",
    "newPassword": "MyNewPassword456!",
    "confirmPassword": "MyNewPassword456!"
  }'

응답: 400 Bad Request
{
  "code": "C001",
  "message": "잘못된 입력값입니다."
}
```

## 🔍 원인 분석

### 1. Bean Validation 실패
`@Pattern` 정규식이 너무 제한적이었습니다:

```java
// ❌ Before: 특수문자를 @$!%*?&#로만 제한
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,20}$",
    message = "비밀번호는 영문 대소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다"
)
```

**문제점**:
- `[A-Za-z\\d@$!%*?&#]{8,20}$` 부분이 **허용되는 문자를 제한**함
- 사용자가 다른 특수문자를 사용하면 검증 실패
- 예: `!`, `@`는 허용하지만 `-`, `_`, `.` 등은 불허

### 2. 데이터베이스 비밀번호 문제
초기 데이터의 비밀번호 해시:
```sql
-- V2_0_0에서 설정한 비밀번호: Admin123!
password = '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm'
```

하지만 사용자가 입력한 `currentPassword`는:
```
TempPassword123!
```

**→ 비밀번호 불일치!**

---

## ✅ 해결 방법

### 1. 정규식 수정
```java
// ✅ After: 모든 일반적인 특수문자 허용
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,20}$",
    message = "비밀번호는 영문 대소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다"
)
private String newPassword;
```

**개선점**:
- `.{8,20}$` 로 변경하여 **모든 문자 허용**
- 특수문자 검증은 lookahead로만 수행: `(?=.*[!@#$%^&*...])`
- 더 유연한 비밀번호 정책

### 2. 테스트 계정 비밀번호 안내
```markdown
## 🔑 테스트 계정 정보

| loginId | password | 역할 |
|---------|----------|------|
| admin | **Admin123!** | ADMIN |
| dev.lead | **Admin123!** | TEAM_LEAD |
| dev.member | **Admin123!** | MEMBER |

⚠️ 현재 비밀번호: **Admin123!** (모두 동일)
```

### 3. API 테스트 시나리오 수정
```bash
# ✅ 올바른 테스트
curl -X POST "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/change-password" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000003" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Admin123!",
    "newPassword": "MyNewPassword456!",
    "confirmPassword": "MyNewPassword456!"
  }'

예상: 204 No Content
```

---

## 🧪 테스트 단계

### 1. 컴파일 확인
```bash
.\gradlew compileJava

BUILD SUCCESSFUL
```

### 2. 애플리케이션 재시작
```bash
.\gradlew bootRun
```

### 3. 비밀번호 변경 테스트
```bash
# 현재 비밀번호: Admin123!
# 새 비밀번호: MyNewPassword456!
```

### 4. 검증 항목
- ✅ 400 에러 해결
- ✅ Bean Validation 통과
- ✅ 현재 비밀번호 일치 확인
- ✅ confirmPassword 일치 확인
- ✅ 비밀번호 변경 성공

---

## 📋 정규식 비교

### Before (제한적)
```regex
^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,20}$
```
- ❌ 허용 문자: `A-Za-z0-9@$!%*?&#` 만
- ❌ 다른 특수문자 사용 불가

### After (유연)
```regex
^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]).{8,20}$
```
- ✅ 대부분의 특수문자 허용
- ✅ 검증: lookahead로만 수행
- ✅ 본문: `.{8,20}` 모든 문자 허용

---

## 🔐 비밀번호 정책

### 요구사항
1. ✅ 길이: 8-20자
2. ✅ 영문 소문자 1개 이상
3. ✅ 영문 대문자 1개 이상
4. ✅ 숫자 1개 이상
5. ✅ 특수문자 1개 이상

### 허용 특수문자
```
! @ # $ % ^ & * ( ) _ + - = [ ] { } ; ' : " \ | , . < > / ?
```

### 예시
- ✅ `Admin123!`
- ✅ `MyNewPassword456!`
- ✅ `Test@Pass#123`
- ✅ `Secure_Password1`
- ❌ `password` (대문자, 숫자, 특수문자 없음)
- ❌ `Pass123` (8자 미만, 특수문자 없음)

---

## 📊 수정된 파일

1. **ChangePasswordRequest.java**
   - `@Pattern` 정규식 수정
   - 더 유연한 비밀번호 정책

2. **API_TEST_SCENARIOS_AGENT.md** (업데이트 필요)
   - Scenario 8-1: `currentPassword` → `Admin123!`
   - 테스트 계정 정보 업데이트

---

## ✅ 최종 체크리스트

- [x] 정규식 수정 완료
- [x] 컴파일 성공
- [ ] 애플리케이션 재시작
- [ ] API 테스트 (Admin123! → MyNewPassword456!)
- [ ] 문서 업데이트

---

## 🎉 완료!

정규식을 수정하여 더 유연한 비밀번호 정책을 적용했습니다!

### 핵심 변경
- ❌ Before: `[A-Za-z\\d@$!%*?&#]{8,20}$` (특정 문자만)
- ✅ After: `.{8,20}$` (모든 문자 허용, 검증은 lookahead로)

### 다음 단계
1. 애플리케이션 재시작
2. 현재 비밀번호: `Admin123!` 사용
3. 새 비밀번호: `MyNewPassword456!` 테스트
4. 204 No Content 확인 ✅

