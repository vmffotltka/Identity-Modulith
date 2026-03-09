package com.identitymodulith.user.infrastructure.retry;

import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataAccessException;

import java.util.function.Supplier;

/**
 * Supplier를 이용하여 DB 접근 메서드를 감싸고 재시도 로직을 적용하는 유틸리티 클래스.
 *
 * <p>사용 예시:</p>
 * <pre>{@code
 * // DB 조회 메서드를 감싸서 retry 적용
 * Agent agent = DatabaseRetrySupplier.withRetry(
 *     () -> agentRepository.findById(agentId)
 * );
 * }</pre>
 */
// TODO : Resilence4J를 이용하여 Retry 구현 필요

@Log4j2
public final class DatabaseRetrySupplier {

    /** 최대 시도 횟수 */
    private static final int MAX_ATTEMPTS = 3;

    /** 초기 재시도 대기 시간 (밀리초) */
    private static final long INITIAL_DELAY_MS = 1000L;

    /** 대기 시간 증가 배수 (Exponential Backoff) */
    private static final double BACKOFF_MULTIPLIER = 2.0;

    private DatabaseRetrySupplier() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * Supplier를 감싸서 실패 시 Exponential Backoff로 재시도하는 메서드.
     * DataAccessException(DB 관련 예외)만 retry 대상이며, 그 외 예외는 즉시 rethrow합니다.
     *
     * @param supplier 실행할 DB 접근 로직 (Supplier)
     * @param <T>      반환 타입
     * @return Supplier의 결과값
     * @throws DataAccessException 모든 재시도 실패 시 마지막 DB 예외를 던짐
     * @throws RuntimeException    DB 예외가 아닌 경우 즉시 던짐 (retry 없음)
     */
    public static <T> T withRetry(Supplier<T> supplier) {
        DataAccessException lastException = null;
        long currentDelay = INITIAL_DELAY_MS;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return supplier.get();
            } catch (DataAccessException e) {
                // DB 관련 예외만 retry 대상
                lastException = e;
                log.warn("DB 연결 실패 (시도 {}/{}): {}", attempt, MAX_ATTEMPTS, e.getMessage());

                if (attempt < MAX_ATTEMPTS) {
                    sleep(currentDelay);
                    currentDelay = (long) (currentDelay * BACKOFF_MULTIPLIER);
                }
            }
            // DataAccessException이 아닌 예외(BusinessException 등)는 catch하지 않고 그대로 전파
        }

        // 마지막 DB 예외를 그대로 rethrow
        throw lastException;
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
