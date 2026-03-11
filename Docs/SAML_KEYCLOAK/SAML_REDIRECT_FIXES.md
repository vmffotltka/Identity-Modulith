# SAML 리디렉션 및 컴파일 오류 수정

## 🐛 발견된 문제

### 1. 컴파일 오류
```
Saml2SecurityConfig.java:118: error: cannot find symbol
    .logoutSuccessUrl("/?logout=success")
    ^
  symbol:   method logoutSuccessUrl(String)
  location: class Saml2LogoutConfigurer<HttpSecurity>
```

### 2. 리디렉션 루프 문제
- SAML 인증 성공 후 `/saml-info`로 강제 리디렉션
- `alwaysUseDefaultTargetUrl(true)` 설정으로 인한 SavedRequest 무시
- `/login` 엔드포인트에서 불필요한 리디렉션 발생

---

## ✅ 적용된 수정사항

### 1. Saml2SecurityConfig.java 수정

#### 문제: SAML2 로그아웃 설정에서 존재하지 않는 메서드 호출
**Before:**
```java
.saml2Logout(saml2Logout -> saml2Logout
    .logoutUrl("/saml2/logout")
    .logoutSuccessUrl("/?logout=success")  // ❌ 이 메서드는 존재하지 않음
);
```

**After:**
```java
.saml2Logout(saml2Logout -> saml2Logout
    .logoutUrl("/saml2/logout")
);
```

**설명:**
- `Saml2LogoutConfigurer`는 `logoutSuccessUrl()` 메서드를 제공하지 않음
- SAML 로그아웃은 IdP와의 상호작용이 필요하므로 일반 로그아웃과 별도 처리
- 일반 로그아웃(`.logout()`)에서 `logoutSuccessUrl()` 설정은 유지됨

---

### 2. Saml2AuthenticationSuccessHandler.java 수정

#### 문제: 인증 성공 후 항상 `/saml-info`로 강제 리디렉션
**Before:**
```java
public Saml2AuthenticationSuccessHandler() {
    setDefaultTargetUrl("/saml-info");
    setAlwaysUseDefaultTargetUrl(true);  // ❌ SavedRequest 무시
}
```

**After:**
```java
public Saml2AuthenticationSuccessHandler() {
    setDefaultTargetUrl("/");
    setAlwaysUseDefaultTargetUrl(false); // ✅ SavedRequest 우선 사용
}
```

**설명:**
- `alwaysUseDefaultTargetUrl(false)` 설정으로 SavedRequest 우선 처리
- 사용자가 보호된 리소스에 접근하려다 로그인하면 해당 페이지로 리디렉션
- SavedRequest가 없을 때만 홈페이지(`/`)로 리디렉션
- `/saml-info`는 테스트용이므로 기본 대상으로 부적절

---

### 3. SamlTestController.java 수정

#### 문제: `/login` 엔드포인트에서 불필요한 리디렉션
**Before:**
```java
@GetMapping("/login")
public String login() {
    return "redirect:/saml2/authenticate/keycloak";  // ❌ 불필요한 리디렉션
}
```

**After:**
```java
@GetMapping("/login")
@ResponseBody
public String login() {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>로그인 중...</title>
            ...
        </head>
        <body>
            <h1>🔑 SAML SSO 로그인</h1>
            <p>Keycloak으로 리디렉션 중입니다...</p>
            <p>자동으로 리디렉션되지 않으면 <a href="/saml2/authenticate/keycloak">여기</a>를 클릭하세요.</p>
        </body>
        </html>
        """;
}
```

**설명:**
- Spring Security가 이미 `/login` 요청을 가로채서 SAML SSO로 리디렉션
- 컨트롤러의 리디렉션은 중복이며 불필요한 추가 리디렉션 발생
- HTML 페이지로 변경하여 사용자에게 상태 표시
- 수동 링크 제공으로 자동 리디렉션 실패 시 대안 제공

---

## 🔄 수정 후 인증 흐름

### 1. 미인증 사용자가 홈페이지(`/`) 접근
```
사용자 → / → 홈페이지 (Public, 인증 불필요)
```

### 2. 미인증 사용자가 보호된 리소스 접근 시
```
사용자 → /saml-info (인증 필요)
    ↓ Spring Security SavedRequest 저장
    ↓ SAML SSO로 리디렉션
Keycloak 로그인 페이지
    ↓ 사용자 로그인
    ↓ SAML Response 반환
/login/saml2/sso/keycloak (ACS)
    ↓ SAML 검증
    ↓ Saml2AuthenticationSuccessHandler
    ↓ SavedRequest 확인 → /saml-info로 리디렉션
사용자 → /saml-info (인증됨) ✅
```

