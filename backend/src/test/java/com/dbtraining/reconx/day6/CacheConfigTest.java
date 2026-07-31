package com.dbtraining.reconx.day6;

import com.dbtraining.reconx.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    @Test
    void cachesHaveIndependentTtlsAndStatistics() {
        SimpleCacheManager manager =
                (SimpleCacheManager)
                        new CacheConfig().cacheManager();

        manager.afterPropertiesSet();

        CaffeineCache instruments =
                (CaffeineCache)
                        manager.getCache("instruments");

        CaffeineCache counterparties =
                (CaffeineCache)
                        manager.getCache("counterparties");

        assertThat(instruments).isNotNull();
        assertThat(counterparties).isNotNull();

        assertThat(instruments.getNativeCache()
                .policy()
                .expireAfterWrite()
                .orElseThrow()
                .getExpiresAfter())
                .isEqualTo(Duration.ofMinutes(5));

        assertThat(counterparties.getNativeCache()
                .policy()
                .expireAfterWrite()
                .orElseThrow()
                .getExpiresAfter())
                .isEqualTo(Duration.ofMinutes(1));

        instruments.put("SAP.DE", "cached");
        assertThat(instruments.get("SAP.DE")).isNotNull();

        assertThat(instruments.getNativeCache()
                .stats()
                .hitCount())
                .isEqualTo(1L);
    }
}
