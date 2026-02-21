# Keycloak 연동 설정 가이드 (실제 서버용)

## 🎯 **현재 상황**
- ✅ Keycloak 서버: `http://1.224.162.188:51446`
- ✅ Admin 계정: `admin` / `nexfron11!`
- ✅ Identity Modulith 코드 수정 완료

---

## 📋 **Step 1: Keycloak 설정 (5분)**

### 1️⃣ **Keycloak Admin Console 접속**
```
URL: http://1.224.162.188:51446/admin/master/console/
Username: admin
Password: nexfron11!
```

### 2️⃣ **Realm 생성**
1. 좌측 상단 드롭다운 클릭
2. **"Create Realm"** 선택
3. **Realm name**: `identity-system`
4. **Enabled**: ON
5. **Create** 버튼 클릭

### 3️⃣ **Client 생성**
1. 좌측 메뉴 **"Clients"** 클릭
2. **"Create client"** 버튼 클릭
3. **General Settings**:
   - Client type: `OpenID Connect`
   - Client ID: `identity-modulith`
   - **Next** 클릭
4. **Capability config**:
   - Client authentication: **ON** (Confidential로 설정)
   - Authorization: **OFF**
   - Authentication flow:
     - ✅ Standard flow (체크)
     - ✅ Direct access grants (체크)
   - **Next** 클릭
5. **Login settings**:
   - Valid redirect URIs: `http://localhost:8080/*`, `http://1.224.162.188:8080/*`
   - Valid post logout redirect URIs: `http://localhost:8080/*`, `http://1.224.162.188:8080/*`
   - Web origins: `http://localhost:8080`, `http://1.224.162.188:8080`
   - **Save** 클릭

### 4️⃣ **Client Secret 확인 및 복사** ⚠️ **중요!**
1. 생성한 `identity-modulith` 클라이언트 선택
2. **"Credentials"** 탭 클릭
3. **Client secret** 값 복사 (예: `abc123xyz456...`)
4. 복사한 값을 메모장에 저장 (다음 단계에서 사용)

### 5️⃣ **역할 생성**
1. 좌측 메뉴 **"Realm roles"** 클릭
2. **"Create role"** 버튼 클릭
3. 다음 역할들을 하나씩 생성:
   - **Role name**: `ADMIN` → **Save**
   - **Role name**: `TEAM_LEAD` → **Save**
   - **Role name**: `MEMBER` → **Save**

### 6️⃣ **테스트 사용자 생성**
1. 좌측 메뉴 **"Users"** 클릭
2. **"Add user"** 버튼 클릭
3. 사용자 정보 입력:
   - **Username**: `test.admin`
   - **Email**: `admin@example.com`
   - **First name**: `Admin`
   - **Last name**: `User`
   - **Email verified**: ON
   - **Enabled**: ON
4. **Create** 클릭

### 7️⃣ **비밀번호 설정**
1. 생성한 사용자 (`test.admin`) 선택
2. **"Credentials"** 탭 클릭
3. **"Set password"** 버튼 클릭
4. **Password**: `password123`
5. **Password confirmation**: `password123`
6. **Temporary**: **OFF** (중요! OFF로 설정)
7. **Save** 클릭
8. 확인 팝업에서 **"Save password"** 클릭

### 8️⃣ **역할 할당**
1. 사용자 (`test.admin`) 선택
2. **"Role mapping"** 탭 클릭
3. **"Assign role"** 버튼 클릭
4. **Filter by clients** 드롭다운에서 **"Filter by realm roles"** 선택
5. `ADMIN` 역할 체크
6. **Assign** 버튼 클릭

---

## 📋 **Step 2: application.yml 설정 수정**

### ⚠️ **중요: Client Secret 설정**

파일: `src/main/resources/application.yml`

**수정할 부분**:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-secret: CHANGE_ME  # ⚠️ 여기를 변경!
```

**변경 후** (Step 1-4에서 복사한 값 사용):
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-secret: abc123xyz456...  # ⚠️ 복사한 실제 값으로 변경
```

---

## 📋 **Step 3: 애플리케이션 빌드 및 실행**

### 1️⃣ **빌드**
```powershell
cd "C:\Users\vmffo\Desktop\회사 자료\프로젝트 폴더\identity-modulith"
.\gradlew build -x test
```

