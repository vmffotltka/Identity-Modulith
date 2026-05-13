# 🔐 Identity Modulith - 인증/인가 통합 모듈

> **"팀과의 투명한 소통과 공식 문서 탐구를 바탕으로, 제약 환경에 맞춰 아키텍처를 절충하고 데이터 조회를 최적화한 프로젝트입니다."**

**Identity Modulith**는 AICC 솔루션과 외부 서비스 연동 시 필요한 인증 및 권한을 관리하는 통합 모듈입니다. 
넥스프론 R&D 인턴십 과정에서 진행되었으며, **'모듈 간 결합도를 낮추는 설계'** 와 **'데이터베이스 조회 성능 개선'** 에 집중했습니다.

---

## 🏛️ 전체 아키텍처 개요

### 시스템 구성도

```
┌─────────────────────────────────────────────────────────────────┐
│                    Identity Modulith                           │
│                  (Modular Monolith)                            │
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐ │
│  │   User 모듈      │  │ Organization 모듈 │ │   RBAC 모듈   │ │
│  ├──────────────────┤  ├──────────────────┤  ├──────────────┤ │
│  │ • Agent 관리     │  │ • Department 관리 │ │ • Role 관리  │ │
│  │ • 인증/권한 조회 │  │ • 부서 구조 관리  │ │ • Permission │ │
│  │ • Keycloak 동기 │  │ • Agent 배치 관리 │ │   관리       │ │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────┘ │
│           │                     │                     │       │
│           └─────────────────────┴─────────────────────┘       │
│                         ↓                                      │
│             ┌─────────────────────────────┐                    │
│             │  공통 계층 (Common Layer)   │                    │
│             │ • Security & SAML           │                    │
│             │ • Cross-cutting Concerns   │                    │
│             │ • Exception Handling        │                    │
│             └─────────────────────────────┘                    │
└─────────────────────────────────────────────────────────────────┘
           ↓                    ↓                    ↓
     ┌──────────────┐   ┌────────────────┐  ┌──────────────┐
     │ PostgreSQL   │   │ Keycloak (IdP) │  │AWS Connect   │
     │(JPA/Flyway) │   │  (SAML 2.0)    │  │ (SSO)        │
     └──────────────┘   └────────────────┘  └──────────────┘
```

### 모듈 간 통신 흐름

```
사용자 요청 (with JWT Token)
         ↓
    ┌─────────────────────────────────────────────┐
    │  Spring Security (SAML 필터 체인)          │
    │  • Token 검증 (RS256 서명 검증)           │
    │  • JWT Context 설정 (Tenant, Agent)      │
    └──────────────────┬──────────────────────┘
                       ↓
         ┌─────────────────────────────┐
         │   Presentation 계층         │
         │  (AgentController 등)       │
         └──────────────┬──────────────┘
                        ↓
         ┌─────────────────────────────┐
         │  Application 계층           │
         │  (AgentService 등)          │
         │  • UseCase 구현             │
         │  • Port 인터페이스 호출     │
         └──────────┬──────────┬───────┘
                    ↓          ↓
       ┌────────────────────────────────┐
       │  Infrastructure 계층           │
       │  • Adapter (Port 구현체)       │
       │  • JPA Repository              │
       │  • Keycloak Admin API 호출     │
       └────────────┬──────────┬────────┘
                    ↓          ↓
          ┌──────────────┐ ┌────────────┐
          │ PostgreSQL   │ │ Keycloak   │
          │ (도메인 DB)  │ │ (Auth DB)  │
          └──────────────┘ └────────────┘
```

---

## 📦 1. 모듈별 역할 및 구조

### 🔹 User 모듈 (상담사 관리)

**책임:**
- 상담사(Agent) 계정 관리 (CRUD)
- 상태 관리 (ACTIVE, SUSPENDED, RETIRED)
- Keycloak과 동기화 (SSO 연동)
- 비밀번호 정책 (초기화, 변경, 만료)

**주요 클래스:**
- `Agent` (도메인 모델): 상태 전이 규칙을 포함한 엔티티
- `AgentService` (Application Service): 11개 유스케이스 구현
- `AgentRepository` (JPA): DB 조회 및 저장

**외부 의존성:**
- `OrganizationPort`: 부서 정보 조회 (Organization 모듈 추상화)
- `RbacPort`: 권한 조회 (RBAC 모듈 추상화)

---

### 🔹 RBAC 모듈 (역할 기반 접근 제어)

