package com.nexfron.identitymodulith.rbac.presentation;

import com.nexfron.identitymodulith.rbac.application.exception.RbacException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RBAC 모듈 전역 예외 처리
 *
 * @RestControllerAdvice를 사용하여 RBAC 모듈에서 발생하는
 * 모든 예외를 일관되게 처리합니다.
 *
 * 처리 대상:
 * - RbacException: RBAC 비즈니스 로직 예외
 * - Exception: 예상치 못한 기타 예외
 *
 * 응답 포맷:
 * {
 *   "code": "에러 코드 (예: ROLE_NOT_FOUND)",
 *   "message": "에러 메시지 (예: 역할을 찾을 수 없습니다)"
 * }
 *
 * HTTP 상태 코드 매핑:
 * - 404 Not Found: 리소스를 찾을 수 없는 경우
 * - 409 Conflict: 리소스 충돌 (중복, 이미 할당됨 등)
 * - 400 Bad Request: 잘못된 요청 (사용자가 있는 역할 삭제 등)
 * - 500 Internal Server Error: 예측하지 못한 서버 오류
 *
 * @see RbacException
 */
@RestControllerAdvice
@RequiredArgsConstructor
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
    public ResponseEntity<ErrorResponse> handleRbacException(RbacException e) {
        // RbacException에서 에러 코드 객체 추출
        RbacException.RbacErrorCode errorCode = e.getErrorCode();

        // 에러 응답 객체 생성
        ErrorResponse response = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        // 에러 코드에 따른 HTTP 상태 코드 매핑
        HttpStatus status = mapErrorCodeToStatus(errorCode);
        return ResponseEntity.status(status).body(response);
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
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        // 일반적인 내부 서버 오류 응답 생성
        ErrorResponse response = ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("서버 오류가 발생했습니다")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * RBAC 에러 코드를 HTTP 상태 코드로 매핑합니다.
     *
     * 매핑 규칙:
     * - NOT_FOUND (404): 요청한 리소스가 없는 경우
     * - CONFLICT (409): 리소스 충돌 (중복, 이미 할당됨 등)
     * - BAD_REQUEST (400): 잘못된 요청 (규칙 위반 등)
     *
     * @param errorCode RBAC 에러 코드
     * @return HTTP 상태 코드
     */
    private HttpStatus mapErrorCodeToStatus(RbacException.RbacErrorCode errorCode) {
        // switch 표현식을 사용하여 에러 코드를 HTTP 상태 코드로 변환
        return switch (errorCode) {
            // 404 Not Found: 리소스를 찾을 수 없는 경우
            case ROLE_NOT_FOUND, PERMISSION_NOT_FOUND -> HttpStatus.NOT_FOUND;

            // 409 Conflict: 리소스 충돌 (중복 생성, 이미 할당됨 등)
            case ROLE_ALREADY_EXISTS, PERMISSION_ALREADY_EXISTS, PERMISSION_ALREADY_ASSIGNED -> HttpStatus.CONFLICT;

            // 403 Forbidden: 권한 부족
            case INSUFFICIENT_PERMISSION -> HttpStatus.FORBIDDEN;

            // 400 Bad Request: 잘못된 요청 (비즈니스 규칙 위반)
            case ROLE_HAS_USERS, ROLE_NOT_ACTIVE -> HttpStatus.BAD_REQUEST;

            // 500 Internal Server Error: 내부 서버 오류
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * 에러 응답 DTO
     *
     * API 응답으로 클라이언트에 반환되는 에러 정보입니다.
     * code: 에러를 식별하는 고유 코드
     * message: 사용자가 이해할 수 있는 에러 메시지
     *
     * JSON 응답 예시:
     * {
     *   "code": "ROLE_NOT_FOUND",
     *   "message": "역할을 찾을 수 없습니다"
     * }
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

        /**
         * ErrorResponse 빌더 클래스
         * 빌더 패턴을 사용하여 ErrorResponse 객체를 생성합니다.
         */
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

