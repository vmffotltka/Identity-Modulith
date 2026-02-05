package com.nexfron.identitymodulith.rbac.domain;

/**
 * 역할 타입
 *
 * RBAC 시스템에서 역할을 2차원으로 분류:
 * - POSITION: 직급/권한 레벨 (ADMIN, TEAM_LEAD, AGENT)
 * - CHANNEL: 업무 채널 (VOICE_INBOUND, VOICE_OUTBOUND, CHAT, EMAIL, CALLBACK)
 *
 * 규칙:
 * - 모든 상담사는 정확히 1개의 POSITION 역할 필요
 * - 0개 이상의 CHANNEL 역할 가능
 */
public enum RoleType {
    /**
     * 직급 역할 (필수, 1개만)
     * 예: ADMIN, TEAM_LEAD, AGENT
     */
    POSITION,

    /**
     * 채널 역할 (선택, 여러 개 가능)
     * 예: VOICE_INBOUND, VOICE_OUTBOUND, CHAT, EMAIL, CALLBACK
     */
    CHANNEL
}
