package com.identitymodulith.user.infrastructure.keycloak;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Keycloak Admin REST API 클라이언트
 *
 * <p>상담사 계정 활성화/비활성화 처리를 위해 Keycloak Admin API를 호출합니다.</p>
 *
 * <h2>인증 방식</h2>
 * <ul>
 *   <li>Client Credentials Grant (admin-cli)로 Access Token 획득</li>
 *   <li>토큰을 사용해 사용자 상태 변경 API 호출</li>
 * </ul>
 *
 * <h2>주요 기능</h2>
 * <ul>
 *   <li>사용자 비활성화 ({@code enabled: false}) — 정지/퇴사 시</li>
 *   <li>사용자 활성화 ({@code enabled: true})  — 복귀 시</li>
 * </ul>
 */
@Slf4j
@Component
public class KeycloakAdminClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    /**
     * Keycloak에서 사용자를 비활성화합니다.
     * 정지(SUSPENDED) 또는 퇴사(RETIRED) 처리 시 호출됩니다.
     *
     * @param username Keycloak 사용자명 (login_id)
     */
    public void disableUser(String username) {
        setUserEnabled(username, false);
    }

    /**
     * Keycloak에서 사용자를 활성화합니다.
     * 정지 해제(ACTIVE) 처리 시 호출됩니다.
     *
     * @param username Keycloak 사용자명 (login_id)
     */
    public void enableUser(String username) {
        setUserEnabled(username, true);
    }

    private void setUserEnabled(String username, boolean enabled) {
        String action = enabled ? "활성화" : "비활성화";
        try {
            String token = getAdminAccessToken();
            String userId = findKeycloakUserId(token, username);

            if (userId == null) {
                log.warn("[Keycloak] 사용자 없음 - username={}", username);
                return;
            }

            HttpHeaders headers = bearerHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId;
            restTemplate.exchange(url, HttpMethod.PUT,
                    new HttpEntity<>(Map.of("enabled", enabled), headers), Void.class);

            log.info("[Keycloak] 사용자 {} 완료 - username={}", action, username);

        } catch (HttpClientErrorException e) {
            log.error("[Keycloak] 사용자 {} 실패 - username={}, status={}, body={}",
                    action, username, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[Keycloak] 사용자 {} 중 오류 - username={}, 원인: {}", action, username, e.getMessage(), e);
        }
    }

    /**
     * admin-cli Client Credentials로 Access Token을 획득합니다.
     */
    private String getAdminAccessToken() {
        String tokenUrl = serverUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("username", adminUsername);
        body.add("password", adminPassword);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, new HttpEntity<>(body, headers),
                new org.springframework.core.ParameterizedTypeReference<>() {});

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("access_token")) {
            throw new IllegalStateException("Keycloak 토큰 응답에 access_token이 없습니다.");
        }
        return (String) responseBody.get("access_token");
    }

    /**
     * username으로 Keycloak 내부 userId(UUID)를 조회합니다.
     */
    private String findKeycloakUserId(String token, String username) {
        String url = serverUrl + "/admin/realms/" + realm + "/users?username=" + username + "&exact=true";

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(bearerHeaders(token)),
                new org.springframework.core.ParameterizedTypeReference<>() {});

        List<Map<String, Object>> users = response.getBody();
        if (users == null || users.isEmpty()) {
            return null;
        }
        return (String) users.getFirst().get("id");
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}





