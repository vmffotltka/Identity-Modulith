package com.identitymodulith.util;

import java.util.regex.Pattern;

/**
 * 비밀번호 정규식 검증 테스트
 */
public class TestPasswordPattern {
    public static void main(String[] args) {
        // 현재 적용된 정규식
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,20}$";
        Pattern pattern = Pattern.compile(regex);

        // 테스트할 비밀번호들
        String[] passwords = {
            "Admin123!",
            "MyNewPassword456!",
            "NewPass123!@",
            "TempPassword123!",
            "password",
            "Pass123",
            "Test@Pass#123"
        };

        System.out.println("=".repeat(80));
        System.out.println("비밀번호 정규식 검증 테스트");
        System.out.println("=".repeat(80));
        System.out.println("정규식: " + regex);
        System.out.println("=".repeat(80));

        for (String pwd : passwords) {
            boolean matches = pattern.matcher(pwd).matches();
            System.out.printf("%-25s : %s%n", pwd, matches ? "✅ 통과" : "❌ 실패");
        }

        System.out.println("=".repeat(80));
    }
}

