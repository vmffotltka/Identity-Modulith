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
 * 퇴사자 예약 삭제 스케줄러
 * <p>
 * 매일 자정에 실행되어 scheduledDeleteAt에 도달한 퇴사자의 개인정보를 익명화합니다.
 * </p>
 *
 * <h3>처리 흐름</h3>
 * <ol>
 *   <li>대상 조회: status=RETIRED AND scheduledDeleteAt <= NOW()</li>
 *   <li>개인정보 익명화:
 *     <ul>
 *       <li>loginId → "ANONYMOUS_" + UUID</li>
 *       <li>name → "Anonymous"</li>
 *       <li>email → null</li>
 *       <li>phone → null</li>
 *       <li>employeeId → null</li>
 *     </ul>
 *   </li>
 *   <li>scheduledDeleteAt = null (처리 완료 표시)</li>
 * </ol>
 *
 * @see Agent#anonymize()
 * @see Agent.RetireDeletePolicy#SCHEDULED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledDeleteBatchService {

    private final AgentRepository agentRepository;

    /**
     * 매일 자정에 예약 삭제 배치 실행
     * <p>
     * cron: "0 0 0 * * *" = 매일 00:00:00에 실행
     * </p>
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processScheduledDeletes() {
        log.info("[USER] 예약 삭제 배치 시작");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 대상 조회
            List<Agent> targets = agentRepository.findAgentsForScheduledDelete(
                    AgentStatus.RETIRED,
                    LocalDateTime.now()
            );

            if (targets.isEmpty()) {
                log.info("[USER] 예약 삭제 대상 없음");
                return;
            }

            log.info("[USER] 예약 삭제 대상: {}명", targets.size());

            // 2. 각 대상 익명화
            int successCount = 0;
            int failureCount = 0;

            for (Agent agent : targets) {
                try {
                    // 익명화 처리
                    agent.anonymize();

                    // scheduledDeleteAt 초기화 (처리 완료 표시)
                    agent.setScheduledDeleteAt(null);

                    agentRepository.save(agent);
                    successCount++;

                    log.debug("[USER] 익명화 완료 - agentId={}", agent.getId());
                } catch (Exception e) {
                    failureCount++;
                    log.error("[USER] 익명화 실패 - agentId={}, 오류: {}", agent.getId(), e.getMessage(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            // 3. 처리 결과 로깅
            log.info("[USER] 예약 삭제 배치 완료 - 성공: {}, 실패: {}, 소요시간: {}ms",
                    successCount, failureCount, duration);

        } catch (Exception e) {
            log.error("[USER] 예약 삭제 배치 오류", e);
            throw e;
        }
    }
}
