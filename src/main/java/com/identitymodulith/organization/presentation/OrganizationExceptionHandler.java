package com.identitymodulith.organization.presentation;

import com.identitymodulith.ApiErrorResponse;
import com.identitymodulith.common.security.context.UnauthorizedException;
import com.identitymodulith.organization.application.exception.OrganizationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Organization 모듈 전역 예외 처리
 *
 * <h3>로그 레벨 규칙:</h3>
 * <ul>
 *   <li>WARN  - 403/409 권한 부족·충돌 / 비즈니스 규칙 위반</li>
 *   <li>INFO  - 400/404 클라이언트 실수</li>
 *   <li>ERROR - 500 내부 오류 / 예측 불가 예외</li>
 * </ul>
 *
 * <h3>응답 형식:</h3>
 * 공통 {@link ApiErrorResponse} 사용 — {timestamp, status, code, message}
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

    // ── 인증 예외 (organization 컨트롤러에서도 발생 가능) ─────────────────────

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("[Org][Auth] 미인증 요청: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(401, "UNAUTHORIZED", e.getMessage()));
    }

    // ── DB 무결성 제약 위반 ──────────────────────────────────────────────────

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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(400, "INVALID_REQUEST", "데이터 무결성 제약 조건을 위반했습니다"));
    }

    // ── Bean Validation ──────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "입력값이 올바르지 않습니다";
        log.warn("[Org][Validation] {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(400, "INVALID_INPUT_VALUE", message));
    }

    // ── 최종 fallback ────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception e) {
        // 정적 리소스 404는 노이즈 방지
        if (e instanceof org.springframework.web.servlet.resource.NoResourceFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        log.error("[Org][Unexpected] {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(500, "INTERNAL_ERROR", "서버 오류가 발생했습니다"));
    }
}
