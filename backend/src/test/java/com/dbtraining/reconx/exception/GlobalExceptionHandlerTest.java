package com.dbtraining.reconx.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void mapsTradeNotFoundToProblemDetail() throws Exception {
        mockMvc.perform(get("/probe/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://reconx.dbtraining.com/errors/trade-not-found"))
                .andExpect(jsonPath("$.title").value("Trade not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("trade 999 was not found"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void mapsDuplicateReferenceToConflictProblemDetail() throws Exception {
        mockMvc.perform(get("/probe/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://reconx.dbtraining.com/errors/duplicate-trade-ref"))
                .andExpect(jsonPath("$.title").value("Duplicate trade reference"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void mapsInvalidTradeToBadRequestProblemDetail() throws Exception {
        mockMvc.perform(get("/probe/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://reconx.dbtraining.com/errors/invalid-trade"))
                .andExpect(jsonPath("$.title").value("Invalid trade"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void mapsReconciliationMismatchWithBreakIdentifier() throws Exception {
        mockMvc.perform(get("/probe/mismatch"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://reconx.dbtraining.com/errors/recon-failure"))
                .andExpect(jsonPath("$.title").value("Reconciliation failure"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.reconBreakId").value(42))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void joinsAllFieldValidationErrorsInProblemDetail() throws Exception {
        mockMvc.perform(post("/probe/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://reconx.dbtraining.com/errors/validation-failed"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("tradeRef: must not be blank")))
                .andExpect(jsonPath("$.detail").value(containsString("counterpartyId: must not be null")))
                .andExpect(jsonPath("$.detail").value(containsString("; ")))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void mapsConstraintViolationsToBadRequestProblemDetail() throws Exception {
        mockMvc.perform(get("/probe/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://reconx.dbtraining.com/errors/constraint-violation"))
                .andExpect(jsonPath("$.title").value("Constraint violation"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("quantity: must be positive"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void logsUnexpectedErrorsAndReturnsSafeProblemDetail() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            mockMvc.perform(get("/probe/generic"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value("https://reconx.dbtraining.com/errors/internal-server-error"))
                    .andExpect(jsonPath("$.title").value("Internal server error"))
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.detail").value(containsString("An unexpected error occurred")))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty())
                    .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                            .doesNotContain("sensitive stack detail"));

            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getFormattedMessage()).contains("Unhandled exception");
                assertThat(event.getThrowableProxy()).isNotNull();
            });
        } finally {
            logger.detachAppender(appender);
            assertDoesNotThrow(appender::stop);
        }
    }

    @RestController
    @RequestMapping("/probe")
    static class ExceptionProbeController {

        @GetMapping("/not-found")
        public void notFound() {
            throw new TradeNotFoundException("trade 999 was not found");
        }

        @GetMapping("/duplicate")
        public void duplicate() {
            throw new DuplicateTradeRefException("trade reference already exists");
        }

        @GetMapping("/invalid")
        public void invalid() {
            throw new InvalidTradeException("quantity must be positive");
        }

        @GetMapping("/mismatch")
        public void mismatch() {
            throw new ReconciliationMismatchException("counterparty quantity differs", 42L);
        }

        @PostMapping("/validation")
        public void validation(@Valid @RequestBody ValidationPayload payload) {
        }

        @GetMapping("/constraint")
        public void constraint() {
            throw new ConstraintViolationException(
                    "quantity: must be positive", Collections.emptySet());
        }

        @GetMapping("/generic")
        public void generic() {
            throw new IllegalStateException("sensitive stack detail");
        }
    }

    record ValidationPayload(
            @NotBlank String tradeRef,
            @NotNull Long counterpartyId) {
    }
}
