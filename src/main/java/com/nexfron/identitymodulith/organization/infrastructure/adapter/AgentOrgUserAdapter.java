package com.nexfron.identitymodulith.organization.infrastructure.adapter;

import com.nexfron.identitymodulith.organization.application.port.OrgUserPort;
import com.nexfron.identitymodulith.organization.application.port.OrgUserView;
import com.nexfron.identitymodulith.organization.domain.model.DataScopeLevel;
import com.nexfron.identitymodulith.organization.infrastructure.persistence.entity.DepartmentEntity;
import com.nexfron.identitymodulith.organization.infrastructure.persistence.repository.JpaDepartmentRepository;
import com.nexfron.identitymodulith.user.UserModuleApi;
import com.nexfron.identitymodulith.user.AgentExternalInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * [Organization ↔ User 연동 어댑터]
 *
 * <h2>목적:</h2>
 * Organization 모듈에서 사용자 정보를 직접 참조하지 않기 위해
 * OrgUserPort 인터페이스를 통해 User(Agent) 모듈을 조회하는 실제 구현체입니다.
 *
 * <h2>책임:</h2>
 * <ul>
 *   <li>User 모듈의 Agent 정보를 Organization 모듈이 필요로 하는 형태로 변환</li>
 *   <li>조직 스코프 / 권한 판단에 필요한 최소 정보만 제공</li>
 *   <li>모듈 간 결합도 최소화 (포트-어댑터 패턴)</li>
 * </ul>
 *
 * <h2>주요 변환 작업:</h2>
 * <ol>
 *   <li>userId(UUID) → Agent 조회 (UserModuleApi 사용)</li>
 *   <li>Agent.organizationId(String) → Department.deptId 매핑</li>
 *   <li>Agent.roles → DataScopeLevel 매핑 (RoleScopeMappingConfig 사용)</li>
 * </ol>
 *
 * <h2>주의사항:</h2>
 * <ul>
 *   <li>Agent.organizationId는 부서 ID를 문자열(UUID)로 저장</li>
 *   <li>Organization 모듈은 부서 트리/경로 계산만 담당</li>
 *   <li>사용자 활성 여부/권한은 이 어댑터에서 해석</li>
 * </ul>
 *
 * @author Identity System Team
 * @version 1.0
 */
@Service
@Primary  // OrgUserPort의 기본 구현체로 지정
public class AgentOrgUserAdapter implements OrgUserPort {

    /**
     * User 모듈의 Agent 조회용 Repository
     * (Organization 모듈은 Agent 엔티티를 직접 알지 않음)
     */
    private final UserModuleApi userModuleApi;

    /**
     * Department 정보 조회용 Repository
     * (departmentName, departmentPath 조회에 사용)
     */
    private final JpaDepartmentRepository departmentRepository;

    /**
     * 생성자 - Lazy 초기화로 순환 참조 방지
     */
    public AgentOrgUserAdapter(@Lazy UserModuleApi userModuleApi,
                               JpaDepartmentRepository departmentRepository) {
        this.userModuleApi = userModuleApi;
        this.departmentRepository = departmentRepository;
    }

    /**
     * 특정 부서에 "활성 상태"의 사용자가 존재하는지 여부 확인
     *
     * - 부서 삭제 가능 여부 판단 시 사용됨
     * - 실제로는 tenant + deptId + ACTIVE 조건을 만족하는 사용자 존재 여부만 필요
     *
     * @param tenantId 테넌트 ID
     * @param deptId   부서 ID (UUID 문자열, Department.deptId)
     * @return 활성 사용자 존재 여부
     */
    @Override
    public boolean existsActiveUserInDepartment(String tenantId, String deptId) {
        // UserModuleApi를 통해 해당 부서의 활성 상담사 목록을 가져온다.
        return userModuleApi.findActiveAgentsByOrganizationId(tenantId, deptId).stream()
                .anyMatch(AgentExternalInfo::isActive);
    }

