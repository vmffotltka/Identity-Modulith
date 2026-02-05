# Identity Modulith

Spring Boot와 Spring Modulith 기반의 모듈식 모놀리스 아키텍처를 적용한 멀티테넌트 ID 관리 시스템입니다.

## ⚡ Quick Start

```bash
# 1. 애플리케이션 실행 (Flyway 자동 마이그레이션)
./gradlew bootRun

# 2. Swagger UI 접속
http://localhost:8080/swagger-ui/index.html

# 🔐 Swagger 인증 정보 (Spring Security Basic Auth)
# 1. 애플리케이션 시작 후 콘솔에서 비밀번호 확인
#    출력 예시: "Using generated security password: 851fc3af-19f7-404f-a1a6-eb51e6ad4aac"
# 
# 2. Swagger UI 우측 상단 "Authorize" 버튼 클릭
# 
# 3. 인증 정보 입력
#    - Username: user
#    - Password: 콘솔에서 복사한 비밀번호
#
# 4. "Authorize" 클릭 → "Close" 버튼으로 닫기
#
# 5. 이제 모든 API를 테스트할 수 있습니다!

# 3. 초기 데이터 확인
# - 관리자 계정: admin / password123
# - 팀장 계정: teamlead01 / password123
# - 상담사 계정: agent01 / password123
```

## 📋 최신 업데이트 (v3.0.0 - 2026-02-05)

### 🎯 주요 개선 사항
- ✅ **Port/Adapter 패턴 완전 적용** - 모듈 간 직접 의존성 제거
- ✅ **DDD + Modular Monolith 아키텍처** - 레이어별 명확한 역할 분리
- ✅ **Department Type Enum 추가** - COMPANY, DIVISION, TEAM, GROUP, CUSTOM
- ✅ **부서 상태 관리 구현** - 활성화/비활성화 API 추가
- ✅ **비즈니스 규칙 강화** - 순환 참조 방지, 삭제 제약 조건
- ✅ **Swagger 문서 완벽 적용** - 모든 API에 상세 설명 추가
- ✅ **통합 테스트 가이드** - Organization, Agent, RBAC 시나리오별 테스트 문서
## 🛠 기술 스택

- **Java 21** - LTS 버전, 최신 언어 기능 활용
- **Spring Boot 3.5.8** - 최신 스프링 부트 프레임워크
- **Spring Modulith 1.4.2** - 모듈형 모놀리스 아키텍처 지원
- **Spring Data JPA** - 데이터 영속성 계층
- **Spring Security** - 인증/인가 처리
- **PostgreSQL 18+** - 고성능 RDBMS
- **Flyway 11.7.2** - 데이터베이스 마이그레이션 자동화
- **Swagger/OpenAPI 3.0** - API 문서 자동 생성
- **Lombok** - 보일러플레이트 코드 제거
- **Gradle 9.2.1** - 빌드 도구

## 빌드 및 실행

```bash
# 빌드 (테스트 제외)
./gradlew build -x test

# 테스트 실행
./gradlew test

# 애플리케이션 실행
./gradlew bootRun
```

## 🗄️ 데이터베이스 구조

### 핵심 테이블 (5개)

| 테이블명 | 모듈 | 설명 |
|---------|------|------|
| **org_departments** | Organization | 조직 계층 구조 (트리) |
| **agents** | User | 사용자(상담사) 정보 |
| **rbac_roles** | RBAC | 역할 정의 (POSITION, CHANNEL) |
| **rbac_permissions** | RBAC | 권한 정의 |
| **rbac_role_permissions** | RBAC | 역할-권한 매핑 (M:N) |
| **rbac_agent_roles** | RBAC | 사용자-역할 매핑 (M:N) |

### 초기 데이터 (자동 생성)

**역할 (8개)**:
- POSITION: `ADMIN`, `TEAM_LEAD`, `AGENT` (직급 기반)
- CHANNEL: `INBOUND_AGENT`, `OUTBOUND_AGENT`, `CHAT_AGENT`, `EMAIL_AGENT`, `MULTI_CHANNEL_AGENT` (채널 기반)

**권한 (35개)**:
- AGENT: 상담사 생성, 조회, 수정, 삭제, 정지, 활성화, 이동, 역할할당, 비밀번호초기화 (9개)
- DEPARTMENT: 부서 생성, 조회, 수정, 삭제, 이동, 비활성화 (6개)
- RBAC: 역할 생성, 조회, 수정, 삭제, 권한 조회, 권한 할당 (6개)
- CHANNEL: 인바운드, 아웃바운드, 채팅, 이메일 채널별 권한 (14개)

**샘플 데이터**:
- 부서 4개: 넥스프론(본사) → 고객서비스본부 → 인바운드팀, 아웃바운드팀
- 사용자 3개: admin(관리자), teamlead01(팀장), agent01(상담사)

### 데이터베이스 초기화 (선택사항)

```bash
# 방법 1: Flyway 자동 마이그레이션 (권장)
# - 애플리케이션 실행 시 자동으로 V1_0_0__Complete_Init.sql 실행
./gradlew bootRun

# 방법 2: 수동 초기화 (완전 재설정 필요 시)
# - PostgreSQL 클라이언트에서 직접 실행
psql -U your_user -d your_database -f src/main/resources/db/migration/V1_0_0__Complete_Init.sql
```

### 데이터 확인 쿼리

