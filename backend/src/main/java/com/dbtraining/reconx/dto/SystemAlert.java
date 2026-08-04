package com.dbtraining.reconx.dto;

import java.util.Objects;

/** Alert payload published to the single-partition system-alerts topic. */
public record SystemAlert(String severity, String code, String message) {

    public SystemAlert {
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
