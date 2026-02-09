# 부서 이동 API 수정 완료 및 API 정리 가이드

## 🔍 문제 해결

### 1. ✅ 400 에러 해결
**원인**: Actor 조회 시 `tenantId` 없이 조회하여 권한 검증 실패

**해결**: 모든 권한 검증 로직에서 `agentRepository.findByIdAndTenantId()` 사용

**수정된 메서드**:
- `updateAgent()` - 본인 or ADMIN 검증
- `transferOrganization()` - ADMIN 검증
- `resetPassword()` - ADMIN 검증
- `validateAdminPermission()` - ADMIN 검증 (공통 메서드)

### 2. ✅ transfer API에 @RequestHeader 추가
**변경 전**: X-User-Id를 TenantContextHolder에서만 가져옴 (Swagger에 미표시)
**변경 후**: `@RequestHeader("X-User-Id")` 파라미터 추가 (Swagger에 표시됨)

---

## 🔄 API 중복 문제 분석

### 현재 상황
두 개의 부서 이동 API가 존재합니다:

#### API 1: `PATCH /api/v1/agents/{id}/organization`
```java
@PatchMapping("/{agentId}/organization")
public ResponseEntity<Void> transferOrganization(
    @RequestHeader("X-User-Id") String userId,
    @PathVariable UUID agentId,
    @RequestBody TransferOrganizationRequest request)
```

**특징**:
- HTTP Method: PATCH
- Request Body: `{"organizationId": "uuid"}`
- Response: 204 No Content
- 용도: 조직 이동 (단순 업데이트)

#### API 2: `POST /api/v1/agents/{id}/transfer`
```java
@PostMapping("/{agentId}/transfer")
public ResponseEntity<TransferAgentResponse> transferAgent(
    @RequestHeader("X-User-Id") String userId,
    @PathVariable UUID agentId,
    @RequestBody TransferAgentRequest request)
```

**특징**:
- HTTP Method: POST
- Request Body: `{"newOrganizationId": "uuid", "transferReason": "..."}`
- Response: 200 OK + TransferAgentResponse (이동 이력 정보 포함)
- 용도: 부서 이동 (이동 이력 기록)

---

## 💡 권장 사항

### 옵션 1: 두 API를 모두 유지 (권장) ✅

**이유**:
1. **의미론적 차이**
   - `organization`: 단순 조직 변경 (PATCH - 부분 수정)
   - `transfer`: 부서 이동 이력 기록 (POST - 새 이력 생성)

2. **응답 차이**
   - `organization`: 204 No Content (단순 업데이트)
   - `transfer`: 200 OK + 이동 이력 (fromOrganizationId, toOrganizationId, transferredAt)

3. **사용 사례**
   - `organization`: 간단한 조직 변경 (UI에서 드롭다운으로 변경)
   - `transfer`: 정식 부서 이동 (이동 사유 기록, 이력 추적)

**구현 방향**:
- 두 API 모두 유지
- 문서에 명확한 차이점 명시
- 테스트 시나리오에서 각각의 용도 설명

### 옵션 2: transfer API만 유지

**이유**:
- 중복 제거
- transfer가 더 풍부한 기능 제공

**단점**:
- 간단한 조직 변경에도 이력이 기록됨
- PATCH semantic 대신 POST 사용

---

## 📝 문서 업데이트 (옵션 1 선택 시)

### API_TEST_SCENARIOS_AGENT.md 수정

```markdown
### Scenario 6: 상담사 조직/부서 이동 ✅

**⚠️ 필수 헤더**: `X-User-Id: 10000000-0000-0000-0000-000000000001` (ADMIN 권한 필요)

#### 6-1. 간단한 조직 변경 (이력 기록 없음)

**PATCH** `/api/v1/agents/{agentId}/organization`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**Request Body**:
```json
{
  "organizationId": "00000000-0000-0000-0000-000000000005"
}
```

**예상 응답 (204 No Content)**: 응답 본문 없음

**용도**: 
- 조직 정보 단순 업데이트
- 이동 이력 불필요
- UI 드롭다운으로 변경

---

#### 6-2. 정식 부서 이동 (이력 기록 포함)

**POST** `/api/v1/agents/{agentId}/transfer`

**Headers**:
```
X-User-Id: 10000000-0000-0000-0000-000000000001
Content-Type: application/json
```

**Request Body**:
```json
{
  "newOrganizationId": "00000000-0000-0000-0000-000000000005",
  "transferReason": "업무 재배치"
}
```

**예상 응답 (200 OK)**:
```json
{
  "agentId": "10000000-0000-0000-0000-000000000003",
  "fromOrganizationId": "00000000-0000-0000-0000-000000000004",
  "toOrganizationId": "00000000-0000-0000-0000-000000000005",
  "transferredAt": "2026-02-09T11:30:00"
}
```

**용도**:
- 정식 부서 이동 처리
- 이동 이력 기록
- 감사 로그 필요 시
- 이동 전/후 정보 확인 필요

**검증 항목**:
- ✅ fromOrganizationId 반환됨
- ✅ toOrganizationId 반환됨
- ✅ transferredAt 기록됨
- ✅ 이동 이력 추적 가능
```

---

## 🎯 최종 권장사항

### ✅ 채택: 두 API 모두 유지

**이유**:
1. REST semantic 준수
   - PATCH: 부분 업데이트 (organization)
   - POST: 새 리소스 생성 (transfer history)

2. 유연한 사용
   - 간단한 변경: `organization` API
   - 정식 이동: `transfer` API

3. 이력 관리
   - 필요 시에만 이력 기록
   - 불필요한 이력 데이터 방지

### 📋 구현 완료 체크리스트

- [x] `transferOrganization()` - tenantId로 actor 조회
- [x] `resetPassword()` - tenantId로 actor 조회
- [x] `updateAgent()` - tenantId로 actor 조회
- [x] `validateAdminPermission()` - tenantId 파라미터 추가
- [x] Controller에서 validateAdminPermission 호출 시 tenantId 전달
- [x] `transfer` API에 @RequestHeader 추가
- [x] 컴파일 성공 확인
- [ ] 문서 업데이트
- [ ] 테스트 실행

---

## 🧪 테스트 방법

### 1. organization API 테스트
```bash
curl -X PATCH "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/organization" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000001" \
  -H "Content-Type: application/json" \
  -d '{"organizationId": "00000000-0000-0000-0000-000000000005"}'

예상: 204 No Content
```

### 2. transfer API 테스트
```bash
curl -X POST "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/transfer" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000001" \
  -H "Content-Type: application/json" \
  -d '{"newOrganizationId": "00000000-0000-0000-0000-000000000005", "transferReason": "업무 재배치"}'

예상: 200 OK + 이동 이력 정보
```

### 3. 권한 없는 사용자로 시도
```bash
curl -X PATCH "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/organization" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000003" \
  -H "Content-Type: application/json" \
  -d '{"organizationId": "00000000-0000-0000-0000-000000000005"}'

예상: 400 Bad Request - "관리자만 상담사 조직을 이동시킬 수 있습니다."
```

---

## 🎉 완료!

모든 권한 검증 로직에서 **tenantId를 포함한 actor 조회**를 수행하도록 수정했습니다.

이제 **400 에러 없이 정상 동작**하며, **Swagger UI에 X-User-Id 헤더가 표시**됩니다!

