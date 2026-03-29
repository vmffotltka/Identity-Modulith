# 📋 프로젝트 변경 이력 (Changelog)

## 🎯 v4.0.1 - 2026-03-29

### ✨ 주요 개선 사항

#### 1️⃣ **Organization Fetch Join 성능 평가 정리**
- ✅ `JpaDepartmentRepository`에 parent Fetch Join 조회 메서드 6종 적용
- ✅ `DepartmentServiceImpl`, `AgentOrgUserAdapter`의 부서 조회 경로 Fetch Join 전환
- ✅ `DepartmentFetchJoinBenchmarkTest`로 LAZY vs FETCH JOIN 비교 로그 정리

#### 2️⃣ **벤치마크 회귀 기준 안정화**
- ✅ `DepartmentFetchJoinBenchmarkTest` 단정 로직을 "after < before"에서
  "쿼리 역행 금지(after <= before) + 쿼리 예산(after <= 2)"으로 조정
- ✅ 소규모/parent 공유 데이터셋에서 발생 가능한 동률(1 vs 1) 케이스를 정상 시나리오로 반영

#### 3️⃣ **성능 문서 보강**
- ✅ `Docs/PERFORMANCE_OPTIMIZATION.md`에 Organization Fetch Join 결과/해석 추가
- ✅ `Docs/PERFORMANCE_OPTIMIZATION_N_PLUS_1.md`에 RBAC 전용 범위 명시 및 교차 참조 추가

---

## 🎯 v4.0.0 - 2026-03-11

### ✨ 주요 개선 사항

#### 1️⃣ **Keycloak SAML 연동 완성**
- ✅ **SAML 2.0 인증 흐름 구현**: Keycloak → SP(Spring Boot) SSO
- ✅ **Saml2AuthenticationSuccessHandler**: 인증 성공 시 로컬 DB Agent 매핑
- ✅ **Saml2AuthenticationFailureHandler**: 인증 실패 시 상세 로그 + 리디렉션
- ✅ **SamlSecurityContextFilter**: SAML 세션 → JwtUserContext 동기화
- ✅ **V3_0_0__Add_Keycloak_Test_Accounts.sql**: `test.admin` 계정 DB 자동 등록
- ✅ **AuthnRequest 서명**: DISABLED (Keycloak 호환)
- ✅ **Assertion 암호화**: DISABLED (Keycloak 호환)
- ✅ **Sign Assertions**: ON (IdP가 assertion만 서명)

#### 2️⃣ **전체 패키지 구조 리팩터링**
- ✅ **common/security 하위 디렉터리 정리**
  - `context/`: TenantContextHolder, JwtUserContext
  - `filter/`: SamlSecurityContextFilter
  - `handler/`: Saml2AuthenticationSuccessHandler, Saml2AuthenticationFailureHandler
  - `principal/`: AuthPrincipal
- ✅ **불필요 파일 삭제**: 중복 클래스, 테스트 전용 파일 정리
- ✅ **DepartmentController 한글 깨짐 수정**: BOM 제거 (UTF-8 without BOM)

#### 3️⃣ **User 모듈 UseCase 파일 병합 완료 확인**
- ✅ `AgentService`가 11개 UseCase 인터페이스 전부 구현 확인
- ✅ UseCase 인터페이스 파일 분리 유지 (헥사고날 아키텍처 의도 보존)

#### 4️⃣ **DTO 구조 통일 — 세 모듈 전체**
- ✅ **RBAC 모듈**: `rbac/presentation/dto/request/`, `response/` 분리 완료
- ✅ **Organization 모듈**: `organization/presentation/dto/request/`, `response/` 분리 완료
- ✅ **User 모듈**: 기존 분리 구조 유지
- ✅ **공통 에러 응답**: `ApiErrorResponse`로 세 모듈 완전 통일

#### 5️⃣ **공통 예외 처리 계층 완성**
- ✅ **CommonExceptionHandler** 추가: `@Slf4j` 누락 수정, 전역 fallback 처리
- ✅ **GlobalExceptionHandler** (User): `@Slf4j` 정상화
- ✅ **RbacExceptionHandler**: `RbacErrorCode.AGENT_NOT_FOUND` 오류 수정
- ✅ **OrganizationExceptionHandler**: 부서코드 중복 별도 처리
- ✅ **HttpStatus 통일**: 모든 핸들러 동일 응답 코드 체계 적용

