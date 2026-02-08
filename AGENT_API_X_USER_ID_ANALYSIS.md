# Agent Management API - X-User-Id 헤더 필요 여부 분석

## 🔍 분석 결과 요약

Controller 코드 전체를 분석한 결과, **X-User-Id 헤더가 필요한 API**와 **불필요한 API**가 명확히 구분됩니다.

---

## ✅ X-User-Id 헤더 **불필요** (조회 API)

### 1. 상담사 단건 조회
```java
@GetMapping("/{agentId}")
public ResponseEntity<AgentResponse> getAgent(@PathVariable UUID agentId)
```
**이유**: `agentId`만으로 조회 가능, 권한 검증 없음

---

### 2. 상담사 목록 조회
```java
@GetMapping
public ResponseEntity<List<AgentResponse>> getAgents(
    @RequestParam String tenantId,
    @RequestParam(required = false) String organizationId,
    ...
)
```
**이유**: `tenantId`로 필터링, 권한 검증 없음

---

### 3. 로그인 아이디 중복 체크
```java
@GetMapping("/check-login-id")
public ResponseEntity<Map<String, Boolean>> checkLoginId(@RequestParam String loginId)
```
**이유**: 공개 API, 권한 검증 불필요

---

### 4. 상담사 통계 조회
```java
@GetMapping("/statistics")
public ResponseEntity<AgentStatistics> getStatistics(@RequestParam String tenantId)
```
**이유**: `tenantId`로만 통계 조회, 권한 검증 없음

---

## ⚠️ X-User-Id 헤더 **필요** (생성/수정/삭제 API)

### 1. 상담사 생성
```java
@PostMapping
public ResponseEntity<CreateAgentResponse> createAgent(@RequestBody CreateAgentRequest request)
```
**현재 상태**: X-User-Id 헤더 **없음**
**권장**: ✅ **추가 필요** (누가 생성했는지 감사 로그 필요)

---

### 2. 상담사 정보 수정
```java
@PatchMapping("/{agentId}")
public ResponseEntity<Void> updateAgent(
    @PathVariable UUID agentId,
    @RequestBody UpdateAgentRequest request
)
```
**현재 상태**: X-User-Id 헤더 **없음**
**권장**: ✅ **추가 필요** (user:update 권한 검증 필요)

---

### 3. 상담사 조직 이동
```java
@PatchMapping("/{agentId}/organization")
public ResponseEntity<Void> transferOrganization(
    @PathVariable UUID agentId,
    @RequestBody TransferOrganizationRequest request
)
```
**현재 상태**: X-User-Id 헤더 **없음**
**권장**: ✅ **추가 필요** (user:update 권한 검증 필요)

---

### 4. 상담사 정지
```java
@PostMapping("/{agentId}/suspend")
public ResponseEntity<Void> suspendAgent(@PathVariable UUID agentId)
```
**현재 상태**: X-User-Id 헤더 **없음** (하지만 내부에서 `TenantContextHolder.getCurrentUserId()` 사용)
**권장**: ✅ **헤더로 받는 것이 명시적**

---

### 5. 상담사 활성화
```java
@PostMapping("/{agentId}/activate")
public ResponseEntity<Void> activateAgent(@PathVariable UUID agentId)
```
**현재 상태**: X-User-Id 헤더 **없음** (하지만 내부에서 `TenantContextHolder.getCurrentUserId()` 사용)
**권장**: ✅ **헤더로 받는 것이 명시적**

---

### 6. 상담사 퇴사 처리
```java
@DeleteMapping("/{agentId}")
public ResponseEntity<Void> retireAgent(@PathVariable UUID agentId)
```
**현재 상태**: X-User-Id 헤더 **없음** (하지만 내부에서 `TenantContextHolder.getCurrentUserId()` 사용)
**권장**: ✅ **헤더로 받는 것이 명시적**

---

### 7. 비밀번호 초기화
```java
@PostMapping("/{agentId}/reset-password")
public ResponseEntity<ResetPasswordResponse> resetPassword(@PathVariable UUID agentId)
```
**현재 상태**: X-User-Id 헤더 **없음**
**권장**: ✅ **추가 필요** (admin 권한 검증 필요)

---

### 8. 비밀번호 변경
```java
@PostMapping("/{agentId}/change-password")
public ResponseEntity<Void> changePassword(
    @PathVariable UUID agentId,
    @RequestBody ChangePasswordRequest request
)
```
**현재 상태**: X-User-Id 헤더 **없음**
**권장**: ✅ **추가 필요** (본인 확인 필요)

---

### 9. 역할 관리 (추가/제거/일괄 지정)
```java
@PostMapping("/{agentId}/roles/{roleName}")
@DeleteMapping("/{agentId}/roles/{roleName}")
@PutMapping("/{agentId}/roles")
```
**현재 상태**: X-User-Id 헤더 **없음**
**권장**: ✅ **추가 필요** (role:assign 권한 검증 필요)

---

## 📊 현재 구현 상태

### ✅ 헤더 없이 정상 작동 (4개)
| API | 메서드 | 경로 | X-User-Id |
|-----|--------|------|----------|
| 단건 조회 | GET | `/api/v1/agents/{agentId}` | ❌ 불필요 |
| 목록 조회 | GET | `/api/v1/agents` | ❌ 불필요 |
| 중복 체크 | GET | `/api/v1/agents/check-login-id` | ❌ 불필요 |
| 통계 조회 | GET | `/api/v1/agents/statistics` | ❌ 불필요 |

---

