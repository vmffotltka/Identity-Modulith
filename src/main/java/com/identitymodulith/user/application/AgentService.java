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
    private final OrganizationPort organizationPort;
    private final RbacModuleApi rbacModuleApi;
    private final RbacPort rbacPort;
    private final ApplicationEventPublisher eventPublisher;

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
     * @param tenantId 테넌트 ID
     * @param agentId 비밀번호를 초기화할 상담사 ID
     * @param actorId 작업을 수행하는 사용자 ID
     * @return 상담사 ID와 새로 생성된 임시 비밀번호를 포함한 결과
     * @throws BusinessException 상담사를 찾을 수 없는 경우 (ErrorCode.AGENT_NOT_FOUND)
     *                           권한이 없는 경우 (ErrorCode.BUSINESS_RULE_VIOLATION)
     */
    @Override
    public ResetPasswordResult resetPassword(String tenantId, UUID agentId, UUID actorId) {
        // 1. 상담사 조회 (tenantId 포함)
        Agent agent = agentRepository.findByIdAndTenantId(agentId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        // 2. ADMIN 권한 검증 (RbacPort 사용)
        boolean isAdmin = rbacPort.hasRole(actorId.toString(), "ADMIN");

        if (!isAdmin) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "관리자만 비밀번호를 초기화할 수 있습니다.");
        }

        // 3. 임시 비밀번호 생성 및 초기화
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
        log.info("[USER] 비밀번호 변경 시작 - agentId={}, actorId={}", command.getAgentId(), command.getActorId());

        // 1. 본인 확인 (PC-004)
        if (!command.getAgentId().equals(command.getActorId())) {
            log.warn("[USER] 본인 확인 실패 - agentId={}, actorId={}", command.getAgentId(), command.getActorId());
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "다른 사용자의 비밀번호를 변경할 수 없습니다.");
        }
        log.debug("[USER] 본인 확인 통과");

        // 2. 상담사 조회
        Agent agent = agentRepository.findByIdAndTenantId(command.getAgentId(), command.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));
        log.debug("[USER] 상담사 조회 성공 - loginId={}", agent.getLoginId());

        // 3. 현재 비밀번호 검증 (PC-001)
        log.debug("[USER] 현재 비밀번호 검증 시작");
        boolean passwordMatches = passwordEncoder.matches(command.getCurrentPassword(), agent.getPassword());
        log.debug("[USER] 비밀번호 일치 여부: {}", passwordMatches);

        if (!passwordMatches) {
            log.warn("[USER] 현재 비밀번호 불일치 - agentId={}", command.getAgentId());
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH,
                    "현재 비밀번호가 일치하지 않습니다.");
        }
        log.debug("[USER] 현재 비밀번호 검증 통과");

        // 4. 새 비밀번호 != 현재 비밀번호 검증 (PC-002)
        if (command.getCurrentPassword().equals(command.getNewPassword())) {
            log.warn("[USER] 새 비밀번호가 현재 비밀번호와 동일 - agentId={}", command.getAgentId());
            throw new BusinessException(ErrorCode.SAME_AS_CURRENT_PASSWORD,
                    "새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }
        log.debug("[USER] 새 비밀번호 검증 통과");

        // 5. 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(command.getNewPassword());
        agent.changePassword(encodedPassword);  // PC-003: passwordMustChange = false
        log.debug("[USER] 비밀번호 암호화 및 변경 완료");

        // 6. 저장
        agentRepository.save(agent);
        log.debug("[USER] 저장 완료");

        // 7. 로깅
        log.info("[USER] 비밀번호 변경 완료 - agentId={}", command.getAgentId());
    }

    /**
     * 상담사의 기본 정보를 수정합니다.
     *
     * @param command 수정할 상담사 ID와 변경할 정보 (이름 등)
     * @throws BusinessException 상담사를 찾을 수 없는 경우 (ErrorCode.AGENT_NOT_FOUND)
     *                           권한이 없는 경우 (ErrorCode.BUSINESS_RULE_VIOLATION)
     */
    @Override
    public void updateAgent(UpdateAgentCommand command) {
        // 1. 상담사 조회 (tenantId 포함)
        Agent agent = agentRepository.findByIdAndTenantId(command.getAgentId(), command.getTenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        // 2. 권한 검증: 본인 또는 관리자만 수정 가능 (RbacPort 사용)
        boolean isAdmin = rbacPort.hasRole(command.getActorId().toString(), "ADMIN");
        boolean isSelf = command.getActorId().equals(command.getAgentId());

        if (!isAdmin && !isSelf) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "본인 또는 관리자만 상담사 정보를 수정할 수 있습니다.");
        }

        // 3. 정보 수정
        agent.updateName(command.getName());
        // JPA dirty checking으로 자동 저장됨 (saveAgent 호출 불필요)
    }

    /**
     * 상담사를 다른 조직으로 이동시킵니다.
     *
     * @param tenantId 테넌트 ID
     * @param agentId 이동할 상담사 ID
     * @param actorId 작업을 수행하는 사용자 ID
     * @param newOrganizationId 새로 배정될 조직 ID
     * @throws BusinessException 상담사를 찾을 수 없는 경우 (ErrorCode.AGENT_NOT_FOUND)
     *                           권한이 없는 경우 (ErrorCode.BUSINESS_RULE_VIOLATION)
     */
    @Override
    public void transferOrganization(String tenantId, UUID agentId, UUID actorId, String newOrganizationId) {
        // 1. 상담사 조회 (tenantId 포함)
        Agent agent = agentRepository.findByIdAndTenantId(agentId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND));

        // 2. 대상 부서 존재 확인
        organizationPort.getDepartmentInfo(tenantId, newOrganizationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND,
                        "이동할 부서를 찾을 수 없습니다."));

        // 3. ADMIN 권한 검증 (RbacPort 사용)
        boolean isAdmin = rbacPort.hasRole(actorId.toString(), "ADMIN");

        if (!isAdmin) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "관리자만 상담사 조직을 이동시킬 수 있습니다.");
        }

        // 4. 조직 이동
        agent.transferOrganization(newOrganizationId);
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
        LocalDateTime now = LocalDateTime.now();
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
     * 역할 ID 목록으로 역할을 일괄 지정합니다.
     * RBAC 모듈과 연동하여 실제 역할을 조회 후 할당합니다.
     * 기존 역할은 모두 제거되고 새로운 역할로 완전히 대체됩니다. (PUT semantic)
     *
     * @param agentId 역할을 지정할 상담사 ID
     * @param roleIds 할당할 역할 ID 세트 (UUID 형식)
     * @throws BusinessException roleIds가 null이거나 empty인 경우
     */
    @Override
    public void assignRolesByIds(UUID agentId, Set<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                "최소 1개 이상의 역할을 지정해야 합니다.");
        }

        log.info("[USER] 역할 ID로 일괄 지정 - agentId={}, roleIds={}", agentId, roleIds);

        // 1. 기존 역할 모두 제거
        rbacPort.removeAllRolesFromAgent(agentId.toString());
        log.debug("[USER] 기존 역할 제거 완료 - agentId={}", agentId);

        // 2. 새로운 역할 할당 (roleId가 UUID 형식인지 확인하여 적절한 메서드 호출)
        for (String roleId : roleIds) {
            // UUID 형식인지 확인 (간단한 길이 체크: UUID는 36자)
            if (roleId != null && roleId.length() == 36 && roleId.contains("-")) {
                // UUID 형식이면 roleId로 할당
                rbacPort.assignRoleToAgentByRoleId(agentId.toString(), roleId);
            } else {
                // 그 외에는 roleName으로 할당
                rbacPort.assignRoleToAgent(agentId.toString(), roleId);
            }
        }

        log.info("[USER] 역할 ID로 일괄 지정 완료 - agentId={}, roleCount={}", agentId, roleIds.size());
    }

    /**
     * 역할 이름 목록으로 역할을 일괄 지정합니다.
     * RBAC 모듈과 연동하여 역할 이름으로 역할을 조회 후 할당합니다.
     * 기존 역할은 모두 제거되고 새로운 역할로 완전히 대체됩니다. (PUT semantic)
     *
     * 비즈니스 규칙 (RA-003):
     * - POSITION 타입 역할은 1개만 할당 가능
     * - CHANNEL 타입 역할은 여러 개 할당 가능
     *
     * @param agentId 역할을 지정할 상담사 ID
     * @param roleNames 할당할 역할 이름 세트 (예: "TEAM_LEAD", "VOICE_INBOUND")
     * @throws BusinessException roleNames가 null이거나 empty인 경우, POSITION 역할이 2개 이상인 경우
     */
    @Override
    public void assignRolesByNames(UUID agentId, Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                "최소 1개 이상의 역할을 지정해야 합니다.");
        }

        log.info("[USER] 역할 이름으로 일괄 지정 - agentId={}, roleNames={}", agentId, roleNames);

        // RA-003: POSITION 역할은 1개만 할당 가능 (검증)
        // ADMIN, TEAM_LEAD, MEMBER는 POSITION 타입
        Set<String> positionRoleNames = Set.of("ADMIN", "TEAM_LEAD", "MEMBER", "AGENT");
        long positionCount = roleNames.stream()
                .filter(positionRoleNames::contains)
                .count();

        if (positionCount > 1) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "POSITION 역할(ADMIN, TEAM_LEAD, MEMBER)은 1개만 할당할 수 있습니다.");
        }

        // 1. 기존 역할 모두 제거
        rbacPort.removeAllRolesFromAgent(agentId.toString());
        log.debug("[USER] 기존 역할 제거 완료 - agentId={}", agentId);

        // 2. 새로운 역할 할당 (POSITION 자동 교체 로직 없이)
        for (String roleName : roleNames) {
            rbacPort.assignRoleToAgentWithoutAutoReplace(agentId.toString(), roleName);
        }

        log.info("[USER] 역할 이름으로 일괄 지정 완료 - agentId={}, roleCount={}", agentId, roleNames.size());
    }

    /**
     * ADMIN 권한을 검증합니다.
     *
     * @param tenantId 테넌트 ID
     * @param actorId 권한을 검증할 사용자 ID
     * @throws BusinessException ADMIN 권한이 없는 경우
     */
    @Override
    public void validateAdminPermission(String tenantId, UUID actorId) {
        boolean isAdmin = rbacPort.hasRole(actorId.toString(), "ADMIN");

        if (!isAdmin) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "관리자만 이 작업을 수행할 수 있습니다.");
        }
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
     * 로그인 ID로 상담사 정보를 조회합니다.
     * SAML 인증 후 사용자 매핑에 사용됩니다.
     *
     * @param loginId 로그인 ID
     * @return 상담사 정보 (없으면 empty)
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AgentExternalInfo> findAgentByLoginId(String loginId) {
        return DatabaseRetrySupplier.withRetry(
                () -> agentRepository.findByLoginId(loginId)
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
     * @throws BusinessException
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

        eventPublisher.publishEvent(new AgentSuspendedEvent(
                agent.getId(), command.getTenantId(),
                command.getActorId().toString(), agent.getSuspendedAt()));
        log.info("[USER] AgentSuspendedEvent 발행 - agentId={}", agent.getId());
    }

    /**
     * 정지된(SUSPENDED) 상담사를 활성화합니다.
     * <p>
     * SUSPENDED 상태의 상담사만 활성화 가능합니다.
     * RETIRED 상태는 복구 불가능하며 활성화할 수 없습니다.
     * </p>
     *
     * @param command 활성화 명령 (tenantId, agentId, actorId 포함)
     * @throws BusinessException
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

        eventPublisher.publishEvent(new AgentActivatedEvent(
                agent.getId(), command.getTenantId(),
                command.getActorId().toString(), LocalDateTime.now()));
        log.info("[USER] AgentActivatedEvent 발행 - agentId={}", agent.getId());
    }

    /**
     * 상담사를 퇴사 처리합니다.
     * <p>
     * 상담사를 RETIRED 상태로 변경합니다. RETIRED 상태는 되돌릴 수 없습니다.
     * deletePolicy에 따라 개인정보를 처리합니다.
     * </p>
     *
     * @param command 퇴사 명령 (deletePolicy 및 retentionDays 포함)
     * @throws BusinessException
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

        eventPublisher.publishEvent(new AgentRetiredEvent(
                agent.getId(), command.getTenantId(),
                command.getActorId().toString(),
                command.getDeletePolicy().name(),
                agent.getRetiredAt()));
        log.info("[USER] AgentRetiredEvent 발행 - agentId={}, policy={}", agent.getId(), command.getDeletePolicy());
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

        // 2. T-001: 대상 부서 존재 확인
        organizationPort.getDepartmentInfo(command.getTenantId(), command.getNewOrganizationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORGANIZATION_NOT_FOUND,
                        "이동할 부서를 찾을 수 없습니다."));

        // 3. T-003: RETIRED 상담사 이동 불가
        if (agent.getStatus() == AgentStatus.RETIRED) {
            throw new BusinessException(ErrorCode.AGENT_ALREADY_RETIRED, "퇴사한 상담사는 부서를 이동할 수 없습니다.");
        }

        // 4. T-002: 동일 부서로 이동 불가
        if (agent.getOrganizationId() != null &&
            agent.getOrganizationId().equals(command.getNewOrganizationId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "이미 해당 조직에 소속되어 있습니다.");
        }

        String fromOrganizationId = agent.getOrganizationId();

        // 5. 조직 변경
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
