package com.dbtraining.reconx.observability;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class MdcFilter implements Filter {

    static final String HDR_CORRELATION = "X-Correlation-Id";
    static final String HDR_TRADE_REF = "X-Trade-Ref";
    static final String CORRELATION_KEY = "correlationId";
    static final String TRADE_REF_KEY = "tradeRef";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String correlationId = header(httpRequest, HDR_CORRELATION, UUID.randomUUID().toString());
        String tradeRef = header(httpRequest, HDR_TRADE_REF, null);

        try {
            MDC.put(CORRELATION_KEY, correlationId);
            if (tradeRef == null) {
                MDC.remove(TRADE_REF_KEY);
            } else {
                MDC.put(TRADE_REF_KEY, tradeRef);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private static String header(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
