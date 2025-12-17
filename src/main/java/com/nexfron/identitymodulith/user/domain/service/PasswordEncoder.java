package com.nexfron.identitymodulith.user.domain.service;

/**
 * 비밀번호 인코딩 서비스 인터페이스
 *
 * DDD의 DIP(의존성 역전 원칙)에 따라 Domain Layer에 위치합니다.
 * Infrastructure Layer에서 이 인터페이스를 구현합니다.
 */
public interface PasswordEncoder {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