### 2️⃣ **실행**
```powershell
.\gradlew bootRun
```

또는

```powershell
java -jar build\libs\identity-modulith-0.0.1-SNAPSHOT.jar
```

### 3️⃣ **실행 확인**
```
Starting IdentityModulithApplication...
...
Started IdentityModulithApplication in 15.234 seconds
```

---

## 📋 **Step 4: 테스트**

### ✅ **테스트 1: 로그인 (JWT 토큰 발급)**

**PowerShell**:
```powershell
$body = @{
    username = "test.admin"
    password = "password123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body

$response
```

**예상 응답**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "token_type": "Bearer",
  "not-before-policy": 0,
  "session_state": "...",
  "scope": "openid profile email"
}
```

### ✅ **테스트 2: JWT로 API 호출**

**PowerShell**:
```powershell
# 위에서 받은 access_token 사용
$token = "eyJhbGciOiJSUzI1NiIsInR5cCI..."

$headers = @{
    "Authorization" = "Bearer $token"
}

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/rbac/roles" `
    -Method GET `
    -Headers $headers

$response
```

**예상 응답**:
```json
[
  {
    "roleId": "...",
    "name": "ADMIN",
    "type": "POSITION",
    ...
  }
]
```

---

## 🎯 **완료 체크리스트**

### Keycloak 설정
- [ ] Realm `identity-system` 생성됨
- [ ] Client `identity-modulith` 생성됨
- [ ] Client Secret 복사함
- [ ] 역할 3개 생성됨 (ADMIN, TEAM_LEAD, MEMBER)
- [ ] 테스트 사용자 `test.admin` 생성됨
- [ ] 비밀번호 `password123` 설정됨 (Temporary: OFF)
- [ ] `ADMIN` 역할 할당됨

### Identity Modulith 설정
- [ ] application.yml에 Client Secret 설정함
- [ ] 빌드 성공 (`.\gradlew build -x test`)
- [ ] 애플리케이션 실행 성공

### 테스트
- [ ] 로그인 성공 (JWT 토큰 발급)
- [ ] JWT로 API 호출 성공

---

## 🐛 **문제 해결**

### 1. 로그인 실패 (401 Unauthorized)
**원인**: Keycloak 사용자 설정 오류
**해결**:
1. Keycloak에서 `test.admin` 사용자 확인
2. **Credentials** 탭에서 비밀번호 재설정
3. **Temporary**: **OFF** 확인
4. **Role mapping** 탭에서 `ADMIN` 역할 확인

### 2. JWT 검증 실패
**원인**: `issuer-uri` 불일치
**해결**:
```yaml
# application.yml 확인
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://1.224.162.188:51446/realms/identity-system  # ⚠️ 정확히 일치해야 함
```

### 3. Client Secret 오류
**원인**: Client Secret 잘못 복사
**해결**:
1. Keycloak → Clients → `identity-modulith` → Credentials 탭
2. Client secret 다시 복사
3. application.yml 업데이트
4. 애플리케이션 재시작

### 4. CORS 오류
**원인**: Web origins 미설정
**해결**:
1. Keycloak → Clients → `identity-modulith` → Settings 탭
2. **Web origins**: `http://localhost:8080`, `http://1.224.162.188:8080` 추가
3. **Save**

---

## 📝 **다음 단계**

### ✅ **완료한 것**
- [x] Keycloak 서버 준비
- [x] Realm, Client, 사용자 생성
- [x] Identity Modulith 코드 수정
- [x] JWT 인증 기본 동작 확인

### 🚧 **다음에 할 것** (선택사항)
- [ ] 커스텀 권한 검증 (PermissionEvaluator)
- [ ] RBAC 동기화 (KeycloakSyncService)
- [ ] 기존 X-User-Id → JWT 전환
- [ ] 프로덕션 환경 설정 (HTTPS, 토큰 만료 시간 등)

---

## 📚 **참고 문서**

- [KEYCLOAK_INTEGRATION_GUIDE.md](./KEYCLOAK_INTEGRATION_GUIDE.md) - 전체 가이드
- [KEYCLOAK_QUICK_START.md](./KEYCLOAK_QUICK_START.md) - 빠른 시작

---

**작성일**: 2026-02-22  
**서버**: http://1.224.162.188:51446  
**상태**: 실행 중 ✅

