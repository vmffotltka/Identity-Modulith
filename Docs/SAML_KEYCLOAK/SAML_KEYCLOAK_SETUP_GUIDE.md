# Keycloak SAML 2.0 연동 설정 가이드

## 🎯 현재 상황 요약

### 해결된 문제들

1. ✅ **컴파일 오류 수정** - 테스트 코드의 메서드 시그니처 불일치 해결
2. ✅ **불필요한 로그 오류 제거** - `favicon.ico`, `.well-known` 관련 404 오류 무시 처리
3. ✅ **SAML AuthnRequest 서명 비활성화** - Keycloak 400 Bad Request 오류 해결

### 남아있는 작업

- ⚠️ **Keycloak 클라이언트 설정 필요** (아래 참조)

---

## 📋 Keycloak 클라이언트 설정 가이드

### 1. Keycloak 관리 콘솔 접속

```
URL: http://1.224.162.188:51446
Realm: identity-system
```

### 2. SAML 클라이언트 생성/수정

#### 기본 설정

| 설정 항목 | 값 |
|---------|---|
| **Client ID** | `http://localhost:8080/saml2/service-provider-metadata/keycloak` |
| **Client Protocol** | `saml` |
| **Name** | Identity Modulith SP |

#### SAML 설정 (Settings 탭)

| 설정 항목 | 값 | 설명 |
|---------|---|------|
| **Valid Redirect URIs** | `http://localhost:8080/login/saml2/sso/keycloak` | SAML Response 수신 URL |
| **Base URL** | `http://localhost:8080` | 애플리케이션 기본 URL |
| **Master SAML Processing URL** | `http://localhost:8080/login/saml2/sso/keycloak` | 통합 처리 URL |
| **⚠️ Client Signature Required** | **OFF** | 🔴 **매우 중요!** AuthnRequest 서명 검증 비활성화 |
| **Force POST Binding** | OFF | HTTP-Redirect 바인딩 허용 |
| **Front Channel Logout** | ON | 로그아웃 지원 |
| **Include AuthnStatement** | ON | 인증 정보 포함 |
| **Sign Documents** | ON | Assertion에 서명 |
| **Sign Assertions** | ON | Assertion 서명 |

### 3. SAML 속성 매핑 (Mappers 탭)

다음 속성들을 매핑해야 합니다:

| Mapper Name | Mapper Type | Property | SAML Attribute Name | Friendly Name |
|------------|-------------|----------|---------------------|---------------|
| username | User Property | username | username | Username |
| email | User Property | email | email | Email |
| firstName | User Property | firstName | firstName | First Name |
| lastName | User Property | lastName | lastName | Last Name |
| roles | Role list | - | roles | Roles |

### 4. 역할(Roles) 설정

Keycloak에 다음 역할들을 생성하세요:

- `ADMIN` - 시스템 관리자
- `USER` - 일반 사용자
- `MANAGER` - 매니저

---

## 🧪 테스트 방법

### 1. 애플리케이션 시작

```powershell
cd "C:\Users\vmffo\Desktop\회사 자료\프로젝트 폴더\identity-modulith"
.\gradlew bootRun
```

### 2. SAML 로그인 테스트

1. 브라우저에서 `http://localhost:8080/saml2/authenticate/keycloak` 접속
2. Keycloak 로그인 페이지로 리다이렉트 확인
3. Keycloak에서 로그인 (테스트 사용자)
4. 성공 시 `http://localhost:8080/?login=success` 리다이렉트 확인

### 3. 인증 정보 확인

```
GET http://localhost:8080/saml-info
```

**응답 예시:**
```json
{
  "authenticated": true,
  "nameId": "testuser",
  "attributes": {
    "username": ["testuser"],
    "email": ["testuser@example.com"],
    "firstName": ["Test"],
    "lastName": ["User"],
    "roles": ["ADMIN", "USER"]
  }
}
```

### 4. 로그아웃 테스트

```
GET http://localhost:8080/logout
```

- 세션 무효화 확인
- `http://localhost:8080/?logout=success` 리다이렉트 확인

---

## 🚨 문제 해결 (Troubleshooting)

### 문제 1: Keycloak에서 400 Bad Request

