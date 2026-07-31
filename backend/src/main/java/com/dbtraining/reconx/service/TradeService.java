package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

import static com.dbtraining.reconx.repository.TradeSpecifications.hasCounterparty;
import static com.dbtraining.reconx.repository.TradeSpecifications.hasStatus;
import static com.dbtraining.reconx.repository.TradeSpecifications.tradeDateBetween;

/**
 * ADV063–ADV067 trade CRUD plus ADV083/ADV086 business metrics.
 */
@Service
@Transactional
public class TradeService {

    private final TradeRepository tradeRepo;
    private final CounterpartyRepository cpRepo;
    private final InstrumentRepository instRepo;

    @SuppressWarnings("unused")
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

    public Trade create(TradeRequest request, String actor) {
        Objects.requireNonNull(request, "request");

        tradeRepo.findByTradeRef(request.tradeRef())
                .ifPresent(existing -> {
                    throw new DuplicateTradeRefException(request.tradeRef());
                });

        Instrument instrument = instrument(request.instrumentId());
        Counterparty counterparty =
                counterparty(request.counterpartyId());

        Trade trade = new Trade();
        applyRequest(trade, request, instrument, counterparty);
        trade.setStatus(TradeStatus.PENDING);

        Trade saved = tradeRepo.save(trade);

        metrics.incrementTradeCreated();
        metrics.recordTradeValue(
                saved.getQuantity()
                        .multiply(saved.getPrice())
                        .doubleValue()
        );

        return saved;
    }

    public Trade update(Long id, TradeRequest request, String actor) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(request, "request");

        Trade trade = tradeRepo.findById(id)
                .orElseThrow(() ->
                        new TradeNotFoundException("id=" + id)
                );

        tradeRepo.findByTradeRef(request.tradeRef())
                .filter(existing ->
                        !Objects.equals(existing.getId(), id)
                )
                .ifPresent(existing -> {
                    throw new DuplicateTradeRefException(
                            request.tradeRef()
                    );
                });

        Instrument instrument = instrument(request.instrumentId());
        Counterparty counterparty =
                counterparty(request.counterpartyId());

        applyRequest(trade, request, instrument, counterparty);
        return tradeRepo.save(trade);
    }

    public Trade updateStatus(
            Long id,
            String status,
            String actor
    ) {
        Objects.requireNonNull(id, "id");

        Trade trade = tradeRepo.findById(id)
                .orElseThrow(() ->
                        new TradeNotFoundException("id=" + id)
                );

        trade.setStatus(status);
        return tradeRepo.save(trade);
    }

    public void softDelete(Long id, String actor) {
        Objects.requireNonNull(id, "id");

        Trade trade = tradeRepo.findById(id)
                .orElseThrow(() ->
                        new TradeNotFoundException("id=" + id)
                );

        trade.softDelete();
        tradeRepo.save(trade);
    }

    @Transactional(readOnly = true)
    public Page<Trade> list(
            LocalDate from,
            LocalDate to,
            String status,
            Long counterpartyId,
            Pageable pageable
    ) {
        Specification<Trade> specification = Specification.allOf(
                hasStatus(status),
                tradeDateBetween(from, to),
                hasCounterparty(counterpartyId)
        );

        return tradeRepo.findAll(specification, pageable);
    }

    private Instrument instrument(Long id) {
        return instRepo.findById(id)
                .orElseThrow(() ->
                        new TradeNotFoundException(
                                "instrument id=" + id
                        )
                );
    }

    private Counterparty counterparty(Long id) {
        return cpRepo.findById(id)
                .orElseThrow(() ->
                        new TradeNotFoundException(
                                "counterparty id=" + id
                        )
                );
    }

    private static void applyRequest(
            Trade trade,
            TradeRequest request,
            Instrument instrument,
            Counterparty counterparty
    ) {
        trade.setTradeRef(request.tradeRef());
        trade.setInstrument(instrument);
        trade.setCounterparty(counterparty);
        trade.setAssetClass(request.assetClass());
        trade.setSide(request.side());
        trade.setQuantity(request.quantity());
        trade.setPrice(request.price());
        trade.setTradeDate(request.tradeDate());
    }
}
