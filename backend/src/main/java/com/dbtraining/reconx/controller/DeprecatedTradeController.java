package com.dbtraining.reconx.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@RestController
public class DeprecatedTradeController {

    private static final String SUNSET_DATE = ZonedDateTime
            .of(2099, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.RFC_1123_DATE_TIME);
    private static final String SUCCESSOR_LINK = "</api/v1/trades>; rel=\"successor-version\"";

    @Deprecated(since = "v1.4.0", forRemoval = true)
    @GetMapping("/v0/trades")
    public ResponseEntity<Void> deprecatedTrades() {
        return ResponseEntity.status(HttpStatus.GONE)
                .header("Deprecation", "true")
                .header("Sunset", SUNSET_DATE)
                .header("Link", SUCCESSOR_LINK)
                .build();
    }
}
