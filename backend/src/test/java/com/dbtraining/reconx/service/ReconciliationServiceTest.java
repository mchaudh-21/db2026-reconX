package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.repository.ReconResultRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReconciliationServiceTest {

    @Test
    void testReconcile_savesResultWithMatchedStatus() {
        ReconResultRepository repository =
                mock(ReconResultRepository.class);

        ReconciliationEngine engine =
                new ReconciliationEngine();

        ReconciliationService service =
                new ReconciliationService(engine, repository);

        EquityTrade internal = equity(
                "EQU-20260603-0043",
                "100.00",
                "1000"
        );

        EquityTrade external = equity(
                "EQU-20260603-0043",
                "100.00",
                "1000"
        );

        service.runRecon(
                List.of(internal),
                List.of(external),
                ReconciliationRule.EXACT
        );

        ArgumentCaptor<ReconResult> captor =
                ArgumentCaptor.forClass(ReconResult.class);

        verify(repository).save(captor.capture());

        ReconResult savedResult = captor.getValue();

        assertThat(savedResult.tradeRef())
                .isEqualTo("EQU-20260603-0043");

        assertThat(savedResult.status())
                .isEqualTo(ReconResult.Status.MATCHED);
    }

    private EquityTrade equity(String ref, String price, String qty) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(qty))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();
    }
}