```sql
-- 테이블 건수 확인
SELECT 
    'org_departments' as table_name, COUNT(*) as count FROM org_departments
UNION ALL SELECT 'agents', COUNT(*) FROM agents
UNION ALL SELECT 'rbac_roles', COUNT(*) FROM rbac_roles
UNION ALL SELECT 'rbac_permissions', COUNT(*) FROM rbac_permissions
UNION ALL SELECT 'rbac_role_permissions', COUNT(*) FROM rbac_role_permissions
UNION ALL SELECT 'rbac_agent_roles', COUNT(*) FROM rbac_agent_roles;

-- 예상 결과:
-- org_departments: 4개
-- agents: 3개
-- rbac_roles: 8개
-- rbac_permissions: 35개
-- rbac_role_permissions: 77개 (ADMIN 35개 + TEAM_LEAD 6개 + AGENT 3개 + 채널별 권한)
-- rbac_agent_roles: 6개
```

## 아키텍처

이 프로젝트는 **Spring Modulith + DDD Layered Architecture** 패턴을 따릅니다.

### 모듈 구조

```
com.nexfron.identitymodulith/
├── user/                    # User 모듈 (상담사 관리)
└── organization/            # Organization 모듈 (조직/부서 관리)
```

### DDD Layered Architecture (각 모듈 내부)

```
{module}/
├── presentation/     # 표현 계층 - Controller, DTO, 예외 핸들러
├── application/      # 응용 계층 - Application Service, UseCase 인터페이스
├── domain/           # 도메인 계층 - Entity, VO, Repository Interface, Domain Service Interface
└── infrastructure/   # 인프라스트럭처 계층 - Repository 구현체, 외부 시스템 연동
```

### 계층별 의존성 규칙

```
Presentation → Application → Domain ← Infrastructure
                               ↑          |
                               └──────────┘
                               (DIP: 인터페이스 구현)
```

- **Presentation**: 사용자 요청/응답 처리, 비즈니스 로직 포함 금지
- **Application**: 트랜잭션 관리, 도메인 객체 조정, "얇게(Thin)" 유지
- **Domain**: 핵심 비즈니스 로직, 어떤 레이어에도 의존하지 않음 (순수 Java)
- **Infrastructure**: Domain 인터페이스 구현 (DIP), 기술적 세부사항 담당

---

## User 모듈 (상담사 관리)

### 패키지 구조 (DDD Layered Architecture)

```
com.nexfron.identitymodulith.user/
│
├── presentation/                          # 표현 계층 (Presentation Layer)
│   ├── AgentController.java               # REST API Controller
│   ├── GlobalExceptionHandler.java        # 전역 예외 처리
│   └── dto/
│       ├── request/                       # 요청 DTO
│       │   ├── CreateAgentRequest.java
│       │   ├── UpdateAgentRequest.java
│       │   ├── TransferOrganizationRequest.java
│       │   └── AssignRolesRequest.java
│       └── response/                      # 응답 DTO
│           ├── AgentResponse.java
│           ├── CreateAgentResponse.java
│           ├── ResetPasswordResponse.java
│           └── ErrorResponse.java
│
├── application/                           # 응용 계층 (Application Layer)
│   ├── AgentService.java                  # Application Service (UseCase 구현체)
│   ├── CreateAgentUseCase.java            # UseCase 인터페이스
│   ├── ResetPasswordUseCase.java
│   ├── UpdateAgentUseCase.java
│   ├── RetireAgentUseCase.java
│   ├── GetAgentUseCase.java
│   ├── ManageRoleUseCase.java
│   └── CheckLoginIdUseCase.java
│
├── domain/                                # 도메인 계층 (Domain Layer) - 핵심!
│   ├── Agent.java                         # 상담사 엔티티 (Aggregate Root)
│   ├── AgentStatus.java                   # 상태 enum (ACTIVE, RETIRED)
│   ├── Role.java                          # 역할 값 객체 (직급, 채널)
│   ├── repository/                        # Repository Interface (DIP 적용)
│   │   └── AgentRepository.java
│   ├── service/                           # Domain Service Interface
│   │   ├── PasswordEncoder.java
│   │   └── PasswordGenerator.java
│   └── exception/                         # 도메인 예외
│       ├── BusinessException.java
│       └── ErrorCode.java
│
└── infrastructure/                        # 인프라스트럭처 계층 (Infrastructure Layer)
    └── persistence/
        ├── entity/
        │   └── AgentJpaEntity.java        # JPA Entity
        ├── repository/
        │   └── AgentJpaRepository.java    # Spring Data JPA Repository
        ├── AgentMapper.java               # Entity ↔ Domain 변환
        ├── AgentRepositoryImpl.java       # AgentRepository 구현체
        ├── PasswordEncoderImpl.java       # PasswordEncoder 구현체
        └── PasswordGeneratorImpl.java     # PasswordGenerator 구현체
```

### DIP(의존성 역전 원칙) 적용

Domain Layer에 정의된 인터페이스를 Infrastructure Layer에서 구현합니다:

| Domain Interface | Infrastructure 구현체 | 설명 |
|------------------|----------------------|------|
| `AgentRepository` | `AgentRepositoryImpl` | 상담사 데이터 저장소 |
| `PasswordEncoder` | `PasswordEncoderImpl` | 비밀번호 암호화 |
| `PasswordGenerator` | `PasswordGeneratorImpl` | 임시 비밀번호 생성 |

> **효과**: Domain Layer가 순수 Java 코드로 유지되어 테스트가 용이하고, Infrastructure 변경이 Domain에 영향을 주지 않습니다.

### 도메인 모델

| 클래스 | 설명 |
|--------|------|
| `Agent` | 콜센터 상담사 엔티티 (Aggregate Root) |
| `AgentStatus` | 상담사 상태 (`ACTIVE`, `RETIRED`) |
| `Role` | 역할 값 객체 - 직급(POSITION), 채널(CHANNEL) |

