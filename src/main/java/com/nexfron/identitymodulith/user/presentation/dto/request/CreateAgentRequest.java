package com.nexfron.identitymodulith.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 상담사 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@Schema(description = "상담사 생성 요청")
public class CreateAgentRequest {

    @Schema(description = "테넌트 ID", example = "default-tenant", required = true)
    @NotBlank(message = "테넌트 ID는 필수입니다")
    private String tenantId;

    @Schema(description = "로그인 아이디 (4-20자, 영문/숫자/특수문자(_.-) 가능)", example = "agent001", required = true)
    @NotBlank(message = "로그인 아이디는 필수입니다")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]{4,20}$", message = "로그인 아이디는 4-20자이며 알파벳, 숫자, _, ., -만 포함 가능합니다")
    private String loginId;

    @Schema(description = "상담사 이름", example = "홍길동", required = true)
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자 이내여야 합니다")
    private String name;

    @Schema(description = "소속 조직 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    @NotBlank(message = "조직 ID는 필수입니다")
    private String organizationId;

    @Schema(description = "역할 목록 (최소 1개 필수)", example = "[\"MEMBER\"]", required = true)
    @NotEmpty(message = "역할은 최소 1개 이상이어야 합니다")
    private Set<String> roles;

    // 선택 필드
    @Schema(description = "이메일", example = "hong@example.com")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Schema(description = "전화번호 (하이픈 포함)", example = "010-1234-5678")
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$|^02-\\d{3,4}-\\d{4}$|^\\d{2,3}-\\d{3,4}-\\d{4}$",
             message = "전화번호 형식이 올바르지 않습니다")
    private String phone;

    @Schema(description = "사번", example = "EMP-2024-001")
    private String employeeId;
}
