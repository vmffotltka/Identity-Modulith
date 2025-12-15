// organization.application.port.OrgUserView.java
package com.nexfron.identitymodulith.organization.application.port;

import com.nexfron.identitymodulith.organization.domain.model.OrgRoleLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 조직 모듈에서 바라보는 "사용자의 조직 정보" 뷰
 *
 * - Users 엔티티를 직접 보지 않고, 필요한 정보만 추상화한다.
 * - 실제 조회/저장은 User 모듈이 책임진다.
 */
@Getter
@Builder
public class OrgUserView {

    private UUID userId;       // Users.id
    private String tenantId;

    private Long deptId;       // 소속 부서 ID (FK: departments.dept_id)
    private String deptOrgPath;// 소속 부서 orgPath (없으면 Dept에서 계산 가능)

    private OrgRoleLevel roleLevel; // Level 2 RBAC용 역할 레벨
    private boolean active;    // 재직 여부
}
