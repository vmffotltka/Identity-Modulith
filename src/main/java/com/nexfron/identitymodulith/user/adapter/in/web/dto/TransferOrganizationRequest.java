package com.nexfron.identitymodulith.user.adapter.in.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class TransferOrganizationRequest {

    private UUID organizationId;
}