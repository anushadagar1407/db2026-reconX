package com.dbtraining.reconx.repository;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.AssetClass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
@DataJpaTest
public class InstrumentRepositoryTest {

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Test
    void testMetadataRoundTrip() {
        Instrument instrument = new Instrument();
        instrument.setSymbol("AAPL");
        instrument.setName("Apple Inc.");
        instrument.setAssetClass(AssetClass.EQUITY);
        instrument.setCurrency("USD");
        instrument.setMetadata(Map.of("isin", "GB00B16GWD56", "cusip", "037833100"));

        instrumentRepository.save(instrument);

        Instrument reloaded = instrumentRepository.findById(instrument.getId()).orElseThrow();
        assertThat(reloaded.getMetadata()).isEqualTo(instrument.getMetadata());
    }
}
