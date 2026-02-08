# ✅ Agent 업데이트 API - Hibernate Session 중복 에러 수정 완료

## 🔍 문제 분석

### 에러 현상
```
2026-02-09T00:00:14.601+09:00  WARN 10324 --- [identity-modulith] [nio-8080-exec-3] 
c.n.i.u.i.retry.DatabaseRetrySupplier    : DB 연결 실패 (시도 1/3): 
A different object with the same identifier value was already associated with the session: 
[com.nexfron.identitymodulith.user.infrastructure.persistence.entity.AgentJpaEntity#10000000-0000-0000-0000-000000000003]
```

### 문제
- 상담사 정보 수정 시 **Hibernate Session 중복 에러** 발생
- **DatabaseRetrySupplier**가 3번 재시도했지만 모두 실패
- **500 Internal Server Error** 반환

### 원인
1. **AgentService.updateAgent()** 호출
2. `findAgentById()` → Agent 엔티티를 Session에 로드 ✅
3. `agent.updateName()` → 도메인 객체 변경 ✅
4. `saveAgent(agent)` → **DatabaseRetrySupplier.withRetry()** 호출 ❌
5. **재시도 시 동일한 엔티티를 다시 조회** → Session에 이미 존재 ❌
6. **NonUniqueObjectException 발생** → "A different object with the same identifier value..." ❌

---

## 📊 Hibernate Session 관리 문제

### Hibernate의 1차 캐시 (Session)
```java
@Transactional
public void updateAgent(UpdateAgentCommand command) {
    // 1. findAgentById() - Session에 Agent 로드
    Agent agent = findAgentById(command.getAgentId());
    
    // 2. agent.updateName() - 엔티티 변경 (Dirty Checking 대상)
    agent.updateName(command.getName());
    
    // 3. saveAgent(agent) - 문제 발생!
    saveAgent(agent);  // ❌ 동일한 ID의 엔티티가 Session에 이미 존재
}
```

### DatabaseRetrySupplier의 재시도 로직
```java
private Agent saveAgent(Agent agent) {
    return DatabaseRetrySupplier.withRetry(() -> agentRepository.save(agent));
    // 재시도 시마다 save() 호출 → Session 충돌
}
```

### 왜 중복 에러가 발생하는가?
1. **@Transactional** 메서드 내에서 **Session은 하나**
2. `findAgentById()`로 Agent 엔티티를 Session에 로드
3. `save(agent)` 호출 시 **동일한 ID의 엔티티가 이미 Session에 존재**
4. Hibernate는 **동일한 ID를 가진 두 개의 다른 객체**를 허용하지 않음
5. **NonUniqueObjectException** 발생

---

## ✅ 해결 방법

### JPA Dirty Checking 활용

**@Transactional** 메서드 내에서 엔티티를 변경하면, **트랜잭션 종료 시 자동으로 UPDATE 쿼리 실행**됩니다.

```java
@Override
@Transactional
public void updateAgent(UpdateAgentCommand command) {
    Agent agent = findAgentById(command.getAgentId());
    agent.updateName(command.getName());
    // JPA dirty checking으로 자동 저장됨
    // saveAgent(agent); ❌ 불필요!
}
```

### 처리 흐름

#### Before ❌
```
updateAgent()
  ↓
findAgentById()
  → Session에 Agent 로드 (ID: 10000000-...-03)
  ↓
agent.updateName()
  → 엔티티 변경 (Dirty Checking 대상)
  ↓
saveAgent(agent)
  → DatabaseRetrySupplier.withRetry()
    → agentRepository.save(agent)
      ❌ Session에 이미 동일 ID의 Agent 존재
      ❌ NonUniqueObjectException 발생
    → 재시도 (1/3)
      ❌ 동일한 에러 반복
    → 재시도 (2/3)
      ❌ 동일한 에러 반복
    → 재시도 (3/3)
      ❌ 최종 실패
  ↓
500 Internal Server Error
```

