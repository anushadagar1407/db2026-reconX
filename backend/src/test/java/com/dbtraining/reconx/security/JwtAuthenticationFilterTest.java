package com.dbtraining.reconx.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-for-jwt-filter-32-bytes!";
    private static final String ISSUER = "reconx-filter-test";

    private JwtTokenProvider provider;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, 15, ISSUER);
        filter = new JwtAuthenticationFilter(provider);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenPopulatesSubjectRoleAndRequestDetails() throws Exception {
        MockHttpServletRequest request = bearerRequest(provider.generate("trader@db.com", "TRADER"));
        request.setRemoteAddr("192.0.2.10");

        CountingFilterChain chain = invoke(request);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentication.getPrincipal()).isEqualTo("trader@db.com");
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TRADER");
        assertThat(authentication.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
        assertThat(((WebAuthenticationDetails) authentication.getDetails()).getRemoteAddress())
                .isEqualTo("192.0.2.10");
        assertThat(chain.invocations).isEqualTo(1);
    }

    @Test
    void missingAuthorizationStillContinuesWithNoAuthentication() throws Exception {
        CountingFilterChain chain = invoke(new MockHttpServletRequest());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.invocations).isEqualTo(1);
    }

    @Test
    void nonBearerAuthorizationStillContinuesWithNoAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dHJhZGVyOnNlY3JldA==");

        CountingFilterChain chain = invoke(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.invocations).isEqualTo(1);
    }

    @Test
    void malformedTokenStillContinuesWithNoAuthentication() throws Exception {
        assertUnauthenticated("not.a.real.token");
    }

    @Test
    void expiredTokenStillContinuesWithNoAuthentication() throws Exception {
        String token = new JwtTokenProvider(SECRET, -1, ISSUER)
                .generate("trader@db.com", "TRADER");

        assertUnauthenticated(token);
    }

    @Test
    void tokenWithInvalidSignatureStillContinuesWithNoAuthentication() throws Exception {
        String token = new JwtTokenProvider(
                "different-secret-for-jwt-filter-32-bytes!", 15, ISSUER)
                .generate("trader@db.com", "TRADER");

        assertUnauthenticated(token);
    }

    @Test
    void validBearerTokenDoesNotOverwriteExistingAuthentication() throws Exception {
        Authentication existing = new UsernamePasswordAuthenticationToken(
                "existing@db.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(existing);

        CountingFilterChain chain = invoke(bearerRequest(provider.generate("trader@db.com", "TRADER")));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        assertThat(chain.invocations).isEqualTo(1);
    }

    @Test
    void missingBearerTokenDoesNotEraseExistingAuthentication() throws Exception {
        Authentication existing = new UsernamePasswordAuthenticationToken(
                "existing@db.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(existing);

        CountingFilterChain chain = invoke(new MockHttpServletRequest());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        assertThat(chain.invocations).isEqualTo(1);
    }

    @Test
    void filterRunsOnceAcrossAsyncDispatch() throws Exception {
        CountingJwtTokenProvider countingProvider = new CountingJwtTokenProvider();
        JwtAuthenticationFilter onceFilter = new JwtAuthenticationFilter(countingProvider);
        String token = countingProvider.generate("trader@db.com", "TRADER");
        MockHttpServletRequest request = bearerRequest(token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        CountingFilterChain chain = new CountingFilterChain();

        onceFilter.doFilter(request, response, chain);
        request.setDispatcherType(DispatcherType.ASYNC);
        onceFilter.doFilter(request, response, chain);

        assertThat(countingProvider.parseCalls).isEqualTo(1);
        assertThat(chain.invocations).isEqualTo(2);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("trader@db.com");
    }

    private void assertUnauthenticated(String token) throws Exception {
        CountingFilterChain chain = invoke(bearerRequest(token));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.invocations).isEqualTo(1);
    }

    private CountingFilterChain invoke(MockHttpServletRequest request) throws Exception {
        CountingFilterChain chain = new CountingFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    private MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }

    private static final class CountingFilterChain implements FilterChain {
        private int invocations;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            invocations++;
        }
    }

    private static final class CountingJwtTokenProvider extends JwtTokenProvider {
        private int parseCalls;

        private CountingJwtTokenProvider() {
            super(SECRET, 15, ISSUER);
        }

        @Override
        public Claims parse(String token) {
            parseCalls++;
            return super.parse(token);
        }
    }
}
