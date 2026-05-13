# 도메인 중심 구조 설계 (DDD + 모듈러 모놀리식)

> **최종 업데이트**: 2026-03-11  
> **상태**: ✅ 구현 완료

---

## 📋 목차
1. [배경 및 문제 인식](#1-배경-및-문제-인식)
2. [설계 원칙](#2-설계-원칙)
3. [아키텍처 대안 비교 (모놀리식 vs 모듈러 모놀리식 vs MSA)](#3-아키텍처-대안-비교-모놀리식-vs-모듈러-모놀리식-vs-msa)
4. [전체 패키지 구조](#4-전체-패키지-구조)
5. [계층별 역할](#5-계층별-역할)
6. [모듈 간 통신 규칙](#6-모듈-간-통신-규칙)
7. [도메인 모델 — Agent](#7-도메인-모델--agent)
8. [모듈러 모놀리식 주장 타당성 검토](#8-모듈러-모놀리식-주장-타당성-검토)
9. [향후 MSA 전환 가능성](#9-향후-msa-전환-가능성)

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

## 3. 아키텍처 대안 비교 (모놀리식 vs 모듈러 모놀리식 vs MSA)

아래 비교는 "어느 구조가 절대적으로 우월한가"가 아니라, **현재 팀 규모/도메인 복잡도/운영 성숙도에서 무엇이 합리적인가**를 기준으로 정리했습니다.

| 대안 | 장점 | 단점 | 적합한 상황 |
|------|------|------|------------|
| **전통 모놀리식** | 단일 코드베이스/배포로 개발 속도 빠름, 디버깅 단순, 트랜잭션 일관성 확보 쉬움 | 모듈 경계가 약해지기 쉬움, 변경 영향 범위 확산, 팀/기능 확장 시 충돌 증가 | 초기 제품, 작은 팀, 도메인 단순, 빠른 PoC |
| **모듈러 모놀리식** | 단일 배포의 단순함 유지 + 모듈 경계/의존성 제어 가능, MSA 전환 브리지 역할, 테스트/리팩터링 경계 명확 | 런타임은 여전히 단일 프로세스(장애 격리 제한), 독립 배포 불가, 아키텍처 규율이 없으면 일반 모놀리식으로 회귀 | 도메인 복잡도 증가 단계, MSA는 아직 과한 팀/운영 성숙도 |
| **MSA** | 서비스별 독립 배포/확장, 장애 격리, 기술 스택 자율성, 팀 자율성 강화 | 분산 트랜잭션/관측성/배포/운영 복잡도 급증, 네트워크 비용/지연, 초기 생산성 저하 가능 | 대규모 트래픽, 조직/팀 분리, 서비스별 독립 릴리스 필요 |

### 왜 현재는 모듈러 모놀리식인가?

- 전통 모놀리식보다 경계를 강제해 **결합도 관리**가 가능하고,
- MSA보다 운영 복잡도가 낮아 **현재 단계에서 비용 대비 효율**이 좋으며,
- 추후 `Port -> 원격 클라이언트` 교체로 **점진적 MSA 전환**이 가능합니다.

---

## 4. 전체 패키지 구조

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

## 5. 계층별 역할

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

## 6. 모듈 간 통신 규칙

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

## 7. 도메인 모델 — Agent

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

## 8. 모듈러 모놀리식 주장 타당성 검토

질문 주신 내용은 전반적으로 맞습니다. 다만 "항상"이 아니라 **전제 조건이 충족될 때** 성립합니다.

| 주장 | 판단 | 왜 맞는가 (근거) | 한계/주의점 |
|------|------|------------------|-----------|
| 모놀리식 대비 모듈 간 결합도가 낮아 MSA 전환 가능성이 높다 | **대체로 맞음** | `*ModuleApi`, `OrganizationPort`, `RbacPort`로 직접 구현 의존을 줄였고, 어댑터 교체 지점이 명확함 | DB 스키마/트랜잭션이 강하게 얽혀 있으면 분리 난이도는 여전히 높음 |
| 모듈 내 응집도가 높아 필요한 기능 추가/삭제가 용이하다 | **맞음 (구조가 유지될 때)** | 도메인/애플리케이션/인프라 분리로 변경 범위가 모듈 내부에 머물 가능성이 큼 | 횡단 규칙(common) 남용, 모듈 간 우회 참조가 생기면 응집도 이점이 약화됨 |
| 포트-어댑터 구조로 외부 영향이 내부 로직에 적다 | **맞음 (완전 차단은 아님)** | 내부 유스케이스는 Port 계약에 의존해 외부 구현 변화의 충격을 줄임 | 계약(인터페이스) 자체가 바뀌면 내부도 수정 필요, 성능/장애 특성은 결국 외부 영향 받음 |

### 모듈러 모놀리식의 핵심 장점 (요약)

- **개발 생산성**: 단일 배포/디버깅 단순성 유지
- **아키텍처 안정성**: 모듈 경계와 책임 분리로 리팩터링 용이
- **전환 유연성**: MSA 전환 시 분리 기준(경계/계약) 이미 존재
- **테스트 용이성**: 모듈 단위 테스트/통합 테스트 범위 설정이 쉬움

### 모듈러 모놀리식의 핵심 단점 (요약)

- **독립 배포 한계**: 모듈별 개별 릴리스가 어렵고 전체 재배포 필요
- **장애 격리 한계**: 단일 프로세스 장애가 시스템 전체에 영향 가능
- **스케일링 제약**: 특정 모듈만 선택적으로 스케일 아웃하기 어려움
- **규율 의존성**: 팀이 경계를 지키지 않으면 일반 모놀리식으로 쉽게 퇴화

---

## 9. 향후 MSA 전환 가능성

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