**책임:**
- 역할(Role) 관리 (생성, 수정, 삭제)
- 권한(Permission) 관리 및 권한 그룹화
- Agent별 권한 조회 및 검증
- 권한 캐싱 (성능 최적화)

**주요 클래스:**
- `RbacManagementServiceImpl`: 역할/권한 CRUD
- `RbacQueryServiceImpl`: 권한 조회 (N+1 최적화가 적용됨)
- `AgentRoleRepository`: 3-JOIN 쿼리로 권한 코드 다이렉 조회

**설계 특징:**
- 계층형이 아닌 **수평적(Flat) RBAC**: 부서 간 역할 교차가 자유로움
- DTO 프로젝션으로 필요한 데이터만 조회
- 스칼라 쿼리로 네트워크 오버헤드 절감

---

### 🔹 Organization 모듈 (부서 관리)

**책임:**
- 부서(Department) 구조 관리
- Agent와 부서 간 관계 설정
- 부서 계층 조회 및 캐싱

**주요 클래스:**
- `Department` (도메인 모델): 부서 정보
- `DepartmentService`: 부서 조회 및 관리
- `DepartmentRepository`: DB 조회

**설계 특징:**
- **Fetch Join + DTO 프로젝션 병행**: Agent 목록 조회 시 성능 최적화
- 중복 조회 방지를 위한 프로젝션 활용

---

### 🔹 Common 계층 (횡단 관심사)

**책임:**
- Spring Security 필터 체인 (SAML 토큰 검증)
- 요청 컨텍스트 관리 (TenantContext, JwtUserContext)
- 공통 예외 처리 (GlobalExceptionHandler)
- 공통 설정 및 유틸리티

**주요 컴포넌트:**
- `Saml2SecurityConfig`: Spring Security + SAML 설정
- `CustomPermissionEvaluator`: 권한 평가 (@PreAuthorize, @Secured)
- `TenantContext`: 요청 테넌트 정보 관리 (ThreadLocal)
- `JwtUserContext`: 현재 인증 사용자 정보 관리

---

## 🏗️ 2. 기술적 의사결정 (Trade-off)

### 📍 Decision 1. 아키텍처: 결합도 낮추기를 위한 DIP 적용
* **상황:** 팀의 '모듈러 모놀리스' 구조 하에서 조직(Org)과 사용자(User) 모듈 간 직접 참조 발생 우려.
* **고민:** 모듈 간 강결합은 향후 서비스 독립 시 스파게티 코드 문제를 야기할 것으로 판단.
* **결과:** **의존성 역전(DIP) 패턴** 적용. 인터페이스(Port)와 어댑터(Adapter)를 통해 모듈 간 결합도를 낮추고 독립성을 확보함.
  - `OrganizationPort` / `OrganizationAdapter`: User 모듈에서 Organization 모듈 호출
  - `RbacPort` / `RbacAdapter`: User 모듈에서 RBAC 모듈 호출
  - `UserModuleApi`: 다른 모듈에서 User 모듈 사용 시 노출할 공개 인터페이스

### 📍 Decision 2. 보안 표준: 비용과 규격에 맞춘 Keycloak 도입
* **상황:** 솔루션-AWS Connect 간 SSO 연동 필요. 단, AWS Connect는 SAML 2.0 규격만 지원함.
* **고민:** Okta 등 상용 솔루션은 라이선스 비용 부담이 큼. 오픈소스 중 규격을 충족하는 대안 필요.
* **결과:** **Keycloak(오픈소스 IdP)** 채택. 비용 없이 SAML 2.0 연동 환경을 구축하고 파편화된 인증 체계를 통합함.
  - SAML 응답의 JWT 토큰과 RS256 공개키를 활용한 검증
  - Spring Security Saml2 필필터로 토큰 검증 자동화

### 📍 Decision 3. 데이터베이스: 도메인에 최적화된 수평적(Flat) RBAC
* **상황:** 초기 기획은 계층형 구조였으나, 실제 상담사 업무는 부서 간 역할 교차가 잦고 비계층적임.
* **고민:** 계층형 구조는 불필요한 자기 참조(Self-Join)로 인해 조회 성능과 관리 복잡도를 높임.
* **결과:** 직관적인 **수평적(Flat) RBAC**로 개편. 쿼리 성능을 방어하고 업무 현장에 맞는 유연한 운영 환경 구축.

---

## ⚡ 3. 권한 조회 최적화

### 🚨 N+1 쿼리 병목 해결

**문제 상황:**
사용자의 권한을 검증할 때 `User → Role → Permission` 과정에서 JPA의 지연 로딩(Lazy Loading)으로 인해 불필요한 엔티티 정보까지 불러오면서 **최대 26회의 N+1 쿼리**가 발생했습니다.

