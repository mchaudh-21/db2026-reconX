package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.dbtraining.reconx.repository.TradeSpecifications.hasCounterparty;
import static com.dbtraining.reconx.repository.TradeSpecifications.hasStatus;
import static com.dbtraining.reconx.repository.TradeSpecifications.tradeDateBetween;

/**
 * ============================================================================
 * TICKET-ADV064 — TradeService.create (POST endpoint backing)
 * TICKET-ADV065 — update
 * TICKET-ADV066 — updateStatus (PATCH)
 * TICKET-ADV067 — softDelete
 * TICKET-ADV083 — increments trade_created_total Counter on create
 * TICKET-ADV129 — publishes TradeEvent on every state change
 * TICKET-ADV055/ADV056/ADV057 — paginated and filterable trade list
 * ============================================================================
 */
@Service
@Transactional
public class TradeService {

    private final TradeRepository tradeRepo;
    private final CounterpartyRepository cpRepo;
    private final InstrumentRepository instRepo;
    private final TradeEventProducer events;
    private final TradeMetrics metrics;

    public TradeService(
            TradeRepository tradeRepo,
            CounterpartyRepository cpRepo,
            InstrumentRepository instRepo,
            TradeEventProducer events,
            TradeMetrics metrics
    ) {
        this.tradeRepo = tradeRepo;
        this.cpRepo = cpRepo;
        this.instRepo = instRepo;
        this.events = events;
        this.metrics = metrics;
    }

    public Trade create(TradeRequest req, String actor) {
        // TODO(TICKET-ADV064): reject duplicate tradeRef via
        // DuplicateTradeRefException, build a new Trade with instrument and
        // counterparty looked up from their repositories, set status to PENDING,
        // save it, record metrics, and publish a TRADE_CREATED event.
        throw new UnsupportedOperationException("TICKET-ADV064");
    }

    public Trade update(Long id, TradeRequest req, String actor) {
        // TODO(TICKET-ADV065): load by id, throw TradeNotFoundException if
        // missing, copy mutable fields from req, save, and publish a
        // TRADE_UPDATED event.
        throw new UnsupportedOperationException("TICKET-ADV065");
    }

    public Trade updateStatus(Long id, String status, String actor) {
        // TODO(TICKET-ADV066): load the trade, set its status, save it, and
        // publish a TRADE_UPDATED event with the new status.
        throw new UnsupportedOperationException("TICKET-ADV066");
    }

    public void softDelete(Long id, String actor) {
        // TODO(TICKET-ADV067): load the trade, call softDelete(), save it, and
        // publish a TRADE_CANCELLED event.
        throw new UnsupportedOperationException("TICKET-ADV067");
    }

    @Transactional(readOnly = true)
    public Page<Trade> list(
            LocalDate from,
            LocalDate to,
            String status,
            Long counterpartyId,
            Pageable pageable
    ) {
        Specification<Trade> specification = Specification
                .where(hasStatus(status))
                .and(tradeDateBetween(from, to))
                .and(hasCounterparty(counterpartyId));

        return tradeRepo.findAll(specification, pageable);
    }
}
