package com.nexfron.identitymodulith.organization.presentation;

import com.nexfron.identitymodulith.organization.exception.OrganizationException;
import com.nexfron.identitymodulith.organization.exception.EntityNotFoundException;
import com.nexfron.identitymodulith.organization.exception.BusinessException;
import lombok.RequiredArgsConstructor;
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
 *   <li>OrganizationException: 조직/부서 관련 비즈니스 로직 예외</li>
 *   <li>EntityNotFoundException: 엔티티(부서) 미존재 예외</li>
 *   <li>BusinessException: 일반 비즈니스 로직 예외</li>
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
 *   <li>403 Forbidden: 권한 없음 (Level 2 RBAC)</li>
 *   <li>500 Internal Server Error: 예측하지 못한 서버 오류</li>
 * </ul>
 *
 * @see OrganizationException
 * @see EntityNotFoundException
 * @see BusinessException
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class OrganizationExceptionHandler {

    /**
     * OrganizationException 처리
     *
     * <h3>발생 시나리오:</h3>
     * <ul>
     *   <li>부서 조회 실패 (DEPARTMENT_NOT_FOUND)</li>
     *   <li>부모 부서 미존재 (INVALID_PARENT)</li>
     *   <li>순환 참조 감지 (CIRCULAR_REFERENCE)</li>
     *   <li>하위 부서 존재 (CHILD_DEPARTMENT_EXISTS)</li>
     *   <li>활성 사용자 존재 (ACTIVE_USERS_EXIST)</li>
     *   <li>권한 부족 (INSUFFICIENT_PERMISSION)</li>
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
        // ...existing code...
        OrganizationException.OrganizationErrorCode errorCode = e.getErrorCode();

        ErrorResponse response = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /**
     * EntityNotFoundException 처리
     *
     * <h3>발생 시나리오:</h3>
     * <ul>
     *   <li>부서 정보 조회 실패</li>
     *   <li>부서의 상위 부서 미존재</li>
     *   <li>사용자 정보 미존재</li>
     * </ul>
     *
     * <h3>처리:</h3>
     * - HTTP 404 Not Found 응답
     * - 일반적인 "엔티티를 찾을 수 없습니다" 메시지
     *
     * @param e EntityNotFoundException 객체
     * @return HTTP 404 Not Found + 에러 응답
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException e) {
        // ...existing code...
        ErrorResponse response = ErrorResponse.builder()
                .code("ENTITY_NOT_FOUND")
                .message(e.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * BusinessException 처리 (일반 비즈니스 예외)
     *
     * <h3>발생 시나리오:</h3>
     * <ul>
     *   <li>비즈니스 규칙 위반</li>
     *   <li>유효성 검증 실패</li>
     *   <li>데이터 무결성 문제</li>
     * </ul>
     *
     * <h3>처리:</h3>
     * - HTTP 400 Bad Request 응답
     * - 예외의 메시지 그대로 클라이언트에 전달
     *
     * @param e BusinessException 객체
     * @return HTTP 400 Bad Request + 에러 응답
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        // ...existing code...
        ErrorResponse response = ErrorResponse.builder()
                .code("BUSINESS_ERROR")
                .message(e.getMessage())
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

