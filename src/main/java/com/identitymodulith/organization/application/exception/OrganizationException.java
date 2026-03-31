package com.identitymodulith.organization.application.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Organization 도메인 예외. */
@Getter
public class OrganizationException extends RuntimeException {

    private final OrganizationErrorCode errorCode;

    public OrganizationException(OrganizationErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public OrganizationException(OrganizationErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public OrganizationException(OrganizationErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    /** Organization 에러 코드 정의. */
    @Getter
    public enum OrganizationErrorCode {
        DEPARTMENT_NOT_FOUND("DEPT_NOT_FOUND", "부서를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
        INVALID_PARENT("INVALID_PARENT", "부모 부서를 찾을 수 없습니다", HttpStatus.BAD_REQUEST),
        CIRCULAR_REFERENCE("CIRCULAR_REFERENCE", "자신의 하위 부서로 이동할 수 없습니다", HttpStatus.BAD_REQUEST),
        CHILD_DEPARTMENT_EXISTS("CHILD_DEPT_EXISTS", "하위 부서가 존재하여 삭제할 수 없습니다", HttpStatus.CONFLICT),
        ACTIVE_USERS_EXIST("ACTIVE_USERS_EXIST", "소속 구성원이 존재하여 삭제할 수 없습니다", HttpStatus.CONFLICT),
        DUPLICATE_DEPT_CODE("DUPLICATE_DEPT_CODE", "이미 존재하는 부서 코드입니다", HttpStatus.CONFLICT),

        ROOT_ALREADY_EXISTS("ROOT_ALREADY_EXISTS", "테넌트에 이미 루트 부서가 존재합니다", HttpStatus.CONFLICT),
        CANNOT_MOVE_ROOT("CANNOT_MOVE_ROOT", "루트 부서는 이동할 수 없습니다", HttpStatus.BAD_REQUEST),
        CANNOT_DEACTIVATE_ROOT("CANNOT_DEACTIVATE_ROOT", "루트 부서는 비활성화할 수 없습니다", HttpStatus.BAD_REQUEST),
        CANNOT_DELETE_ROOT("CANNOT_DELETE_ROOT", "루트 부서는 삭제할 수 없습니다", HttpStatus.BAD_REQUEST),

        PARENT_DEPT_INACTIVE("PARENT_DEPT_INACTIVE", "상위 부서가 비활성 상태입니다", HttpStatus.BAD_REQUEST),
        CUSTOM_TYPE_NAME_REQUIRED("CUSTOM_TYPE_NAME_REQUIRED", "CUSTOM 타입은 커스텀 타입명이 필수입니다", HttpStatus.BAD_REQUEST),
        CODE_CANNOT_BE_CHANGED("CODE_CANNOT_BE_CHANGED", "부서 코드는 변경할 수 없습니다", HttpStatus.BAD_REQUEST),
        SAME_PARENT_DEPARTMENT("SAME_PARENT_DEPT", "이미 동일한 상위 부서입니다", HttpStatus.BAD_REQUEST),

        USER_NOT_FOUND("USER_NOT_FOUND", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
        USER_INACTIVE("USER_INACTIVE", "비활성화된 사용자입니다", HttpStatus.FORBIDDEN),
        USER_DEPARTMENT_NOT_FOUND("USER_DEPT_NOT_FOUND", "사용자의 소속 부서를 찾을 수 없습니다", HttpStatus.NOT_FOUND),

        INSUFFICIENT_PERMISSION("INSUFFICIENT_PERMISSION", "권한이 없습니다", HttpStatus.FORBIDDEN),
        INVALID_REQUEST("INVALID_REQUEST", "잘못된 요청입니다", HttpStatus.BAD_REQUEST),
        INTERNAL_ERROR("INTERNAL_ERROR", "내부 서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);

        private final String code;
        private final String message;
        private final HttpStatus httpStatus;

        OrganizationErrorCode(String code, String message, HttpStatus httpStatus) {
            this.code = code;
            this.message = message;
            this.httpStatus = httpStatus;
        }
    }
}