    /**
     * 사용자 ID 기반으로 조직 스코프 계산에 필요한 사용자 정보 조회
     *
     * - 부서 이동 / 조직 조회 등 대부분의 Organization API 에서 사용됨
     * - userId 는 HTTP Header(X-User-Id) 로 전달됨
     * - <b>Optional 반환으로 null 안전성 확보</b>
     *
     * @param tenantId 테넌트 ID
     * @param userId   사용자(Agent) UUID
     * @return OrgUserView Optional (사용자가 없으면 empty)
     */
    @Override
    public java.util.Optional<OrgUserView> findOrgInfoByUserId(String tenantId, UUID userId) {
        return userModuleApi.findAgentById(tenantId, userId)
                .map(this::toViewFromExternal);
    }

    /**
     * 여러 부서에 속한 활성 사용자 조회
     *
     * - 현재 Organization 모듈에서는 사용하지 않음
     * - 추후 조직 단위 사용자 목록 API가 추가될 경우 확장 예정
     *
     * @param tenantId 테넌트 ID
     * @param deptIds 부서 ID 리스트 (UUID 문자열들)
     * @return 해당 부서들에 속한 활성 사용자 목록
     *
     * NOTE:
     * 성능을 고려하면 AgentJpaRepository 에
     * tenant + deptId IN (...) 쿼리를 추가하는 것이 바람직함
     */
    @Override
    public List<OrgUserView> findActiveUsersByDeptIds(String tenantId, List<String> deptIds) {
        // 단순 구현: 각 부서별로 UserModuleApi 호출하고 결과를 합친다.
        List<OrgUserView> result = new ArrayList<>();
        for (String deptId : deptIds) {
            List<AgentExternalInfo> agents = userModuleApi.findActiveAgentsByOrganizationId(tenantId, deptId);
            agents.stream()
                    .map(this::toViewFromExternal)
                    .forEach(result::add);
        }
        return result;
    }

    /**
     * 특정 부서의 전체 직원 수 조회 (활성 + 비활성)
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID (UUID 문자열)
     * @return 전체 직원 수
     */
    @Override
    public long countEmployeesByDepartment(String tenantId, String deptId) {
        // UserModuleApi를 통해 해당 부서의 모든 상담사 수를 조회
        // 현재는 활성 상담사만 조회 가능하므로, 전체 조회 API가 필요하면 UserModuleApi에 추가 필요
        return userModuleApi.findActiveAgentsByOrganizationId(tenantId, deptId).size();
    }

    /**
     * 특정 부서의 활성 직원 수 조회 (ACTIVE 상태만)
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID (UUID 문자열)
     * @return 활성 직원 수
     */
    @Override
    public long countActiveEmployeesByDepartment(String tenantId, String deptId) {
        return userModuleApi.findActiveAgentsByOrganizationId(tenantId, deptId).stream()
                .filter(AgentExternalInfo::isActive)
                .count();
    }

