package com.dbtraining.reconx.repository.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * TICKET-ADV069 — associates a recon break with its reconciliation job.
 * TICKET-ADV070 — status transitions: OPEN -> RESOLVED.
 */
@Entity
@Table(name = "recon_breaks")
public class ReconBreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Column(name = "job_id", length = 36)
    private String jobId;

    @Column(name = "discrepancy_type", nullable = false, length = 30)
    private String discrepancyType;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "detected_at")
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    public ReconBreak() {
    }

    public Long getId() {
        return id;
    }

    public Long getTradeId() {
        return tradeId;
    }

    public String getJobId() {
        return jobId;
    }

    public String getDiscrepancyType() {
        return discrepancyType;
    }

    public String getStatus() {
        return status;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setDiscrepancyType(String discrepancyType) {
        this.discrepancyType = discrepancyType;
    }

    public void resolve(String note) {
        this.status = "RESOLVED";
        this.resolvedAt = Instant.now();
        this.resolutionNote = note;
    }
}
