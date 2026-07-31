package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/dlq")
@PreAuthorize("hasRole('ADMIN')")
public class DlqAdminController {

    private final DlqMessageRepository repo;
    private final TradeEventProducer producer;

    public DlqAdminController(
            DlqMessageRepository repo,
            TradeEventProducer producer
    ) {
        this.repo = repo;
        this.producer = producer;
    }

    @PostMapping("/replay")
    public ResponseEntity<Map<String, Object>> replay(
            @RequestParam UUID eventId,
            @RequestParam(defaultValue = "false") boolean dryRun
    ) {
        DlqMessage msg = repo.findByEventId(eventId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "No DLQ message: " + eventId
                        )
                );

        if (dryRun) {
            return ResponseEntity.ok(
                    Map.of(
                            "dryRun", true,
                            "wouldReplayTo", msg.getOriginalTopic(),
                            "tradeRef", msg.getTradeRef()
                    )
            );
        }

        producer.publish(msg.getPayload());
        repo.delete(msg);

        return ResponseEntity.ok(
                Map.of(
                        "replayed", true,
                        "eventId", eventId,
                        "topic", msg.getOriginalTopic()
                )
        );
    }
}