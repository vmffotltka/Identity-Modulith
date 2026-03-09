package com.identitymodulith;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Context 로드 통합 테스트
 *
 * <p>실제 데이터베이스 연결이 필요하므로 로컬 테스트 시 비활성화됩니다.
 * CI/CD 환경에서는 테스트 DB 설정 후 활성화하세요.
 */
@SpringBootTest
@Disabled("실제 DB 연결 필요 - CI/CD 환경에서만 실행")
class IdentityModulithApplicationTests {

    @Test
    void contextLoads() {
        // Spring Context가 정상적으로 로드되는지 확인
    }

}
