package com.identitymodulith.user.infrastructure.batch;

import com.identitymodulith.user.application.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상담사 퇴사 처리 배치 스케줄러
 * <p>
 * 예약 삭제 정책(SCHEDULED)으로 퇴사 처리된 상담사의 개인정보를
 * 자동으로 익명화하는 배치 작업입니다.
 * </p>
 *
 * <h2>실행 일정</h2>
 * - 매일 자정(00:00) 실행
 * - timezone: Asia/Seoul
 *
 * <h2>처리 로직</h2>
 * 1. scheduledDeleteAt <= 현재 시간인 퇴사 상담사 조회
 * 2. 개인정보 익명화 (email, phone, name 등)
 * 3. 상담사 정보 저장
 * 4. 처리 결과 로깅
 *
 * <h2>오류 처리</h2>
 * - 데이터베이스 오류: DatabaseRetrySupplier를 통한 자동 재시도
 * - 기타 오류: 로깅 후 계속 진행 (배치 작업 중단 방지)
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRetirementBatchScheduler {

    private final AgentService agentService;

    /**
     * 예약 삭제 대상 상담사의 개인정보를 익명화합니다.
     * <p>
     * 스케줄: 매일 자정(00:00) 실행
     * </p>
     *
     * <h3>처리 대상</h3>
     * - status = RETIRED
     * - retireDeletePolicy = SCHEDULED
     * - scheduledDeleteAt <= 현재 시간
     *
     * <h3>처리 결과</h3>
     * - loginId: ANONYMOUS_xxx로 변경
     * - name: "Anonymous"로 변경
     * - email, phone, employeeId: null로 초기화
     * - password: null로 초기화 (로그인 불가)
     *
     * @return 익명화된 상담사 수
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void executeScheduledDeletion() {
        try {
            log.info("[배치] 예약 삭제 상담사 익명화 작업 시작");

            int count = agentService.deleteScheduledRetiredAgents();

            log.info("[배치] 예약 삭제 상담사 익명화 완료: {} 명", count);

        } catch (Exception e) {
            log.error("[배치] 예약 삭제 상담사 익명화 작업 중 오류 발생", e);
            // 배치 작업 중단 방지 - 오류는 로깅하고 계속 진행
        }
    }

    /**
     * 테스트/운영 용도 수동 실행 메서드
     * <p>
     * 스케줄러가 정상 작동하는지 확인할 때 사용합니다.
     * </p>
     *
     * @return 익명화된 상담사 수
     */
    @Transactional
    public int executeScheduledDeletionManually() {
        log.info("[수동 배치] 예약 삭제 상담사 익명화 작업 시작");
        int count = agentService.deleteScheduledRetiredAgents();
        log.info("[수동 배치] 예약 삭제 상담사 익명화 완료: {} 명", count);
        return count;
    }
}
