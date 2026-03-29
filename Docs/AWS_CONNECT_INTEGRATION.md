# AWS Connect 연동 가이드

> **작성일**: 2026-03-12  
> **대상**: AWS Connect SSO 연동 및 CTI(Computer Telephony Integration) 구현

---

## 1. 연동 개요

### 1.1 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                     상담사 (Agent)                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
    ┌──────────────────┼──────────────────┐
    │                  │                  │
    ▼                  ▼                  ▼
┌─────────┐    ┌────────────────┐   ┌──────────────┐
│AWS      │    │  Keycloak IdP  │   │ 프론트엔드    │
│Connect  │◄───┤  (SAML 2.0)    │   │ (React 등)   │
│         │    └────────────────┘   └──────┬───────┘
│         │                                │
│         │         SAML SSO               │ HTTP API
│         │                                │
└────┬────┘                                │
     │                                     │
     │ EventBridge / API Gateway           │
     │                                     │
     ▼                                     ▼
┌─────────────────────────────────────────────────────────────┐
│        identity-modulith (Spring Boot :8080)                │
│                                                             │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  AWS Connect Integration Module (신규)                  │ │
│  │                                                         │ │
│  │  - ConnectEventListener (통화 이벤트 수신)               │ │
│  │  - AgentStatusController (상담사 상태 API)               │ │
│  │  - CallRecordService (통화 기록 저장)                    │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─────────┐  ┌─────────┐  ┌─────────────┐                │
│  │  user   │  │  rbac   │  │organization │                │
│  │  모듈   │  │  모듈   │  │   모듈      │                │
│  └─────────┘  └─────────┘  └─────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 연동 방식

| 구분 | 방식 | 설명 |
|------|------|------|
| **SSO 인증** | SAML 2.0 | Keycloak IdP ↔ AWS Connect |
| **통화 이벤트** | Amazon EventBridge | AWS Connect → 백엔드 (Webhook) |
| **상담사 상태** | REST API | 프론트엔드 ↔ 백엔드 ↔ AWS Connect SDK |
| **통화 기록** | DB 저장 | ContactFlowEvent → PostgreSQL |

---

## 2. AWS Connect SAML SSO 설정

### 2.1 Keycloak 설정

#### Step 1: AWS Connect용 SAML 클라이언트 생성

> ⚠️ **중요**:
> - `signin.aws.amazon.com/saml` → AWS IAM SAML (AWS 콘솔 로그인용) ❌
> - `ssotest.my.connect.aws/saml` → AWS Connect 전용 SAML ✅
>
> AWS Connect SAML은 IAM SAML과 **다른 엔드포인트**를 사용합니다.

**현재 인스턴스 (`ssotest`) 기준 올바른 값:**

| 항목 | 값 |
|------|-----|
| **Client ID (SP Entity ID)** | `https://ssotest.my.connect.aws` |
| **ACS URL (Master SAML Processing URL)** | `https://ssotest.my.connect.aws/saml` |
| **Valid Redirect URIs** | `https://ssotest.my.connect.aws/*` |
| **Name ID Format** | `email` |
| **IDP Initiated SSO Relay State** | `https://ssotest.my.connect.aws/home` |

Keycloak Admin Console에서:

1. **Clients** → **Create Client** (또는 기존 클라이언트 수정)
   - Client ID: `https://ssotest.my.connect.aws`
   - Client Protocol: `saml`
   
2. **Settings 탭**
   - Valid Redirect URIs: `https://ssotest.my.connect.aws/*`
   - Master SAML Processing URL: `https://ssotest.my.connect.aws/saml`
   - IDP Initiated SSO URL Name: `aws-connect`
   - **IDP Initiated SSO Relay State**: `https://ssotest.my.connect.aws/home`
   - **Name ID Format**: `email`
   
3. **SAML Keys 탭**
   - Client Signature Required: `OFF`
   - Sign Assertions: `ON`
   - Sign Documents: `OFF`

