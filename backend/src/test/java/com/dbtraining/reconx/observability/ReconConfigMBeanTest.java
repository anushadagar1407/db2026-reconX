package com.dbtraining.reconx.observability;

import com.dbtraining.reconx.config.CacheConfig;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.jmx.export.annotation.ManagedResource;

import javax.management.MBeanServer;
import javax.management.ObjectName;

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

    @Test
    void registersExpectedAttributesAndOperationWithJmxEnabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class))
                .withPropertyValues("spring.jmx.enabled=true")
                .withUserConfiguration(CacheConfig.class, ReconConfigMBean.class)
                .run(context -> {
                    ObjectName objectName = new ObjectName("reconx:type=ReconConfig");
                    MBeanServer mBeanServer = context.getBean(MBeanServer.class);

                    assertThat(mBeanServer.isRegistered(objectName)).isTrue();
                    assertThat(mBeanServer.getMBeanInfo(objectName).getAttributes())
                            .extracting(attribute -> attribute.getName())
                            .contains("PriceTolerance", "CachingEnabled");
                    assertThat(mBeanServer.getMBeanInfo(objectName).getOperations())
                            .extracting(operation -> operation.getName())
                            .contains("clearCache");
                });
    }
}