### API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/agents` | 상담사 생성 (임시 비밀번호 발급) |
| `GET` | `/api/v1/agents` | 상담사 목록 조회 |
| `GET` | `/api/v1/agents/{agentId}` | 상담사 상세 조회 |
| `GET` | `/api/v1/agents/check-login-id?loginId=` | 아이디 중복 체크 |
| `PATCH` | `/api/v1/agents/{agentId}` | 상담사 정보 수정 |
| `PATCH` | `/api/v1/agents/{agentId}/organization` | 조직 이동 |
| `POST` | `/api/v1/agents/{agentId}/reset-password` | 비밀번호 초기화 (임시 비밀번호 발급) |
| `DELETE` | `/api/v1/agents/{agentId}` | 상담사 퇴사 처리 (Soft Delete) |
| `PUT` | `/api/v1/agents/{agentId}/roles` | 역할 지정 |

### 비즈니스 규칙

#### 1. 상담사 생성 (Onboarding)

- 아이디 중복 체크 필수
- 시스템이 난수로 임시 비밀번호 생성
- 생성 API 응답에 `tempPassword` 포함 (일회성, 재조회 불가)
- 초기 설정: `passwordMustChange: true`, `status: ACTIVE`

#### 2. 역할 지정

- **역할(Role)**: 직급(POSITION), 채널(CHANNEL) 복수 선택 가능

#### 3. 정보 수정

- 기본 정보 변경, 조직 이동 가능
- 비밀번호 초기화 시 새 임시 비밀번호 팝업 노출

#### 4. 상담사 삭제 (Offboarding)

- Soft Delete 방식: `status`를 `RETIRED`로 변경
- `retiredAt` 퇴사 일시 기록
- 로그인 차단 및 상담 배정 제외
- 데이터는 통계/이력 조회를 위해 영구 보존

#### 5. 정보 조회

- 기본적으로 `ACTIVE` 상태 상담사만 조회
- `includeRetired=true` 파라미터로 퇴사자 포함 조회
- 조회 API에서 비밀번호(해시값 포함) 절대 리턴 금지

---

## Organization 모듈 (조직/부서 관리)

### 📁 패키지 구조 (DDD Layered Architecture)

```
com.nexfron.identitymodulith.organization/
│
├── presentation/                                # 표현 계층
│   ├── DepartmentController.java                 # 부서 관리 REST API
│   ├── OrganizationExceptionHandler.java         # 조직 모듈 전역 예외 핸들러
│   └── dto/
│       └── DepartmentDto.java                    # 요청/응답 DTO
│           ├── CreateRequest                     # 부서 생성 요청
│           ├── UpdateRequest                     # 부서 수정 요청
│           ├── MoveRequest                       # 부서 이동 요청
│           ├── Response                          # 부서 정보 응답
│           ├── Statistics                        # 부서 통계 응답
│           └── DepartmentMembers                 # 부서별 사용자 목록
│
├── application/                                  # 응용 계층
│   ├── port/                                     # 포트 (다른 모듈과의 인터페이스)
│   │   ├── OrgUserPort.java                      # User 모듈 연동 인터페이스
│   │   └── OrgUserView.java                      # 사용자 조직 정보 DTO
│   └── service/
│       ├── DepartmentServiceImpl.java            # 부서 관리 핵심 서비스
│       │   ├── createDepartment()                 # 부서 생성
│       │   ├── updateDepartment()                 # 부서 정보 수정
│       │   ├── moveDepartment()                   # 부서 이동 (하위 부서 경로 자동 재계산)
│       │   ├── deleteDepartment()                 # 부서 삭제 (검증 포함)
│       │   ├── getDepartmentTree()                # 전체 조직도 조회
│       │   ├── getDepartmentTreeWithinScope()     # 권한 범위 내 조직도 조회
│       │   ├── searchDepartments()                # 부서 검색
│       │   ├── getSubtree()                       # 하위 부서 트리 조회
│       │   ├── getDepartmentsByDepth()            # 깊이별 부서 조회
│       │   ├── getDepartmentsByType()             # 타입별 부서 조회
│       │   ├── getDepartmentStatistics()          # 부서 통계 조회
│       │   ├── getDepartmentMembers()             # 부서별 사용자 목록
│       │   ├── activateDepartment()               # 부서 활성화
│       │   └── deactivateDepartment()             # 부서 비활성화
│       └── DepartmentEntityService.java          # 부서 엔티티 서비스
│
├── domain/                                       # 도메인 계층
│   ├── model/
│   │   ├── DepartmentEntity.java                 # 부서 엔티티 (Aggregate Root)
│   │   │   ├── create()                          # 부서 생성 팩토리 메서드
│   │   │   ├── update()                          # 부서 정보 수정
│   │   │   ├── moveTo()                          # 부서 이동
│   │   │   ├── activate()                        # 부서 활성화
│   │   │   ├── deactivate()                      # 부서 비활성화
│   │   │   ├── canBeDeleted()                    # 삭제 가능 여부 검증
│   │   │   └── updatePathForSubtree()            # 하위 부서 경로 재계산
│   │   ├── DepartmentType.java                   # 부서 타입 enum
│   │   │   ├── COMPANY                            # 회사/최상위 조직
│   │   │   ├── DIVISION                           # 본부급 조직
│   │   │   ├── TEAM                               # 팀급 조직
│   │   │   ├── GROUP                              # 그룹
│   │   │   └── CUSTOM                             # 사용자 정의
│   │   └── DataScopeLevel.java                   # 데이터 스코프 레벨 enum
│   │       ├── ADMIN                              # 전체 데이터 접근
│   │       ├── TEAM_LEAD                          # 본인 부서 + 하위 부서
│   │       └── MEMBER                             # 본인 부서만
│   └── repository/
│       └── DepartmentRepository.java             # Spring Data JPA Repository
│
├── infrastructure/                               # 인프라스트럭처 계층
│   ├── adapter/
│   │   ├── AgentOrgUserAdapter.java              # OrgUserPort 구현체 (User 모듈 연동)
│   │   └── DepartmentInfoAdapter.java            # Department 정보 조회 Adapter
│   └── config/
│       └── OrganizationConfig.java               # 조직 모듈 설정
│
└── exception/                                    # 예외 처리
    ├── DepartmentException.java                   # 부서 예외
    └── DepartmentErrorCode.java                   # 부서 에러 코드 enum
        ├── DEPARTMENT_NOT_FOUND                   # 부서를 찾을 수 없음
        ├── INVALID_PARENT_DEPARTMENT              # 잘못된 상위 부서
        ├── CIRCULAR_REFERENCE                     # 순환 참조
        ├── HAS_CHILD_DEPARTMENTS                  # 하위 부서 존재
        ├── HAS_ACTIVE_MEMBERS                     # 소속 직원 존재
        ├── DEPARTMENT_ALREADY_ACTIVE              # 이미 활성 상태
        └── PARENT_DEPARTMENT_INACTIVE             # 상위 부서 비활성 상태
```

