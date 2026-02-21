package com.nexfron.identitymodulith.user.presentation;

import com.nexfron.identitymodulith.rbac.application.exception.RbacException;
import com.nexfron.identitymodulith.user.domain.exception.BusinessException;
import com.nexfron.identitymodulith.user.domain.exception.ErrorCode;
import com.nexfron.identitymodulith.user.presentation.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode, e.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * RBAC 예외 처리
     */
    @ExceptionHandler(RbacException.class)
    public ResponseEntity<java.util.Map<String, String>> handleRbacException(RbacException e) {
        log.warn("[GlobalExceptionHandler] RbacException 발생!");

        RbacException.RbacErrorCode errorCode = e.getErrorCode();

        log.warn("[GlobalExceptionHandler] errorCode={}, code={}, message={}",
            errorCode, errorCode.getCode(), errorCode.getMessage());

        // HTTP 상태 코드 매핑
        HttpStatus status = switch (errorCode) {
            case ROLE_NOT_FOUND, PERMISSION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ROLE_ALREADY_EXISTS, PERMISSION_ALREADY_EXISTS, PERMISSION_ALREADY_ASSIGNED -> HttpStatus.CONFLICT;
            case INSUFFICIENT_PERMISSION -> HttpStatus.FORBIDDEN;
            case ROLE_HAS_USERS, ROLE_NOT_ACTIVE, PERMISSION_IN_USE -> HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        log.warn("[GlobalExceptionHandler] status={}", status);

        java.util.Map<String, String> response = java.util.Map.of(
            "code", errorCode.getCode(),
            "message", errorCode.getMessage()
        );

        log.warn("[RBAC Exception] code={}, message={}, status={}", errorCode.getCode(), errorCode.getMessage(), status);

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Bean Validation 에러 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("[Validation Error] {}", errorMessage);

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[Unexpected Error] {}", e.getMessage(), e);

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "서버 내부 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
