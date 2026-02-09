# 부서 이동 API - 부서 존재 여부 검증 추가 ✅

## 🔍 문제 상황

### 에러
```
POST /api/v1/agents/{agentId}/transfer
Request: {"newOrganizationId": "99999999-9999-9999-9999-999999999999"}

응답: 200 OK
Content-Length: 0
```

**문제**: 존재하지 않는 부서로 이동 시도 시 **200 OK가 반환**되고 **응답 본문이 비어있음**

### 원인
- **부서 존재 여부 검증이 누락**됨
- 주석에 "T-001 검증은 Organization 모듈에서 수행"이라고 되어 있었지만 실제로는 검증 안 됨
- DB의 Foreign Key 제약 조건은 NULL을 허용하므로 DB 레벨에서도 막히지 않음

---

## ✅ 해결 방법

### 1. ErrorCode 추가
```java
// ErrorCode.java
ORGANIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, "A006", "부서를 찾을 수 없습니다."),
```

### 2. transferAgent 메서드 수정 (POST /transfer)
```java
@Override
public TransferAgentUseCase.TransferAgentResult transferAgent(
        TransferAgentUseCase.TransferAgentCommand command) {
    
    // 1. 상담사 조회
    Agent agent = agentRepository.findByIdAndTenantId(...)
            .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

    // 2. ✅ 대상 부서 존재 확인 (추가)
    organizationPort.getDepartmentInfo(command.getTenantId(), command.getNewOrganizationId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND, 
                    "이동할 부서를 찾을 수 없습니다."));

    // 3. RETIRED 상담사 이동 불가
    if (agent.getStatus() == AgentStatus.RETIRED) {
        throw new BusinessException(ErrorCode.AGENT_ALREADY_RETIRED, ...);
    }

    // 4. 동일 부서로 이동 불가
    if (agent.getOrganizationId() != null &&
        agent.getOrganizationId().equals(command.getNewOrganizationId())) {
        throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, ...);
    }

    // 5. 조직 변경
    agent.transferOrganization(command.getNewOrganizationId());
    agentRepository.save(agent);
    
    // ...
}
```

### 3. transferOrganization 메서드 수정 (PATCH /organization)
```java
@Override
public void transferOrganization(String tenantId, UUID agentId, UUID actorId, String newOrganizationId) {
    // 1. 상담사 조회
    Agent agent = agentRepository.findByIdAndTenantId(agentId, tenantId)
            .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

    // 2. ✅ 대상 부서 존재 확인 (추가)
    organizationPort.getDepartmentInfo(tenantId, newOrganizationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND,
                    "이동할 부서를 찾을 수 없습니다."));

    // 3. ADMIN 권한 검증
    Agent actor = agentRepository.findByIdAndTenantId(actorId, tenantId)
            .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND, "권한을 확인할 수 없습니다."));
    
    boolean isAdmin = actor.getRoles().stream()
            .anyMatch(role -> "ADMIN".equals(role.getName()));

    if (!isAdmin) {
        throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "관리자만 상담사 조직을 이동시킬 수 있습니다.");
    }

    // 4. 조직 이동
    agent.transferOrganization(newOrganizationId);
}
```

---

## 🧪 테스트 시나리오

### 1. 존재하는 부서로 이동 (성공) ✅

```http
POST /api/v1/agents/10000000-0000-0000-0000-000000000003/transfer
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json

{
  "newOrganizationId": "00000000-0000-0000-0000-000000000005",
  "transferReason": "업무 재배치"
}

예상: 200 OK
{
  "agentId": "10000000-0000-0000-0000-000000000003",
  "fromOrganizationId": "00000000-0000-0000-0000-000000000004",
  "toOrganizationId": "00000000-0000-0000-0000-000000000005",
  "transferredAt": "2026-02-09T13:00:00"
}
```

### 2. 존재하지 않는 부서로 이동 시도 (실패) ❌

```http
POST /api/v1/agents/10000000-0000-0000-0000-000000000003/transfer
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json

{
  "newOrganizationId": "99999999-9999-9999-9999-999999999999"
}

예상: 404 Not Found
{
  "code": "A006",
  "message": "이동할 부서를 찾을 수 없습니다."
}
```

### 3. PATCH API로도 동일 검증

```http
PATCH /api/v1/agents/10000000-0000-0000-0000-000000000003/organization
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json

{
  "organizationId": "99999999-9999-9999-9999-999999999999"
}

예상: 404 Not Found
{
  "code": "A006",
  "message": "이동할 부서를 찾을 수 없습니다."
}
```

---

## 📊 검증 순서

### transferAgent (POST /transfer)
1. ✅ 상담사 조회 (tenantId 포함)
2. ✅ **부서 존재 확인** (추가)
3. ✅ RETIRED 상담사 이동 불가 검증
4. ✅ 동일 부서로 이동 불가 검증
5. ✅ 조직 변경 수행

### transferOrganization (PATCH /organization)
1. ✅ 상담사 조회 (tenantId 포함)
2. ✅ **부서 존재 확인** (추가)
3. ✅ ADMIN 권한 검증
4. ✅ 조직 변경 수행

---

## 🔧 수정된 파일

1. **ErrorCode.java**
   - `ORGANIZATION_NOT_FOUND` 에러 코드 추가

2. **AgentService.java**
   - `transferAgent()` - 부서 존재 확인 추가
   - `transferOrganization()` - 부서 존재 확인 추가

3. **API_TEST_SCENARIOS_AGENT.md**
   - Scenario 6-2의 예상 응답 코드 수정 (A006)

---

## ✅ 컴파일 확인

```bash
.\gradlew compileJava

BUILD SUCCESSFUL in 8s
```

---

## 🎯 기대 효과

### Before (문제)
```
POST /transfer with invalid deptId
→ 200 OK (empty body)
→ 부서 검증 안 됨
→ 데이터 무결성 문제 발생 가능
```

### After (해결)
```
POST /transfer with invalid deptId
→ 404 Not Found
→ {
    "code": "A006",
    "message": "이동할 부서를 찾을 수 없습니다."
  }
→ 부서 검증 수행 ✅
→ 데이터 무결성 보장 ✅
```

---

## 🎉 완료!

이제 **존재하지 않는 부서로 이동 시도 시 404 Not Found**가 반환되며, **적절한 에러 메시지**가 표시됩니다!

### 다음 단계
1. ✅ 코드 수정 완료
2. ✅ 컴파일 성공
3. ⏳ 애플리케이션 재시작
4. ⏳ API 테스트 (Swagger UI)
5. ⏳ 시나리오 6-2 검증

---

## 💡 핵심 포인트

### OrganizationPort 활용
```java
// ✅ 부서 존재 확인
organizationPort.getDepartmentInfo(tenantId, deptId)
    .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND));
```

### 검증 순서의 중요성
1. **상담사 조회** (대상 확인)
2. **부서 존재 확인** (이동 가능 여부)
3. **비즈니스 규칙 검증** (RETIRED, 동일 부서 등)
4. **권한 검증** (ADMIN)
5. **실제 변경 수행**

### 에러 응답 일관성
- **404 Not Found**: 리소스를 찾을 수 없음 (상담사, 부서)
- **400 Bad Request**: 비즈니스 규칙 위반 (이미 퇴사, 동일 부서 등)
- **403 Forbidden**: 권한 없음 (ADMIN 아님)

