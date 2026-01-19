package com.nexfron.identitymodulith.common.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 성능 모니터링 AOP
 *
 * <h2>기능:</h2>
 * <ul>
 *   <li>메서드 실행 시간 측정</li>
 *   <li>슬로우 쿼리 감지 (1초 초과)</li>
 *   <li>성능 로그 자동 기록</li>
 * </ul>
 *
 * <h3>적용 범위:</h3>
 * - RBAC 서비스: 역할/권한 관리
 * - Organization 서비스: 부서 관리
 *
 * <h3>로그 레벨:</h3>
 * - INFO: 정상 실행 (100ms 이상)
 * - WARN: 슬로우 쿼리 (1000ms 이상)
 *
 * @author Infrastructure Team
 * @version 1.0
 */
@Slf4j
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final long SLOW_QUERY_THRESHOLD_MS = 1000; // 1초
    private static final long LOG_THRESHOLD_MS = 100; // 100ms

    /**
     * RBAC 서비스 성능 모니터링
     *
     * <p>RbacManagementServiceImpl의 모든 public 메서드 실행 시간을 측정합니다.
     */
    @Around("execution(* com.nexfron.identitymodulith.rbac.application.RbacManagementServiceImpl.*(..))")
    public Object monitorRbacPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "RBAC");
    }

    /**
     * Organization 서비스 성능 모니터링
     *
     * <p>DepartmentService의 모든 public 메서드 실행 시간을 측정합니다.
     */
    @Around("execution(* com.nexfron.identitymodulith.organization.application.service.DepartmentService.*(..))")
    public Object monitorOrganizationPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "ORG");
    }

    /**
     * 성능 모니터링 공통 로직
     *
     * @param joinPoint AOP 조인 포인트
     * @param modulePrefix 모듈 접두사 (로그 구분용)
     * @return 메서드 실행 결과
     * @throws Throwable 메서드 실행 중 발생한 예외
     */
    private Object monitorPerformance(ProceedingJoinPoint joinPoint, String modulePrefix) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();

        try {
            // 메서드 실행
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;

            // ✅ P1-3: 성능 로깅
            if (duration >= SLOW_QUERY_THRESHOLD_MS) {
                // 슬로우 쿼리 경고
                log.warn("[{} 성능 경고] 메서드={}, 소요시간={}ms (임계값: {}ms 초과)",
                        modulePrefix, methodName, duration, SLOW_QUERY_THRESHOLD_MS);
            } else if (duration >= LOG_THRESHOLD_MS) {
                // 정상 범위 INFO 로깅
                log.info("[{} 성능] 메서드={}, 소요시간={}ms",
                        modulePrefix, methodName, duration);
            } else {
                // 빠른 실행은 DEBUG 레벨
                log.debug("[{} 성능] 메서드={}, 소요시간={}ms",
                        modulePrefix, methodName, duration);
            }

            return result;

        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;

            // 예외 발생 시에도 소요 시간 로깅
            log.error("[{} 성능 - 예외] 메서드={}, 소요시간={}ms, 예외={}",
                    modulePrefix, methodName, duration, e.getClass().getSimpleName());

            throw e;
        }
    }

    /**
     * 쿼리 실행 성능 모니터링
     *
     * <p>JPA Repository 메서드의 실행 시간을 측정합니다.
     * N+1 문제나 슬로우 쿼리를 감지할 수 있습니다.
     */
    @Around("execution(* com.nexfron.identitymodulith..repository.*Repository.*(..))")
    public Object monitorRepositoryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;

            // ✅ P1-3: Repository 쿼리 성능 로깅
            if (duration >= SLOW_QUERY_THRESHOLD_MS) {
                log.warn("[DB 슬로우 쿼리] Repository={}, 메서드={}, 소요시간={}ms",
                        className, methodName, duration);
            } else if (duration >= LOG_THRESHOLD_MS) {
                log.debug("[DB 쿼리] Repository={}, 메서드={}, 소요시간={}ms",
                        className, methodName, duration);
            }

            return result;

        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DB 쿼리 실패] Repository={}, 메서드={}, 소요시간={}ms, 예외={}",
                    className, methodName, duration, e.getClass().getSimpleName());
            throw e;
        }
    }

    /**
     * 캐시 성능 모니터링
     *
     * <p>캐시 조회 시간을 측정하여 캐시 효율성을 분석합니다.
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object monitorCachePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long duration = System.currentTimeMillis() - startTime;

        // ✅ P1-3: 캐시 성능 로깅
        if (duration >= LOG_THRESHOLD_MS) {
            log.info("[캐시 조회] 메서드={}, 소요시간={}ms (캐시 미스 가능)",
                    methodName, duration);
        } else {
            log.debug("[캐시 조회] 메서드={}, 소요시간={}ms (캐시 히트)",
                    methodName, duration);
        }

        return result;
    }
}

