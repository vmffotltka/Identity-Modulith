package com.nexfron.identitymodulith.user.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class Role {

    private final String name;
    private final RoleType type;

    public enum RoleType {
        POSITION,  // 직급
        CHANNEL    // 채널 (전화, 채팅 등)
    }
}