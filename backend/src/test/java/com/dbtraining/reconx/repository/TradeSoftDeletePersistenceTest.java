package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.service.TradeService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DataJpaTest
@AutoConfigureTestDatabase
class TradeSoftDeletePersistenceTest {

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private TradeService tradeService;

    @BeforeEach
    void setUpService() {
        tradeService = new TradeService(
                tradeRepository,
                mock(CounterpartyRepository.class),
                mock(InstrumentRepository.class),
                mock(TradeEventProducer.class),
                mock(TradeMetrics.class));
    }

    @Test
    void hibernateReadsHideDeletedRowsButJdbcStillSeesThePhysicalRow() {
        jdbcTemplate.update("""
                INSERT INTO counterparties (name, lei_code, region)
                VALUES ('Soft Delete Counterparty', '5493001SOFTDELETE01', 'NAMR')
                """);
        Long counterpartyId = jdbcTemplate.queryForObject(
                "SELECT id FROM counterparties WHERE lei_code = '5493001SOFTDELETE01'", Long.class);

        jdbcTemplate.update("""
                INSERT INTO instruments (symbol, name, asset_class, currency, isin)
                VALUES ('SOFTDEL', 'Soft Delete Instrument', 'EQUITY', 'USD', 'US0000000001')
                """);
        Long instrumentId = jdbcTemplate.queryForObject(
                "SELECT id FROM instruments WHERE symbol = 'SOFTDEL'", Long.class);

        jdbcTemplate.update("""
                INSERT INTO trades (
                    trade_ref, instrument_id, counterparty_id, asset_class, side,
                    quantity, price, trade_date, status, created_at
                ) VALUES (?, ?, ?, 'EQUITY', 'BUY', 10.0000, 125.5000, DATE '2026-07-29', 'PENDING', ?)
                """, "TRD-20260729-9999", instrumentId, counterpartyId,
                Timestamp.from(java.time.Instant.now()));
        Long tradeId = jdbcTemplate.queryForObject(
                "SELECT id FROM trades WHERE trade_ref = 'TRD-20260729-9999'", Long.class);

        assertThat(tradeRepository.findById(tradeId)).isPresent();
        tradeService.softDelete(tradeId, "delete-actor");
        entityManager.flush();
        entityManager.clear();

        assertThat(tradeRepository.findById(tradeId)).isEmpty();
        assertThat(tradeRepository.findAll()).noneMatch(trade -> tradeId.equals(trade.getId()));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trades WHERE id = ?", Integer.class, tradeId)).isEqualTo(1);
        Timestamp deletedAt = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM trades WHERE id = ?", Timestamp.class, tradeId);
        assertThat(deletedAt).isNotNull();

        assertThatThrownBy(() -> tradeService.softDelete(tradeId, "delete-actor"))
                .isInstanceOf(TradeNotFoundException.class)
                .hasMessage("id=" + tradeId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trades WHERE id = ?", Integer.class, tradeId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM trades WHERE id = ?", Timestamp.class, tradeId))
                .isEqualTo(deletedAt);
    }
}
