package com.nexfron.identitymodulith.organization.common.exception;
/**
 * InvalidDepartmentMoveException
 *
 * - 조직(Department) 이동 시 비즈니스 규칙을 위반했을 때 발생하는 예외
 * - 예: 자신의 하위 부서로 이동하려는 경우 (순환 참조)
 *
 * 설계 의도:
 * - IllegalArgumentException 대신 도메인 의미가 드러나는 예외 사용
 * - Service 계층에서 BusinessException 등으로 변환 가능
 */
public class InvalidDepartmentMoveException extends RuntimeException {
    public InvalidDepartmentMoveException(String message) {
        super(message);
    }
}
