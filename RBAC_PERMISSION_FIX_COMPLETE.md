# 🎉 RBAC 권한 검증 구현 완료!

## ✅ 해결된 문제들

### 1. **Agent.getRoles() 문제**
- **문제**: Agent 엔티티가 deprecated된 `role_id` JSON 컬럼에서 역할을 가져옴
- **실제**: 역할 데이터는 `rbac_agent_roles` 테이블에 저장됨
- **해결**: RbacPort를 통해 RBAC 모듈에서 역할 조회

### 2. **권한 검증 실패**
- **문제**: `agent.getRoles()`가 빈 배열을 반환하여 ADMIN 검증 실패
- **해결**: `rbacPort.hasRole(agentId, "ADMIN")` 사용

### 3. **부서 존재 확인 누락**
- **문제**: 존재하지 않는 부서로 이동 시 200 OK 반환
- **해결**: `organizationPort.getDepartmentInfo()` 호출하여 404 반환

---

## 🔧 구현된 기능

### 1. RbacPort 인터페이스 확장
```java
public interface RbacPort {
    void assignRoleToAgent(String agentId, String roleName);
    void revokeRoleFromAgent(String agentId, String roleName);
    boolean roleExists(String roleName);
    boolean hasRole(String agentId, String roleName);  // ✅ 추가
}
```

### 2. RbacAdapter 구현
```java
@Override
public boolean hasRole(String agentId, String roleName) {
    log.debug("[User->RBAC] 역할 보유 확인 - agentId={}, roleName={}", agentId, roleName);
    try {
        return rbacManagementService.hasRole(agentId, roleName);
    } catch (Exception e) {
        log.warn("[User->RBAC] 역할 확인 실패 - agentId={}, roleName={}, error={}", 
                agentId, roleName, e.getMessage());
        return false;
    }
}
```

### 3. RbacManagementService 확장
```java
@Override
@Transactional(readOnly = true)
public boolean hasRole(String agentId, String roleName) {
    Set<String> roles = getRolesByAgent(agentId);
    boolean result = roles.contains(roleName);
    log.debug("[RBAC] 역할 확인 - agentId={}, roleName={}, hasRole={}", agentId, roleName, result);
    return result;
}
```

### 4. AgentService 권한 검증 수정
```java
// ❌ Before: Agent 도메인 모델에서 직접 조회 (비어있음)
Agent actor = agentRepository.findByIdAndTenantId(actorId, tenantId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

boolean isAdmin = actor.getRoles().stream()
        .anyMatch(role -> "ADMIN".equals(role.getName()));

// ✅ After: RbacPort를 통해 RBAC 모듈에서 조회
boolean isAdmin = rbacPort.hasRole(actorId.toString(), "ADMIN");
```

---

## 📊 수정된 파일

### 인터페이스 & 포트
1. `RbacPort.java` - `hasRole()` 메서드 추가
2. `RbacManagementService.java` - `hasRole()` 메서드 추가
3. `ManageRoleUseCase.java` - `validateAdminPermission(tenantId, actorId)` 시그니처 변경

### 구현체
4. `RbacAdapter.java` - `hasRole()` 구현
5. `RbacManagementServiceImpl.java` - `hasRole()` 구현
6. `AgentService.java` - 4개 메서드 수정:
   - `updateAgent()` - RbacPort 사용
   - `transferOrganization()` - RbacPort 사용
   - `resetPassword()` - RbacPort 사용
   - `validateAdminPermission()` - RbacPort 사용

### 에러 코드
7. `ErrorCode.java` - `ORGANIZATION_NOT_FOUND` 추가

---

## 🧪 테스트 시나리오

### 1. 부서 이동 (존재하는 부서)
```bash
curl -X PATCH "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/organization" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000001" \
  -H "Content-Type: application/json" \
  -d '{"organizationId": "00000000-0000-0000-0000-000000000005"}'

예상: 204 No Content ✅
```

### 2. 부서 이동 (존재하지 않는 부서)
```bash
curl -X PATCH "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/organization" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000001" \
  -H "Content-Type: application/json" \
  -d '{"organizationId": "99999999-9999-9999-9999-999999999999"}'

예상: 404 Not Found
{
  "code": "A006",
  "message": "이동할 부서를 찾을 수 없습니다."
}
```

