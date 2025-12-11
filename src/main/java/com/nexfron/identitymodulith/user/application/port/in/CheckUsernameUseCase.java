package com.nexfron.identitymodulith.user.application.port.in;

public interface CheckUsernameUseCase {

    boolean isUsernameUnique(String username);
}