package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.dto.TradeResponse;
import com.dbtraining.reconx.exception.GlobalExceptionHandler;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TradeControllerTest {

    private final TradeService service = mock(TradeService.class);
    private final TradeMapper mapper = mock(TradeMapper.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TradeController(service, mapper))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void createReturnsCreatedTradeAndLocation() throws Exception {
        Trade saved = mock(Trade.class);
        TradeResponse response = new TradeResponse(
                42L, "TRD-20260730-0001", 2L, "Deutsche Bank",
                1L, "DBK",
                new BigDecimal("100.0"), new BigDecimal("245.50"),
                LocalDate.of(2026, 7, 30), "PENDING", null, null);
        when(saved.getId()).thenReturn(42L);
        when(service.create(any(), any())).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        mockMvc.perform(post("/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tradeRef": "TRD-20260730-0001",
                                  "instrumentId": 1,
                                  "counterpartyId": 2,
                                  "assetClass": "EQUITY",
                                  "side": "BUY",
                                  "quantity": 100.0,
                                  "price": 245.50,
                                  "tradeDate": "2026-07-30"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/trades/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.tradeRef").value("TRD-20260730-0001"));
    }

    @Test
    void createReturnsProblemDetailForInvalidRequest() throws Exception {
        mockMvc.perform(post("/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "instrumentId": 1,
                                  "counterpartyId": 2,
                                  "assetClass": "EQUITY",
                                  "side": "BUY",
                                  "quantity": -5,
                                  "price": 245.50,
                                  "tradeDate": "2999-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("tradeRef"),
                                org.hamcrest.Matchers.containsString("quantity"),
                                org.hamcrest.Matchers.containsString("tradeDate"))));
    }
}
