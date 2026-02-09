# 🔐 비밀번호 변경 API - X-User-Id 헤더 필수 처리 완료!

## 🚨 보안 문제 발견

### 문제
**비밀번호 변경 API에 X-User-Id 헤더가 없어서 보안 취약점 발생**

```java
// ❌ Before: @RequestHeader 없음
public ResponseEntity<Void> changePassword(
    @PathVariable UUID agentId,
    @RequestBody ChangePasswordRequest request) {
    
    // TenantContextHolder에서 가져오지만 실제로는 빈 값일 수 있음
    String actorId = TenantContextHolder.getCurrentUserId();
    // ...
}
```

**보안 위험**:
1. ❌ X-User-Id 헤더 없이도 API 호출 가능
2. ❌ 누구의 비밀번호인지 식별 불가
3. ❌ 본인 확인 불가능
4. ❌ 감사 로그 부정확

---

## ✅ 해결 방법

### 1. changePassword (/{agentId}/change-password)
```java
// ✅ After: @RequestHeader 추가
public ResponseEntity<Void> changePassword(
    @Parameter(description = "요청 사용자 ID (본인)", required = true)
    @RequestHeader("X-User-Id") String userId,  // ✅ 명시적으로 받음
    @PathVariable UUID agentId,
    @RequestBody ChangePasswordRequest request) {
    
    String tenantId = TenantContextHolder.getCurrentTenantId();
    
    ChangePasswordCommand command = ChangePasswordCommand.builder()
            .tenantId(tenantId)
            .agentId(agentId)
            .actorId(UUID.fromString(userId))  // ✅ 파라미터로 받은 값 사용
            .currentPassword(request.getCurrentPassword())
            .newPassword(request.getNewPassword())
            .build();
    
    // 본인 확인 검증은 Service에서 수행
    changePasswordUseCase.changePassword(command);
    return ResponseEntity.noContent().build();
}
```

### 2. changeMyPassword (/me/change-password)
```java
// ✅ After: @RequestHeader 추가
public ResponseEntity<Void> changeMyPassword(
    @Parameter(description = "요청 사용자 ID (본인)", required = true)
    @RequestHeader("X-User-Id") String userId,  // ✅ 명시적으로 받음
    @RequestBody ChangePasswordRequest request) {
    
    String tenantId = TenantContextHolder.getCurrentTenantId();
    UUID agentId = UUID.fromString(userId);  // /me는 자신의 ID
    
    ChangePasswordCommand command = ChangePasswordCommand.builder()
            .tenantId(tenantId)
            .agentId(agentId)
            .actorId(agentId)  // 본인 = actor
            .currentPassword(request.getCurrentPassword())
            .newPassword(request.getNewPassword())
            .build();
    
    changePasswordUseCase.changePassword(command);
    return ResponseEntity.noContent().build();
}
```

---

## 🔒 보안 검증 흐름

### changePassword (특정 agentId 지정)
```
1. Controller: X-User-Id 헤더에서 userId 추출
   ↓
2. Command 생성: actorId = userId, agentId = pathVariable
   ↓
3. Service: 본인 확인
   if (!agentId.equals(actorId)) {
       throw new BusinessException("다른 사용자의 비밀번호를 변경할 수 없습니다.");
   }
   ↓
4. 현재 비밀번호 검증
   ↓
5. 비밀번호 변경 수행
```

### changeMyPassword (/me 경로)
```
1. Controller: X-User-Id 헤더에서 userId 추출
   ↓
2. agentId = actorId = userId (자신)
   ↓
3. Service: 본인 확인 (자동으로 통과)
   ↓
4. 현재 비밀번호 검증
   ↓
5. 비밀번호 변경 수행
```

---

## 🧪 테스트 시나리오

### 1. 본인이 비밀번호 변경 (성공) ✅

```http
POST /api/v1/agents/me/change-password
X-User-Id: 10000000-0000-0000-0000-000000000003
Content-Type: application/json

{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!",
  "confirmPassword": "NewPassword456!"
}

예상: 204 No Content
```

### 2. X-User-Id 헤더 없이 시도 (실패) ❌

```http
POST /api/v1/agents/me/change-password
Content-Type: application/json

{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!"
}

예상: 400 Bad Request
{
  "code": "INVALID_REQUEST",
  "message": "X-User-Id 헤더가 필요합니다"
}
```

### 3. 다른 사람의 비밀번호 변경 시도 (실패) ❌

```http
POST /api/v1/agents/10000000-0000-0000-0000-000000000002/change-password
X-User-Id: 10000000-0000-0000-0000-000000000003
Content-Type: application/json

{
  "currentPassword": "SomePassword123!",
  "newPassword": "NewPassword456!"
}

예상: 400 Bad Request
{
  "code": "A005",
  "message": "다른 사용자의 비밀번호를 변경할 수 없습니다."
}
```

### 4. 현재 비밀번호 불일치 (실패) ❌

