package com.dbtraining.reconx.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.Set;

import com.dbtraining.reconx.dto.PagedResponse;
import com.dbtraining.reconx.dto.StatusUpdate;
import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.dto.TradeResponse;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import com.dbtraining.reconx.service.TradeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * ============================================================================
 * TICKET-ADV063-ADV067 — TradeController (full CRUD + filterable list)
 * TICKET-ADV080 — API versioning: every endpoint under /v1/
 *
 * Combined with the /api context-path from application.yml, full URLs are
 * /api/v1/trades, /api/v1/trades/{id} etc.
 * ============================================================================
 */
@RestController
@RequestMapping("/v1/trades")
@Tag(name = "trades", description = "Trade CRUD and search")
@SecurityRequirement(name = "bearerAuth")
public class TradeController {

    private static final Set<String> SORTABLE_PROPERTIES = Set.of(
            "id",
            "tradeRef",
            "instrument.id",
            "instrument.symbol",
            "counterparty.id",
            "counterparty.name",
            "assetClass",
            "side",
            "quantity",
            "price",
            "tradeDate",
            "status",
            "deletedAt",
            "createdAt",
            "modifiedAt");

    private final TradeService service;
    private final TradeMapper mapper;

    public TradeController(TradeService service, TradeMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List trades — paginated, filterable, sortable")
    public PagedResponse<TradeResponse> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TradeStatus status,
            @RequestParam(required = false) Long counterpartyId,
            @RequestParam(name = "page", required = false) Integer requestedPage,
            @RequestParam(name = "size", required = false) Integer requestedSize,
            @RequestParam(name = "sort", required = false) String requestedSort,
            @PageableDefault(size = 20, sort = "tradeDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        validatePageable(requestedPage, requestedSize, requestedSort);
        var page = service.list(
                from,
                to,
                status == null ? null : status.name(),
                counterpartyId,
                pageable);
        return PagedResponse.of(page, mapper::toResponse);
    }

    private static void validatePageable(Integer requestedPage,
                                         Integer requestedSize,
                                         String requestedSort) {
        if (requestedPage != null && requestedPage < 0) {
            throw invalidParameter("page", requestedPage);
        }
        if (requestedSize != null && requestedSize <= 0) {
            throw invalidParameter("size", requestedSize);
        }
        if (requestedSort == null) {
            return;
        }
        String[] parts = requestedSort.split(",", -1);
        if (parts.length > 2
                || parts[0].isBlank()
                || !SORTABLE_PROPERTIES.contains(parts[0])
                || (parts.length == 2
                && !parts[1].equalsIgnoreCase("asc")
                && !parts[1].equalsIgnoreCase("desc"))) {
            throw invalidParameter("sort", requestedSort);
        }
    }

    private static MethodArgumentTypeMismatchException invalidParameter(String name, Object value) {
        return new MethodArgumentTypeMismatchException(value, String.class, name, null, null);
    }

    @PostMapping
    @Operation(summary = "Create a trade")
    public ResponseEntity<TradeResponse> create(@Valid @RequestBody TradeRequest req,
                                                @AuthenticationPrincipal Object principal) {
        // TICKET-ADV064: call service.create(req, actor), build a Location
        //   header at /api/v1/trades/{id}, and return 201 Created with the
        //   mapped TradeResponse body.
        Trade saved = service.create(req, String.valueOf(principal));
        URI location = URI.create("/api/v1/trades/" + saved.getId());
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a trade")
    public TradeResponse getById(@PathVariable Long id) {
        return mapper.toResponse(service.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Full update of a trade")
    public TradeResponse update(@PathVariable Long id,
                                @Valid @RequestBody TradeRequest req,
                                @AuthenticationPrincipal Object principal) {
        return mapper.toResponse(
                service.update(id, req, String.valueOf(principal))
        );
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update only the status field")
    public TradeResponse updateStatus(@PathVariable Long id,
            @Valid @RequestBody StatusUpdate request,
            @AuthenticationPrincipal Object principal) {
        return mapper.toResponse(
                service.updateStatus(id, request.status(), String.valueOf(principal))
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete (sets deleted_at)")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal Object principal) {
        // TODO(TICKET-ADV067): service.softDelete(id, actor); return 204 No Content.
        throw new UnsupportedOperationException("TICKET-ADV067");
    }
}
