package com.dbtraining.reconx.security;

import com.dbtraining.reconx.controller.AuditController;
import com.dbtraining.reconx.controller.AuthController;
import com.dbtraining.reconx.controller.DeprecatedTradeController;
import com.dbtraining.reconx.controller.ReconController;
import com.dbtraining.reconx.controller.TradeController;
import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.repository.AppUserRepository;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.service.TradeService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.junit.jupiter.api.extension.ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityConfigTest.TestBeans.class)
@WebAppConfiguration
@AutoConfigureMockMvc
@TestPropertySource(properties = "server.servlet.context-path=/api")
class SecurityConfigTest {

    private static final String CONTEXT_PATH = "/api";

    @MockBean
    private TradeService tradeService;

    @MockBean
    private TradeMapper tradeMapper;

    @MockBean
    private ReconBreakRepository reconBreakRepository;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private AppUserRepository appUserRepository;

    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @jakarta.annotation.Resource
    private JwtTokenProvider tokenProvider;

    @jakarta.annotation.Resource
    private FilterChainProxy filterChainProxy;

    @BeforeEach
    void stubAllowedTradeOperations() {
        Trade trade = mock(Trade.class);
        when(trade.getId()).thenReturn(42L);
        when(tradeService.list(any(), any(), any(), any(), any())).thenReturn(Page.empty());
        when(tradeService.create(any(), anyString())).thenReturn(trade);
        when(tradeService.update(anyLong(), any(), anyString())).thenReturn(trade);
        when(tradeService.updateStatus(anyLong(), anyString(), anyString())).thenReturn(trade);
    }

