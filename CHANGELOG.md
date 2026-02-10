# 📋 프로젝트 변경 이력 (Changelog)

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