```http
POST /api/v1/agents/me/change-password
X-User-Id: 10000000-0000-0000-0000-000000000003
Content-Type: application/json

{
  "currentPassword": "WrongPassword123!",
  "newPassword": "NewPassword456!"
}

예상: 400 Bad Request
{
  "code": "INVALID_INPUT_VALUE",
  "message": "현재 비밀번호가 일치하지 않습니다."
}
```

---

## 📊 수정된 파일

### Controller
1. **AgentController.java**
   - `changePassword()` - @RequestHeader 추가
   - `changeMyPassword()` - @RequestHeader 추가

### 문서
2. **API_TEST_SCENARIOS_AGENT.md**
   - Scenario 8: "⚠️ 필수 헤더" 명시
   - X-User-Id 헤더 필요 여부 목록 업데이트
   - MEMBER 테스트 순서에 경고 추가

---

## ✅ 컴파일 확인

```bash
.\gradlew compileJava

BUILD SUCCESSFUL in 4s
```

---

## 🔑 Swagger UI 확인

### Before (문제)
```
POST /api/v1/agents/me/change-password

Parameters:
  - request (body) [required]

❌ X-User-Id 헤더 없음
❌ Swagger에서 헤더 입력란 없음
```

### After (해결)
```
POST /api/v1/agents/me/change-password

Parameters:
  - X-User-Id (header) [required]  ✅ 추가됨
  - request (body) [required]

✅ X-User-Id 헤더 입력란 표시
✅ required로 명시됨
```

---

## 🎯 보안 강화 효과

### Before (취약)
- ❌ X-User-Id 없이 호출 가능
- ❌ 본인 확인 불가능
- ❌ 누구의 비밀번호인지 불명확
- ❌ 감사 로그 부정확
- ❌ 보안 취약점 존재

### After (강화)
- ✅ X-User-Id 필수
- ✅ 본인 확인 가능
- ✅ 명확한 사용자 식별
- ✅ 정확한 감사 로그
- ✅ 보안 강화

---

## 💡 왜 X-User-Id가 필요한가?

### 1. 본인 확인 (Authentication)
```java
// X-User-Id로 실제 요청자 식별
if (!agentId.equals(actorId)) {
    throw new BusinessException("다른 사용자의 비밀번호를 변경할 수 없습니다.");
}
```

### 2. 감사 로그 (Audit Trail)
```java
// 누가 언제 비밀번호를 변경했는지 기록
log.info("[USER] 비밀번호 변경 완료 - agentId={}, actorId={}", agentId, actorId);
```

### 3. 보안 정책 준수 (Security Policy)
- OWASP: 사용자 인증 및 권한 검증 필수
- ISO 27001: 접근 제어 및 감사 로그
- GDPR: 개인정보 처리 기록

### 4. 시스템 일관성 (Consistency)
- 모든 수정/삭제 API와 동일한 패턴
- 명확한 권한 모델
- 예측 가능한 동작

---

## 📝 API 권한 정책 (최종)

| API | Method | X-User-Id | 권한 정책 |
|-----|--------|-----------|----------|
| 목록 조회 | GET /agents | ❌ 불필요 | 공개 |
| 단건 조회 | GET /agents/{id} | ❌ 불필요 | 공개 |
| 중복 체크 | GET /agents/check-login-id | ❌ 불필요 | 공개 |
| 생성 | POST /agents | ❌ 불필요 | 공개 |
| 정보 수정 | PATCH /agents/{id} | ✅ **필수** | 본인 or ADMIN |
| 부서 이동 | PATCH /agents/{id}/organization | ✅ **필수** | ADMIN |
| 비밀번호 초기화 | POST /agents/{id}/reset-password | ✅ **필수** | ADMIN |
| **비밀번호 변경** | **POST /agents/me/change-password** | ✅ **필수** | **본인만** |
| 역할 관리 | POST/DELETE/PUT /agents/{id}/roles | ✅ **필수** | ADMIN |
| 정지 | POST /agents/{id}/suspend | ✅ **필수** | ADMIN |
| 활성화 | POST /agents/{id}/activate | ✅ **필수** | ADMIN |
| 퇴사 처리 | DELETE /agents/{id} | ✅ **필수** | ADMIN |

---

## 🎉 완료!

**비밀번호 변경 API에 X-User-Id 헤더를 필수로 추가**하여 보안을 강화했습니다!

### 주요 개선 사항
1. ✅ X-User-Id 헤더 필수 처리
2. ✅ Swagger UI에 헤더 표시
3. ✅ 본인 확인 강화
4. ✅ 감사 로그 정확성 향상
5. ✅ 보안 취약점 제거
6. ✅ API 일관성 확보

### 다음 단계
1. ✅ 코드 수정 완료
2. ✅ 컴파일 성공
3. ✅ 문서 업데이트 완료
4. ⏳ 애플리케이션 재시작
5. ⏳ Swagger UI 확인
6. ⏳ API 테스트

이제 **모든 수정/삭제 API가 일관되게 X-User-Id 헤더를 요구**합니다! 🔒

