package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.SystemAlert;

/** Pluggable destination for operational alerts. */
public interface AlertSink {

    void notify(SystemAlert alert);
}