**해결 방법:**
데이터베이스 조회의 비트 스트림 오버헤드를 줄이고자, 꼭 필요한 권한 코드 문자열만 가져오는 **스칼라 프로젝션(Scalar Projection)** 과 JOIN을 적용하여 쿼리를 최적화했습니다.

```java
// Before: N+1 쿼리 발생 (26회)
Set<String> permissionCodes = roleIds.stream()
    .flatMap(roleId -> {
        var permissions = rolePermissionRepository
                .findPermissionsByRoleIdAndTenant(roleId, tenantId); // N쿼리
        return permissions.stream().map(PermissionJpaEntity::getCode);
    })
    .collect(Collectors.toSet());

// After: 1쿼리로 최적화
List<String> permissionCodes = agentRoleRepository
    .findPermissionCodesByAgentIdAndTenant(agentId, tenantId); // 3-JOIN + 스칼라 프로젝션
```

**성능 개선 결과:**

| 성능 지표 | 개선 전 | 개선 후 | 개선율 |
| :--- | :--- | :--- | :--- |
| **실행 쿼리 수** | 26 queries | **1 query** | 96% ↓ |
| **평균 응답 시간** | 255ms | **10ms** | **96% ⚡** |
| **DB 네트워크 왕복** | 26회 | 1회 | 25회 절감 |
| **메모리 사용량** | 전체 Entity | Code만 | ~80% ↓ |

**적용된 최적화 기법:**
1. **스칼라 프로젝션**: 필요한 컬럼만 SELECT (Code)
2. **3-way JOIN**: Agent → AgentRole → Role → Permission 한 번에 조회
3. **Batch 쿼리**: Stream 대신 단일 쿼리로 통합

---

## 🛠 4. Tech Stack

* **Backend:** `Spring Boot`, `Spring Security`
* **Data & ORM:** `PostgreSQL`, `JPA (Hibernate)`
* **Infra & Security:** `Keycloak`, `SAML 2.0`, `Modular Monolith`

---

## 🚀 5. Quick Start

```bash
# 1. 애플리케이션 실행
./gradlew bootRun

# 2. Swagger UI 접속을 통한 API 테스트
http://localhost:8080/swagger-ui/index.html

````

---

## 📐 6. 아키텍처 계층 설명

### 계층 구조 (Layered Architecture with DDD)

```
┌─────────────────────────────────────────┐
│    Presentation Layer (Controller)      │
│  • HTTP 요청 처리                        │
│  • DTO 변환 (Request/Response)         │
│  • 예외 처리 (ExceptionHandler)         │
└────────────────┬────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│   Application Layer (Service)           │
│  • Use Case 구현                        │
│  • Port 인터페이스 호출                 │
│  • 비즈니스 흐름 조율                   │
└────────────────┬────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│    Domain Layer (Business Logic)        │
│  • 도메인 모델 (Entity, ValueObject)   │
│  • 비즈니스 규칙 (캡슐화)              │
│  • 도메인 서비스                        │
│  • 도메인 이벤트                        │
└────────────────┬────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│  Infrastructure Layer (Technical)       │
│  • JPA/Repository 구현                  │
│  • Port Adapter 구현                    │
│  • Keycloak API 호출                    │
│  • DB 연결/트랜잭션                    │
└─────────────────────────────────────────┘
```

**핵심 원칙:**
- **단방향 의존성**: 상층은 하층에 의존하지만, 하층은 상층에 의존하지 않음
- **도메인 중심**: 비즈니스 규칙은 도메인 계층에 집중
- **포트-어댑터 패턴**: 외부 의존성(DB, API)을 인터페이스로 추상화

---

## 🔗 7. 모듈 간 통신 규칙

### 핵심 원칙

> **모듈 내부 구현 클래스를 직접 참조하지 않는다.**  
> **루트 패키지의 `*ModuleApi` 인터페이스를 통해서만 통신한다.**

### 통신 패턴

```
┌─────────────────────────────────────────────────────────────┐
│                   User 모듈                                 │
│                                                             │
│  AgentService (Application Layer)                          │
│      │                                                      │
│      ├─ OrganizationPort ──────────────────────────────┐   │
│      │  (인터페이스)                   (추상화)         │   │
│      │                                                  │   │
│      └─ RbacPort ──────────────────────────────────┐   │   │
│         (인터페이스)          (추상화)              │   │   │
│                                                    │   │   │
│  OrganizationAdapter (Infrastructure Layer)   │   │   │   │
│  RbacAdapter (Infrastructure Layer)          │   │   │   │
│      │                                        │   │   │   │
│      ├─ OrganizationModuleApi (호출) ────────┘   │   │   │
│      │                                            │   │   │
│      └─ RbacModuleApi (호출) ──────────────────┘   │   │   │
└─────────────────────────────────────────────────────┼───────┤
                                                     ↓
                          ┌────────────────────────────────┐
                          │  Organization/RBAC 모듈        │
                          │ (ModuleApi 구현)               │
                          └────────────────────────────────┘
