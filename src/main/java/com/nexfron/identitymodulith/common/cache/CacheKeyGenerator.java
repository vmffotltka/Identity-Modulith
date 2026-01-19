package com.nexfron.identitymodulith.common.cache;

/**
 * 캐시 키 생성 유틸리티
 *
 * <h2>목적:</h2>
 * - 캐시 키 생성 로직 중앙화
 * - 오타 방지 및 일관성 보장
 * - 테넌트 격리 보장
 *
 * <h2>캐시 키 형식:</h2>
 * <pre>
 * {cacheName}::{tenantId}::{identifier}
 * 예: userPermissions::tenant-001::user-123
 * </pre>
 *
 * @author Identity System Team
 * @version 1.0
 */
public final class CacheKeyGenerator {

    /**
     * 구분자 (일관성을 위해 상수화)
     */
    private static final String DELIMITER = "::";

    /**
     * 생성자 private (유틸리티 클래스)
     */
    private CacheKeyGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 사용자 권한 캐시 키 생성
     *
     * <p>캐시명: userPermissions
     *
     * @param tenantId 테넌트 ID
     * @param userId   사용자 ID
     * @return 캐시 키 (예: "userPermissions::tenant-001::user-123")
     */
    public static String userPermissions(String tenantId, String userId) {
        return buildKey("userPermissions", tenantId, userId);
    }

    /**
     * 역할 권한 캐시 키 생성
     *
     * <p>캐시명: rolePermissions
     *
     * @param tenantId 테넌트 ID
     * @param roleName 역할명
     * @return 캐시 키 (예: "rolePermissions::tenant-001::ADMIN")
     */
    public static String rolePermissions(String tenantId, String roleName) {
        return buildKey("rolePermissions", tenantId, roleName);
    }

    /**
     * 접근 가능 부서 캐시 키 생성
     *
     * <p>캐시명: accessibleDepartments
     *
     * @param tenantId 테넌트 ID
     * @param userId   사용자 ID
     * @return 캐시 키 (예: "accessibleDepartments::tenant-001::user-123")
     */
    public static String accessibleDepartments(String tenantId, String userId) {
        return buildKey("accessibleDepartments", tenantId, userId);
    }

    /**
     * 조직도 트리 캐시 키 생성
     *
     * <p>캐시명: departmentTree
     *
     * @param tenantId 테넌트 ID
     * @return 캐시 키 (예: "departmentTree::tenant-001")
     */
    public static String departmentTree(String tenantId) {
        return buildKey("departmentTree", tenantId);
    }

    /**
     * 부서 통계 캐시 키 생성
     *
     * <p>캐시명: departmentStatistics
     *
     * @param tenantId 테넌트 ID
     * @param deptId   부서 ID
     * @return 캐시 키 (예: "departmentStatistics::tenant-001::dept-123")
     */
    public static String departmentStatistics(String tenantId, String deptId) {
        return buildKey("departmentStatistics", tenantId, deptId);
    }

    /**
     * 캐시 키 빌더 (내부 헬퍼 메서드)
     *
     * @param parts 캐시 키 구성 요소들
     * @return 구분자로 결합된 캐시 키
     */
    private static String buildKey(String... parts) {
        if (parts == null || parts.length == 0) {
            throw new IllegalArgumentException("Cache key parts cannot be null or empty");
        }

        // Null 체크 및 빈 문자열 검증
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Cache key part cannot be null or empty: " + java.util.Arrays.toString(parts));
            }
        }

        return String.join(DELIMITER, parts);
    }
}

