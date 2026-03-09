package com.identitymodulith.rbac.application.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * RBAC 비즈니스 예외
 */
@Getter
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

    /**
     * RBAC 에러 코드 정의
     */
    @Getter
    public enum RbacErrorCode {
        ROLE_NOT_FOUND("ROLE_NOT_FOUND", "역할을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
        ROLE_ALREADY_EXISTS("ROLE_ALREADY_EXISTS", "이미 존재하는 역할입니다", HttpStatus.CONFLICT),
        ROLE_NOT_ACTIVE("ROLE_NOT_ACTIVE", "비활성화된 역할입니다", HttpStatus.BAD_REQUEST),
        PERMISSION_NOT_FOUND("PERMISSION_NOT_FOUND", "권한을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
        PERMISSION_ALREADY_EXISTS("PERMISSION_ALREADY_EXISTS", "이미 존재하는 권한입니다", HttpStatus.CONFLICT),
        PERMISSION_ALREADY_ASSIGNED("PERMISSION_ALREADY_ASSIGNED", "이미 할당된 권한입니다", HttpStatus.CONFLICT),
        PERMISSION_IN_USE("PERMISSION_IN_USE", "역할에서 사용 중인 권한은 삭제할 수 없습니다", HttpStatus.BAD_REQUEST),
        ROLE_HAS_USERS("ROLE_HAS_USERS", "사용자가 있는 역할은 삭제할 수 없습니다", HttpStatus.BAD_REQUEST),
        AGENT_RETIRED("AGENT_RETIRED", "퇴사 또는 비활성 상담사에게 역할을 할당할 수 없습니다", HttpStatus.UNPROCESSABLE_ENTITY),
        INSUFFICIENT_PERMISSION("INSUFFICIENT_PERMISSION", "권한이 부족합니다", HttpStatus.FORBIDDEN),
        INTERNAL_ERROR("INTERNAL_ERROR", "내부 서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);

        private final String code;
        private final String message;
        private final HttpStatus httpStatus;

        RbacErrorCode(String code, String message, HttpStatus httpStatus) {
            this.code = code;
            this.message = message;
            this.httpStatus = httpStatus;
        }
    }
}

