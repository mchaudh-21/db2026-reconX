package com.dbtraining.reconx.audit;

import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Map;

public class AuditEventPublisher {

    private final ApplicationEventPublisher publisher;

    public AuditEventPublisher(
            ApplicationEventPublisher publisher
    ) {
        this.publisher = publisher;
    }

    public void publish(
            String eventType,
            String aggregateId,
            Map<String, Object> attributes
    ) {
        publisher.publishEvent(
                new AuditEvent(
                        eventType,
                        aggregateId,
                        Instant.now(),
                        attributes
                )
        );
    }
}
