package com.nexfron.identitymodulith.organization.infrastructure.config;

import com.nexfron.identitymodulith.organization.domain.model.DataScopeLevel;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * RoleScopeMappingConfig - 역할명과 데이터 스코프 레벨 매핑 설정
 *
 * <h2>목적:</h2>
 * RBAC의 역할명을 Organization 모듈의 DataScopeLevel로 명시적으로 매핑합니다.
 *
 * <h2>설계 원칙:</h2>
 * <ul>
 *   <li>문자열 포함 여부 검사 대신 명시적 매핑 사용</li>
 *   <li>역할명 변경 시 한 곳에서만 수정</li>
 *   <li>기본값은 MEMBER (최소 권한 원칙)</li>
 * </ul>
 *
 * <h2>데이터 스코프 레벨:</h2>
 * <ul>
 *   <li><b>ADMIN</b>: 전체 부서 조회 가능</li>
 *   <li><b>TEAM_LEAD</b>: 본인 부서 + 하위 부서 조회 가능</li>
 *   <li><b>MEMBER</b>: 본인 부서만 조회 가능</li>
 * </ul>
 *
 * @author Identity System Team
 * @version 1.0
 */
@Configuration
public class RoleScopeMappingConfig {

    /**
     * 역할명 → 데이터 스코프 레벨 매핑 테이블
     *
     * <h3>POSITION 타입 역할:</h3>
     * <ul>
     *   <li>ADMIN: 시스템 관리자 → ADMIN</li>
     *   <li>MANAGER: 매니저 → ADMIN</li>
     *   <li>TEAM_LEAD: 팀장 → TEAM_LEAD</li>
     *   <li>MEMBER: 일반 직원 → MEMBER</li>
     * </ul>
     *
     * <h3>CHANNEL 타입 역할:</h3>
     * <ul>
     *   <li>SUPERVISOR: 수퍼바이저 → TEAM_LEAD</li>
     *   <li>PHONE_AGENT: 전화 상담사 → MEMBER</li>
     *   <li>CHAT_AGENT: 채팅 상담사 → MEMBER</li>
     *   <li>EMAIL_AGENT: 이메일 상담사 → MEMBER</li>
     * </ul>
     */
    private static final Map<String, DataScopeLevel> ROLE_SCOPE_MAP = Map.ofEntries(
            // POSITION 타입 역할
            Map.entry("ADMIN", DataScopeLevel.ADMIN),
            Map.entry("MANAGER", DataScopeLevel.ADMIN),
            Map.entry("TEAM_LEAD", DataScopeLevel.TEAM_LEAD),
            Map.entry("MEMBER", DataScopeLevel.MEMBER),

            // CHANNEL 타입 역할
            Map.entry("SUPERVISOR", DataScopeLevel.TEAM_LEAD),
            Map.entry("PHONE_AGENT", DataScopeLevel.MEMBER),
            Map.entry("CHAT_AGENT", DataScopeLevel.MEMBER),
            Map.entry("EMAIL_AGENT", DataScopeLevel.MEMBER)
    );

    /**
     * 역할명으로 데이터 스코프 레벨 조회
     *
     * <p>매핑 테이블에 없는 역할은 MEMBER(최소 권한)로 간주합니다.
     *
     * <h3>사용 예시:</h3>
     * <pre>
     * DataScopeLevel level = RoleScopeMappingConfig.getDataScope("ADMIN");
     * // 결과: DataScopeLevel.ADMIN
     *
     * DataScopeLevel level = RoleScopeMappingConfig.getDataScope("UNKNOWN_ROLE");
     * // 결과: DataScopeLevel.MEMBER (기본값)
     * </pre>
     *
     * @param roleName 역할명 (예: "ADMIN", "PHONE_AGENT")
     * @return 데이터 스코프 레벨
     */
    public static DataScopeLevel getDataScope(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return DataScopeLevel.MEMBER;
        }

        // 대소문자 무시하고 매핑
        String normalizedRoleName = roleName.trim().toUpperCase();
        return ROLE_SCOPE_MAP.getOrDefault(normalizedRoleName, DataScopeLevel.MEMBER);
    }
}

