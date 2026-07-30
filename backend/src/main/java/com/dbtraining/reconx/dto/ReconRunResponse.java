package com.dbtraining.reconx.dto;

import java.util.UUID;

/** TICKET-ADV068 — initial asynchronous reconciliation job handle. */
public record ReconRunResponse(UUID jobId, String status) {}
