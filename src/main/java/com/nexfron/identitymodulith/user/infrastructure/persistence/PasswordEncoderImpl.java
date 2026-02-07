package com.nexfron.identitymodulith.user.infrastructure.persistence;

import com.nexfron.identitymodulith.user.domain.service.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * PasswordEncoder 인터페이스의 구현체
 *
 * Domain Layer의 PasswordEncoder 인터페이스를 구현하여
 * DIP(의존성 역전 원칙)를 적용합니다.
 */
@Component
public class PasswordEncoderImpl implements PasswordEncoder {

    // TODO: 실제 운영에서는 BCryptPasswordEncoder 등 Spring Security 사용 권장

    @Override
    public String encode(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to encode password", e);
        }
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }
}
