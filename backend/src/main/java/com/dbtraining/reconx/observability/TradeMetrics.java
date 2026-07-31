package com.dbtraining.reconx.observability;

import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * ADV083, ADV085, ADV086, ADV092 — ReconX business metrics.
 */
@Component
public class TradeMetrics {

    private final Counter tradeCreated;
    private final DistributionSummary tradeValue;

    public TradeMetrics(
            MeterRegistry registry,
            ReconBreakRepository breakRepository,
            TradeRepository tradeRepository
    ) {
        this.tradeCreated = Counter.builder("trade.creation")
                .description("Total trades created successfully")
                .register(registry);

        this.tradeValue = DistributionSummary
                .builder("trade.value")
                .description("Distribution of trade notional values in USD")
                
                .publishPercentileHistogram()
                .register(registry);

        Gauge.builder(
                        "recon_break_count",
                        breakRepository,
                        repository -> repository.countByStatus("OPEN")
                )
                .description("Current open reconciliation breaks")
                .register(registry);

        /*
         * The repository domain currently has four workflow states:
         * PENDING, MATCHED, BREAK, CANCELLED. Use the domain as the source of
         * truth instead of inventing UNMATCHED/DISPUTED states from the guide.
         */
        for (TradeStatus status : TradeStatus.values()) {
            Gauge.builder(
                            "trades_by_status",
                            tradeRepository,
                            repository ->
                                    repository.countByStatus(status)
                    )
                    .description("Current trades grouped by workflow status")
                    .tag("status", status.name())
                    .register(registry);
        }
    }

    public void incrementTradeCreated() {
        tradeCreated.increment();
    }

    public void recordTradeValue(double value) {
        tradeValue.record(value);
    }
}
