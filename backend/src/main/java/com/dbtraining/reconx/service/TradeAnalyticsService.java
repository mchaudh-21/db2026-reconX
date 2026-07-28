package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.BondTrade;
import com.dbtraining.reconx.model.DerivativeTrade;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.FXTrade;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * TICKET-ADV034 through TICKET-ADV036 — stream-based trade analytics.
 */
@Service
public class TradeAnalyticsService {

    /**
     * TICKET-ADV034 — One-pass BigDecimal summary per counterparty.
     */
    public Map<Long, NotionalSummary> notionalByCounterparty(
            List<? extends TradeType> trades
    ) {
        if (trades == null || trades.isEmpty()) {
            return Map.of();
        }

        return trades.stream().collect(Collectors.groupingBy(
                this::counterpartyIdOf,
                Collector.of(
                        NotionalAccumulator::new,
                        NotionalAccumulator::add,
                        NotionalAccumulator::combinedWith,
                        NotionalAccumulator::finish
                )
        ));
    }

    /**
     * TICKET-ADV035 — VWAP per equity instrument, calculated by the custom
     * VwapCollector with no double conversion.
     */
    public Map<String, BigDecimal> vwapByInstrument(
            List<EquityTrade> equityTrades
    ) {
        if (equityTrades == null || equityTrades.isEmpty()) {
            return Map.of();
        }

        return equityTrades.stream().collect(Collectors.groupingBy(
                EquityTrade::instrumentSymbol,
                new VwapCollector()
        ));
    }

    /**
     * TICKET-ADV036 — Signed P&L per instrument.
     */
    public Map<String, BigDecimal> pnlByInstrument(
            List<EquityTrade> equityTrades
    ) {
        if (equityTrades == null || equityTrades.isEmpty()) {
            return Map.of();
        }

        return equityTrades.stream().collect(Collectors.groupingBy(
                EquityTrade::instrumentSymbol,
                Collectors.mapping(
                        this::pnl,
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
        ));
    }

    private BigDecimal pnl(EquityTrade trade) {
        BigDecimal absoluteValue = trade.price().multiply(trade.quantity());
        return trade.side() == Side.SELL
                ? absoluteValue
                : absoluteValue.negate();
    }

    private long counterpartyIdOf(TradeType trade) {
        return switch (trade) {
            case EquityTrade equity -> equity.counterpartyId();
            case FXTrade fx -> fx.counterpartyId();
            case BondTrade bond -> bond.counterpartyId();
            case DerivativeTrade derivative -> derivative.counterpartyId();
        };
    }

    /**
     * All five aggregates required by ADV034.
     */
    public record NotionalSummary(
            long count,
            BigDecimal total,
            BigDecimal min,
            BigDecimal max,
            BigDecimal average
    ) {
    }

    private static final class NotionalAccumulator {
        private long count;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal min;
        private BigDecimal max;

        void add(TradeType trade) {
            BigDecimal amount = trade.notional().amount();

            count++;
            total = total.add(amount);
            min = min == null || amount.compareTo(min) < 0 ? amount : min;
            max = max == null || amount.compareTo(max) > 0 ? amount : max;
        }

        NotionalAccumulator combinedWith(NotionalAccumulator other) {
            NotionalAccumulator combined = new NotionalAccumulator();
            combined.count = count + other.count;
            combined.total = total.add(other.total);
            combined.min = smaller(min, other.min);
            combined.max = larger(max, other.max);
            return combined;
        }

        NotionalSummary finish() {
            if (count == 0) {
                return new NotionalSummary(
                        0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );
            }

            BigDecimal average = total.divide(
                    BigDecimal.valueOf(count),
                    6,
                    RoundingMode.HALF_UP
            );

            return new NotionalSummary(count, total, min, max, average);
        }

        private static BigDecimal smaller(BigDecimal left, BigDecimal right) {
            if (left == null) {
                return right;
            }
            if (right == null) {
                return left;
            }
            return left.compareTo(right) <= 0 ? left : right;
        }

        private static BigDecimal larger(BigDecimal left, BigDecimal right) {
            if (left == null) {
                return right;
            }
            if (right == null) {
                return left;
            }
            return left.compareTo(right) >= 0 ? left : right;
        }
    }
}
