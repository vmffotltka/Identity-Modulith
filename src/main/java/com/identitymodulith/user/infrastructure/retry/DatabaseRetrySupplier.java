package com.identitymodulith.user.infrastructure.retry;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Resilience4J 기반 DB 재시도 유틸리티
 *
 * <p>DB 연결 실패(DataAccessException) 시 Exponential Backoff로 최대 3회 재시도합니다.</p>
 * <ul>
 *   <li>1차 재시도: 1,000ms 대기</li>
 *   <li>2차 재시도: 2,000ms 대기 (multiplier = 2)</li>
 * </ul>
 *
 * <p>사용 예시:</p>
 * <pre>{@code
 * Agent agent = DatabaseRetrySupplier.withRetry(
 *     () -> agentRepository.findById(agentId)
 * );
 * }</pre>
 */
@Slf4j
public final class DatabaseRetrySupplier {

    private static final Retry RETRY = Retry.of("db-retry",
            RetryConfig.<Object>custom()
                    .maxAttempts(3)
                    .intervalFunction(attempt -> Duration.ofMillis(1000L * (1L << (attempt - 1))).toMillis())
                    .retryExceptions(DataAccessException.class)
                    .build());

    static {
        RETRY.getEventPublisher()
                .onRetry(e -> log.warn("[DB-Retry] 재시도 {}/3 - 대기 {}ms - 원인: {}",
                        e.getNumberOfRetryAttempts(),
                        1000L * (1L << (e.getNumberOfRetryAttempts() - 1)),
                        e.getLastThrowable().getMessage()))
                .onError(e -> log.error("[DB-Retry] 모든 재시도 실패 - 원인: {}",
                        e.getLastThrowable().getMessage()));
    }

    private DatabaseRetrySupplier() {}

    /**
     * Supplier를 Resilience4J Retry로 감싸서 실행합니다.
     * DataAccessException 발생 시에만 재시도(Exponential Backoff)하며,
     * 그 외 예외는 즉시 전파됩니다.
     *
     * @param supplier 실행할 DB 접근 로직
     * @param <T>      반환 타입
     * @return 실행 결과
     */
    public static <T> T withRetry(Supplier<T> supplier) {
        return Retry.decorateSupplier(RETRY, supplier).get();
    }
}
