package com.identitymodulith.user.presentation;

import com.identitymodulith.user.infrastructure.persistence.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 임시 비밀번호 해시 생성 컨트롤러 (개발용)
 */
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevController {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AgentRepository agentRepository;

    /**
     * 비밀번호 해시 생성
     * GET /api/dev/hash-password?password=Admin123!
     */
    @GetMapping("/hash-password")
    public Map<String, Object> hashPassword(@RequestParam String password) {
        String hash = encoder.encode(password);
        boolean matches = encoder.matches(password, hash);

        Map<String, Object> result = new HashMap<>();
        result.put("plainPassword", password);
        result.put("bcryptHash", hash);
        result.put("verified", matches);
        result.put("hashLength", hash.length());
        result.put("sqlUpdate", String.format(
                "UPDATE user_agents SET password = '%s' WHERE agent_id = '10000000-0000-0000-0000-000000000003';",
                hash
        ));

        return result;
    }

    /**
     * 비밀번호 검증
     * GET /api/dev/verify-password?password=Admin123!&hash=$2a$10$...
     */
    @GetMapping("/verify-password")
    public Map<String, Object> verifyPassword(
            @RequestParam String password,
            @RequestParam String hash) {

        boolean matches = encoder.matches(password, hash);

        Map<String, Object> result = new HashMap<>();
        result.put("plainPassword", password);
        result.put("hash", hash);
        result.put("hashLength", hash.length());
        result.put("matches", matches);

        return result;
    }

    /**
     * DB의 실제 비밀번호 해시 조회 및 검증
     * GET /api/dev/check-agent-password?agentId=...&password=Admin123!
     */
    @GetMapping("/check-agent-password")
    public Map<String, Object> checkAgentPassword(
            @RequestParam String agentId,
            @RequestParam String password) {

        Map<String, Object> result = new HashMap<>();

        try {
            var agent = agentRepository.findById(UUID.fromString(agentId))
                    .orElse(null);

            if (agent == null) {
                result.put("error", "Agent not found");
                result.put("agentId", agentId);
                return result;
            }

            String dbHash = agent.getPassword();
            boolean matches = encoder.matches(password, dbHash);

            result.put("agentId", agentId);
            result.put("loginId", agent.getLoginId());
            result.put("plainPassword", password);
            result.put("dbPasswordHash", dbHash);
            result.put("hashLength", dbHash.length());
            result.put("matches", matches);
            result.put("message", matches ? "✅ 비밀번호 일치" : "❌ 비밀번호 불일치");

            if (!matches) {
                // 실제 정상 해시 생성
                String correctHash = encoder.encode(password);
                result.put("correctHash", correctHash);
                result.put("sqlFix", String.format(
                        "UPDATE user_agents SET password = '%s' WHERE agent_id = '%s';",
                        correctHash, agentId
                ));
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }
}

