package com.dbtraining.reconx.observability;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.jmx.export.annotation.ManagedResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReconConfigMBeanTest {

    private final CacheManager cacheManager = new ConcurrentMapCacheManager(
            "instruments", "counterparties");
    private final ReconConfigMBean config = new ReconConfigMBean(cacheManager);

    @Test
    void exposesExpectedObjectNameAndMutableAttributes() {
        ManagedResource managedResource = ReconConfigMBean.class.getAnnotation(ManagedResource.class);

        assertThat(managedResource.objectName()).isEqualTo("reconx:type=ReconConfig");
        assertThat(config.getPriceTolerance()).isEqualTo(0.01);
        assertThat(config.isCachingEnabled()).isTrue();

        config.setPriceTolerance(0.025);
        config.setCachingEnabled(false);

        assertThat(config.getPriceTolerance()).isEqualTo(0.025);
        assertThat(config.isCachingEnabled()).isFalse();
    }

    @Test
    void rejectsInvalidPriceTolerance() {
        assertThatThrownBy(() -> config.setPriceTolerance(-0.01))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.setPriceTolerance(1.01))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.setPriceTolerance(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.setPriceTolerance(Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clearCacheEvictsEveryConfiguredCache() {
        cacheManager.getCache("instruments").put("SAP.DE", "instrument");
        cacheManager.getCache("counterparties").put(42L, "counterparty");

        config.clearCache();

        assertThat(cacheManager.getCache("instruments").get("SAP.DE")).isNull();
        assertThat(cacheManager.getCache("counterparties").get(42L)).isNull();
    }
}
