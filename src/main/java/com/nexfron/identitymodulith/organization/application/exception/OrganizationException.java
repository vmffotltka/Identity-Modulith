package com.nexfron.identitymodulith.organization.application.exception;

/**
 * Organization 비즈니스 예외
 * 
 * RBAC 모듈과 동일한 패턴으로 통일
 */
public class OrganizationException extends RuntimeException {

    private final OrganizationErrorCode errorCode;

    public OrganizationException(OrganizationErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public OrganizationException(OrganizationErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public OrganizationErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Organization 에러 코드 정의
     */
    public enum OrganizationErrorCode {
        DEPARTMENT_NOT_FOUND("DEPT_NOT_FOUND", "부서를 찾을 수 없습니다", 404),
        INVALID_PARENT("INVALID_PARENT", "부모 부서를 찾을 수 없습니다", 400),
        CIRCULAR_REFERENCE("CIRCULAR_REFERENCE", "자신의 하위 부서로 이동할 수 없습니다", 400),
        CHILD_DEPARTMENT_EXISTS("CHILD_DEPT_EXISTS", "하위 부서가 존재하여 삭제할 수 없습니다", 409),
        ACTIVE_USERS_EXIST("ACTIVE_USERS_EXIST", "소속 구성원이 존재하여 삭제할 수 없습니다", 409),
        INSUFFICIENT_PERMISSION("INSUFFICIENT_PERMISSION", "권한이 없습니다", 403),
        INVALID_REQUEST("INVALID_REQUEST", "잘못된 요청입니다", 400),
        INTERNAL_ERROR("INTERNAL_ERROR", "내부 서버 오류가 발생했습니다", 500);

        private final String code;
        private final String message;
        private final int httpStatus;

        OrganizationErrorCode(String code, String message, int httpStatus) {
            this.code = code;
            this.message = message;
            this.httpStatus = httpStatus;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }
}

