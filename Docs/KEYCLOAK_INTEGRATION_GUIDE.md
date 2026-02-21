# Keycloak 연동 가이드

## 📋 목차
1. [개요](#개요)
2. [Keycloak 설치 및 설정](#keycloak-설치-및-설정)
3. [Spring Boot 연동](#spring-boot-연동)
4. [RBAC 동기화 전략](#rbac-동기화-전략)
5. [인증/인가 구현](#인증인가-구현)
6. [테스트 시나리오](#테스트-시나리오)
7. [운영 가이드](#운영-가이드)

---

## 개요

### 🎯 **목표**
Identity Modulith의 RBAC 시스템을 **Keycloak**과 연동하여:
- ✅ SSO (Single Sign-On) 구현
- ✅ OAuth 2.0 / OpenID Connect 인증
- ✅ JWT 기반 토큰 인증
- ✅ 역할/권한 동기화

### 🏗️ **아키텍처**

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│   Frontend      │ ───▶ │   Keycloak       │ ───▶ │ Identity        │
│   (React/Vue)   │      │   (인증 서버)     │      │ Modulith        │
│                 │      │                  │      │ (리소스 서버)    │
└─────────────────┘      └──────────────────┘      └─────────────────┘
        │                        │                         │
        │  1. 로그인 요청         │                         │
        │ ────────────────────▶  │                         │
        │                        │                         │
        │  2. JWT 토큰 발급      │                         │
        │ ◀────────────────────  │                         │
        │                        │                         │
        │  3. API 요청 (JWT)                              │
        │ ────────────────────────────────────────────────▶│
        │                        │                         │
        │                        │  4. JWT 검증            │
        │                        │ ◀───────────────────────│
        │                        │                         │
        │                        │  5. 권한 확인 (Local)   │
        │                        │         (RBAC)          │
        │                        │                         │
        │  6. 응답                                         │
        │ ◀────────────────────────────────────────────────│
```

---

## Keycloak 설치 및 설정

### Step 1: Keycloak 설치 (Docker)

#### 📝 **docker-compose.yml 생성**

프로젝트 루트에 `docker-compose.yml` 파일 생성:

```yaml
version: '3.8'

services:
  keycloak:
    image: quay.io/keycloak/keycloak:23.0.7
    container_name: identity-keycloak
    environment:
      # 관리자 계정
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin123
      
      # 데이터베이스 설정
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: keycloak123
      
      # HTTP 설정 (개발 환경)
      KC_HTTP_ENABLED: true
      KC_HOSTNAME_STRICT: false
      KC_HOSTNAME_STRICT_HTTPS: false
      
    ports:
      - "8180:8080"  # Keycloak Admin Console
    command:
      - start-dev
    depends_on:
      - postgres
    networks:
      - identity-network

  postgres:
    image: postgres:16
    container_name: identity-postgres
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres123
      # 멀티 데이터베이스 생성
      POSTGRES_MULTIPLE_DATABASES: nexfron,keycloak
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./init-multi-db.sh:/docker-entrypoint-initdb.d/init-multi-db.sh
    ports:
      - "5432:5432"
    networks:
      - identity-network

volumes:
  postgres-data:

networks:
  identity-network:
    driver: bridge
```

#### 📝 **init-multi-db.sh 생성**

```bash
#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE nexfron;
    CREATE DATABASE keycloak;
    
    -- nexfron DB 사용자
    CREATE USER nexfron WITH PASSWORD 'nexfron123';
    GRANT ALL PRIVILEGES ON DATABASE nexfron TO nexfron;
    
    -- keycloak DB 사용자
    CREATE USER keycloak WITH PASSWORD 'keycloak123';
    GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;
EOSQL
```

#### 🚀 **실행**

```bash
# 1. 실행 권한 부여 (Git Bash)
chmod +x init-multi-db.sh

# 2. Docker Compose 실행
docker-compose up -d

# 3. Keycloak 시작 확인 (약 30초 소요)
docker logs -f identity-keycloak

# 4. 접속 확인
# Keycloak Admin: http://localhost:8180
# PostgreSQL: localhost:5432
```

---

### Step 2: Realm 및 Client 생성

#### 1️⃣ **Keycloak Admin Console 접속**
- URL: http://localhost:8180
- Username: `admin`
- Password: `admin123`

#### 2️⃣ **Realm 생성**
1. 왼쪽 상단 드롭다운에서 **"Create Realm"** 클릭
2. **Realm name**: `identity-system`
3. **Enabled**: ON
4. **Create** 버튼 클릭

#### 3️⃣ **Client 생성**
1. 좌측 메뉴 **"Clients"** → **"Create client"** 클릭
2. **General Settings**:
   - Client type: `OpenID Connect`
   - Client ID: `identity-modulith`
3. **Capability config**:
   - Client authentication: `ON` (Confidential)
   - Authorization: `OFF`
   - Authentication flow:
     - ✅ Standard flow
     - ✅ Direct access grants
4. **Login settings**:
   - Valid redirect URIs: `http://localhost:8080/*`
   - Valid post logout redirect URIs: `http://localhost:8080/*`
   - Web origins: `http://localhost:8080`
5. **Save** 클릭

#### 4️⃣ **Client Secret 확인**
1. `identity-modulith` 클라이언트 선택
2. **"Credentials"** 탭 클릭
3. **Client secret** 복사 (나중에 사용)

---

### Step 3: 역할 및 사용자 생성

#### 1️⃣ **역할 생성**
1. 좌측 메뉴 **"Realm roles"** → **"Create role"** 클릭
2. 다음 역할 생성:
   - `ADMIN`
   - `TEAM_LEAD`
   - `MEMBER`

#### 2️⃣ **테스트 사용자 생성**
1. 좌측 메뉴 **"Users"** → **"Add user"** 클릭
2. **Username**: `test.admin`
3. **Email**: `admin@example.com`
4. **First name**: `Admin`
5. **Last name**: `User`
6. **Save** 클릭

#### 3️⃣ **비밀번호 설정**
1. 생성한 사용자 선택 → **"Credentials"** 탭
2. **"Set password"** 클릭
3. **Password**: `password123`
4. **Temporary**: `OFF`
5. **Save** 클릭

#### 4️⃣ **역할 할당**
1. 사용자 선택 → **"Role mapping"** 탭
2. **"Assign role"** 클릭
3. `ADMIN` 역할 선택
4. **Assign** 클릭

---

## Spring Boot 연동

### Step 1: 의존성 추가

#### 📝 **build.gradle 수정**

```gradle
dependencies {
    // 기존 dependencies...
    
    // ========== Keycloak & OAuth 2.0 ==========
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    implementation 'org.keycloak:keycloak-spring-boot-starter:23.0.7'
    
    // JWT 처리
    implementation 'org.springframework.security:spring-security-oauth2-jose'
}
```

---

### Step 2: application.yml 설정

#### 📝 **src/main/resources/application.yml**

```yaml
spring:
  application:
    name: identity-modulith
    
  # 기존 datasource 설정...
  
  # ========== Keycloak & OAuth 2.0 설정 ==========
  security:
    oauth2:
      resourceserver:
        jwt:
          # Keycloak의 공개 키 엔드포인트
          issuer-uri: http://localhost:8180/realms/identity-system
          jwk-set-uri: http://localhost:8180/realms/identity-system/protocol/openid-connect/certs
      
      client:
        registration:
          keycloak:
            client-id: identity-modulith
            client-secret: YOUR_CLIENT_SECRET_HERE  # Keycloak에서 복사한 값
            authorization-grant-type: authorization_code
            scope: openid, profile, email
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        
        provider:
          keycloak:
            issuer-uri: http://localhost:8180/realms/identity-system
            user-name-attribute: preferred_username

# Keycloak 연동 설정
keycloak:
  realm: identity-system
  auth-server-url: http://localhost:8180
  resource: identity-modulith
  public-client: false
  bearer-only: true
  ssl-required: none  # 개발 환경 (프로덕션: external)
  
  # CORS 설정
  cors: true
  cors-allowed-origins: http://localhost:3000,http://localhost:8080
  cors-allowed-methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
```

---

### Step 3: Security Configuration

#### 📝 **KeycloakSecurityConfig.java 생성**

`src/main/java/com/nexfron/identitymodulith/common/security/KeycloakSecurityConfig.java`:

```java
package com.nexfron.identitymodulith.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class KeycloakSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // CSRF 비활성화 (JWT 사용 시)
            .csrf(csrf -> csrf.disable())
            
            // 세션 미사용 (Stateless)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 인증/인가 규칙
            .authorizeHttpRequests(auth -> auth
                // Public 엔드포인트 (인증 불필요)
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/health",
                    "/api/v1/auth/**"  // 로그인/토큰 발급
                ).permitAll()
                
                // ADMIN 권한 필요
                .requestMatchers(
                    "/api/rbac/**",
                    "/api/v1/organizations/**"
                ).hasRole("ADMIN")
                
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            
            // OAuth 2.0 Resource Server 설정
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    /**
     * JWT에서 권한(Role) 추출
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        
        // Keycloak의 roles를 GrantedAuthority로 변환
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        
        // JWT의 'preferred_username' 또는 'sub'를 Principal name으로 사용
        converter.setPrincipalClaimName("preferred_username");
        
        return converter;
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",  // React 개발 서버
            "http://localhost:8080"   // 자체 서버
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Keycloak JWT에서 역할 추출 Converter
     */
    static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            // 1. realm_access.roles에서 역할 추출
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            Collection<String> realmRoles = realmAccess != null 
                ? (Collection<String>) realmAccess.get("roles") 
                : List.of();

            // 2. resource_access.{client-id}.roles에서 역할 추출
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            Collection<String> clientRoles = List.of();
            if (resourceAccess != null) {
                Map<String, Object> client = (Map<String, Object>) resourceAccess.get("identity-modulith");
                if (client != null) {
                    clientRoles = (Collection<String>) client.get("roles");
                }
            }

            // 3. 모든 역할을 ROLE_ 접두사와 함께 GrantedAuthority로 변환
            return Stream.concat(
                    realmRoles.stream(),
                    clientRoles.stream()
                )
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        }
    }
}
```

---

### Step 4: 커스텀 필터 (Tenant 추출)

#### 📝 **JwtTenantFilter.java 생성**

`src/main/java/com/nexfron/identitymodulith/common/security/JwtTenantFilter.java`:

```java
package com.nexfron.identitymodulith.common.security;

import com.nexfron.identitymodulith.common.config.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT에서 Tenant ID를 추출하여 TenantContextHolder에 설정하는 필터
 */
@Component
@Slf4j
public class JwtTenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                // JWT에서 tenant_id 클레임 추출
                String tenantId = jwt.getClaim("tenant_id");
                
                if (tenantId == null || tenantId.isBlank()) {
                    // tenant_id가 없으면 기본값 사용
                    tenantId = "default-tenant";
                    log.debug("[JWT Filter] tenant_id 클레임 없음, 기본값 사용: {}", tenantId);
                }
                
                TenantContextHolder.setCurrentTenantId(tenantId);
                log.debug("[JWT Filter] Tenant 설정 완료 - tenantId: {}, username: {}", 
                    tenantId, jwt.getClaim("preferred_username"));
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            // 요청 종료 시 컨텍스트 클리어
            TenantContextHolder.clear();
        }
    }
}
```

#### 📝 **SecurityFilterChain에 필터 추가**

`KeycloakSecurityConfig.java` 수정:

```java
@Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http, 
        JwtTenantFilter jwtTenantFilter) throws Exception {
    http
        // ...기존 설정...
        
        // JWT Tenant 필터 추가
        .addFilterAfter(jwtTenantFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
        
        // OAuth 2.0 Resource Server 설정
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );

    return http.build();
}
```

---

### Step 5: 인증 컨트롤러 추가

#### 📝 **AuthController.java 생성**

`src/main/java/com/nexfron/identitymodulith/common/security/AuthController.java`:

```java
package com.nexfron.identitymodulith.common.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Tag(name = "Authentication", description = "인증 관리 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    @Operation(summary = "로그인", description = "사용자 인증 후 JWT 토큰을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        log.info("[Auth] 로그인 요청 - username: {}", request.username());

        try {
            // Keycloak Token Endpoint로 요청
            String tokenUrl = issuerUri + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("username", request.username());
            body.add("password", request.password());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);

            log.info("[Auth] 로그인 성공 - username: {}", request.username());
            
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("[Auth] 로그인 실패 - username: {}, error: {}", request.username(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials"));
        }
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새 Access Token을 발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody RefreshRequest request) {
        try {
            String tokenUrl = issuerUri + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", request.refreshToken());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("[Auth] 토큰 갱신 실패 - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid refresh token"));
        }
    }

    @Operation(summary = "로그아웃", description = "토큰을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        try {
            String logoutUrl = issuerUri + "/protocol/openid-connect/logout";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", request.refreshToken());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(logoutUrl, entity, String.class);

            log.info("[Auth] 로그아웃 완료");
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("[Auth] 로그아웃 실패 - error: {}", e.getMessage());
            return ResponseEntity.noContent().build();
        }
    }

    // DTOs
    record LoginRequest(String username, String password) {}
    record RefreshRequest(String refreshToken) {}
    record LogoutRequest(String refreshToken) {}
}
```

---

## RBAC 동기화 전략

### 전략 1: Keycloak을 Master로 (권장 ❌)
- Keycloak의 역할/권한을 그대로 사용
- Identity Modulith는 읽기만 수행
- **단점**: Identity Modulith의 RBAC 기능 사용 불가

### 전략 2: Identity Modulith를 Master로 (권장 ✅)
- Identity Modulith의 RBAC를 Keycloak에 동기화
- Keycloak은 인증만 담당, 권한은 로컬 DB 사용
- **장점**: 기존 RBAC 시스템 그대로 활용

### 전략 3: 하이브리드 (최종 권장 ⭐)
- **인증**: Keycloak (JWT 발급)
- **역할/권한 관리**: Identity Modulith (RBAC)
- **권한 검증**: JWT의 역할 + 로컬 DB의 세밀한 권한

---

## 하이브리드 전략 구현

### Step 1: Keycloak 동기화 Service

#### 📝 **KeycloakSyncService.java 생성**

`src/main/java/com/nexfron/identitymodulith/common/security/KeycloakSyncService.java`:

```java
package com.nexfron.identitymodulith.common.security;

import com.nexfron.identitymodulith.rbac.application.service.RbacManagementService;
import com.nexfron.identitymodulith.rbac.application.service.RbacManagementService.CreateRoleRequest;
import com.nexfron.identitymodulith.rbac.application.service.RbacManagementService.RoleDto;
import com.nexfron.identitymodulith.rbac.domain.RoleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Keycloak과 Identity Modulith RBAC 동기화 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakSyncService {

    private final RbacManagementService rbacManagementService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;

    /**
     * Identity Modulith → Keycloak 역할 동기화
     */
    public void syncRoleToKeycloak(String roleName) {
        log.info("[Keycloak Sync] 역할 동기화 시작 - roleName: {}", roleName);

        try {
            // 1. Admin Access Token 획득
            String adminToken = getAdminAccessToken();

            // 2. Keycloak에 역할 생성
            createKeycloakRole(roleName, adminToken);

            log.info("[Keycloak Sync] 역할 동기화 완료 - roleName: {}", roleName);

        } catch (Exception e) {
            log.error("[Keycloak Sync] 역할 동기화 실패 - roleName: {}, error: {}", roleName, e.getMessage());
            throw new RuntimeException("Keycloak 동기화 실패", e);
        }
    }

    /**
     * Keycloak → Identity Modulith 역할 동기화
     */
    public void syncRoleFromKeycloak(String roleName, String adminUserId) {
        log.info("[Keycloak Sync] 역할 가져오기 시작 - roleName: {}", roleName);

        try {
            // 1. Keycloak에서 역할 존재 확인
            String adminToken = getAdminAccessToken();
            boolean exists = checkKeycloakRoleExists(roleName, adminToken);

            if (!exists) {
                log.warn("[Keycloak Sync] Keycloak에 역할 없음 - roleName: {}", roleName);
                return;
            }

            // 2. Identity Modulith에 역할 생성
            CreateRoleRequest request = new CreateRoleRequest(
                roleName,
                RoleType.POSITION,
                "Keycloak에서 동기화된 역할"
            );

            rbacManagementService.createRole(request, adminUserId);

            log.info("[Keycloak Sync] 역할 가져오기 완료 - roleName: {}", roleName);

        } catch (Exception e) {
            log.error("[Keycloak Sync] 역할 가져오기 실패 - roleName: {}, error: {}", roleName, e.getMessage());
        }
    }

    /**
     * Admin Access Token 획득
     */
    private String getAdminAccessToken() {
        String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = String.format(
            "grant_type=client_credentials&client_id=%s&client_secret=%s",
            clientId, clientSecret
        );

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);
        return (String) response.getBody().get("access_token");
    }

    /**
     * Keycloak에 역할 생성
     */
    private void createKeycloakRole(String roleName, String adminToken) {
        String roleUrl = String.format("%s/admin/realms/%s/roles", keycloakUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> roleData = Map.of(
            "name", roleName,
            "description", "Identity Modulith에서 동기화된 역할"
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(roleData, headers);

        restTemplate.postForEntity(roleUrl, entity, String.class);
    }

    /**
     * Keycloak에 역할 존재 확인
     */
    private boolean checkKeycloakRoleExists(String roleName, String adminToken) {
        String roleUrl = String.format("%s/admin/realms/%s/roles/%s", keycloakUrl, realm, roleName);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                roleUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

### Step 6: 사용자 생성 시 Keycloak 동기화

#### 📝 **AgentService.java 수정**

```java
@Service
@RequiredArgsConstructor
public class AgentService {
    
    private final KeycloakSyncService keycloakSyncService;
    
    @Transactional
    public AgentResponse createAgent(CreateAgentRequest request, String currentUserId) {
        // 1. Identity Modulith에 사용자 생성
        Agent agent = Agent.create(...);
        Agent savedAgent = agentRepository.save(agent);
        
        // 2. Keycloak에 사용자 생성 (비동기 권장)
        try {
            keycloakSyncService.createKeycloakUser(
                savedAgent.getLoginId(),
                savedAgent.getEmail(),
                tempPassword
            );
        } catch (Exception e) {
            log.error("[Keycloak Sync] 사용자 동기화 실패 - loginId: {}", savedAgent.getLoginId(), e);
            // 실패해도 계속 진행 (Keycloak은 보조 시스템)
        }
        
        return toResponse(savedAgent, tempPassword);
    }
}
```

---

## 인증/인가 구현

### Step 1: JWT 토큰 검증

#### 📝 **@PreAuthorize 사용 (메소드 레벨 권한)**

```java
@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    // ADMIN 권한 필요
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AgentResponse> createAgent(...) {
        // ...
    }

    // 본인 또는 ADMIN만 접근
    @PreAuthorize("hasRole('ADMIN') or #agentId == authentication.principal.claims['agent_id']")
    @GetMapping("/{agentId}")
    public ResponseEntity<AgentResponse> getAgent(@PathVariable String agentId) {
        // ...
    }
}
```

---

### Step 2: 커스텀 권한 검증 (세밀한 권한)

#### 📝 **PermissionEvaluator 구현**

`src/main/java/com/nexfron/identitymodulith/common/security/CustomPermissionEvaluator.java`:

```java
package com.nexfron.identitymodulith.common.security;

import com.nexfron.identitymodulith.rbac.RbacModuleApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * 커스텀 권한 평가자
 * - Spring Security의 @PreAuthorize에서 hasPermission() 사용 가능
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final RbacModuleApi rbacModuleApi;

    @Override
    public boolean hasPermission(
            Authentication authentication,
            Object targetDomainObject,
            Object permission) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }

        // JWT에서 agentId 추출
        String agentId = jwt.getClaim("agent_id");
        if (agentId == null) {
            log.warn("[Permission] agentId 없음 - username: {}", jwt.getClaim("preferred_username"));
            return false;
        }

        // RBAC 모듈에서 권한 확인
        Set<String> permissions = rbacModuleApi.getEffectivePermissions(UUID.fromString(agentId));
        boolean hasPermission = permissions.contains(permission.toString());

        log.debug("[Permission] 권한 확인 - agentId: {}, permission: {}, granted: {}", 
            agentId, permission, hasPermission);

        return hasPermission;
    }

    @Override
    public boolean hasPermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            Object permission) {
        return hasPermission(authentication, null, permission);
    }
}
```

#### 📝 **SecurityConfig에 등록**

```java
@Configuration
@EnableMethodSecurity
public class KeycloakSecurityConfig {

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            CustomPermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
```

#### 📝 **사용 예시**

```java
// 세밀한 권한 검증
@PreAuthorize("hasPermission(null, 'user:create')")
@PostMapping
public ResponseEntity<AgentResponse> createAgent(...) {
    // user:create 권한이 있어야 실행됨
}

@PreAuthorize("hasPermission(null, 'org:delete')")
@DeleteMapping("/{deptId}")
public ResponseEntity<Void> deleteDepartment(@PathVariable String deptId) {
    // org:delete 권한이 있어야 실행됨
}
```

---

## 테스트 시나리오

### Scenario 1: 로그인 및 토큰 발급

```bash
# 1. 로그인
curl -X POST 'http://localhost:8080/api/v1/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "test.admin",
    "password": "password123"
  }'
```

**응답**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "token_type": "Bearer"
}
```

---

### Scenario 2: JWT로 API 호출

```bash
# 2. Access Token으로 API 호출
curl -X GET 'http://localhost:8080/api/rbac/roles' \
  -H 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI...'
```

**응답**:
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

### Scenario 3: 토큰 갱신

```bash
# 3. Refresh Token으로 새 Access Token 발급
curl -X POST 'http://localhost:8080/api/v1/auth/refresh' \
  -H 'Content-Type: application/json' \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI..."
  }'
```

---

### Scenario 4: 로그아웃

```bash
# 4. 로그아웃
curl -X POST 'http://localhost:8080/api/v1/auth/logout' \
  -H 'Content-Type: application/json' \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI..."
  }'
```

---

## 구현 체크리스트

### ✅ **Phase 1: 기본 연동 (1~2일)**
- [ ] Keycloak Docker 설치 및 실행
- [ ] Realm 및 Client 생성
- [ ] Spring Boot 의존성 추가
- [ ] application.yml 설정
- [ ] SecurityConfig 구현
- [ ] 로그인/로그아웃 API 구현

### ✅ **Phase 2: 권한 검증 (2~3일)**
- [ ] JwtTenantFilter 구현
- [ ] CustomPermissionEvaluator 구현
- [ ] @PreAuthorize로 권한 검증
- [ ] 기존 X-User-Id 헤더를 JWT로 전환

### ✅ **Phase 3: RBAC 동기화 (3~5일)**
- [ ] KeycloakSyncService 구현
- [ ] 역할 생성 시 Keycloak 동기화
- [ ] 사용자 생성 시 Keycloak 동기화
- [ ] 동기화 실패 처리 (재시도, 로깅)

### ✅ **Phase 4: 테스트 (2~3일)**
- [ ] 로그인 플로우 테스트
- [ ] JWT 검증 테스트
- [ ] 권한 체크 테스트
- [ ] 동기화 테스트

---

## 주의사항

### ⚠️ **보안**
1. **Client Secret 관리**: 환경 변수 또는 Vault 사용
2. **JWT 검증**: 반드시 Keycloak의 공개 키로 검증
3. **HTTPS 필수**: 프로덕션 환경에서는 HTTPS만 사용
4. **토큰 만료 시간**: Access Token 5분, Refresh Token 30분

### ⚠️ **동기화**
1. **단방향 동기화 권장**: Identity Modulith → Keycloak
2. **실패 처리**: 동기화 실패 시에도 로컬 DB는 정상 동작
3. **재시도 로직**: 네트워크 오류 시 재시도
4. **로깅**: 모든 동기화 작업 로깅

### ⚠️ **성능**
1. **동기화 비동기 처리**: `@Async`로 비동기 처리
2. **캐시 활용**: JWT 검증 결과 캐싱 (5분)
3. **커넥션 풀**: Keycloak 호출용 별도 RestTemplate

---

## 마이그레이션 가이드

### 기존 X-User-Id → JWT 전환

#### **변경 전**:
```java
@GetMapping("/roles")
public ResponseEntity<List<RoleDto>> getAllRoles(
    @RequestHeader("X-User-Id") String userId) {
    // ...
}
```

#### **변경 후**:
```java
@GetMapping("/roles")
public ResponseEntity<List<RoleDto>> getAllRoles(
    Authentication authentication) {
    
    Jwt jwt = (Jwt) authentication.getPrincipal();
    String userId = jwt.getClaim("agent_id");
    // ...
}
```

---

## 참고 자료

### 📚 **Keycloak 공식 문서**
- [Server Administration Guide](https://www.keycloak.org/docs/latest/server_admin/)
- [Securing Applications](https://www.keycloak.org/docs/latest/securing_apps/)

### 📚 **Spring Security OAuth 2.0**
- [OAuth 2.0 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [JWT Authentication](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

### 📂 **프로젝트 문서**
- [API_SPECIFICATION.md](./API_SPECIFICATION.md) - API 명세
- [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md) - DB 스키마

---

## 예상 타임라인

```
Week 1: Keycloak 설치 및 기본 연동
├─ Day 1-2: Docker 설정, Realm/Client 생성
├─ Day 3-4: Spring Security 설정, 로그인 API
└─ Day 5: 테스트 및 검증

Week 2: 권한 검증 및 RBAC 동기화
├─ Day 1-2: CustomPermissionEvaluator 구현
├─ Day 3-4: KeycloakSyncService 구현
└─ Day 5: 통합 테스트

Week 3: 기존 시스템 마이그레이션
├─ Day 1-2: X-User-Id → JWT 전환
├─ Day 3-4: 전체 API 테스트
└─ Day 5: 성능 테스트 및 최적화
```

**총 소요 기간**: **약 3주** (1인 기준)

---

**문서 작성일**: 2026-02-22  
**작성자**: Identity System Team  
**최종 검토일**: 2026-02-22  
**버전**: 1.0

