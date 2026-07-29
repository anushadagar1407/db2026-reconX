package com.dbtraining.reconx.exception;

/**
 * ============================================================================
 * Root of the exception hierarchy
 *
 * WHAT:    Abstract parent for every domain-level exception raised by the
 *          reconciliation service.
 * HOW:     Extends {@link RuntimeException} (we don't want checked-exception noise
 *          on the controller signatures). All subclasses go in this package.
 * WHY:     One root means {@code @RestControllerAdvice} can {@code catch (ReconException)}
 *          and map every domain-specific subtype to an RFC-7807 ProblemDetail
 *          without an explicit handler per type.
 * OBSERVE: Concrete leaves encode HTTP semantics (404/409/400/422) used by
 *          {@link GlobalExceptionHandler}.
 * ============================================================================
 */
public abstract class ReconException extends RuntimeException {

    /**
     * Creates a domain exception with a human-readable detail message.
     *
     * @param message explanation suitable for ProblemDetail detail text; may be {@code null}
     */
    protected ReconException(String message) {
        super(message);
    }

    /**
     * Creates a domain exception with a detail message and underlying cause.
     *
     * @param message explanation suitable for ProblemDetail detail text; may be {@code null}
     * @param cause   root cause to chain; may be {@code null}
     */
    protected ReconException(String message, Throwable cause) {
        super(message, cause);
    }
}
