package com.dbtraining.reconx.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
 * OBSERVE: Individual handlers are scaffolded under TICKET-ADV062 until wired.
 * ============================================================================
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

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
        // TODO(TICKET-ADV062): return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        throw new UnsupportedOperationException("TICKET-ADV062");
    }

    /**
     * Maps a duplicate trade reference to HTTP 409 ProblemDetail.
     *
     * @param ex domain conflict exception from the service layer
     * @return ProblemDetail with {@link HttpStatus#CONFLICT}
     */
    @ExceptionHandler(DuplicateTradeRefException.class)
    public ProblemDetail duplicate(DuplicateTradeRefException ex) {
        // TODO(TICKET-ADV062): map DuplicateTradeRefException -> HttpStatus.CONFLICT (409).
        throw new UnsupportedOperationException("TICKET-ADV062");
    }

    /**
     * Maps an invalid trade to HTTP 400 ProblemDetail.
     *
     * @param ex domain validation exception from the service layer
     * @return ProblemDetail with {@link HttpStatus#BAD_REQUEST}
     */
    @ExceptionHandler(InvalidTradeException.class)
    public ProblemDetail invalid(InvalidTradeException ex) {
        // TODO(TICKET-ADV062): map InvalidTradeException -> HttpStatus.BAD_REQUEST (400).
        throw new UnsupportedOperationException("TICKET-ADV062");
    }

    /**
     * Maps a reconciliation mismatch to HTTP 422 ProblemDetail.
     *
     * @param ex domain mismatch exception from the reconciliation engine
     * @return ProblemDetail with {@link HttpStatus#UNPROCESSABLE_ENTITY}
     */
    @ExceptionHandler(ReconciliationMismatchException.class)
    public ProblemDetail mismatch(ReconciliationMismatchException ex) {
        // TODO(TICKET-ADV062): map ReconciliationMismatchException -> HttpStatus.UNPROCESSABLE_ENTITY (422).
        throw new UnsupportedOperationException("TICKET-ADV062");
    }

    /**
     * Maps bean-validation field errors on request bodies to HTTP 400 ProblemDetail.
     *
     * @param ex Spring MVC binding/validation failure
     * @return ProblemDetail listing field errors joined into the detail text
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex) {
        // TODO(TICKET-ADV062): join field errors ("field: message; ...") and return BAD_REQUEST ProblemDetail.
        //   Hint: ex.getBindingResult().getFieldErrors().stream().map(...).collect(Collectors.joining("; "))
        throw new UnsupportedOperationException("TICKET-ADV062");
    }

    /**
     * Maps constraint violations (e.g. method-level validation) to HTTP 400 ProblemDetail.
     *
     * @param ex Jakarta constraint violation exception
     * @return ProblemDetail with {@link HttpStatus#BAD_REQUEST}
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail constraint(ConstraintViolationException ex) {
        // TODO(TICKET-ADV062): map ConstraintViolationException -> HttpStatus.BAD_REQUEST (400).
        throw new UnsupportedOperationException("TICKET-ADV062");
    }
}