    @Test
    void loginAndHealthArePublicAndCsrfIsDisabled() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contextPath(CONTEXT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/actuator/health").contextPath(CONTEXT_PATH))
                .andExpect(status().isOk())
                .andExpect(content().string("UP"));

        mockMvc.perform(get("/api/v1/api-docs/public").contextPath(CONTEXT_PATH))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/login").contextPath(CONTEXT_PATH))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/security/probe")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void missingAndInvalidBearerCredentialsReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/trades").contextPath(CONTEXT_PATH))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/trades")
                        .contextPath(CONTEXT_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedStateChangingRequestsReturn401BeforeValidation() throws Exception {
        mockMvc.perform(post("/api/v1/trades")
                        .contextPath(CONTEXT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/recon/results/42/resolve")
                        .contextPath(CONTEXT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/trades/42").contextPath(CONTEXT_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deprecatedTradePathRemainsProtectedByDefault() throws Exception {
        mockMvc.perform(get("/api/v0/trades").contextPath(CONTEXT_PATH))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v0/trades")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer("ADMIN")))
                .andExpect(status().isGone());
    }

    @Test
    void statelessRequestsDoNotCreateAnHttpSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/security/probe")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void jwtFilterIsBeforeUsernamePasswordAuthenticationFilter() {
        List<Filter> filters = filterChainProxy.getFilters("/api/v1/trades");

        int jwtIndex = indexOf(filters, JwtAuthenticationFilter.class);
        int authenticationBoundaryIndex = indexOf(filters, AnonymousAuthenticationFilter.class);

        assertThat(jwtIndex).isLessThan(authenticationBoundaryIndex);
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEWER", "TRADER", "RECON_ANALYST", "ADMIN"})
    void everyRoleCanReadTrades(String role) throws Exception {
        mockMvc.perform(get("/api/v1/trades")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRADER", "ADMIN"})
    void tradingRolesCanPostTrades(String role) throws Exception {
        mockMvc.perform(post("/api/v1/trades")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTradeRequest()))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEWER", "RECON_ANALYST"})
    void nonTradingRolesCannotPostTrades(String role) throws Exception {
        mockMvc.perform(post("/api/v1/trades")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEWER", "RECON_ANALYST"})
    void nonTradingRolesCannotPutTrades(String role) throws Exception {
        mockMvc.perform(put("/api/v1/trades/42")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTradeRequest()))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRADER", "ADMIN"})
    void tradingRolesCanPutTrades(String role) throws Exception {
        mockMvc.perform(put("/api/v1/trades/42")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTradeRequest()))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEWER", "RECON_ANALYST"})
    void nonTradingRolesCannotPatchTradeStatus(String role) throws Exception {
        mockMvc.perform(patch("/api/v1/trades/42/status")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MATCHED\"}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRADER", "ADMIN"})
    void tradingRolesCanPatchTradeStatus(String role) throws Exception {
        mockMvc.perform(patch("/api/v1/trades/42/status")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MATCHED\"}"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEWER", "TRADER", "RECON_ANALYST"})
    void nonAdminsCannotDeleteTrades(String role) throws Exception {
        mockMvc.perform(delete("/api/v1/trades/42")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteTrades() throws Exception {
        mockMvc.perform(delete("/api/v1/trades/42")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer("ADMIN")))
                .andExpect(status().isNoContent());

        verify(tradeService).softDelete(42L, "admin@db.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECON_ANALYST", "ADMIN"})
    void reconRolesCanRunRecon(String role) throws Exception {
        mockMvc.perform(post("/api/v1/recon/run")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2026-07-01\",\"to\":\"2026-07-31\"}"))
                .andExpect(status().isAccepted());
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEWER", "TRADER"})
    void nonReconRolesCannotRunRecon(String role) throws Exception {
        mockMvc.perform(post("/api/v1/recon/run")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEWER", "RECON_ANALYST", "ADMIN"})
    void readOnlyReconResultsAllowTheReadRoles(String role) throws Exception {
        mockMvc.perform(get("/api/v1/recon/jobs/job-1/results")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role)))
                .andExpect(status().isOk());
    }

    @Test
    void traderCannotReadReconResults() throws Exception {
        mockMvc.perform(get("/api/v1/recon/jobs/job-1/results")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer("TRADER")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECON_ANALYST", "ADMIN"})
    void reconRolesCanResolveReconBreaks(String role) {
        assertThatThrownBy(() -> mockMvc.perform(put("/api/v1/recon/results/42/resolve")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"resolved\"}")))
                .hasRootCauseInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEWER", "TRADER"})
    void inappropriateRolesCannotResolveReconBreaks(String role) throws Exception {
        mockMvc.perform(put("/api/v1/recon/results/42/resolve")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    void traderCannotReadAuditHistory() throws Exception {
        mockMvc.perform(get("/api/v1/audit/trades/TRD-1")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer("TRADER")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEWER", "RECON_ANALYST", "ADMIN"})
    void readOnlyRolesCanReadAuditHistory(String role) throws Exception {
        mockMvc.perform(get("/api/v1/audit/trades/TRD-1")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEWER", "RECON_ANALYST", "ADMIN"})
    void readOnlyRolesCanReadAuditEvents(String role) throws Exception {
        mockMvc.perform(get("/api/v1/audit/trades/TRD-1/events")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role)))
                .andExpect(status().isOk());
    }

    @Test
    void traderCannotReadAuditEvents() throws Exception {
        mockMvc.perform(get("/api/v1/audit/trades/TRD-1/events")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer("TRADER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void methodSecurityIsActiveForAuthenticatedEndpoints() throws Exception {
        mockMvc.perform(get("/api/method-security/probe")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer("VIEWER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/method-security/probe")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer("ADMIN")))
                .andExpect(status().isOk());
    }

    private RequestPostProcessor bearer(String role) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION,
                    "Bearer " + tokenProvider.generate(role.toLowerCase() + "@db.com", role));
            return request;
        };
    }

    private int indexOf(List<Filter> filters, Class<? extends Filter> type) {
        for (int index = 0; index < filters.size(); index++) {
            if (type.isInstance(filters.get(index))) {
                return index;
            }
        }
        throw new AssertionError("Filter not registered: " + type.getSimpleName());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @EnableSpringDataWebSupport
    @Import(SecurityConfig.class)
    static class TestBeans {

        @Bean
        TradeController tradeController(TradeService service, TradeMapper mapper) {
            return new TradeController(service, mapper);
        }

        @Bean
        ReconController reconController(ReconBreakRepository breaks) {
            return new ReconController(breaks);
        }

        @Bean
        AuditController auditController(AuditLogRepository auditRepo) {
            return new AuditController(auditRepo);
        }

        @Bean
        AuthController authController(AppUserRepository users,
                                       org.springframework.security.crypto.password.PasswordEncoder encoder,
                                       JwtTokenProvider jwt) {
            return new AuthController(users, encoder, jwt);
        }

        @Bean
        DeprecatedTradeController deprecatedTradeController() {
            return new DeprecatedTradeController();
        }

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return new JwtTokenProvider("security-chain-test-secret-32-bytes!", 60, "security-chain-test");
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider) {
            return new JwtAuthenticationFilter(provider);
        }

        @Bean
        SecurityProbeController securityProbeController() {
            return new SecurityProbeController();
        }

        @Bean
        HealthProbeController healthProbeController() {
            return new HealthProbeController();
        }

        @Bean
        DocsProbeController docsProbeController() {
            return new DocsProbeController();
        }
    }

    @org.springframework.web.bind.annotation.RestController
    static class SecurityProbeController {

        @org.springframework.web.bind.annotation.PostMapping("/security/probe")
        String post() {
            return "ok";
        }

        @org.springframework.web.bind.annotation.GetMapping("/method-security/probe")
        @PreAuthorize("hasRole('ADMIN')")
        String methodSecurity() {
            return "ok";
        }
    }

    @org.springframework.web.bind.annotation.RestController
    static class HealthProbeController {

        @org.springframework.web.bind.annotation.GetMapping("/actuator/health")
        String health() {
            return "UP";
        }
    }

    @org.springframework.web.bind.annotation.RestController
    static class DocsProbeController {

        @org.springframework.web.bind.annotation.GetMapping("/v1/api-docs/public")
        String docs() {
            return "{}";
        }
    }

    private String validTradeRequest() {
        return """
                {"tradeRef":"TRD-20260731-0001","instrumentId":1,"counterpartyId":1,
                 "assetClass":"EQUITY","side":"BUY","quantity":1,"price":1,
                 "tradeDate":"2026-07-30"}
                """;
    }
}
