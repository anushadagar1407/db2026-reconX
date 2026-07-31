package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.service.InstrumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** TICKET-ADV081 — cached instrument lookup by symbol. */
@RestController
@RequestMapping("/v1/instruments")
@Tag(name = "instruments", description = "Instrument reference data")
@SecurityRequirement(name = "bearerAuth")
public class InstrumentController {

    private final InstrumentService service;

    public InstrumentController(InstrumentService service) {
        this.service = service;
    }

    @GetMapping("/{symbol}")
    @Operation(summary = "Get an instrument by symbol")
    @PreAuthorize("hasAnyRole('VIEWER', 'TRADER', 'RECON_ANALYST', 'ADMIN')")
    public Instrument findBySymbol(@PathVariable String symbol) {
        return service.findBySymbol(symbol);
    }
}
