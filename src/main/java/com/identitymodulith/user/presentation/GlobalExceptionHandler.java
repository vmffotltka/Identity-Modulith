package com.identitymodulith.user.presentation;

import com.identitymodulith.ApiErrorResponse;
import com.identitymodulith.user.domain.exception.BusinessException;
import com.identitymodulith.user.domain.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * User 모듈 전용 예외 처리
 *
 * <p>공통 예외(UnauthorizedException, MethodArgumentNotValidException, fallback 등)는
 * {@link com.identitymodulith.common.exception.CommonExceptionHandler}에서 처리합니다.</p>
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.identitymodulith.user")
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        HttpStatus status = errorCode.getStatus();

        if (status == HttpStatus.FORBIDDEN || status == HttpStatus.CONFLICT) {
            log.warn("[User] code={}, status={}, message={}", errorCode.getCode(), status.value(), e.getMessage());
        } else if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            log.error("[User] code={}, status={}, message={}", errorCode.getCode(), status.value(), e.getMessage(), e);
        } else {
            log.info("[User] code={}, status={}, message={}", errorCode.getCode(), status.value(), e.getMessage());
        }

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), errorCode.getCode(), e.getMessage()));
    }
}
