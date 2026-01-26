# Keycloak 연동 필수 작업 목록

> 📅 작성일: 2026-01-22  
> 🎯 목표: Keycloak JWT 기반 인증 연동

---

## 🚨 필수 작업 (우선순위 높음)

### 1. **의존성 추가** (build.gradle)

```groovy
dependencies {
    // Keycloak Spring Boot Adapter (또는 OAuth2 Resource Server)
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    
    // 또는 Keycloak Adapter (선택)
    // implementation 'org.keycloak:keycloak-spring-boot-starter:23.0.0'
}
```

**선택 가이드:**
- **OAuth2 Resource Server (권장)**: Spring Security 표준, 유연함
- **Keycloak Adapter**: Keycloak 전용, 간편함

---

### 2. **application.yml 설정**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.example.com/realms/{realm-name}
          # 또는
          jwk-set-uri: https://keycloak.example.com/realms/{realm-name}/protocol/openid-connect/certs

# 테넌트 ID 추출 방법 (Custom Claim 매핑)
app:
  security:
    jwt:
      tenant-claim-name: "tenant_id"  # Keycloak에서 설정한 custom claim 이름
      user-id-claim-name: "sub"       # 표준 claim (사용자 ID)
```

---

### 3. **SecurityConfig.java 생성** ⭐ 최우선

**위치:** `src/main/java/com/nexfron/identitymodulith/config/SecurityConfig.java`

```java
package com.nexfron.identitymodulith.config;

