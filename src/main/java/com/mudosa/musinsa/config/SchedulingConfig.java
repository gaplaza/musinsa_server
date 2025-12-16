package com.mudosa.musinsa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduling 설정
 * - OutboxRelay의 @Scheduled 메서드 활성화
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}