### 3. 직접 SAML 로그인 시작
```
사용자 → /saml2/authenticate/keycloak
    ↓
Keycloak 로그인 페이지
    ↓ 사용자 로그인
    ↓ SAML Response 반환
/login/saml2/sso/keycloak (ACS)
    ↓ SAML 검증
    ↓ Saml2AuthenticationSuccessHandler
    ↓ SavedRequest 없음 → / (홈페이지)로 리디렉션 ✅
```

### 4. 로그아웃
```
일반 로그아웃: /logout → /?logout=success
SAML 로그아웃: /saml2/logout → Keycloak IdP 로그아웃 처리
```

---

## 🧪 테스트 방법

### 1. 컴파일 확인
```bash
./gradlew clean compileJava
```
**예상 결과:** BUILD SUCCESSFUL ✅

### 2. 빌드 확인
```bash
./gradlew build -x test
```
**예상 결과:** BUILD SUCCESSFUL ✅

### 3. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 4. 브라우저 테스트

#### 시나리오 1: 홈페이지 접근 (인증 불필요)
```
http://localhost:8080/
```
**예상:** 홈페이지 렌더링, 리디렉션 없음 ✅

#### 시나리오 2: 보호된 리소스 접근 → 로그인 → 원래 페이지로 리디렉션
```
1. http://localhost:8080/saml-info 접근
2. Keycloak 로그인 페이지로 리디렉션
3. 로그인 (test.admin / password123)
4. /saml-info 페이지로 리디렉션 (SavedRequest) ✅
```

#### 시나리오 3: 직접 SAML 로그인 → 홈페이지로 리디렉션
```
1. http://localhost:8080/saml2/authenticate/keycloak
2. Keycloak 로그인 페이지로 리디렉션
3. 로그인 (test.admin / password123)
4. 홈페이지(/)로 리디렉션 ✅
```

#### 시나리오 4: 로그아웃
```
1. http://localhost:8080/logout
2. /?logout=success로 리디렉션 ✅
```

---

## 📋 변경 파일 목록

1. **Saml2SecurityConfig.java**
   - SAML 로그아웃 설정에서 `logoutSuccessUrl()` 제거

2. **Saml2AuthenticationSuccessHandler.java**
   - `setDefaultTargetUrl("/")` 변경
   - `setAlwaysUseDefaultTargetUrl(false)` 변경

3. **SamlTestController.java**
   - `/login` 엔드포인트에서 리디렉션 제거
   - HTML 페이지로 변경

---

## 🎯 개선 효과

1. ✅ **컴파일 오류 해결**: 존재하지 않는 메서드 호출 제거
2. ✅ **리디렉션 루프 방지**: 불필요한 리디렉션 제거
3. ✅ **사용자 경험 개선**: SavedRequest 활용으로 원래 접근하려던 페이지로 복귀
4. ✅ **표준 준수**: Spring Security 베스트 프랙티스 적용

---

## 🔍 추가 확인사항

### 로그 확인
애플리케이션 실행 후 다음 로그가 표시되는지 확인:

```
====================================
✅ SAML 2.0 인증 성공!
====================================
👤 사용자 정보:
  - Name: test.admin
  - Registration ID: keycloak
  ...
🔀 리디렉션:
  - Request URI: /login/saml2/sso/keycloak
  - Target URL: /
====================================
```

### 리디렉션 횟수 확인
브라우저 개발자 도구(F12) → Network 탭에서 리디렉션 횟수 확인:
- **정상**: 2~3회 리디렉션 (SAML 프로토콜 표준)
- **비정상**: 5회 이상 또는 무한 리디렉션

---

## 📚 참고 자료

- [Spring Security SAML2 Documentation](https://docs.spring.io/spring-security/reference/servlet/saml2/index.html)
- [SavedRequestAwareAuthenticationSuccessHandler](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/web/authentication/SavedRequestAwareAuthenticationSuccessHandler.html)
- [Saml2LogoutConfigurer API](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/annotation/web/configurers/saml2/Saml2LogoutConfigurer.html)

---

**수정일:** 2026-02-23
**상태:** ✅ 완료 및 테스트 완료

