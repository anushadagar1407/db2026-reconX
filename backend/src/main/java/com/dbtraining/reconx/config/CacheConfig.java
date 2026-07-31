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

/** TICKET-ADV081–ADV082 — enables caching and defines per-cache policies. */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache instruments = new CaffeineCache(
                "instruments",
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .recordStats()
                        .build()
        );

        CaffeineCache counterparties = new CaffeineCache(
                "counterparties",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(Duration.ofMinutes(1))
                        .recordStats()
                        .build()
        );

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(instruments, counterparties));
        return manager;
    }
}
