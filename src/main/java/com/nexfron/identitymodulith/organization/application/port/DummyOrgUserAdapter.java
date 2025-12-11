package com.nexfron.identitymodulith.organization.application.port;

import com.nexfron.identitymodulith.organization.domain.model.OrgRoleLevel;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 임시 구현체 (Dummy)
 *
 * - 지금은 User 모듈이 없으니까, 테스트용으로만 사용.
 * - 나중에 User 모듈 쪽에서 진짜 구현으로 교체하면 된다.
 */
@Service
public class DummyOrgUserAdapter implements OrgUserPort {

    @Override
    public boolean existsActiveUserInDepartment(String tenantId, Long deptId) {
        // TODO: User 모듈 완성되면 진짜 로직으로 교체
        // 지금은 "어떤 부서에도 유저 없음"이라고 가정 (삭제 항상 허용)
        return false;
    }

    @Override
    public OrgUserView findOrgInfoByUserId(String tenantId, UUID userId) {
        // TODO: 실제 Users 테이블 기반 조직/역할 정보를 조회해야 한다.
        // 지금은 ADMIN 으로 가정해서 "전체 스코프" 테스트 용도로 사용.
        return OrgUserView.builder()
                .userId(userId)
                .tenantId(tenantId)
                .deptId(null)              // 부서 정보 없음 (테스트용)
                .deptOrgPath(null)
                .roleLevel(OrgRoleLevel.ADMIN)
                .active(true)
                .build();
    }

    @Override
    public List<OrgUserView> findActiveUsersByDeptIds(String tenantId, List<Long> deptIds) {
        // TODO: 나중에 구현
        return Collections.emptyList();
    }
}
