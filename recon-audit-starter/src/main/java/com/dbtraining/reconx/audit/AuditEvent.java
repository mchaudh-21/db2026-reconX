package com.dbtraining.reconx.audit;

import java.time.Instant;
import java.util.Map;

public record AuditEvent(
        String eventType,
        String aggregateId,
        Instant occurredAt,
        Map<String, Object> attributes
) {
    public AuditEvent {
        attributes = attributes == null
                ? Map.of()
                : Map.copyOf(attributes);
    }
}
