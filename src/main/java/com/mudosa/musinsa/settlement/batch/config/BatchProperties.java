package com.mudosa.musinsa.settlement.batch.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/* 배치 처리 관련 설정값 */
@Getter
@ConfigurationProperties(prefix = "batch.settlement")
public class BatchProperties {

    private final int chunkSize;
    private final int maxSkipCount;

    @ConstructorBinding
    public BatchProperties(Integer chunkSize, Integer maxSkipCount) {
        this.chunkSize = chunkSize != null ? chunkSize : 10;
        this.maxSkipCount = maxSkipCount != null ? maxSkipCount : 100;
    }
}