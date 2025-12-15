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

이 프로젝트는 **Spring Modulith + Clean Architecture** 패턴을 따릅니다.

### 모듈 구조

```
com.nexfron.identitymodulith/
├── user/                    # User 모듈 (상담사 관리)
└── organization/            # Organization 모듈 (조직/부서 관리)
```

### Clean Architecture 레이어 (각 모듈 내부)

```
{module}/
├── domain/           # 엔티티, 값 객체, 도메인 서비스 (비즈니스 규칙)
├── application/      # 유스케이스, 입출력 포트 (애플리케이션 로직)
└── adapter/
    ├── in/
    │   └── web/      # REST Controller (Driving Adapter)
    └── out/
        └── persistence/  # JPA Repository 구현 (Driven Adapter)
```

### 의존성 규칙

- **Domain**: 어떤 레이어에도 의존하지 않음 (순수 Java)
- **Application**: Domain에만 의존, 외부 기술에 의존하지 않음
- **Adapter**: Application과 Domain에 의존, 프레임워크/라이브러리 사용

---

## User 모듈 (상담사 관리)

### 패키지 구조

```
com.nexfron.identitymodulith.user/
├── domain/
│   ├── Agent.java              # 상담사 엔티티
│   ├── AgentStatus.java        # 상태 enum (ACTIVE, RETIRED)
│   ├── Role.java               # 역할 값 객체 (직급, 채널)
│   └── Skill.java              # 스킬 값 객체
├── application/
│   ├── port/
│   │   ├── in/                 # Input Ports (UseCase 인터페이스)
│   │   │   ├── CreateAgentUseCase.java
│   │   │   ├── ResetPasswordUseCase.java
│   │   │   ├── UpdateAgentUseCase.java
│   │   │   ├── RetireAgentUseCase.java
│   │   │   ├── GetAgentUseCase.java
│   │   │   ├── ManageRoleSkillUseCase.java
│   │   │   └── CheckUsernameUseCase.java
│   │   └── out/                # Output Ports
│   │       ├── AgentRepository.java
│   │       ├── PasswordEncoder.java
│   │       └── PasswordGenerator.java
│   └── service/
│       └── AgentService.java   # UseCase 구현체
└── adapter/
    ├── in/
    │   └── web/
    │       ├── AgentController.java
    │       └── dto/
    │           ├── CreateAgentRequest.java
    │           ├── CreateAgentResponse.java
    │           ├── UpdateAgentRequest.java
    │           ├── TransferOrganizationRequest.java
    │           ├── ResetPasswordResponse.java
    │           ├── AgentResponse.java
    │           ├── AssignRolesRequest.java
    │           └── AssignSkillsRequest.java
    └── out/
        └── persistence/
            ├── AgentJpaEntity.java
            ├── AgentRoleJpaEntity.java
            ├── AgentSkillJpaEntity.java
            ├── AgentJpaRepository.java
            ├── AgentMapper.java
            ├── AgentRepositoryAdapter.java
            ├── PasswordEncoderAdapter.java
            └── PasswordGeneratorAdapter.java
```

### 도메인 모델

| 클래스 | 설명 |
|--------|------|
| `Agent` | 콜센터 상담사 엔티티 |
| `AgentStatus` | 상담사 상태 (`ACTIVE`, `RETIRED`) |
| `Role` | 역할 값 객체 - 직급(POSITION), 채널(CHANNEL) |
| `Skill` | 스킬 값 객체 - 단순 보유 여부만 관리 |

### API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/agents` | 상담사 생성 (임시 비밀번호 발급) |
| `GET` | `/api/v1/agents` | 상담사 목록 조회 |
| `GET` | `/api/v1/agents/{agentId}` | 상담사 상세 조회 |
| `GET` | `/api/v1/agents/check-username?username=` | 아이디 중복 체크 |
| `PATCH` | `/api/v1/agents/{agentId}` | 상담사 정보 수정 |
| `PATCH` | `/api/v1/agents/{agentId}/organization` | 조직 이동 |
| `POST` | `/api/v1/agents/{agentId}/reset-password` | 비밀번호 초기화 (임시 비밀번호 발급) |
| `DELETE` | `/api/v1/agents/{agentId}` | 상담사 퇴사 처리 (Soft Delete) |
| `PUT` | `/api/v1/agents/{agentId}/roles` | 역할 지정 |
| `PUT` | `/api/v1/agents/{agentId}/skills` | 스킬 지정 |

### 비즈니스 규칙

#### 1. 상담사 생성 (Onboarding)

- 아이디 중복 체크 필수
- 시스템이 난수로 임시 비밀번호 생성
- 생성 API 응답에 `tempPassword` 포함 (일회성, 재조회 불가)
- 초기 설정: `passwordMustChange: true`, `status: ACTIVE`

#### 2. 역할 및 스킬 지정

- **역할(Role)**: 직급, 채널 복수 선택
- **스킬(Skill)**: 숙련도 없이 단순 보유 여부만 체크

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