### 3. 권한 없는 사용자로 부서 이동 시도
```bash
curl -X PATCH "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/organization" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000003" \
  -H "Content-Type: application/json" \
  -d '{"organizationId": "00000000-0000-0000-0000-000000000005"}'

예상: 400 Bad Request
{
  "code": "A005",
  "message": "관리자만 상담사 조직을 이동시킬 수 있습니다."
}
```

---

## 🔄 권한 검증 흐름

### Before (문제)
```
1. Controller
   ↓
2. AgentService
   ↓
3. AgentRepository.findById(actorId)  // Agent 조회
   ↓
4. agent.getRoles()  // ❌ role_id (JSON) 컬럼에서 읽음 → 빈 배열
   ↓
5. ADMIN 검증 실패 → 400 에러
```

### After (해결)
```
1. Controller
   ↓
2. AgentService
   ↓
3. RbacPort.hasRole(actorId, "ADMIN")  // Port 호출
   ↓
4. RbacAdapter → RbacManagementService  // RBAC 모듈 연동
   ↓
5. AgentRoleRepository 조회  // ✅ rbac_agent_roles 테이블
   ↓
6. ADMIN 검증 성공 → 정상 처리
```

---

## 📝 데이터 흐름

### DB 스키마
```sql
-- 역할 정의
rbac_roles (role_id, name, type)
  - 20000000-0000-0000-0000-000000000001, 'ADMIN', 'POSITION'
  - 20000000-0000-0000-0000-000000000002, 'TEAM_LEAD', 'POSITION'
  - 20000000-0000-0000-0000-000000000003, 'MEMBER', 'POSITION'

-- 사용자-역할 매핑
rbac_agent_roles (agent_id, role_id)
  - 10000000-0000-0000-0000-000000000001, 20000000-0000-0000-0000-000000000001  // admin → ADMIN
  - 10000000-0000-0000-0000-000000000002, 20000000-0000-0000-0000-000000000002  // dev.lead → TEAM_LEAD
  - 10000000-0000-0000-0000-000000000003, 20000000-0000-0000-0000-000000000003  // dev.member → MEMBER

-- user_agents.role_id 컬럼 (deprecated)
user_agents (agent_id, role_id)
  - ❌ NULL 또는 빈 JSON (사용 안 함)
```

### 권한 조회 흐름
```java
// 1. AgentService에서 RbacPort 호출
boolean isAdmin = rbacPort.hasRole("10000000-0000-0000-0000-000000000001", "ADMIN");

// 2. RbacAdapter가 RbacManagementService 호출
rbacManagementService.hasRole(agentId, roleName);

// 3. RbacManagementService가 DB 조회
Set<String> roles = getRolesByAgent(agentId);
// → ["ADMIN"] 반환

// 4. 역할 확인
return roles.contains("ADMIN");  // true
```

---

## ✅ 컴파일 확인

```bash
.\gradlew compileJava

BUILD SUCCESSFUL
```

---

## 🎯 다음 단계

1. ✅ 코드 수정 완료
2. ✅ 컴파일 성공
3. ⏳ 애플리케이션 실행 중...
4. ⏳ Swagger UI 접속
5. ⏳ API 테스트 (부서 이동, 권한 검증)

---

## 💡 핵심 개선 사항

### 1. Port-Adapter 패턴 적용
- ✅ User 모듈이 RBAC 모듈을 직접 의존하지 않음
- ✅ RbacPort 인터페이스를 통한 간접 연동
- ✅ 모듈 간 결합도 최소화

### 2. 역할 데이터의 단일 진실 공급원(Single Source of Truth)
- ✅ `rbac_agent_roles` 테이블만 사용
- ✅ Deprecated된 `role_id` JSON 컬럼 무시
- ✅ RBAC 모듈에서 권한 관리 일원화

### 3. 명확한 에러 메시지
- ✅ "관리자만 상담사 조직을 이동시킬 수 있습니다."
- ✅ "이동할 부서를 찾을 수 없습니다."
- ✅ 사용자가 이해하기 쉬운 에러 메시지

---

## 🎉 완료!

이제 **RBAC 모듈과 연동하여 실제 역할 데이터로 권한 검증**을 수행합니다!

애플리케이션이 시작되면 Swagger UI에서 테스트해보세요:
```
http://localhost:8080/swagger-ui/index.html
```

