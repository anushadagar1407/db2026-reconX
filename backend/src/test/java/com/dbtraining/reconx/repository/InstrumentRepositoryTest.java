package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.AssetClass;
import com.dbtraining.reconx.repository.entity.Instrument;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties =
        "spring.jpa.properties.hypersistence.utils.jackson.object.mapper=com.fasterxml.jackson.databind.ObjectMapper")
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
}
