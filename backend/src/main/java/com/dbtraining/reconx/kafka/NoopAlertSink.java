package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.SystemAlert;

/** Default alert sink used until an external notifier is configured. */
public class NoopAlertSink implements AlertSink {

    @Override
    public void notify(SystemAlert alert) {
        // Logging is handled by AlertConsumer; external sinks can replace this bean later.
    }
}
