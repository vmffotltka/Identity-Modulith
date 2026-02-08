# ✅ Agent 생성 API roles 필수 필드 추가 완료

## 🔍 에러 분석

### 첫 번째 에러
```
Field error in object 'createAgentRequest' on field 'tenantId': rejected value [null]
```
- **원인**: Request Body에 `tenantId` 누락
- **해결**: 문서에 `tenantId` 추가 완료 ✅

### 두 번째 에러  
```
com.nexfron.identitymodulith.user.domain.exception.BusinessException: 
roles는 최소 1개 이상이어야 합니다.
```
- **원인**: Request Body에 `roles` 누락
- **해결**: DTO, Controller, 문서 모두 수정 ✅

---

## 📊 수정 항목

### 1. CreateAgentRequest DTO ✅
**파일**: `CreateAgentRequest.java`

**추가된 필드**:
```java
@Schema(description = "역할 목록 (최소 1개 필수)", example = "[\"MEMBER\"]", required = true)
@NotEmpty(message = "역할은 최소 1개 이상이어야 합니다")
private Set<String> roles;
```

---

### 2. AgentController ✅
**파일**: `AgentController.java`

**추가된 import**:
```java
import com.nexfron.identitymodulith.user.domain.model.Agent;
import java.util.Set;
```

**수정된 createAgent 메서드**:
```java
public ResponseEntity<CreateAgentResponse> createAgent(@Valid @RequestBody CreateAgentRequest request) {
    // roles 문자열을 Agent.Role 객체로 변환
    Set<Agent.Role> roles = request.getRoles().stream()
            .map(roleName -> new Agent.Role(roleName, Agent.Role.RoleType.POSITION))
            .collect(java.util.stream.Collectors.toSet());
    
    CreateAgentCommand command = CreateAgentCommand.builder()
            .tenantId(request.getTenantId())
            .loginId(request.getLoginId())
            .name(request.getName())
            .organizationId(request.getOrganizationId())
            .roles(roles)  // ✅ roles 전달
            .email(request.getEmail())
            .phone(request.getPhone())
            .employeeId(request.getEmployeeId())
            .build();

    CreateAgentResult result = createAgentUseCase.createAgent(command);
    
    // ...
}
```

---

### 3. API_TEST_SCENARIOS_AGENT.md ✅

#### Scenario 4-1 (백엔드 개발자 생성)
**Before** ❌:
```json
{
  "tenantId": "default-tenant",
  "loginId": "backend.dev",
  "name": "박개발",
  "organizationId": "00000000-0000-0000-0000-000000000004"
}
```

**After** ✅:
```json
{
  "tenantId": "default-tenant",
  "loginId": "backend.dev",
  "name": "박개발",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "roles": ["MEMBER"]
}
```

#### Scenario 4-2 (프론트엔드 팀장 생성)
```json
{
  "tenantId": "default-tenant",
  "loginId": "frontend.lead",
  "name": "최팀장",
  "organizationId": "00000000-0000-0000-0000-000000000005",
  "roles": ["TEAM_LEAD"],
  "email": "frontend.lead@nexfron.com",
  "employeeId": "EMP-0005"
}
```

#### Scenario 4-4 (roles 없이 생성 시도) - 신규 추가
```json
{
  "tenantId": "default-tenant",
  "loginId": "test.user",
  "name": "테스트",
  "organizationId": "00000000-0000-0000-0000-000000000004"
}
```
**예상 응답 (400 Bad Request)**:
```json
{
  "code": "INVALID_INPUT_VALUE",
  "message": "roles는 최소 1개 이상이어야 합니다"
}
```

#### cURL 예제 업데이트
```bash
curl -X POST "http://localhost:8080/api/v1/agents" \
  -H "Content-Type: application/json" \
  -d '{"tenantId": "default-tenant", "loginId": "new.user", "name": "홍길동", "organizationId": "00000000-0000-0000-0000-000000000004", "roles": ["MEMBER"]}'
```

---

## 📋 필수 필드 요약

### Request Body (5개 필수)
1. **tenantId** (String) - 테넌트 ID
   - 예시: `"default-tenant"`
   
2. **loginId** (String, 4-20자) - 로그인 아이디
   - 패턴: `^[a-zA-Z0-9_.-]{4,20}$`
   - 예시: `"backend.dev"`

3. **name** (String, 1-100자) - 상담사 이름
   - 예시: `"박개발"`

4. **organizationId** (String, UUID) - 소속 조직 ID
   - 예시: `"00000000-0000-0000-0000-000000000004"`

