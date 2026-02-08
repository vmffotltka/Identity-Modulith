# 🚀 데이터베이스 완전 초기화 가이드

## ⚠️ 주의사항

**이 스크립트는 모든 데이터를 삭제하고 처음부터 다시 만듭니다!**
- Flyway 히스토리 삭제
- 모든 테이블 삭제
- 새로운 테이블 생성
- 테스트 데이터 삽입

---

## 🎯 실행 방법

### 방법 1: PostgreSQL에서 직접 실행 (추천)

```bash
# PostgreSQL 연결
psql -U postgres -d your_database_name

# 스크립트 실행
\i C:/Project/identity-modulith-master/reset_database.sql

# 또는 상대 경로
\i reset_database.sql

# 종료
\q
```

### 방법 2: 커맨드라인에서 직접 실행

```bash
psql -U postgres -d your_database_name -f reset_database.sql
```

### 방법 3: IntelliJ Database 툴에서 실행

```
1. IntelliJ에서 Database 탭 열기
2. PostgreSQL 데이터베이스 연결
3. reset_database.sql 파일 열기
4. Ctrl+Enter로 전체 실행
```

---

## ✅ 실행 후 확인

스크립트 실행 후 다음 메시지가 나오면 성공:

```
=============================================================================
Identity Modulith Database Reset Complete!
=============================================================================
Tables Created:
  1. org_departments
  2. user_agents
  3. rbac_roles
  4. rbac_permissions
  5. rbac_role_permissions
  6. rbac_agent_roles
=============================================================================
Test Accounts:
  - admin / admin123 (시스템관리자)
  - dev.lead / admin123 (김팀장)
  - dev.member / admin123 (이개발)
=============================================================================
```

---

## 🏃 애플리케이션 실행

데이터베이스 초기화 후 Spring Boot 애플리케이션 실행:

### IntelliJ에서 실행 (가장 쉬움)

```
1. IdentityModulithApplication.java 우클릭
2. Run 'IdentityModulithApplication'
```

### Gradle로 실행

```bash
./gradlew bootRun
```

---

## 📊 생성된 데이터

### 부서 (org_departments)
- 넥스프론 (회사)
  - 개발본부
    - 백엔드팀
    - 프론트엔드팀
  - 영업본부

### 사용자 (user_agents)
| 로그인ID | 이름 | 역할 | 부서 |
|---------|------|------|------|
| admin | 시스템관리자 | ADMIN | 넥스프론 |
| dev.lead | 김팀장 | TEAM_LEAD | 백엔드팀 |
| dev.member | 이개발 | MEMBER | 백엔드팀 |

### 역할 (rbac_roles)
- ADMIN (전체 접근)
- TEAM_LEAD (부서 접근)
- MEMBER (개인 접근)

### 권한 (rbac_permissions)
- user:create, user:read, user:update, user:delete
- org:create, org:read, org:update
- rbac:manage
- report:view, report:export

---

## 🔧 테이블 구조

모든 테이블이 JPA 엔티티와 **완벽히 일치**합니다:

| 테이블명 | JPA 엔티티 | 설명 |
|---------|-----------|------|
| `org_departments` | `DepartmentJpaEntity` | 조직/부서 |
| `user_agents` | `AgentJpaEntity` | 사용자/상담사 |
| `rbac_roles` | `RoleJpaEntity` | 역할 |
| `rbac_permissions` | `PermissionJpaEntity` | 권한 |
| `rbac_role_permissions` | `RolePermissionJpaEntity` | 역할-권한 매핑 |
| `rbac_agent_roles` | `AgentRoleJpaEntity` | 사용자-역할 매핑 |

### 주요 개선사항

✅ **rbac_agent_roles** 테이블
- RBAC 모듈에 정의되어 있으므로 `rbac_` 접두사 사용
- 다른 RBAC 테이블들(rbac_roles, rbac_permissions 등)과 명명 규칙 일관성 유지

✅ **assigned_at** 컬럼 추가
- `rbac_agent_roles.assigned_at`
- `rbac_role_permissions.assigned_at`
- 감사 추적 및 이력 관리 지원

---

## ❓ FAQ

### Q: V3_0_0 마이그레이션 파일은?
A: 삭제했습니다. 이제 V1, V2만 사용하며, 둘 다 동일한 최신 스키마를 생성합니다.

### Q: Flyway 오류가 계속 나면?
A: `reset_database.sql` 실행 → Flyway 히스토리 초기화됨 → 애플리케이션 재실행

### Q: 데이터를 백업하려면?
A: 
```bash
pg_dump -U postgres your_database > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Q: 실서버에서도 사용 가능?
A: ⚠️ **절대 안됩니다!** 이 스크립트는 **모든 데이터를 삭제**합니다. 개발/테스트 환경에서만 사용하세요.

---

## 🎉 완료 체크리스트

- ✅ V3_0_0 마이그레이션 파일 삭제
- ✅ reset_database.sql 생성 (통합 스크립트)
- ✅ 테이블명 통일 (rbac_agent_roles - RBAC 모듈 일관성)
- ✅ assigned_at 컬럼 추가
- ✅ 테스트 데이터 포함

---

**다음 단계**:
1. `reset_database.sql` 실행
2. Spring Boot 애플리케이션 실행
3. 끝! 🎉

**작성일**: 2026-02-07

