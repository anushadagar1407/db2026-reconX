package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.ReconRunRequest;
import com.dbtraining.reconx.dto.ReconRunResponse;
import com.dbtraining.reconx.dto.ResolutionRequest;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.entity.ReconBreak;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * TICKET-ADV068 — POST /api/v1/recon/run — returns 202 + jobId
 * TICKET-ADV069 — GET  /api/v1/recon/jobs/{jobId}/results
 * TICKET-ADV070 — PUT  /api/v1/recon/results/{id}/resolve
 */
@RestController
@RequestMapping("/v1/recon")
@Tag(name = "recon", description = "Reconciliation operations")
@SecurityRequirement(name = "bearerAuth")
public class ReconController {

    private static final Logger log = LoggerFactory.getLogger(ReconController.class);

    private final ReconBreakRepository breaks;

    public ReconController(ReconBreakRepository breaks) { this.breaks = breaks; }

    @PostMapping("/run")
    @Operation(summary = "Trigger a reconciliation job (async)")
    @PreAuthorize("hasAnyRole('RECON_ANALYST', 'ADMIN')")
    public ResponseEntity<ReconRunResponse> runRecon(@Valid @RequestBody ReconRunRequest req) {
        // TICKET-ADV068: generate a jobId, write a row to recon_jobs, and
        //   return 202 Accepted with {"jobId": ..., "status": "QUEUED"}. A
        //   worker (Day 6 / Kafka consumer) picks the job up asynchronously.
        UUID jobId = UUID.randomUUID();
        log.info("recon job dispatched: jobId={}", jobId);

        URI resultsLocation = URI.create("/api/v1/recon/jobs/" + jobId + "/results");
        return ResponseEntity.accepted()
                .location(resultsLocation)
                .body(new ReconRunResponse(jobId, "QUEUED"));
    }

    @GetMapping("/jobs/{jobId}/results")
    @Operation(summary = "Get results for a recon job")
    @PreAuthorize("hasAnyRole('VIEWER', 'RECON_ANALYST', 'ADMIN')")
    public List<ReconBreak> results(@PathVariable String jobId) {
        // TODO(TICKET-ADV069): once recon_jobs + recon_breaks tables are wired,
        //   return breaks.findByJobId(jobId). Day-0 returns an empty list so
        //   the React breaks-table renders "no breaks" gracefully.
        return Collections.emptyList();
    }

    @GetMapping("/results/{id}")
    @Operation(summary = "Get a single recon break by id")
    public ResponseEntity<ReconBreak> getById(@PathVariable Long id) {
        ReconBreak rb = breaks.findById(id)
                .orElseThrow(() -> new RuntimeException("recon_break" + id + " not found"));
        return ResponseEntity.ok(rb);
    }

    @PutMapping("/results/{id}/resolve")
    @Operation(summary = "Mark a recon break as RESOLVED with a note")
    @PreAuthorize("hasAnyRole('RECON_ANALYST', 'ADMIN')")
    public ResponseEntity<ReconBreak> resolve(@PathVariable Long id,
                                              @Valid @RequestBody ResolutionRequest request) {
        // TICKET-ADV070: load the ReconBreak, call rb.resolve(note), save,
        //   and return 200 with the updated entity. Throw TradeNotFoundException
        //   when the id is unknown.
        ReconBreak rb = breaks.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("recon_break " + id));
        rb.resolve(request.note());
        return ResponseEntity.ok(breaks.save(rb));
    }
}
