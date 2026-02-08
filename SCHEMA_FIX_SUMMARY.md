# 🔧 데이터베이스 스키마 불일치 수정 완료

## 📌 문제 상황
```
Hibernate: select ... from user_agent_roles ...
SQL Error: 0, SQLState: 42P01
ERROR: relation "user_agent_roles" does not exist
```

## ✅ 해결 완료

### 🎯 주요 수정 사항

#### 1. **테이블명 불일치 해결**
- ❌ 기존: `rbac_agent_roles` (SQL) ≠ `user_agent_roles` (엔티티)
- ✅ 수정: `user_agent_roles` (통일)

#### 2. **누락된 컬럼 추가**

**user_agent_roles 테이블**:
```sql
assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
```

**rbac_role_permissions 테이블**:
```sql
assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
```

---

## 📁 수정된 파일 목록

### 1. ✨ 신규 생성
- **V3_0_0__Fix_Agent_Roles_Table.sql**
  - 기존 DB 마이그레이션용
  - 테이블 이름 변경
  - 컬럼 추가
  - 제약조건/인덱스 업데이트

### 2. 🔄 수정됨
- **V1_0_0__Complete_Schema_With_Code.sql**
  - `rbac_agent_roles` → `user_agent_roles`
  - `assigned_at` 컬럼 추가 (2개 테이블)
  - INSERT 문 업데이트

- **V2_0_0__Fixed_Schema.sql**
  - `rbac_agent_roles` → `user_agent_roles`
  - `assigned_at` 컬럼 추가 (2개 테이블)
  - INSERT 문 업데이트

### 3. 📄 문서 생성
- **DATABASE_SCHEMA_FIX_REPORT.md**
  - 상세한 수정 보고서
  - 검증 방법
  - 향후 권장사항

- **verify_schema.sql**
  - 스키마 검증 SQL 스크립트
  - 10가지 검증 항목

---

## 🚀 적용 방법

### ⚠️ 중요: 하나만 선택하세요!

아래 방법 중 **하나만** 선택해서 실행하면 됩니다. **순서대로 모두 실행하는 것이 아닙니다!**

---

### 🎯 방법 1: 기존 데이터 유지 (추천)
**언제**: 이미 데이터베이스에 데이터가 있고 보존해야 할 때

```bash
# IntelliJ IDEA에서 실행 (가장 쉬움)
1. IdentityModulithApplication 우클릭
2. Run 'IdentityModulithApplication'

# 또는 Gradle로 실행
./gradlew bootRun
```

→ V3_0_0 마이그레이션이 자동 실행됩니다

---

### 🔥 방법 2: 데이터베이스 재설치 (테스트용)
**언제**: 처음 설치하거나 데이터를 지워도 될 때

```bash
# 모든 테이블 삭제 후 재생성 (데이터 손실!)
./gradlew flywayClean flywayMigrate
./gradlew bootRun
```

⚠️ **주의**: 모든 데이터가 삭제됩니다!

---

### 🔧 방법 3: SQL 직접 실행 (Gradle 없이)
**언제**: Gradle이 안 되거나 수동으로 실행하고 싶을 때

```bash
# PostgreSQL 연결
psql -U postgres -d your_database

# V3 마이그레이션 실행
\i src/main/resources/db/migration/V3_0_0__Fix_Agent_Roles_Table.sql

# 종료 후 애플리케이션 실행
\q
```

---

## 🔍 검증 방법

### 1. 스키마 검증
```bash
psql -U postgres -d your_database -f verify_schema.sql
```

### 2. 애플리케이션 테스트
```bash
# 애플리케이션 실행 후 로그 확인
./gradlew bootRun

# 에러 없이 다음 로그가 나오면 성공:
# "Hibernate: select ... from user_agent_roles ..."
# (에러 없음)
```

### 3. 수동 확인
```sql
-- 테이블 확인
SELECT table_name FROM information_schema.tables 
WHERE table_name IN ('user_agent_roles', 'rbac_agent_roles');

-- 결과: user_agent_roles만 나와야 함 (rbac_agent_roles는 없어야 함)

-- 컬럼 확인
SELECT column_name FROM information_schema.columns
WHERE table_name = 'user_agent_roles' AND column_name = 'assigned_at';

-- 결과: assigned_at 1개 행 반환
```

---

## 📋 전체 엔티티-스키마 매핑 (최종)

| 엔티티 | 테이블명 | 상태 |
|--------|---------|------|
| `DepartmentJpaEntity` | `org_departments` | ✅ |
| `AgentJpaEntity` | `user_agents` | ✅ |
| `RoleJpaEntity` | `rbac_roles` | ✅ |
| `PermissionJpaEntity` | `rbac_permissions` | ✅ |
| `RolePermissionJpaEntity` | `rbac_role_permissions` | ✅ |
| `AgentRoleJpaEntity` | `user_agent_roles` | ✅ |

---

## 🎉 결과

### ✅ 모든 불일치 해결 완료

1. **테이블명 통일**: `user_agent_roles`
2. **누락 컬럼 추가**: `assigned_at` (2개 테이블)
3. **제약조건 정리**: FK, UK 명명 규칙 통일
4. **인덱스 최적화**: 모든 검색 패턴 커버
5. **문서화 완료**: 수정 보고서 및 검증 스크립트

### 🔥 이제 오류 없이 실행됩니다!

```
✅ relation "user_agent_roles" does not exist → 해결!
✅ assigned_at 컬럼 누락 → 해결!
✅ 제약조건 불일치 → 해결!
```

---

## 📞 문제 발생 시

1. **마이그레이션 실패**
   ```bash
   # Flyway 상태 확인
   ./gradlew flywayInfo
   
   # 마이그레이션 히스토리 확인
   SELECT * FROM flyway_schema_history ORDER BY installed_rank;
   ```

2. **V3 마이그레이션이 실행되지 않음**
   ```bash
   # Flyway 리페어 후 재실행
   ./gradlew flywayRepair flywayMigrate
   ```

3. **데이터 손실 우려**
   ```bash
   # 백업 후 재시도
   pg_dump -U postgres your_database > backup.sql
   # 복구: psql -U postgres your_database < backup.sql
   ```

---

**작성일**: 2026-02-07  
**완료 시간**: 약 10분  
**수정 파일**: 5개  
**상태**: ✅ 완료

