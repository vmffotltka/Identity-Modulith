package com.nexfron.identitymodulith.rbac.presentation;

import com.nexfron.identitymodulith.rbac.application.exception.RbacException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RBAC 모듈 전역 예외 처리
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class RbacExceptionHandler {

    /**
     * RbacException 처리
     */
    @ExceptionHandler(RbacException.class)
    public ResponseEntity<ErrorResponse> handleRbacException(RbacException e) {
        RbacException.RbacErrorCode errorCode = e.getErrorCode();

        ErrorResponse response = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        HttpStatus status = mapErrorCodeToStatus(errorCode);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        ErrorResponse response = ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("서버 오류가 발생했습니다")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus mapErrorCodeToStatus(RbacException.RbacErrorCode errorCode) {
        return switch (errorCode) {
            case ROLE_NOT_FOUND, PERMISSION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ROLE_ALREADY_EXISTS, PERMISSION_ALREADY_EXISTS, PERMISSION_ALREADY_ASSIGNED -> HttpStatus.CONFLICT;
            case ROLE_HAS_USERS -> HttpStatus.BAD_REQUEST;
        };
    }

    /**
     * 에러 응답 DTO
     */
    public static class ErrorResponse {
        private String code;
        private String message;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public static ErrorResponseBuilder builder() {
            return new ErrorResponseBuilder();
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public static class ErrorResponseBuilder {
            private String code;
            private String message;

            public ErrorResponseBuilder code(String code) {
                this.code = code;
                return this;
            }

            public ErrorResponseBuilder message(String message) {
                this.message = message;
                return this;
            }

            public ErrorResponse build() {
                return new ErrorResponse(this.code, this.message);
            }
        }
    }
}

