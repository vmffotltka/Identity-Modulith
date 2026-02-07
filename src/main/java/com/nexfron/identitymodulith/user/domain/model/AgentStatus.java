package com.nexfron.identitymodulith.user.domain.model;

/**
 * 상담사(Agent) 상태 열거형
 * - ACTIVE: 활성 (정상 업무 가능)
 * - SUSPENDED: 정지 (임시로 로그인 및 업무 불가, 복귀 가능)
 * - RETIRED: 퇴사 (영구 퇴사, 복구 불가능)
 */
public enum AgentStatus {
    ACTIVE,      // 활성 상태
    SUSPENDED,   // 정지 상태 (임시)
    RETIRED      // 퇴사 상태 (영구)
}