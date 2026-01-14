# Identity Modulith

Spring Boot와 Spring Modulith 기반의 모듈식 모놀리스 아키텍처를 적용한 인증/ID 관리 애플리케이션입니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.8
- Spring Modulith 1.4.2
- Spring Data JPA
- Lombok
- Gradle

## 빌드 및 실행

```bash
# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 애플리케이션 실행
./gradlew bootRun
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

### 패키지 구조

```
com.nexfron.identitymodulith.organization/
├── domain/
│   └── model/
│       ├── Department.java         # 부서 엔티티 (트리 구조)
│       └── OrgRoleLevel.java       # 역할 레벨 enum (MEMBER, TEAM_LEAD, ADMIN)
├── application/
│   ├── port/
│   │   ├── OrgUserPort.java        # User 모듈 연동 포트
│   │   ├── OrgUserView.java        # 유저 조직 정보 DTO
│   │   └── DummyOrgUserAdapter.java # 임시 구현체
│   └── service/
│       ├── DepartmentService.java  # 부서 CRUD 서비스
│       └── OrgScopeService.java    # Level 2 RBAC 스코프 계산 서비스
├── api/
│   ├── DepartmentController.java   # REST Controller
│   └── dto/
│       └── DepartmentDto.java      # 요청/응답 DTO
├── infrastructure/
│   └── repository/
│       └── DepartmentRepository.java # JPA Repository
└── common/
    └── exception/
        ├── BusinessException.java
        └── EntityNotFoundException.java
```

### 도메인 모델

| 클래스 | 설명 |
|--------|------|
| `Department` | 부서 엔티티 - 트리 구조 (`parent`, `orgPath`, `depth`) |
| `OrgRoleLevel` | 역할 레벨 - `MEMBER`, `TEAM_LEAD`, `ADMIN` |

### Department 테이블

| 타입 | 컬럼명 | 제약조건 | 비고 |
|------|--------|----------|------|
| Long | dept_id | PK, Auto | 부서 ID |
| String | tenant_id | NOT NULL | 테넌트 구분 |
| Long | parent_id | FK | 상위 부서 (Self Reference) |
| String | name | NOT NULL | 부서명 |
| String | type | | 부서 타입 (팀, 본부 등) |
| String | org_path | NOT NULL | 트리 경로 (예: /1/5/10) |
| Integer | depth | DEFAULT 0 | 트리 깊이 |
| DateTime | created_at | | 생성 일시 |

### API 엔드포인트

| Method | Endpoint | 헤더 | 설명 |
|--------|----------|------|------|
| `POST` | `/api/v1/departments` | X-Tenant-Id | 부서 생성 |
| `PUT` | `/api/v1/departments/{deptId}/move` | X-Tenant-Id, X-User-Id | 부서 이동 |
| `DELETE` | `/api/v1/departments/{deptId}` | X-Tenant-Id, X-User-Id | 부서 삭제 |
| `GET` | `/api/v1/departments/tree` | X-Tenant-Id | 전체 조직도 트리 조회 |
| `GET` | `/api/v1/departments/my-scope` | X-Tenant-Id, X-User-Id | 내 권한 범위 조직도 조회 |

### Level 2 RBAC (Data Scope)

역할 레벨에 따라 조회 가능한 조직 범위가 결정됩니다:

| 역할 레벨 | 조회 범위 |
|-----------|----------|
| `MEMBER` | 본인 부서만 |
| `TEAM_LEAD` | 본인 부서 + 하위 부서 |
| `ADMIN` | 테넌트 전체 조직 |

### 비즈니스 규칙

#### 1. 부서 생성

- `parentId`가 없으면 최상위 부서로 생성
- `orgPath`는 자동 계산 (예: `/1/5/10`)
- `depth`는 트리 깊이에 따라 자동 설정

#### 2. 부서 이동

- 순환 참조 방지 (자기 하위로 이동 불가)
- 이동 시 하위 부서의 `orgPath`도 자동 재계산
- 이동 권한은 `OrgScopeService`로 검증

#### 3. 부서 삭제

- 하위 부서가 존재하면 삭제 불가
- 소속 구성원이 있으면 삭제 불가
- 삭제 권한은 `OrgScopeService`로 검증

#### 4. 조직도 조회

- `/tree`: 테넌트 전체 조직도
- `/my-scope`: 로그인 유저의 권한 범위 내 조직도만 반환

---

## 📚 데이터베이스 스키마

RBAC 모듈과 Organization 모듈의 테이블 정의, 컬럼 설명, 표준 데이터 형식은 다음 문서를 참고하세요:

👉 **[DB 스키마 표준 가이드](./DB_SCHEMA_STANDARD.md)**

주요 내용:
- **RBAC 테이블**: permissions, roles, role_permissions, agent_roles
- **Organization 테이블**: departments
- **데이터 형식**: UUID, tenant_id, org_path, 타임스탬프 등 표준
- **정합성 규칙**: 제약조건, 유니크 키, 외래키
- **쿼리 예시**: 권한 조회, 조직도 조회 등 자주 사용되는 SQL

---

## 📚 RBAC 표준 데이터

RBAC 모듈의 역할(Role), 권한(Permission), 그리고 역할-권한 매핑의 표준을 정의한 문서입니다:

👉 **[RBAC 표준 데이터 정의 가이드](./RBAC_DATA_STANDARD.md)**

주요 내용:
- **역할 분류**: POSITION(직급), CHANNEL(채널), SKILL(역량)
- **권한 코드 표준**: 도메인:액션 형식 (예: `user:create`, `org:manage`)
- **표준 역할**: ADMIN, MANAGER, TEAM_LEAD, MEMBER, PHONE_AGENT, CHAT_AGENT, SUPERVISOR 등
- **역할별 권한 매핑**: 각 역할이 보유해야 할 권한 정의
- **SQL 예시**: 표준 권한/역할 데이터 삽입 스크립트

**마이그레이션 스크립트**:
- `V1_0_1__RBAC_standard_data_init.sql`: 기존 데이터 삭제 후 표준 데이터 삽입

---