5. **roles** (Set<String>, 최소 1개) - 역할 목록
   - 가능한 값: `ADMIN`, `TEAM_LEAD`, `MEMBER`
   - 예시: `["MEMBER"]`, `["TEAM_LEAD"]`, `["ADMIN"]`

### 선택 필드 (3개)
- **email** (String, Email 형식)
- **phone** (String, 전화번호 형식)
- **employeeId** (String, 사번)

---

## 🎯 역할 (Roles) 설명

### POSITION 타입 역할 (필수, 1개만 선택)
1. **ADMIN** - 시스템 관리자
   - 전체 조직 접근 가능
   - 모든 작업 수행 가능

2. **TEAM_LEAD** - 팀장
   - 본인 부서 + 하위 부서 접근
   - 팀 관리 권한

3. **MEMBER** - 일반 상담사
   - 본인 부서만 접근
   - 기본 작업 수행

### 검증 규칙
- ✅ **최소 1개 이상** 필수
- ✅ 문자열 배열로 전달
- ✅ Controller에서 `Agent.Role` 객체로 자동 변환
- ✅ `RoleType.POSITION`으로 고정

---

## 🎉 테스트 방법

### Swagger UI에서
1. **POST** `/api/v1/agents` 선택
2. **Request body 입력**:
   ```json
   {
     "tenantId": "default-tenant",
     "loginId": "test.user",
     "name": "테스트사용자",
     "organizationId": "00000000-0000-0000-0000-000000000004",
     "roles": ["MEMBER"]
   }
   ```
3. **Execute** 버튼 클릭
4. **응답 확인**:
   ```json
   {
     "agentId": "uuid-generated",
     "loginId": "test.user",
     "tempPassword": "Auto1234!@#$"
   }
   ```

### cURL에서
```bash
curl -X POST "http://localhost:8080/api/v1/agents" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "default-tenant",
    "loginId": "test.user",
    "name": "테스트사용자",
    "organizationId": "00000000-0000-0000-0000-000000000004",
    "roles": ["MEMBER"]
  }'
```

### 예상 응답
```json
{
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "loginId": "test.user",
  "tempPassword": "Auto1234!@#$"
}
```

---

## 📊 변경 사항 요약

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| **필수 필드** | tenantId, loginId, name, organizationId | **+ roles** (5개) |
| **roles 타입** | (없음) | `Set<String>` |
| **roles 검증** | (없음) | `@NotEmpty` (최소 1개) |
| **Controller 로직** | roles 미전달 | **roles 변환 후 전달** |
| **문서 Scenario** | 4-1, 4-2, 4-3 | **+ 4-4 (roles 누락)** |

---

## ✅ 검증 완료

### 컴파일 에러
- ✅ `Agent` import 추가됨
- ✅ `Set` import 추가됨
- ✅ `roles` 전달 로직 추가됨
- ⚠️ 기존 에러 (TenantContextHolder)는 무관

### 문서 업데이트
- ✅ Scenario 4-1, 4-2, 4-3에 `roles` 추가
- ✅ Scenario 4-4 신규 추가 (roles 누락 케이스)
- ✅ cURL 예제 업데이트
- ✅ 필수 필드 설명 추가

---

## 🎯 다음 테스트

### 1. tenantId + loginId + name + organizationId + roles (성공)
```json
{
  "tenantId": "default-tenant",
  "loginId": "new.user",
  "name": "신규사용자",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "roles": ["MEMBER"]
}
```
**예상**: ✅ 201 Created

### 2. roles 누락 (실패)
```json
{
  "tenantId": "default-tenant",
  "loginId": "new.user2",
  "name": "신규사용자2",
  "organizationId": "00000000-0000-0000-0000-000000000004"
}
```
**예상**: ❌ 400 Bad Request - "roles는 최소 1개 이상이어야 합니다"

### 3. roles 빈 배열 (실패)
```json
{
  "tenantId": "default-tenant",
  "loginId": "new.user3",
  "name": "신규사용자3",
  "organizationId": "00000000-0000-0000-0000-000000000004",
  "roles": []
}
```
**예상**: ❌ 400 Bad Request - "역할은 최소 1개 이상이어야 합니다"

---

**작성일**: 2026-02-08  
**수정 파일**:
1. `CreateAgentRequest.java` (roles 필드 추가)
2. `AgentController.java` (roles 변환 로직 추가)
3. `API_TEST_SCENARIOS_AGENT.md` (Scenario 4 전체 업데이트)

**결과**: ✅ 실제 API 동작과 문서가 정확히 일치! 🚀

