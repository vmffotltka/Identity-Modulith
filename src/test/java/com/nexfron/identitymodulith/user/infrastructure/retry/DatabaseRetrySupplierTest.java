package com.nexfron.identitymodulith.user.infrastructure.retry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * DatabaseRetrySupplier의 withRetry() 메서드를 테스트합니다.
 *
 * <p>테스트 시나리오:</p>
 * <ul>
 *     <li>성공 케이스 - 첫 번째 시도에서 성공</li>
 *     <li>재시도 후 성공 - 1~2회 실패 후 성공</li>
 *     <li>모든 재시도 실패 - 3회 모두 DataAccessException 발생</li>
 *     <li>비 DataAccessException - 재시도 없이 즉시 전파</li>
 * </ul>
 */
class DatabaseRetrySupplierTest {

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCase {

        @Test
        @DisplayName("첫 번째 시도에서 성공하면 결과를 반환한다")
        void shouldReturnResultOnFirstAttempt() {
            // given
            String expected = "success";
            Supplier<String> supplier = () -> expected;

            // when
            String result = DatabaseRetrySupplier.withRetry(supplier);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("null 값도 정상적으로 반환한다")
        void shouldReturnNullWhenSupplierReturnsNull() {
            // given
            Supplier<String> supplier = () -> null;

            // when
            String result = DatabaseRetrySupplier.withRetry(supplier);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("재시도 후 성공 케이스")
    class RetrySuccessCase {

        @Test
        @DisplayName("첫 번째 실패 후 두 번째 시도에서 성공한다")
        void shouldSucceedOnSecondAttempt() {
            // given
            AtomicInteger attempts = new AtomicInteger(0);
            String expected = "success";

            Supplier<String> supplier = () -> {
                int currentAttempt = attempts.incrementAndGet();
                if (currentAttempt == 1) {
                    throw new QueryTimeoutException("DB 연결 타임아웃");
                }
                return expected;
            };

            // when
            String result = DatabaseRetrySupplier.withRetry(supplier);

            // then
            assertThat(result).isEqualTo(expected);
            assertThat(attempts.get()).isEqualTo(2);
        }

        @Test
        @DisplayName("두 번 실패 후 세 번째 시도에서 성공한다")
        void shouldSucceedOnThirdAttempt() {
            // given
            AtomicInteger attempts = new AtomicInteger(0);
            String expected = "success";

            Supplier<String> supplier = () -> {
                int currentAttempt = attempts.incrementAndGet();
                if (currentAttempt < 3) {
                    throw new TransientDataAccessResourceException("일시적 DB 오류");
                }
                return expected;
            };

            // when
            String result = DatabaseRetrySupplier.withRetry(supplier);

            // then
            assertThat(result).isEqualTo(expected);
            assertThat(attempts.get()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("모든 재시도 실패 케이스")
    class AllRetriesFailCase {

        @Test
        @DisplayName("세 번 모두 실패하면 마지막 예외를 던진다")
        void shouldThrowLastExceptionAfterAllRetriesFail() {
            // given
            AtomicInteger attempts = new AtomicInteger(0);
            String lastErrorMessage = "세 번째 실패";

            Supplier<String> supplier = () -> {
                int currentAttempt = attempts.incrementAndGet();
                String message = currentAttempt == 3 ? lastErrorMessage : "실패 " + currentAttempt;
                throw new QueryTimeoutException(message);
            };

            // when & then
            assertThatThrownBy(() -> DatabaseRetrySupplier.withRetry(supplier))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining(lastErrorMessage);

            assertThat(attempts.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("Mockito로 정확히 3번 호출되는지 검증한다")
        @SuppressWarnings("unchecked")
        void shouldCallSupplierExactlyThreeTimes() {
            // given
            Supplier<String> mockSupplier = mock(Supplier.class);
            when(mockSupplier.get()).thenThrow(new QueryTimeoutException("DB 오류"));

            // when & then
            assertThatThrownBy(() -> DatabaseRetrySupplier.withRetry(mockSupplier))
                    .isInstanceOf(DataAccessException.class);

            verify(mockSupplier, times(3)).get();
        }
    }

    @Nested
    @DisplayName("비 DataAccessException 케이스")
    class NonDataAccessExceptionCase {

        @Test
        @DisplayName("RuntimeException은 재시도 없이 즉시 전파된다")
        void shouldNotRetryForRuntimeException() {
            // given
            AtomicInteger attempts = new AtomicInteger(0);
            String errorMessage = "비즈니스 로직 오류";

            Supplier<String> supplier = () -> {
                attempts.incrementAndGet();
                throw new RuntimeException(errorMessage);
            };

            // when & then
            assertThatThrownBy(() -> DatabaseRetrySupplier.withRetry(supplier))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(errorMessage);

            assertThat(attempts.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("IllegalArgumentException은 재시도 없이 즉시 전파된다")
        void shouldNotRetryForIllegalArgumentException() {
            // given
            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> supplier = () -> {
                attempts.incrementAndGet();
                throw new IllegalArgumentException("잘못된 인자");
            };

            // when & then
            assertThatThrownBy(() -> DatabaseRetrySupplier.withRetry(supplier))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(attempts.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("NullPointerException은 재시도 없이 즉시 전파된다")
        void shouldNotRetryForNullPointerException() {
            // given
            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> supplier = () -> {
                attempts.incrementAndGet();
                throw new NullPointerException("null 참조");
            };

            // when & then
            assertThatThrownBy(() -> DatabaseRetrySupplier.withRetry(supplier))
                    .isInstanceOf(NullPointerException.class);

            assertThat(attempts.get()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("다양한 DataAccessException 서브클래스 케이스")
    class DataAccessExceptionSubclassCase {

        @Test
        @DisplayName("QueryTimeoutException은 재시도 대상이다")
        void shouldRetryForQueryTimeoutException() {
            // given
            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> supplier = () -> {
                int currentAttempt = attempts.incrementAndGet();
                if (currentAttempt == 1) {
                    throw new QueryTimeoutException("쿼리 타임아웃");
                }
                return "success";
            };

            // when
            String result = DatabaseRetrySupplier.withRetry(supplier);

            // then
            assertThat(result).isEqualTo("success");
            assertThat(attempts.get()).isEqualTo(2);
        }

        @Test
        @DisplayName("TransientDataAccessResourceException은 재시도 대상이다")
        void shouldRetryForTransientDataAccessResourceException() {
            // given
            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> supplier = () -> {
                int currentAttempt = attempts.incrementAndGet();
                if (currentAttempt == 1) {
                    throw new TransientDataAccessResourceException("일시적 리소스 오류");
                }
                return "success";
            };

            // when
            String result = DatabaseRetrySupplier.withRetry(supplier);

            // then
            assertThat(result).isEqualTo("success");
            assertThat(attempts.get()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Exponential Backoff 검증")
    class ExponentialBackoffCase {

        @Test
        @DisplayName("재시도 시 대기 시간이 적용된다 (최소 1초 이상 소요)")
        void shouldApplyDelayBetweenRetries() {
            // given
            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> supplier = () -> {
                int currentAttempt = attempts.incrementAndGet();
                if (currentAttempt < 2) {
                    throw new QueryTimeoutException("DB 오류");
                }
                return "success";
            };

            // when
            long startTime = System.currentTimeMillis();
            DatabaseRetrySupplier.withRetry(supplier);
            long elapsedTime = System.currentTimeMillis() - startTime;

            // then
            // 첫 번째 재시도 대기 시간이 1000ms 이므로 최소 900ms 이상 걸려야 함
            assertThat(elapsedTime).isGreaterThanOrEqualTo(900L);
        }

        @Test
        @DisplayName("두 번 재시도 시 총 대기 시간은 약 3초 이상이다 (1초 + 2초)")
        void shouldApplyExponentialBackoff() {
            // given
            AtomicInteger attempts = new AtomicInteger(0);

            Supplier<String> supplier = () -> {
                int currentAttempt = attempts.incrementAndGet();
                if (currentAttempt < 3) {
                    throw new QueryTimeoutException("DB 오류");
                }
                return "success";
            };

            // when
            long startTime = System.currentTimeMillis();
            DatabaseRetrySupplier.withRetry(supplier);
            long elapsedTime = System.currentTimeMillis() - startTime;

            // then
            // 1차 대기: 1000ms, 2차 대기: 2000ms = 총 3000ms
            // 테스트 안정성을 위해 2700ms 이상으로 검증
            assertThat(elapsedTime).isGreaterThanOrEqualTo(2700L);
        }
    }

    @Nested
    @DisplayName("제네릭 타입 테스트")
    class GenericTypeCase {

        @Test
        @DisplayName("Integer 타입 반환을 지원한다")
        void shouldSupportIntegerType() {
            // given
            Supplier<Integer> supplier = () -> 42;

            // when
            Integer result = DatabaseRetrySupplier.withRetry(supplier);

            // then
            assertThat(result).isEqualTo(42);
        }

        @Test
        @DisplayName("복잡한 객체 타입 반환을 지원한다")
        void shouldSupportComplexObjectType() {
            // given
            record TestData(String name, int value) {}
            TestData expected = new TestData("test", 100);
            Supplier<TestData> supplier = () -> expected;

            // when
            TestData result = DatabaseRetrySupplier.withRetry(supplier);

            // then
            assertThat(result).isEqualTo(expected);
            assertThat(result.name()).isEqualTo("test");
            assertThat(result.value()).isEqualTo(100);
        }
    }
}
