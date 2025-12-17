package com.nexfron.identitymodulith.user.infrastructure.retry;

import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Supplier를 이용하여 DB 접근 메서드를 감싸고 재시도 로직을 적용하는 유틸리티 클래스.
 *
 * <p>사용 예시:</p>
 * <pre>{@code
 * // DB 조회 메서드를 감싸서 retry 적용
 * Agent agent = RetrySupplier.withRetry(
 *     () -> agentRepository.findById(agentId),
 *     3,    // 최대 시도 횟수
 *     1000  // 재시도 간격 (ms)
 * );
 * }</pre>
 */
@Log4j2
public final class RetrySupplier {

    private RetrySupplier() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * Supplier를 감싸서 실패 시 재시도하는 메서드.
     *
     * @param supplier      실행할 DB 접근 로직 (Supplier)
     * @param maxAttempts   최대 시도 횟수
     * @param delayMs       재시도 간 대기 시간 (밀리초)
     * @param <T>           반환 타입
     * @return Supplier의 결과값
     * @throws RuntimeException 모든 재시도 실패 시 마지막 예외를 던짐
     */
    public static <T> T withRetry(Supplier<T> supplier, int maxAttempts, long delayMs) {
        return withRetry(supplier, maxAttempts, delayMs, 1.0);
    }

    /**
     * Supplier를 감싸서 실패 시 Exponential Backoff로 재시도하는 메서드.
     *
     * @param supplier          실행할 DB 접근 로직 (Supplier)
     * @param maxAttempts       최대 시도 횟수
     * @param initialDelayMs    초기 재시도 대기 시간 (밀리초)
     * @param backoffMultiplier 대기 시간 증가 배수 (예: 2.0이면 매번 2배씩 증가)
     * @param <T>               반환 타입
     * @return Supplier의 결과값
     * @throws RuntimeException 모든 재시도 실패 시 마지막 예외를 던짐
     */
    public static <T> T withRetry(Supplier<T> supplier, int maxAttempts, long initialDelayMs, double backoffMultiplier) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts는 1 이상이어야 합니다.");
        }

        RuntimeException lastException = null;
        long currentDelay = initialDelayMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return supplier.get();
            } catch (RuntimeException e) {
                lastException = e;
                log.warn("DB 연결 실패 (시도 {}/{}): {}", attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    sleep(currentDelay);
                    currentDelay = (long) (currentDelay * backoffMultiplier);
                }
            }
        }

        // 마지막 예외를 그대로 rethrow
        throw lastException;
    }

    /**
     * 반환값이 없는 Runnable을 감싸서 재시도하는 메서드.
     *
     * @param runnable    실행할 DB 접근 로직 (Runnable)
     * @param maxAttempts 최대 시도 횟수
     * @param delayMs     재시도 간 대기 시간 (밀리초)
     * @throws RuntimeException 모든 재시도 실패 시 마지막 예외를 던짐
     */
    public static void withRetry(Runnable runnable, int maxAttempts, long delayMs) {
        withRetry(() -> {
            runnable.run();
            return null;
        }, maxAttempts, delayMs);
    }

    /**
     * 기본 설정으로 재시도 (3회, 500ms 간격, 2배 backoff).
     *
     * @param supplier 실행할 DB 접근 로직
     * @param <T>      반환 타입
     * @return Supplier의 결과값
     */
    public static <T> T withDefaultRetry(Supplier<T> supplier) {
        return withRetry(supplier, 3, 500, 2.0);
    }

    /**
     * DB 연결용 설정으로 재시도 (5회, 1초 간격, 2배 backoff).
     *
     * @param supplier 실행할 DB 접근 로직
     * @param <T>      반환 타입
     * @return Supplier의 결과값
     */
    public static <T> T withDatabaseRetry(Supplier<T> supplier) {
        return withRetry(supplier, 5, 1000, 2.0);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재시도 대기 중 인터럽트 발생", e);
        }
    }
}
