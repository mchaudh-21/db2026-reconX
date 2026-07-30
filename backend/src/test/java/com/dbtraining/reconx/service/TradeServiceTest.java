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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeServiceTest {

    private TradeRepository tradeRepo;
    private CounterpartyRepository cpRepo;
    private InstrumentRepository instRepo;
    private TradeService service;

    @BeforeEach
    void setUp() {
        tradeRepo = mock(TradeRepository.class);
        cpRepo = mock(CounterpartyRepository.class);
        instRepo = mock(InstrumentRepository.class);

        service = new TradeService(
                tradeRepo,
                cpRepo,
                instRepo,
                mock(TradeEventProducer.class),
                mock(TradeMetrics.class)
        );
    }

    @Test
    void createBuildsPendingTradeAndResolvesRelationships() {
        TradeRequest request = request(
                "EQU-20260730-0001",
                11L,
                22L,
                "100.0000",
                "245.5000"
        );

        Instrument instrument = instrument(11L);
        Counterparty counterparty = counterparty(22L);

        when(tradeRepo.findByTradeRef(request.tradeRef()))
                .thenReturn(Optional.empty());
        when(instRepo.findById(11L)).thenReturn(Optional.of(instrument));
        when(cpRepo.findById(22L)).thenReturn(Optional.of(counterparty));
        when(tradeRepo.save(any(Trade.class))).thenAnswer(invocation -> {
            Trade saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 42L);
            return saved;
        });

        Trade saved = service.create(request, "trader@db.com");

        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(saved.getTradeRef()).isEqualTo(request.tradeRef());
        assertThat(saved.getInstrument()).isSameAs(instrument);
        assertThat(saved.getCounterparty()).isSameAs(counterparty);
        assertThat(saved.getStatus()).isEqualTo(TradeStatus.PENDING);
        assertThat(saved.getQuantity()).isEqualByComparingTo("100.0000");
        assertThat(saved.getPrice()).isEqualByComparingTo("245.5000");
    }

    @Test
    void createRejectsDuplicateTradeReference() {
        TradeRequest request = request(
                "EQU-20260730-0001",
                11L,
                22L,
                "100",
                "245.50"
        );

        when(tradeRepo.findByTradeRef(request.tradeRef()))
                .thenReturn(Optional.of(new Trade()));

        assertThatThrownBy(() ->
                service.create(request, "trader@db.com"))
                .isInstanceOf(DuplicateTradeRefException.class)
                .hasMessageContaining(request.tradeRef());
    }

    @Test
    void updateReplacesEveryRequestControlledField() {
        Trade existing = new Trade();
        ReflectionTestUtils.setField(existing, "id", 42L);
        existing.setTradeRef("EQU-20260729-0001");
        existing.setInstrument(instrument(1L));
        existing.setCounterparty(counterparty(2L));
        existing.setAssetClass("EQUITY");
        existing.setSide("SELL");
        existing.setQuantity(new BigDecimal("1"));
        existing.setPrice(new BigDecimal("1"));
        existing.setTradeDate(LocalDate.now().minusDays(1));

        TradeRequest replacement = request(
                "EQU-20260730-0002",
                11L,
                22L,
                "150.0000",
                "250.0000"
        );

        Instrument newInstrument = instrument(11L);
        Counterparty newCounterparty = counterparty(22L);

        when(tradeRepo.findById(42L)).thenReturn(Optional.of(existing));
        when(tradeRepo.findByTradeRef(replacement.tradeRef()))
                .thenReturn(Optional.empty());
        when(instRepo.findById(11L))
                .thenReturn(Optional.of(newInstrument));
        when(cpRepo.findById(22L))
                .thenReturn(Optional.of(newCounterparty));
        when(tradeRepo.save(existing)).thenReturn(existing);

        Trade updated =
                service.update(42L, replacement, "trader@db.com");

        assertThat(updated.getTradeRef())
                .isEqualTo(replacement.tradeRef());
        assertThat(updated.getInstrument()).isSameAs(newInstrument);
        assertThat(updated.getCounterparty()).isSameAs(newCounterparty);
        assertThat(updated.getAssetClass()).isEqualTo("EQUITY");
        assertThat(updated.getSide()).isEqualTo("BUY");
        assertThat(updated.getQuantity()).isEqualByComparingTo("150.0000");
        assertThat(updated.getPrice()).isEqualByComparingTo("250.0000");
        assertThat(updated.getTradeDate())
                .isEqualTo(replacement.tradeDate());
    }

    @Test
    void updateStatusAndSoftDeleteMutateOnlyLifecycleFields() {
        Trade trade = new Trade();
        ReflectionTestUtils.setField(trade, "id", 42L);
        trade.setStatus(TradeStatus.PENDING);

        when(tradeRepo.findById(42L))
                .thenReturn(Optional.of(trade));
        when(tradeRepo.save(trade)).thenReturn(trade);

        Trade matched =
                service.updateStatus(42L, "MATCHED", "trader@db.com");

        assertThat(matched.getStatus()).isEqualTo(TradeStatus.MATCHED);

        service.softDelete(42L, "admin@db.com");

        assertThat(trade.getStatus()).isEqualTo(TradeStatus.CANCELLED);
        assertThat(trade.getDeletedAt()).isNotNull();
        verify(tradeRepo, times(2)).save(trade);
    }

    @Test
    void missingTradeReturnsDomainNotFoundException() {
        when(tradeRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.softDelete(999L, "admin@db.com"))
                .isInstanceOf(TradeNotFoundException.class)
                .hasMessageContaining("id=999");
    }

    @Test
    @SuppressWarnings("unchecked")
    void listDelegatesToSpecificationExecutorWithPageable() {
        var pageable = PageRequest.of(0, 20);
        when(tradeRepo.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.list(
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                "PENDING",
                22L,
                pageable
        );

        ArgumentCaptor<Specification<Trade>> captor =
                ArgumentCaptor.forClass(Specification.class);

        verify(tradeRepo).findAll(captor.capture(), eq(pageable));
        assertThat(captor.getValue()).isNotNull();
    }

    private static TradeRequest request(
            String tradeRef,
            Long instrumentId,
            Long counterpartyId,
            String quantity,
            String price
    ) {
        return new TradeRequest(
                tradeRef,
                instrumentId,
                counterpartyId,
                "EQUITY",
                "BUY",
                new BigDecimal(quantity),
                new BigDecimal(price),
                LocalDate.now()
        );
    }

    private static Instrument instrument(Long id) {
        Instrument instrument = new Instrument();
        ReflectionTestUtils.setField(instrument, "id", id);
        instrument.setSymbol("AAPL");
        instrument.setName("Apple");
        instrument.setAssetClass("EQUITY");
        instrument.setCurrency("USD");
        return instrument;
    }

    private static Counterparty counterparty(Long id) {
        Counterparty counterparty = new Counterparty();
        ReflectionTestUtils.setField(counterparty, "id", id);
        counterparty.setName("Apex");
        counterparty.setLeiCode("APEX0000000000000001");
        counterparty.setRegion("US");
        return counterparty;
    }
}
