package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquityTradeTest {

    @Test
    void builder_buildsWhenAllRequiredPresent() {
        EquityTrade trade = sampleEquity("ABC-20260306-0001");
        assertThat(trade.tradeRef()).isEqualTo(TradeRef.of("ABC-20260306-0001"));
        assertThat(trade.notional().amount()).isEqualByComparingTo("10000");
        assertThat(trade.assetClass()).isEqualTo(TradeType.AssetClass.EQUITY);
        // TICKET-ADV019: build an EquityTrade via the Builder with all required fields,
        //                     then assert tradeRef, notional (price*qty) and assetClass = EQUITY.    }
    }

    @Test
    void builder_missingPrice_throws() {

        assertThatThrownBy(() -> EquityTrade.builder()
                .tradeRef(TradeRef.of("ABC-20260306-0001"))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build()
    )
                .isInstanceOf(NullPointerException.class);
    }
        // TICKET-ADV019: omit .price(...) on the Builder and assert build() throws
        //                     NullPointerException whose message mentions "price".

    @Test
    void equality_byTradeRef() {
        // TICKET-ADV028: two EquityTrades with the same tradeRef are equal and share hashCode;
        //                     a third with a different tradeRef is not equal.
        org.junit.jupiter.api.Assertions.fail("TICKET-ADV028 not implemented yet");
    }

    private EquityTrade sampleEquity(String ref) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("100"))
                .currency("EUR").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L).build();
    }
}
