package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.exception.GlobalExceptionHandler;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.service.TradeService;
import com.dbtraining.reconx.service.TradeStreamService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TradeControllerSoftDeleteTest {

    private final TradeService service = mock(TradeService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("delete-actor", "password"));
        mockMvc = MockMvcBuilders.standaloneSetup(new TradeController(
                        service, mock(TradeMapper.class), mock(TradeStreamService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deleteReturnsNoContentWithAnEmptyBody() throws Exception {
        mockMvc.perform(delete("/v1/trades/{id}", 42L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).softDelete(42L, "delete-actor");
    }

    @Test
    void missingTradeReturnsProblemDetailNotFound() throws Exception {
        doThrow(new TradeNotFoundException("id=999"))
                .when(service).softDelete(999L, "delete-actor");

        mockMvc.perform(delete("/v1/trades/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type")
                        .value("https://reconx.dbtraining.com/errors/trade-not-found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("id=999"))
                .andExpect(jsonPath("$.instance").value("/v1/trades/999"));
    }
}
