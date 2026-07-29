package com.dbtraining.reconx.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * ============================================================================
 * TICKET-ADV073 — JwtAuthenticationFilter
 *
 * WHAT:    Reads `Authorization: Bearer <token>`, parses it via
 *          {@link JwtTokenProvider}, and sets the SecurityContext for the
 *          current request.
 * HOW:     Extends OncePerRequestFilter so it runs exactly once per request.
 *          On a bad / expired token the context is cleared (NOT a 401) —
 *          Spring's normal auth path turns the missing principal into a 401
 *          when a protected endpoint is hit.
 * WHY:     Stateless auth: every request carries its own credential.
 * OBSERVE: A request with a valid token populates SecurityContextHolder; the
 *          downstream controller can use @AuthenticationPrincipal etc.
 * ============================================================================
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider provider;

    public JwtAuthenticationFilter(JwtTokenProvider provider) { this.provider = provider; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = provider.parse(header.substring("Bearer ".length()));
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    String subject = claims.getSubject();
                    List<String> roles = extractRoles(claims);
                    if (subject == null || subject.isBlank() || roles.isEmpty()) {
                        SecurityContextHolder.clearContext();
                    } else {
                        List<SimpleGrantedAuthority> authorities = roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList();
                        var authentication = new UsernamePasswordAuthenticationToken(
                                subject, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException ex) {
                SecurityContextHolder.clearContext();
            } catch (RuntimeException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }

    private List<String> extractRoles(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof Collection<?> roles) {
            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(role -> !role.isBlank())
                    .toList();
        }
        if (rolesClaim instanceof String role && !role.isBlank()) {
            return List.of(role);
        }

        Object roleClaim = claims.get("role");
        if (roleClaim instanceof String role && !role.isBlank()) {
            return List.of(role);
        }
        return List.of();
    }
}
