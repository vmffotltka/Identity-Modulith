package com.nexfron.identitymodulith.user.adapter.in.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateAgentRequest {

    private String loginId;
    private String name;
    private String organizationId;
}