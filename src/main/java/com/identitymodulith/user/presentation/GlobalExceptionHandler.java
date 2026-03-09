package com.identitymodulith.user.presentation;

import com.identitymodulith.ApiErrorResponse;
import com.identitymodulith.common.security.context.UnauthorizedException;
import com.identitymodulith.user.domain.exception.BusinessException;
import com.identitymodulith.user.domain.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * User 모듈 전역 예외 처리
 *
 * <h3>로그 레벨 규칙:</h3>
 * <ul>
 *   <li>WARN  - 403/409 권한 부족·충돌 / 비즈니스 규칙 위반</li>
 *   <li>INFO  - 400/404 클라이언트 실수</li>
 *   <li>ERROR - 500 내부 오류 / 예측 불가 예외</li>
 * </ul>
 *
 * 모듈 경계 원칙:
 * - OrganizationException → OrganizationExceptionHandler에서 처리
 * - RbacException        → RbacExceptionHandler에서 처리
 * - BusinessException    → 여기서 처리 (user 모듈 전용)
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

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("[User][Auth] 미인증 요청: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(401, "UNAUTHORIZED", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("[User][Validation] {}", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(400, "INVALID_INPUT_VALUE", errorMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception e) {
        log.error("[User][Unexpected] {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}
