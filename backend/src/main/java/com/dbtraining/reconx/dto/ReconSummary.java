package com.dbtraining.reconx.dto;

/**
 * TICKET-ADV038 — Immutable summary of reconciliation results.
 */
public record ReconSummary(long total, long matched, long broken) {

    public static ReconSummary empty() {
        return new ReconSummary(0, 0, 0);
    }

    public static final class Builder {
        private long total;
        private long matched;
        private long broken;

        public void add(ReconResult result) {
            total++;

            if (result.status() == ReconResult.Status.MATCHED) {
                matched++;
            } else {
                broken++;
            }
        }

        public Builder combine(Builder other) {
            total += other.total;
            matched += other.matched;
            broken += other.broken;
            return this;
        }

        public ReconSummary build() {
            return new ReconSummary(total, matched, broken);
        }
    }
}