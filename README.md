# Identity Modulith – Organization Module

조직(부서) 구조 관리, 계층 기반 RBAC(Level 2 RBAC: Data Scope),
조직도 트리 조회, 부서 이동/삭제 로직을 제공하는 모듈입니다.

Spring Modulith + Port/Adapter 아키텍처 기반으로 설계되었으며,
User 모듈과는 OrgUserPort 인터페이스를 통해 느슨하게 연결됩니다.

---

## 프로젝트 구조

```
identity-modulith
└── src/main/java/com/nexfron/identitymodulith
    └── organization
        ├── common
        │   └── exception
        │       ├── BusinessException.java
        │       └── EntityNotFoundException.java
        │
        ├── api
        │   ├── DepartmentController.java
        │   └── dto
        │       └── DepartmentDto.java
        │
        ├── application
        │   ├── port
        │   │   ├── OrgUserPort.java
        │   │   └── OrgUserView.java
        │   │
        │   └── service
        │       ├── DepartmentService.java
        │       └── OrgScopeService.java
        │
        ├── domain
        │   └── model
        │       ├── Department.java
        │       └── OrgRoleLevel.java
        │
        └── infrastructure
            └── repository
                └── DepartmentRepository.java
```

---

## 패키지별 역할

### organization.common.exception
- **BusinessException**  
  비즈니스 규칙 위반 시 throw하는 애플리케이션 공통 예외.
- **EntityNotFoundException**  
  엔티티 조회 실패 시 사용되는 예외.

---

### organization.api
- **DepartmentController**  
  부서 생성 / 이동 / 삭제 / 트리 조회 REST API 제공.
- **DepartmentDto**  
  Request/Response 구조 정의.  
  Entity ↔ DTO 변환 포함.

---

### organization.application.port
- **OrgUserPort**  
  User 모듈과 연결되는 Port 인터페이스.  
  DB 직접 조회 없이 사용자 존재 여부 등을 추상화함.
- **OrgUserView**  
  조직 모듈에서 필요로 하는 최소한의 사용자 필드 구조.

---

### organization.application.service
- **DepartmentService**  
  부서 생성, 이동, 삭제, 트리 변환 등 조직 관리 핵심 로직 담당.
- **OrgScopeService**  
  Level 2 RBAC(Data Scope) 계산 서비스.  
  사용자 역할(OrgRoleLevel)에 따라 접근 가능한 조직 범위 반환.

---

### organization.domain.model
- **Department**  
  조직 구조의 핵심 엔티티.  
  parent, depth, orgPath, 경로 재계산 등 도메인 규칙 포함.
- **OrgRoleLevel**  
  RBAC 범위 Enum (MEMBER / TEAM_LEAD / ADMIN).

---

### organization.infrastructure.repository
- **DepartmentRepository**  
  Spring Data JPA 기반 Repository.  
  orgPath prefix 기반 하위 조직 조회 제공.

---

## 제공 기능 요약

### 1. 부서 생성(Create)
- parent 기반 orgPath 생성  
- PostPersist 훅으로 실제 deptId 기반 경로 확정

### 2. 부서 이동(Move)
- 순환 참조 방지: 자식 → 부모 방향 이동 금지  
- orgPath prefix 기반 하위 부서 일괄 업데이트

### 3. 부서 삭제(Delete)
- 하위 부서 존재 시 삭제 불가  
- OrgUserPort 통해 부서 소속 사용자 존재 시 삭제 불가

### 4. 조직도 트리 조회
- flat list → tree 구조로 변환하여 반환

### 5. Level 2 RBAC (Data Scope)
- MEMBER → 본인 부서  
- TEAM_LEAD → 본인 + 하위 부서  
- ADMIN → 전체 테넌트 부서  
- OrgScopeService에서 계산

---

## API 테스트 예시

### 1) 부서 생성
```
POST /api/v1/departments
Header: X-Tenant-Id: tenantA
{
  "name": "본부",
  "type": "HQ",
  "parentId": null
}
```

### 2) 조직도 조회
```
GET /api/v1/departments/tree
Header: X-Tenant-Id: tenantA
```

### 3) 부서 이동
```
PUT /api/v1/departments/{deptId}/move
{
  "newParentId": 2
}
```

### 4) 부서 삭제
```
DELETE /api/v1/departments/{deptId}
```

---

## 아키텍처 개요

- Spring Modulith 기반 내부 모듈 구조  
- Port/Adapter 패턴으로 User 모듈과 결합 최소화  
- Domain Model이 비즈니스 규칙을 소유  
- Service는 도메인 규칙을 조합하여 애플리케이션 서비스 제공  
- User DB 직접 접근 없이 OrgUserPort abstraction 기반 통신  

---

## 향후 확장 계획

- User 모듈 Adapter 구현 (DB or REST 연동)
- 조직 변경 이력(Audit Log) 저장 기능
- Soft Delete 도입 고려
- Swagger/OpenAPI 문서 자동화
- 조직도 캐싱 적용
