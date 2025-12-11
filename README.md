📘 Identity Modulith – Organization Module

조직(부서) 구조 관리, 계층 기반 RBAC(Level 2 RBAC: Data Scope),
조직도 트리 조회, 부서 이동/삭제 로직을 포함하는 모듈입니다.

Spring Modulith + Hexagonal(Port/Adapter) 아키텍처로 구성되어 있으며,
User 모듈과는 OrgUserPort 인터페이스를 통해 의존성을 최소화합니다.

📁 프로젝트 구조 (Tree Structure)
```angular2html
identity-modulith
└── src/main/java/com/nexfron/identitymodulith
    ├── common
    │   └── exception
    │       ├── BusinessException.java
    │       └── EntityNotFoundException.java
    │
    └── organization
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

