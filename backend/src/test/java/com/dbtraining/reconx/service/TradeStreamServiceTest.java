package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TradeStreamServiceTest {

    @Test
    void subscribedBrowserReceivesPublishedTrade() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);
        TradeStreamService service = new TradeStreamService() {
            @Override
            SseEmitter createEmitter() {
                return emitter;
            }
        };

        assertThat(service.subscribe()).isSameAs(emitter);
        assertThat(service.subscriberCount()).isOne();
        clearInvocations(emitter);

        service.publish(trade());

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    private TradeResponse trade() {
        Instant now = Instant.parse("2026-08-03T12:00:00Z");
        return new TradeResponse(
                42L, "TRD-20260803-0001", 1L, "Apex", 2L, "SAP.DE",
                new BigDecimal("100"), new BigDecimal("125.50"),
                LocalDate.of(2026, 8, 3), "PENDING", now, now);
    }
}
