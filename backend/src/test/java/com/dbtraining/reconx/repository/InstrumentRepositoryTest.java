package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.AssetClass;
import com.dbtraining.reconx.repository.entity.Instrument;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class InstrumentRepositoryTest {

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void metadataDefaultsToAnEmptyMap() {
        assertThat(new Instrument().getMetadata()).isEmpty();
    }

    @Test
    void metadataRoundTripsThroughTheJsonColumn() {
        Map<String, Object> metadata = Map.of(
                "isin", "GB00B16GWD56",
                "cusip", "037833100",
                "attributes", Map.of("sector", "Technology", "active", true),
                "tags", List.of("large-cap", "dividend")
        );

        Instrument instrument = new Instrument();
        instrument.setSymbol("ADV051-ROUNDTRIP");
        instrument.setName("ADV051 round-trip instrument");
        instrument.setAssetClass(AssetClass.EQUITY);
        instrument.setCurrency("USD");
        instrument.setMetadata(metadata);

        Instrument saved = instrumentRepository.saveAndFlush(instrument);
        entityManager.clear();

        Instrument reloaded = instrumentRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getAssetClass()).isEqualTo(AssetClass.EQUITY);
        assertThat(reloaded.getMetadata()).isEqualTo(metadata);
    }

    @Test
    void seededInstrumentMetadataAndClassificationsRemainReadable() {
        Instrument equity = instrumentRepository.findBySymbol("SAP.DE").orElseThrow();
        Instrument fixedIncome = instrumentRepository.findBySymbol("US10Y").orElseThrow();

        assertThat(equity.getMetadata()).containsEntry("sector", "Technology");
        assertThat(fixedIncome.getAssetClass()).isEqualTo(AssetClass.FIXED_INCOME);
    }
}
