package com.identitymodulith.user.infrastructure.batch;

import com.identitymodulith.user.domain.model.Agent;
import com.identitymodulith.user.domain.model.AgentStatus;
import com.identitymodulith.user.infrastructure.persistence.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상담사 퇴사 처리 배치 스케줄러
 *
 * <p>예약 삭제 정책(SCHEDULED)으로 퇴사 처리된 상담사의 개인정보를
 * 자동으로 익명화하는 배치 작업입니다.</p>
 *
 * <h2>실행 일정</h2>
 * - 매일 자정(00:00 KST) 실행
 *
 * <h2>처리 대상</h2>
 * - status = RETIRED, scheduledDeleteAt &lt;= 현재 시간
 *
 * <h2>처리 결과</h2>
 * - loginId → "ANONYMOUS_" + UUID
 * - name, email, phone, employeeId → 익명화
 * - scheduledDeleteAt → null (완료 표시)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRetirementBatchScheduler {

    private final AgentRepository agentRepository;

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
        log.info("[배치] 예약 삭제 상담사 익명화 시작");
        long start = System.currentTimeMillis();
        int success = 0, failure = 0;

        try {
            List<Agent> targets = agentRepository.findAgentsForScheduledDelete(
                    AgentStatus.RETIRED, LocalDateTime.now());

            if (targets.isEmpty()) {
                log.info("[배치] 예약 삭제 대상 없음");
                return;
            }

            log.info("[배치] 예약 삭제 대상: {}명", targets.size());

            for (Agent agent : targets) {
                try {
                    agent.anonymize();
                    agent.setScheduledDeleteAt(null);
                    agentRepository.save(agent);
                    success++;
                } catch (Exception e) {
                    failure++;
                    log.error("[배치] 익명화 실패 - agentId={}, 오류: {}", agent.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("[배치] 예약 삭제 배치 오류", e);
        } finally {
            log.info("[배치] 완료 - 성공: {}, 실패: {}, 소요: {}ms",
                    success, failure, System.currentTimeMillis() - start);
        }
    }
}
