package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.service.TradeStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** TICKET-ADV104 — browser-facing Server-Sent Events endpoint. */
@RestController
@RequestMapping("/v1/trades")
public class TradeStreamController {

    private final TradeStreamService stream;

    public TradeStreamController(TradeStreamService stream) {
        this.stream = stream;
    }

    @CrossOrigin(origins = "http://localhost:5500")
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return stream.subscribe();
    }
}
