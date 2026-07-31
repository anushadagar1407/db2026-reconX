package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.dto.TradeResponse;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.exception.GlobalExceptionHandler;
import com.dbtraining.reconx.exception.InvalidTradeException;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TradeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TradeService service;

    @Mock
    private TradeMapper mapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TradeController(service, mapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

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

        var request = ArgumentCaptor.forClass(TradeRequest.class);
        verify(service).create(request.capture(), anyString());
        assertThat(request.getValue()).isEqualTo(new TradeRequest(
                "TRD-20260730-0001", 1L, 2L, "EQUITY", "BUY",
                new BigDecimal("100.0"), new BigDecimal("245.50"),
                LocalDate.of(2026, 7, 30)));
        verify(mapper).toResponse(saved);
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

        verifyNoInteractions(service, mapper);
    }

    @Test
    void createReturnsConflictProblemDetailForDuplicateReference() throws Exception {
        when(service.create(any(), any()))
                .thenThrow(new DuplicateTradeRefException("Trade reference already exists"));

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
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Duplicate trade reference"))
                .andExpect(jsonPath("$.detail").value("Trade reference already exists"));

        verify(service).create(any(TradeRequest.class), anyString());
        verifyNoInteractions(mapper);
    }

    @Test
    void updateStatusReturnsMappedTradeResponse() throws Exception {
        Trade updated = new Trade();
        TradeResponse response = response();
        when(service.updateStatus(eq(42L), eq("MATCHED"), anyString())).thenReturn(updated);
        when(mapper.toResponse(updated)).thenReturn(response);

        mockMvc.perform(patch("/v1/trades/42/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MATCHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("MATCHED"));

        verify(service).updateStatus(eq(42L), eq("MATCHED"), anyString());
        verify(mapper).toResponse(updated);
    }

    @Test
    void updateStatusReturnsBadRequestProblemDetailForInvalidStatus() throws Exception {
        when(service.updateStatus(eq(42L), eq("FOOBAR"), anyString()))
                .thenThrow(new InvalidTradeException("Invalid trade status: FOOBAR"));

        mockMvc.perform(patch("/v1/trades/42/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FOOBAR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid trade"))
                .andExpect(jsonPath("$.detail").value("Invalid trade status: FOOBAR"));

        verify(service).updateStatus(eq(42L), eq("FOOBAR"), anyString());
        verifyNoInteractions(mapper);
    }

    @Test
    void updateStatusReturnsNotFoundProblemDetailForMissingTrade() throws Exception {
        when(service.updateStatus(eq(42L), eq("MATCHED"), anyString()))
                .thenThrow(new TradeNotFoundException("Trade not found: id=42"));

        mockMvc.perform(patch("/v1/trades/42/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MATCHED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Trade not found"))
                .andExpect(jsonPath("$.detail").value("Trade not found: id=42"));

        verify(service).updateStatus(eq(42L), eq("MATCHED"), anyString());
        verifyNoInteractions(mapper);
    }

    @Test
    void listReturnsStableEnvelopeForRequestedPageSize() throws Exception {
        Trade trade = new Trade();
        TradeResponse response = response();
        when(service.list(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(trade),
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "tradeDate")),
                        21));
        when(mapper.toResponse(trade)).thenReturn(response);

        mockMvc.perform(get("/v1/trades")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(5)))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].tradeRef").value("TRD-2026-000001"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(5))
                .andExpect(jsonPath("$.pageable").doesNotExist());

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(isNull(), isNull(), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getSort().getOrderFor("tradeDate").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listUsesDefaultPageSizeAndSortWhenPageableIsOmitted() throws Exception {
        when(service.list(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(),
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "tradeDate")),
                        41));

        mockMvc.perform(get("/v1/trades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(41))
                .andExpect(jsonPath("$.totalPages").value(3));

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(isNull(), isNull(), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().getOrderFor("tradeDate").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listForwardsDateStatusAndCounterpartyFilters() throws Exception {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(service.list(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/v1/trades")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("status", "MATCHED")
                        .param("counterpartyId", "42"))
                .andExpect(status().isOk());

        verify(service).list(eq(from), eq(to), eq("MATCHED"), eq(42L), any(Pageable.class));
    }

    @Test
    void listAcceptsExplicitSortDirection() throws Exception {
        when(service.list(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/v1/trades").param("sort", "tradeDate,asc"))
                .andExpect(status().isOk());

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(isNull(), isNull(), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("tradeDate").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void invalidDateReturnsBadRequestWithoutQueryingService() throws Exception {
        mockMvc.perform(get("/v1/trades").param("from", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request parameter"))
                .andExpect(jsonPath("$.detail")
                        .value("Request parameter 'from' has an invalid value"));

        verifyNoInteractions(service, mapper);
    }

    @Test
    void invalidStatusReturnsBadRequestWithoutQueryingService() throws Exception {
        mockMvc.perform(get("/v1/trades").param("status", "SETTLED"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request parameter"))
                .andExpect(jsonPath("$.detail")
                        .value("Request parameter 'status' has an invalid value"));

        verifyNoInteractions(service, mapper);
    }

    @Test
    void invalidPageReturnsBadRequestWithoutQueryingService() throws Exception {
        assertInvalidParameter("page", "not-a-number");
    }

    @Test
    void negativePageReturnsBadRequestWithoutQueryingService() throws Exception {
        assertInvalidParameter("page", "-1");
    }

    @Test
    void invalidSizeReturnsBadRequestWithoutQueryingService() throws Exception {
        assertInvalidParameter("size", "not-a-number");
    }

    @Test
    void nonPositiveSizeReturnsBadRequestWithoutQueryingService() throws Exception {
        assertInvalidParameter("size", "0");
    }

    @Test
    void invalidSortReturnsBadRequestWithoutQueryingService() throws Exception {
        assertInvalidParameter("sort", "notATradeProperty,desc");
    }

    @Test
    void invalidSortDirectionReturnsBadRequestWithoutQueryingService() throws Exception {
        assertInvalidParameter("sort", "tradeDate,sideways");
    }

    private void assertInvalidParameter(String name, String value) throws Exception {
        mockMvc.perform(get("/v1/trades").param(name, value))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request parameter"))
                .andExpect(jsonPath("$.detail")
                        .value("Request parameter '%s' has an invalid value".formatted(name)));

        verifyNoInteractions(service, mapper);
    }

    private static TradeResponse response() {
        return new TradeResponse(
                1L,
                "TRD-2026-000001",
                10L,
                "Acme Capital",
                20L,
                "AAPL",
                new BigDecimal("2.0000"),
                new BigDecimal("100.5000"),
                LocalDate.of(2026, 5, 1),
                "MATCHED",
                Instant.parse("2026-05-01T10:15:30Z"),
                Instant.parse("2026-05-01T10:15:30Z"));
    }
}
