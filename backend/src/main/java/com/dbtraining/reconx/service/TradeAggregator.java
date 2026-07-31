package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * TICKET-ADV137 — Rebuild the current trade state from its audit events.
 */
@Service
public class TradeAggregator {

    private final AuditLogRepository auditRepo;
    private final ObjectMapper objectMapper;

    public TradeAggregator(
            AuditLogRepository auditRepo,
            ObjectMapper objectMapper
    ) {
        this.auditRepo = auditRepo;
        this.objectMapper = objectMapper;
    }

    public Optional<JsonNode> rebuild(String tradeRef) {
        List<AuditLogEntry> events =
                auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef);

        if (events.isEmpty()) {
            return Optional.empty();
        }

        JsonNode state = null;

        for (AuditLogEntry entry : events) {
            TradeEvent.EventType eventType =
                    TradeEvent.EventType.valueOf(entry.getEventType());

            switch (eventType) {
                case TRADE_CREATED, TRADE_UPDATED ->
                        state = parseJson(entry.getAfterState());

                case TRADE_CANCELLED ->
                        state = null;
            }
        }

        return Optional.ofNullable(state);
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to parse audit state JSON",
                    exception
            );
        }
    }
}