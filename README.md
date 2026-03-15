# Identity Modulith

> AICC 솔루션의 인증·권한 모듈을 DDD 기반 Modular Monolith 구조로 설계한 프로젝트입니다.
> 넥스프론 R&D 현장실습(2025.07 – 2026.02) 중 실무에 적용했습니다.

---

## ⚡ Quick Start
```bash
# 1. 애플리케이션 실행 (Flyway 자동 마이그레이션)
./gradlew bootRun

# 2. Swagger UI 접속
http://localhost:8080/swagger-ui/index.html
# Authorize → Username: user / Password: 콘솔 출력값
```

**샘플 계정**
| 계정 | ID | PW |
|---|---|---|
| 관리자 | admin | password123 |
| 팀장 | teamlead01 | password123 |
| 상담사 | agent01 | password123 |

---

## 🛠 Tech Stack

`Spring Boot 3` `Spring Security` `Spring Modulith` `JPA` `PostgreSQL` `Keycloak` `SAML 2.0`

---

## 🏗 Architecture

### 왜 Modular Monolith인가?

인증/권한 로직이 서비스 계층에 혼재된 스파게티 코드를 해결해야 했습니다.
MSA도 고려했지만 **제한된 팀 규모와 초기 서비스의 운영 복잡도**를 감안해 오버엔지니어링이라 판단,
논리적 도메인 분리가 가능하면서 단일 배포를 유지하는 Modular Monolith를 선택했습니다.
```
com.nexfron.identitymodulith/
├── common/        # 공통 컴포넌트 (TenantContext, AuthPrincipal)
├── user/          # 상담사 관리
├── organization/  # 조직/부서 관리
└── rbac/          # 역할 기반 접근 제어
```

각 모듈은 `Presentation → Application → Domain ← Infrastructure` 계층 구조를 따르며,
모듈 간 의존성은 전용 Port 인터페이스로만 제한합니다.

### 왜 SAML 2.0인가?

AWS Connect는 SAML 2.0 기반 IdP 인증만 지원합니다.
OAuth2/OIDC도 검토했지만 기술적 제약과 B2B 엔터프라이즈 보안 요건을 종합해
**Keycloak을 IdP로 활용한 SAML 2.0 SSO 구조**를 채택했습니다.
```
User → AWS Connect → Keycloak (SAML AuthnRequest)
                   ← SAML Response (Assertion)
     ← 세션 발급 및 서비스 제공
```

---

## ⚡ 핵심 성과

### RBAC 권한 조회 N+1 개선

`User → Role → Permission` 조회 시 Lazy Loading으로 N+1 쿼리 병목이 발생했습니다.
Fetch Join + DTO 프로젝션으로 단일 쿼리로 개편했습니다.
(Cartesian Product 문제는 엔티티 전체가 아닌 필수 데이터만 조회하는 DTO 프로젝션으로 해결)

| 항목 | 개선 전 | 개선 후 |
|---|---|---|
| 실행 쿼리 수 | 26 queries | 1 query |
| 평균 응답 시간 | 255ms | 10ms **(96% 단축)** |

---

## 📁 상세 문서

설계 의사결정 및 상세 스펙은 [`/Docs`](./Docs) 폴더를 참고해주세요.

| 문서 | 설명 |
|---|---|
| [SAML2_VS_OIDC_COMPARISON.md](./Docs/SAML2_VS_OIDC_COMPARISON.md) | SAML 2.0 vs OIDC 선택 근거 |
| [PERFORMANCE_OPTIMIZATION_N_PLUS_1.md](./Docs/PERFORMANCE_OPTIMIZATION_N_PLUS_1.md) | N+1 쿼리 개선 상세 분석 |
| [ARCHITECTURE_DIAGRAMS.md](./Docs/ARCHITECTURE_DIAGRAMS.md) | 시스템 아키텍처 다이어그램 |
