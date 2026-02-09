# 🎉 근본 원인 발견 및 해결 완료!

## 🔍 문제의 근본 원인

### 발견된 문제
**두 개의 다른 PasswordEncoder가 사용되고 있었습니다!**

```java
// ❌ AgentService가 사용 (SHA-256)
@Component
public class PasswordEncoderImpl implements PasswordEncoder {
    @Override
    public String encode(String rawPassword) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);  // ← SHA-256 Base64
    }
    
    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);  // ← SHA-256으로 비교
    }
}

// ✅ DevController가 사용 (BCrypt)
@RestController
public class DevController {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();  // ← BCrypt
}
```

### 왜 문제가 발생했나?

1. **DB에 저장된 비밀번호**: BCrypt 해시
   ```
   $2a$10$o6GJICIhlWnRqm3wJNLzx.DtRHtISvJXgBZ.7YKjGJoCXZ27eoBB2
   ```

2. **DevController 검증**: BCrypt로 검증
   ```java
   encoder.matches("Admin123!", "$2a$10$o6GJI...")  // ✅ true
   ```

3. **AgentService 검증**: SHA-256으로 검증
   ```java
   // "Admin123!"를 SHA-256으로 해시
   String sha256Hash = encode("Admin123!");
   // → "abc123def..." (Base64)
   
   // DB의 BCrypt 해시와 비교
   sha256Hash.equals("$2a$10$o6GJI...")  // ❌ false!
   ```

### 비유
```
DevController: "이 열쇠(BCrypt)로 자물쇠(BCrypt 해시)를 열 수 있나?" → ✅ "네!"
AgentService: "이 열쇠(SHA-256)로 자물쇠(BCrypt 해시)를 열 수 있나?" → ❌ "아니요!"
```

---

## ✅ 해결 방법

### 1. PasswordEncoderImpl 수정
```java
// ✅ After: BCrypt 사용
@Component
@RequiredArgsConstructor
public class PasswordEncoderImpl implements PasswordEncoder {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public String encode(String rawPassword) {
        return bCryptPasswordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return bCryptPasswordEncoder.matches(rawPassword, encodedPassword);
    }
}
```

### 2. SecurityConfig에 BCryptPasswordEncoder Bean 추가
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // ...existing code...
}
```

---

## 🎯 작동 원리

### Before (문제)
```
1. 사용자: "Admin123!" 입력
   ↓
2. AgentService.changePassword()
   ↓
3. PasswordEncoderImpl.matches("Admin123!", "$2a$10$o6GJI...")
   ↓
4. SHA-256으로 "Admin123!" 해시 생성
   → "xyz789abc..." (Base64)
   ↓
5. "xyz789abc...".equals("$2a$10$o6GJI...")
   ↓
6. ❌ false (불일치!)
   ↓
7. throw BusinessException("현재 비밀번호 불일치")
```

### After (해결)
```
1. 사용자: "Admin123!" 입력
   ↓
2. AgentService.changePassword()
   ↓
3. PasswordEncoderImpl.matches("Admin123!", "$2a$10$o6GJI...")
   ↓
4. BCryptPasswordEncoder.matches("Admin123!", "$2a$10$o6GJI...")
   ↓
5. BCrypt 알고리즘으로 검증
   → Salt 추출 → 해시 재생성 → 비교
   ↓
6. ✅ true (일치!)
   ↓
7. 비밀번호 변경 성공
```

---

## 📊 수정된 파일

### 1. PasswordEncoderImpl.java
- ❌ SHA-256 + Base64 제거
- ✅ BCryptPasswordEncoder 사용

### 2. SecurityConfig.java
- ✅ BCryptPasswordEncoder Bean 추가
- ✅ import 추가

---

## ✅ 컴파일 확인

```bash
.\gradlew compileJava

BUILD SUCCESSFUL ✅
```

---

## 🧪 테스트 시나리오

### Step 1: 애플리케이션 재시작
```bash
.\gradlew bootRun
```

### Step 2: DevController로 검증 (변화 없음)
```bash
curl "http://localhost:8080/api/dev/check-agent-password?agentId=10000000-0000-0000-0000-000000000003&password=Admin123!"