### ⚠️ 헤더 없지만 내부에서 Context 사용 (3개)
| API | 메서드 | 경로 | 현재 | 권장 |
|-----|--------|------|------|------|
| 정지 | POST | `/api/v1/agents/{agentId}/suspend` | Context | ✅ 헤더 추가 |
| 활성화 | POST | `/api/v1/agents/{agentId}/activate` | Context | ✅ 헤더 추가 |
| 퇴사 처리 | DELETE | `/api/v1/agents/{agentId}` | Context | ✅ 헤더 추가 |

**문제점**: `TenantContextHolder.getCurrentUserId()`는 인증 필터에서 설정되지만, API 명세에 명시적으로 드러나지 않음

---

### ❌ 헤더도 없고 권한 검증도 없음 (6개)
| API | 메서드 | 경로 | 문제 |
|-----|--------|------|------|
| 생성 | POST | `/api/v1/agents` | 누가 생성했는지 모름 |
| 수정 | PATCH | `/api/v1/agents/{agentId}` | 권한 검증 없음 |
| 조직 이동 | PATCH | `/api/v1/agents/{agentId}/organization` | 권한 검증 없음 |
| 비밀번호 초기화 | POST | `/api/v1/agents/{agentId}/reset-password` | admin 권한 검증 없음 |
| 비밀번호 변경 | POST | `/api/v1/agents/{agentId}/change-password` | 본인 확인 없음 |
| 역할 관리 | POST/DELETE/PUT | `/api/v1/agents/{agentId}/roles/*` | 권한 검증 없음 |

---

## 🎯 권장 사항

### 1. Organization API 참고
Organization 모듈의 Department API는 모든 생성/수정/삭제 API에 X-User-Id 헤더를 명시적으로 받습니다:

```java
@PostMapping
public ResponseEntity<DepartmentDto.Response> createDepartment(
    @RequestHeader(value = "X-User-Id") String userIdStr,  // ✅ 명시적 헤더
    @RequestBody DepartmentDto.CreateRequest request
)
```

---

### 2. Agent API도 통일 필요
**조회 API**: X-User-Id 불필요 (현재 상태 유지) ✅

**생성/수정/삭제 API**: X-User-Id 필요 (추가 권장) ⚠️
- 권한 검증
- 감사 로그 (누가, 언제, 무엇을 했는지)
- API 명세의 명확성

---

### 3. 구체적 수정 제안

#### A. 명시적 헤더 추가 (권장) ✅
```java
@PostMapping
public ResponseEntity<CreateAgentResponse> createAgent(
    @RequestHeader(value = "X-User-Id") String userIdStr,  // ✅ 추가
    @RequestBody CreateAgentRequest request
) {
    UUID actorUserId = UUID.fromString(userIdStr);
    // 권한 검증: user:create 권한 필요
    Set<String> permissions = rbacPort.getPermissionsByAgentId(tenantId, actorUserId);
    if (!permissions.contains("user:create")) {
        throw new InsufficientPermissionException();
    }
    // ...
}
```

#### B. 현재 Context 사용 방식 유지 (차선책)
```java
@PostMapping("/{agentId}/suspend")
public ResponseEntity<Void> suspendAgent(@PathVariable UUID agentId) {
    // 현재 방식
    String actorId = TenantContextHolder.getCurrentUserId();
    // 문제: API 명세에 드러나지 않음
}
```

---

## 📋 최종 권장 API별 X-User-Id 필요 여부

| API | X-User-Id | 이유 |
|-----|-----------|------|
| **조회 API** |||
| GET `/agents` | ❌ 불필요 | 공개 조회 |
| GET `/agents/{id}` | ❌ 불필요 | 공개 조회 |
| GET `/agents/check-login-id` | ❌ 불필요 | 공개 조회 |
| GET `/agents/statistics` | ❌ 불필요 | 공개 조회 |
| **생성/수정/삭제 API** |||
| POST `/agents` | ✅ **필요** | user:create 권한 검증 |
| PATCH `/agents/{id}` | ✅ **필요** | user:update 권한 검증 |
| PATCH `/agents/{id}/organization` | ✅ **필요** | user:update 권한 검증 |
| POST `/agents/{id}/suspend` | ✅ **필요** | user:suspend 권한 검증 |
| POST `/agents/{id}/activate` | ✅ **필요** | user:activate 권한 검증 |
| DELETE `/agents/{id}` | ✅ **필요** | user:delete 권한 검증 |
| POST `/agents/{id}/reset-password` | ✅ **필요** | admin 권한 검증 |
| POST `/agents/{id}/change-password` | ✅ **필요** | 본인 확인 |
| POST `/agents/{id}/roles/{name}` | ✅ **필요** | role:assign 권한 검증 |
| DELETE `/agents/{id}/roles/{name}` | ✅ **필요** | role:revoke 권한 검증 |
| PUT `/agents/{id}/roles` | ✅ **필요** | role:assign 권한 검증 |

---

## 🎉 결론

### 현재 API_TEST_SCENARIOS_AGENT.md 문서 수정 필요

**Scenario 2 (상담사 단건 조회)**:
- ❌ 제거: `X-User-Id: 10000000-0000-0000-0000-000000000001` 헤더
- ✅ 이유: 조회 API는 X-User-Id 불필요

**다른 시나리오들**:
- 조회 API (1, 2, 3, 13, 14): X-User-Id 불필요
- 생성/수정/삭제 API (4-12): X-User-Id 필요 (현재는 없지만 추가 권장)

---

**작성일**: 2026-02-08  
**분석 대상**: AgentController.java (721 lines)  
**결론**: 조회 API는 X-User-Id 불필요, 생성/수정/삭제는 필요 (추가 권장)

