package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeLookupServiceTest {

    @Mock
    private TradeRepository tradeRepo;

    @Mock
    private CounterpartyRepository cpRepo;

    @Mock
    private Trade trade;

    @Mock
    private Counterparty counterparty;

    private TradeLookupService service;

    @BeforeEach
    void setUp() {
        service = new TradeLookupService(tradeRepo, cpRepo);
    }

    @Test
    void returnsCounterpartyForExistingTradeRef() {
        String tradeRef = "TRADE-001";

        when(tradeRepo.findByTradeRef(tradeRef))
                .thenReturn(Optional.of(trade));
        when(trade.getCounterparty())
                .thenReturn(counterparty);
        when(counterparty.getId())
                .thenReturn(7L);
        when(cpRepo.findById(7L))
                .thenReturn(Optional.of(counterparty));

        Counterparty result = service.counterpartyForTradeRef(tradeRef);

        assertThat(result).isSameAs(counterparty);
    }

    @Test
    void missingTradeRefThrowsExceptionContainingReference() {
        String tradeRef = "MISSING-001";

        when(tradeRepo.findByTradeRef(tradeRef))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.counterpartyForTradeRef(tradeRef))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(tradeRef);
    }
}