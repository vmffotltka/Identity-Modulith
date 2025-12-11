package com.nexfron.identitymodulith.user.application.port.out;

public interface PasswordEncoder {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}