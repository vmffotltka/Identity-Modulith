# 🔐 Identity Modulith - B2B 환경에 최적화된 인증/인가 통합 모듈

> **"무조건적인 최신 기술(MSA) 도입을 지양하고, 팀의 운영 리소스와 비즈니스 인프라 제약을 분석하여 도출한 실용적 아키텍처입니다."**

**Identity Modulith**는 AICC(인공지능 컨택센터) 솔루션의 인증 및 권한을 중앙 통제하는 통합 모듈입니다. 
넥스프론 R&D 인턴십 과정에서 진행되었으며, **'초기 서비스의 운영 복잡도 제어'** 와 **'트래픽 피크 타임의 DB 부하 방어'** 라는 두 가지 핵심 목표를 달성하는 데 집중했습니다.

---

## 🏗️ 1. 아키텍처 및 기술 선택의 이유 (Trade-off & Decision)
*※ 본 섹션은 기술적 이상과 비즈니스/인프라 현실 사이의 트레이드오프를 고민하고 의사결정한 과정입니다.*

### 📍 Decision 1. 아키텍처: MSA vs Modular Monolith
* **상황 및 제약:** 인증/권한 로직이 타 서비스 계층과 혼재되어 강결합(Spaghetti Code)이 발생. 모듈의 독립성이 필요했으나, 당시 인프라 운영 예산과 팀 규모가 제한적이었습니다.
* **트레이드오프 고민:** 완전한 MSA(Microservices Architecture)는 완벽한 배포 독립성을 제공하지만, 네트워크 오버헤드와 분산 트랜잭션 관리라는 운영 복잡도를 초래합니다. 
* **의사결정 및 결과:** 운영 리소스를 방어하면서도 결합도를 낮추기 위해 **DDD 기반의 모듈러 모놀리스(Modular Monolith)** 구조를 선택했습니다. `Presentation → Application → Domain ← Infrastructure` 계층을 철저히 분리하고, 모듈 간 참조는 전용 Port 인터페이스를 통한 **의존성 역전(DIP)** 으로 제한했습니다. 결과적으로 단일 배포의 운영 편의성을 유지하면서도, 특정 모듈의 장애가 전체 시스템으로 전파되는 것을 격리했습니다.

### 📍 Decision 2. 보안 표준: OIDC vs SAML 2.0
* **상황 및 제약:** AICC 솔루션과 AWS Connect 간의 안전한 SSO(Single Sign-On) 연동 환경 구축 필요.
* **트레이드오프 고민:** OAuth 2.0 / OIDC 기반 인증이 최신 트렌드이며 개발 편의성이 높으나, AWS Connect 인프라가 기술적으로 SAML 2.0 기반 IdP 인증만을 지원하는 '인프라적 제약'이 존재했습니다.
* **의사결정 및 결과:** 최신 기술이라는 '이상' 대신, 인프라 제약과 B2B 엔터프라이즈 보안 요구사항이라는 '현실'을 반영하여 **Keycloak을 IdP로 활용한 SAML 2.0 SSO 아키텍처**를 구축했습니다. 이를 통해 파편화된 인증 체계를 일원화하고 유지보수 공수를 크게 절감했습니다.

### 📍 Decision 3. 데이터베이스: 계층형 RBAC vs 수평적(Flat) RBAC
* **상황 및 제약:** 초기 기획된 계층형 권한 구조(Hierarchical RBAC)는 상담사의 부서 간 역할 교차가 잦은 AICC 업무 현실과 맞지 않았습니다.
* **트레이드오프 고민:** 계층형 구조는 확장에 유리해 보였으나, 권한 조회 시 DB의 자기 참조(Self-Join)와 재귀 쿼리를 유발하여 시스템 피로도를 높이는 원인이 되었습니다.
* **의사결정 및 결과:** 비즈니스 유연성과 운영 효율을 근거로 **수평적(Flat) RBAC** 설계로 전면 개편을 주도했습니다. 구조적 복잡성을 덜어내어 쿼리 성능을 확보하고, 유지보수성을 극대화했습니다.

---

## ⚡ 2. 운영 안정성 최적화 (Performance & Stability)

### 🚨 N+1 쿼리 병목 제거 및 DB 커넥션 풀 고갈 방어
AICC 특성상 출근 시간대에 상담사들의 대규모 동시 접속이 발생합니다. 이때 `User → Role → Permission` 권한 조회 과정에서 JPA의 Lazy Loading으로 인해 **26회의 N+1 쿼리 병목**이 발생하는 것을 식별했습니다. 이는 피크 타임에 DB 커넥션 풀을 고갈시켜 전체 장애를 유발할 수 있는 치명적 리스크였습니다.

**[해결 과정 및 결과]**
무작정 Fetch Join을 사용하여 엔티티 전체를 메모리에 올리는(Cartesian Product) 대신, 영속성 컨텍스트의 오버헤드를 최소화하기 위해 **3-JOIN 쿼리와 필수 권한 코드 문자열만 추출하는 스칼라 프로젝션(Scalar Projection)** 을 적용했습니다.

| 성능 지표 | 개선 전 | 개선 후 | 비고 |
| :--- | :--- | :--- | :--- |
| **실행 쿼리 수** | 26 queries | **1 query** | DB 커넥션 점유 최소화 |
| **평균 응답 시간** | 255ms | **10ms** | **96% 성능 개선** |

---

## 🛠 3. Tech Stack

* **Backend:** `Spring Boot 3`, `Spring Security`, `Spring Modulith`
* **Data & ORM:** `PostgreSQL`, `JPA (Hibernate)`
* **Infra & Security:** `Keycloak`, `SAML 2.0`

---

## 🚀 4. Quick Start

```bash
# 1. 애플리케이션 실행 (Flyway 자동 마이그레이션 포함)
./gradlew bootRun

# 2. Swagger UI 접속을 통한 API 테스트
http://localhost:8080/swagger-ui/index.html
# Authorize → Username: user / Password: (애플리케이션 콘솔 출력값 확인)