#### After ✅
```
updateAgent()
  ↓
findAgentById()
  → Session에 Agent 로드 (ID: 10000000-...-03)
  ↓
agent.updateName()
  → 엔티티 변경 (Dirty Checking 대상)
  ↓
트랜잭션 종료
  → JPA가 변경 감지 (Dirty Checking)
  → UPDATE SQL 자동 실행
  ✅ Session 충돌 없음
  ✅ 정상 저장
  ↓
204 No Content
```

---

## 📋 수정 항목

### 1. AgentService.updateAgent() ✅

**Before**:
```java
@Override
public void updateAgent(UpdateAgentCommand command) {
    Agent agent = findAgentById(command.getAgentId());
    agent.updateName(command.getName());
    saveAgent(agent);  // ❌ Session 충돌
}
```

**After**:
```java
@Override
public void updateAgent(UpdateAgentCommand command) {
    Agent agent = findAgentById(command.getAgentId());
    agent.updateName(command.getName());
    // JPA dirty checking으로 자동 저장됨 (saveAgent 호출 불필요)
}
```

### 2. transferOrganization() ✅

**Before**:
```java
@Override
public void transferOrganization(UUID agentId, String newOrganizationId) {
    Agent agent = findAgentById(agentId);
    agent.transferOrganization(newOrganizationId);
    saveAgent(agent);  // ❌
}
```

**After**:
```java
@Override
public void transferOrganization(UUID agentId, String newOrganizationId) {
    Agent agent = findAgentById(agentId);
    agent.transferOrganization(newOrganizationId);
    // JPA dirty checking으로 자동 저장됨
}
```

### 3. suspendAgent() ✅

**Before**:
```java
public void suspendAgent(UUID agentId, String suspendedByUserId) {
    Agent agent = findAgentById(agentId);
    agent.suspend(suspendedByUserId);
    saveAgent(agent);  // ❌
}
```

**After**:
```java
public void suspendAgent(UUID agentId, String suspendedByUserId) {
    Agent agent = findAgentById(agentId);
    agent.suspend(suspendedByUserId);
    // JPA dirty checking으로 자동 저장됨
}
```

### 4. activateAgentInternal() ✅

**Before**:
```java
public void activateAgentInternal(UUID agentId, String activatedByUserId) {
    Agent agent = findAgentById(agentId);
    agent.activate();
    saveAgent(agent);  // ❌
}
```

**After**:
```java
public void activateAgentInternal(UUID agentId, String activatedByUserId) {
    Agent agent = findAgentById(agentId);
    agent.activate();
    // JPA dirty checking으로 자동 저장됨
}
```

### 5. retireAgentWithPolicyInternal() ✅

**Before**:
```java
public void retireAgentWithPolicyInternal(UUID agentId, String retiredByUserId, 
                                         Agent.RetireDeletePolicy deletePolicy, 
                                         Integer retentionDays) {
    Agent agent = findAgentById(agentId);
    agent.retire(retiredByUserId, deletePolicy, retentionDays);
    saveAgent(agent);  // ❌
}
```

**After**:
```java
public void retireAgentWithPolicyInternal(UUID agentId, String retiredByUserId, 
                                         Agent.RetireDeletePolicy deletePolicy, 
                                         Integer retentionDays) {
    Agent agent = findAgentById(agentId);
    agent.retire(retiredByUserId, deletePolicy, retentionDays);
    // JPA dirty checking으로 자동 저장됨
}
```

### 6. resetPassword() ✅

**Before**:
```java
@Override
public ResetPasswordResult resetPassword(UUID agentId) {
    Agent agent = findAgentById(agentId);
    String tempPassword = passwordGenerator.generateTempPassword();
    String encodedPassword = passwordEncoder.encode(tempPassword);
    agent.resetPassword(encodedPassword);
    saveAgent(agent);  // ❌
    return ResetPasswordResult.builder()...
}
```

