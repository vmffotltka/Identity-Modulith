package com.identitymodulith.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * PasswordEncoder Bean 설정
 *
 * 순환 참조 방지를 위해 Saml2SecurityConfig에서 분리
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCryptPasswordEncoder Bean 등록
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