### 🎯 핵심 기능

#### 1. **부서 트리 구조 관리 (Materialized Path)**
- **자기참조 트리**: `parent_dept_id`로 부서 계층 구성
- **조직 경로 (org_path)**: `/dept1/dept2/dept3/` 형식으로 경로 저장
- **깊이 (depth)**: 트리 깊이 자동 계산 (0부터 시작)
- **하위 부서 조회**: `org_path` LIKE 쿼리로 빠른 조회

#### 2. **부서 타입 (DepartmentType)**
- **COMPANY**: 최상위 조직 (회사, 계열사)
- **DIVISION**: 본부급 조직
- **TEAM**: 팀급 조직
- **GROUP**: 그룹/파트
- **CUSTOM**: 사용자 정의 타입 (`custom_type_name`으로 이름 지정)

#### 3. **부서 이동 (Move)**
- **순환 참조 방지**: 자기 자신이나 하위 부서로 이동 불가 검증
- **자동 경로 재계산**: 이동 시 하위 부서들의 `org_path`, `depth` 일괄 업데이트
- **트랜잭션 보장**: 이동 실패 시 전체 롤백
- **권한 검증**: Level 1 RBAC 적용

#### 4. **부서 상태 관리**
- **활성화 (activate)**: 
  - 상위 부서가 활성 상태여야 함
  - 활성화 후 신규 직원 배치 가능
- **비활성화 (deactivate)**:
  - 활성 하위 부서가 없어야 함
  - 소속 직원이 있어도 비활성화 가능 (경고 로그)
  - 비활성화 후 신규 직원 배치 불가

#### 5. **데이터 스코프 (Level 1 RBAC)**
- **역할 기반 접근 제어**: 사용자 역할에 따라 조회 가능한 부서 범위 제한
- **스코프 레벨**:
  - `ADMIN`: 전체 조직 접근
  - `TEAM_LEAD`: 본인 부서 + 하위 부서
  - `MEMBER`: 본인 부서만
- **통합 구현**: `getAccessibleDepartmentIds()` 메서드로 통합

#### 6. **삭제 검증**
- ❌ 하위 부서 존재 시 삭제 불가
- ❌ 소속 활성 사용자 존재 시 삭제 불가
- ✅ 권한 검증 (본인 스코프 내 부서만 삭제 가능)

### 📊 주요 API 엔드포인트

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| `POST` | `/api/org/departments` | 부서 생성 | ADMIN |
| `PATCH` | `/api/org/departments/{deptId}` | 부서 정보 수정 | ADMIN |
| `PUT` | `/api/org/departments/{deptId}/move` | 부서 이동 | ADMIN |
| `DELETE` | `/api/org/departments/{deptId}` | 부서 삭제 | ADMIN |
| `GET` | `/api/org/departments` | 전체 조직도 트리 조회 | ALL |
| `GET` | `/api/org/departments/scoped` | 권한 범위 내 조직도 조회 | ALL |
| `GET` | `/api/org/departments/search?keyword=` | 부서 검색 | ALL |
| `GET` | `/api/org/departments/{deptId}/subtree` | 하위 부서 트리 조회 | ALL |
| `GET` | `/api/org/departments/by-depth?depth=` | 깊이별 부서 조회 | ALL |
| `GET` | `/api/org/departments/by-type?type=` | 타입별 부서 조회 | ALL |
| `GET` | `/api/org/departments/{deptId}/statistics` | 부서 통계 조회 | ALL |
| `GET` | `/api/org/departments/{deptId}/members` | 부서별 사용자 목록 조회 | ALL |
| `POST` | `/api/org/departments/{deptId}/activate` | 부서 활성화 | ADMIN |
| `POST` | `/api/org/departments/{deptId}/deactivate` | 부서 비활성화 | ADMIN |

### 🔐 데이터 스코프 레벨

| 스코프 레벨 | 설명 | 조회 범위 | 적용 역할 예시 |
|------------|------|----------|---------------|
| `ADMIN` | 전체 데이터 접근 | 테넌트 내 모든 부서 | ADMIN |
| `TEAM_LEAD` | 본인 부서 + 하위 | 본인 부서와 하위 부서 전체 | TEAM_LEAD |
| `MEMBER` | 본인 부서만 | 본인이 소속된 부서만 | AGENT, MEMBER |