#### 6️⃣ **보안 관련 클래스 `@Slf4j` 수정**
- ✅ `Saml2SecurityConfig` — `log` 필드 누락으로 컴파일 오류 수정
- ✅ `SamlTestController` — `@Slf4j` 추가
- ✅ `Saml2AuthenticationSuccessHandler` — `@Slf4j` 추가
- ✅ `Saml2AuthenticationFailureHandler` — `@Slf4j` 추가
- ✅ `SamlSecurityContextFilter` — `@Slf4j` 추가
- ✅ `CustomPermissionEvaluator` — `@Slf4j` 추가
- ✅ `JwtUserContext` — `@Slf4j` 추가
- ✅ `TenantContextHolder` (context 패키지) — `@Slf4j` 추가

#### 7️⃣ **`AgentExternalInfo` record 변환**
- ✅ Lombok `@Builder` 대신 Java record + builder 패턴으로 변경
- ✅ `isActive()`, `getId()`, `getName()`, `getTenantId()`, `getLoginId()` 메서드 접근 정상화

#### 8️⃣ **중복 클래스 제거**
- ✅ `AgentMapper` 중복 선언 오류 수정
- ✅ `GlobalExceptionHandler` 중복 선언 오류 수정

#### 9️⃣ **N+1 성능 벤치마크 테스트 안정화**
- ✅ `RbacPerformanceBenchmarkTest`: 외래 키 위반, 낙관적 잠금 오류 수정
- ✅ JdbcTemplate 직접 INSERT/DELETE로 Hibernate 캐시 영향 배제
- ✅ 테스트 격리: `@AfterEach`에서 FK 역순 정리

#### 🔟 **RBAC 테스트 DTO 정합성 수정**
- ✅ `RbacManagementServiceImplTest`: `CreateRoleRequest`, `CreatePermissionRequest` import 경로 수정
- ✅ `RbacAgentRoleManagementTest`: `assignRole()` NPE 수정
- ✅ `RoleResponse` record 생성자 불일치 수정

### 📊 N+1 성능 최적화 실측 결과

| 메서드 | Before | After | 개선율 | 쿼리 변화 |
|--------|:------:|:-----:|:------:|:---------:|
| `getEffectivePermissions` | **255 ms** | **10 ms** | **96.1% ↓** | 26 → 1 |
| `permissionsOfRoles` | **230 ms** | **12 ms** | **94.8% ↓** | 26 → 2 |

> 측정 환경: 로컬 PostgreSQL / 역할 5개 / 역할당 권한 4개 (총 20개) / 워밍업 3회 + 측정 10회

### 🐛 버그 수정

- 🐛 `Saml2SecurityConfig.signingX509Credentials(List::clear)` — 메서드 레퍼런스 타입 불일치 수정
- 🐛 `AgentExternalInfo.isActive()` 미존재 — record 변환으로 해결
- 🐛 `rbac_agent_roles` FK 위반 (벤치마크 테스트) — `user_agents` 먼저 삽입하도록 순서 수정
- 🐛 `DepartmentController.java` BOM 문자 `\ufeff` 제거
- 🐛 `X-User-Id` 헤더 — SecurityContext에서 직접 추출하도록 Controller 개선
- 🐛 `RbacErrorCode.AGENT_NOT_FOUND` 미존재 → `USER_NOT_FOUND`로 교체

### 📁 추가/수정된 파일

**추가 (10개)**:
```
common/exception/CommonExceptionHandler.java
common/security/context/JwtUserContext.java
common/security/context/TenantContextHolder.java (context 패키지로 이동)
common/security/filter/SamlSecurityContextFilter.java
common/security/handler/Saml2AuthenticationSuccessHandler.java
common/security/handler/Saml2AuthenticationFailureHandler.java
common/security/principal/AuthPrincipal.java
Docs/ARCHITECTURE_DDD_MODULITH.md (신규 작성)
Docs/EXCEPTION_AND_LOGGING.md (신규 작성)
Docs/PERFORMANCE_OPTIMIZATION_N_PLUS_1.md (신규 작성)
Docs/PERFORMANCE_OPTIMIZATION.md (신규 작성)
```

