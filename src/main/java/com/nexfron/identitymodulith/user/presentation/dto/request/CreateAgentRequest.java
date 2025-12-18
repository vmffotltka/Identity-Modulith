package com.nexfron.identitymodulith.user.presentation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateAgentRequest {

    private String tenantId;
    private String loginId;
    private String name;
    private String organizationId;
}
