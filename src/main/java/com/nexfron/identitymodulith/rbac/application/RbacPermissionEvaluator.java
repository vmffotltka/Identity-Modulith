package com.nexfron.identitymodulith.rbac.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RBAC 권한 평가자 (Permission Evaluator)
 *
 * Spring Security의 메서드 레벨 권한 검사(@PreAuthorize, @PostAuthorize)를 담당합니다.
 *
 * 역할:
 * - 요청 시 사용자의 권한 정보를 조회하여 권한 있음/없음 판단
 * - @PreAuthorize, @PostAuthorize 등 권한 검사 애노테이션과 연동
 *
 * 데이터 표준:
 * - tenantId: "tenant-001", "acme-corp" 등 (테넌트 고유 ID)
 * - agentId: UUID 문자열 (예: "550e8400-e29b-41d4-a716-446655440000")
 * - permissionCode: "user:manage", "org:view", "report:export" 등 (도메인:액션)
 *
 * 사용 예시:
 * {@code
 * @PreAuthorize("@rbac.hasPermission(authentication, 'user:create')")
 * public UserResponse createUser(CreateUserRequest request) {
 *     // 권한 확인 후 실행됨
 * }
 *
 * @PostAuthorize("@rbac.hasPermission(authentication, 'user:read')")
 * public UserResponse getUser(String userId) {
 *     // 메서드 실행 후 권한 확인
 * }
 * }
 *
 * @see org.springframework.security.access.prepost.PreAuthorize
 * @see org.springframework.security.access.prepost.PostAuthorize
 * @see RbacQueryService
 */
@Component("rbac")
@RequiredArgsConstructor
@Slf4j
public class RbacPermissionEvaluator {

    private final RbacQueryService rbacQueryService;
    private final AuditLogService auditLogService;