**수정 (20개)**:
```
Saml2SecurityConfig.java — @Slf4j 추가, signingX509Credentials 수정
SamlTestController.java — @Slf4j 추가
Saml2AuthenticationSuccessHandler.java — @Slf4j 추가, AgentExternalInfo 호환
AgentExternalInfo.java — record 패턴으로 변환
AgentMapper.java — 중복 제거
GlobalExceptionHandler.java — @Slf4j 추가, 중복 제거
CommonExceptionHandler.java — @Slf4j 추가
RbacExceptionHandler.java — AGENT_NOT_FOUND → USER_NOT_FOUND
DepartmentController.java — BOM 제거, X-User-Id 제거
RbacController.java — BatchPermissionRequest import 추가
RbacManagementServiceImpl.java — RoleResponse 생성자 수정
RbacManagementServiceImplTest.java — DTO 경로 수정
RbacAgentRoleManagementTest.java — NPE 수정
RbacPerformanceBenchmarkTest.java — FK/낙관적 잠금 수정
V3_0_0__Add_Keycloak_Test_Accounts.sql — 신규 추가
Docs/CHANGELOG.md — v4.0.0 추가
Docs/DATABASE/DATABASE_SCHEMA.md — V2 스키마 기준으로 전면 업데이트
Docs/ARCHITECTURE_DDD_MODULITH.md — 최신 패키지 구조 반영
Docs/EXCEPTION_AND_LOGGING.md — 최신 구현 반영
Docs/PERFORMANCE_OPTIMIZATION_N_PLUS_1.md — 실측 결과 반영
```

---

## 🎯 v3.1.0 - 2026-02-10

### ✨ 주요 개선 사항

#### 1️⃣ **비밀번호 암호화 BCrypt로 통일**
- ✅ **PasswordEncoderImpl 변경**: SHA-256 → BCrypt
- ✅ **SecurityConfig에 BCryptPasswordEncoder Bean 등록**
- ✅ **초기 데이터 비밀번호**: 모든 계정 `Admin123!`

#### 2️⃣ **RBAC 권한 검증 완전 구현**
- ✅ **RbacPort 확장**: hasRole(), hasPermission() 추가
- ✅ **RbacAdapter 구현**: rbac_agent_roles 테이블 연동
- ✅ **권한 검증 수정**: deprecated JSON 컬럼 제거

#### 3️⃣ **비밀번호 에러 코드 세분화**
- ✅ **P001**: PASSWORD_MISMATCH - 현재 비밀번호 불일치
- ✅ **P002**: PASSWORD_CONFIRMATION_MISMATCH - 확인 비밀번호 불일치
- ✅ **P003**: SAME_AS_CURRENT_PASSWORD - 동일한 비밀번호

#### 4️⃣ **부서 이동 검증 강화**
- ✅ **존재하지 않는 부서 이동 시 404 반환**
- ✅ **부서 존재 확인 로직 추가**

#### 5️⃣ **X-User-Id 헤더 일관성**
- ✅ **비밀번호 변경 API**: X-User-Id 필수 추가
- ✅ **모든 수정/삭제 API**: X-User-Id 필수 적용

#### 6️⃣ **Bean Validation 에러 처리**
- ✅ **GlobalExceptionHandler**: MethodArgumentNotValidException 처리
- ✅ **상세한 검증 메시지 반환**

#### 7️⃣ **개발 디버깅 도구**
- ✅ **DevController**: 비밀번호 해시 생성/검증 API

### 🐛 버그 수정

- 🐛 PasswordEncoder 불일치 (SHA-256 vs BCrypt)
- 🐛 confirmPassword 필드 누락
- 🐛 @Setter 누락으로 JSON 역직렬화 실패
- 🐛 hasRole() 메서드 누락
- 🐛 부서 존재 확인 누락 (200 OK → 404 수정)
- 🐛 GlobalExceptionHandler 불완전

### 📋 API 테스트 진행 상황

- ✅ **Organization API**: 전체 완료 (Scenario 1-12)
- 🔄 **Agent API**: Scenario 8까지 완료, **Scenario 9 진행 중**
- ⏳ **RBAC API**: 대기 중

### 📁 추가/수정된 파일

