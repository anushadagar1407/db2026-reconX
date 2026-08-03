package com.dbtraining.reconx.config;


import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@Component
@ManagedResource(objectName = "reconx:type=ReconConfig", description = "Runtime recon configuration")
public class ReconConfig {

    // Volatile ensures visibility across threads
    private volatile double priceTolerance = 0.5;
    private volatile boolean cachingEnabled = true;
    private final CacheManager cacheManager;

    public ReconConfig(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @ManagedAttribute(description = "Price tolerance for recon matching")
    public double getPriceTolerance() {
        return priceTolerance;
    }

    @ManagedAttribute(description = "Update price tolerance")
    public void setPriceTolerance(double priceTolerance) {
        this.priceTolerance = priceTolerance;
    }

    @ManagedAttribute(description = "Enable or disable caching")
    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    @ManagedAttribute(description = "Toggle caching")
    public void setCachingEnabled(boolean cachingEnabled) {
        this.cachingEnabled = cachingEnabled;
    }

    @ManagedOperation(description = "Clear recon cache")
    public void clearCache() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            cache.clear();
        });
    }
}
