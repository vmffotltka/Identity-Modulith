package com.identitymodulith.user.application;

import com.identitymodulith.common.security.context.TenantContextHolder;
import com.identitymodulith.rbac.RbacModuleApi;
import com.identitymodulith.user.AgentExternalInfo;
import com.identitymodulith.user.UserModuleApi;
import com.identitymodulith.user.application.port.OrganizationPort;
import com.identitymodulith.user.application.port.RbacPort;
import com.identitymodulith.user.domain.model.Agent;
import com.identitymodulith.user.domain.model.Agent.Role;
import com.identitymodulith.user.domain.model.AgentStatus;
import com.identitymodulith.user.domain.exception.BusinessException;
import com.identitymodulith.user.domain.exception.ErrorCode;
import com.identitymodulith.user.infrastructure.persistence.repository.AgentRepository;
import com.identitymodulith.user.domain.service.PasswordEncoder;
import com.identitymodulith.user.domain.service.PasswordGenerator;
import com.identitymodulith.user.infrastructure.retry.DatabaseRetrySupplier;
import com.identitymodulith.user.domain.event.AgentActivatedEvent;
import com.identitymodulith.user.domain.event.AgentRetiredEvent;
import com.identitymodulith.user.domain.event.AgentSuspendedEvent;
import com.identitymodulith.user.domain.event.AgentTransferredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** 상담사 관련 유스케이스 구현체. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AgentService implements
        CreateAgentUseCase,
        ResetPasswordUseCase,
        ChangePasswordUseCase,
        UpdateAgentUseCase,
        RetireAgentUseCase,
        SuspendAgentUseCase,
        ActivateAgentUseCase,
        TransferAgentUseCase,
        GetAgentUseCase,
        GetAgentStatisticsUseCase,
        ManageRoleUseCase,
        CheckLoginIdUseCase,
        UserModuleApi {

    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final OrganizationPort organizationPort;
    private final RbacModuleApi rbacModuleApi;
    private final RbacPort rbacPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public CreateAgentResult createAgent(CreateAgentCommand command) {
        command.validate();

        if (!isLoginIdUnique(command.getLoginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        String tempPassword = passwordGenerator.generateTempPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);

        String tenantId = TenantContextHolder.getCurrentTenantId();

        Agent agent = Agent.builder()
                .tenantId(tenantId)
                .loginId(command.getLoginId())
                .password(encodedPassword)
                .name(command.getName())
                .organizationId(command.getOrganizationId())
                .status(AgentStatus.ACTIVE)
                .passwordMustChange(true)
                .roles(command.getRoles())
                .build();

        Agent savedAgent = saveAgent(agent);

        log.info("[USER] 상담사 생성 완료 - agentId={}, loginId={}", savedAgent.getId(), savedAgent.getLoginId());

        return CreateAgentResult.builder()
                .agentId(savedAgent.getId())
                .loginId(savedAgent.getLoginId())
                .tempPassword(tempPassword)
                .build();
    }

    @Override
    public ResetPasswordResult resetPassword(String tenantId, UUID agentId, UUID actorId) {
        Agent agent = agentRepository.findByIdAndTenantId(agentId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        boolean isAdmin = rbacPort.hasRole(actorId.toString(), "ADMIN");

        if (!isAdmin) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "관리자만 비밀번호를 초기화할 수 있습니다.");
        }

        String tempPassword = passwordGenerator.generateTempPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);

        agent.resetPassword(encodedPassword);

        return ResetPasswordResult.builder()
                .agentId(agent.getId())
                .tempPassword(tempPassword)
                .build();
    }

    /** 본인 비밀번호를 변경한다. */
    @Override
    public void changePassword(ChangePasswordUseCase.ChangePasswordCommand command) {
        log.info("[USER] 비밀번호 변경 시작 - agentId={}, actorId={}", command.getAgentId(), command.getActorId());

        if (!command.getAgentId().equals(command.getActorId())) {
            log.warn("[USER] 본인 확인 실패 - agentId={}, actorId={}", command.getAgentId(), command.getActorId());
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "다른 사용자의 비밀번호를 변경할 수 없습니다.");
        }
        log.debug("[USER] 본인 확인 통과");

        Agent agent = agentRepository.findByIdAndTenantId(command.getAgentId(), command.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));
        log.debug("[USER] 상담사 조회 성공 - loginId={}", agent.getLoginId());

        log.debug("[USER] 현재 비밀번호 검증 시작");
        boolean passwordMatches = passwordEncoder.matches(command.getCurrentPassword(), agent.getPassword());
        log.debug("[USER] 비밀번호 일치 여부: {}", passwordMatches);

        if (!passwordMatches) {
            log.warn("[USER] 현재 비밀번호 불일치 - agentId={}", command.getAgentId());
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH,
                    "현재 비밀번호가 일치하지 않습니다.");
        }
        log.debug("[USER] 현재 비밀번호 검증 통과");

        if (command.getCurrentPassword().equals(command.getNewPassword())) {
            log.warn("[USER] 새 비밀번호가 현재 비밀번호와 동일 - agentId={}", command.getAgentId());
            throw new BusinessException(ErrorCode.SAME_AS_CURRENT_PASSWORD,
                    "새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }
        log.debug("[USER] 새 비밀번호 검증 통과");

        String encodedPassword = passwordEncoder.encode(command.getNewPassword());
        agent.changePassword(encodedPassword);
        log.debug("[USER] 비밀번호 암호화 및 변경 완료");

        agentRepository.save(agent);
        log.debug("[USER] 저장 완료");

        log.info("[USER] 비밀번호 변경 완료 - agentId={}", command.getAgentId());
    }

    @Override
    public void updateAgent(UpdateAgentCommand command) {
        Agent agent = agentRepository.findByIdAndTenantId(command.getAgentId(), command.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        boolean isAdmin = rbacPort.hasRole(command.getActorId().toString(), "ADMIN");
        boolean isSelf = command.getActorId().equals(command.getAgentId());

        if (!isAdmin && !isSelf) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "본인 또는 관리자만 상담사 정보를 수정할 수 있습니다.");
        }

        agent.updateName(command.getName());
    }

    @Override
    public void transferOrganization(String tenantId, UUID agentId, UUID actorId, String newOrganizationId) {
        Agent agent = agentRepository.findByIdAndTenantId(agentId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        organizationPort.getDepartmentInfo(tenantId, newOrganizationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND,
                        "이동할 부서를 찾을 수 없습니다."));

        boolean isAdmin = rbacPort.hasRole(actorId.toString(), "ADMIN");

        if (!isAdmin) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "관리자만 상담사 조직을 이동시킬 수 있습니다.");
        }

        agent.transferOrganization(newOrganizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentInfo getAgent(UUID agentId) {
        Agent agent = findAgentById(agentId);
        return toAgentInfo(agent);
    }

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
                .filter(agent -> criteria.getStatus() == null || agent.getStatus() == criteria.getStatus())
                .filter(agent -> criteria.getNameKeyword() == null ||
                        agent.getName().contains(criteria.getNameKeyword()))
                .filter(agent -> criteria.getLoginIdKeyword() == null ||
                        agent.getLoginId().contains(criteria.getLoginIdKeyword()))
                .map(this::toAgentInfo)
                .toList();
    }

    /** 상담사 역할을 일괄 교체한다. */
    @Override
    public void assignRoles(UUID agentId, Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, 
                "최소 1개 이상의 역할을 지정해야 합니다.");
        }

        Agent agent = findAgentById(agentId);
        agent.getRoles().clear();
        roles.forEach(agent::addRole);
        saveAgent(agent);
    }

    /** 역할 ID/이름 혼합 입력을 허용해 역할을 일괄 교체한다. */
    @Override
    public void assignRolesByIds(UUID agentId, Set<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                "최소 1개 이상의 역할을 지정해야 합니다.");
        }

        log.info("[USER] 역할 ID로 일괄 지정 - agentId={}, roleIds={}", agentId, roleIds);

        rbacPort.removeAllRolesFromAgent(agentId.toString());
        log.debug("[USER] 기존 역할 제거 완료 - agentId={}", agentId);

        for (String roleId : roleIds) {
            if (roleId != null && roleId.length() == 36 && roleId.contains("-")) {
                rbacPort.assignRoleToAgentByRoleId(agentId.toString(), roleId);
            } else {
                rbacPort.assignRoleToAgent(agentId.toString(), roleId);
            }
        }

        log.info("[USER] 역할 ID로 일괄 지정 완료 - agentId={}, roleCount={}", agentId, roleIds.size());
    }

    /** 역할 이름으로 역할을 일괄 교체한다. */
    @Override
    public void assignRolesByNames(UUID agentId, Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                "최소 1개 이상의 역할을 지정해야 합니다.");
        }

        log.info("[USER] 역할 이름으로 일괄 지정 - agentId={}, roleNames={}", agentId, roleNames);

        // RA-003: POSITION 역할은 1개만 허용한다.
        Set<String> positionRoleNames = Set.of("ADMIN", "TEAM_LEAD", "MEMBER", "AGENT");
        long positionCount = roleNames.stream()
                .filter(positionRoleNames::contains)
                .count();

        if (positionCount > 1) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "POSITION 역할(ADMIN, TEAM_LEAD, MEMBER)은 1개만 할당할 수 있습니다.");
        }

        rbacPort.removeAllRolesFromAgent(agentId.toString());
        log.debug("[USER] 기존 역할 제거 완료 - agentId={}", agentId);

        for (String roleName : roleNames) {
            rbacPort.assignRoleToAgentWithoutAutoReplace(agentId.toString(), roleName);
        }

        log.info("[USER] 역할 이름으로 일괄 지정 완료 - agentId={}, roleCount={}", agentId, roleNames.size());
    }

    @Override
    public void validateAdminPermission(String tenantId, UUID actorId) {
        boolean isAdmin = rbacPort.hasRole(actorId.toString(), "ADMIN");

        if (!isAdmin) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "관리자만 이 작업을 수행할 수 있습니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLoginIdUnique(String loginId) {
        return DatabaseRetrySupplier.withRetry(() -> !agentRepository.existsByLoginId(loginId));
    }

    private Agent findAgentById(UUID agentId) {
        return DatabaseRetrySupplier.withRetry(
                () -> agentRepository.findById(agentId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND))
        );
    }

    private AgentInfo toAgentInfo(Agent agent) {
        String departmentName = null;
        String departmentPath = null;

        if (agent.getOrganizationId() != null && !agent.getOrganizationId().isEmpty()) {
            Optional<OrganizationPort.DepartmentInfo> deptInfo =
                organizationPort.getDepartmentInfo(agent.getTenantId(), agent.getOrganizationId());

            if (deptInfo.isPresent()) {
                departmentName = deptInfo.get().getName();
                departmentPath = deptInfo.get().getPath();
            }
        }

        return AgentInfo.builder()
                .id(agent.getId())
                .loginId(agent.getLoginId())
                .name(agent.getName())
                .organizationId(agent.getOrganizationId())
                .departmentName(departmentName)
                .departmentPath(departmentPath)
                .employeeId(agent.getEmployeeId())
                .email(agent.getEmail())
                .phone(agent.getPhone())
                .status(agent.getStatus())
                .passwordMustChange(agent.isPasswordMustChange())
                .createdAt(agent.getCreatedAt())
                .retiredAt(agent.getRetiredAt())
                .roles(agent.getRoles())
                .build();
    }

    private Agent saveAgent(Agent agent) {
        return DatabaseRetrySupplier.withRetry(() -> agentRepository.save(agent));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentExternalInfo> findActiveAgentsByOrganizationId(String tenantId, String organizationId) {
        return DatabaseRetrySupplier.withRetry(
                () -> agentRepository.findByOrganizationIdAndStatus(organizationId, AgentStatus.ACTIVE)
        ).stream()
                .filter(agent -> tenantId.equals(agent.getTenantId()))
                .map(this::toAgentExternalInfo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentExternalInfo> findActiveAgentsByOrganizationIds(String tenantId, List<String> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return List.of();
        }
        return DatabaseRetrySupplier.withRetry(
                () -> agentRepository.findByTenantIdAndOrganizationIdsAndStatus(
                        tenantId, organizationIds, AgentStatus.ACTIVE)
        ).stream()
                .map(this::toAgentExternalInfo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AgentExternalInfo> findAgentById(String tenantId, UUID agentId) {
        return DatabaseRetrySupplier.withRetry(
                () -> agentRepository.findByTenantIdAndAgentId(tenantId, agentId)
        ).map(this::toAgentExternalInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AgentExternalInfo> findAgentByLoginId(String loginId) {
        return DatabaseRetrySupplier.withRetry(
                () -> agentRepository.findByLoginId(loginId)
        ).map(this::toAgentExternalInfo);
    }

    private AgentExternalInfo toAgentExternalInfo(Agent agent) {
        Set<RbacModuleApi.RoleInfo> rbacRoles = rbacModuleApi.getRolesByAgentId(agent.getId().toString());

        Set<AgentExternalInfo.RoleInfo> roleInfos = rbacRoles.stream()
                .map(rbacRole -> {
                    AgentExternalInfo.RoleInfo.RoleType roleType;
                    try {
                        roleType = AgentExternalInfo.RoleInfo.RoleType.valueOf(rbacRole.getType().name());
                    } catch (IllegalArgumentException e) {
                        roleType = AgentExternalInfo.RoleInfo.RoleType.POSITION;
                    }

                    return new AgentExternalInfo.RoleInfo(
                            rbacRole.getName(),
                            roleType
                    );
                })
                .collect(Collectors.toSet());

        return AgentExternalInfo.builder()
                .id(agent.getId())
                .tenantId(agent.getTenantId())
                .loginId(agent.getLoginId())
                .name(agent.getName())
                .email(agent.getEmail())
                .employeeId(agent.getEmployeeId())
                .organizationId(agent.getOrganizationId())
                .roles(roleInfos)
                .active(agent.isActive())
                .build();
    }

    @Override
    public void suspendAgent(SuspendAgentUseCase.SuspendAgentCommand command) {
        if (command.getActorId().equals(command.getAgentId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "본인을 정지할 수 없습니다.");
        }

        Agent agent = agentRepository.findByIdAndTenantId(command.getAgentId(), command.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        if (!agent.getStatus().name().equals("ACTIVE")) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "ACTIVE 상태인 상담사만 정지할 수 있습니다.");
        }

        agent.suspend(command.getActorId().toString());
        agentRepository.save(agent);

        eventPublisher.publishEvent(new AgentSuspendedEvent(
                agent.getId(), command.getTenantId(),
                command.getActorId().toString(), agent.getSuspendedAt()));
        log.info("[USER] AgentSuspendedEvent 발행 - agentId={}", agent.getId());
    }

    @Override
    public void activateAgent(ActivateAgentUseCase.ActivateAgentCommand command) {
        Agent agent = agentRepository.findByIdAndTenantId(command.getAgentId(), command.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        if (agent.getStatus().name().equals("RETIRED")) {
            throw new BusinessException(ErrorCode.AGENT_ALREADY_RETIRED, "퇴사한 상담사는 복구할 수 없습니다.");
        }

        if (!agent.getStatus().name().equals("SUSPENDED")) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "SUSPENDED 상태인 상담사만 활성화할 수 있습니다.");
        }

        agent.activate();
        agentRepository.save(agent);

        eventPublisher.publishEvent(new AgentActivatedEvent(
                agent.getId(), command.getTenantId(),
                command.getActorId().toString(), LocalDateTime.now()));
        log.info("[USER] AgentActivatedEvent 발행 - agentId={}", agent.getId());
    }

    @Override
    public void retireAgent(RetireAgentUseCase.RetireAgentCommand command) {
        if (command.getActorId().equals(command.getAgentId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "본인을 퇴사 처리할 수 없습니다.");
        }

        if (command.getDeletePolicy() == RetireAgentUseCase.RetireDeletePolicy.SCHEDULED
                && command.getRetentionDays() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "SCHEDULED 정책인 경우 retentionDays는 필수입니다.");
        }

        Agent agent = agentRepository.findByIdAndTenantId(command.getAgentId(), command.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        if (agent.getStatus().name().equals("RETIRED")) {
            throw new BusinessException(ErrorCode.AGENT_ALREADY_RETIRED, "이미 퇴사한 상담사입니다.");
        }

        Agent.RetireDeletePolicy agentDeletePolicy = Agent.RetireDeletePolicy.valueOf(command.getDeletePolicy().name());
        agent.retire(command.getActorId().toString(), agentDeletePolicy, command.getRetentionDays());

        if (command.getDeletePolicy() == RetireAgentUseCase.RetireDeletePolicy.IMMEDIATE) {
            agent.anonymize();
        } else if (command.getDeletePolicy() == RetireAgentUseCase.RetireDeletePolicy.SCHEDULED) {
            agent.setScheduledDeleteAt(
                    LocalDateTime.now().plusDays(command.getRetentionDays()));
        }

        agentRepository.save(agent);

        eventPublisher.publishEvent(new AgentRetiredEvent(
                agent.getId(), command.getTenantId(),
                command.getActorId().toString(),
                command.getDeletePolicy().name(),
                agent.getRetiredAt()));
        log.info("[USER] AgentRetiredEvent 발행 - agentId={}, policy={}", agent.getId(), command.getDeletePolicy());
    }

    /** 상담사를 다른 조직으로 이동한다. */
    @Override
    public TransferAgentUseCase.TransferAgentResult transferAgent(
            TransferAgentUseCase.TransferAgentCommand command) {

        log.info("[USER] 상담사 부서 이동 시작 - agentId={}, newOrganizationId={}",
                command.getAgentId(), command.getNewOrganizationId());

        Agent agent = agentRepository.findByIdAndTenantId(command.getAgentId(), command.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        // T-001: 대상 부서가 존재해야 한다.
        organizationPort.getDepartmentInfo(command.getTenantId(), command.getNewOrganizationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND,
                        "이동할 부서를 찾을 수 없습니다."));

        // T-003: RETIRED 상담사는 이동할 수 없다.
        if (agent.getStatus() == AgentStatus.RETIRED) {
            throw new BusinessException(ErrorCode.AGENT_ALREADY_RETIRED, "퇴사한 상담사는 부서를 이동할 수 없습니다.");
        }

        // T-002: 동일 부서로의 이동은 차단한다.
        if (agent.getOrganizationId() != null &&
            agent.getOrganizationId().equals(command.getNewOrganizationId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "이미 해당 조직에 소속되어 있습니다.");
        }

        String fromOrganizationId = agent.getOrganizationId();

        agent.transferOrganization(command.getNewOrganizationId());
        agentRepository.save(agent);

        log.info("[USER] 상담사 부서 이동 완료 - agentId={}, from={}, to={}",
                command.getAgentId(), fromOrganizationId, command.getNewOrganizationId());

        eventPublisher.publishEvent(new AgentTransferredEvent(
                agent.getId(), command.getTenantId(),
                fromOrganizationId, command.getNewOrganizationId(),
                LocalDateTime.now()));
        log.info("[USER] AgentTransferredEvent 발행 - agentId={}", agent.getId());

        return TransferAgentUseCase.TransferAgentResult.builder()
                .agentId(agent.getId())
                .fromOrganizationId(fromOrganizationId)
                .toOrganizationId(command.getNewOrganizationId())
                .transferredAt(LocalDateTime.now())
                .build();
    }

    /** 테넌트 전체 상담사 통계를 조회한다. */
    @Override
    @Transactional(readOnly = true)
    public AgentStatistics getStatistics(String tenantId) {
        log.debug("[USER] 상담사 통계 조회 - tenantId={}", tenantId);

        List<Agent> allAgents = agentRepository.findByTenantId(tenantId);

        return calculateStatistics(allAgents);
    }

    /** 조직 단위 상담사 통계를 조회한다. */
    @Override
    @Transactional(readOnly = true)
    public AgentStatistics getStatisticsByOrganization(String tenantId, String organizationId) {
        log.debug("[USER] 조직별 상담사 통계 조회 - tenantId={}, organizationId={}", tenantId, organizationId);

        List<Agent> agents = agentRepository.findByTenantIdAndOrganizationId(tenantId, organizationId);

        return calculateStatistics(agents);
    }

    private AgentStatistics calculateStatistics(List<Agent> agents) {
        int totalCount = agents.size();
        int activeCount = 0;
        int suspendedCount = 0;
        int retiredCount = 0;
        int passwordChangeRequired = 0;

        Map<String, Integer> byOrganization = new HashMap<>();
        Map<String, Integer> byStatus = new HashMap<>();

        for (Agent agent : agents) {
            switch (agent.getStatus()) {
                case ACTIVE -> activeCount++;
                case SUSPENDED -> suspendedCount++;
                case RETIRED -> retiredCount++;
            }

            if (agent.isPasswordMustChange()) {
                passwordChangeRequired++;
            }

            String orgId = agent.getOrganizationId();
            if (orgId != null) {
                byOrganization.merge(orgId, 1, Integer::sum);
            }

            byStatus.merge(agent.getStatus().name(), 1, Integer::sum);
        }

        return AgentStatistics.builder()
                .totalCount(totalCount)
                .activeCount(activeCount)
                .suspendedCount(suspendedCount)
                .retiredCount(retiredCount)
                .passwordChangeRequired(passwordChangeRequired)
                .byOrganization(byOrganization)
                .byStatus(byStatus)
                .build();
    }
}
