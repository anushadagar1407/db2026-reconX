package com.dbtraining.reconx.security;

import com.dbtraining.reconx.controller.AuditController;
import com.dbtraining.reconx.controller.AuthController;
import com.dbtraining.reconx.controller.ReconController;
import com.dbtraining.reconx.controller.TradeController;
import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.repository.AppUserRepository;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.service.TradeService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @ValueSource(strings = {"VIEWER", "RECON_ANALYST"})
    void nonTradingRolesCannotPostTrades(String role) throws Exception {
        mockMvc.perform(post("/api/v1/trades")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCannotPostTrades() throws Exception {
        mockMvc.perform(post("/api/v1/trades")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer("VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void traderCannotDeleteTrades() throws Exception {
        mockMvc.perform(delete("/api/v1/trades/42")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer("TRADER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDeleteReachesTheScaffoldedController() {
        assertThatThrownBy(() -> mockMvc.perform(delete("/api/v1/trades/42")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer("ADMIN"))))
                .hasRootCauseInstanceOf(UnsupportedOperationException.class);
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

    @Test
    void reconAnalystResolveReachesTheScaffoldedController() {
        assertThatThrownBy(() -> mockMvc.perform(put("/api/v1/recon/results/42/resolve")
                        .contextPath(CONTEXT_PATH)
                        .with(bearer("RECON_ANALYST"))
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
}
