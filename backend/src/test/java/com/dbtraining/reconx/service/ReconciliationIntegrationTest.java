package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.repository.ExternalTradeRepository;
import com.dbtraining.reconx.repository.InternalTradeRepository;
import com.dbtraining.reconx.repository.ReconResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ReconciliationIntegrationTest {

    @Autowired
    private InternalTradeRepository internalTradeRepository;

    @Autowired
    private ExternalTradeRepository externalTradeRepository;

    @Autowired
    private ReconResultRepository reconResultRepository;

    @Autowired
    private ReconciliationService reconciliationService;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reconx")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Test
    void containerIsRunning() {
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    @Transactional
    void insertedTradesAreReconciledAndPersisted() {
        EquityTrade internal = equityTrade();
        EquityTrade external = equityTrade();

        internalTradeRepository.save(internal);
        externalTradeRepository.save(external);

        reconciliationService.runRecon(
                internalTradeRepository.findAll(),
                externalTradeRepository.findAll(),
                ReconciliationRule.EXACT);

        List<ReconResult> persisted = reconResultRepository.findAll();

        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
        assertThat(persisted.get(0).tradeRef()).isEqualTo("EQU-20260730-0001");
    }

    private EquityTrade equityTrade() {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of("EQU-20260730-0001"))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("245.50"))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 7, 30))
                .counterpartyId(1L)
                .build();
    }
}
