package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TradeAnalyticsServiceTest {

    private TradeAnalyticsService service;
    private AtomicInteger counter;

    @BeforeEach
    void setUp() {
        service = new TradeAnalyticsService();
        counter = new AtomicInteger(1000);
    }

    private EquityTrade createTrade(String symbol, String price, String quantity) {
        String refStr = String.format("TRD-20260729-%04d", counter.getAndIncrement());
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(refStr))
                .instrumentSymbol(symbol)
                .quantity(new BigDecimal(quantity))
                .price(new BigDecimal(price))
                .currency("USD")
                .side(Side.BUY)
                .tradeDate(LocalDate.now())
                .counterpartyId(101L)
                .build();
    }

    @Test
    @DisplayName("VWAP should accurately calculate weight-averaged prices per instrument")
    void testVwapByInstrument() {
        // Arrange
        List<EquityTrade> trades = List.of(
            createTrade("AAPL", "150.25", "100"),
            createTrade("AAPL", "152.50", "200"),
            createTrade("MSFT", "310.10", "50"),
            createTrade("MSFT", "315.40", "150"),
            createTrade("MSFT", "312.25", "75")
        );

        // Act
        Map<String, BigDecimal> result = service.vwapByInstrument(trades);

        // --- PRINT RESULTS TO CONSOLE ---
        System.out.println("\n================ TEST OUTPUT ================");
        System.out.println("Calculated VWAP Map: " + result);
        System.out.println("AAPL Expected: 151.7500 | Actual: " + result.get("AAPL"));
        System.out.println("MSFT Expected: 313.5773 | Actual: " + result.get("MSFT"));
        System.out.println("=============================================\n");

        // Assert
        assertNotNull(result, "Result map should not be null");
        assertEquals(new BigDecimal("151.7500"), result.get("AAPL"), "AAPL VWAP mismatch");
        assertEquals(new BigDecimal("313.5773"), result.get("MSFT"), "MSFT VWAP mismatch");
    }
}