### 🗄️ Department 테이블 구조

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|---------|------|
| `dept_id` | VARCHAR(36) | PK | 부서 ID (UUID) |
| `tenant_id` | VARCHAR(50) | NOT NULL | 테넌트 ID |
| `parent_id` | VARCHAR(36) | FK (self) | 상위 부서 ID |
| `name` | VARCHAR(100) | NOT NULL | 부서명 |
| `org_path` | VARCHAR(500) | NOT NULL | 조직 경로 (예: /dept1/dept2) |
| `depth` | INTEGER | NOT NULL | 트리 깊이 (0부터 시작) |
| `type` | VARCHAR(32) | | 부서 타입 (HQ, DIVISION, TEAM 등) |
| `created_at` | TIMESTAMP | NOT NULL | 생성 일시 |

### 📝 비즈니스 규칙

1. **부서 생성**
   - `parent_id`가 NULL이면 최상위 부서 (depth=0)
   - `org_path`는 자동 계산: 부모 경로 + 본인 ID
   - 같은 부모 아래 동일 이름 불가

2. **부서 이동**
   - 자기 자신의 하위로 이동 불가 (순환 참조 방지)
   - 이동 시 하위 부서들의 경로 자동 재계산
   - 권한 검증: 이동 대상과 목적지 모두 접근 가능해야 함

3. **부서 삭제**
   - 하위 부서 존재 시 삭제 불가
   - 소속 활성 사용자 존재 시 삭제 불가
   - Soft Delete 아님, 물리 삭제

4. **조직도 조회**
   - 기본: 전체 조직도 반환
   - 권한 범위 조회: 사용자 역할에 따라 필터링된 트리 반환

---

## RBAC 모듈 (역할 기반 접근 제어)

### 📁 패키지 구조 (DDD Layered Architecture)

```
com.nexfron.identitymodulith.rbac/
│
├── presentation/                                  # 표현 계층
│   ├── RbacController.java                        # RBAC 관리 REST API
│   └── RbacExceptionHandler.java                  # RBAC 모듈 전역 예외 핸들러
│
├── application/                                   # 응용 계층
│   ├── RbacManagementService.java                 # RBAC 관리 서비스 인터페이스
│   │   └── (DTO 정의)
│   │       ├── CreateRoleRequest                  # 역할 생성 요청
│   │       ├── UpdateRoleRequest                  # 역할 수정 요청
│   │       ├── CreatePermissionRequest            # 권한 생성 요청
│   │       ├── RoleDto                           # 역할 DTO
│   │       └── PermissionDto                     # 권한 DTO
│   ├── RbacManagementServiceImpl.java             # RBAC 관리 서비스 구현체
│   │   ├── createRole()                           # 역할 생성
│   │   ├── updateRole()                           # 역할 수정
│   │   ├── deleteRole()                           # 역할 삭제
│   │   ├── activateRole()                         # 역할 활성화
│   │   ├── deactivateRole()                       # 역할 비활성화
│   │   ├── createPermission()                     # 권한 생성
│   │   ├── assignPermissionToRole()               # 역할에 권한 할당
│   │   ├── revokePermissionFromRole()             # 역할에서 권한 회수
│   │   ├── assignRoleToAgent()                    # 사용자에게 역할 할당
│   │   └── revokeRoleFromAgent()                  # 사용자에게서 역할 회수
│   ├── RbacQueryService.java                      # RBAC 조회 서비스 인터페이스
│   ├── RbacQueryServiceImpl.java                  # RBAC 조회 서비스 구현체
│   │   ├── permissionsOfRoles()                   # 역할 → 권한 조회
│   │   └── rolesOfAgent()                         # 사용자 → 역할 조회
│   ├── RbacPermissionEvaluator.java               # Spring Security 권한 평가자
│   │   └── hasPermission()                        # 권한 검증
│   └── exception/
│       └── RbacException.java                      # RBAC 모듈 예외
│           └── RbacErrorCode                       # 에러 코드 enum
│
├── domain/                                        # 도메인 계층
│   └── RbacConstants.java                         # RBAC 상수 정의
│       ├── ROLE_NAME_MAX_LENGTH                   # 역할명 최대 길이
│       ├── ROLE_TYPE_MAX_LENGTH                   # 역할 타입 최대 길이
│       └── PERMISSION_CODE_MAX_LENGTH             # 권한 코드 최대 길이
│
├── infrastructure/                                # 인프라스트럭처 계층
│   ├── persistence/
│   │   ├── entity/                                # JPA 엔티티
│   │   │   ├── RoleJpaEntity.java                 # 역할 엔티티
│   │   │   ├── PermissionJpaEntity.java           # 권한 엔티티
│   │   │   ├── RolePermissionJpaEntity.java       # 역할-권한 매핑 엔티티
│   │   │   └── AgentRoleJpaEntity.java            # 사용자-역할 매핑 엔티티
│   │   └── repository/                            # Spring Data JPA Repository
│   │       ├── RoleJpaRepository.java             # 역할 저장소
│   │       ├── PermissionJpaRepository.java       # 권한 저장소
│   │       ├── RolePermissionJpaRepository.java   # 역할-권한 매핑 저장소
│   │       └── AgentRoleJpaRepository.java        # 사용자-역할 매핑 저장소
│   └── adapter/
│       └── AgentPermissionAdapter.java            # PermissionPort 구현체
│
└── (테스트 파일 생략)
```

### 🎯 핵심 기능

#### 1. **역할 (Role) 관리**
- **역할 생성/수정/삭제**: 테넌트별 역할 관리
- **역할 타입**: POSITION (직급), CHANNEL (채널), SKILL (스킬) 등
- **활성화/비활성화**: Soft Delete 방식 (`is_active` 플래그)
- **낙관적 잠금**: `@Version`으로 동시성 제어

