package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.service.ReconciliationEngine;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ReconciliationConsumerTest {

    private final ReconciliationEngine reconEngine = mock(ReconciliationEngine.class);
    private final ReconciliationConsumer consumer = new ReconciliationConsumer(reconEngine);

    @Test
    void schedulesReconciliationForCreatedTrade() {
        consumer.onTradeEvent(TradeEvent.created("TRD-NEW"));

        verify(reconEngine).scheduleRecon("TRD-NEW");
        verifyNoMoreInteractions(reconEngine);
    }

    @Test
    void schedulesReconciliationForUpdatedTrade() {
        consumer.onTradeEvent(TradeEvent.updated("TRD-UPDATED"));

        verify(reconEngine).scheduleRecon("TRD-UPDATED");
        verifyNoMoreInteractions(reconEngine);
    }

    @Test
    void cancelsPendingReconciliationForCancelledTrade() {
        consumer.onTradeEvent(TradeEvent.cancelled("TRD-CANCELLED"));

        verify(reconEngine).cancelPendingRecon("TRD-CANCELLED");
        verifyNoMoreInteractions(reconEngine);
    }
}
