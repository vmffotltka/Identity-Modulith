package com.nexfron.identitymodulith.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 초기 데이터용 비밀번호 해시 생성 유틸리티
 *
 * 실행: java GeneratePasswordHash.java
 */
public class GeneratePasswordHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 테스트용 비밀번호: Admin123!
        String password = "Admin123!";
        String hash = encoder.encode(password);

        System.out.println("=".repeat(80));
        System.out.println("비밀번호 해시 생성 (BCrypt)");
        System.out.println("=".repeat(80));
        System.out.println("원본 비밀번호: " + password);
        System.out.println("해시값: " + hash);
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("SQL 업데이트:");
        System.out.println("UPDATE user_agents SET password = '" + hash + "' WHERE agent_id = '10000000-0000-0000-0000-000000000001';");
        System.out.println("UPDATE user_agents SET password = '" + hash + "' WHERE agent_id = '10000000-0000-0000-0000-000000000002';");
        System.out.println("UPDATE user_agents SET password = '" + hash + "' WHERE agent_id = '10000000-0000-0000-0000-000000000003';");
        System.out.println("=".repeat(80));

        // 검증
        boolean matches = encoder.matches(password, hash);
        System.out.println("\n검증 테스트: " + (matches ? "✅ 성공" : "❌ 실패"));
    }
}

