package com.nexfron.identitymodulith.user.application.port.in;

public interface CheckLoginIdUseCase {

    boolean isLoginIdUnique(String loginId);
}