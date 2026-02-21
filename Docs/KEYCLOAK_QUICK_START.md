# Keycloak 연동 Quick Start

> 30분 안에 Keycloak 연동 시작하기

## 🚀 빠른 시작 (3단계)

### Step 1: Keycloak 실행 (5분)

```bash
# 1. docker-compose.yml 다운로드 또는 복사
# (KEYCLOAK_INTEGRATION_GUIDE.md의 docker-compose.yml 사용)

# 2. Docker Compose 실행
docker-compose up -d

# 3. Keycloak 접속 확인
# URL: http://localhost:8180
# ID/PW: admin / admin123
```

---

### Step 2: Realm 및 Client 생성 (10분)

#### 1️⃣ **Realm 생성**
1. Keycloak Admin Console 접속: http://localhost:8180
2. 좌측 상단 드롭다운 → **"Create Realm"**
3. Realm name: `identity-system`
4. **Create**

#### 2️⃣ **Client 생성**
1. 좌측 메뉴 **"Clients"** → **"Create client"**
2. Client ID: `identity-modulith`
3. Client authentication: **ON**
4. Valid redirect URIs: `http://localhost:8080/*`
5. **Save**
6. **"Credentials"** 탭에서 **Client secret** 복사

#### 3️⃣ **테스트 사용자 생성**
1. 좌측 메뉴 **"Users"** → **"Add user"**
2. Username: `test.admin`
3. **Save**
4. **"Credentials"** 탭 → Password: `password123` → Temporary: **OFF**
5. **"Role mapping"** 탭 → **"Assign role"** → `ADMIN` 선택

---

### Step 3: Spring Boot 설정 (15분)

#### 1️⃣ **build.gradle에 의존성 추가**

```gradle
dependencies {
    // OAuth 2.0 & Keycloak
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    implementation 'org.keycloak:keycloak-spring-boot-starter:23.0.7'
}
```

#### 2️⃣ **application.yml 설정**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/identity-system
      client:
        registration:
          keycloak:
            client-id: identity-modulith
            client-secret: YOUR_CLIENT_SECRET_HERE  # 복사한 값
            authorization-grant-type: authorization_code
            scope: openid, profile, email
        provider:
          keycloak:
            issuer-uri: http://localhost:8180/realms/identity-system
```

#### 3️⃣ **SecurityConfig 생성**

`src/main/java/com/nexfron/identitymodulith/common/security/KeycloakSecurityConfig.java`:

```java
@Configuration
@EnableWebSecurity
public class KeycloakSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        
        return http.build();
    }
}
```

#### 4️⃣ **빌드 및 실행**

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

---

## ✅ 테스트

### 1. 로그인 (토큰 발급)

```bash
curl -X POST 'http://localhost:8180/realms/identity-system/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=identity-modulith' \
  -d 'client_secret=YOUR_CLIENT_SECRET' \
  -d 'username=test.admin' \
  -d 'password=password123'
```

**응답**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "token_type": "Bearer"
}
```

### 2. API 호출 (JWT 사용)

```bash
curl -X GET 'http://localhost:8080/api/rbac/roles' \
  -H 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI...'
```

**성공 응답**: 역할 목록 반환

---

## 🎯 다음 단계

### ✅ **완료한 것**
- [x] Keycloak 설치 및 실행
- [x] Realm, Client, 사용자 생성
- [x] Spring Boot 기본 연동
- [x] JWT 인증 동작 확인

### 🚧 **다음에 할 것**
- [ ] 로그인 API 구현 (AuthController)
- [ ] 커스텀 권한 검증 (PermissionEvaluator)
- [ ] RBAC 동기화 (KeycloakSyncService)
- [ ] 기존 X-User-Id → JWT 전환

👉 **상세 가이드**: [KEYCLOAK_INTEGRATION_GUIDE.md](./KEYCLOAK_INTEGRATION_GUIDE.md)

---

## 🐛 문제 해결

### Keycloak 접속 안 됨
```bash
# Keycloak 로그 확인
docker logs -f identity-keycloak

# 재시작
docker-compose restart keycloak
```

### JWT 검증 실패
- `issuer-uri` 확인: http://localhost:8180/realms/identity-system
- `client-secret` 확인: Keycloak에서 다시 복사

### 포트 충돌
```yaml
# docker-compose.yml 수정
ports:
  - "8181:8080"  # Keycloak 포트 변경
```

---

**소요 시간**: 약 30분  
**난이도**: ⭐⭐ (중)

