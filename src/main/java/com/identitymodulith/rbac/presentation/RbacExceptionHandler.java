package com.identitymodulith.rbac.presentation;

import com.identitymodulith.ApiErrorResponse;
import com.identitymodulith.rbac.application.exception.RbacException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RBAC 모듈 전용 예외 처리
 *
 * <p>공통 예외(UnauthorizedException, MethodArgumentNotValidException, fallback 등)는
 * {@link com.identitymodulith.common.exception.CommonExceptionHandler}에서 처리합니다.</p>
 */
@RestControllerAdvice(basePackages = "com.identitymodulith.rbac")
@Slf4j
public class RbacExceptionHandler {

    /**
     * RbacException 전역 예외 처리
     *
     * RBAC 비즈니스 로직에서 발생한 예외를 처리합니다.
     * 에러 코드를 기반으로 적절한 HTTP 상태 코드를 반환합니다.
     *
     * 처리 흐름:
     * 1. RbacException에서 에러 코드 추출
     * 2. ErrorResponse 객체 생성 (에러 코드, 메시지)
     * 3. 에러 코드에 따른 HTTP 상태 코드 결정
     * 4. ResponseEntity에 상태 코드와 응답 본문을 담아 반환
     *
     * @param e RBAC 예외
     * @return 클라이언트에 반환할 ResponseEntity (상태 코드 + 에러 응답)
     */
    @ExceptionHandler(RbacException.class)
    public ResponseEntity<ApiErrorResponse> handleRbacException(RbacException e) {
        // RbacException에서 에러 코드 객체 추출
        RbacException.RbacErrorCode errorCode = e.getErrorCode();
        HttpStatus status = errorCode.getHttpStatus();

        // 로그 레벨에 따른 로그 출력
        if (status == HttpStatus.FORBIDDEN || status == HttpStatus.CONFLICT) {
            log.warn("[RBAC] code={}, status={}, message={}", errorCode.getCode(), status.value(), e.getMessage());
        } else if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            log.error("[RBAC] code={}, status={}, message={}", errorCode.getCode(), status.value(), e.getMessage(), e);
        } else {
            log.info("[RBAC] code={}, status={}, message={}", errorCode.getCode(), status.value(), e.getMessage());
        }

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), errorCode.getCode(), e.getMessage()));
    }
}