    /**
     * 주어진 권한을 사용자가 보유하고 있는지 검증합니다.
     *
     * 동작 흐름:
     * 1. Authentication 객체 유효성 검사
     * 2. Principal(AuthPrincipal) 추출: tenantId, agentId 획득
     * 3. RbacQueryService.permissionsOf(tenantId, agentId) 호출
     *    -> 사용자의 모든 권한 코드 조회 (agent_roles -> role_permissions -> permissions)
     * 4. 요청한 permissionCode가 조회 결과에 포함되어 있는지 확인
     * 5. true/false 반환
     *
     * 보안:
     * - Authentication이 null이면 false 반환 (미인증 요청 거부)
     * - Principal이 null이면 false 반환 (인증 정보 부실 거부)
     * - tenantId를 필터링에 포함하여 멀티테넌시 데이터 보호
     *
     * 성능:
     * - RbacQueryService는 보통 캐시를 적용하여 DB 조회 최소화
     * - 권한 검사는 매 요청마다 발생하므로 캐싱 권장
     *
     * @param authentication 현재 인증 정보 (Spring Security에서 제공)
     *                       Principal으로 AuthPrincipal 포함 필수
     * @param permissionCode 검증할 권한 코드 (예: "user:manage", "org:view")
     *                       표준 형식: "{domain}:{action}" (소문자)
     *
     * @return true: 권한 보유 / false: 권한 없음, 인증 정보 없음, 또는 예외 발생
     *
     * @apiNote
     *  - 인증 필터(@Component 또는 SecurityConfig)에서 Authentication 객체의 Principal에
     *    AuthPrincipal(tenantId, agentId)을 설정해야 합니다.
     *  - 예: JWT 토큰 파싱, Keycloak 헤더 필터 등에서 Principal 주입
     *
     * 예외 안내:
     *  - ClassCastException: Principal이 AuthPrincipal이 아닐 경우 발생
     *    (인증 필터 설정을 확인하세요)
     *  - NullPointerException: tenantId/agentId 중 하나가 null일 경우 발생
     *    (Principal 설정 검증 필요)
     */
    public boolean hasPermission(Authentication authentication, String permissionCode) {
        long startTime = System.currentTimeMillis();

        // 인증 정보 검증
        if (authentication == null || authentication.getPrincipal() == null) {
            log.warn("[RBAC 권한 검증] 인증 정보 없음: authentication={}", authentication);
            return false;
        }

        try {
            // Principal에서 tenantId와 agentId 추출
            var principal = (AuthPrincipal) authentication.getPrincipal();
            UUID agentId = principal.agentId();
            String tenantId = principal.tenantId();

            log.trace("[RBAC 권한 검증 시작] agentId={}, tenantId={}, permissionCode={}",
                    agentId, tenantId, permissionCode);

            // 사용자의 모든 권한 조회 및 권한 검사
            var userPermissions = rbacQueryService.permissionsOf(tenantId, agentId);
            boolean hasPermission = userPermissions.contains(permissionCode);

            long duration = System.currentTimeMillis() - startTime;

            // 권한 검증 결과 로깅
            if (hasPermission) {
                log.debug("[RBAC 권한 검증 성공] agentId={}, permissionCode={}, 보유권한수={}, 소요시간={}ms",
                        agentId, permissionCode, userPermissions.size(), duration);
            } else {
                log.warn("[RBAC 권한 검증 실패] agentId={}, 요청권한={}, 보유권한={}, 소요시간={}ms",
                        agentId, permissionCode, userPermissions, duration);

                // 🔴 권한 거부 감사 로그 기록 (보안 감시)
                try {
                    auditLogService.recordAccessDenied(tenantId, agentId.toString(), permissionCode, userPermissions);
                } catch (Exception auditEx) {
                    // 감사 로그 기록 실패는 권한 검사 결과에 영향을 주지 않음
                    log.error("[RBAC] 감사 로그 기록 실패: agentId={}, permissionCode={}",
                            agentId, permissionCode, auditEx);
                }
            }

            return hasPermission;
        } catch (ClassCastException e) {
            // Principal이 AuthPrincipal이 아닌 경우
            log.error("[RBAC 권한 검증] Principal 타입 오류: 예상=AuthPrincipal, 실제={}, 권한={}",
                    authentication.getPrincipal().getClass().getSimpleName(), permissionCode, e);
            return false;
        } catch (NullPointerException e) {
            // tenantId 또는 agentId가 null인 경우
            log.error("[RBAC 권한 검증] Principal 정보 부실: 권한={}", permissionCode, e);
            return false;
        } catch (Exception e) {
            // 예상치 못한 예외
            log.error("[RBAC 권한 검증] 예외 발생: 권한={}, 메시지={}", permissionCode, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 인증 Principal 정보 (Record)
     *
     * Spring Security의 Authentication 객체에 포함되는 인가(Authorization) 정보입니다.
     *
     * 역할:
     * - 현재 로그인한 사용자의 테넌트 ID와 에이전트 ID를 보유
     * - 권한 검사(hasPermission) 시 사용자 식별 및 테넌트 필터링 용도
     *
     * 생성 방법:
     * 인증 필터에서 JWT/OAuth 토큰을 파싱한 후 Principal 설정:
     * {@code
     * // 인증 필터 예시
     * UsernamePasswordAuthenticationToken token =
     *     new UsernamePasswordAuthenticationToken(
     *         new AuthPrincipal(tenantId, UUID.fromString(agentId)),
     *         null,
     *         authorities  // GrantedAuthority 목록
     *     );
     * SecurityContextHolder.getContext().setAuthentication(token);
     * }
     *
     * @param tenantId 테넌트 ID (조직/회사 식별, 멀티테넌시 필터 용도)
     *                 예: "tenant-001", "acme-corp"
     *                 길이: 1~50자, 영문자/숫자/하이픈 만 사용
     *
     * @param agentId 에이전트(사용자) ID (UUID 형식)
     *                예: UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
     *                또는 문자열로 변환 후 저장: "550e8400-e29b-41d4-a716-446655440000"
     */
    public record AuthPrincipal(String tenantId, UUID agentId) {}
}
