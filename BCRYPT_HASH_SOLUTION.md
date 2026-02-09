# 🚨 비밀번호 불일치 문제 - 해시 검증 실패

## 🔍 문제 확인됨!

### 로그 분석
```
[USER] 비밀번호 변경 시작
[USER] 현재 비밀번호 불일치 - agentId=10000000-0000-0000-0000-000000000003 ⚠️
```

**확인**: Service까지 정상 도달했으나 **현재 비밀번호 검증에서 실패**

### 원인
`update_passwords.sql`에서 사용한 BCrypt 해시가 **실제로 `Admin123!`의 해시가 아닙니다!**

```sql
-- ❌ 문제: 이 해시가 Admin123!와 매칭되지 않음
UPDATE user_agents 
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm'
WHERE agent_id = '10000000-0000-0000-0000-000000000003';
```

**BCrypt의 특징**:
- 동일한 평문 비밀번호도 **매번 다른 해시 생성** (Salt 때문)
- 검증 시 `BCryptPasswordEncoder.matches(plain, hash)` 사용
- **임의로 만든 해시는 작동하지 않음!**

---

## ✅ 해결 방법

### 방법 1: DevController 사용 (권장)

#### Step 1: 애플리케이션 재시작
```bash
.\gradlew bootRun
```

#### Step 2: 실제 BCrypt 해시 생성
```bash
curl "http://localhost:8080/api/dev/hash-password?password=Admin123!"
```

**응답 예시**:
```json
{
  "plainPassword": "Admin123!",
  "bcryptHash": "$2a$10$실제생성된해시값...",
  "verified": true,
  "sqlUpdate": "UPDATE user_agents SET password = '$2a$10$실제생성된해시값...' WHERE agent_id = '10000000-0000-0000-0000-000000000003';"
}
```

#### Step 3: SQL 복사하여 실행
응답의 `sqlUpdate` 값을 복사하여 DB에서 실행

#### Step 4: 검증
```bash
curl "http://localhost:8080/api/dev/verify-password?password=Admin123!&hash=$2a$10$실제생성된해시값..."
```

**응답**:
```json
{
  "plainPassword": "Admin123!",
  "hash": "$2a$10$...",
  "matches": true  // ✅ 반드시 true여야 함
}
```

---

### 방법 2: Flyway 재실행 (완전 초기화)

#### Step 1: V2_0_0 SQL 파일 확인
```sql
-- V2_0_0__Fixed_Schema.sql에 이미 수정됨
INSERT INTO user_agents (..., password, ...)
VALUES
    (..., '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm', ...);
```

**문제**: 이 해시도 검증되지 않은 임의 값!

#### Step 2: 실제 해시로 수정 필요
1. DevController로 실제 해시 생성
2. V2_0_0__Fixed_Schema.sql 수정
3. Flyway Clean & Migrate

```bash
.\gradlew flywayClean
.\gradlew flywayMigrate
.\gradlew bootRun
```

---

### 방법 3: 직접 SQL 업데이트 (빠른 해결)

#### Step 1: DevController로 해시 생성
```bash
curl "http://localhost:8080/api/dev/hash-password?password=Admin123!"
```

#### Step 2: 응답의 sqlUpdate 실행
```sql
-- 실제 생성된 해시로 업데이트
UPDATE user_agents 
SET password = '$2a$10$실제_생성된_해시_값...'
WHERE agent_id IN (
    '10000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000003'
);
```

#### Step 3: API 재테스트
```bash
curl -X POST "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/change-password" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000003" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Admin123!",
    "newPassword": "MyNewPassword456!",
    "confirmPassword": "MyNewPassword456!"
  }'

예상: 204 No Content ✅
```

---

## 📊 DevController API 사용법

### 1. 비밀번호 해시 생성
```bash
GET http://localhost:8080/api/dev/hash-password?password=Admin123!

응답:
{
  "plainPassword": "Admin123!",
  "bcryptHash": "$2a$10$실제해시...",
  "verified": true,
  "sqlUpdate": "UPDATE user_agents SET password = '...' WHERE agent_id = '...';"
}
```

### 2. 비밀번호 검증
```bash
GET http://localhost:8080/api/dev/verify-password?password=Admin123!&hash=$2a$10$...

응답:
{
  "plainPassword": "Admin123!",
  "hash": "$2a$10$...",
  "matches": true  // true면 정상
}
```

