package com.dbtraining.reconx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** TICKET-ADV066 — PATCH /api/v1/trades/{id}/status body. */
public record StatusUpdate(
        @NotBlank(message = "status is required")
        @Pattern(
                regexp = "^(PENDING|MATCHED|UNMATCHED|DISPUTED)$",
                message = "status must be PENDING, MATCHED, UNMATCHED, or DISPUTED"
        )
        String status
) {}