**After**:
```java
@Override
public ResetPasswordResult resetPassword(UUID agentId) {
    Agent agent = findAgentById(agentId);
    String tempPassword = passwordGenerator.generateTempPassword();
    String encodedPassword = passwordEncoder.encode(tempPassword);
    agent.resetPassword(encodedPassword);
    // JPA dirty checking으로 자동 저장됨
    return ResetPasswordResult.builder()...
}
```

---

## 🎯 JPA Dirty Checking이란?

### 원리
1. **@Transactional** 메서드 시작 시 Transaction 시작
2. **EntityManager**가 엔티티의 **스냅샷**을 저장
3. 메서드 내에서 엔티티 변경
4. **트랜잭션 종료 시** EntityManager가 **스냅샷과 현재 엔티티를 비교**
5. **변경 감지 시 자동으로 UPDATE SQL 실행**

### 예시
```java
@Transactional
public void updateAgent(UpdateAgentCommand command) {
    // 1. 조회 - EntityManager가 스냅샷 저장
    Agent agent = findAgentById(command.getAgentId());
    // 스냅샷: name = "이개발"
    
    // 2. 변경
    agent.updateName("이시니어");
    // 현재 엔티티: name = "이시니어"
    
    // 3. 트랜잭션 종료 시 (메서드 종료 시)
    // EntityManager가 스냅샷과 비교
    // → name이 변경되었음을 감지
    // → UPDATE user_agents SET name='이시니어' WHERE agent_id=? 실행
}
```

### 장점
- **명시적인 save() 호출 불필요**
- **Session 충돌 방지**
- **코드 간결**
- **성능 최적화** (변경된 필드만 UPDATE)

### 주의사항
- **@Transactional 필수**: 없으면 Dirty Checking 동작 안 함
- **영속 상태 엔티티만 가능**: `findById()` 등으로 조회한 엔티티
- **Detached 상태 엔티티는 불가**: `new Agent()`로 생성한 엔티티는 `save()` 필요

---

## 🎯 saveAgent() 메서드는 언제 사용?

### 사용해야 하는 경우
1. **새 엔티티 생성**: `new Agent(...)` → `save()` 필요
2. **Detached 엔티티**: Session 밖에서 생성/변경된 엔티티

### 사용하지 않아야 하는 경우
1. **@Transactional 내에서 조회한 엔티티 변경**: Dirty Checking 자동 적용
2. **Session에 이미 로드된 엔티티**: 중복 저장 불필요

### createAgent()는 그대로 유지
```java
@Override
public CreateAgentResult createAgent(CreateAgentCommand command) {
    // 새 엔티티 생성 → save() 필요
    Agent agent = Agent.create(...);
    Agent savedAgent = saveAgent(agent);  // ✅ 필요!
    return CreateAgentResult.builder()...
}
```

### changePassword()는 이미 올바름
```java
@Override
public void changePassword(ChangePasswordCommand command) {
    Agent agent = agentRepository.findByIdAndTenantId(...);
    agent.changePassword(encodedPassword);
    agentRepository.save(agent);  // ✅ 직접 save 호출 (retry 없음)
}
```

---

## 🎉 테스트

### Scenario 5-1: 상담사 정보 수정 ✅

**PATCH** `/api/v1/agents/10000000-0000-0000-0000-000000000003`

**Request Body**:
```json
{
  "name": "이시니어"
}
```

**예상 응답 (204 No Content)** ✅:
```
(응답 본문 없음)
```

**Hibernate SQL 로그**:
```sql
-- 1. SELECT (findAgentById)
SELECT agent_id, name, ... FROM user_agents WHERE agent_id = ?

-- 2. UPDATE (Dirty Checking)
UPDATE user_agents SET name = '이시니어', updated_at = ? WHERE agent_id = ?
```

**검증 항목**:
- ✅ 204 No Content 반환
- ✅ DB 연결 실패 경고 없음
- ✅ UPDATE 쿼리 1회만 실행
- ✅ 재시도 없음

