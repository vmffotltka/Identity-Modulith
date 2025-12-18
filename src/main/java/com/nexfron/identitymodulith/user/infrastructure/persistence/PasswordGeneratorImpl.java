package com.nexfron.identitymodulith.user.infrastructure.persistence;

import com.nexfron.identitymodulith.user.domain.service.PasswordGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * PasswordGenerator 인터페이스의 구현체
 *
 * Domain Layer의 PasswordGenerator 인터페이스를 구현하여
 * DIP(의존성 역전 원칙)를 적용합니다.
 */
@Component
public class PasswordGeneratorImpl implements PasswordGenerator {

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
