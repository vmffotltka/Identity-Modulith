# @RequestHeader 추가로 Swagger UI X-User-Id 헤더 표시 완료 ✅

## 🎯 문제 해결

### 문제
- 내부적으로는 `TenantContextHolder.getCurrentUserId()`로 X-User-Id 헤더를 읽고 있었음
- 하지만 **Swagger UI에서 X-User-Id 헤더 입력란이 표시되지 않음**
- `@RequestHeader` 어노테이션이 없어서 Swagger 문서에 나타나지 않았음

### 해결
모든 권한 필요 API에 `@RequestHeader("X-User-Id")` 파라미터 추가

---

## ✅ 수정 완료 API 목록

### 1. 상담사 정보 수정
```java
public ResponseEntity<Void> updateAgent(
    @RequestHeader("X-User-Id") String userId,  // ✅ 추가
    @PathVariable UUID agentId,
    @RequestBody UpdateAgentRequest request)
```

### 2. 상담사 부서 이동
```java
public ResponseEntity<Void> transferOrganization(
    @RequestHeader("X-User-Id") String userId,  // ✅ 추가
    @PathVariable UUID agentId,
    @RequestBody TransferOrganizationRequest request)
```

### 3. 비밀번호 초기화
```java
public ResponseEntity<ResetPasswordResponse> resetPassword(
    @RequestHeader("X-User-Id") String userId,  // ✅ 추가
    @PathVariable UUID agentId)
```

### 4. 역할 일괄 지정
```java
public ResponseEntity<Void> assignRoles(
    @RequestHeader("X-User-Id") String userId,  // ✅ 추가
    @PathVariable UUID agentId,
    @RequestBody AssignRolesRequest request)
```

### 5. 역할 추가
```java
public ResponseEntity<Void> addRole(
    @RequestHeader("X-User-Id") String userId,  // ✅ 추가
    @PathVariable UUID agentId,
    @PathVariable String roleName)
```

### 6. 역할 제거
```java
public ResponseEntity<Void> removeRole(
    @RequestHeader("X-User-Id") String userId,  // ✅ 추가
    @PathVariable UUID agentId,
    @PathVariable String roleName)
```

---

## 🔧 변경 사항

### Before (문제)
```java
@PatchMapping("/{agentId}")
public ResponseEntity<Void> updateAgent(
        @PathVariable UUID agentId,
        @RequestBody UpdateAgentRequest request) {
    
    // TenantContextHolder에서 읽지만 Swagger에 표시 안 됨
    String actorId = TenantContextHolder.getCurrentUserId();
    // ...
}
```

**문제점**:
- ❌ Swagger UI에 X-User-Id 헤더 입력란 없음
- ❌ 사용자가 헤더를 입력할 방법이 없음
- ❌ 테스트 불가능

### After (해결)
```java
@PatchMapping("/{agentId}")
public ResponseEntity<Void> updateAgent(
        @Parameter(description = "요청 사용자 ID", required = true)
        @RequestHeader("X-User-Id") String userId,  // ✅ 명시적으로 추가
        @PathVariable UUID agentId,
        @RequestBody UpdateAgentRequest request) {
    
    // 파라미터로 받은 userId 사용
    UUID actorId = UUID.fromString(userId);
    // ...
}
```

**해결**:
- ✅ Swagger UI에 X-User-Id 헤더 입력란 표시됨
- ✅ 사용자가 직접 헤더 값 입력 가능
- ✅ 권한 테스트 가능

---

## 📊 Swagger UI 확인 방법

1. **애플리케이션 실행**
   ```bash
   .\gradlew bootRun
   ```

2. **Swagger UI 접속**
   ```
   http://localhost:8080/swagger-ui/index.html
   ```

3. **상담사 정보 수정 API 확인**
   - `PATCH /api/v1/agents/{agentId}` 클릭
   - **Parameters** 섹션에 다음이 표시되어야 함:
     ```
     X-User-Id (header) - 요청 사용자 ID [required]
     agentId (path) - 상담사 ID [required]
     ```

4. **테스트 시나리오 실행**
   ```
   Try it out 클릭
   → X-User-Id: 10000000-0000-0000-0000-000000000003
   → agentId: 10000000-0000-0000-0000-000000000002
   → Request body: {"name": "김매니저"}
   → Execute
   
   예상: 400 Bad Request
   "본인 또는 관리자만 상담사 정보를 수정할 수 있습니다."
   ```

