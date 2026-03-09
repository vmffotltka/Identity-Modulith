package com.identitymodulith.user.infrastructure.persistence.repository;

import com.identitymodulith.user.domain.model.Agent;
import com.identitymodulith.user.domain.model.AgentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 상담사(Agent) Repository 인터페이스
 * <p>
 * Repository 패턴에 따라 도메인 모델과 데이터 액세스를 분리합니다.
 * Infrastructure Layer의 AgentRepositoryImpl에서 이 인터페이스를 구현합니다.
 * </p>
 */
public interface AgentRepository {

    Agent save(Agent agent);

    Optional<Agent> findById(UUID id);

    Optional<Agent> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    List<Agent> findByOrganizationId(String organizationId);

    List<Agent> findByOrganizationIdAndStatus(String organizationId, AgentStatus status);

    List<Agent> findAll();

    List<Agent> findAllByStatus(AgentStatus status);

    /**
     * ID와 테넌트 ID로 상담사를 조회합니다.
     * 테넌트 격리를 위해 두 조건을 모두 확인합니다.
     *
     * @param agentId 상담사 ID
     * @param tenantId 테넌트 ID
     * @return 조회된 상담사 (없거나 테넌트 불일치 시 empty)
     */
    Optional<Agent> findByIdAndTenantId(UUID agentId, String tenantId);

    /**
     * 테넌트 ID와 상담사 ID로 상담사를 조회합니다.
     * 테넌트 격리를 위해 두 조건을 모두 확인합니다.
     *
     * @param tenantId 테넌트 ID
     * @param agentId 상담사 ID
     * @return 조회된 상담사 (없거나 테넌트 불일치 시 empty)
     */
    Optional<Agent> findByTenantIdAndAgentId(String tenantId, UUID agentId);

    /**
     * 퇴사 예정일이 현재 시간 이전인 RETIRED 상담사 목록을 조회합니다.
     * 배치 작업에서 자동 삭제 대상을 찾기 위해 사용됩니다.
     *
     * @param beforeDateTime 비교 대상 시간
     * @return 삭제 예정인 RETIRED 상담사 목록
     */
    List<Agent> findRetiredWithScheduledDelete(LocalDateTime beforeDateTime);

    /**
     * 예약 삭제 대상 상담사 목록을 조회합니다.
     * status가 RETIRED이고 scheduledDeleteAt이 현재 시간 이전인 상담사를 반환합니다.
     *
     * @param status 상담사 상태 (RETIRED)
     * @param now 현재 시간
     * @return 예약 삭제 대상 상담사 목록
     */
    List<Agent> findAgentsForScheduledDelete(AgentStatus status, LocalDateTime now);

    /**
     * 특정 상태의 상담사 수를 조회합니다.
     * 통계 및 모니터링 용도로 사용됩니다.
     *
     * @param status 상담사 상태
     * @return 해당 상태의 상담사 수
     */
    long countByStatus(AgentStatus status);

    /**
     * 테넌트의 모든 상담사를 조회합니다.
     *
     * @param tenantId 테넌트 ID
     * @return 해당 테넌트의 모든 상담사 목록
     */
    List<Agent> findByTenantId(String tenantId);

    /**
     * 이름에 특정 키워드가 포함된 상담사를 검색합니다.
     *
     * @param nameKeyword 검색 키워드 (부분 일치)
     * @return 검색된 상담사 목록
     */
    List<Agent> findByNameContaining(String nameKeyword);

    /**
     * 로그인 ID에 특정 키워드가 포함된 상담사를 검색합니다.
     *
     * @param loginIdKeyword 검색 키워드 (부분 일치)
     * @return 검색된 상담사 목록
     */
    List<Agent> findByLoginIdContaining(String loginIdKeyword);

    /**
     * 테넌트와 상태로 상담사를 필터링합니다.
     *
     * @param tenantId 테넌트 ID
     * @param status 상담사 상태
     * @return 필터링된 상담사 목록
     */
    List<Agent> findByTenantIdAndStatus(String tenantId, AgentStatus status);

    /**
     * 테넌트와 조직 ID로 상담사를 조회합니다.
     *
     * @param tenantId 테넌트 ID
     * @param organizationId 조직 ID
     * @return 해당 조직의 상담사 목록
     */
    List<Agent> findByTenantIdAndOrganizationId(String tenantId, String organizationId);

    /**
     * 여러 조직 ID에 속한 특정 상태의 상담사를 단일 IN 쿼리로 조회합니다. (N+1 방지)
     *
     * @param tenantId        테넌트 ID
     * @param organizationIds 조직 ID 목록
     * @param status          상담사 상태
     * @return 해당 조직들에 속한 상담사 목록
     */
    List<Agent> findByTenantIdAndOrganizationIdsAndStatus(String tenantId, List<String> organizationIds, AgentStatus status);
}
