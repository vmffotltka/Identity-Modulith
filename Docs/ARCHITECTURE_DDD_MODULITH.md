# 도메인 중심 구조 설계 (DDD + 모듈러 모놀리식)

> **최종 업데이트**: 2026-03-11  
> **상태**: ✅ 구현 완료

---

## 📋 목차
1. [배경 및 문제 인식](#1-배경-및-문제-인식)
2. [설계 원칙](#2-설계-원칙)
3. [전체 패키지 구조](#3-전체-패키지-구조)
4. [계층별 역할](#4-계층별-역할)
5. [모듈 간 통신 규칙](#5-모듈-간-통신-규칙)
6. [도메인 모델 — Agent](#6-도메인-모델--agent)
7. [향후 MSA 전환 가능성](#7-향후-msa-전환-가능성)

---

## 1. 배경 및 문제 인식

### 문제
초기 설계에서 **인증/권한 로직이 서비스 계층에 혼재**되어 있어:
- 비즈니스 로직 변경 시 영향 범위를 파악하기 어려움
- 인증 로직 수정이 비즈니스 로직에 영향을 줌
- 테스트 작성 시 경계가 불명확

### 판단
비즈니스 로직 보호와 확장성을 위해 **도메인 경계를 명확히 분리**할 필요성 확인.  
향후 MSA 전환 가능성을 고려한 낮은 결합도 설계 채택.

---

## 2. 설계 원칙

| 원칙 | 내용 |
|------|------|
| **도메인 주도 설계 (DDD)** | 비즈니스 규칙은 도메인 모델 내부에 캡슐화 |
| **모듈러 모놀리식** | 단일 배포 단위이지만 모듈 경계가 명확 |
| **포트 & 어댑터 패턴** | 외부 시스템(DB, 다른 모듈)과의 의존성을 인터페이스로 추상화 |
| **단방향 의존성** | 하위 계층은 상위 계층을 알지 못함 (Domain → Application → Infrastructure) |
| **공개 API 인터페이스** | 모듈 간 통신은 루트 패키지의 `*ModuleApi` 인터페이스만 사용 |

---

## 3. 전체 패키지 구조

```
com.identitymodulith
├── ApiErrorResponse.java           ← 공통 에러 응답 포맷 (모든 모듈 공유)
├── IdentityModulithApplication.java
│
├── common/                         ← 횡단 관심사 (Cross-cutting concerns)
│   ├── config/                     ← 공통 설정
│   ├── domain/
│   │   └── DataScopeLevel.java     ← 공통 열거형 (ADMIN / TEAM_LEAD / MEMBER)
│   ├── exception/
│   │   └── CommonExceptionHandler.java  ← 공통 전역 예외 처리
│   └── security/
│       ├── Saml2SecurityConfig.java
│       ├── CustomPermissionEvaluator.java
│       ├── SamlTestController.java  (@Profile("dev"))
│       ├── context/                ← 요청 컨텍스트 (TenantContext, JwtUserContext)
│       ├── filter/                 ← Security 필터
│       ├── handler/                ← SAML 인증 성공/실패 핸들러
│       └── principal/              ← 인증 주체 구현체
│
├── user/                           ← User 모듈 (상담사 관리)
│   ├── UserModuleApi.java          ← 모듈 공개 API (다른 모듈에서 의존)
│   ├── AgentExternalInfo.java      ← 모듈 외부 공개 DTO
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Agent.java          ← 핵심 도메인 모델 (비즈니스 규칙 내장)
│   │   │   └── AgentStatus.java
│   │   ├── exception/
│   │   │   ├── BusinessException.java
│   │   │   └── ErrorCode.java
│   │   ├── event/                  ← 도메인 이벤트
│   │   └── service/                ← 도메인 서비스 (순수 비즈니스 로직)
│   ├── application/
│   │   ├── AgentService.java       ← Application Service (유스케이스 구현)
│   │   ├── *UseCase.java           ← 유스케이스 인터페이스 (11개)
│   │   ├── port/
│   │   │   ├── OrganizationPort.java  ← Organization 모듈 추상화
│   │   │   └── RbacPort.java          ← RBAC 모듈 추상화
│   │   └── eventhandler/
│   ├── infrastructure/
│   │   ├── adapter/                ← Port 구현체 (실제 모듈 호출)
│   │   ├── batch/                  ← 배치 스케줄러
│   │   ├── keycloak/               ← Keycloak Admin API 클라이언트
│   │   ├── persistence/            ← JPA 구현체
│   │   └── retry/                  ← DB 재시도 유틸리티
│   └── presentation/
│       ├── AgentController.java
│       ├── MeController.java
│       ├── DevController.java      (@Profile("dev"))
│       ├── GlobalExceptionHandler.java  ← User 모듈 예외 처리
│       └── dto/
│           ├── request/
│           └── response/
│
├── rbac/                           ← RBAC 모듈 (역할 기반 접근 제어)
│   ├── RbacModuleApi.java          ← 모듈 공개 API
│   ├── domain/                     ← 도메인 (RoleType 등)
│   ├── application/
│   │   ├── service/                ← RbacManagementServiceImpl, RbacQueryServiceImpl
│   │   ├── exception/
│   │   │   └── RbacException.java
│   │   └── port/
│   ├── infrastructure/
│   │   └── persistence/
│   └── presentation/
│       ├── RbacController.java
│       ├── RbacExceptionHandler.java  ← RBAC 모듈 예외 처리
│       └── dto/
│           ├── request/
│           └── response/
│
└── organization/                   ← Organization 모듈 (부서 관리)
    ├── OrganizationModuleApi.java  ← 모듈 공개 API
    ├── domain/
    ├── application/
    │   ├── service/
    │   ├── exception/
    │   └── port/
    ├── infrastructure/
    └── presentation/
        ├── DepartmentController.java
        ├── OrganizationExceptionHandler.java  ← Organization 모듈 예외 처리
        └── dto/
            ├── request/
            └── response/
```

---

## 4. 계층별 역할

### Domain 계층
```
역할: 비즈니스 규칙의 단일 진실 공급원 (Single Source of Truth)
의존: 없음 (순수 Java)

포함:
- 도메인 모델 (Agent, AgentStatus)
- 비즈니스 예외 (BusinessException, ErrorCode)
- 도메인 이벤트 (AgentRetiredEvent 등)
- 도메인 서비스 (PasswordEncoder, PasswordGenerator)
```

**예시 — 도메인 모델에 캡슐화된 비즈니스 규칙:**
```java
// Agent.java — 상태 전이 검증이 도메인 내부에 있음
public void suspend(String suspendedByUserId) {
    if (this.status != AgentStatus.ACTIVE) {
        throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                "ACTIVE 상태만 정지할 수 있습니다.");
    }
    this.status = AgentStatus.SUSPENDED;
    this.suspendedAt = LocalDateTime.now();
}
```

### Application 계층
```
역할: 유스케이스 조율 (Use Case Orchestration)
의존: Domain 계층, Port 인터페이스

포함:
- AgentService (11개 유스케이스 구현)
- UseCase 인터페이스 (CreateAgentUseCase, RetireAgentUseCase 등)
- Port 인터페이스 (OrganizationPort, RbacPort)
```

**유스케이스 인터페이스 목록:**

| 인터페이스 | 설명 |
|-----------|------|
| `CreateAgentUseCase` | 상담사 생성 |
| `ResetPasswordUseCase` | 비밀번호 초기화 (관리자) |
| `ChangePasswordUseCase` | 비밀번호 변경 (본인) |
| `UpdateAgentUseCase` | 정보 수정 |
| `RetireAgentUseCase` | 퇴사 처리 |
| `SuspendAgentUseCase` | 정지 처리 |
| `ActivateAgentUseCase` | 정지 해제 |
| `TransferAgentUseCase` | 부서 이동 |
| `GetAgentUseCase` | 상담사 조회 |
| `GetAgentStatisticsUseCase` | 통계 조회 |
| `ManageRoleUseCase` | 역할 관리 |

### Infrastructure 계층
```
역할: 기술적 구현 (DB, 외부 API, 배치)
의존: Application 계층의 Port 인터페이스를 구현

포함:
- AgentRepositoryImpl (Port 구현체)
- RbacAdapter (RbacPort 구현 → RbacModuleApi 호출)
- OrganizationAdapter (OrganizationPort 구현 → OrganizationModuleApi 호출)
- KeycloakAdminClient (Keycloak REST API 호출)
- AgentRetirementBatchScheduler (배치)
- DatabaseRetrySupplier (DB 재시도)
```

### Presentation 계층
```
역할: HTTP 인터페이스
의존: Application 계층 (UseCase 인터페이스만)

포함:
- AgentController, MeController
- DTO (Request/Response 분리)
- ExceptionHandler (모듈 전용)
```

---

## 5. 모듈 간 통신 규칙

### 핵심 원칙
> 모듈 내부 구현 클래스를 직접 참조하지 않는다.  
> 루트 패키지의 `*ModuleApi` 인터페이스를 통해서만 통신한다.

```
┌─────────────────────────────────────────────────────────┐
│                   User 모듈                             │
│                                                         │
│  AgentService                                           │
│      │                                                  │
│      ├── OrganizationPort ──→ OrganizationModuleApi     │
│      │   (인터페이스)             (공개 API)             │
│      │                               │                  │
│      └── RbacPort ──→ RbacModuleApi  │                  │
│          (인터페이스)    (공개 API)   │                  │
└──────────────────────────────────────┼──────────────────┘
                                       ↓
                          ┌────────────────────────┐
                          │  Organization 모듈     │
                          │  DepartmentServiceImpl │
                          └────────────────────────┘
```

**Port 인터페이스 예시** (`OrganizationPort.java`):
```java
// User 모듈이 Organization 모듈에 의존하지 않고 이 인터페이스만 의존
public interface OrganizationPort {
    Optional<DepartmentInfo> getDepartmentInfo(String tenantId, String deptId);
}
```

**Adapter 구현** (`OrganizationAdapter.java`):
```java
// Infrastructure 계층에서 실제로 OrganizationModuleApi를 호출
@Component
public class OrganizationAdapter implements OrganizationPort {
    private final OrganizationModuleApi organizationModuleApi;
    // ...
}
```

---

## 6. 도메인 모델 — Agent

`Agent` 도메인 모델은 단순 데이터 구조가 아니라 **비즈니스 규칙을 내장한 풍부한 도메인 모델(Rich Domain Model)**입니다.

```
Agent 상태 전이:

  ┌──────────┐   suspend()   ┌───────────┐
  │  ACTIVE  │──────────────▶│ SUSPENDED │
  │          │◀──────────────│           │
  └──────────┘  activate()   └───────────┘
       │                           │
       │ retire()                  │ retire()
       ▼                           ▼
  ┌──────────┐             ┌──────────┐
  │  RETIRED │             │  RETIRED │
  │ (복구 불가) │           │ (복구 불가) │
  └──────────┘             └──────────┘
```

**내장된 비즈니스 규칙:**
- `suspend()`: ACTIVE 상태만 정지 가능
- `activate()`: SUSPENDED 상태만 활성화 가능
- `retire()`: 이미 RETIRED이면 예외 발생
- `anonymize()`: IMMEDIATE 정책 시 개인정보 익명화
- `changePassword()`: 변경 후 `passwordMustChange = false` 자동 설정

---

## 7. 향후 MSA 전환 가능성

현재 모듈러 모놀리식 구조는 다음 이유로 **MSA 전환에 유리**합니다:

| 현재 (모듈러 모놀리식) | MSA 전환 시 |
|----------------------|-----------|
| `OrganizationPort` 인터페이스 | HTTP Client로 교체 |
| `RbacPort` 인터페이스 | HTTP Client로 교체 |
| `*ModuleApi` 인터페이스 | gRPC 또는 REST Client로 교체 |
| 도메인 모델 내 비즈니스 규칙 | 그대로 유지 |
| DB 트랜잭션 경계 | 분산 트랜잭션(Saga 패턴)으로 전환 필요 |

**전환 난이도가 낮은 이유:**  
모듈 내부는 서로를 직접 참조하지 않으므로, `Port → HTTP Client` 교체만으로 독립 배포가 가능한 구조입니다.

