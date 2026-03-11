package com.identitymodulith.common.exception;

import com.identitymodulith.ApiErrorResponse;
import com.identitymodulith.common.security.context.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 공통 전역 예외 처리
 *
 * <p>모든 모듈에서 공통으로 발생하는 예외를 처리합니다.
 * 모듈 전용 예외(BusinessException, RbacException, OrganizationException)는
 * 각 모듈의 ExceptionHandler에서 처리합니다.</p>
 *
 * <h3>로그 레벨 규칙:</h3>
 * <ul>
 *   <li>WARN  - 401/403/409</li>
 *   <li>INFO  - 400/404 클라이언트 실수</li>
 *   <li>ERROR - 500 내부 오류</li>
 * </ul>
 */
@RestControllerAdvice
@Slf4j
public class CommonExceptionHandler {

    // ── 인증 예외 ─────────────────────────────────────────────────────────────

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("[Common][Auth] 미인증 요청: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(401, "UNAUTHORIZED", e.getMessage()));
    }

    // ── Bean Validation ───────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.info("[Common][Validation] {}", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(400, "INVALID_INPUT_VALUE", errorMessage));
    }

    // ── DB 무결성 제약 위반 ────────────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException e) {
        log.warn("[Common][DB] 데이터 무결성 위반: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(409, "DATA_INTEGRITY_VIOLATION", "데이터 무결성 제약 조건을 위반했습니다"));
    }

    // ── 정적 리소스 404 ───────────────────────────────────────────────────────

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // ── 최종 fallback ─────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception e) {
        log.error("[Common][Unexpected] {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(500, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다"));
    }
}