#### 2. **권한 (Permission) 관리**
- **권한 코드 표준**: `domain:action` 형식 (예: `user:create`, `org:delete`)
- **테넌트 격리**: 같은 권한 코드도 테넌트별로 별도 관리
- **권한 설명**: 선택적 description 필드

#### 3. **역할-권한 매핑**
- **다대다 관계**: 하나의 역할에 여러 권한 할당 가능
- **매핑 관리**: 권한 할당/회수 API 제공
- **중복 방지**: 유니크 제약조건으로 중복 매핑 방지

#### 4. **사용자-역할 할당**
- **다대다 관계**: 한 사용자에게 여러 역할 할당 가능
- **동적 권한 계산**: 사용자의 모든 역할 → 권한 집합 자동 계산

#### 5. **권한 평가 (Permission Evaluation)**
- **RbacPermissionEvaluator**: Spring Security와 통합
- **`@PreAuthorize("hasPermission('user:delete')")`**: 메서드 레벨 권한 검증

### 📊 주요 API 엔드포인트

#### 역할 관리
| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| `POST` | `/api/rbac/roles` | 역할 생성 | `rbac:manage` |
| `GET` | `/api/rbac/roles` | 역할 목록 조회 | `rbac:view` |
| `GET` | `/api/rbac/roles/{roleName}` | 역할 상세 조회 | `rbac:view` |
| `PUT` | `/api/rbac/roles/{roleName}` | 역할 수정 | `rbac:manage` |
| `DELETE` | `/api/rbac/roles/{roleName}` | 역할 삭제 | `rbac:manage` |
| `PATCH` | `/api/rbac/roles/{roleName}/activate` | 역할 활성화 | `rbac:manage` |
| `PATCH` | `/api/rbac/roles/{roleName}/deactivate` | 역할 비활성화 | `rbac:manage` |

#### 권한 관리
| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| `POST` | `/api/rbac/permissions` | 권한 생성 | `rbac:manage` |
| `GET` | `/api/rbac/permissions` | 권한 목록 조회 | `rbac:view` |

#### 역할-권한 매핑
| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| `POST` | `/api/rbac/roles/{roleName}/permissions` | 역할에 권한 할당 | `rbac:manage` |
| `DELETE` | `/api/rbac/roles/{roleName}/permissions/{permissionCode}` | 역할에서 권한 회수 | `rbac:manage` |

#### 사용자-역할 할당
| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| `POST` | `/api/rbac/agents/{agentId}/roles/{roleName}` | 사용자에게 역할 할당 | `rbac:manage` |
| `DELETE` | `/api/rbac/agents/{agentId}/roles/{roleName}` | 사용자에게서 역할 회수 | `rbac:manage` |
| `GET` | `/api/rbac/agents/{agentId}/roles` | 사용자의 역할 조회 | `rbac:view` |
| `GET` | `/api/rbac/agents/{agentId}/permissions` | 사용자의 권한 조회 | `rbac:view` |

### 🗄️ 테이블 구조

#### 1. **roles** (역할 테이블)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|---------|------|
| `role_id` | VARCHAR(36) | PK | 역할 ID (UUID) |
| `tenant_id` | VARCHAR(50) | NOT NULL | 테넌트 ID |
| `name` | VARCHAR(64) | NOT NULL | 역할명 (ADMIN, TEAM_LEADER 등) |
| `type` | VARCHAR(32) | NOT NULL | 역할 타입 (POSITION, CHANNEL, SKILL) |
| `description` | VARCHAR(255) | | 역할 설명 |
| `is_active` | BOOLEAN | DEFAULT true | 활성화 상태 |
| `version` | BIGINT | DEFAULT 0 | 낙관적 잠금 버전 |
| `created_at` | TIMESTAMP | NOT NULL | 생성 일시 |
| `updated_at` | TIMESTAMP | | 수정 일시 |

**유니크 제약**: `(tenant_id, name)`

#### 2. **permissions** (권한 테이블)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|---------|------|
| `permission_id` | VARCHAR(36) | PK | 권한 ID (UUID) |
| `tenant_id` | VARCHAR(50) | NOT NULL | 테넌트 ID |
| `code` | VARCHAR(128) | NOT NULL | 권한 코드 (domain:action) |
| `created_at` | TIMESTAMP | NOT NULL | 생성 일시 |

**유니크 제약**: `(tenant_id, code)`

#### 3. **role_permissions** (역할-권한 매핑)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|---------|------|
| `id` | BIGSERIAL | PK | 매핑 ID |
| `role_id` | VARCHAR(36) | FK, NOT NULL | 역할 ID |
| `permission_id` | VARCHAR(36) | FK, NOT NULL | 권한 ID |
| `assigned_at` | TIMESTAMP | NOT NULL | 할당 일시 |

**유니크 제약**: `(role_id, permission_id)`

#### 4. **agent_roles** (사용자-역할 매핑)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|---------|------|
| `id` | BIGSERIAL | PK | 매핑 ID |
| `agent_id` | VARCHAR(36) | FK, NOT NULL | 사용자 ID |
| `role_id` | VARCHAR(36) | FK, NOT NULL | 역할 ID |
| `assigned_at` | TIMESTAMP | NOT NULL | 할당 일시 |

**유니크 제약**: `(agent_id, role_id)`

