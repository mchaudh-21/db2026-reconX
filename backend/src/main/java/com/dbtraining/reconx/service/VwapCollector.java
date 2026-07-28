package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.EquityTrade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * TICKET-ADV035 — Custom collector for:
 *
 * VWAP = SUM(price * quantity) / SUM(quantity)
 */
public final class VwapCollector
        implements Collector<EquityTrade, VwapCollector.Accumulator, BigDecimal> {

    /**
     * The guide conflicts between four and six decimal places.
     * This implementation follows the "Done when" criterion: six places.
     */
    public static final int SCALE = 6;

    @Override
    public Supplier<Accumulator> supplier() {
        return Accumulator::new;
    }

    @Override
    public BiConsumer<Accumulator, EquityTrade> accumulator() {
        return Accumulator::add;
    }

    @Override
    public BinaryOperator<Accumulator> combiner() {
        return Accumulator::combinedWith;
    }

    @Override
    public Function<Accumulator, BigDecimal> finisher() {
        return Accumulator::finish;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.UNORDERED);
    }

    /**
     * Mutable reduction container used only while the stream is collecting.
     */
    public static final class Accumulator {
        private BigDecimal weightedTotal = BigDecimal.ZERO;
        private BigDecimal quantityTotal = BigDecimal.ZERO;

        private Accumulator() {
        }

        private Accumulator(BigDecimal weightedTotal, BigDecimal quantityTotal) {
            this.weightedTotal = weightedTotal;
            this.quantityTotal = quantityTotal;
        }

        void add(EquityTrade trade) {
            weightedTotal = weightedTotal.add(
                    trade.price().multiply(trade.quantity())
            );
            quantityTotal = quantityTotal.add(trade.quantity());
        }

        Accumulator combinedWith(Accumulator other) {
            // Return a fresh accumulator so parallel partial results are not
            // unexpectedly mutated after combination.
            return new Accumulator(
                    weightedTotal.add(other.weightedTotal),
                    quantityTotal.add(other.quantityTotal)
            );
        }

        BigDecimal finish() {
            if (quantityTotal.signum() == 0) {
                return BigDecimal.ZERO;
            }

            return weightedTotal.divide(
                    quantityTotal,
                    SCALE,
                    RoundingMode.HALF_UP
            );
        }
    }
}