**추가 (14개)**:
```
DevController.java
GeneratePasswordHash.java
TestPasswordPattern.java
PASSWORD_CHANGE_SECURITY_FIX.md
PASSWORD_CONFIRM_FIX.md
PASSWORD_VALIDATION_FIX.md
ERROR_MESSAGE_IMPROVEMENT.md
PASSWORD_SETTER_ISSUE.md
BCRYPT_HASH_SOLUTION.md
ROOT_CAUSE_SOLVED.md
PASSWORD_ERROR_CODE_FIX.md
FINAL_PASSWORD_FIX.md
DEBUG_PASSWORD_CHANGE_400.md
FINAL_DEBUG_GUIDE.md
update_passwords.sql
check_password.sql
verify_current_password.sql
check_actual_password_hash.sql
```

**수정 (11개)**:
```
PasswordEncoderImpl.java
SecurityConfig.java
ChangePasswordRequest.java
AgentController.java
AgentService.java
GlobalExceptionHandler.java
ErrorCode.java
RbacPort.java
RbacAdapter.java
V2_0_0__Fixed_Schema.sql
API_TEST_SCENARIOS_AGENT.md
AssignRolesRequest.java
ManageRoleUseCase.java
```

---

## 🎯 v3.0.0 - 2026-02-05

### ✨ 주요 개선 사항

#### 1️⃣ **Port/Adapter 패턴 완전 적용**
- ✅ **모듈 간 직접 의존성 제거**
  - User → RBAC: `RbacPort` 인터페이스 + `RbacAdapter` 구현
  - Organization → User: `OrgUserPort` 인터페이스 + `AgentOrgUserAdapter` 구현
  - RBAC → User: `PermissionPort` 인터페이스 + `AgentPermissionAdapter` 구현
- ✅ **Infrastructure 레이어에서만 외부 모듈 의존**
  - Presentation/Application 레이어는 Port 인터페이스만 의존
  - 테스트 용이성 향상 (Mock 주입 가능)
- ✅ **DDD + Modular Monolith 아키텍처 강화**

#### 2️⃣ **Department 모듈 기능 확장**
- ✅ **DepartmentType Enum 추가**
  - `COMPANY`: 최상위 조직 (회사, 계열사)
  - `DIVISION`: 본부급 조직
  - `TEAM`: 팀급 조직
  - `GROUP`: 그룹/파트
  - `CUSTOM`: 사용자 정의 타입
- ✅ **부서 상태 관리 구현**
  - `activateDepartment()`: 부서 활성화
  - `deactivateDepartment()`: 부서 비활성화
  - `is_active` 컬럼 추가
- ✅ **추가 조회 API 구현**
  - 키워드 검색, 하위 부서 트리, 깊이별 조회, 타입별 조회, 부서별 사용자 목록

#### 3️⃣ **비즈니스 규칙 강화**
- ✅ **순환 참조 방지**: 부서를 자신이나 하위 부서로 이동 불가
- ✅ **삭제 제약 조건**: 하위 부서/소속 직원 존재 시 삭제 불가
- ✅ **에러 코드 체계화**: `DepartmentErrorCode` enum 추가

#### 4️⃣ **Swagger 문서 완벽 적용**
- ✅ **모든 Controller에 Swagger 어노테이션 추가**
  - @Tag, @Operation, @ApiResponses, @Parameter
- ✅ **Swagger UI 접근**: `http://localhost:8080/swagger-ui/index.html`

#### 5️⃣ **데이터베이스 구조 개선**
- ✅ **테이블명 표준화**: `org_departments`, `rbac_roles`, `rbac_permissions` 등
- ✅ **단일 마이그레이션 파일**: `V1_0_0__Complete_Init.sql`
- ✅ **초기 데이터**: 역할 8개, 권한 35개, 샘플 사용자 3개, 샘플 부서 4개

#### 6️⃣ **문서화 대폭 강화**
- ✅ **새 문서 추가**: `ORGANIZATION_API_TEST_GUIDE.md`
- ✅ **기존 문서 업데이트**: `README.md`, `DB_COMPREHENSIVE_GUIDE.md`, `CHANGELOG.md`

### 📁 주요 파일 변경

**추가된 파일**:
```
✅ Docs/ORGANIZATION_API_TEST_GUIDE.md                    - Organization API 테스트 가이드
✅ organization/domain/model/DepartmentType.java          - 부서 타입 Enum
✅ organization/exception/DepartmentErrorCode.java        - 에러 코드 Enum
✅ user/application/port/RbacPort.java                    - RBAC 연동 인터페이스
✅ user/infrastructure/adapter/RbacAdapter.java           - RbacPort 구현체
```