#### Step 2: SAML Attribute Mapper 추가

AWS Connect는 다음 SAML Attribute를 요구합니다:

| Attribute Name | Keycloak User Property | 설명 |
|----------------|----------------------|------|
| `email` | `email` | 상담사 이메일 (필수) |
| `username` | `username` | 로그인 ID |
| `firstName` | `firstName` | 이름 |
| `lastName` | `lastName` | 성 |
| `Role` | User Attribute: `connect_security_profile` | AWS Connect 보안 프로필 |

**Mapper 추가 예시 (Role):**
```
Name: aws-connect-role
Mapper Type: User Attribute
User Attribute: connect_security_profile
SAML Attribute Name: Role
SAML Attribute NameFormat: Basic
```

#### Step 3: IdP 메타데이터 다운로드

```
URL: http://1.224.162.188:51446/realms/identity-system/protocol/saml/descriptor
```

이 URL의 XML을 저장하여 AWS Connect에 업로드합니다.

### 2.2 AWS Connect 설정

#### Step 1: SAML 2.0 IdP 등록

AWS Connect 콘솔:

1. **User Management** → **Identity Management**
2. **SAML 2.0-based authentication** 선택
3. Keycloak IdP 메타데이터 XML 업로드
4. **Relay State URL**: `https://ssotest.my.connect.aws/home`

#### Step 2: 사용자 계정 매핑

AWS Connect는 SAML `email` 또는 `username`으로 사용자를 매핑합니다.

| Keycloak username | AWS Connect User | identity-modulith login_id |
|-------------------|------------------|----------------------------|
| `test.admin` | `test.admin@example.com` | `test.admin` |

**중요:** 
- Keycloak username = identity-modulith `login_id`
- AWS Connect User의 이메일은 Keycloak `email`과 일치

#### Step 3: 보안 프로필 할당

AWS Connect에서:
- **Security Profiles** → 생성 (예: `Agent`, `Supervisor`, `Admin`)
- Keycloak User Attribute `connect_security_profile`에 보안 프로필 이름 설정

---

## 3. 백엔드 구현 (identity-modulith)

### 3.1 의존성 추가 (`build.gradle`)

```gradle
dependencies {
    // AWS SDK for Java v2
    implementation platform('software.amazon.awssdk:bom:2.20.0')
    implementation 'software.amazon.awssdk:connect'
    implementation 'software.amazon.awssdk:eventbridge'
    
    // 기존 의존성...
}
```

### 3.2 모듈 구조

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
│   ├── model/
│   │   ├── CallRecord.java
│   │   └── AgentStatus.java (enum)
│   └── repository/
│       └── CallRecordRepository.java
├── infrastructure/
│   ├── config/
│   │   └── AwsConnectConfig.java
│   └── persistence/
│       └── CallRecordJpaRepository.java
└── presentation/
    ├── ConnectEventController.java
    └── AgentStatusController.java
```

### 3.3 `application.yml` 설정

```yaml
aws:
  connect:
    enabled: true
    region: ap-northeast-2  # 서울 리전
    instance-id: arn:aws:connect:ap-northeast-2:123456789012:instance/abcd-1234
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
    event-webhook-path: /api/aws-connect/events  # EventBridge → 이 엔드포인트
```

### 3.4 주요 구현 내용

#### 3.4.1 통화 이벤트 수신 (EventBridge Webhook)

**`ConnectEventController.java`**

```java
@RestController
@RequestMapping("/api/aws-connect/events")
@RequiredArgsConstructor
@Slf4j
public class ConnectEventController {

    private final ConnectEventService connectEventService;

