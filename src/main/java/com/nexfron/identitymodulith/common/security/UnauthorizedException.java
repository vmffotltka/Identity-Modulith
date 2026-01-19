package com.nexfron.identitymodulith.common.security;

/**
 * UnauthorizedException - 인증/권한 예외
 *
 * <h2>발생 상황:</h2>
 * <ul>
 *   <li>인증 정보가 없는 경우</li>
 *   <li>테넌트 ID를 추출할 수 없는 경우</li>
 *   <li>권한이 부족한 경우</li>
 * </ul>
 *
 * @author Identity System Team
 * @version 1.0
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}

