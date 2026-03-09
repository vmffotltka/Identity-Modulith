package com.identitymodulith.user.infrastructure.persistence;

import com.identitymodulith.user.domain.service.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * PasswordEncoder 인터페이스의 구현체
 *
 * Domain Layer의 PasswordEncoder 인터페이스를 구현하여
 * DIP(의존성 역전 원칙)를 적용합니다.
 *
 * BCryptPasswordEncoder를 사용하여 안전한 비밀번호 암호화를 수행합니다.
 */
@Component
@RequiredArgsConstructor
public class PasswordEncoderImpl implements PasswordEncoder {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public String encode(String rawPassword) {
        return bCryptPasswordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return bCryptPasswordEncoder.matches(rawPassword, encodedPassword);
    }
}
