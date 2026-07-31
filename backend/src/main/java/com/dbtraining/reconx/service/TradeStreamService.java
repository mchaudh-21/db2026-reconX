package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Browser-facing Server-Sent Events broadcaster for newly created trades.
 */
@Service
public class TradeStreamService {

    private static final Logger log =
            LoggerFactory.getLogger(TradeStreamService.class);

    private final List<SseEmitter> emitters =
            new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .comment("ReconX trade stream connected")
            );
        } catch (IOException exception) {
            emitters.remove(emitter);
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    public void publish(TradeResponse trade) {
        emitters.forEach(emitter -> send(emitter, trade));
    }

    private void send(SseEmitter emitter, TradeResponse trade) {
        try {
            emitter.send(
                    SseEmitter.event()
                            .name("trade")
                            .data(trade)
            );
        } catch (IOException | IllegalStateException exception) {
            emitters.remove(emitter);

            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
                log.debug("SSE emitter was already completed");
            }
        }
    }
}
