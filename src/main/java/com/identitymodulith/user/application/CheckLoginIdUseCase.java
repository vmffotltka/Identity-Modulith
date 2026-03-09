package com.identitymodulith.user.application;

public interface CheckLoginIdUseCase {

    boolean isLoginIdUnique(String loginId);
}
