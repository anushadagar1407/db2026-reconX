package com.dbtraining.reconx.service;

import com.dbtraining.reconx.config.CacheConfig;
import com.dbtraining.reconx.exception.InvalidTradeException;
import com.dbtraining.reconx.observability.ReconConfigMBean;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.entity.Instrument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        CacheConfig.class,
        ReconConfigMBean.class,
        InstrumentServiceCacheTest.TestConfig.class
})
class InstrumentServiceCacheTest {

    @Autowired
    private InstrumentService service;

    @Autowired
    private InstrumentRepository repository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ReconConfigMBean reconConfig;

    @BeforeEach
    void clearCacheAndMock() {
        cacheManager.getCache("instruments").clear();
        reconConfig.setCachingEnabled(true);
        clearInvocations(repository);
    }

    @Test
    void repeatedSymbolLookupHitsRepositoryOnlyOnce() {
        Instrument sap = new Instrument();
        sap.setSymbol("SAP.DE");
        when(repository.findBySymbol("SAP.DE")).thenReturn(Optional.of(sap));

        Instrument first = service.findBySymbol("SAP.DE");
        Instrument second = service.findBySymbol("SAP.DE");

        assertThat(first).isSameAs(sap);
        assertThat(second).isSameAs(sap);
        verify(repository).findBySymbol("SAP.DE");
    }

    @Test
    void unknownSymbolIsNotCached() {
        when(repository.findBySymbol("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findBySymbol("UNKNOWN"))
                .isInstanceOf(InvalidTradeException.class)
                .hasMessage("Unknown instrument symbol: UNKNOWN");
        assertThatThrownBy(() -> service.findBySymbol("UNKNOWN"))
                .isInstanceOf(InvalidTradeException.class);

        verify(repository, org.mockito.Mockito.times(2)).findBySymbol("UNKNOWN");
    }

    @Test
    void disabledCachingBypassesReadsAndWrites() {
        Instrument sap = new Instrument();
        sap.setSymbol("SAP.DE");
        when(repository.findBySymbol("SAP.DE")).thenReturn(Optional.of(sap));
        reconConfig.setCachingEnabled(false);

        service.findBySymbol("SAP.DE");
        service.findBySymbol("SAP.DE");

        verify(repository, org.mockito.Mockito.times(2)).findBySymbol("SAP.DE");
        assertThat(cacheManager.getCache("instruments").get("SAP.DE")).isNull();
    }

    @Configuration
    static class TestConfig {

        @Bean
        InstrumentRepository instrumentRepository() {
            return mock(InstrumentRepository.class);
        }

        @Bean
        InstrumentService instrumentService(InstrumentRepository repository) {
            return new InstrumentService(repository);
        }

    }
}