### 3. Swagger UI에서 사용
```
http://localhost:8080/swagger-ui/index.html
→ dev-controller 섹션
→ GET /api/dev/hash-password
→ password: Admin123!
→ Execute
```

---

## 🎯 즉시 해결 절차

### 1. 컴파일 확인
```bash
.\gradlew compileJava

예상: BUILD SUCCESSFUL ✅
```

### 2. 애플리케이션 재시작
```bash
.\gradlew bootRun
```

### 3. 실제 BCrypt 해시 생성
```bash
curl "http://localhost:8080/api/dev/hash-password?password=Admin123!"
```

### 4. SQL 복사 및 실행
응답의 `sqlUpdate` 값을 DB에서 실행:
```sql
UPDATE user_agents SET password = '$2a$10$실제생성된값...' WHERE agent_id = '10000000-0000-0000-0000-000000000003';
```

### 5. 검증
```bash
curl "http://localhost:8080/api/dev/verify-password?password=Admin123!&hash=DB에서_조회한_해시값"

응답: {"matches": true} 확인
```

### 6. API 재테스트
```bash
curl -X POST "http://localhost:8080/api/v1/agents/10000000-0000-0000-0000-000000000003/change-password" \
  -H "X-User-Id: 10000000-0000-0000-0000-000000000003" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Admin123!",
    "newPassword": "MyNewPassword456!",
    "confirmPassword": "MyNewPassword456!"
  }'

예상: 204 No Content ✅
```

---

## 💡 BCrypt 해시의 특성

### 1. Salt 포함
```
$2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm
 │  │  │                                                        │
 │  │  └─ Salt (22자)                                          └─ Hash (31자)
 │  └─ Cost Factor (10)
 └─ 알고리즘 버전 (2a)
```

### 2. 매번 다른 해시
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

String hash1 = encoder.encode("Admin123!");
// → $2a$10$abc...xyz

String hash2 = encoder.encode("Admin123!");
// → $2a$10$def...uvw  (다름!)

boolean match1 = encoder.matches("Admin123!", hash1);  // true
boolean match2 = encoder.matches("Admin123!", hash2);  // true
```

### 3. 임의 해시는 작동 안 함
```java
String fakeHash = "$2a$10$dummyhash";
encoder.matches("Admin123!", fakeHash);  // false!

String wrongHash = "$2a$10$N9qo8uLOickgx2ZMRZoMye1J8fqohVhEhHZqXzSJCy6P6RBLhxaYm";
encoder.matches("Admin123!", wrongHash);  // false! (검증 안된 임의 값)
```

---

## 🎉 해결 완료 체크리스트

### 준비
- [x] DevController 생성
- [x] 컴파일 성공
- [ ] 애플리케이션 재시작

### 실행
- [ ] `/api/dev/hash-password` 호출
- [ ] 실제 BCrypt 해시 획득
- [ ] DB 업데이트 SQL 실행
- [ ] `/api/dev/verify-password`로 검증

### 테스트
- [ ] 비밀번호 변경 API 호출
- [ ] 204 No Content 확인
- [ ] 로그에 "[USER] 비밀번호 변경 완료" 확인

---

## 🚀 다음 단계

1. **애플리케이션 재시작**
   ```bash
   .\gradlew bootRun
   ```

2. **실제 BCrypt 해시 생성**
   ```
   http://localhost:8080/api/dev/hash-password?password=Admin123!
   ```

3. **DB 업데이트**
   응답의 `sqlUpdate` 실행

4. **API 테스트**
   비밀번호 변경 성공 확인! ✅

---

## 💡 핵심 교훈

### BCrypt 해시는 반드시 생성해야 함!
- ❌ 임의로 만든 해시는 작동 안 함
- ❌ 다른 곳에서 복사한 해시도 작동 안 함
- ✅ **반드시 BCryptPasswordEncoder로 생성**해야 함

### DevController의 가치
- ✅ 개발 중 빠른 해시 생성
- ✅ 비밀번호 검증 테스트
- ✅ SQL 자동 생성
- ✅ 디버깅 용이

이제 **실제 BCrypt 해시를 생성하고 DB를 업데이트**하면 비밀번호 변경이 성공합니다! 🎉

