package com.dbtraining.reconx.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * ADV081–ADV082 — independent Caffeine policies and cache statistics.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String INSTRUMENTS = "instruments";
    public static final String COUNTERPARTIES = "counterparties";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache(INSTRUMENTS, Duration.ofMinutes(5)),
                buildCache(COUNTERPARTIES, Duration.ofMinutes(1))
        ));
        return manager;
    }

    private static CaffeineCache buildCache(
            String name,
            Duration timeToLive
    ) {
        return new CaffeineCache(
                name,
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(timeToLive)
                        .recordStats()
                        .build()
        );
    }
}
