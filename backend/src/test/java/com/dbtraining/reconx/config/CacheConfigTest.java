package com.dbtraining.reconx.config;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CacheConfig.class)
class CacheConfigTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void registersBothNamedCaches() {
        assertThat(cacheManager.getCacheNames())
                .containsExactlyInAnyOrder("instruments", "counterparties");
        assertThat(cacheManager.getCache("instruments")).isInstanceOf(CaffeineCache.class);
        assertThat(cacheManager.getCache("counterparties")).isInstanceOf(CaffeineCache.class);
    }

    @Test
    void instrumentsExpireAfterFiveMinutesAndHoldAtMostFiveHundredEntries() {
        Cache<Object, Object> cache = nativeCache("instruments");

        assertThat(cache.policy().expireAfterWrite()).isPresent();
        assertThat(cache.policy().expireAfterWrite().orElseThrow()
                .getExpiresAfter(TimeUnit.MINUTES)).isEqualTo(5);
        assertThat(cache.policy().eviction()).isPresent();
        assertThat(cache.policy().eviction().orElseThrow().getMaximum()).isEqualTo(500);
    }

    @Test
    void counterpartiesExpireAfterOneMinuteAndHoldAtMostTwoHundredEntries() {
        Cache<Object, Object> cache = nativeCache("counterparties");

        assertThat(cache.policy().expireAfterWrite()).isPresent();
        assertThat(cache.policy().expireAfterWrite().orElseThrow()
                .getExpiresAfter(TimeUnit.MINUTES)).isEqualTo(1);
        assertThat(cache.policy().eviction()).isPresent();
        assertThat(cache.policy().eviction().orElseThrow().getMaximum()).isEqualTo(200);
    }

    @Test
    void recordsHitAndMissStatisticsForMetrics() {
        Cache<Object, Object> cache = nativeCache("instruments");
        cache.put("SAP.DE", "instrument");

        assertThat(cache.getIfPresent("SAP.DE")).isEqualTo("instrument");
        assertThat(cache.getIfPresent("UNKNOWN")).isNull();
        assertThat(cache.stats().hitCount()).isPositive();
        assertThat(cache.stats().missCount()).isPositive();
    }

    @SuppressWarnings("unchecked")
    private Cache<Object, Object> nativeCache(String name) {
        CaffeineCache springCache = (CaffeineCache) cacheManager.getCache(name);
        assertThat(springCache).isNotNull();
        return (Cache<Object, Object>) springCache.getNativeCache();
    }
}
