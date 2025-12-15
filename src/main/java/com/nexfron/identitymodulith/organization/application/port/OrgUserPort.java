package com.nexfron.identitymodulith.organization.application.port;

import java.util.List;
import java.util.UUID;

/**
 * User 모듈 ↔ Organization 모듈 사이의 경계
 *
 * - 구현체는 나중에 User 모듈로 옮길 예정.
 * - 지금은 Dummy 구현체 하나만 만들어서 테스트한다.
 */
public interface OrgUserPort {

    /**
     * 특정 테넌트 + 부서에 "활성 유저"가 1명이라도 있는지 여부
     * - 부서 삭제 시 검증에 사용
     */
    boolean existsActiveUserInDepartment(String tenantId, Long deptId);

    /**
     * 특정 유저의 조직/역할 정보 조회
     * - Level 2 RBAC 스코프 계산에 사용
     */
    OrgUserView findOrgInfoByUserId(String tenantId, UUID userId);

    /**
     * (옵션) 여러 부서에 속한 활성 유저들 목록 조회
     * - 아직 안 쓰더라도 시그니처만 잡아두면 나중에 유저 리스트 API에 쓰기 좋음
     */
    List<OrgUserView> findActiveUsersByDeptIds(String tenantId, List<Long> deptIds);
}
