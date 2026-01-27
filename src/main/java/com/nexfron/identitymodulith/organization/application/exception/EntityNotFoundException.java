package com.nexfron.identitymodulith.organization.application.exception;

/**
 * 엔티티를 찾지 못했을 때 사용하는 예외
 * - "존재하지 않는 부서입니다" 같은 상황에서 사용
 */
public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
