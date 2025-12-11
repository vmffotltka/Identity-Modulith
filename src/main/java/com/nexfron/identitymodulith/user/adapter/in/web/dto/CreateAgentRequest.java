package com.nexfron.identitymodulith.user.adapter.in.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class CreateAgentRequest {

    private String username;
    private String name;
    private UUID organizationId;
}