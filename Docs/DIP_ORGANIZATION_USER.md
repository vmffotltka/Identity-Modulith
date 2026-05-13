# Organization ↔ User DIP 적용 근거 문서

> 최종 업데이트: 2026-04-11  
> 대상: Modular Monolith 모듈 간 의존성 (Organization, User)

---

## 1) 한 줄 결론

Organization 모듈은 User 모듈 구현을 직접 참조하지 않고, `OrgUserPort`(내부 포트) + `UserModuleApi`(상대 모듈 공개 API) 조합으로 의존성을 역전해 사용하고 있습니다.

---

## 2) 왜 인터페이스를 도입했는가

### 문제(직접 참조 시)
- Application 계층이 타 모듈의 Service/Repository에 직접 의존하면 모듈 경계가 무너짐
- 테스트에서 타 모듈 전체를 같이 띄워야 해서 단위 테스트 비용 증가
- 순환 의존(Organization -> User, User -> Organization) 위험 증가

### 목표
- Application 계층은 "무엇이 필요한지(포트)"만 선언
- Infrastructure 계층이 "어떻게 연결할지(어댑터)"를 담당
- 모듈 간 통신은 루트 공개 API(`*ModuleApi`)만 사용

---

## 3) Before / After (면접 설명용)

### Before (개념)
```java
// Organization Application이 User 구현을 직접 참조하는 형태 (개념 예시)
@Service
public class DepartmentServiceImpl {
    private final AgentService agentService; // 타 모듈 구현체 직접 의존
}
```

### After (현재 코드)
```java
// Organization Application은 포트만 의존
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl {
    private final OrgUserPort orgUserPort;
}

// Infrastructure에서 실제 UserModuleApi 연결
@Service
@Primary
public class AgentOrgUserAdapter implements OrgUserPort {
    private final UserModuleApi userModuleApi;
}
```

핵심은 "고수준 정책(DepartmentServiceImpl)"이 "저수준 구현(AgentService)"에 직접 의존하지 않는다는 점입니다.

---

## 4) 실제 적용 위치 (코드 근거)

### A. Organization -> User 방향 DIP

1. 포트 선언(Organization 내부)
- `src/main/java/com/identitymodulith/organization/application/port/OrgUserPort.java`
- Organization Application이 필요한 사용자 조회 기능만 선언

2. 포트 사용(Application 계층)
- `src/main/java/com/identitymodulith/organization/application/service/DepartmentServiceImpl.java`
- `private final OrgUserPort orgUserPort;` 주입 후 사용
- 예: `deleteDepartment`, `deactivateDepartment`, `getDepartmentStatistics`, `getDepartmentMembers`, `getAccessibleDepartmentIds`

3. 포트 구현(Infrastructure 계층)
- `src/main/java/com/identitymodulith/organization/infrastructure/adapter/AgentOrgUserAdapter.java`
- `implements OrgUserPort`
- 내부에서 `UserModuleApi` 호출
- 생성자에 `@Lazy UserModuleApi`를 사용해 순환 초기화 리스크 완화

4. 상대 모듈 공개 API
- `src/main/java/com/identitymodulith/user/UserModuleApi.java`
- `findActiveAgentsByOrganizationId`, `findAgentById` 등 모듈 간 통신용 계약 제공

5. 공개 API 구현
- `src/main/java/com/identitymodulith/user/application/AgentService.java`
- `AgentService implements UserModuleApi`

### B. 반대 방향(User -> Organization)도 동일 패턴

1. 포트 선언(User 내부)
- `src/main/java/com/identitymodulith/user/application/port/OrganizationPort.java`

2. 포트 구현(User Infrastructure)
- `src/main/java/com/identitymodulith/user/infrastructure/adapter/OrganizationPortAdapter.java`
- `OrganizationModuleApi`를 주입받아 DTO 변환

3. 상대 모듈 공개 API
- `src/main/java/com/identitymodulith/organization/OrganizationModuleApi.java`
- `src/main/java/com/identitymodulith/organization/application/service/DepartmentServiceImpl.java`에서 구현

즉, 양방향 호출이 있어도 "Application은 포트만", "실제 모듈 호출은 어댑터만"이라는 규칙을 지키고 있습니다.

---

## 5) 장점 / 단점 (트레이드오프)

### 장점
- 결합도 감소: Application이 타 모듈 구현 변경에 덜 영향받음
- 테스트 용이성: `OrgUserPort`, `OrganizationPort`를 Mock으로 대체 가능
- 모듈 경계 명확: 공개 API(`*ModuleApi`) 외 내부 구현 접근 차단
- MSA 전환 유리: 어댑터만 HTTP/gRPC 클라이언트로 교체하면 됨

### 단점
- 코드량 증가: 포트/어댑터/DTO 변환 계층이 추가됨
- 추적 복잡도 증가: 호출 경로가 한 단계 더 길어짐
- 계약 설계 부담: 포트 시그니처를 잘못 설계하면 재작업 비용 큼
- 런타임 바인딩 이슈 가능: 구현체 Bean 누락 시 시작 단계에서 실패

---

## 6) 면접에서 자주 나오는 질문 포인트

1. "왜 인터페이스를 늘렸나요?"
- 구현 교체보다도 "모듈 경계 보호"와 "테스트 독립성"이 1차 목적이라고 답변

2. "그냥 Service 직접 주입하면 안 되나요?"
- 단기적으로는 가능하지만 모듈 내부 구조가 외부로 새고, 순환 의존/변경 전파 위험이 커진다고 설명

3. "실제로 어디에 적용했나요?"
- `DepartmentServiceImpl -> OrgUserPort -> AgentOrgUserAdapter -> UserModuleApi` 흐름을 파일명으로 바로 제시

4. "단점은요?"
- 추상화 과다 시 오히려 가독성 저하, 포트 설계 미스 시 비용 증가를 인정하고 기준(핵심 유스케이스 중심 포트 설계) 제시

---

## 7) 검증 체크리스트

- [ ] Organization Application 계층에서 User 구현체 직접 import가 없는가?
- [ ] Organization -> User 호출이 `OrgUserPort`를 통해서만 이루어지는가?
- [ ] 실제 모듈 API 호출(`UserModuleApi`)은 Infrastructure Adapter에만 존재하는가?
- [ ] User -> Organization도 동일하게 `OrganizationPort` 패턴을 따르는가?
- [ ] 공개 API가 루트 패키지(`UserModuleApi`, `OrganizationModuleApi`)에 위치하는가?

참고 문서:
- `Docs/ARCHITECTURE_DDD_MODULITH.md`
- `Docs/CHANGELOG.md` (v3.0.0 Port/Adapter 패턴 적용 항목)

