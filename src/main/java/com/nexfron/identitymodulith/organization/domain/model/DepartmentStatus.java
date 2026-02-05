package com.nexfron.identitymodulith.organization.domain.model;

/**
 * 부서 상태 Enum
 *
 * 부서의 활성화/비활성화 상태를 관리합니다.
 *
 * @author Organization Module Team
 * @version 1.0
 */
public enum DepartmentStatus {
    /**
     * 활성 상태
     * - 정상적으로 운영되는 부서
     * - 소속 직원 배치 가능
     * - 조직도에 표시됨
     */
    ACTIVE,

    /**
     * 비활성 상태
     * - 일시적으로 운영 중단된 부서
     * - 신규 직원 배치 불가
     * - 기존 직원은 유지 (다른 부서로 이동 권장)
     * - 조직도에서 비활성으로 표시
     * - 재활성화 가능
     */
    INACTIVE
}
