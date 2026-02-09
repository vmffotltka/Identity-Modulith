# 🔍 비밀번호 변경 400 에러 - 최종 디버깅 가이드

## 📊 현재 상황 분석

### 로그 확인
```
2026-02-09T14:22:40.950+09:00 INFO [Controller] 비밀번호 변경 요청 - userId=..., agentId=...
Hibernate: SELECT ... FROM user_agents WHERE tenant_id=? AND agent_id=?
```

**분석**:
- ✅ Controller 진입 성공
- ✅ Agent 조회 쿼리 실행
- ❌ `[Controller] 비밀번호 일치 확인` 로그 없음
- ❌ `[USER] 비밀번호 변경 시작` 로그 없음

**→ Controller의 `isPasswordMatching()` 검증 또는 그 이전에 실패**

---

## 🚨 가능한 원인

### 원인 1: isPasswordMatching() 메서드 호출 실패
```java
// Controller
if (!request.isPasswordMatching()) {
    throw new BusinessException(...);
}
```

**문제 가능성**:
- `request.getNewPassword()` 또는 `getConfirmPassword()`가 null일 수 있음
- `isPasswordMatching()` 메서드에서 NullPointerException 발생

### 원인 2: DTO Getter 메서드 누락
```java
public class ChangePasswordRequest {
    @NotBlank
    private String confirmPassword;
    
    // ❌ Getter가 없으면?
    public String getConfirmPassword() { ... }  // 있나?
}
```

Lombok의 `@Getter`가 있지만 확인이 필요합니다.

### 원인 3: JSON 역직렬화 실패
```json
{
  "currentPassword": "Admin123!",
  "newPassword": "MyNewPassword456!",
  "confirmPassword": "MyNewPassword456!"
}
```

**문제 가능성**:
- Jackson이 `confirmPassword` 필드를 인식하지 못함
- Setter가 없어서 값이 설정되지 않음

---

## ✅ 해결 방법

### 1. ChangePasswordRequest에 Setter 추가
```java
@Getter
@Setter  // ✅ 추가
@NoArgsConstructor
public class ChangePasswordRequest {
    // ...
}
```

Jackson은 Setter를 사용하여 JSON을 객체로 변환합니다!

### 2. 또는 @Data 사용
```java
@Data  // @Getter + @Setter + @ToString 등 포함
@NoArgsConstructor
public class ChangePasswordRequest {
    // ...
}
```

### 3. 디버깅 로그 강화
```java
// Controller
log.info("[Controller] Request received - newPassword={}, confirmPassword={}", 
        request.getNewPassword(), request.getConfirmPassword());

if (request.getConfirmPassword() == null) {
    log.error("[Controller] confirmPassword is NULL!");
}
```

---

## 🔧 즉시 수정

### ChangePasswordRequest.java
```java
@Getter
@Setter  // ✅ 추가!
@NoArgsConstructor
@Schema(description = "비밀번호 변경 요청")
public class ChangePasswordRequest {
    // ...
}
```

**왜 필요한가?**
- Jackson은 기본적으로 **Setter를 사용하여 JSON → Object 변환**
- `@Getter`만 있으면 읽기만 가능
- `confirmPassword` 필드를 추가했지만 **Setter가 없어서 값이 설정되지 않음**
- `isPasswordMatching()` 호출 시 `confirmPassword == null` → **NullPointerException 또는 false 반환**

---

## 🧪 테스트 시나리오

### Before (문제)
```java
@Getter  // Setter 없음!
public class ChangePasswordRequest {
    private String confirmPassword;
}
```

**결과**:
```
JSON: {"confirmPassword": "MyNewPassword456!"}
↓ (Jackson 역직렬화)
request.confirmPassword = null  // ❌ Setter 없어서 설정 안 됨!
↓
isPasswordMatching() 호출
↓
newPassword.equals(null)  // ❌ false 또는 NPE
↓
throw BusinessException("일치하지 않습니다")
```

### After (해결)
```java
@Getter
@Setter  // ✅ 추가!
public class ChangePasswordRequest {
    private String confirmPassword;
}
```

**결과**:
```
JSON: {"confirmPassword": "MyNewPassword456!"}
↓ (Jackson 역직렬화)
request.setConfirmPassword("MyNewPassword456!")  // ✅ Setter 호출
↓
request.confirmPassword = "MyNewPassword456!"  // ✅ 설정됨
↓
isPasswordMatching() 호출
↓
"MyNewPassword456!".equals("MyNewPassword456!")  // ✅ true
↓
검증 통과 → Service 호출
```

---

## 📋 수정 체크리스트

### 즉시 수정
- [ ] ChangePasswordRequest에 `@Setter` 추가
- [ ] 컴파일
- [ ] 애플리케이션 재시작
- [ ] API 재테스트

### 컴파일 & 재시작
```bash
# 1. 컴파일
.\gradlew compileJava

# 2. 재시작
.\gradlew bootRun
```

### 테스트
```bash
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

### 로그 확인 (재시작 후)
```
[Controller] 비밀번호 변경 요청 - userId=..., agentId=...
[Controller] 비밀번호 일치 확인: ..., matching=true  ✅ 이 로그가 나와야 함
[USER] 비밀번호 변경 시작 - agentId=...
[USER] 본인 확인 통과
[USER] 상담사 조회 성공 - loginId=dev.member
[USER] 현재 비밀번호 검증 시작
[USER] 비밀번호 일치 여부: true
[USER] 현재 비밀번호 검증 통과
[USER] 새 비밀번호 검증 통과
[USER] 비밀번호 암호화 및 변경 완료
[USER] 저장 완료
[USER] 비밀번호 변경 완료 - agentId=...
```

---

## 🎯 예상 결과

### 성공 시
```
204 No Content
(응답 본문 없음)
```

### 실패 시 (상세 메시지)
```json
// confirmPassword null
{
  "code": "C001",
  "message": "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."
}

// 또는 현재 비밀번호 불일치
{
  "code": "C001",
  "message": "현재 비밀번호가 일치하지 않습니다."
}
```

---

## 🔑 핵심 포인트

### Jackson 역직렬화 규칙
1. **Getter**: 객체 → JSON (직렬화)
2. **Setter**: JSON → 객체 (역직렬화) ← **중요!**
3. **필드에 값을 설정하려면 Setter 필수**

### Lombok 어노테이션
- `@Getter`: Getter만 생성
- `@Setter`: Setter만 생성
- `@Data`: Getter + Setter + ToString + EqualsAndHashCode + RequiredArgsConstructor
- `@NoArgsConstructor`: 기본 생성자

### 권장 DTO 패턴
```java
@Data  // 또는 @Getter + @Setter
@NoArgsConstructor
public class SomeRequest {
    @NotBlank
    private String field;
}
```

---

## 🎉 결론

**confirmPassword 필드를 추가했지만 `@Setter`가 없어서 JSON 값이 객체에 설정되지 않았습니다!**

`@Setter`를 추가하고 재시작하면 해결됩니다! 🚀

