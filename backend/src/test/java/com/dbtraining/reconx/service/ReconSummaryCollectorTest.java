package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.dto.ReconSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ReconSummaryCollectorTest {

    @Test
    void parallelStreamProducesSameSummaryAsSerialStream() {
        List<ReconResult> results = IntStream.range(0, 10_000)
                .mapToObj(i -> i % 4 == 0
                        ? ReconResult.breakResult(
                                "TRADE-" + i,
                                "VALUE_MISMATCH",
                                "Values differ")
                        : ReconResult.matched("TRADE-" + i))
                .toList();

        ReconSummary serialSummary =
                results.stream().collect(new ReconSummaryCollector());

        ReconSummary parallelSummary =
                results.parallelStream().collect(new ReconSummaryCollector());

        assertThat(parallelSummary).isEqualTo(serialSummary);
        assertThat(parallelSummary.total()).isEqualTo(10_000);
        assertThat(parallelSummary.matched()).isEqualTo(7_500);
        assertThat(parallelSummary.broken()).isEqualTo(2_500);
    }

    @Test
    void emptyStreamReturnsEmptySummary() {
        ReconSummary summary = List.<ReconResult>of()
                .stream()
                .collect(new ReconSummaryCollector());

        assertThat(summary).isEqualTo(ReconSummary.empty());
        assertThat(summary.total()).isZero();
        assertThat(summary.matched()).isZero();
        assertThat(summary.broken()).isZero();
    }
}