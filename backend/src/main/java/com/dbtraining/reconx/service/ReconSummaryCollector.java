package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.dto.ReconSummary;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * TICKET-ADV038 — Collector that summarises reconciliation results.
 */
public final class ReconSummaryCollector
        implements Collector<
                ReconResult,
                ReconSummary.Builder,
                ReconSummary> {

    @Override
    public Supplier<ReconSummary.Builder> supplier() {
        return ReconSummary.Builder::new;
    }

    @Override
    public BiConsumer<ReconSummary.Builder, ReconResult> accumulator() {
        return ReconSummary.Builder::add;
    }

    @Override
    public BinaryOperator<ReconSummary.Builder> combiner() {
        return ReconSummary.Builder::combine;
    }

    @Override
    public Function<ReconSummary.Builder, ReconSummary> finisher() {
        return ReconSummary.Builder::build;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.singleton(Characteristics.UNORDERED);
    }
}