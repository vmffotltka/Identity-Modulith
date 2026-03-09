package com.identitymodulith.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 비밀번호 변경 요청 DTO (본인용)
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "비밀번호 변경 요청")
public class ChangePasswordRequest {

    @Schema(
        description = "현재 비밀번호",
        example = "CurrentPass123!",
        required = true
    )
    @NotBlank(message = "현재 비밀번호는 필수입니다")
    private String currentPassword;

    @Schema(
        description = "새 비밀번호 (8-20자, 영문 대소문자, 숫자, 특수문자 포함)",
        example = "NewPass123!@",
        required = true
    )
    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8-20자여야 합니다")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,20}$",
        message = "비밀번호는 영문 대소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다"
    )
    private String newPassword;

    @Schema(
        description = "새 비밀번호 확인",
        example = "NewPass123!@",
        required = true
    )
    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String confirmPassword;

    /**
     * 비밀번호 일치 여부 검증
     */
    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