#### 5. **audit_logs** (감사 로그)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|---------|------|
| `audit_id` | VARCHAR(36) | PK | 감사 로그 ID (UUID) |
| `tenant_id` | VARCHAR(50) | NOT NULL | 테넌트 ID |
| `action` | VARCHAR(32) | NOT NULL | 작업 유형 (CREATE, UPDATE, DELETE, ASSIGN, REVOKE) |
| `resource_type` | VARCHAR(64) | NOT NULL | 대상 리소스 타입 (ROLE, PERMISSION, AGENT_ROLE) |
| `resource_id` | VARCHAR(255) | NOT NULL | 대상 리소스 ID |
| `operator_id` | VARCHAR(255) | NOT NULL | 작업자 ID |
| `changes` | TEXT | | 변경 내용 (JSON) |
| `timestamp` | TIMESTAMP | NOT NULL | 작업 일시 |
| `remarks` | TEXT | | 추가 정보 |
| `ip_address` | VARCHAR(45) | | 클라이언트 IP |

**인덱스**: `tenant_id`, `resource_type`, `operator_id`, `timestamp DESC`

### 📝 비즈니스 규칙

1. **역할 생성**
   - 역할명은 테넌트 내에서 유일해야 함
   - 역할 타입은 POSITION, CHANNEL 중 하나
   - 기본 상태는 `is_active = true`

2. **역할 삭제**
   - 해당 역할을 가진 사용자가 있으면 삭제 불가
   - 물리 삭제 대신 `is_active = false`로 비활성화 권장

3. **권한 생성**
   - 권한 코드는 `domain:action` 형식 준수 (정규식 검증)
   - 테넌트 내에서 권한 코드 유일해야 함

4. **역할-권한 매핑**
   - 이미 할당된 권한을 재할당 시도 시 예외 발생
   - 역할이나 권한 삭제 시 매핑도 자동 삭제 (CASCADE)

5. **사용자-역할 할당**
   - 이미 할당된 역할을 재할당 시도 시 예외 발생
   - 사용자나 역할 삭제 시 매핑도 자동 삭제 (CASCADE)

6. **권한 평가**
   - 사용자의 모든 역할 → 모든 권한 합집합 계산
   - 하나라도 권한이 있으면 접근 허용
   - 비활성화된 역할은 권한 계산에서 제외
- **권한 삭제**: 전체 RBAC 캐시 무효화

---

## Common 모듈 (공통 컴포넌트)

### 📁 패키지 구조

```
com.nexfron.identitymodulith.common/
│
├── security/                                      # 보안 관련 공통 컴포넌트
│   ├── TenantContextHolder.java                   # 테넌트 컨텍스트 관리 (ThreadLocal)
│   │   ├── setCurrentTenantId()                   # 현재 스레드의 테넌트 ID 설정
│   │   ├── getCurrentTenantId()                   # 현재 스레드의 테넌트 ID 조회
│   │   └── clear()                                # 테넌트 컨텍스트 클리어
│   ├── AuthPrincipal.java                          # 인증 주체 (Principal)
│   │   ├── userId                                 # 사용자 ID
│   │   ├── tenantId                               # 테넌트 ID
│   │   ├── username                               # 사용자명
│   │   └── authorities                            # 권한 목록
│   └── UnauthorizedException.java                  # 인증/인가 예외
│
└── cache/                                         # 캐시 관련 공통 컴포넌트
```

### 🎯 핵심 컴포넌트

#### 1. **TenantContextHolder** (테넌트 컨텍스트 관리)

**역할**:
- **멀티테넌시 지원**: 각 HTTP 요청마다 테넌트 ID를 ThreadLocal에 저장
- **자동 추출**: Spring Security의 `Authentication` 객체에서 테넌트 ID 추출
- **스레드 안전**: ThreadLocal 사용으로 동시 요청 간 격리 보장

**사용 예시**:
```java
// Controller나 Filter에서 설정
String tenantId = extractTenantIdFromRequest(request);
TenantContextHolder.setCurrentTenantId(tenantId);

// Service에서 조회
String tenantId = TenantContextHolder.getCurrentTenantId();
List<Role> roles = roleRepository.findByTenantId(tenantId);

// 요청 종료 시 클리어 (Filter에서 자동 처리)
TenantContextHolder.clear();
```

**주의사항**:
- 비동기 작업 시 자식 스레드로 컨텍스트 전파 필요
- 반드시 요청 종료 시 `clear()` 호출 (메모리 누수 방지)

#### 2. **AuthPrincipal** (인증 주체)

**역할**:
- **Spring Security 통합**: `Authentication.getPrincipal()`로 반환되는 객체
- **사용자 정보 전달**: 컨트롤러 → 서비스 → 도메인 레이어까지 사용자 정보 전파
- **권한 정보 포함**: 사용자의 권한 목록 포함

**필드**:
- `userId`: 사용자 ID (UUID)
- `tenantId`: 테넌트 ID
- `username`: 로그인 ID 또는 사용자명
- `authorities`: 권한 목록 (Spring Security의 `GrantedAuthority`)

**사용 예시**:
```java
@GetMapping("/profile")
public ResponseEntity<ProfileDto> getProfile(
    @AuthenticationPrincipal AuthPrincipal principal
) {
    String userId = principal.getUserId();
    String tenantId = principal.getTenantId();
    // ...
}
```

#### 3. **UnauthorizedException** (인증/인가 예외)

**역할**:
- **인증 실패**: 로그인되지 않은 사용자 접근 시 발생
- **권한 부족**: 필요한 권한이 없는 사용자 접근 시 발생
- **테넌트 불일치**: 다른 테넌트의 리소스 접근 시 발생

**HTTP 상태 코드**: 401 Unauthorized 또는 403 Forbidden

**사용 예시**:
```java
if (!hasPermission(userId, "user:delete")) {
    throw new UnauthorizedException("권한이 없습니다.");
}
```

### 📝 멀티테넌시 아키텍처

#### 테넌트 격리 전략

1. **데이터베이스 레벨**
   - 모든 테이블에 `tenant_id` 컬럼 포함
   - 모든 쿼리에 `WHERE tenant_id = :tenantId` 조건 자동 추가
   - 유니크 제약조건에 `tenant_id` 포함

