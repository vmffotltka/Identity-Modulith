package com.nexfron.identitymodulith.user.adapter.in.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
public class AssignSkillsRequest {

    private Set<String> skills;
}