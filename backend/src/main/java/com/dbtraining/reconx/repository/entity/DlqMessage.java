package com.dbtraining.reconx.repository.entity;

import com.dbtraining.reconx.dto.TradeEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "dlq_message",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dlq_message_event_id",
                        columnNames = "event_id"
                )
        }
)
public class DlqMessage {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().findAndRegisterModules();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "trade_ref", nullable = false, length = 30)
    private String tradeRef;

    @Column(name = "original_topic", nullable = false, length = 100)
    private String originalTopic;

    @Column(name = "source_partition", nullable = false)
    private Integer partition;

    @Column(name = "source_offset", nullable = false)
    private Long offset;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    protected DlqMessage() {
    }

    public DlqMessage(
            UUID eventId,
            String tradeRef,
            String originalTopic,
            Integer partition,
            Long offset,
            TradeEvent payload,
            String reason,
            Instant firstSeen
    ) {
        this.eventId = eventId;
        this.tradeRef = tradeRef;
        this.originalTopic = originalTopic;
        this.partition = partition;
        this.offset = offset;
        this.payloadJson = serializePayload(payload);
        this.reason = reason;
        this.firstSeen = firstSeen;
    }

    private static String serializePayload(TradeEvent payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(
                    "Unable to serialize DLQ payload",
                    ex
            );
        }
    }

    public TradeEvent getPayload() {
        try {
            return OBJECT_MAPPER.readValue(payloadJson, TradeEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Unable to deserialize DLQ payload for event " + eventId,
                    ex
            );
        }
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getTradeRef() {
        return tradeRef;
    }

    public String getOriginalTopic() {
        return originalTopic;
    }

    public Integer getPartition() {
        return partition;
    }

    public Long getOffset() {
        return offset;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getReason() {
        return reason;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }
}