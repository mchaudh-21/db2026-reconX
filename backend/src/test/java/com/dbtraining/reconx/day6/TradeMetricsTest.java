package com.dbtraining.reconx.day6;

import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TradeMetricsTest {

    @Test
    void exposesAllRequiredBusinessMeters() {
        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();

        ReconBreakRepository breakRepository =
                mock(ReconBreakRepository.class);

        TradeRepository tradeRepository =
                mock(TradeRepository.class);

        when(breakRepository.countByStatus("OPEN"))
                .thenReturn(7L);

        when(tradeRepository.countByStatus(
                TradeStatus.PENDING
        )).thenReturn(11L);

        TradeMetrics metrics = new TradeMetrics(
                registry,
                breakRepository,
                tradeRepository
        );

        metrics.incrementTradeCreated();
        metrics.recordTradeValue(24550.0);

        assertThat(registry.get("trade.creation")
                .counter()
                .count())
                .isEqualTo(1.0);

        assertThat(registry.get("trade.value")
                .summary()
                .count())
                .isEqualTo(1L);

        assertThat(registry.get("trade.value")
                .summary()
                .totalAmount())
                .isEqualTo(24550.0);

        assertThat(registry.get("recon_break_count")
                .gauge()
                .value())
                .isEqualTo(7.0);

        assertThat(registry.get("trades_by_status")
                .tag("status", "PENDING")
                .gauge()
                .value())
                .isEqualTo(11.0);
    }
}
