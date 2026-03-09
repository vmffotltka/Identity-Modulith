package com.identitymodulith.rbac.domain;

/**
 * 채널 타입 (고정 Enum)
 *
 * 컨택센터에서 지원하는 상담 채널을 정의합니다.
 * 각 채널 타입은 CHANNEL 역할로 매핑됩니다.
 *
 * 예시:
 * - 상담사가 VOICE_INBOUND 역할을 가지면 인바운드 전화 상담 가능
 * - 상담사가 CHAT 역할을 가지면 채팅 상담 가능
 * - 여러 채널 역할을 동시에 가질 수 있음 (멀티채널 상담사)
 */
public enum ChannelType {
    /**
     * 인바운드 전화
     * - 고객이 걸어오는 전화 상담
     */
    VOICE_INBOUND,

    /**
     * 아웃바운드 전화
     * - 상담사가 고객에게 거는 전화 상담
     */
    VOICE_OUTBOUND,

    /**
     * 채팅 상담
     * - 실시간 문자 채팅 상담
     */
    CHAT,

    /**
     * 이메일 상담
     * - 이메일을 통한 비실시간 상담
     */
    EMAIL,

    /**
     * 콜백 관리
     * - 고객 요청 시 다시 전화하는 콜백 관리
     */
    CALLBACK
}
