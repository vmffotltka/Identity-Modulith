# 🔧 비밀번호 불일치 문제 - 최종 해결 가이드

## 🚨 현재 상황

### 로그
```
[USER] 현재 비밀번호 불일치 - agentId=10000000-0000-0000-0000-000000000003
```

### 시도한 방법
1. ✅ `/api/dev/hash-password?password=Admin123!` 호출
2. ✅ 생성된 해시로 SQL UPDATE 실행
3. ❌ 여전히 비밀번호 불일치 발생

### 문제 가능성
1. **SQL UPDATE가 실제로 반영되지 않음**
   - 트랜잭션 커밋 안 됨
   - WHERE 조건 불일치
   - 다른 테이블 업데이트

2. **애플리케이션 캐시 문제**
   - JPA 1차 캐시에 이전 데이터 남아있음
   - 애플리케이션 재시작 필요

3. **잘못된 agent_id로 업데이트**
   - UPDATE 쿼리의 agent_id가 다름

---

## ✅ 즉시 해결 절차

### Step 1: 애플리케이션 재시작
```bash
# 기존 프로세스 종료 (Ctrl+C)
# 재시작
.\gradlew bootRun
```

### Step 2: DB 실제 해시 확인 및 검증
```bash
curl "http://localhost:8080/api/dev/check-agent-password?agentId=10000000-0000-0000-0000-000000000003&password=Admin123!"
```

**예상 응답**:
```json
{
  "agentId": "10000000-0000-0000-0000-000000000003",
  "loginId": "dev.member",
  "plainPassword": "Admin123!",
  "dbPasswordHash": "$2a$10$실제DB에저장된해시...",
  "hashLength": 60,
  "matches": true,  // ← 이게 false면 문제!
  "message": "✅ 비밀번호 일치"
}
```

**만약 `matches: false`면**:
```json
{
  ...,
  "matches": false,
  "message": "❌ 비밀번호 불일치",
  "correctHash": "$2a$10$올바른해시...",
  "sqlFix": "UPDATE user_agents SET password = '$2a$10$올바른해시...' WHERE agent_id = '10000000-0000-0000-0000-000000000003';"
}
```

→ `sqlFix`를 복사하여 DB에서 실행

### Step 3: SQL 실행 후 재확인
```bash
# 1. sqlFix 실행 (DB 클라이언트)
UPDATE user_agents SET password = '...' WHERE agent_id = '...';

# 2. 즉시 검증
curl "http://localhost:8080/api/dev/check-agent-password?agentId=10000000-0000-0000-0000-000000000003&password=Admin123!"

# 예상: {"matches": true} ✅
```

### Step 4: 비밀번호 변경 재시도
```bash
curl -X POST "http://localhost:8080/api/v1/agents/me/change-password" \
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

## 🔍 DevController API 활용

### 1. check-agent-password (추가됨!)
```bash
GET /api/dev/check-agent-password?agentId={id}&password={plain}
```

**기능**:
- ✅ DB에서 실제 해시 조회
- ✅ 평문 비밀번호와 비교
- ✅ 불일치 시 올바른 해시 자동 생성
- ✅ 수정 SQL 자동 생성

**장점**:
- DB 조회 → 검증 → 수정 SQL까지 한 번에!
- 실제 저장된 해시 확인 가능
- 트러블슈팅 시간 단축

### 2. hash-password
```bash
GET /api/dev/hash-password?password=Admin123!
```

**기능**:
- 새 BCrypt 해시 생성
- SQL UPDATE 문 생성

### 3. verify-password
```bash
GET /api/dev/verify-password?password=Admin123!&hash=$2a$10$...
```

**기능**:
- 평문과 해시 일치 여부 확인
- 수동으로 복사한 해시 검증 시 사용

---

## 🎯 문제 해결 시나리오

### 시나리오 A: SQL UPDATE 미반영
```bash
# 1. 확인
curl "http://localhost:8080/api/dev/check-agent-password?agentId=10000000-0000-0000-0000-000000000003&password=Admin123!"

# 응답: {"matches": false, "dbPasswordHash": "$2a$10$dummyhash"} ← 여전히 더미!

# 2. sqlFix 복사하여 실행
UPDATE user_agents SET password = '...' WHERE agent_id = '...';
COMMIT;  ← 중요!

