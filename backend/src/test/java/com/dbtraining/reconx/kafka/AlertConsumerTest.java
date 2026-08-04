package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.SystemAlert;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AlertConsumerTest {

    @Test
    void forwardsReceivedAlertToSink() {
        AlertSink sink = mock(AlertSink.class);
        AlertConsumer consumer = new AlertConsumer(sink);
        SystemAlert alert = new SystemAlert("CRITICAL", "OPS-001", "Trade processing failed");

        consumer.onAlert(alert);

        verify(sink).notify(alert);
    }
}
