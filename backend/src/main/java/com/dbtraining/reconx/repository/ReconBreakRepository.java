package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.ReconBreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconBreakRepository extends JpaRepository<ReconBreak, Long> {

    /** TICKET-ADV069 — retrieve all breaks produced by a recon job. */
    List<ReconBreak> findByJobIdOrderByIdAsc(String jobId);

    /** TICKET-ADV085 — exported as recon_break_count gauge. */
    long countByStatus(String status);
}
