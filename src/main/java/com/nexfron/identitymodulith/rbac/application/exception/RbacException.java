package com.nexfron.identitymodulith.rbac.application.exception;

/**
 * RBAC 비즈니스 예외
 */
public class RbacException extends RuntimeException {

    private final RbacErrorCode errorCode;

    public RbacException(RbacErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public RbacException(RbacErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public RbacException(RbacErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public RbacErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * RBAC 에러 코드 정의
     */
    public enum RbacErrorCode {
        ROLE_NOT_FOUND("ROLE_NOT_FOUND", "역할을 찾을 수 없습니다"),
        ROLE_ALREADY_EXISTS("ROLE_ALREADY_EXISTS", "이미 존재하는 역할입니다"),
        ROLE_NOT_ACTIVE("ROLE_NOT_ACTIVE", "비활성화된 역할입니다"),
        PERMISSION_NOT_FOUND("PERMISSION_NOT_FOUND", "권한을 찾을 수 없습니다"),
        PERMISSION_ALREADY_EXISTS("PERMISSION_ALREADY_EXISTS", "이미 존재하는 권한입니다"),
        PERMISSION_ALREADY_ASSIGNED("PERMISSION_ALREADY_ASSIGNED", "이미 할당된 권한입니다"),
        ROLE_HAS_USERS("ROLE_HAS_USERS", "사용자가 있는 역할은 삭제할 수 없습니다"),
        INTERNAL_ERROR("INTERNAL_ERROR", "내부 서버 오류가 발생했습니다");

        private final String code;
        private final String message;

        RbacErrorCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}

