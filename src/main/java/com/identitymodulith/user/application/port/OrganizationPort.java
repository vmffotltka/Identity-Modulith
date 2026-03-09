package com.identitymodulith.user.application.port;

import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

/**
 * User 모듈에서 Organization 모듈의 Department 정보를 조회하기 위한 Port
 *
 * <h3>목적:</h3>
 * - Agent 조회 시 소속 부서의 이름과 경로를 제공하기 위함
 * - 모듈 간 직접 의존을 방지 (Port-Adapter 패턴)
 *
 * <h3>사용처:</h3>
 * - AgentService.getAgent() - Agent 상세 조회 시 부서 정보 포함
 * - AgentService.getAgents() - Agent 목록 조회 시 부서 정보 포함
 *
 * @author Identity System Team
 * @version 1.0
 */
public interface OrganizationPort {

    /**
     * 부서 ID로 부서 정보 조회
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID (UUID 문자열)
     * @return 부서 정보 (없으면 empty)
     */
    Optional<DepartmentInfo> getDepartmentInfo(String tenantId, String deptId);

    /**
     * 부서 정보 DTO
     */
    @Getter
    @Builder
    class DepartmentInfo {
        private final String deptId;          // 부서 ID
        private final String name;            // 부서명 (예: "인바운드팀")
        private final String path;            // 부서 전체 경로 (예: "넥스프론 > 고객서비스본부 > 인바운드팀")
    }
}