    /**
     * 특정 부서에 속한 사용자 목록 조회
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID (UUID 문자열)
     * @return 부서 소속 사용자 정보 목록
     */
    @Override
    public List<com.nexfron.identitymodulith.organization.presentation.dto.DepartmentDto.MemberInfo> getUsersByDepartment(String tenantId, String deptId) {
        // UserModuleApi를 통해 해당 부서의 모든 상담사 조회
        return userModuleApi.findActiveAgentsByOrganizationId(tenantId, deptId).stream()
                .map(agent -> new com.nexfron.identitymodulith.organization.presentation.dto.DepartmentDto.MemberInfo(
                        agent.getId().toString(),
                        "user_" + agent.getId().toString().substring(0, 8),  // loginId (임시)
                        "User " + agent.getId().toString().substring(0, 8),  // name (임시)
                        agent.getOrganizationId(),
                        "",  // jobTitle (User 모듈에서 제공 필요)
                        agent.isActive() ? "ACTIVE" : "RETIRED"
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * AgentExternalInfo → OrgUserView 변환
     *
     * Organization 모듈이 필요로 하는 정보만 추려서 전달
     * Department 정보를 조회하여 departmentName과 departmentPath 설정
     */
    private OrgUserView toViewFromExternal(AgentExternalInfo info) {
        // organizationId는 이미 부서 ID (UUID 문자열)
        String deptId = info.getOrganizationId();

        // Department 정보 조회
        String departmentName = null;
        String departmentPath = null;

        if (deptId != null && !deptId.isEmpty()) {
            departmentRepository.findByDeptIdAndTenantId(deptId, info.getTenantId())
                .ifPresent(dept -> {
                    // departmentName은 직접 설정
                    // departmentPath는 orgPath에서 각 부서 이름을 조회하여 구성
                });

            // departmentName 설정
            departmentRepository.findByDeptIdAndTenantId(deptId, info.getTenantId())
                .ifPresent(dept -> {});

            java.util.Optional<DepartmentEntity> deptOpt =
                departmentRepository.findByDeptIdAndTenantId(deptId, info.getTenantId());

            if (deptOpt.isPresent()) {
                DepartmentEntity dept = deptOpt.get();
                departmentName = dept.getName();

                // departmentPath 구성: orgPath를 따라 올라가며 이름 수집
                departmentPath = buildDepartmentPath(dept, info.getTenantId());
            }
        }

        return OrgUserView.builder()
                .userId(info.getId())
                .tenantId(info.getTenantId())
                .deptId(deptId)
                .deptOrgPath(null)
                .departmentName(departmentName)
                .departmentPath(departmentPath)
                .roleLevel(mapRoleLevelFromExternal(info))
                .active(info.isActive())
                .build();
    }

    /**
     * Department 전체 경로 구성 (예: "넥스프론 > 고객서비스본부 > 인바운드팀")
     *
     * @param dept 대상 부서
     * @param tenantId 테넌트 ID
     * @return 부서 전체 경로 문자열
     */
    private String buildDepartmentPath(DepartmentEntity dept, String tenantId) {
        java.util.List<String> pathNames = new java.util.ArrayList<>();
        DepartmentEntity current = dept;

        // 현재 부서부터 루트까지 올라가며 이름 수집
        while (current != null) {
            pathNames.add(0, current.getName());  // 앞에 추가 (역순으로)

            if (current.getParent() != null) {
                String parentId = current.getParent().getDeptId();
                current = departmentRepository.findByDeptIdAndTenantId(parentId, tenantId)
                    .orElse(null);
            } else {
                break;
            }
        }

        return String.join(" > ", pathNames);
    }


    /**
     * Agent 역할 정보를 Organization 권한 레벨로 매핑
     *
     * <p><b>개선된 매핑 방식:</b>
     * {@link DataScopeLevel#fromRoleName(String)} 메서드를 통해 명시적 매핑합니다.
     *
     * <p><b>매핑 규칙:</b>
     * <ul>
     *   <li>사용자가 가진 모든 역할의 스코프 레벨 중 <b>최고 레벨</b>을 반환</li>
     *   <li>ADMIN > TEAM_LEAD > MEMBER 순서</li>
     *   <li>매핑되지 않은 역할은 MEMBER로 간주 (최소 권한 원칙)</li>
     * </ul>
     *
     * <p><b>예시:</b>
     * <pre>
     * 역할 [MEMBER, PHONE_AGENT] → MEMBER (둘 다 MEMBER 레벨)
     * 역할 [TEAM_LEAD, PHONE_AGENT] → TEAM_LEAD (TEAM_LEAD > MEMBER)
     * 역할 [ADMIN, TEAM_LEAD] → ADMIN (ADMIN > TEAM_LEAD)
     * </pre>
     *
     * @param info Agent 외부 정보
     * @return 최고 데이터 스코프 레벨
     */
    private DataScopeLevel mapRoleLevelFromExternal(AgentExternalInfo info) {
        return info.getRoles().stream()
                .map(role -> DataScopeLevel.fromRoleName(role.getName()))
                .max(java.util.Comparator.naturalOrder())  // ADMIN > TEAM_LEAD > MEMBER
                .orElse(DataScopeLevel.MEMBER);  // 역할이 없으면 MEMBER (최소 권한)
    }
}
