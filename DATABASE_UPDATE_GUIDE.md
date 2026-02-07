# 🚀 데이터베이스 스키마 업데이트 완료!

## ✅ 변경 사항

### 1. **DepartmentEntity에 필드 추가**
- ✅ `code` 필드 추가 (VARCHAR(30), NOT NULL)
  - 사용자 친화적 부서 코드
  - 예: `DEV-BE`, `SALES-DIV`, `NEXFRON`
  - 테넌트별 고유: UNIQUE (tenant_id, code)
  
- ✅ `customTypeName` 필드 추가 (VARCHAR(50), NULLABLE)
  - type='CUSTOM'일 때 사용자 정의 타입명
  - 예: "센터", "파트", "실"

### 2. **새로운 마이그레이션 스크립트**
- 파일: `V1_0_0__Complete_Schema_With_Code.sql`
- 엔티티 구조를 정확히 반영한 완전한 스키마
- 표준 데이터셋 포함:
  - 부서 5개 (넥스프론, 개발본부, 영업본부, 백엔드팀, 프론트엔드팀)
  - 사용자 3명 (admin, dev.lead, dev.member)
  - 역할 3개 (ADMIN, TEAM_LEAD, MEMBER)
  - 권한 10개 (user, org, rbac, report 관련)
  - 역할-권한 매핑 18개
  - 사용자-역할 매핑 3개

---

## 🗄️ 데이터베이스 초기화 방법

### Option 1: PostgreSQL 클라이언트 사용 (권장)

#### 1. pgAdmin 또는 DBeaver 접속
```
Host: 1.224.162.188
Port: 51445
Database: nexfron
Username: admin
Password: nexfron11!
```

#### 2. Flyway 메타데이터 삭제 SQL 실행
```sql
DROP TABLE IF EXISTS flyway_schema_history CASCADE;
```

또는 프로젝트 루트의 `reset_flyway.sql` 파일 실행:
```bash
psql -h 1.224.162.188 -p 51445 -U admin -d nexfron -f reset_flyway.sql
```

#### 3. 애플리케이션 재시작
```bash
.\gradlew bootRun
```

---

### Option 2: Spring Boot 설정 변경 (개발 환경 전용)

`application.yml`에 다음 추가:
```yaml
spring:
  flyway:
    clean-disabled: false  # Flyway clean 활성화 (⚠️ 위험: 모든 데이터 삭제)
```

그리고 애플리케이션 시작 전에:
```bash
.\gradlew flywayClean
.\gradlew bootRun
```

**⚠️ 경고**: 이 방법은 모든 데이터를 삭제합니다!

---

## 📊 생성된 데이터 확인

애플리케이션 시작 후:

### 1. 부서 데이터 확인
```sql
SELECT dept_id, name, code, type, depth, org_path 
FROM org_departments 
ORDER BY depth, dept_id;
```

**예상 결과**:
| name | code | type | depth |
|------|------|------|-------|
| 넥스프론 | NEXFRON | COMPANY | 0 |
| 개발본부 | DEV-DIV | DIVISION | 1 |
| 영업본부 | SALES-DIV | DIVISION | 1 |
| 백엔드팀 | DEV-BE | TEAM | 2 |
| 프론트엔드팀 | DEV-FE | TEAM | 2 |

### 2. 사용자 데이터 확인
```sql
SELECT agent_id, login_id, name, employee_id, email, status 
FROM user_agents 
ORDER BY created_at;
```

**예상 결과**:
| login_id | name | employee_id | email |
|----------|------|-------------|-------|
| admin | 시스템관리자 | EMP-0001 | admin@nexfron.com |
| dev.lead | 김팀장 | EMP-0002 | dev.lead@nexfron.com |
| dev.member | 이개발 | EMP-0003 | dev.member@nexfron.com |

### 3. 역할-권한 확인
```sql
SELECT r.name AS role_name, 
       COUNT(rp.permission_id) AS permission_count
FROM rbac_roles r
LEFT JOIN rbac_role_permissions rp ON r.role_id = rp.role_id
WHERE r.tenant_id = 'default-tenant'
GROUP BY r.role_id, r.name
ORDER BY permission_count DESC;
```

**예상 결과**:
| role_name | permission_count |
|-----------|------------------|
| ADMIN | 10 (모든 권한) |
| TEAM_LEAD | 5 |
| MEMBER | 3 |

---

## 🎯 API 테스트

### 1. Swagger UI 접속
```
http://localhost:8080/swagger-ui/index.html
```

### 2. 부서 생성 테스트 (POST /api/org/departments)
```json
{
  "name": "DevOps팀",
  "type": "TEAM",
  "code": "DEV-OPS",
  "parentId": "dept-00000000-0000-0000-0000-000000000002"
}
```

**✅ 성공 응답 (201 Created)**:
```json
{
  "deptId": "uuid-generated",
  "tenantId": "default-tenant",
  "name": "DevOps팀",
  "type": "TEAM",
  "code": "DEV-OPS",
  "parentId": "dept-00000000-0000-0000-0000-000000000002",
  "orgPath": "/dept-00000000-0000-0000-0000-000000000001/dept-00000000-0000-0000-0000-000000000002/uuid-generated",
  "depth": 2,
  "status": "ACTIVE"
}
```

### 3. 전체 조직도 조회 (GET /api/org/departments)
```
GET http://localhost:8080/api/org/departments
```

---

## 📝 테스트 계정

| 사용자 | 로그인 ID | 비밀번호 | 역할 | 부서 |
|--------|-----------|----------|------|------|
| 시스템관리자 | `admin` | `admin123` | ADMIN | - |
| 김팀장 | `dev.lead` | `admin123` | TEAM_LEAD | 백엔드팀 |
| 이개발 | `dev.member` | `admin123` | MEMBER | 백엔드팀 |

---

## 🔍 트러블슈팅

### 문제 1: Flyway 마이그레이션 충돌
```
Caused by: org.flywaydb.core.api.exception.FlywayValidateException: 
Validate failed: Detected failed migration to version 1.0.0
```

**해결**:
```sql
-- Flyway 메타데이터 삭제
DROP TABLE IF EXISTS flyway_schema_history CASCADE;
```

### 문제 2: code 컬럼이 없다는 에러
```
ERROR: column de1_0.code does not exist
```

**해결**:
1. Flyway 메타데이터 삭제 (위 참조)
2. 애플리케이션 재시작

### 문제 3: UNIQUE 제약 위반
```
ERROR: duplicate key value violates unique constraint "uk_dept_tenant_code"
```

**해결**: `code` 값이 중복되지 않도록 고유한 값 사용

---

## 📚 다음 단계

1. ✅ 애플리케이션 재시작
2. ✅ Swagger UI에서 API 테스트
3. ✅ 조직도 조회 확인
4. ✅ 부서 생성/수정/삭제 테스트
5. ✅ User API 테스트
6. ✅ RBAC API 테스트

---

## 🎉 완료!

모든 준비가 완료되었습니다! 애플리케이션을 재시작하고 API 테스트를 시작하세요! 🚀

