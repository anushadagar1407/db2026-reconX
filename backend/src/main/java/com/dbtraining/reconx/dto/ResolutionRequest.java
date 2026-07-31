package com.dbtraining.reconx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** TICKET-ADV070 - PUT /api/v1/recon/results/{id}/resolve body. */
public record ResolutionRequest(
        @NotBlank @Size(max = 500) String note
) {}
