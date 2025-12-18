// organization.domain.model.DataScopeLevel.java
package com.nexfron.identitymodulith.organization.domain.model;

/**
 * Level 2 RBAC (Data Scope) 관점의 역할 레벨
 *
 * - 실제 Users 테이블의 role/position 정보를 이 enum 으로 매핑해서 사용한다.
 */
public enum DataScopeLevel {

    MEMBER,       // 본인 부서 ONLY
    TEAM_LEAD,    // 본인 + 하위 부서
    ADMIN;        // 전체 조직

    /** 전체 조직 조회 가능 여부 */
    public boolean canSeeWholeTenant() {
        return this == ADMIN;
    }

    /** 자기 부서 + 하위 부서 조회 가능 여부 */
    public boolean canSeeSubTree() {
        return this == TEAM_LEAD || this == ADMIN;
    }
}
