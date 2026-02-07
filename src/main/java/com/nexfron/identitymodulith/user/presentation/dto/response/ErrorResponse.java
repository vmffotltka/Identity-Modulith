package com.nexfron.identitymodulith.user.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexfron.identitymodulith.user.domain.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "에러 응답")
public class ErrorResponse {

    @Schema(description = "에러 발생 시각", example = "2024-01-01T10:00:00")
    private final LocalDateTime timestamp;

    @Schema(description = "HTTP 상태 코드", example = "400")
    private final int status;

    @Schema(description = "에러 코드", example = "INVALID_INPUT_VALUE")
    private final String code;

    @Schema(description = "에러 메시지", example = "유효하지 않은 입력값입니다.")
    private final String message;

    @Builder
    private ErrorResponse(int status, String code, String message) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .message(message)
                .build();
    }
}
