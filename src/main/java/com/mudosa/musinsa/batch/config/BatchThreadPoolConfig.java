package com.mudosa.musinsa.batch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
public class BatchThreadPoolConfig {

    @Bean("batchTaskExecutor")
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);

        executor.setQueueCapacity(0);

        executor.setThreadNamePrefix("batch-partition-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("[BatchThreadPool] 초기화 완료 - coreSize: {}, maxSize: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }
}
