package com.identitymodulith.organization.presentation;

import com.identitymodulith.ApiErrorResponse;
import com.identitymodulith.organization.application.exception.OrganizationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Organization 모듈 전용 예외 처리
 *
 * <p>공통 예외(UnauthorizedException, MethodArgumentNotValidException, fallback 등)는
 * {@link com.identitymodulith.common.exception.CommonExceptionHandler}에서 처리합니다.</p>
 *
 * <p>DB 무결성 제약 위반은 Organization 전용 부서코드 중복 처리가 있어 여기서 처리합니다.</p>
 */
@RestControllerAdvice(basePackages = "com.identitymodulith.organization")
@Slf4j
public class OrganizationExceptionHandler {

    // ── 조직 비즈니스 예외 ───────────────────────────────────────────────────

    @ExceptionHandler(OrganizationException.class)
    public ResponseEntity<ApiErrorResponse> handleOrganizationException(OrganizationException e) {
        OrganizationException.OrganizationErrorCode errorCode = e.getErrorCode();
        HttpStatus status = errorCode.getHttpStatus();

        if (status == HttpStatus.FORBIDDEN || status == HttpStatus.CONFLICT) {
            log.warn("[Org] code={}, status={}, message={}", errorCode.getCode(), status.value(), e.getMessage());
        } else if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            log.error("[Org] code={}, status={}, message={}", errorCode.getCode(), status.value(), e.getMessage(), e);
        } else {
            log.info("[Org] code={}, status={}, message={}", errorCode.getCode(), status.value(), e.getMessage());
        }

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), errorCode.getCode(), e.getMessage()));
    }

    // ── DB 무결성 제약 위반 (부서 코드 중복 특수 처리) ──────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException e) {
        String message = e.getMessage();

        if (message != null && (message.contains("uk_departments_tenant_code") ||
                                 message.contains("uk_org_departments_tenant_code"))) {
            log.warn("[Org][DB] 부서 코드 중복");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiErrorResponse.of(409, "DUPLICATE_DEPT_CODE", "이미 존재하는 부서 코드입니다"));
        }

        log.warn("[Org][DB] 무결성 제약 위반: {}", message);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(409, "DATA_INTEGRITY_VIOLATION", "데이터 무결성 제약 조건을 위반했습니다"));
    }
}
