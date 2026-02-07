package com.nexfron.identitymodulith.user.infrastructure.adapter;

import com.nexfron.identitymodulith.organization.OrganizationModuleApi;
import com.nexfron.identitymodulith.user.application.port.OrganizationPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * User 모듈에서 Organization 모듈의 Department 정보를 조회하기 위한 Adapter
 *
 * <h3>역할:</h3>
 * - OrganizationPort 인터페이스 구현
 * - OrganizationModuleApi를 통해 Department 정보 조회
 * - 모듈 간 결합도 최소화 (Port-Adapter 패턴)
 *
 * <h3>동작:</h3>
 * - OrganizationModuleApi.getDepartmentInfo() 호출
 * - DepartmentInfo DTO 변환 (API → Port)
 *
 * @author Identity System Team
 * @version 1.0
 */
@Component
public class OrganizationPortAdapter implements OrganizationPort {

    private final OrganizationModuleApi organizationModuleApi;

    /**
     * 생성자 - Lazy 초기화로 순환 참조 방지
     */
    public OrganizationPortAdapter(@Lazy OrganizationModuleApi organizationModuleApi) {
        this.organizationModuleApi = organizationModuleApi;
    }

    /**
     * 부서 ID로 부서 정보 조회
     *
     * @param tenantId 테넌트 ID
     * @param deptId 부서 ID
     * @return 부서 정보 (이름, 전체 경로 포함)
     */
    @Override
    public Optional<DepartmentInfo> getDepartmentInfo(String tenantId, String deptId) {
        return organizationModuleApi.getDepartmentInfo(tenantId, deptId)
                .map(apiInfo -> DepartmentInfo.builder()
                        .deptId(apiInfo.getDeptId())
                        .name(apiInfo.getName())
                        .path(apiInfo.getFullPath())
                        .build());
    }
}
