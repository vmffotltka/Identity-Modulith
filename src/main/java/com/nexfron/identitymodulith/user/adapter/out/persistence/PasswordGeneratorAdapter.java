package com.nexfron.identitymodulith.user.adapter.out.persistence;

import com.nexfron.identitymodulith.user.application.port.out.PasswordGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PasswordGeneratorAdapter implements PasswordGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
    private static final int PASSWORD_LENGTH = 12;
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generateTempPassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }
}