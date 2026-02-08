package com.nexfron.identitymodulith.user.application;

import com.nexfron.identitymodulith.common.security.TenantContextHolder;
import com.nexfron.identitymodulith.rbac.RbacModuleApi;
import com.nexfron.identitymodulith.user.AgentExternalInfo;
// ...existing code...
import com.nexfron.identitymodulith.user.UserModuleApi;
import com.nexfron.identitymodulith.user.application.port.OrganizationPort;
import com.nexfron.identitymodulith.user.domain.model.Agent;
import com.nexfron.identitymodulith.user.domain.model.Agent.Role;
import com.nexfron.identitymodulith.user.domain.model.AgentStatus;
import com.nexfron.identitymodulith.user.domain.exception.BusinessException;
import com.nexfron.identitymodulith.user.domain.exception.ErrorCode;
import com.nexfron.identitymodulith.user.infrastructure.persistence.repository.AgentRepository;
import com.nexfron.identitymodulith.user.domain.service.PasswordEncoder;
import com.nexfron.identitymodulith.user.domain.service.PasswordGenerator;
import com.nexfron.identitymodulith.user.infrastructure.retry.DatabaseRetrySupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 상담사(Agent) 관련 비즈니스 로직을 처리하는 Application Service 클래스.
 * 상담사 생성, 비밀번호 초기화, 정보 수정, 퇴사 처리, 조회, 역할 관리 등의 유스케이스를 구현합니다.
 */
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
    private final OrganizationPort organizationPort;  // Organization 모듈 연동
    private final RbacModuleApi rbacModuleApi;  // RBAC 모듈 연동

    /**
     * 새로운 상담사를 생성합니다.
     * 시스템이 임시 비밀번호를 자동 생성하며, 생성된 상담사는 첫 로그인 시 비밀번호 변경이 필요합니다.
     * 현재 요청의 tenantId를 TenantContextHolder에서 자동으로 추출합니다.
     *
     * @param command 상담사 생성에 필요한 정보 (로그인ID, 이름, 조직ID)
     * @return 생성된 상담사 ID, 로그인ID, 임시 비밀번호를 포함한 결과
     * @throws BusinessException 로그인ID가 이미 존재하는 경우 (ErrorCode.DUPLICATE_USERNAME)
     */
    @Override
    public CreateAgentResult createAgent(CreateAgentCommand command) {
        // C-001~C-003 검증
        command.validate();

        // C-001: loginId 중복 검증
        if (!isLoginIdUnique(command.getLoginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        String tempPassword = passwordGenerator.generateTempPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);

        // TenantContextHolder에서 현재 요청의 tenantId를 안전하게 추출
        String tenantId = TenantContextHolder.getCurrentTenantId();

        Agent agent = Agent.builder()
                .tenantId(tenantId)
                .loginId(command.getLoginId())
                .password(encodedPassword)
                .name(command.getName())
                .organizationId(command.getOrganizationId())
                .status(AgentStatus.ACTIVE)
                .passwordMustChange(true)
                .roles(command.getRoles())  // C-003: roles 설정
                .build();

        Agent savedAgent = saveAgent(agent);

        log.info("[USER] 상담사 생성 완료 - agentId={}, loginId={}", savedAgent.getId(), savedAgent.getLoginId());

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
        // JPA dirty checking으로 자동 저장됨

        return ResetPasswordResult.builder()
                .agentId(agent.getId())
                .tempPassword(tempPassword)
                .build();
    }

    /**
     * 상담사 본인의 비밀번호를 변경합니다.
     * <p>
     * 현재 비밀번호를 알고 있는 상태에서 새 비밀번호로 변경합니다.
     * 관리자의 비밀번호 초기화(ResetPassword)와는 다릅니다.
     * </p>
     *
     * @param command 비밀번호 변경 명령 (현재 비밀번호, 새 비밀번호 포함)
     * @throws BusinessException
     *         - AGENT_NOT_FOUND: 상담사를 찾을 수 없음
     *         - INVALID_PASSWORD: 현재 비밀번호가 일치하지 않음
     *         - SAME_PASSWORD: 새 비밀번호가 현재 비밀번호와 동일함
     *         - CANNOT_CHANGE_OTHERS_PASSWORD: 본인이 아닌 다른 사용자의 비밀번호 변경 시도
     *         - AGENT_ALREADY_RETIRED: 퇴사한 상담사
     */
    @Override
    public void changePassword(ChangePasswordUseCase.ChangePasswordCommand command) {
        // 1. 본인 확인 (PC-004)
        if (!command.getAgentId().equals(command.getActorId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "다른 사용자의 비밀번호를 변경할 수 없습니다.");
        }

        // 2. 상담사 조회
        Agent agent = agentRepository.findByIdAndTenantId(command.getAgentId(), command.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        // 3. 현재 비밀번호 검증 (PC-001)
        if (!passwordEncoder.matches(command.getCurrentPassword(), agent.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "현재 비밀번호가 일치하지 않습니다.");
        }

        // 4. 새 비밀번호 != 현재 비밀번호 검증 (PC-002)
        if (command.getCurrentPassword().equals(command.getNewPassword())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        // 5. 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(command.getNewPassword());
        agent.changePassword(encodedPassword);  // PC-003: passwordMustChange = false

        // 6. 저장
        agentRepository.save(agent);

        // 7. 로깅
        log.info("[USER] 비밀번호 변경 완료 - agentId={}", command.getAgentId());
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
        // JPA dirty checking으로 자동 저장됨 (saveAgent 호출 불필요)
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
        // JPA dirty checking으로 자동 저장됨
    }

    /**
     * 상담사를 정지(Suspend)합니다.
     * ACTIVE 상태의 상담사만 정지 가능하며, 정지된 상담사는 임시로 로그인 및 상담 배정이 불가능합니다.
     * 정지된 상담사는 activate() 메서드로 복귀 가능합니다.
     *
     * 참고: 현재 공개 API는 아니며, 내부 용도로만 사용됩니다.
     *
     * @param agentId 정지할 상담사 ID
     * @param suspendedByUserId 정지를 수행한 관리자 ID
     * @throws BusinessException 상담사를 찾을 수 없거나 ACTIVE 상태가 아닌 경우
     */
    public void suspendAgent(UUID agentId, String suspendedByUserId) {
        Agent agent = findAgentById(agentId);
        agent.suspend(suspendedByUserId);
        // JPA dirty checking으로 자동 저장됨
    }

    /**
     * 정지된 상담사를 활성화합니다.
     * SUSPENDED 상태의 상담사만 활성화 가능하며, RETIRED 상태는 복구 불가능합니다.
     *
     * 참고: 현재 공개 API는 아니며, 내부 용도로만 사용됩니다.
     *
     * @param agentId 활성화할 상담사 ID
     * @param activatedByUserId 활성화를 수행한 관리자 ID (감사 로그용, 현재 미사용)
     * @throws BusinessException 상담사를 찾을 수 없거나 SUSPENDED 상태가 아닌 경우
     */
    public void activateAgentInternal(UUID agentId, String activatedByUserId) {
        Agent agent = findAgentById(agentId);
        agent.activate();
        // JPA dirty checking으로 자동 저장됨
    }

    /**
     * 퇴사 후 데이터 처리 정책을 지정하여 상담사를 퇴사 처리합니다.
     * 상태가 RETIRED로 변경되며, 퇴사 일시가 기록됩니다.
     * 퇴사 정책에 따라 다음과 같이 처리됩니다:
     * - IMMEDIATE: 즉시 개인정보 익명화
     * - SCHEDULED: retentionDays 후 자동 삭제 (배치 작업)
     * - PRESERVE: 데이터 영구 보존
     *
     * 내부 용도용 메서드입니다.
     *
     * @param agentId 퇴사 처리할 상담사 ID
     * @param retiredByUserId 퇴사를 처리한 관리자 ID
     * @param deletePolicy 퇴사 후 데이터 처리 정책
     * @param retentionDays SCHEDULED 정책일 경우 보관 기간 (일 단위)
     * @throws BusinessException 상담사를 찾을 수 없거나 이미 RETIRED인 경우
     */
    public void retireAgentWithPolicyInternal(UUID agentId, String retiredByUserId, Agent.RetireDeletePolicy deletePolicy, Integer retentionDays) {
        Agent agent = findAgentById(agentId);
        agent.retire(retiredByUserId, deletePolicy, retentionDays);
        // JPA dirty checking으로 자동 저장됨
    }

    /**
     * 퇴사 예정인 상담사들을 자동으로 삭제합니다.
     * 스케줄러에서 정기적으로 호출되는 배치 작업입니다.
     * scheduledDeleteAt이 현재 시간 이전인 RETIRED 상담사의 개인정보를 익명화합니다.
     *
     * @return 처리된 상담사 수
     */
    public int deleteScheduledRetiredAgents() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<Agent> agents = DatabaseRetrySupplier.withRetry(
                () -> agentRepository.findRetiredWithScheduledDelete(now)
        );

        agents.forEach(Agent::anonymizePersonalInfo);
        agents.forEach(this::saveAgent);

        return agents.size();
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
     * 기본적으로 ACTIVE 상태의 상담사만 조회되며, 다양한 필터링 옵션을 지원합니다.
     *
     * @param criteria 검색 조건 (조직 ID, 상태, 키워드, 퇴사자 포함 여부 등)
     * @return 검색 조건에 맞는 상담사 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<AgentInfo> getAgents(AgentSearchCriteria criteria) {
        List<Agent> agents;

        // 1. 기본 조회 (조직 ID + 상태 기반)
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

        // 2. 추가 필터링 (Java Stream)
        return agents.stream()
                // 상태 필터
                .filter(agent -> criteria.getStatus() == null || agent.getStatus() == criteria.getStatus())
                // 이름 검색 (부분 일치)
                .filter(agent -> criteria.getNameKeyword() == null ||
                        agent.getName().contains(criteria.getNameKeyword()))
                // 로그인 ID 검색 (부분 일치)
                .filter(agent -> criteria.getLoginIdKeyword() == null ||
                        agent.getLoginId().contains(criteria.getLoginIdKeyword()))
                .map(this::toAgentInfo)
                .toList();
    }

    /**
     * 상담사에게 역할(Role)을 지정합니다.
     * 기존에 할당된 역할은 모두 제거되고 새로운 역할 세트로 대체됩니다.
     *
     * <h3>검증 규칙</h3>
     * - roles 필수: 최소 1개 이상의 역할 필요
     * - 역할 종류: POSITION (직급), CHANNEL (채널)
     *
     * @param agentId 역할을 지정할 상담사 ID
     * @param roles 지정할 역할 세트 (최소 1개 필수)
     * @throws BusinessException roles가 null이거나 empty인 경우
     */
    @Override
    public void assignRoles(UUID agentId, Set<Role> roles) {
        // roles 필수 검증
        if (roles == null || roles.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, 
                "최소 1개 이상의 역할을 지정해야 합니다.");
        }

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
 * Department 정보와 연락처 정보를 포함합니다.
 *
 * @param agent 변환할 Agent 도메인 객체
 * @return 변환된 AgentInfo DTO
 */
private AgentInfo toAgentInfo(Agent agent) {
    // Department 정보 조회 (organizationPort 사용)
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

    @Override
    @Transactional(readOnly = true)
    public List<AgentExternalInfo> findActiveAgentsByOrganizationIds(String tenantId, List<String> organizationIds) {
        List<AgentExternalInfo> result = new ArrayList<>();
        for (String orgId : organizationIds) {
            result.addAll(findActiveAgentsByOrganizationId(tenantId, orgId));
        }
        return result;
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
        // RBAC 모듈에서 실제 역할 조회
        Set<RbacModuleApi.RoleInfo> rbacRoles = rbacModuleApi.getRolesByAgentId(agent.getId().toString());

        // RbacModuleApi.RoleInfo를 AgentExternalInfo.RoleInfo로 변환
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

    /**
     * 상담사를 정지(SUSPENDED) 상태로 변경합니다.
     * <p>
     * ACTIVE 상태의 상담사만 정지 가능하며, 본인은 정지할 수 없습니다.
     * 정지된 상담사는 로그인할 수 없으며 모든 활성 세션이 종료됩니다.
     * </p>
     *
     * @param command 정지 명령 (tenantId, agentId, actorId 포함)
     * @throws com.nexfron.identitymodulith.user.domain.exception.BusinessException
     *         - AGENT_NOT_FOUND: 상담사를 찾을 수 없음
     *         - INVALID_STATUS_TRANSITION: ACTIVE 상태가 아님
     *         - CANNOT_SUSPEND_SELF: 본인 정지 시도
     */
    @Override
    public void suspendAgent(SuspendAgentUseCase.SuspendAgentCommand command) {
        // 현재 사용자가 본인을 정지하려는지 확인
        if (command.getActorId().equals(command.getAgentId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "본인을 정지할 수 없습니다.");
        }

        // 상담사 조회 (tenantId 포함)
        Agent agent = agentRepository.findByIdAndTenantId(command.getAgentId(), command.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        // 상태 전이 검증: ACTIVE → SUSPENDED만 가능
        if (!agent.getStatus().name().equals("ACTIVE")) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "ACTIVE 상태인 상담사만 정지할 수 있습니다.");
        }

        // 상태 변경
        agent.suspend(command.getActorId().toString());
        agentRepository.save(agent);

        // 이벤트 발행 (KeyCloak 동기화, 세션 종료 등)
        // TODO: AgentSuspended 이벤트 발행
    }

    /**
     * 정지된(SUSPENDED) 상담사를 활성화합니다.
     * <p>
     * SUSPENDED 상태의 상담사만 활성화 가능합니다.
     * RETIRED 상태는 복구 불가능하며 활성화할 수 없습니다.
     * </p>
     *
     * @param command 활성화 명령 (tenantId, agentId, actorId 포함)
     * @throws com.nexfron.identitymodulith.user.domain.exception.BusinessException
     *         - AGENT_NOT_FOUND: 상담사를 찾을 수 없음
     *         - INVALID_STATUS_TRANSITION: SUSPENDED 상태가 아님
     *         - AGENT_ALREADY_RETIRED: 이미 퇴사한 상담사 (복구 불가)
     *         - DEPT_INACTIVE: 소속 부서가 비활성
     */
    @Override
    public void activateAgent(ActivateAgentUseCase.ActivateAgentCommand command) {
        // 상담사 조회 (tenantId 포함)
        Agent agent = agentRepository.findByIdAndTenantId(command.getAgentId(), command.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        // 상태 검증
        if (agent.getStatus().name().equals("RETIRED")) {
            throw new BusinessException(ErrorCode.AGENT_ALREADY_RETIRED, "퇴사한 상담사는 복구할 수 없습니다.");
        }

        if (!agent.getStatus().name().equals("SUSPENDED")) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "SUSPENDED 상태인 상담사만 활성화할 수 있습니다.");
        }

        // 상태 변경
        agent.activate();
        agentRepository.save(agent);

        // 이벤트 발행 (KeyCloak 동기화 등)
        // TODO: AgentActivated 이벤트 발행
    }

    /**
     * 상담사를 퇴사 처리합니다.
     * <p>
     * 상담사를 RETIRED 상태로 변경합니다. RETIRED 상태는 되돌릴 수 없습니다.
     * deletePolicy에 따라 개인정보를 처리합니다.
     * </p>
     *
     * @param command 퇴사 명령 (deletePolicy 및 retentionDays 포함)
     * @throws com.nexfron.identitymodulith.user.domain.exception.BusinessException
     *         - AGENT_NOT_FOUND: 상담사를 찾을 수 없음
     *         - AGENT_ALREADY_RETIRED: 이미 퇴사한 상담사
     *         - CANNOT_RETIRE_SELF: 본인 퇴사 시도
     *         - INVALID_REQUEST: deletePolicy=SCHEDULED인데 retentionDays 없음
     */
    @Override
    public void retireAgent(RetireAgentUseCase.RetireAgentCommand command) {
        // 현재 사용자가 본인을 퇴사 처리하려는지 확인
        if (command.getActorId().equals(command.getAgentId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "본인을 퇴사 처리할 수 없습니다.");
        }

        // SCHEDULED 정책 검증
        if (command.getDeletePolicy() == RetireAgentUseCase.RetireDeletePolicy.SCHEDULED
                && command.getRetentionDays() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "SCHEDULED 정책인 경우 retentionDays는 필수입니다.");
        }

        // 상담사 조회 (tenantId 포함)
        Agent agent = agentRepository.findByIdAndTenantId(command.getAgentId(), command.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        // 이미 퇴사했는지 확인
        if (agent.getStatus().name().equals("RETIRED")) {
            throw new BusinessException(ErrorCode.AGENT_ALREADY_RETIRED, "이미 퇴사한 상담사입니다.");
        }

        // 상태를 RETIRED로 변경
        Agent.RetireDeletePolicy agentDeletePolicy = Agent.RetireDeletePolicy.valueOf(command.getDeletePolicy().name());
        agent.retire(command.getActorId().toString(), agentDeletePolicy, command.getRetentionDays());

        // deletePolicy에 따른 처리
        if (command.getDeletePolicy() == RetireAgentUseCase.RetireDeletePolicy.IMMEDIATE) {
            // 즉시 익명화
            agent.anonymize();
        } else if (command.getDeletePolicy() == RetireAgentUseCase.RetireDeletePolicy.SCHEDULED) {
            // 예약 삭제 일시 설정
            agent.setScheduledDeleteAt(
                    LocalDateTime.now().plusDays(command.getRetentionDays()));
        }
        // PRESERVE인 경우 데이터 변경 없음

        agentRepository.save(agent);

        // 이벤트 발행 (모든 역할/권한 제거, KeyCloak 동기화 등)
        // TODO: AgentRetired 이벤트 발행
    }

    // ========== 부서 이동 ==========

    /**
     * 상담사를 다른 조직(부서)으로 이동시킵니다.
     *
     * <h3>비즈니스 규칙 (AGENT_SCENARIOS 6절)</h3>
     * <ul>
     *   <li>T-001: 대상 부서 존재 및 ACTIVE 확인 필요 (Organization 모듈에서 검증)</li>
     *   <li>T-002: 동일 부서로 이동 불가</li>
     *   <li>T-003: RETIRED 상담사 이동 불가</li>
     *   <li>T-004: 행위자의 DataScope에 양쪽 부서 포함 필요 (Controller/Security에서 검증)</li>
     * </ul>
     *
     * @param command 부서 이동 명령
     * @return 부서 이동 결과
     * @throws BusinessException
     *         - AGENT_NOT_FOUND: 상담사를 찾을 수 없음
     *         - AGENT_ALREADY_RETIRED: 퇴사한 상담사는 이동 불가
     *         - SAME_ORGANIZATION: 동일한 조직으로 이동 시도
     */
    @Override
    public TransferAgentUseCase.TransferAgentResult transferAgent(
            TransferAgentUseCase.TransferAgentCommand command) {

        log.info("[USER] 상담사 부서 이동 시작 - agentId={}, newOrganizationId={}",
                command.getAgentId(), command.getNewOrganizationId());

        // 1. 상담사 조회 (tenantId 포함)
        Agent agent = agentRepository.findByIdAndTenantId(command.getAgentId(), command.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        // 2. T-003: RETIRED 상담사 이동 불가
        if (agent.getStatus() == AgentStatus.RETIRED) {
            throw new BusinessException(ErrorCode.AGENT_ALREADY_RETIRED, "퇴사한 상담사는 부서를 이동할 수 없습니다.");
        }

        // 3. T-002: 동일 부서로 이동 불가
        if (agent.getOrganizationId() != null &&
            agent.getOrganizationId().equals(command.getNewOrganizationId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "이미 해당 조직에 소속되어 있습니다.");
        }

        String fromOrganizationId = agent.getOrganizationId();

        // 4. 조직 변경 (T-001 검증은 Organization 모듈에서 수행)
        agent.transferOrganization(command.getNewOrganizationId());
        agentRepository.save(agent);

        log.info("[USER] 상담사 부서 이동 완료 - agentId={}, from={}, to={}",
                command.getAgentId(), fromOrganizationId, command.getNewOrganizationId());

        // 5. 이벤트 발행 (비동기 처리)
        // TODO: AgentTransferred 이벤트 발행

        return TransferAgentUseCase.TransferAgentResult.builder()
                .agentId(agent.getId())
                .fromOrganizationId(fromOrganizationId)
                .toOrganizationId(command.getNewOrganizationId())
                .transferredAt(LocalDateTime.now())
                .build();
    }

    // ========== 통계 조회 ==========

    /**
     * 테넌트별 전체 상담사 통계 조회
     * AG-021: 대시보드에서 실시간 통계 제공
     */
    @Override
    @Transactional(readOnly = true)
    public AgentStatistics getStatistics(String tenantId) {
        log.debug("[USER] 상담사 통계 조회 - tenantId={}", tenantId);

        List<Agent> allAgents = agentRepository.findByTenantId(tenantId);

        return calculateStatistics(allAgents);
    }

    /**
     * 조직별 상담사 통계 조회
     * AG-021: 대시보드에서 실시간 통계 제공
     */
    @Override
    @Transactional(readOnly = true)
    public AgentStatistics getStatisticsByOrganization(String tenantId, String organizationId) {
        log.debug("[USER] 조직별 상담사 통계 조회 - tenantId={}, organizationId={}", tenantId, organizationId);

        List<Agent> agents = agentRepository.findByTenantIdAndOrganizationId(tenantId, organizationId);

        return calculateStatistics(agents);
    }

    /**
     * 상담사 목록으로부터 통계 계산
     */
    private AgentStatistics calculateStatistics(List<Agent> agents) {
        int totalCount = agents.size();
        int activeCount = 0;
        int suspendedCount = 0;
        int retiredCount = 0;
        int passwordChangeRequired = 0;

        java.util.Map<String, Integer> byOrganization = new java.util.HashMap<>();
        java.util.Map<String, Integer> byStatus = new java.util.HashMap<>();

        for (Agent agent : agents) {
            // 상태별 카운트
            switch (agent.getStatus()) {
                case ACTIVE -> activeCount++;
                case SUSPENDED -> suspendedCount++;
                case RETIRED -> retiredCount++;
            }

            // 비밀번호 변경 필요 카운트
            if (agent.isPasswordMustChange()) {
                passwordChangeRequired++;
            }

            // 조직별 카운트
            String orgId = agent.getOrganizationId();
            if (orgId != null) {
                byOrganization.merge(orgId, 1, Integer::sum);
            }

            // 상태별 카운트 (맵)
            String statusName = agent.getStatus().name();
            byStatus.merge(statusName, 1, Integer::sum);
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

    /**
     * 감사 로그 기록 (콘솔 로깅)
     *
     * @param tenantId 테넌트 ID
     * @param agentId 상담사 ID
     * @param actionType 작업 유형 (SUSPEND, ACTIVATE, RETIRE 등)
     * @param description 변경 내용 설명
     */
    private void logAudit(String tenantId, UUID agentId, String actionType, String description) {
        log.info("[AUDIT] Action: {}, Agent: {}, Description: {}", actionType, agentId, description);
    }
}
