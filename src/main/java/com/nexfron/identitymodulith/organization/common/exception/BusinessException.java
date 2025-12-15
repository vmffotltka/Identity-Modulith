package com.nexfron.identitymodulith.organization.common.exception;

import lombok.Getter;

/**
 * 비즈니스 예외의 최상위 부모
 *
 * - HTTP 로 노출할 때는 상태 코드, 에러 코드 등을 매핑해서 사용 가능
 * - 지금은 message 만 들고 있는 단순한 구조
 */
@Getter
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