import com.nexfron.identitymodulith.common.security.AuthPrincipal;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security 설정
 * 
 * Keycloak JWT 토큰 기반 인증 및 권한 검증
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize 활성화
@Slf4j
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS 설정 (필요 시)
            .cors(cors -> cors.configurationSource(request -> {
                var config = new org.springframework.web.cors.CorsConfiguration();
                config.addAllowedOrigin("*");
                config.addAllowedMethod("*");
                config.addAllowedHeader("*");
                return config;
            }))
            
            // CSRF 비활성화 (JWT 사용 시 불필요)
            .csrf(csrf -> csrf.disable())
            
            // 세션 사용 안 함 (Stateless)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 엔드포인트별 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/health"
                ).permitAll()
                
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            
            // JWT 인증 설정
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    /**
     * JWT를 Spring Security Authentication으로 변환
     * 
     * Keycloak JWT 구조:
     * {
     *   "sub": "user-uuid",
     *   "tenant_id": "tenant-001",  // custom claim
     *   "realm_access": {
     *     "roles": ["ADMIN", "TEAM_LEAD"]
     *   }
     * }
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        
        // Authorities 추출 (Keycloak roles → GrantedAuthority)
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("realm_access.roles");
            if (roles == null) {
                roles = List.of();
            }
            
            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        });
        
        // Principal을 AuthPrincipal로 변환
        converter.setPrincipalClaimName("sub"); // 사용자 ID
        
        return new Converter<Jwt, AbstractAuthenticationToken>() {
            @Override
            public AbstractAuthenticationToken convert(Jwt jwt) {
                Collection<GrantedAuthority> authorities = 
                    converter.getJwtGrantedAuthoritiesConverter().convert(jwt);
                
                // JWT에서 정보 추출
                String userId = jwt.getClaimAsString("sub");
                String tenantId = jwt.getClaimAsString("tenant_id"); // ⚠️ Keycloak 설정 필요
                
                if (tenantId == null) {
                    log.warn("JWT에 tenant_id claim이 없습니다. sub: {}", userId);
                    tenantId = "default"; // 또는 예외 발생
                }
                
                // AuthPrincipal 구현체 생성 (Record 사용)
                record KeycloakPrincipal(String tenantId, String userId) implements AuthPrincipal {
                    @Override
                    public String getTenantId() { return tenantId; }
                    
                    @Override
                    public String getUserId() { return userId; }
                }
                
                var principal = new KeycloakPrincipal(tenantId, userId);
                
                return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    principal, jwt, authorities
                );
            }
        };
    }
}
```

---

### 4. **Keycloak 서버 설정** (Keycloak Admin Console)

#### 4.1 Realm 생성
- Realm 이름: `identity-system` (예시)

#### 4.2 Client 생성
- Client ID: `identity-modulith-api`
- Client Protocol: `openid-connect`
- Access Type: `bearer-only` (API 서버용)

#### 4.3 **Custom Claim 추가** (중요!)
Keycloak에서 JWT에 `tenant_id` claim을 포함시키려면:

1. **User Attribute 추가**:
   - Users → 사용자 선택 → Attributes 탭
   - Key: `tenant_id`, Value: `tenant-001`

2. **Mapper 생성**:
   - Clients → `identity-modulith-api` → Mappers → Create
   - Mapper Type: `User Attribute`
   - Name: `tenant-id-mapper`
   - User Attribute: `tenant_id`
   - Token Claim Name: `tenant_id`
   - Claim JSON Type: `String`
   - Add to ID token: ON
   - Add to access token: ON

#### 4.4 Roles 매핑
- Realm Roles 또는 Client Roles 생성:
  - `ADMIN`, `TEAM_LEAD`, `AGENT` 등
- 사용자에게 Role 할당

---

### 5. **테스트 시나리오**

#### 5.1 JWT 토큰 발급 (Keycloak)
```bash
curl -X POST "https://keycloak.example.com/realms/identity-system/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=identity-modulith-api" \
  -d "username=admin" \
  -d "password=admin123"
```

응답 예시:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

#### 5.2 API 호출 테스트
```bash
curl -X GET "http://localhost:8080/api/rbac/roles" \
  -H "Authorization: Bearer {access_token}" \
  -H "X-Tenant-Id: tenant-001"
```

#### 5.3 권한 검증 테스트
```bash
# @PreAuthorize("@rbac.hasPermission('user:manage')") 테스트
curl -X POST "http://localhost:8080/api/rbac/roles" \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "SENIOR_AGENT",
    "type": "POSITION",
    "description": "수석 상담사"
  }'
```

---

## 🔧 선택적 개선 사항

### 1. **테넌트 ID 추출 전략 개선**

현재 `TenantContextHolder`는 3가지 방법으로 tenantId 추출:
1. AuthPrincipal 인터페이스 ✅ (권장)
2. UserDetails username 파싱 (`tenantId:userId`)
3. Principal 문자열 직접 사용

**Keycloak 연동 후**는 방법 1만 사용하도록 최적화 가능.

### 2. **X-Tenant-Id 헤더 검증**

현재는 클라이언트가 보낸 `X-Tenant-Id` 헤더를 신뢰합니다.  
보안 강화를 위해 JWT의 `tenant_id` claim과 일치 여부 검증 추가:

```java
@Component
public class TenantValidationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String headerTenantId = request.getHeader("X-Tenant-Id");
        String jwtTenantId = TenantContextHolder.getCurrentTenantId();
        
        if (!jwtTenantId.equals(headerTenantId)) {
            throw new UnauthorizedException("테넌트 ID 불일치");
        }
    }
}
```

### 3. **권한 캐싱 개선**

현재 `RbacQueryService`는 메모리 캐시 사용 중.  
프로덕션 환경에서는 Redis 캐시로 전환 권장:

```yaml
spring:
  cache:
    type: redis
  redis:
    host: localhost
    port: 6379
```

---

## 📊 작업 우선순위

| 우선순위 | 작업 | 예상 시간 | 필수 여부 |
|---------|------|----------|---------|
| 🔴 P0 | SecurityConfig.java 생성 | 2시간 | **필수** |
| 🔴 P0 | application.yml JWT 설정 | 30분 | **필수** |
| 🔴 P0 | Keycloak 서버 설정 | 1시간 | **필수** |
| 🟡 P1 | build.gradle 의존성 추가 | 10분 | **필수** |
| 🟡 P1 | 토큰 발급/검증 테스트 | 1시간 | **필수** |
| 🟢 P2 | 테넌트 ID 검증 필터 | 1시간 | 권장 |
| 🟢 P2 | Redis 캐시 전환 | 2시간 | 선택 |

---

## ✅ 검증 체크리스트

연동 완료 후 다음 항목을 확인하세요:

- [ ] Keycloak에서 JWT 토큰 발급 성공
- [ ] `/api/rbac/roles` 호출 시 401 Unauthorized (토큰 없을 때)
- [ ] `/api/rbac/roles` 호출 시 200 OK (유효한 토큰 포함 시)
- [ ] `TenantContextHolder.getCurrentTenantId()` 정상 작동
- [ ] `TenantContextHolder.getCurrentUserId()` 정상 작동
- [ ] 다른 테넌트의 데이터 접근 차단 (멀티테넌시 격리)
- [ ] `@PreAuthorize("@rbac.hasPermission('user:manage')")` 권한 검증 작동
- [ ] Swagger UI에서 "Authorize" 버튼으로 JWT 입력 가능
- [ ] 감사 로그에 operatorId 정상 기록

---

## 📚 참고 자료

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Keycloak Spring Boot Adapter](https://www.keycloak.org/docs/latest/securing_apps/#_spring_boot_adapter)
- [멀티테넌시 패턴](https://docs.microsoft.com/azure/architecture/patterns/multitenant-identity)

---

*작성자: Identity System Team*  
*최종 수정: 2026-01-22*