```

**예시:**
```java
// User 모듈 Application Layer
public class AgentService {
    private final OrganizationPort organizationPort;
    private final RbacPort rbacPort;
    
    // OrganizationPort를 통해 부서 정보 조회
    DepartmentInfo dept = organizationPort.getDepartmentInfo(tenantId, deptId);
    
    // RbacPort를 통해 권한 조회
    Set<String> permissions = rbacPort.getPermissionsByAgent(agentId, tenantId);
}

// Infrastructure Layer
@Component
public class OrganizationAdapter implements OrganizationPort {
    private final OrganizationModuleApi organizationModuleApi;
    
    @Override
    public Optional<DepartmentInfo> getDepartmentInfo(String tenantId, String deptId) {
        // 실제로 OrganizationModuleApi 호출
        return organizationModuleApi.findDepartment(tenantId, deptId);
    }
}
```

---

## 🔐 8. 인증/인가 흐름

### SAML 기반 토큰 검증 과정

```
1. 사용자 로그인 (AWS Connect)
   ↓
2. Keycloak으로 SAML 인증 요청
   ↓
3. Keycloak이 SAML Response 반환 (JWT 포함)
   ↓
4. Spring Security Saml2 필터
   ├─ SAML Response 파싱
   ├─ RS256 공개키로 JWT 서명 검증
   ├─ Token Claims 추출 (sub, tenant, permissions)
   ├─ TenantContext 설정 (ThreadLocal)
   └─ JwtUserContext 설정 (SecurityContextHolder)
   ↓
5. 요청 처리 (Controller → Service)
   ├─ @PreAuthorize("hasPermission(...)") 평가
   └─ CustomPermissionEvaluator 호출
   ↓
6. 권한 검증 (RbacQueryService.permissionsOf)
   ├─ Agent ID로 권한 조회 (1쿼리)
   └─ 요청 권한이 조회된 권한에 포함되는지 검증
   ↓
7. 비즈니스 로직 실행 (AgentService 등)
   ↓
8. 응답 반환
```

**핵심 컴포넌트:**

| 컴포넌트 | 책임 |
|---------|------|
| `Saml2SecurityConfig` | Spring Security + SAML 필터 체인 구성 |
| `Saml2AuthenticationSuccessHandler` | 토큰 추출 및 컨텍스트 설정 |
| `TenantContext` | 현재 요청의 테넌트 정보 관리 |
| `JwtUserContext` | 현재 인증 사용자 정보 관리 |
| `CustomPermissionEvaluator` | `@PreAuthorize` 진행 시 권한 평가 |
| `RbacQueryService.permissionsOf` | Agent의 권한 조회 (최적화됨) |

---

## 📚 9. 참고 문서

- **[ARCHITECTURE_DDD_MODULITH.md](./Docs/ARCHITECTURE_DDD_MODULITH.md)**: 전체 아키텍처 설계 원칙 및 계층 설명
- **[DIP_ORGANIZATION_USER.md](./Docs/DIP_ORGANIZATION_USER.md)**: 의존성 역전 원칙 적용 사례 (Organization-User 모듈)
- **[SPRING_SECURITY_AUTHN_AUTHZ_FLOW.md](./Docs/SPRING_SECURITY_AUTHN_AUTHZ_FLOW.md)**: Spring Security 인증/인가 상세 흐름
- **[PERFORMANCE_OPTIMIZATION_N_PLUS_1.md](./Docs/PERFORMANCE_OPTIMIZATION_N_PLUS_1.md)**: N+1 쿼리 해결 및 최적화 기법
- **[SAML_KEYCLOAK_SETUP_GUIDE.md](./Docs/SAML_KEYCLOAK/SAML_KEYCLOAK_SETUP_GUIDE.md)**: Keycloak + SAML 2.0 설정 가이드
- **[DATABASE_SCHEMA.md](./Docs/DATABASE/DATABASE_SCHEMA.md)**: 데이터베이스 스키마 설명

---