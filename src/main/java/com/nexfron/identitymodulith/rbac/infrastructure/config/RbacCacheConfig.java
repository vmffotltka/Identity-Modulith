package com.nexfron.identitymodulith.rbac.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RBAC 캐시 설정
 *
 * <h2>목적:</h2>
 * Spring Cache Abstraction을 사용하여 RBAC 조회 성능 개선
 * - 반복적인 권한 조회 최소화
 * - DB 부하 감소
 * - 응답 시간 개선
 *
 * <h2>캐시 전략:</h2>
 * 1. userPermissions: 사용자의 권한 캐시
 *    - 사용자의 모든 권한을 메모리에 저장
 *    - 권한 할당/회수 시 무효화
 *    - 유효 시간: 사용자 로그인 세션 동안
 *
 * 2. roleDefinitions: 역할 정의 캐시
 *    - 역할의 정보를 메모리에 저장
 *    - 역할 수정 시 무효화
 *    - 유효 시간: 수동 무효화까지
 *
 * 3. accessibleDepts: 접근 가능 부서 캐시
 *    - 사용자가 접근 가능한 부서 범위 캐시
 *    - 부서 이동, 권한 변경 시 무효화
 *
 * <h2>구현 메모:</h2>
 * 현재는 ConcurrentMapCacheManager 사용 (메모리 기반)
 * - 단일 서버 환경에 적합
 * - 분산 서버 환경: Redis CacheManager 사용 권장
 *   {@code
 *   @Bean
 *   public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
 *       return new RedisCacheManager.create(connectionFactory);
 *   }
 *   }
 *
 * <h2>캐시 무효화:</h2>
 * 1. assignPermissionToRole() 후 @CacheEvict 적용
 * 2. revokePermissionFromRole() 후 @CacheEvict 적용
 * 3. assignRoleToAgent() 후 @CacheEvict 적용
 * 4. revokeRoleFromAgent() 후 @CacheEvict 적용
 *
 * <h2>모니터링:</h2>
 * - Actuator /metrics/cache.* 엔드포인트로 캐시 통계 확인
 * - 캐시 히트율, 미스율, 제거율 등 모니터링
 *
 * @see org.springframework.cache.annotation.Cacheable
 * @see org.springframework.cache.annotation.CacheEvict
 * @see org.springframework.cache.annotation.CachePut
 */
@Configuration
@EnableCaching
@Slf4j
public class RbacCacheConfig {

    /**
     * Spring Cache Manager 설정
     *
     * 캐시 목록:
     * 1. "userPermissions" - 사용자 권한 캐시
     *    - Key: "tenantId:agentId"
     *    - Value: Set<String> (권한 코드 집합)
     *    - 사용처: RbacQueryServiceImpl.permissionsOf()
     *
     * 2. "roleDefinitions" - 역할 정의 캐시
     *    - Key: "tenantId:roleName"
     *    - Value: RoleDto
     *    - 사용처: RbacManagementServiceImpl.getRoleByName()
     *
     * 3. "accessibleDepts" - 접근 가능 부서 캐시
     *    - Key: "tenantId:userId:dataScope"
     *    - Value: Set<String> (부서 ID 집합)
     *    - 사용처: DepartmentServiceImpl.getAccessibleDepartmentIds() (통합됨)
     *
     * @return CacheManager 빈
     */
    @Bean
    public CacheManager cacheManager() {
        log.info("[RBAC 캐시] 캐시 매니저 초기화: ConcurrentMapCacheManager");

        // 메모리 기반 캐시 매니저 생성
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                "userPermissions",       // 사용자 권한
                "roleDefinitions",       // 역할 정의
                "accessibleDepts"        // 접근 가능 부서 (Organization 모듈)
        );

        log.info("[RBAC 캐시] 캐시 설정 완료: {} 개 캐시 활성화", 3);
        return cacheManager;
    }
}

