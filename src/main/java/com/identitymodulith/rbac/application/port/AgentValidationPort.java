package com.identitymodulith.rbac.application.port;

/**
 * Agent 유효성 검증을 위한 Port 인터페이스
 *
 * <p>RBAC 모듈이 User 모듈을 직접 의존하지 않도록 Port/Adapter 패턴을 사용합니다.
 * 실제 구현체는 User 모듈의 infrastructure layer에 위치합니다.</p>
 */
public interface AgentValidationPort {

    /**
     * 상담사가 활성 상태인지 확인합니다.
     * RETIRED 또는 비활성 상담사에게는 역할을 할당할 수 없습니다.
     *
     * @param agentId 상담사 ID (loginId 또는 UUID)
     * @return 활성 상태면 true, 비활성/퇴사/존재하지 않으면 false
     */
    boolean isActiveAgent(String agentId);
}

