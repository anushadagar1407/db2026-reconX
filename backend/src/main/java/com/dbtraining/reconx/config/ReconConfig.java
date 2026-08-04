package com.dbtraining.reconx.config;


import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ManagedResource(
        objectName = "reconx:type=ReconConfig",
        description = "Runtime tuning for the reconciliation engine"
)
public class ReconConfig {

    private volatile double priceTolerance = 0.01;
    private volatile boolean cachingEnabled = true;
    private final CacheManager cacheManager;

    public ReconConfig(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @ManagedAttribute(description = "Price tolerance for break detection (0.0 - 1.0)")
    public double getPriceTolerance() {
        return priceTolerance;
    }

    @ManagedAttribute(description = "Update price tolerance (0.0 - 1.0)")
    public void setPriceTolerance(double priceTolerance) {
        if (!Double.isFinite(priceTolerance) || priceTolerance < 0 || priceTolerance > 1) {
            throw new IllegalArgumentException("price tolerance must be between 0 and 1");
        }
        this.priceTolerance = priceTolerance;
    }

    @ManagedAttribute(description = "Enabled or disabled cache interception")
    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    @ManagedAttribute(description = "Toggle caching state")
    public void setCachingEnabled(boolean cachingEnabled) {
        this.cachingEnabled = cachingEnabled;
    }

    @ManagedOperation(description = "Evict all application caches")
    public void clearCache() {
        cacheManager.getCacheNames().stream()
                .map(cacheManager::getCache)
                .filter(Objects::nonNull)
                .forEach(Cache::clear);
    }
}
