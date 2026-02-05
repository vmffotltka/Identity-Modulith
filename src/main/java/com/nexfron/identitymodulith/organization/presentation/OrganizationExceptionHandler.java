package com.nexfron.identitymodulith.organization.presentation;

import com.nexfron.identitymodulith.organization.application.exception.OrganizationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Organization 모듈 전역 예외 처리
 *
 * RBAC 모듈의 RbacExceptionHandler와 동일한 패턴으로 구현되어 있습니다.
 *
 * <h3>처리 대상:</h3>
 * <ul>
 *   <li>OrganizationException: 조직/부서 관련 모든 예외 통합 처리</li>
 *   <li>Exception: 예상치 못한 기타 예외</li>
 * </ul>
 *
 * <h3>응답 포맷:</h3>
 * <pre>
 * {
 *   "code": "에러 코드 (예: DEPT_NOT_FOUND)",
 *   "message": "에러 메시지 (예: 부서를 찾을 수 없습니다)"
 * }
 * </pre>
 *
 * <h3>HTTP 상태 코드 매핑:</h3>
 * <ul>
 *   <li>404 Not Found: 부서를 찾을 수 없는 경우</li>
 *   <li>400 Bad Request: 부서 이동 실패, 순환 참조 등</li>
 *   <li>409 Conflict: 하위 부서나 활성 사용자가 존재하여 삭제 불가</li>
 *   <li>403 Forbidden: 권한 없음 (Level 1 RBAC)</li>
 *   <li>500 Internal Server Error: 예측하지 못한 서버 오류</li>
 * </ul>
 *
 * @see OrganizationException
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class OrganizationExceptionHandler {

    /**
     * OrganizationException 처리 (통합 예외 처리)
     *
     * <h3>발생 시나리오:</h3>
     * <ul>
     *   <li>부서 조회 실패 (DEPARTMENT_NOT_FOUND)</li>
     *   <li>부모 부서 미존재 (INVALID_PARENT)</li>
     *   <li>순환 참조 감지 (CIRCULAR_REFERENCE)</li>
     *   <li>하위 부서 존재 (CHILD_DEPARTMENT_EXISTS)</li>
     *   <li>활성 사용자 존재 (ACTIVE_USERS_EXIST)</li>
     *   <li>권한 부족 (INSUFFICIENT_PERMISSION)</li>
     *   <li>사용자 정보 미존재 (USER_NOT_FOUND)</li>
     *   <li>비활성 사용자 (USER_INACTIVE)</li>
     * </ul>
     *
     * <h3>처리 흐름:</h3>
     * <ol>
     *   <li>OrganizationException에서 에러 코드 추출</li>
     *   <li>ErrorResponse 객체 생성 (에러 코드, 메시지)</li>
     *   <li>에러 코드의 HTTP 상태 코드로 응답</li>
     * </ol>
     *
     * <h3>예시:</h3>
     * <pre>
     * // 부서를 찾을 수 없는 경우
     * 요청: DELETE /api/org/departments/invalid-id
     * 예외: OrganizationException(DEPARTMENT_NOT_FOUND)
     * 응답: 404 Not Found
     * {
     *   "code": "DEPT_NOT_FOUND",
     *   "message": "부서를 찾을 수 없습니다"
     * }
     * </pre>
     *
     * @param e OrganizationException 객체
     * @return HTTP 상태 코드 + 에러 응답 본문
     */
    @ExceptionHandler(OrganizationException.class)
    public ResponseEntity<ErrorResponse> handleOrganizationException(OrganizationException e) {
        OrganizationException.OrganizationErrorCode errorCode = e.getErrorCode();

        ErrorResponse response = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /**
     * 데이터베이스 무결성 제약 위반 처리
     *
     * <h3>발생 시나리오:</h3>
     * <ul>
     *   <li>부서 코드 중복 (uk_departments_tenant_code 위반)</li>
     *   <li>FK 제약 위반</li>
     *   <li>UNIQUE 제약 위반</li>
     * </ul>
     *
     * <h3>처리:</h3>
     * - code 중복: DUPLICATE_DEPT_CODE (409 Conflict)
     * - 기타: INVALID_REQUEST (400 Bad Request)
     *
     * @param e DataIntegrityViolationException
     * @return HTTP 409 or 400 + 에러 메시지
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException e) {

        String message = e.getMessage();

        // 부서 코드 중복 감지
        if (message != null && (message.contains("uk_departments_tenant_code") ||
                                 message.contains("uk_org_departments_tenant_code"))) {
            ErrorResponse response = ErrorResponse.builder()
                    .code("DUPLICATE_DEPT_CODE")
                    .message("이미 존재하는 부서 코드입니다")
                    .build();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        // 기타 무결성 제약 위반
        ErrorResponse response = ErrorResponse.builder()
                .code("INVALID_REQUEST")
                .message("데이터 무결성 제약 조건을 위반했습니다")
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


    /**
     * 예기치 않은 일반 예외 처리
     *
     * <h3>발생 시나리오:</h3>
     * <ul>
     *   <li>데이터베이스 연결 오류</li>
     *   <li>NullPointerException 등 예측하지 못한 오류</li>
     *   <li>외부 시스템 호출 실패</li>
     * </ul>
     *
     * <h3>처리:</h3>
     * - HTTP 500 Internal Server Error 응답
     * - 클라이언트에는 상세 정보 미노출 (보안)
     * - 서버 로그에는 전체 스택 트레이스 기록 (Spring 기본 동작)
     *
     * @param e 발생한 예외
     * @return HTTP 500 Internal Server Error + 일반 에러 메시지
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        // 클라이언트에는 상세 정보 미노출
        ErrorResponse response = ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("서버 오류가 발생했습니다")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 에러 응답 DTO
     *
     * <h3>속성:</h3>
     * <ul>
     *   <li><b>code</b>: 에러를 식별하는 고유 코드
     *       <br/>예: DEPT_NOT_FOUND, INVALID_PARENT, CIRCULAR_REFERENCE
     *       <br/>클라이언트가 에러 타입을 프로그래매틱하게 구분할 때 사용
     *   </li>
     *   <li><b>message</b>: 사용자가 읽을 수 있는 에러 메시지
     *       <br/>예: "부서를 찾을 수 없습니다", "자신의 하위 부서로 이동할 수 없습니다"
     *       <br/>사용자 인터페이스에 직접 표시 가능
     *   </li>
     * </ul>
     *
     * <h3>JSON 예시:</h3>
     * <pre>
     * {
     *   "code": "DEPT_NOT_FOUND",
     *   "message": "부서를 찾을 수 없습니다"
     * }
     * </pre>
     *
     * @see ErrorResponse#builder()
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