# 3. 재확인
curl "http://localhost:8080/api/dev/check-agent-password?..."

# 응답: {"matches": true} ✅
```

### 시나리오 B: 잘못된 agent_id
```bash
# 확인
curl "http://localhost:8080/api/dev/check-agent-password?agentId=10000000-0000-0000-0000-000000000003&password=Admin123!"

# 응답: {"error": "Agent not found"} ← agent_id 확인 필요

# 해결: 정확한 agent_id 확인
SELECT agent_id, login_id FROM user_agents WHERE login_id = 'dev.member';
```

### 시나리오 C: JPA 캐시 문제
```bash
# 1. 애플리케이션 재시작 (필수!)
.\gradlew bootRun

# 2. 재확인
curl "http://localhost:8080/api/dev/check-agent-password?..."
```

---

## 🛠️ 즉시 실행 명령어

### 1단계: 애플리케이션 재시작
```bash
.\gradlew bootRun
```

### 2단계: DB 해시 확인 (브라우저에서 실행)
```
http://localhost:8080/api/dev/check-agent-password?agentId=10000000-0000-0000-0000-000000000003&password=Admin123!
```

**예상 응답 (성공)**:
```json
{
  "agentId": "10000000-0000-0000-0000-000000000003",
  "loginId": "dev.member",
  "plainPassword": "Admin123!",
  "dbPasswordHash": "$2a$10$...",
  "hashLength": 60,
  "matches": true,
  "message": "✅ 비밀번호 일치"
}
```

**예상 응답 (실패 - 수정 필요)**:
```json
{
  "agentId": "10000000-0000-0000-0000-000000000003",
  "loginId": "dev.member",
  "plainPassword": "Admin123!",
  "dbPasswordHash": "$2a$10$dummyhash",
  "hashLength": 17,
  "matches": false,
  "message": "❌ 비밀번호 불일치",
  "correctHash": "$2a$10$새로생성된올바른해시...",
  "sqlFix": "UPDATE user_agents SET password = '$2a$10$새로생성된올바른해시...' WHERE agent_id = '10000000-0000-0000-0000-000000000003';"
}
```

### 3단계: sqlFix 실행 (matches가 false인 경우)
응답의 `sqlFix` 값을 복사하여 DB에서 실행:
```sql
UPDATE user_agents SET password = '$2a$10$...' WHERE agent_id = '10000000-0000-0000-0000-000000000003';
COMMIT;
```

### 4단계: 재확인
```
http://localhost:8080/api/dev/check-agent-password?agentId=10000000-0000-0000-0000-000000000003&password=Admin123!
```

**반드시 확인**: `"matches": true` ✅

### 5단계: 비밀번호 변경 API 테스트
```bash
curl -X POST "http://localhost:8080/api/v1/agents/me/change-password" \
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

## 📊 추가된 기능

### DevController.java
1. **check-agent-password** (신규!)
   - DB의 실제 해시 조회
   - 비밀번호 검증
   - 불일치 시 수정 SQL 자동 생성

2. **hash-password**
   - 새 해시 생성

3. **verify-password**
   - 수동 해시 검증

---

## ✅ 컴파일 확인

```bash
.\gradlew compileJava

BUILD SUCCESSFUL ✅
```

---

## 🎉 최종 체크리스트

### 준비
- [x] DevController 개선
- [x] 컴파일 성공
- [ ] **애플리케이션 재시작** ⚠️

### 실행
- [ ] `/api/dev/check-agent-password` 호출
- [ ] `matches: true` 확인 (false면 sqlFix 실행)
- [ ] 비밀번호 변경 API 호출
- [ ] 204 No Content 확인

---

## 🚀 즉시 실행

1. **애플리케이션 재시작**
2. **브라우저에서 아래 URL 접속**:
   ```
   http://localhost:8080/api/dev/check-agent-password?agentId=10000000-0000-0000-0000-000000000003&password=Admin123!
   ```
3. **응답 확인**:
   - `matches: true` → 비밀번호 변경 API 테스트
   - `matches: false` → `sqlFix` 복사하여 DB에서 실행 후 재확인

이제 **DB의 실제 해시를 직접 확인하고 검증**할 수 있습니다! 🎯