2. **애플리케이션 레벨**
   - `TenantContextHolder`로 현재 테넌트 ID 관리
   - Repository 메서드에 테넌트 ID 파라미터 필수
   - 테넌트 불일치 시 예외 발생


#### 테넌트 추출 우선순위

1. HTTP 헤더: `X-Tenant-Id`
2. JWT 토큰: `claims.tenantId`
3. 사용자명: `tenantId:userId` 형식
4. 기본값: `default-tenant`

### 🔐 보안 고려사항

1. **테넌트 격리 보장**
   - 모든 Repository 메서드에 `tenantId` 검증 필수
   - 크로스 테넌트 접근 차단

2. **ThreadLocal 관리**
   - 요청 종료 시 반드시 `TenantContextHolder.clear()` 호출
   - 비동기 작업 시 컨텍스트 전파 처리

3. **권한 검증**
   - `@PreAuthorize`로 메서드 레벨 권한 검증
   - `RbacPermissionEvaluator`로 세밀한 권한 제어

---

## 📚 참고 문서

### 핵심 문서
- **[CHANGELOG.md](./CHANGELOG.md)**: 버전별 변경 이력 및 주요 개선 사항
- **[DB_COMPREHENSIVE_GUIDE.md](./DB_COMPREHENSIVE_GUIDE.md)**: 데이터베이스 스키마, 테이블 구조, 컬럼 설명, 표준 데이터 형식

### API 문서
- **[API_SPECIFICATION_V2.md](./Docs/API_SPECIFICATION_V2.md)**: 전체 API 명세서 (User, Organization, RBAC)
- **[ORGANIZATION_API_TEST_GUIDE.md](./Docs/ORGANIZATION_API_TEST_GUIDE.md)**: Organization API 단계별 테스트 가이드 (Swagger UI 활용)
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html` (실시간 API 문서 및 테스트)

### 시나리오 문서
- **[AGENT_SCENARIOS.md](./Docs/AGENT_SCENARIOS.md)**: 상담사 관리 시나리오 (채용, 역할 변경, 퇴사 등)
- **[DEPARTMENT_SCENARIOS.md](./Docs/DEPARTMENT_SCENARIOS.md)**: 조직 관리 시나리오 (조직 구조 변경, 권한 관리 등)
- **[RBAC_SCENARIOS.md](./Docs/RBAC_SCENARIOS.md)**: RBAC 관리 시나리오 (역할/권한 관리 등)
- **[EVENT_STORMING.md](./Docs/EVENT_STORMING.md)**: 이벤트 스토밍 결과 (도메인 이벤트, 커맨드, 애그리게잇)

### 아키텍처 문서
- **[PROJECT_COMPLETION_REPORT.md](./PROJECT_COMPLETION_REPORT.md)**: 프로젝트 완료 보고서 (구현 내용 총정리)

---

## 🎯 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 테스트 커버리지 리포트 생성
./gradlew jacocoTestReport

# 특정 모듈 테스트만 실행
./gradlew test --tests "com.nexfron.identitymodulith.user.*"
./gradlew test --tests "com.nexfron.identitymodulith.organization.*"
./gradlew test --tests "com.nexfron.identitymodulith.rbac.*"
```

### 테스트 구조
- **User 모듈**: 18개 테스트 (상담사 생성, 수정, 삭제, 역할 관리 등)
- **Organization 모듈**: 15개 테스트 (부서 생성, 이동, 삭제, 조직도 조회 등)
- **RBAC 모듈**: 20개 테스트 (역할/권한 관리, 할당/회수 등)

---

## 🚀 배포

### 프로덕션 빌드

```bash
# JAR 파일 생성
./gradlew bootJar

# 생성된 파일 위치
./build/libs/identity-modulith-0.0.1-SNAPSHOT.jar
```

### 환경 변수 설정

```bash
# 데이터베이스
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/your_database
export SPRING_DATASOURCE_USERNAME=your_username
export SPRING_DATASOURCE_PASSWORD=your_password

# 애플리케이션
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=prod
```

### Docker 실행 (선택사항)

```bash
# Dockerfile 생성 후
docker build -t identity-modulith .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/nexfron \
  -e SPRING_DATASOURCE_USERNAME=nexfron \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  identity-modulith
```

---

## 📞 문의 및 지원

- **프로젝트**: Identity Modulith v3.0.0
- **마지막 업데이트**: 2026-02-05
- **라이선스**: Proprietary

---

## ✅ 체크리스트

### 초기 설정
- [ ] PostgreSQL 18+ 설치 및 데이터베이스 생성
- [ ] Java 21 설치
- [ ] application.yml에 데이터베이스 정보 설정
- [ ] `./gradlew bootRun`으로 애플리케이션 실행
- [ ] Swagger UI 접속 확인 (`http://localhost:8080/swagger-ui/index.html`)

### 기능 검증
- [ ] Organization API: 부서 생성, 조회, 이동, 삭제 테스트
- [ ] User API: 상담사 생성, 역할 할당, 비밀번호 초기화 테스트
- [ ] RBAC API: 역할/권한 생성, 할당/회수 테스트
- [ ] 데이터 스코프: 권한 범위 내 조직도 조회 테스트
- [ ] 멀티테넌시: 다른 테넌트 데이터 격리 확인

### 프로덕션 준비
- [ ] 모든 테스트 통과 확인 (`./gradlew test`)
- [ ] 프로덕션 환경 변수 설정
- [ ] 데이터베이스 백업 전략 수립
- [ ] 로그 모니터링 설정
- [ ] 성능 테스트 (부하 테스트)

---

