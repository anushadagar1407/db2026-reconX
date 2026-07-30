package com.dbtraining.reconx.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-for-jwt-provider-32-bytes!";
    private static final long EXPIRATION_MINUTES = 15;
    private static final String ISSUER = "reconx-test";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, EXPIRATION_MINUTES, ISSUER);
    }

    @Test
    void generatedTokenContainsIdentityRoleAndExpirationClaims() {
        Claims claims = provider.parse(provider.generate("trader@db.com", "TRADER"));

        assertThat(claims.getSubject()).isEqualTo("trader@db.com");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.get("role", String.class)).isEqualTo("TRADER");
        assertThat(claims.get("roles")).isEqualTo(List.of("TRADER"));
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(Duration.between(
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()))
                .isEqualTo(Duration.ofSeconds(provider.expirationSeconds()));
    }

    @Test
    void expirationEnvelopeUsesConfiguredSeconds() {
        assertThat(provider.expirationSeconds()).isEqualTo(900);
    }

    @Test
    void parserRejectsTokenSignedWithAnotherSecret() {
        String token = new JwtTokenProvider(
                "another-test-secret-for-jwt-32-bytes!", EXPIRATION_MINUTES, ISSUER)
                .generate("trader@db.com", "TRADER");

        assertThatThrownBy(() -> provider.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parserRequiresConfiguredIssuer() {
        String token = new JwtTokenProvider(SECRET, EXPIRATION_MINUTES, "different-issuer")
                .generate("trader@db.com", "TRADER");

        assertThatThrownBy(() -> provider.parse(token))
                .isInstanceOf(JwtException.class);
    }
}