**수정된 파일**:
```
🔧 README.md                                              - v3.0.0 전체 리마스터
🔧 organization/domain/model/DepartmentEntity.java        - DepartmentType 적용
🔧 organization/application/service/DepartmentServiceImpl.java - 추가 API 구현
🔧 organization/presentation/DepartmentController.java    - Swagger 완벽 적용
🔧 모든 테스트 파일                                        - DepartmentType 적용
```

### 🐛 버그 수정
- 🐛 **순환 의존성 해결**: Port/Adapter 패턴으로 모듈 간 순환 참조 제거
- 🐛 **Flyway 버전 충돌**: V1_0_13 중복 파일 제거
- 🐛 **에러 코드 누락**: `INSUFFICIENT_PERMISSION` 에러 코드 추가
- 🐛 **테스트 실패**: DepartmentType Enum 적용으로 테스트 코드 수정

---

## 🎯 v2.0.0 - 2026-01-21

### ✨ 주요 개선 사항

#### 1️⃣ **데이터베이스 표준화**
- ✅ **모든 PK를 UUID로 통일** (VARCHAR(50))
- ✅ **명명 규칙 통일** (snake_case, 소문자)
- ✅ **핵심 테이블 6개 유지**
  ```
  1. org_departments        (조직 관리)
  2. user_agents            (사용자 관리)
  3. rbac_roles             (역할 관리)
  4. rbac_permissions       (권한 관리)
  5. rbac_role_permissions  (역할-권한 매핑)
  6. user_agent_roles       (사용자-역할 매핑)
  ```
- ✅ **표준 데이터 자동 삽입**
  - 31개 권한 (AGENT:9, DEPARTMENT:6, RBAC:6, CHANNEL:10)
  - 8개 역할 (ADMIN, TEAM_LEAD, AGENT, 채널별 역할 등)
  - 3개 샘플 사용자 (admin, teamlead01, agent01)
  - 4개 샘플 부서 (넥스프론, 고객서비스본부, 인바운드팀, 아웃바운드팀)

#### 2️⃣ **RBAC 레벨 수정**
- ❌ **계층형 RBAC (Level 2)** 제거
- ✅ **Flat RBAC (Level 1)** 적용
  - 이유: 상담사 환경에서는 채널별/스킬별 권한이 계층보다 중요
  - 예: Inbound, Outbound, Chat, Phone 채널별 권한

#### 3️⃣ **코드 품질 개선**
- ✅ **주석 대폭 강화** - 모든 Entity, Service, Controller 상세 주석 추가
- ✅ **한글 인코딩 정상화** - 테스트 파일 한글 깨짐 해결
- ✅ **불필요한 코드 제거** - Deprecated 메서드, 미사용 클래스 정리
- ✅ **테스트 코드 정비** - 93개 테스트 모두 통과

#### 4️⃣ **공통 모듈(Common) 추가**
- ✅ **TenantContextHolder** - 멀티테넌시 컨텍스트 관리
- ✅ **AuthPrincipal** - 인증 사용자 정보
- ✅ **UnauthorizedException** - 권한 예외 처리

---

## 📁 주요 파일 변경

### 추가된 파일
```
✅ common/security/TenantContextHolder.java               - 멀티테넌시 핵심
✅ common/security/AuthPrincipal.java                     - 인증 정보
✅ common/security/UnauthorizedException.java             - 권한 예외
✅ DB_COMPREHENSIVE_GUIDE.md                              - DB 설계 가이드
✅ CHANGELOG.md                                           - 이 파일
```

### 수정된 파일
```
🔧 README.md                 - Swagger 인증 정보 업데이트
🔧 application.yml           - Flyway 검증 비활성화
🔧 .gitignore                - 테스트 출력 파일 추가
🔧 모든 Entity 파일           - 상세 주석 추가
🔧 모든 Service 파일          - 멀티테넌시 적용
```

---

## 📖 추가 문서

- **README.md** - 프로젝트 개요 및 실행 방법
- **DB_COMPREHENSIVE_GUIDE.md** - DB 구조 및 컬럼 설명

