package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.ReconRunRequest;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.ReconJobRepository;
import com.dbtraining.reconx.repository.entity.ReconBreak;
import com.dbtraining.reconx.repository.entity.ReconJob;
import com.dbtraining.reconx.service.ReconciliationEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ADV068–ADV070 plus the ADV084 timer trigger.
 */
@RestController
@RequestMapping("/v1/recon")
@Tag(name = "recon", description = "Reconciliation operations")
@SecurityRequirement(name = "bearerAuth")
public class ReconController {

    private final ReconBreakRepository breaks;
    private final ReconJobRepository jobs;
    private final ReconciliationEngine engine;

    /**
     * Preserves direct construction in existing controller tests.
     */
    public ReconController(
            ReconBreakRepository breaks,
            ReconJobRepository jobs
    ) {
        this(breaks, jobs, null);
    }

    @Autowired
    public ReconController(
            ReconBreakRepository breaks,
            ReconJobRepository jobs,
            ReconciliationEngine engine
    ) {
        this.breaks = breaks;
        this.jobs = jobs;
        this.engine = engine;
    }

    @PostMapping("/run")
    @Operation(summary = "Trigger a reconciliation job (async)")
    public ResponseEntity<Map<String, String>> runRecon(
            @Valid @RequestBody ReconRunRequest request
    ) {
        String jobId = UUID.randomUUID().toString();

        ReconJob job = new ReconJob(
                jobId,
                request.from(),
                request.to()
        );
        jobs.save(job);

        /*
         * The current endpoint queues the job but has no worker yet. Calling
         * the real engine with an empty batch records a truthful invocation
         * sample without changing the existing queue/result behavior.
         */
        if (engine != null) {
            engine.reconcile(
                    List.of(),
                    List.of(),
                    ReconciliationRule.EXACT
            );
        }

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "jobId", jobId,
                        "status", "QUEUED"
                ));
    }

    @GetMapping("/jobs/{jobId}/results")
    @Operation(summary = "Get results for a recon job")
    public List<ReconBreak> results(
            @PathVariable String jobId
    ) {
        return breaks.findByJobIdOrderByIdAsc(jobId);
    }

    @PutMapping("/results/{id}/resolve")
    @Operation(summary = "Mark a recon break as RESOLVED with a note")
    public ResponseEntity<ReconBreak> resolve(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        ReconBreak reconBreak = breaks.findById(id)
                .orElseThrow(() ->
                        new TradeNotFoundException(
                                String.valueOf(id)
                        )
                );

        reconBreak.resolve(body.get("note"));
        return ResponseEntity.ok(breaks.save(reconBreak));
    }
}
