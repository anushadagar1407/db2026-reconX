package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.repository.AppUserRepository;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.security.JwtTokenProvider;
import com.dbtraining.reconx.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApiVersioningControllerTest {

    @Mock
    private TradeService tradeService;

    @Mock
    private TradeMapper tradeMapper;

    @Mock
    private ReconBreakRepository reconBreakRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TradeController(tradeService, tradeMapper),
                        new ReconController(reconBreakRepository),
                        new AuditController(auditLogRepository),
                        new AuthController(appUserRepository, passwordEncoder, jwtTokenProvider),
                        new DeprecatedTradeController())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void deprecatedTradesReturnsGoneWithDeprecationMetadata() throws Exception {
        String sunset = mockMvc.perform(get("/api/v0/trades").contextPath("/api"))
                .andExpect(status().isGone())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Link", "</api/v1/trades>; rel=\"successor-version\""))
                .andExpect(content().string(""))
                .andReturn()
                .getResponse()
                .getHeader("Sunset");

        assertThat(sunset).matches("^[A-Z][a-z]{2}, \\d{2} [A-Z][a-z]{2} \\d{4} \\d{2}:\\d{2}:\\d{2} GMT$");
        assertThat(ZonedDateTime.parse(sunset, DateTimeFormatter.RFC_1123_DATE_TIME))
                .isAfter(ZonedDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void currentDomainControllersUseV1WithinApiContext() throws Exception {
        mockMvc.perform(get("/api/v1/trades").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    assertThat(result.getRequest().getContextPath()).isEqualTo("/api");
                    assertThat(result.getRequest().getRequestURI()).isEqualTo("/api/v1/trades");
                })
                .andExpect(jsonPath("$.items").isArray());

        mockMvc.perform(get("/api/v1/recon/jobs/job-1/results").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(get("/api/v1/audit/trades/TRD-20260729-0001").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(get("/api/api/v1/trades").contextPath("/api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void authAndManagementPathsRemainOutsideVersionedDomainRoutes() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .contextPath("/api")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", containsString("POST")));

        mockMvc.perform(options("/api/v1/auth/login").contextPath("/api"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/actuator/health").contextPath("/api"))
                .andExpect(status().isNotFound());
    }
}