    /**
     * AWS Connect ContactFlowEvent 수신
     * EventBridge에서 호출
     */
    @PostMapping
    public ResponseEntity<Void> handleConnectEvent(
            @RequestBody String eventJson,
            @RequestHeader("X-Amz-Sns-Message-Type") String messageType) {
        
        log.info("[AWS Connect] Event 수신 - type: {}", messageType);
        
        if ("SubscriptionConfirmation".equals(messageType)) {
            // SNS Topic 구독 확인
            connectEventService.confirmSubscription(eventJson);
            return ResponseEntity.ok().build();
        }
        
        // 통화 이벤트 처리
        connectEventService.processEvent(eventJson);
        return ResponseEntity.ok().build();
    }
}
```

#### 3.4.2 상담사 상태 조회/변경 API

**`AgentStatusController.java`**

```java
@RestController
@RequestMapping("/api/aws-connect/agent-status")
@RequiredArgsConstructor
public class AgentStatusController {

    private final AgentStatusService agentStatusService;

    /**
     * 현재 상담사 상태 조회
     */
    @GetMapping("/me")
    public ResponseEntity<AgentStatusDto> getMyStatus() {
        String agentId = getCurrentAgentId(); // SecurityContext에서 추출
        AgentStatusDto status = agentStatusService.getAgentStatus(agentId);
        return ResponseEntity.ok(status);
    }

    /**
     * 상담사 상태 변경 (Available, After Call Work, Break 등)
     */
    @PutMapping("/me")
    public ResponseEntity<Void> updateMyStatus(
            @RequestBody @Valid UpdateAgentStatusRequest request) {
        
        String agentId = getCurrentAgentId();
        agentStatusService.updateAgentStatus(agentId, request.getStatus());
        return ResponseEntity.ok().build();
    }
}
```

**응답 예시:**
```json
{
  "agentId": "test.admin",
  "status": "AVAILABLE",
  "statusStartTime": "2026-03-12T10:30:00Z",
  "currentContactId": null,
  "routingProfileArn": "arn:aws:connect:...:routing-profile/basic-routing"
}
```

#### 3.4.3 통화 기록 저장

**`CallRecord.java` (Domain Entity)**

```java
@Entity
@Table(name = "aws_connect_call_records")
@Getter
public class CallRecord {
    @Id
    private String contactId;  // AWS Connect Contact ID
    
    private String tenantId;
    private String agentId;    // user_agents.agent_id (FK)
    
    private String customerNumber;
    private String direction;  // INBOUND, OUTBOUND
    
    @Enumerated(EnumType.STRING)
    private CallStatus status;  // CONNECTED, ENDED, MISSED
    
    private LocalDateTime initiatedAt;
    private LocalDateTime connectedAt;
    private LocalDateTime endedAt;
    
    private Integer durationSeconds;
    private String recordingUrl;  // S3 URL
    
    // ...
}
```

**`CallRecordService.java`**

```java
@Service
@RequiredArgsConstructor
@Transactional
public class CallRecordService {

    private final CallRecordRepository callRecordRepository;

    public void saveCallRecord(ConnectEventDto event) {
        CallRecord record = CallRecord.builder()
            .contactId(event.getContactId())
            .tenantId(extractTenantId(event.getAgentArn()))
            .agentId(extractAgentId(event.getAgentArn()))
            .customerNumber(event.getCustomerEndpoint().getAddress())
            .direction(event.getInitiationMethod())
            .status(CallStatus.fromConnectEvent(event.getEventType()))
            .initiatedAt(event.getInitiationTimestamp())
            .build();
        
        callRecordRepository.save(record);
    }
}
```

#### 3.4.4 AWS Connect SDK 연동

**`AgentStatusService.java`**

```java
@Service
@RequiredArgsConstructor
public class AgentStatusService {

    private final ConnectClient connectClient;
    private final AwsConnectConfig config;

    public AgentStatusDto getAgentStatus(String agentId) {
        GetCurrentUserDataRequest request = GetCurrentUserDataRequest.builder()
            .instanceId(config.getInstanceId())
            .filters(Filters.builder()
                .agents(List.of(agentId))
                .build())
            .build();
        
        GetCurrentUserDataResponse response = connectClient.getCurrentUserData(request);
        
        UserData userData = response.userDataList().get(0);
        return AgentStatusDto.from(userData);
    }
    
