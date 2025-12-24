package com.nexfron.identitymodulith.user.application;

import com.nexfron.identitymodulith.user.AgentExternalInfo;
import com.nexfron.identitymodulith.user.UserModuleApi;
import com.nexfron.identitymodulith.user.domain.model.Agent;
import com.nexfron.identitymodulith.user.domain.model.Agent.Role;
import com.nexfron.identitymodulith.user.domain.model.AgentStatus;
import com.nexfron.identitymodulith.user.domain.exception.BusinessException;
import com.nexfron.identitymodulith.user.domain.exception.ErrorCode;
import com.nexfron.identitymodulith.user.domain.repository.AgentRepository;
import com.nexfron.identitymodulith.user.domain.service.PasswordEncoder;
import com.nexfron.identitymodulith.user.domain.service.PasswordGenerator;
import com.nexfron.identitymodulith.user.infrastructure.retry.DatabaseRetrySupplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 상담사(Agent) 관련 비즈니스 로직을 처리하는 Application Service 클래스.
 * 상담사 생성, 비밀번호 초기화, 정보 수정, 퇴사 처리, 조회, 역할 관리 등의 유스케이스를 구현합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AgentService implements
        CreateAgentUseCase,
        ResetPasswordUseCase,
        UpdateAgentUseCase,
        RetireAgentUseCase,
        GetAgentUseCase,
        ManageRoleUseCase,
        CheckLoginIdUseCase,
        UserModuleApi {

    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;

    /**
     * 새로운 상담사를 생성합니다.
     * 시스템이 임시 비밀번호를 자동 생성하며, 생성된 상담사는 첫 로그인 시 비밀번호 변경이 필요합니다.
     *
     * @param command 상담사 생성에 필요한 정보 (로그인ID, 이름, 조직ID)
     * @return 생성된 상담사 ID, 로그인ID, 임시 비밀번호를 포함한 결과
     * @throws BusinessException 로그인ID가 이미 존재하는 경우 (ErrorCode.DUPLICATE_USERNAME)
     */
    @Override
    public CreateAgentResult createAgent(CreateAgentCommand command) {
        if (!isLoginIdUnique(command.getLoginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        String tempPassword = passwordGenerator.generateTempPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);

        Agent agent = Agent.builder()
                .tenantId(command.getTenantId())
                .loginId(command.getLoginId())
                .password(encodedPassword)
                .name(command.getName())
                .organizationId(command.getOrganizationId())
                .status(AgentStatus.ACTIVE)
                .passwordMustChange(true)
                .build();

        Agent savedAgent = saveAgent(agent);

        return CreateAgentResult.builder()
                .agentId(savedAgent.getId())
                .loginId(savedAgent.getLoginId())
                .tempPassword(tempPassword)
                .build();
    }

    /**
     * 상담사의 비밀번호를 초기화합니다.
     * 새로운 임시 비밀번호가 생성되며, 상담사는 다음 로그인 시 비밀번호 변경이 필요합니다.
     *
     * @param agentId 비밀번호를 초기화할 상담사 ID
     * @return 상담사 ID와 새로 생성된 임시 비밀번호를 포함한 결과
     * @throws BusinessException 상담사를 찾을 수 없는 경우 (ErrorCode.AGENT_NOT_FOUND)
     */
    @Override
    public ResetPasswordResult resetPassword(UUID agentId) {
        Agent agent = findAgentById(agentId);

        String tempPassword = passwordGenerator.generateTempPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);

        agent.resetPassword(encodedPassword);
        saveAgent(agent);

        return ResetPasswordResult.builder()
                .agentId(agent.getId())
                .tempPassword(tempPassword)
                .build();
    }

    /**
     * 상담사의 기본 정보를 수정합니다.
     *
     * @param command 수정할 상담사 ID와 변경할 정보 (이름 등)
     * @throws BusinessException 상담사를 찾을 수 없는 경우 (ErrorCode.AGENT_NOT_FOUND)
     */
    @Override
    public void updateAgent(UpdateAgentCommand command) {
        Agent agent = findAgentById(command.getAgentId());
        agent.updateName(command.getName());
        saveAgent(agent);
    }

    /**
     * 상담사를 다른 조직으로 이동시킵니다.
     *
     * @param agentId 이동할 상담사 ID
     * @param newOrganizationId 새로 배정될 조직 ID
     * @throws BusinessException 상담사를 찾을 수 없는 경우 (ErrorCode.AGENT_NOT_FOUND)
     */
    @Override
    public void transferOrganization(UUID agentId, String newOrganizationId) {
        Agent agent = findAgentById(agentId);
        agent.transferOrganization(newOrganizationId);
        saveAgent(agent);
    }

    /**
     * 상담사를 퇴사 처리합니다 (Soft Delete).
     * 상태가 RETIRED로 변경되며, 퇴사 일시가 기록됩니다.
     * 퇴사 처리된 상담사는 로그인이 차단되고 상담 배정에서 제외됩니다.
     *
     * @param agentId 퇴사 처리할 상담사 ID
     * @throws BusinessException 상담사를 찾을 수 없는 경우 (ErrorCode.AGENT_NOT_FOUND)
     */
    @Override
    public void retireAgent(UUID agentId) {
        Agent agent = findAgentById(agentId);
        agent.retire();
        saveAgent(agent);
    }

    /**
     * 특정 상담사의 상세 정보를 조회합니다.
     *
     * @param agentId 조회할 상담사 ID
     * @return 상담사 상세 정보 (비밀번호 해시값 제외)
     * @throws BusinessException 상담사를 찾을 수 없는 경우 (ErrorCode.AGENT_NOT_FOUND)
     */
    @Override
    @Transactional(readOnly = true)
    public AgentInfo getAgent(UUID agentId) {
        Agent agent = findAgentById(agentId);
        return toAgentInfo(agent);
    }

    /**
     * 검색 조건에 따라 상담사 목록을 조회합니다.
     * 기본적으로 ACTIVE 상태의 상담사만 조회되며, 퇴사자 포함 옵션을 통해 전체 조회가 가능합니다.
     *
     * @param criteria 검색 조건 (조직 ID, 퇴사자 포함 여부 등)
     * @return 검색 조건에 맞는 상담사 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<AgentInfo> getAgents(AgentSearchCriteria criteria) {
        List<Agent> agents;

        if (criteria.getOrganizationId() != null) {
            if (criteria.isIncludeRetired()) {
                agents = DatabaseRetrySupplier.withRetry(
                        () -> agentRepository.findByOrganizationId(criteria.getOrganizationId()));
            } else {
                agents = DatabaseRetrySupplier.withRetry(
                        () -> agentRepository.findByOrganizationIdAndStatus(
                                criteria.getOrganizationId(), AgentStatus.ACTIVE));
            }
        } else {
            if (criteria.isIncludeRetired()) {
                agents = DatabaseRetrySupplier.withRetry(agentRepository::findAll);
            } else {
                agents = DatabaseRetrySupplier.withRetry(
                        () -> agentRepository.findAllByStatus(AgentStatus.ACTIVE));
            }
        }

        return agents.stream()
                .map(this::toAgentInfo)
                .toList();
    }

    /**
     * 상담사에게 역할(Role)을 지정합니다.
     * 기존에 할당된 역할은 모두 제거되고 새로운 역할 세트로 대체됩니다.
     *
     * @param agentId 역할을 지정할 상담사 ID
     * @param roles 지정할 역할 세트 (직급, 채널 등)
     */
    @Override
    public void assignRoles(UUID agentId, Set<Role> roles) {
        Agent agent = findAgentById(agentId);
        agent.getRoles().clear();
        roles.forEach(agent::addRole);
        saveAgent(agent);
    }

    /**
     * 로그인 ID의 중복 여부를 확인합니다.
     *
     * @param loginId 확인할 로그인 ID
     * @return 사용 가능하면 true, 이미 존재하면 false
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isLoginIdUnique(String loginId) {
        return DatabaseRetrySupplier.withRetry(() -> !agentRepository.existsByLoginId(loginId));
    }

    /**
     * ID로 상담사를 조회하는 내부 헬퍼 메서드.
     * DB 연결 실패 시 재시도를 수행합니다.
     *
     * @param agentId 조회할 상담사 ID
     * @return 조회된 상담사 엔티티
     * @throws BusinessException 상담사를 찾을 수 없는 경우 (ErrorCode.AGENT_NOT_FOUND)
     */
    private Agent findAgentById(UUID agentId) {
        return DatabaseRetrySupplier.withRetry(
                () -> agentRepository.findById(agentId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND))
        );
    }

    /**
     * Agent 도메인 객체를 AgentInfo DTO로 변환하는 내부 헬퍼 메서드.
     * 비밀번호 해시값은 DTO에 포함되지 않습니다.
     *
     * @param agent 변환할 Agent 도메인 객체
     * @return 변환된 AgentInfo DTO
     */
    private AgentInfo toAgentInfo(Agent agent) {
        return AgentInfo.builder()
                .id(agent.getId())
                .loginId(agent.getLoginId())
                .name(agent.getName())
                .organizationId(agent.getOrganizationId())
                .status(agent.getStatus())
                .passwordMustChange(agent.isPasswordMustChange())
                .createdAt(agent.getCreatedAt())
                .retiredAt(agent.getRetiredAt())
                .roles(agent.getRoles())
                .build();
    }

    /**
     * Agent를 저장하는 내부 헬퍼 메서드.
     * DB 연결 실패 시 재시도를 수행합니다.
     *
     * @param agent 저장할 Agent 엔티티
     * @return 저장된 Agent 엔티티
     */
    private Agent saveAgent(Agent agent) {
        return DatabaseRetrySupplier.withRetry(() -> agentRepository.save(agent));
    }

    // ========== UserModuleApi 구현 (모듈 간 통신용 Public API) ==========

    /**
     * 특정 부서에 속한 활성 상태의 상담사 목록을 조회합니다.
     * Organization 모듈에서 부서 삭제 가능 여부 판단 시 사용됩니다.
     *
     * @param tenantId       테넌트 ID
     * @param organizationId 부서(조직) ID
     * @return 해당 부서의 활성 상담사 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<AgentExternalInfo> findActiveAgentsByOrganizationId(String tenantId, String organizationId) {
        return DatabaseRetrySupplier.withRetry(
                () -> agentRepository.findByOrganizationIdAndStatus(organizationId, AgentStatus.ACTIVE)
        ).stream()
                .filter(agent -> tenantId.equals(agent.getTenantId()))
                .map(this::toAgentExternalInfo)
                .collect(Collectors.toList());
    }

    /**
     * 상담사 ID로 상담사 정보를 조회합니다.
     * Organization 모듈에서 조직 스코프 계산에 필요한 사용자 정보 조회 시 사용됩니다.
     *
     * @param tenantId 테넌트 ID
     * @param agentId  상담사 UUID
     * @return 상담사 정보 (없거나 테넌트 불일치 시 empty)
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AgentExternalInfo> findAgentById(String tenantId, UUID agentId) {
        return DatabaseRetrySupplier.withRetry(
                () -> agentRepository.findByTenantIdAndAgentId(tenantId, agentId)
        ).map(this::toAgentExternalInfo);
    }

    /**
     * Agent 도메인 객체를 AgentExternalInfo DTO로 변환합니다.
     * 모듈 간 통신에 필요한 최소한의 정보만 포함됩니다.
     *
     * @param agent 변환할 Agent 도메인 객체
     * @return 변환된 AgentExternalInfo DTO
     */
    private AgentExternalInfo toAgentExternalInfo(Agent agent) {
        Set<AgentExternalInfo.RoleInfo> roleInfos = agent.getRoles().stream()
                .map(role -> new AgentExternalInfo.RoleInfo(
                        role.getName(),
                        AgentExternalInfo.RoleInfo.RoleType.valueOf(role.getType().name())
                ))
                .collect(Collectors.toSet());

        return AgentExternalInfo.builder()
                .id(agent.getId())
                .tenantId(agent.getTenantId())
                .organizationId(agent.getOrganizationId())
                .roles(roleInfos)
                .active(agent.isActive())
                .build();
    }
}
