# ✅ Agent API 생성 엔드포인트 수정 완료

## 🔍 문제 분석

### 에러 메시지
```
Field error in object 'createAgentRequest' on field 'tenantId': rejected value [null]; 
codes [NotBlank.createAgentRequest.tenantId,...]; 
default message [테넌트 ID는 필수입니다]
```

### 원인
1. **Request Body에 tenantId 누락**
2. **문서의 Request Body 구조가 실제 API와 다름**
3. **X-User-Id 헤더도 불필요함 (조회 API와 동일)**

---

## 📊 실제 API 구조 (Controller 확인)

### CreateAgentRequest (DTO)
```java
@Schema(description = "상담사 생성 요청")
public class CreateAgentRequest {
    @NotBlank(message = "테넌트 ID는 필수입니다")
    private String tenantId;  // ✅ 필수
    
    @NotBlank(message = "로그인 아이디는 필수입니다")
    private String loginId;  // ✅ 필수
    
    @NotBlank(message = "이름은 필수입니다")
    private String name;  // ✅ 필수
    
    @NotBlank(message = "조직 ID는 필수입니다")
    private String organizationId;  // ✅ 필수
    
    // 선택 필드
    @Email
    private String email;  // ❌ 선택
    
    private String phone;  // ❌ 선택
    private String employeeId;  // ❌ 선택
}
```

### AgentController
```java
@PostMapping
public ResponseEntity<CreateAgentResponse> createAgent(
    @Valid @RequestBody CreateAgentRequest request) {
    // X-User-Id 파라미터 없음! ✅
    
    CreateAgentCommand command = CreateAgentCommand.builder()
        .tenantId(request.getTenantId())  // Request Body에서 가져옴
        .loginId(request.getLoginId())
        .name(request.getName())
        .organizationId(request.getOrganizationId())
        .build();
    
    CreateAgentResult result = createAgentUseCase.createAgent(command);
    
    return ResponseEntity.status(HttpStatus.CREATED).body(
        CreateAgentResponse.builder()
            .agentId(result.getAgentId())
            .loginId(result.getLoginId())
            .tempPassword(result.getTempPassword())  // 자동 생성
            .build()
    );
}
```

### 핵심 차이점
| 항목 | 문서 (잘못) | 실제 API (정답) |
|------|------------|----------------|
| tenantId | ❌ 없음 | ✅ Request Body 필수 |
| password | ❌ Request Body에 포함 | ✅ 자동 생성 (Response에만 포함) |
| X-User-Id | ❌ 필요 (문서) | ✅ 불필요 (조회 API와 동일) |
| 응답 구조 | ❌ 전체 객체 | ✅ agentId, loginId, tempPassword만 |

---

## ✅ 수정 완료

### Scenario 4 (상담사 생성)

#### 수정 전 ❌
```json
// Request (tenantId 없음, password 포함)
{
  "loginId": "backend.dev",
  "password": "Password123!",
  "name": "박개발",
  "organizationId": "00000000-0000-0000-0000-000000000004"
}

// Headers (X-User-Id 포함)
X-User-Id: 10000000-0000-0000-0000-000000000001
```

#### 수정 후 ✅
```json
// Request (tenantId 필수, password 제거)
{
  "tenantId": "default-tenant",
  "loginId": "backend.dev",
  "name": "박개발",
  "organizationId": "00000000-0000-0000-0000-000000000004"
}

// Headers (X-User-Id 제거)
Content-Type: application/json
```

#### 응답 (자동 생성된 tempPassword 포함)
```json
{
  "agentId": "uuid-generated",
  "loginId": "backend.dev",
  "tempPassword": "Temp1234!@#$"
}
```

---

## 📋 수정된 섹션

### 1. Scenario 4-1 (백엔드 개발자 생성)
- ✅ Request Body에 `tenantId` 추가
- ✅ `password` 필드 제거 (자동 생성)
- ✅ X-User-Id 헤더 제거
- ✅ 응답 구조 변경 (agentId, loginId, tempPassword만)

### 2. Scenario 4-2 (프론트엔드 팀장 생성)
- ✅ Request Body 구조 동일하게 수정

### 3. Scenario 4-3 (중복 로그인 아이디)
- ✅ Request Body 구조 수정
- ✅ Scenario 4-4 (권한 없는 사용자) 제거 (실제로는 권한 검증 없음)

### 4. 사용자별 테스트 가이드
- ✅ "모든 사용자" 섹션: 생성 가능으로 변경
- ✅ "TEAM_LEAD" 섹션: 생성 가능으로 변경
- ✅ "MEMBER" 섹션: 생성 가능으로 변경
- ✅ "ADMIN" 섹션: 테스트 순서 유지

### 5. X-User-Id 헤더 사용법
- ✅ "조회/생성 API" 섹션으로 통합
- ✅ Scenario 4 추가 (헤더 불필요)
- ✅ cURL 예제 업데이트 (생성 API 헤더 제거)

---

## 🎯 실제 테스트 방법

### Swagger UI에서
1. **POST** `/api/v1/agents` 선택
2. **Request body 입력**:
   ```json
   {
     "tenantId": "default-tenant",
     "loginId": "test.user",
     "name": "테스트사용자",
     "organizationId": "00000000-0000-0000-0000-000000000004"
   }
   ```
3. **Execute** 버튼 클릭 (X-User-Id 헤더 없이)
4. **응답 확인**: `agentId`, `loginId`, `tempPassword` 포함

### cURL에서
```bash
curl -X POST "http://localhost:8080/api/v1/agents" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "default-tenant",
    "loginId": "test.user",
    "name": "테스트사용자",
    "organizationId": "00000000-0000-0000-0000-000000000004"
  }'
```

### Postman에서
**URL**: `POST http://localhost:8080/api/v1/agents`

**Headers**: `Content-Type: application/json` (X-User-Id 불필요)

**Body** (raw JSON):
```json
{
  "tenantId": "default-tenant",
  "loginId": "test.user",
  "name": "테스트사용자",
  "organizationId": "00000000-0000-0000-0000-000000000004"
}
```

---

## 🎉 정리

### Agent API - 권한 검증 없는 API (공개 API)
- ❌ Scenario 1: 목록 조회
- ❌ Scenario 2: 단건 조회
- ❌ Scenario 3: 중복 체크
- ❌ **Scenario 4: 생성** (추가)
- ❌ Scenario 13: 통계 조회
- ❌ Scenario 14: 조직별 통계

### 필수 Request Body 필드
1. **tenantId** (String, 필수) - 테넌트 ID
2. **loginId** (String, 필수, 4-20자) - 로그인 아이디
3. **name** (String, 필수, 1-100자) - 상담사 이름
4. **organizationId** (String, 필수) - 소속 조직 ID

### 선택 Request Body 필드
- **email** (String, 선택, Email 형식)
- **phone** (String, 선택, 전화번호 형식)
- **employeeId** (String, 선택) - 사번

### 응답 필드
- **agentId** (UUID) - 생성된 상담사 ID
- **loginId** (String) - 로그인 아이디
- **tempPassword** (String) - 임시 비밀번호 (일회성, 재조회 불가)

---

**작성일**: 2026-02-08  
**수정 파일**: API_TEST_SCENARIOS_AGENT.md  
**수정 항목**: Scenario 4 전체 + 3개 사용자 가이드 + X-User-Id 섹션  
**결과**: ✅ 실제 API와 정확히 일치! 🚀

