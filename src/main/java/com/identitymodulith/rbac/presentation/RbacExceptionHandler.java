package com.identitymodulith.rbac.presentation;

import com.identitymodulith.ApiErrorResponse;
import com.identitymodulith.rbac.application.exception.RbacException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RBAC 모듈 전역 예외 처리
 *
 * <h3>로그 레벨 규칙:</h3>
 * <ul>
 *   <li>WARN  - 403 권한 부족, 409 충돌</li>
 *   <li>INFO  - 404 리소스 없음, 400 잘못된 요청</li>
 *   <li>ERROR - 500 내부 오류 / 예측 불가 예외</li>
 * </ul>
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

    /**
     * 예기치 않은 일반 예외 처리
     *
     * RbacException이 아닌 모든 예외를 처리합니다.
     * 클라이언트에는 상세한 에러 정보를 노출하지 않고,
     * 일반적인 내부 서버 오류 메시지만 반환합니다.
     *
     * 로깅은 Spring의 기본 예외 처리 메커니즘에 의해 자동으로 수행됩니다.
     *
     * @param e 발생한 예외
     * @return 클라이언트에 반환할 ResponseEntity (500 상태 코드 + 일반 오류 메시지)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception e) {
        log.error("[RBAC][Unexpected] {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(500, "INTERNAL_ERROR", "서버 오류가 발생했습니다"));
    }
}
