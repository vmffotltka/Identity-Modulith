package com.identitymodulith;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 공통 API 에러 응답 포맷
 *
 * <p>모든 모듈(user, rbac, organization)에서 동일한 형식으로 에러를 반환합니다.
 * 루트 패키지에 위치하여 모든 모듈에서 참조 가능합니다.</p>
 *
 * <h3>응답 예시:</h3>
 * <pre>
 * {
 *   "timestamp": "2026-03-10T12:00:00",
 *   "status": 403,
 *   "code": "INSUFFICIENT_PERMISSION",
 *   "message": "권한이 없습니다"
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "공통 API 에러 응답")
public class ApiErrorResponse {

    @Schema(description = "에러 발생 시각", example = "2026-03-10T12:00:00")
    private final LocalDateTime timestamp;

    @Schema(description = "HTTP 상태 코드", example = "403")
    private final int status;

    @Schema(description = "에러 코드", example = "INSUFFICIENT_PERMISSION")
    private final String code;

    @Schema(description = "에러 메시지", example = "권한이 없습니다")
    private final String message;

    private ApiErrorResponse(int status, String code, String message) {
        this.timestamp = LocalDateTime.now();
        this.status    = status;
        this.code      = code;
        this.message   = message;
    }

    public static ApiErrorResponse of(int status, String code, String message) {
        return new ApiErrorResponse(status, code, message);
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus()              { return status; }
    public String getCode()             { return code; }
    public String getMessage()          { return message; }
}

