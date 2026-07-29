package com.dbtraining.reconx.audit;

import org.springframework.context.ApplicationEventPublisher;

public class AuditEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final AuditProperties properties;

    public AuditEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            AuditProperties properties) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.properties = properties;
    }

    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }

    public AuditProperties getProperties() {
        return properties;
    }
}
