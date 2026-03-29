# AWS Connect 연동 준비 완료 - 요약

> **작업 일시**: 2026-03-12  
> **AWS Connect Instance**: `https://ssotest.my.connect.aws`

---

## ✅ 완료된 작업

### 1. 문서 작성

| 문서 | 내용 |
|------|------|
| `AWS_CONNECT_INTEGRATION.md` | 전체 연동 가이드 (아키텍처, 구현 코드 예시) |
| `AWS_CONNECT_SETUP_GUIDE.md` | 실제 설정 단계별 가이드 (Keycloak, AWS 콘솔) |

### 2. 백엔드 설정

**`Saml2SecurityConfig.java` - CORS 설정 업데이트**
```java
configuration.setAllowedOrigins(List.of(
    frontendUrl,                        // http://localhost:3000
    "http://localhost:3000",
    "http://127.0.0.1:3000",
    "https://ssotest.my.connect.aws"   // ← AWS Connect CCP 임베드용
));
```

✅ **컴파일 성공**

---

## 📋 다음 단계 (우선순위 순)

### Phase 1: SAML SSO 설정 (AWS 콘솔 작업)

#### 1️⃣ AWS Connect Instance ID 확인

```bash
# AWS CLI 사용
aws connect list-instances --region us-east-1

# 또는 AWS 콘솔에서:
# https://console.aws.amazon.com/connect/ → ssotest 선택 → Instance ARN 복사
```

**필요한 정보:**
- Instance ARN: `arn:aws:connect:us-east-1:123456789012:instance/abcd-1234` (예시)
- Region: `us-east-1` (또는 실제 리전)

#### 2️⃣ Keycloak SAML 클라이언트 생성

**Keycloak Admin Console**: `http://1.224.162.188:51446/admin`

1. **Clients** → **Create client**
2. **Client ID**: `https://ssotest.my.connect.aws/connect/saml`
3. **Settings**:
   - Valid Redirect URIs: `https://ssotest.my.connect.aws/*`
   - Master SAML Processing URL: `https://ssotest.my.connect.aws/connect/saml`
   - Sign assertions: **ON**
   - Client signature required: **OFF**

4. **Mappers 추가** (필수):
   - `email` (User Property)
   - `username` (User Property)
   - `firstName` (User Property)
   - `lastName` (User Property)
   - `Role` (Hardcoded attribute: `Agent`)

5. **IdP 메타데이터 다운로드**:
   ```
   http://1.224.162.188:51446/realms/identity-system/protocol/saml/descriptor
   ```

#### 3️⃣ AWS Connect SAML IdP 등록

**AWS Connect 콘솔**: https://console.aws.amazon.com/connect/

1. 인스턴스 `ssotest` 선택
2. **Users** → **User management** → **Identity management** 탭
3. **Change** → **SAML 2.0-based authentication** 선택
4. Keycloak IdP 메타데이터 XML 업로드
5. **Relay State URL**: `https://ssotest.my.connect.aws/connect/home`

#### 4️⃣ AWS Connect에 테스트 사용자 추가

1. **Add new users** 클릭
2. **Login name**: `test.admin` (Keycloak username과 동일)
3. **Email**: `admin@example.com` (Keycloak email과 동일)
4. **Routing Profile**: Basic Routing Profile
5. **Security Profiles**: Agent

#### 5️⃣ SSO 로그인 테스트

```
https://ssotest.my.connect.aws/connect/login
```

1. 브라우저 시크릿 모드 열기
2. 위 URL 접속
3. Keycloak 로그인 페이지로 리디렉션 확인
4. `test.admin` / 비밀번호 입력
5. AWS Connect CCP 화면 로드 확인

---

### Phase 2: 백엔드 구현 (Spring Boot)

#### 1️⃣ AWS SDK 의존성 추가

**`build.gradle`:**
```gradle
dependencies {
    // AWS SDK for Java v2
    implementation platform('software.amazon.awssdk:bom:2.20.0')
    implementation 'software.amazon.awssdk:connect'
    implementation 'software.amazon.awssdk:eventbridge'
    
    // 기존 의존성...
}
```

#### 2️⃣ `application.yml` 설정 추가

```yaml
aws:
  connect:
    enabled: true
    region: us-east-1  # 실제 리전으로 변경
    instance-id: ${AWS_CONNECT_INSTANCE_ID}  # ARN 전체
    access-url: https://ssotest.my.connect.aws
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
    event-webhook-path: /api/aws-connect/events
    webhook-api-key: ${AWS_CONNECT_WEBHOOK_API_KEY:change-me}
```

#### 3️⃣ DB 스키마 추가 (Flyway Migration)

**`src/main/resources/db/migration/V4.0.0__aws_connect_integration.sql`:**
```sql
-- AWS Connect 통화 기록 테이블
CREATE TABLE aws_connect_call_records (
    contact_id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    agent_id VARCHAR(255) NOT NULL,
    
    customer_number VARCHAR(50),
    direction VARCHAR(20) NOT NULL,  -- INBOUND, OUTBOUND
    status VARCHAR(20) NOT NULL,     -- CONNECTED, ENDED, MISSED
    
    initiated_at TIMESTAMPTZ,
    connected_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    duration_seconds INTEGER,
    
    recording_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    CONSTRAINT fk_call_record_agent FOREIGN KEY (agent_id) 
        REFERENCES user_agents(agent_id) ON DELETE CASCADE
);

CREATE INDEX idx_call_records_agent ON aws_connect_call_records(agent_id);
CREATE INDEX idx_call_records_tenant ON aws_connect_call_records(tenant_id);
CREATE INDEX idx_call_records_initiated ON aws_connect_call_records(initiated_at DESC);

-- RBAC 권한 추가
INSERT INTO rbac_permissions (permission_id, tenant_id, code, name, description)
VALUES 
    (gen_random_uuid()::text, 'default-tenant', 'connect:view', '통화 기록 조회', 'AWS Connect 통화 기록 조회 권한'),
    (gen_random_uuid()::text, 'default-tenant', 'connect:manage', '통화 설정 관리', 'AWS Connect 설정 관리 권한');
```

