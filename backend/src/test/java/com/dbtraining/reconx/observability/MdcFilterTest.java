package com.dbtraining.reconx.observability;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MdcFilterTest {

    private final MdcFilter filter = new MdcFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void isAnOrderedSpringComponent() {
        assertThat(MdcFilter.class).hasAnnotation(Component.class);
        assertThat(MdcFilter.class.getAnnotation(Order.class).value()).isEqualTo(1);
    }

    @Test
    void propagatesExplicitCorrelationAndTradeReferenceBeforeTheChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcFilter.HDR_CORRELATION, "foo-123");
        request.addHeader(MdcFilter.HDR_TRADE_REF, "TRD-42");
        AtomicReference<String> correlationId = new AtomicReference<>();
        AtomicReference<String> tradeRef = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            correlationId.set(MDC.get(MdcFilter.CORRELATION_KEY));
            tradeRef.set(MDC.get(MdcFilter.TRADE_REF_KEY));
        });

        assertThat(correlationId).hasValue("foo-123");
        assertThat(tradeRef).hasValue("TRD-42");
        assertContextCleared();
    }

    @Test
    void generatesUuidAndOmitsTradeReferenceWhenHeadersAreAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AtomicReference<String> correlationId = new AtomicReference<>();
        AtomicReference<String> tradeRef = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            correlationId.set(MDC.get(MdcFilter.CORRELATION_KEY));
            tradeRef.set(MDC.get(MdcFilter.TRADE_REF_KEY));
        });

        assertThat(correlationId.get()).isNotBlank();
        assertThat(UUID.fromString(correlationId.get())).isNotNull();
        assertThat(tradeRef.get()).isNull();
        assertContextCleared();
    }

    @Test
    void treatsBlankHeadersAsMissingAndRemovesStaleTradeReference() throws Exception {
        MDC.put(MdcFilter.TRADE_REF_KEY, "stale-trade");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcFilter.HDR_CORRELATION, " ");
        request.addHeader(MdcFilter.HDR_TRADE_REF, "");
        AtomicReference<String> correlationId = new AtomicReference<>();
        AtomicReference<String> tradeRef = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            correlationId.set(MDC.get(MdcFilter.CORRELATION_KEY));
            tradeRef.set(MDC.get(MdcFilter.TRADE_REF_KEY));
        });

        assertThat(correlationId.get()).isNotBlank();
        assertThat(UUID.fromString(correlationId.get())).isNotNull();
        assertThat(tradeRef.get()).isNull();
        assertContextCleared();
    }

    @Test
    void clearsMdcWhenTheChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcFilter.HDR_CORRELATION, "exception-correlation");
        request.addHeader(MdcFilter.HDR_TRADE_REF, "exception-trade");
        ServletException failure = new ServletException("chain failure");

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            assertThat(MDC.get(MdcFilter.CORRELATION_KEY)).isEqualTo("exception-correlation");
            assertThat(MDC.get(MdcFilter.TRADE_REF_KEY)).isEqualTo("exception-trade");
            throw failure;
        })).isSameAs(failure);

        assertContextCleared();
    }

    @Test
    void configuresPlainDevAndJsonUatProdLogging() throws Exception {
        Document document;
        try (InputStream stream = getClass().getResourceAsStream("/logback-spring.xml")) {
            assertThat(stream).isNotNull();
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
        }

        Element dev = profile(document, "dev");
        String pattern = childText(dev, "pattern");
        assertThat(pattern)
                .contains("%X{correlationId:-}")
                .contains("%X{tradeRef:-}");

        Element uatProd = profile(document, "uat,prod");
        Element encoder = child(uatProd, "encoder");
        assertThat(encoder.getAttribute("class"))
                .isEqualTo("net.logstash.logback.encoder.LogstashEncoder");
        assertThat(childText(encoder, "includeMdc")).isEqualTo("true");
        assertThat(childText(encoder, "customFields"))
                .isEqualTo("{\"service\":\"reconx-api\"}");
    }

    private static void assertContextCleared() {
        assertThat(MDC.get(MdcFilter.CORRELATION_KEY)).isNull();
        assertThat(MDC.get(MdcFilter.TRADE_REF_KEY)).isNull();
        assertThat(MDC.get("stale")).isNull();
    }

    private static Element profile(Document document, String name) {
        NodeList profiles = document.getElementsByTagName("springProfile");
        for (int index = 0; index < profiles.getLength(); index++) {
            Element profile = (Element) profiles.item(index);
            if (name.equals(profile.getAttribute("name"))) {
                return profile;
            }
        }
        throw new AssertionError("Missing spring profile: " + name);
    }

    private static Element child(Element parent, String name) {
        NodeList children = parent.getElementsByTagName(name);
        if (children.getLength() == 0) {
            throw new AssertionError("Missing element: " + name);
        }
        return (Element) children.item(0);
    }

    private static String childText(Element parent, String name) {
        return child(parent, name).getTextContent();
    }
}
