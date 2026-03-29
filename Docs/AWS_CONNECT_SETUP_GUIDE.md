# AWS Connect 연동 설정 예시

## 1. application.yml 추가 설정

아래 설정을 `src/main/resources/application.yml`에 추가하세요:

```yaml
# =============================================================================
# AWS Connect 연동 설정
# =============================================================================
aws:
  connect:
    enabled: true
    region: ${AWS_REGION:us-east-1}  # AWS Connect 인스턴스가 위치한 리전
    instance-id: ${AWS_CONNECT_INSTANCE_ID}  # ARN 형식: arn:aws:connect:us-east-1:123456789012:instance/abcd-1234
    access-url: https://ssotest.my.connect.aws
    
    # AWS 자격증명 (환경변수로 오버라이드 권장)
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
    
    # EventBridge Webhook 엔드포인트
    event-webhook-path: /api/aws-connect/events
    
    # API Key (EventBridge → 백엔드 인증용)
    webhook-api-key: ${AWS_CONNECT_WEBHOOK_API_KEY:change-me-in-production}
```

## 2. AWS Connect Instance ID 확인 방법

### 방법 1: AWS Connect 콘솔에서 확인

1. AWS Connect 콘솔 접속: https://console.aws.amazon.com/connect/
2. 인스턴스 선택: `ssotest`
3. **Instance ARN** 복사 (예시: `arn:aws:connect:us-east-1:123456789012:instance/abcd-1234-efgh-5678`)

### 방법 2: AWS CLI로 확인

```bash
aws connect list-instances --region us-east-1

# 출력 예시:
# {
#   "InstanceSummaryList": [
#     {
#       "Id": "abcd-1234-efgh-5678",
#       "Arn": "arn:aws:connect:us-east-1:123456789012:instance/abcd-1234-efgh-5678",
#       "InstanceAlias": "ssotest",
#       "InstanceAccessUrl": "https://ssotest.my.connect.aws"
#     }
#   ]
# }
```

Instance ARN 전체를 `AWS_CONNECT_INSTANCE_ID` 환경변수에 설정하거나, `application.yml`에 직접 입력하세요.

## 3. Keycloak SAML 클라이언트 생성 (상세)

### Step 1: Keycloak Admin Console 접속

- URL: `http://1.224.162.188:51446/admin`
- Realm: `identity-system`

### Step 2: 클라이언트 생성

1. **Clients** 메뉴 클릭
2. **Create client** 버튼 클릭
3. **General Settings**:
   ```
   Client type: SAML
   Client ID: https://ssotest.my.connect.aws/connect/saml
   Name: AWS Connect SAML (선택사항)
   Description: AWS Connect SSO integration
   ```
4. **Save** 클릭

### Step 3: Client Settings 구성

**Settings 탭:**
```
Root URL: https://ssotest.my.connect.aws
Home URL: https://ssotest.my.connect.aws/connect/home
Valid redirect URIs: https://ssotest.my.connect.aws/*
IDP-Initiated SSO URL name: aws-connect
Master SAML Processing URL: https://ssotest.my.connect.aws/connect/saml
```

**SAML Capabilities:**
```
Name ID format: email
Force name ID format: ON
Force POST binding: OFF
Front channel logout: OFF
Force artifact binding: OFF
Sign documents: OFF
Sign assertions: ON
Signature algorithm: RSA_SHA256
SAML signature key name: KEY_ID
Canonicalization method: EXCLUSIVE
```

**Signature and Encryption:**
```
Client signature required: OFF
Encrypt assertions: OFF
```

**Save** 클릭

### Step 4: Mappers 추가

**Mapper 1: email**
```
Mapper Type: User Property
Property: email
Friendly Name: email
SAML Attribute Name: email
SAML Attribute NameFormat: Basic
```

**Mapper 2: username**
```
Mapper Type: User Property
Property: username
Friendly Name: username
SAML Attribute Name: username
SAML Attribute NameFormat: Basic
```

**Mapper 3: firstName**
```
Mapper Type: User Property
Property: firstName
Friendly Name: firstName
SAML Attribute Name: firstName
SAML Attribute NameFormat: Basic
```

**Mapper 4: lastName**
```
Mapper Type: User Property
Property: lastName
Friendly Name: lastName
SAML Attribute Name: lastName
SAML Attribute NameFormat: Basic
```

**Mapper 5: Role (AWS Connect Security Profile)**
```
Mapper Type: Hardcoded attribute
SAML Attribute Name: Role
SAML Attribute NameFormat: Basic
Attribute value: Agent  (또는 Admin, CallCenterManager 등)
```

