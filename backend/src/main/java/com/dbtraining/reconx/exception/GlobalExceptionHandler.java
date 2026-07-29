package com.dbtraining.reconx.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * GlobalExceptionHandler
 *
 * WHAT:    RFC 7807 {@link ProblemDetail} mapping for every {@link ReconException}
 *          subtype (and bean-validation failures) at the HTTP boundary.
 * HOW:     {@link RestControllerAdvice} with one {@link ExceptionHandler} per exception type.
 * WHY:     Clients should not parse free-text stack messages; structured status + detail
 *          keeps error handling consistent across controllers.
 * OBSERVE: Every mapped response is rendered as an RFC 7807 problem document.
 * ============================================================================
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Default Spring-managed advice constructor. */
    public GlobalExceptionHandler() {}

    /**
     * Maps a missing trade to HTTP 404 ProblemDetail.
     *
     * @param ex domain not-found exception from the service layer
     * @return ProblemDetail with {@link HttpStatus#NOT_FOUND}
     */
    @ExceptionHandler(TradeNotFoundException.class)
    public ProblemDetail notFound(TradeNotFoundException ex) {
        return problem(
                HttpStatus.NOT_FOUND,
                "https://reconx.dbtraining.com/errors/trade-not-found",
                "Trade not found",
                ex.getMessage());
    }

    /**
     * Maps a duplicate trade reference to HTTP 409 ProblemDetail.
     *
     * @param ex domain conflict exception from the service layer
     * @return ProblemDetail with {@link HttpStatus#CONFLICT}
     */
    @ExceptionHandler(DuplicateTradeRefException.class)
    public ProblemDetail duplicate(DuplicateTradeRefException ex) {
        return problem(
                HttpStatus.CONFLICT,
                "https://reconx.dbtraining.com/errors/duplicate-trade-ref",
                "Duplicate trade reference",
                ex.getMessage());
    }

    /**
     * Maps an invalid trade to HTTP 400 ProblemDetail.
     *
     * @param ex domain validation exception from the service layer
     * @return ProblemDetail with {@link HttpStatus#BAD_REQUEST}
     */
    @ExceptionHandler(InvalidTradeException.class)
    public ProblemDetail invalid(InvalidTradeException ex) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "https://reconx.dbtraining.com/errors/invalid-trade",
                "Invalid trade",
                ex.getMessage());
    }

    /**
     * Maps a reconciliation mismatch to HTTP 422 ProblemDetail.
     *
     * @param ex domain mismatch exception from the reconciliation engine
     * @return ProblemDetail with {@link HttpStatus#UNPROCESSABLE_ENTITY}
     */
    @ExceptionHandler(ReconciliationMismatchException.class)
    public ProblemDetail mismatch(ReconciliationMismatchException ex) {
        ProblemDetail problem = problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "https://reconx.dbtraining.com/errors/recon-failure",
                "Reconciliation failure",
                ex.getMessage());
        problem.setProperty("reconBreakId", ex.getReconBreakId());
        return problem;
    }

    /**
     * Maps an otherwise unclassified domain exception to HTTP 422.
     *
     * @param ex domain exception without a more specific HTTP mapping
     * @return ProblemDetail with {@link HttpStatus#UNPROCESSABLE_ENTITY}
     */
    @ExceptionHandler(ReconException.class)
    public ProblemDetail recon(ReconException ex) {
        ProblemDetail problem = problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "https://reconx.dbtraining.com/errors/recon-failure",
                "Reconciliation failure",
                ex.getMessage());
        problem.setProperty("reconBreakId", null);
        return problem;
    }

    /**
     * Maps bean-validation field errors on request bodies to HTTP 400 ProblemDetail.
     *
     * @param ex Spring MVC binding/validation failure
     * @return ProblemDetail listing field errors joined into the detail text
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(
                HttpStatus.BAD_REQUEST,
                "https://reconx.dbtraining.com/errors/validation-failed",
                "Validation failed",
                detail);
    }

    /**
     * Maps constraint violations (e.g. method-level validation) to HTTP 400 ProblemDetail.
     *
     * @param ex Jakarta constraint violation exception
     * @return ProblemDetail with {@link HttpStatus#BAD_REQUEST}
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail constraint(ConstraintViolationException ex) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "https://reconx.dbtraining.com/errors/constraint-violation",
                "Constraint violation",
                ex.getMessage());
    }

    /**
     * Keeps implementation details out of responses for errors that are not part
     * of the public domain error contract.
     *
     * @param ex uncaught application exception
     * @return safe HTTP 500 ProblemDetail
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail generic(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "https://reconx.dbtraining.com/errors/internal-server-error",
                "Internal server error",
                "An unexpected error occurred — please contact support with the correlationId");
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