#### 4️⃣ awsconnect 모듈 생성

```
src/main/java/com/identitymodulith/awsconnect/
├── application/
│   ├── dto/
│   │   ├── ConnectEventDto.java
│   │   ├── AgentStatusDto.java
│   │   └── CallRecordDto.java
│   └── service/
│       ├── ConnectEventService.java
│       ├── AgentStatusService.java
│       └── CallRecordService.java
├── domain/
│   └── model/
│       └── CallRecord.java
├── infrastructure/
│   ├── config/
│   │   └── AwsConnectConfig.java
│   └── persistence/
│       └── CallRecordJpaRepository.java
└── presentation/
    ├── ConnectEventController.java
    └── AgentStatusController.java
```

#### 5️⃣ 핵심 API 구현

**통화 이벤트 수신:**
```
POST /api/aws-connect/events
```

**상담사 상태 조회:**
```
GET /api/aws-connect/agent-status/me
```

**통화 기록 조회:**
```
GET /api/aws-connect/call-records?agentId={agentId}&from={date}&to={date}
```

---

### Phase 3: 프론트엔드 구현 (React)

#### 1️⃣ Amazon Connect Streams API 설치

```bash
npm install amazon-connect-streams
```

#### 2️⃣ CCP 컴포넌트 생성

```javascript
import 'amazon-connect-streams';

function ConnectCCP() {
  useEffect(() => {
    connect.core.initCCP(document.getElementById('ccp-container'), {
      ccpUrl: 'https://ssotest.my.connect.aws/connect/ccp-v2',
      loginPopup: true,
      softphone: { allowFramedSoftphone: true }
    });
    
    // 통화 이벤트 리스너
    connect.contact(function(contact) {
      contact.onConnected(() => {
        console.log('통화 연결:', contact.getContactId());
      });
    });
  }, []);
  
  return <div id="ccp-container" style={{ width: '400px', height: '600px' }} />;
}
```

---

## 🎯 테스트 시나리오

### 1. SSO 로그인 테스트

| 단계 | 액션 | 예상 결과 |
|------|------|----------|
| 1 | `https://ssotest.my.connect.aws/connect/login` 접속 | Keycloak 로그인 페이지로 리디렉션 |
| 2 | `test.admin` / 비밀번호 입력 | SAML Assertion 전송 |
| 3 | 로그인 완료 | AWS Connect CCP 화면 로드 |

### 2. 통화 이벤트 수신 테스트

| 단계 | 액션 | 예상 결과 |
|------|------|----------|
| 1 | AWS Connect에서 테스트 통화 발신 | EventBridge 이벤트 발생 |
| 2 | 백엔드 `/api/aws-connect/events` 호출됨 | 로그에 이벤트 수신 확인 |
| 3 | DB 확인 | `aws_connect_call_records` 테이블에 레코드 생성 |

---

## 📚 참고 문서

| 문서 | 경로 |
|------|------|
| 전체 연동 가이드 | `Docs/AWS_CONNECT_INTEGRATION.md` |
| 설정 단계별 가이드 | `Docs/AWS_CONNECT_SETUP_GUIDE.md` |
| 프론트엔드 개발 가이드 | `Docs/FRONTEND_GUIDE.md` |

---

## 🔧 환경변수 (운영 환경)

```bash
# AWS Connect
AWS_REGION=us-east-1
AWS_CONNECT_INSTANCE_ID=arn:aws:connect:us-east-1:123456789012:instance/abcd-1234
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS_CONNECT_WEBHOOK_API_KEY=your-secure-api-key

# 기존 설정
APP_FRONTEND_URL=http://localhost:3000
APP_FRONTEND_LOGIN_SUCCESS_URL=http://localhost:3000
APP_FRONTEND_LOGOUT_SUCCESS_URL=http://localhost:3000/login
```

---

## ✅ 체크리스트

**Phase 1: SAML SSO (AWS 콘솔)**
- [ ] AWS Connect Instance ID 확인
- [ ] Keycloak SAML 클라이언트 생성
- [ ] AWS Connect SAML IdP 등록
- [ ] 테스트 사용자 추가 (`test.admin`)
- [ ] SSO 로그인 테스트

**Phase 2: 백엔드 구현**
- [ ] AWS SDK 의존성 추가
- [ ] `application.yml` 설정
- [ ] DB 스키마 추가 (Flyway Migration)
- [ ] `awsconnect` 모듈 생성
- [ ] 통화 이벤트 수신 API 구현
- [ ] 상담사 상태 조회 API 구현

**Phase 3: 프론트엔드 구현**
- [ ] Amazon Connect Streams API 설치
- [ ] CCP 컴포넌트 구현
- [ ] 통화 이벤트 백엔드 동기화

---

**다음 단계**: Keycloak SAML 클라이언트 생성부터 시작하세요!  
**문서 위치**: `Docs/AWS_CONNECT_SETUP_GUIDE.md` (상세 설정 가이드)

