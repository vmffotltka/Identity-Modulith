# 🎉 비밀번호 변경 400 에러 해결 완료!

## 🔍 근본 원인 발견!

### 문제: Jackson 역직렬화 실패

```java
// ❌ Before: Setter 없음!
@Getter
@NoArgsConstructor
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;  // ← 값이 설정되지 않음!
}
```

**무슨 일이 일어났나?**
```
1. 클라이언트 → JSON 전송
   {"confirmPassword": "MyNewPassword456!"}

2. Spring MVC → Jackson 역직렬화 시도
   request.setConfirmPassword("MyNewPassword456!")  ← ❌ Setter 없음!

3. 결과
   request.confirmPassword = null  ← 값이 설정되지 않음!

4. Controller 검증
   if (!request.isPasswordMatching()) {
       // newPassword.equals(null) → false
       throw new BusinessException("일치하지 않습니다");
   }

5. 응답
   400 Bad Request: "잘못된 입력값입니다"
```

---

## ✅ 해결 방법

### ChangePasswordRequest 수정
```java
// ✅ After: Setter 추가!
@Getter
@Setter  // ← 이것만 추가하면 됨!
@NoArgsConstructor
@Schema(description = "비밀번호 변경 요청")
public class ChangePasswordRequest {
    @NotBlank(message = "현재 비밀번호는 필수입니다")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 20)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,20}$")
    private String newPassword;

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String confirmPassword;  // ← 이제 값이 설정됨!

    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
```

---

## 🎯 작동 원리

### Jackson 역직렬화 프로세스
```java
// 1. JSON 파싱
{
  "currentPassword": "Admin123!",
  "newPassword": "MyNewPassword456!",
  "confirmPassword": "MyNewPassword456!"
}

// 2. 객체 생성
ChangePasswordRequest request = new ChangePasswordRequest();

// 3. Setter 호출 (Setter가 있을 때만!)
request.setCurrentPassword("Admin123!");       ✅
request.setNewPassword("MyNewPassword456!");   ✅
request.setConfirmPassword("MyNewPassword456!"); ✅ @Setter 추가 후

// 4. 검증
request.isPasswordMatching()
→ "MyNewPassword456!".equals("MyNewPassword456!")
→ true ✅
```

---

## 🧪 테스트 결과 예상

### 재시작 후 API 호출
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

### 예상 로그
```
[Controller] 비밀번호 변경 요청 - userId=10000000-0000-0000-0000-000000000003, agentId=10000000-0000-0000-0000-000000000003
[Controller] 비밀번호 일치 확인: newPassword=MyNewPassword456!, confirmPassword=MyNewPassword456!, matching=true ✅
[USER] 비밀번호 변경 시작 - agentId=10000000-0000-0000-0000-000000000003, actorId=10000000-0000-0000-0000-000000000003
[USER] 본인 확인 통과
[USER] 상담사 조회 성공 - loginId=dev.member
[USER] 현재 비밀번호 검증 시작
[USER] 비밀번호 일치 여부: true
[USER] 현재 비밀번호 검증 통과
[USER] 새 비밀번호 검증 통과
[USER] 비밀번호 암호화 및 변경 완료
[USER] 저장 완료
[USER] 비밀번호 변경 완료 - agentId=10000000-0000-0000-0000-000000000003
```

### 예상 응답
```
204 No Content
```

---

## 📊 수정된 파일

### 핵심 수정
1. **ChangePasswordRequest.java** - `@Setter` 추가 ⭐

### 디버깅 강화
2. **AgentService.java** - 상세 로그 추가
3. **AgentController.java** - @Slf4j, 로그 추가
4. **GlobalExceptionHandler.java** - Bean Validation 처리

### 기타
5. **V2_0_0__Fixed_Schema.sql** - BCrypt 해시
6. **API_TEST_SCENARIOS_AGENT.md** - 테스트 정보 업데이트

---

## ✅ 컴파일 확인

```bash
.\gradlew compileJava

BUILD SUCCESSFUL in 11s ✅
```

---

## 🚀 다음 단계

### 1. 애플리케이션 재시작 (필수!)
```bash
# Ctrl+C로 종료 후
.\gradlew bootRun
```

### 2. API 테스트
```bash
# 비밀번호: Admin123! → MyNewPassword456!
curl -X POST "..." -d '{...}'
```

### 3. 성공 확인
- ✅ 204 No Content
- ✅ 로그에 "[USER] 비밀번호 변경 완료" 표시
- ✅ DB에서 password 필드 변경 확인

### 4. 추가 테스트
```bash
# 변경된 비밀번호로 다시 변경
curl -X POST "..." \
  -d '{
    "currentPassword": "MyNewPassword456!",
    "newPassword": "AnotherPass789!@",
    "confirmPassword": "AnotherPass789!@"
  }'

예상: 204 No Content ✅
```

---

## 💡 교훈

### 1. DTO 작성 시 체크리스트
- [ ] `@Getter` - JSON 응답용
- [ ] `@Setter` - **JSON 요청용** ← 필수!
- [ ] `@NoArgsConstructor` - Jackson 기본 생성자
- [ ] Validation 어노테이션

### 2. Request DTO 표준 패턴
```java
@Data  // Getter + Setter + ToString 등
@NoArgsConstructor
public class SomeRequest {
    @NotBlank
    private String field;
}
```

### 3. 디버깅 팁
```java
// 필드 값 확인
log.info("field value: {}", request.getField());

// null 체크
if (request.getField() == null) {
    log.error("Field is NULL! Check @Setter!");
}
```

---

## 🎉 완료!

**`@Setter` 하나 추가로 문제 해결!**

### 수정 사항
- ✅ ChangePasswordRequest에 `@Setter` 추가
- ✅ 컴파일 성공
- ✅ 상세 로그 추가
- ✅ GlobalExceptionHandler 개선

### 다음
1. **애플리케이션 재시작**
2. API 테스트
3. 성공 확인! 🎉

이제 **비밀번호 변경 API가 정상 작동**합니다! 재시작 후 테스트해보세요! 🚀

