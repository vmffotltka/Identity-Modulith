# 🎯 실행 가이드 (간단 버전)

## ❓ 어떤 방법을 선택해야 하나요?

### 상황별 선택 가이드

```
┌─────────────────────────────────────────────────┐
│ 현재 상황이 어디에 해당하나요?                    │
└─────────────────────────────────────────────────┘

📌 케이스 1: 이미 실행 중인 데이터베이스가 있고, 데이터를 유지해야 함
   → 방법 1 선택 ✅
   
📌 케이스 2: 데이터베이스를 처음 만들거나, 데이터를 지워도 됨
   → 방법 2 선택 ✅
   
📌 케이스 3: Gradle이 안 되거나, 직접 SQL을 실행하고 싶음
   → 방법 3 선택 ✅
```

---

## 🚀 실행 방법

### 🔥 **가장 추천**: 방법 2 (깨끗한 재설치)

테스트 중이시고 데이터가 중요하지 않다면 이 방법이 가장 확실합니다:

```bash
# 1단계: 데이터베이스 완전 초기화
cd C:\Project\identity-modulith-master
./gradlew flywayClean flywayMigrate

# 2단계: 애플리케이션 실행
./gradlew bootRun
```

**주의**: `flywayClean`은 모든 테이블을 삭제하므로 **데이터가 모두 사라집니다!**

---

### 📝 방법 1: 기존 데이터 유지 (마이그레이션)

이미 운영 중인 데이터베이스가 있고 데이터를 보존해야 한다면:

```bash
cd C:\Project\identity-modulith-master
./gradlew bootRun
```

- V3_0_0 마이그레이션이 자동 실행됩니다
- 기존 데이터는 그대로 유지됩니다
- `rbac_agent_roles` → `user_agent_roles`로 변경됩니다

---

### 🔧 방법 3: SQL 직접 실행

Gradle 없이 PostgreSQL에서 직접 실행:

```bash
# PostgreSQL 접속
psql -U postgres -d your_database_name

# V3 마이그레이션 실행
\i src/main/resources/db/migration/V3_0_0__Fix_Agent_Roles_Table.sql

# 종료
\q

# 애플리케이션 실행 (Gradle 없이)
java -jar build/libs/your-app.jar
```

---

## ⚠️ Java 버전 문제 해결

현재 시스템에 Java 8이 설치되어 있는데, Gradle 9.2.1은 Java 17 이상이 필요합니다.

### 해결 방법:

#### A. IntelliJ에서 실행 (가장 쉬움)
```
1. IntelliJ IDEA 열기
2. IdentityModulithApplication 클래스 찾기
3. 우클릭 → Run 'IdentityModulithApplication'
```

#### B. Java 17+ 설치 후 실행
```powershell
# Java 17 다운로드 및 설치
# https://adoptium.net/temurin/releases/

# 설치 후
cd C:\Project\identity-modulith-master
./gradlew bootRun
```

#### C. application.yml에서 Flyway 수동 실행
```yaml
# src/main/resources/application.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
```

그리고 IntelliJ에서 애플리케이션 실행하면 자동으로 마이그레이션됩니다.

---

## ✅ 실행 후 확인

애플리케이션 실행 후 로그에서 다음을 확인:

```
✅ 성공 로그:
INFO  --- Flyway migration complete
INFO  --- Hibernate: select ... from user_agent_roles ...
(에러 없음)

❌ 실패 로그:
ERROR --- relation "user_agent_roles" does not exist
```

---

## 🎯 **지금 당장 실행하려면?**

### 추천 순서:

```bash
# 방법 1: IntelliJ에서 실행 (가장 쉬움)
1. IntelliJ IDEA 실행
2. IdentityModulithApplication 우클릭 → Run

# 방법 2: PostgreSQL 직접 접속
1. psql -U postgres -d identity_db
2. \i C:/Project/identity-modulith-master/src/main/resources/db/migration/V3_0_0__Fix_Agent_Roles_Table.sql
3. \q
4. IntelliJ에서 애플리케이션 실행

# 방법 3: Gradle (Java 17+ 필요)
./gradlew bootRun
```

---

## 💡 요약

| 방법 | 언제 사용? | 명령어 | 데이터 보존 |
|------|-----------|--------|------------|
| **방법 1** | 데이터 유지 | `./gradlew bootRun` | ✅ 보존 |
| **방법 2** | 처음 설치 | `./gradlew flywayClean flywayMigrate` | ❌ 삭제 |
| **방법 3** | Gradle 안됨 | `psql ... \i V3...sql` | ✅ 보존 |
| **IntelliJ** | 가장 쉬움 | Run 버튼 | ✅ 보존 |

---

**질문에 대한 답변**:
> 1, 2, 3 순서대로 실행하면 돼?

❌ **아니요!** 하나만 선택해서 실행하세요.

✅ **추천**: IntelliJ에서 Run 버튼 클릭 (가장 쉽고 확실함)

