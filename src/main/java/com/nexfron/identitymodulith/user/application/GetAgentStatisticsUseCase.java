package com.nexfron.identitymodulith.user.application;

import com.nexfron.identitymodulith.user.domain.model.AgentStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 상담사 통계 조회 Use Case
 *
 * AG-021: 대시보드에서 실시간 통계 제공
 */
public interface GetAgentStatisticsUseCase {

    /**
     * 테넌트별 전체 상담사 통계 조회
     */
    AgentStatistics getStatistics(String tenantId);

    /**
     * 조직별 상담사 통계 조회
     */
    AgentStatistics getStatisticsByOrganization(String tenantId, String organizationId);

    /**
     * 상담사 통계 정보
     */
    @Getter
    @Builder
    class AgentStatistics {
        private final int totalCount;              // 전체 상담사 수
        private final int activeCount;             // 활성 상담사 수 (ACTIVE)
        private final int suspendedCount;          // 정지된 상담사 수 (SUSPENDED)
        private final int retiredCount;            // 퇴사 상담사 수 (RETIRED)
        private final int passwordChangeRequired;  // 비밀번호 변경 필요 상담사 수
        private final Map<String, Integer> byOrganization;  // 조직별 상담사 수
        private final Map<String, Integer> byStatus;        // 상태별 상담사 수
    }
}