**원인:** Keycloak "Client Signature Required"가 ON이지만 SP가 서명하지 않음

**해결:**
1. Keycloak 클라이언트 설정에서 **Client Signature Required: OFF** 설정
2. 애플리케이션 재시작

### 문제 2: Invalid Redirect URI

**원인:** Keycloak Valid Redirect URIs 설정 불일치

**해결:**
1. Keycloak에서 정확히 `http://localhost:8080/login/saml2/sso/keycloak` 추가
2. 와일드카드 사용 시: `http://localhost:8080/*`

### 문제 3: No RelyingPartyRegistration found

**원인:** SAML 메타데이터 로드 실패 또는 Registration ID 불일치

**해결:**
1. Keycloak 메타데이터 URL 확인: `http://1.224.162.188:51446/realms/identity-system/protocol/saml/descriptor`
2. Registration ID는 `keycloak`로 고정

### 문제 4: Assertion 복호화 실패

**원인:** Keycloak이 Assertion을 암호화했지만 SP에 복호화 키가 없음

**해결:**
1. Keycloak 클라이언트 설정에서 **Encrypt Assertions: OFF**
2. 또는 SP 메타데이터에 공개키 등록

---

## 📝 로그 확인

### 애플리케이션 시작 시 로그

```
2026-02-23T21:00:27.738+09:00  INFO ... SAML 2.0 RelyingPartyRegistration 초기화 시작
2026-02-23T21:00:29.617+09:00  INFO ... ✅ 자체 서명 인증서 생성 완료 (개발/테스트용)
2026-02-23T21:00:32.295+09:00  INFO ... ✅ SAML 2.0 RelyingPartyRegistration 초기화 성공
2026-02-23T21:00:32.295+09:00  INFO ... - Registration ID: keycloak
2026-02-23T21:00:32.295+09:00  INFO ... - AuthnRequest Signing: DISABLED (서명 안 함)
2026-02-23T21:00:32.295+09:00  INFO ... 📝 Keycloak 클라이언트 필수 설정:
2026-02-23T21:00:32.295+09:00  INFO ...    3. ⚠️  Client Signature Required: OFF  (매우 중요!)
```

### SAML 로그인 요청 시 로그

```
2026-02-23T21:00:40.511+09:00  INFO ... 🏠 홈 페이지 접근
2026-02-23T21:00:40.511+09:00  INFO ... ❌ 인증 정보 없음 (미인증 사용자)
```

---

## 🔐 보안 고려사항

### 개발 환경 (현재)

- ✅ HTTP 사용 (HTTPS 불필요)
- ✅ AuthnRequest 서명 비활성화 (간단한 테스트)
- ✅ 자체 서명 인증서 사용

### 프로덕션 환경 (권장)

- 🔒 **HTTPS 필수** (SAML은 민감한 정보 전송)
- 🔒 **AuthnRequest 서명 활성화** (보안 강화)
- 🔒 **공인 인증서 사용** (Let's Encrypt 등)
- 🔒 **Assertion 암호화 활성화** (데이터 보호)

---

## 📚 참고 자료

- [Spring Security SAML 공식 문서](https://docs.spring.io/spring-security/reference/servlet/saml2/index.html)
- [Keycloak SAML 클라이언트 설정](https://www.keycloak.org/docs/latest/server_admin/#saml-clients)
- [SAML 2.0 스펙](http://docs.oasis-open.org/security/saml/Post2.0/sstc-saml-tech-overview-2.0.html)

---

## ✅ 체크리스트

애플리케이션 배포 전 확인사항:

- [ ] Keycloak 클라이언트 생성 완료
- [ ] Client Signature Required: OFF 설정
- [ ] Valid Redirect URIs 설정 완료
- [ ] SAML 속성 매핑 완료 (username, email, roles)
- [ ] 테스트 사용자 생성 및 역할 할당
- [ ] SAML 로그인 테스트 성공
- [ ] 인증 정보 확인 (/saml-info)
- [ ] 로그아웃 테스트 성공

---

**작성일:** 2026-02-23  
**버전:** 1.0  
**상태:** ✅ 준비 완료 (Keycloak 설정만 남음)