    public void updateAgentStatus(String agentId, AgentStatus newStatus) {
        // AWS Connect API로 상담사 상태 변경
        // (실제로는 CCP에서 변경하는 것이 일반적)
    }
}
```

#### 3.4.5 AWS SDK 설정

**`AwsConnectConfig.java`**

```java
@Configuration
@EnableConfigurationProperties(AwsConnectProperties.class)
public class AwsConnectConfig {

    @Bean
    public ConnectClient connectClient(AwsConnectProperties props) {
        return ConnectClient.builder()
            .region(Region.of(props.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    props.getAccessKey(),
                    props.getSecretKey()
                )
            ))
            .build();
    }
}
```

---

## 4. EventBridge 설정 (AWS 콘솔)

### 4.1 EventBridge Rule 생성

1. **EventBridge 콘솔** → **Rules** → **Create rule**
2. **Event pattern:**
   ```json
   {
     "source": ["aws.connect"],
     "detail-type": ["Amazon Connect Contact Event"]
   }
   ```
3. **Target:**
   - Type: `API destination`
   - URL: `https://your-backend.com/api/aws-connect/events`
   - HTTP method: `POST`
   - Authentication: `API Key` 또는 `OAuth`

### 4.2 대안: SNS → HTTP(S) Subscription

EventBridge 대신 SNS Topic을 사용할 수도 있습니다:

1. AWS Connect → **Contact Flows** → Contact Flow에서 **Set recording and analytics behavior** 블록 추가
2. SNS Topic ARN 설정
3. SNS Topic → **Subscriptions** → **HTTPS** 추가
   - Endpoint: `https://your-backend.com/api/aws-connect/events`

---

## 5. 프론트엔드 연동 (CCP Embedded)

### 5.1 Amazon Connect Streams API

프론트엔드에 AWS Connect CCP(Contact Control Panel)를 임베드하여 상담사가 전화를 받고 걸 수 있습니다.

**React 예시:**

```javascript
import 'amazon-connect-streams';

function ConnectCCP() {
  useEffect(() => {
    const ccpUrl = 'https://ssotest.my.connect.aws/ccp-v2';
    
    connect.core.initCCP(document.getElementById('ccp-container'), {
      ccpUrl: ccpUrl,
      loginPopup: true,               // SSO 팝업
      softphone: {
        allowFramedSoftphone: true
      }
    });
    
    // 통화 이벤트 수신
    connect.contact(function(contact) {
      contact.onConnected(function() {
        console.log('통화 연결됨:', contact.getContactId());
        // 백엔드 API 호출 (통화 정보 동기화)
        axios.post('/api/aws-connect/sync-call', {
          contactId: contact.getContactId()
        });
      });
      
      contact.onEnded(function() {
        console.log('통화 종료됨');
      });
    });
  }, []);
  
  return <div id="ccp-container" style={{ width: '400px', height: '600px' }} />;
}
```

### 5.2 필요한 CORS 설정

AWS Connect CCP는 iframe으로 로드되므로 백엔드 CORS 설정에 추가:

```java
configuration.setAllowedOrigins(List.of(
    frontendUrl,
    "https://ssotest.my.connect.aws"  // ← AWS Connect 추가
));
```

---

