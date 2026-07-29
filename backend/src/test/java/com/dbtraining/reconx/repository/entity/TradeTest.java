package com.dbtraining.reconx.repository.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeTest {

    @Test
    void newTradeDefaultsToPendingStatus() {
        Trade trade = new Trade();

        assertEquals(TradeStatus.PENDING, trade.getStatus());
    }

    @Test
    void tradeEqualsItself() {
        Trade trade = new Trade();

        assertEquals(trade, trade);
    }

    @Test
    void transientTradesAreNotEqual() {
        Trade first = new Trade();
        Trade second = new Trade();

        assertNotEquals(first, second);
    }

    @Test
    void tradeIsNotEqualToNullOrAnotherType() {
        Trade trade = new Trade();

        assertFalse(trade.equals(null));
        assertFalse(trade.equals("trade"));
    }

    @Test
    void persistedTradesWithSameIdAreEqualAndHaveSameHashCode() {
        Trade first = tradeWithId(42L);
        Trade second = tradeWithId(42L);

        assertTrue(first.equals(second));
        assertTrue(second.equals(first));
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void persistedTradesWithDifferentIdsAreNotEqual() {
        Trade first = tradeWithId(42L);
        Trade second = tradeWithId(43L);

        assertNotEquals(first, second);
    }

    private Trade tradeWithId(Long id) {
        Trade trade = new Trade();
        ReflectionTestUtils.setField(trade, "id", id);
        return trade;
    }
}