> ⚠️ 실제 운영 시에는 User Attribute `connect_security_profile`을 읽어오도록 변경

### Step 5: IdP 메타데이터 다운로드

1. **Realm Settings** → **SAML 2.0 Identity Provider Metadata** 링크 클릭
2. URL: `http://1.224.162.188:51446/realms/identity-system/protocol/saml/descriptor`
3. XML 파일 저장 → AWS Connect에 업로드

## 4. AWS Connect SAML IdP 등록

### Step 1: AWS Connect 콘솔 접속

- URL: https://console.aws.amazon.com/connect/
- 인스턴스 `ssotest` 선택

### Step 2: Identity Management 설정

1. **Users** → **User management** (좌측 메뉴)
2. **Identity management** 탭
3. **Change** 버튼 클릭
4. **SAML 2.0-based authentication** 선택
5. **Next** 클릭

### Step 3: SAML 설정

1. **Upload SAML metadata document**:
   - Keycloak에서 다운로드한 XML 파일 업로드
   
2. **Relay state URL** (선택사항):
   ```
   https://ssotest.my.connect.aws/connect/home
   ```
   
3. **Add administrators**:
   - 기존 관리자 계정 유지 (SAML 설정 실패 대비)
   
4. **Save** 클릭

### Step 4: 사용자 추가

1. **Users** → **User management**
2. **Add new users** 클릭
3. **First name**: Admin
4. **Last name**: User
5. **Login name**: `test.admin` (Keycloak username과 동일)
6. **Email**: `admin@example.com` (Keycloak email과 동일)
7. **Routing Profile**: Basic Routing Profile
8. **Security Profiles**: Agent (또는 Admin)
9. **Save** 클릭

> 📌 **중요**: AWS Connect의 Login name은 Keycloak의 username과 **정확히 일치**해야 합니다.

## 5. 백엔드 CORS 설정 업데이트

`Saml2SecurityConfig.java`의 CORS 설정에 AWS Connect URL 추가:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(
        frontendUrl,                          // http://localhost:3000
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "https://ssotest.my.connect.aws"     // ← AWS Connect 추가
    ));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

## 6. 테스트 순서

### 6.1 SSO 로그인 테스트

1. 브라우저 시크릿 모드 열기
2. AWS Connect 로그인 URL 접속:
   ```
   https://ssotest.my.connect.aws/connect/login
   ```
3. Keycloak 로그인 페이지로 자동 리디렉션되는지 확인
4. Keycloak 계정 입력:
   - Username: `test.admin`
   - Password: (Keycloak에서 설정한 비밀번호)
5. AWS Connect CCP 화면이 나타나는지 확인

### 6.2 문제 해결

**증상 1: 로그인 후 "User not found" 에러**
- 원인: AWS Connect에 `test.admin` 사용자가 없음
- 해결: AWS Connect 콘솔에서 Login name = `test.admin`인 사용자 추가

**증상 2: SAML 에러 (Invalid signature)**
- 원인: Keycloak 서명 설정 문제
- 해결: Keycloak Client Settings에서 "Sign assertions: ON", "Client signature required: OFF" 확인

**증상 3: Keycloak으로 리디렉션 안 됨**
- 원인: AWS Connect SAML IdP 메타데이터 업로드 실패
- 해결: Keycloak IdP 메타데이터 XML 다시 다운로드 및 업로드

## 7. 환경변수 설정 (운영 환경)

`.env` 파일 또는 시스템 환경변수:

```bash
# AWS Connect
AWS_REGION=us-east-1
AWS_CONNECT_INSTANCE_ID=arn:aws:connect:us-east-1:123456789012:instance/abcd-1234
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS_CONNECT_WEBHOOK_API_KEY=your-secure-api-key-here

# 백엔드
APP_FRONTEND_URL=http://localhost:3000
APP_FRONTEND_LOGIN_SUCCESS_URL=http://localhost:3000
APP_FRONTEND_LOGOUT_SUCCESS_URL=http://localhost:3000/login
```

## 8. 다음 단계

- [x] AWS Connect Instance ID 확인
- [x] Keycloak SAML 클라이언트 생성
- [x] AWS Connect SAML IdP 등록
- [x] 테스트 사용자 추가
- [x] SSO 로그인 테스트
- [ ] 백엔드 AWS SDK 의존성 추가 (`build.gradle`)
- [ ] AWS Connect 통화 이벤트 수신 API 구현
- [ ] 프론트엔드 CCP 임베드

---

**작성일**: 2026-03-12  
**AWS Connect Instance**: `https://ssotest.my.connect.aws`

