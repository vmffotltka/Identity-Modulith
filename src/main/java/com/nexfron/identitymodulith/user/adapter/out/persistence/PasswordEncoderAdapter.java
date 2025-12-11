package com.nexfron.identitymodulith.user.adapter.out.persistence;

import com.nexfron.identitymodulith.user.application.port.out.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class PasswordEncoderAdapter implements PasswordEncoder {

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