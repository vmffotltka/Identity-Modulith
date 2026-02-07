package com.nexfron.identitymodulith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Identity Modulith Application
 *
 * 활성화된 기능:
 * - @EnableScheduling: 감사 로그 아카이빙 배치 작업
 */
@SpringBootApplication
@EnableScheduling
public class IdentityModulithApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityModulithApplication.class, args);
    }

}