## 6. DB 스키마 추가

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
```

---

## 7. 보안 고려사항

### 7.1 EventBridge Webhook 인증

EventBridge/SNS에서 백엔드로 이벤트를 전송할 때 인증:

1. **API Key 검증**
   ```java
   @PostMapping
   public ResponseEntity<Void> handleConnectEvent(
           @RequestHeader("X-Api-Key") String apiKey,
           @RequestBody String eventJson) {
       
       if (!apiKey.equals(expectedApiKey)) {
           throw new UnauthorizedException();
       }
       // ...
   }
   ```

2. **SNS 메시지 서명 검증**
   ```java
   // AWS SDK로 SNS 메시지 서명 검증
   AmazonSNS snsClient = AmazonSNSClientBuilder.defaultClient();
   // 서명 검증 로직...
   ```

### 7.2 RBAC 권한

AWS Connect 관련 API에 권한 추가:

```sql
INSERT INTO rbac_permissions (permission_id, tenant_id, code, name, description)
VALUES 
    ('perm-connect-view', 'default-tenant', 'connect:view', '통화 기록 조회', 'AWS Connect 통화 기록 조회 권한'),
    ('perm-connect-manage', 'default-tenant', 'connect:manage', '통화 설정 관리', 'AWS Connect 설정 관리 권한');
```

---

## 8. 테스트 시나리오

### 8.1 SSO 로그인 테스트

1. AWS Connect 로그인 URL 접속: `https://ssotest.my.connect.aws/login`
2. Keycloak 로그인 페이지로 리디렉션 확인
3. `test.admin` / 비밀번호 입력
4. AWS Connect CCP 화면 로드 확인

### 8.2 통화 이벤트 수신 테스트

1. AWS Connect에서 테스트 통화 발신
2. 백엔드 `/api/aws-connect/events` 엔드포인트 호출 확인
3. DB `aws_connect_call_records` 테이블에 레코드 생성 확인

### 8.3 상담사 상태 API 테스트

```bash
# 현재 상태 조회
curl -X GET http://localhost:8080/api/aws-connect/agent-status/me \
  -H "Cookie: JSESSIONID=..." \
  -H "Content-Type: application/json"

# 상태 변경 (Break)
curl -X PUT http://localhost:8080/api/aws-connect/agent-status/me \
  -H "Cookie: JSESSIONID=..." \
  -H "Content-Type: application/json" \
  -d '{"status": "BREAK"}'
```

---

## 9. 포트폴리오 작성 포인트

### 9.1 기술 스택

- **SAML 2.0 SSO**: Keycloak IdP ↔ AWS Connect SP 연동
- **AWS SDK for Java v2**: ConnectClient, EventBridge
- **실시간 통화 이벤트 처리**: EventBridge → Webhook
- **CTI 통합**: Amazon Connect Streams API (프론트엔드)

### 9.2 구현 내용

| 항목 | 내용 |
|------|------|
| SSO 인증 | SAML 2.0 기반 AWS Connect 로그인 |
| 통화 이벤트 | EventBridge Webhook으로 실시간 수신 |
| 상담사 상태 | AWS Connect SDK로 상태 조회/변경 |
| 통화 기록 | PostgreSQL에 저장 (분석/리포트용) |
| CCP 임베드 | React에 Amazon Connect CCP 통합 |

### 9.3 비즈니스 가치

- **상담사 SSO**: 별도 로그인 없이 AWS Connect 접근 (UX 개선)
- **통화 기록 통합**: 콜센터 통화 이력을 identity-modulith DB에 중앙화
- **상담사 관리 일원화**: user 모듈에서 상담사 정보 + AWS Connect 상태 통합 관리

---

## 10. 참고 자료

- [AWS Connect SAML 2.0 통합](https://docs.aws.amazon.com/connect/latest/adminguide/configure-saml.html)
- [Amazon Connect Streams API](https://github.com/amazon-connect/amazon-connect-streams)
- [EventBridge를 사용한 Contact Event 수신](https://docs.aws.amazon.com/connect/latest/adminguide/contact-events.html)
- [AWS SDK for Java v2 - Connect](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/connect/package-summary.html)

---

**작성일**: 2026-03-12  
**다음 단계**: AWS Connect 인스턴스 생성 → Keycloak SAML 클라이언트 설정 → 백엔드 모듈 구현






