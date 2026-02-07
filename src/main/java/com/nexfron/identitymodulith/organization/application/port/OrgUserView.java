// organization.application.port.OrgUserView.java
package com.nexfron.identitymodulith.organization.application.port;

import com.nexfron.identitymodulith.organization.domain.model.DataScopeLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 조직 모듈에서 바라보는 "사용자의 조직 정보" 뷰
 *
 * - Users 엔티티를 직접 보지 않고, 필요한 정보만 추상화한다.
 * - 실제 조회/저장은 User 모듈이 책임진다.
 *
 * 데이터 타입:
 * - userId: UUID
 * - tenantId: String (테넌트 식별자)
 * - deptId: String (UUID 문자열, departments.dept_id)
 */
@Getter
@Builder
public class OrgUserView {

    private UUID userId;           // Users.id (UUID)
    private String tenantId;       // 테넌트 ID

    private String deptId;         // 소속 부서 ID (UUID 문자열, FK: departments.dept_id)
    private String deptOrgPath;    // 소속 부서 orgPath (없으면 Dept에서 계산 가능)
    private String departmentName; // 소속 부서명 (예: "인바운드팀")
    private String departmentPath; // 소속 부서 전체 경로 (예: "넥스프론 > 고객서비스본부 > 인바운드팀")

    private DataScopeLevel roleLevel; // Level 1 RBAC용 역할 레벨
    private boolean active;           // 재직 여부
}