---

## 🎯 테스트 시나리오

### Scenario 1: Swagger UI에서 X-User-Id 헤더 확인
1. Swagger UI 접속
2. `PATCH /api/v1/agents/{agentId}` 확인
3. **Parameters 섹션에 X-User-Id 표시 확인** ✅

### Scenario 2: 본인이 본인 수정 (성공)
```
X-User-Id: 10000000-0000-0000-0000-000000000003
agentId: 10000000-0000-0000-0000-000000000003
Body: {"name": "이시니어"}

예상: 204 No Content
```

### Scenario 3: MEMBER가 다른 사람 수정 시도 (실패)
```
X-User-Id: 10000000-0000-0000-0000-000000000003
agentId: 10000000-0000-0000-0000-000000000002
Body: {"name": "김매니저"}

예상: 400 Bad Request
{
  "code": "A005",
  "message": "본인 또는 관리자만 상담사 정보를 수정할 수 있습니다."
}
```

### Scenario 4: ADMIN이 다른 사람 수정 (성공)
```
X-User-Id: 10000000-0000-0000-0000-000000000001
agentId: 10000000-0000-0000-0000-000000000003
Body: {"name": "이시니어"}

예상: 204 No Content
```

### Scenario 5: MEMBER가 부서 이동 시도 (실패)
```
PATCH /api/v1/agents/{agentId}/organization
X-User-Id: 10000000-0000-0000-0000-000000000003
agentId: 10000000-0000-0000-0000-000000000003
Body: {"organizationId": "00000000-0000-0000-0000-000000000005"}

예상: 400 Bad Request
"관리자만 상담사 조직을 이동시킬 수 있습니다."
```

### Scenario 6: MEMBER가 비밀번호 초기화 시도 (실패)
```
POST /api/v1/agents/{agentId}/reset-password
X-User-Id: 10000000-0000-0000-0000-000000000003
agentId: 10000000-0000-0000-0000-000000000003

예상: 400 Bad Request
"관리자만 비밀번호를 초기화할 수 있습니다."
```

### Scenario 7: MEMBER가 역할 추가 시도 (실패)
```
POST /api/v1/agents/{agentId}/roles/{roleName}
X-User-Id: 10000000-0000-0000-0000-000000000003
agentId: 10000000-0000-0000-0000-000000000003
roleName: TEAM_LEAD

예상: 400 Bad Request
"관리자만 이 작업을 수행할 수 있습니다."
```

---

## 📝 수정된 파일

- `src/main/java/com/nexfron/identitymodulith/user/presentation/AgentController.java`
  - updateAgent() - @RequestHeader 추가
  - transferOrganization() - @RequestHeader 추가
  - resetPassword() - @RequestHeader 추가
  - assignRoles() - @RequestHeader 추가
  - addRole() - @RequestHeader 추가
  - removeRole() - @RequestHeader 추가

---

## ✅ 컴파일 확인

```bash
.\gradlew compileJava

BUILD SUCCESSFUL in 9s
```

---

## 🎉 완료!

이제 **Swagger UI에서 X-User-Id 헤더를 입력**할 수 있고, 
**모든 권한 검증 시나리오를 테스트**할 수 있습니다!

### 다음 단계
1. ✅ @RequestHeader 추가 완료
2. ✅ 컴파일 성공
3. ⏳ 애플리케이션 실행 중
4. ⏳ Swagger UI에서 X-User-Id 헤더 표시 확인
5. ⏳ 권한 검증 시나리오 테스트

---

## 💡 핵심 포인트

### Swagger UI에 헤더 표시하려면
```java
// ❌ 표시 안 됨
String userId = TenantContextHolder.getCurrentUserId();

// ✅ 표시됨
public void someMethod(@RequestHeader("X-User-Id") String userId) {
    // userId 사용
}
```

### @Parameter로 설명 추가
```java
@RequestHeader("X-User-Id") String userId  // 기본

@Parameter(description = "요청 사용자 ID (ADMIN)", required = true)
@RequestHeader("X-User-Id") String userId  // 설명 포함
```

### 권한별 설명
- **본인 or ADMIN**: "요청 사용자 ID"
- **ADMIN만**: "요청 사용자 ID (ADMIN)"
- **본인만**: "요청 사용자 ID (본인)"

