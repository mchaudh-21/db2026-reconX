package com.dbtraining.reconx.repository.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "recon_jobs")
public class ReconJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true, length = 36)
    private String jobId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(nullable = false, length = 20)
    private String status = "QUEUED";

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "trades_processed", nullable = false)
    private int tradesProcessed;

    @Column(name = "breaks_detected", nullable = false)
    private int breaksDetected;

    public ReconJob() {
    }

    public ReconJob(String jobId, LocalDate fromDate, LocalDate toDate) {
        this.jobId = jobId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.status = "QUEUED";
    }

    public Long getId() {
        return id;
    }

    public String getJobId() {
        return jobId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public String getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public int getTradesProcessed() {
        return tradesProcessed;
    }

    public int getBreaksDetected() {
        return breaksDetected;
    }
}
