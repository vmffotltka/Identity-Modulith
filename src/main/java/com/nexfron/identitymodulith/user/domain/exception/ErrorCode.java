package com.nexfron.identitymodulith.user.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common Errors (C)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "잘못된 타입의 값입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C005", "요청한 리소스를 찾을 수 없습니다."),

    // Agent Errors (A)
    AGENT_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "상담사를 찾을 수 없습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "A002", "이미 사용 중인 아이디입니다."),
    AGENT_ALREADY_RETIRED(HttpStatus.BAD_REQUEST, "A003", "이미 퇴사 처리된 상담사입니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "A004", "잘못된 상태 전이입니다."),
    BUSINESS_RULE_VIOLATION(HttpStatus.BAD_REQUEST, "A005", "비즈니스 규칙을 위반했습니다."),

    // Authentication Errors (AUTH)
    PASSWORD_ENCODING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH001", "비밀번호 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