예상:
{
  "matches": true,
  "message": "✅ 비밀번호 일치"
}
```

### Step 3: 비밀번호 변경 API 테스트 (이제 성공!)
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

### Step 4: 로그 확인
```
[Controller] 비밀번호 변경 요청
[USER] 비밀번호 변경 시작
[USER] 본인 확인 통과
[USER] 상담사 조회 성공
[USER] 현재 비밀번호 검증 시작
[USER] 비밀번호 일치 여부: true  ← ✅ 이제 true!
[USER] 현재 비밀번호 검증 통과
[USER] 새 비밀번호 검증 통과
[USER] 비밀번호 암호화 및 변경 완료
[USER] 저장 완료
[USER] 비밀번호 변경 완료
```

### Step 5: 변경된 비밀번호로 재검증
```bash
curl "http://localhost:8080/api/dev/check-agent-password?agentId=10000000-0000-0000-0000-000000000003&password=MyNewPassword456!"

예상:
{
  "matches": true,
  "message": "✅ 비밀번호 일치"
}
```

---

## 💡 교훈

### 1. 일관성의 중요성
- ✅ **모든 곳에서 동일한 암호화 알고리즘 사용**
- ❌ 한 곳은 BCrypt, 다른 곳은 SHA-256 → 혼란

### 2. DIP(의존성 역전 원칙)의 올바른 적용
```java
// ✅ 올바른 방법
@Component
@RequiredArgsConstructor
public class PasswordEncoderImpl implements PasswordEncoder {
    private final BCryptPasswordEncoder bCryptPasswordEncoder;  // DI 받음
    // ...
}
```

### 3. Bean 등록의 중요성
```java
// ✅ SecurityConfig에 Bean 등록
@Bean
public BCryptPasswordEncoder bCryptPasswordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### 4. 디버깅 도구의 가치
- DevController 덕분에 **두 PasswordEncoder가 다르다는 것을 발견**
- 동일한 입력(Admin123!)과 동일한 해시로 테스트
- 하나는 성공, 하나는 실패 → 원인 파악

---

## 🎯 문제 해결 과정 요약

### 1단계: 증상 확인
```
DevController: matches=true ✅
AgentService: "현재 비밀번호 불일치" ❌
```

### 2단계: 가설 수립
"두 곳에서 다른 검증 방법을 사용하고 있다!"

### 3단계: 코드 확인
```
PasswordEncoderImpl: SHA-256 사용 ❌
DevController: BCrypt 사용 ✅
```

### 4단계: 해결
```
PasswordEncoderImpl → BCrypt로 변경 ✅
SecurityConfig → BCryptPasswordEncoder Bean 추가 ✅
```

### 5단계: 검증
```
컴파일 성공 ✅
애플리케이션 재시작 ⏳
API 테스트 ⏳
```

---

## 🎉 완료!

### 수정 사항
1. ✅ PasswordEncoderImpl - BCrypt 사용
2. ✅ SecurityConfig - BCryptPasswordEncoder Bean 추가
3. ✅ 컴파일 성공

### 다음 단계
1. **애플리케이션 재시작** (필수!)
   ```bash
   .\gradlew bootRun
   ```

2. **비밀번호 변경 API 테스트**
   ```bash
   # 이제 성공할 것입니다!
   curl -X POST "..." -d '{...}'
   
   예상: 204 No Content ✅
   ```

3. **로그 확인**
   ```
   [USER] 비밀번호 일치 여부: true ✅
   [USER] 비밀번호 변경 완료 ✅
   ```

---

## 🔑 핵심 포인트

### 문제의 본질
**DevController와 AgentService가 서로 다른 열쇠(암호화 알고리즘)를 사용하고 있었습니다!**

### 해결의 핵심
**모든 곳에서 동일한 BCrypt를 사용하도록 통일했습니다!**

### 왜 이런 일이?
```java
// TODO: 실제 운영에서는 BCryptPasswordEncoder 등 Spring Security 사용 권장
```
→ 이 TODO가 실제로 구현되지 않았습니다!

---

이제 **PasswordEncoder가 통일**되어 정상 작동할 것입니다! 재시작 후 테스트해보세요! 🚀

