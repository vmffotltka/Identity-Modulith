package com.identitymodulith.organization;

import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

/**
 * Organization 모듈의 Public API (모듈 간 통신용)
 *
 * <h3>목적:</h3>
 * - 다른 모듈(User, RBAC)에서 Department 정보를 조회할 수 있도록 제공
 * - 모듈 간 직접 의존을 방지 (Spring Modulith 규칙 준수)
 *
 * <h3>제공 정보:</h3>
 * - 부서 ID, 이름, 전체 경로
 * - 최소한의 정보만 제공하여 결합도 최소화
 *
 * @author Identity System Team
 * @version 1.0
 */
public interface OrganizationModuleApi {

    /**
     * 부서 ID로 부서 정보 조회
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID (UUID 문자열)
     * @return 부서 정보 (없으면 empty)
     */
    Optional<DepartmentInfo> getDepartmentInfo(String tenantId, String deptId);

    /**
     * 부서 정보 DTO (모듈 간 통신용)
     */
    @Getter
    @Builder
    class DepartmentInfo {
        private final String deptId;          // 부서 ID
        private final String name;            // 부서명 (예: "인바운드팀")
        private final String fullPath;        // 부서 전체 경로 (예: "넥스프론 > 고객서비스본부 > 인바운드팀")
    }
}