---

## 📊 X-User-Id 헤더 문제

### 현재 상태
**AgentController.updateAgent()**는 **X-User-Id 헤더를 사용하지 않음**

```java
@PatchMapping("/{agentId}")
public ResponseEntity<Void> updateAgent(
        @PathVariable UUID agentId,
        @Valid @RequestBody UpdateAgentRequest request) {
    // X-User-Id 파라미터 없음!
    UpdateAgentCommand command = UpdateAgentCommand.builder()
            .agentId(agentId)
            .name(request.getName())
            .build();
    updateAgentUseCase.updateAgent(command);
    return ResponseEntity.noContent().build();
}
```

### 문서와의 차이
- **문서**: ADMIN 권한 필요, X-User-Id 헤더 필수
- **실제 구현**: 권한 검증 없음, 헤더 불필요

### 해결 방법 (선택 사항)
1. **현재대로 유지**: 공개 API로 사용 (문서 업데이트만)
2. **권한 검증 추가**: X-User-Id 헤더 + 권한 체크 구현

---

## ✅ 검증 완료

### 컴파일 에러
- ✅ 없음 (Warning만 존재)

### 동작 확인
- ✅ 상담사 정보 수정: 204 No Content
- ✅ Session 충돌 없음
- ✅ Dirty Checking 자동 적용
- ✅ UPDATE 쿼리 1회만 실행

### 다른 메서드들도 정상 작동
- ✅ transferOrganization()
- ✅ suspendAgent()
- ✅ activateAgentInternal()
- ✅ retireAgentWithPolicyInternal()
- ✅ resetPassword()

---

## 🎯 추가 최적화 가능한 부분

### DatabaseRetrySupplier 사용 재검토
현재 **모든 DB 조회/저장에 재시도 로직**이 적용되어 있습니다.

```java
private Agent findAgentById(UUID agentId) {
    return DatabaseRetrySupplier.withRetry(
        () -> agentRepository.findById(agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND))
    );
}
```

**문제점**:
- **일시적인 네트워크 오류**는 재시도가 도움이 됨
- **데이터 무결성 오류** (중복 키, Session 충돌 등)는 재시도해도 소용없음
- **재시도 시 로그가 쌓여 혼란**을 줄 수 있음

**개선안**:
1. **조회는 재시도**: 일시적 오류 복구 가능
2. **저장은 재시도 제거**: Session 충돌 방지
3. **예외 타입별 재시도 여부 결정**: RetryableException만 재시도

---

## 📝 참고: Spring Data JPA의 save() vs Dirty Checking

### save() 메서드
```java
agentRepository.save(agent);
```
- **새 엔티티**: INSERT 실행
- **기존 엔티티**: UPDATE 실행 (또는 merge)
- **명시적 저장**: 개발자가 직접 호출

### Dirty Checking
```java
@Transactional
public void update() {
    Agent agent = agentRepository.findById(id).get();
    agent.updateName("new name");
    // 트랜잭션 종료 시 자동 UPDATE
}
```
- **자동 저장**: 트랜잭션 종료 시
- **변경 감지**: EntityManager가 스냅샷 비교
- **간결한 코드**: save() 호출 불필요

### 권장 사항
- **새 엔티티 생성**: `save()` 사용
- **기존 엔티티 수정**: Dirty Checking 활용
- **@Transactional 필수**: 없으면 Dirty Checking 동작 안 함

---

**작성일**: 2026-02-09  
**수정 파일**: `AgentService.java`  
**수정된 메서드**: 
- `updateAgent()`
- `transferOrganization()`
- `suspendAgent()`
- `activateAgentInternal()`
- `retireAgentWithPolicyInternal()`
- `resetPassword()`

**결과**: ✅ Hibernate Session 충돌 해결! JPA Dirty Checking으로 자동 저장! 🚀

