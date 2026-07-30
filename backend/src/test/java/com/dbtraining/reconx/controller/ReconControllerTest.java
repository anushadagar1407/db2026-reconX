package com.dbtraining.reconx.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dbtraining.reconx.exception.GlobalExceptionHandler;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReconControllerTest {

    private static final String VALIDATION_TYPE =
            "https://reconx.dbtraining.com/errors/validation-failed";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ReconController(mock(ReconBreakRepository.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void queuesValidRequestWithUuidAndResultsLocation() throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/recon/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2026-03-01\",\"to\":\"2026-03-31\",\"counterpartyId\":42}"))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID jobId = UUID.fromString(body.path("jobId").asText());

        assertThat(body.path("status").asText()).isEqualTo("QUEUED");
        assertThat(result.getResponse().getHeader("Location"))
                .isEqualTo("/api/v1/recon/jobs/" + jobId + "/results");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"to\":\"2026-03-31\"}",
            "{\"from\":\"2026-03-01\"}",
            "{\"from\":\"not-a-date\",\"to\":\"2026-03-31\"}",
            "{\"from\":\"2026-03-01\",\"to\":\"not-a-date\"}"
    })
    void rejectsMissingOrMalformedDatesWithValidationProblem(String requestBody) throws Exception {
        mockMvc.perform(post("/v1/recon/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(VALIDATION_TYPE))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsInvertedDateRangeWithValidationProblem() throws Exception {
        mockMvc.perform(post("/v1/recon/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2026-04-01\",\"to\":\"2026-03-31\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(VALIDATION_TYPE))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("from must be on or before to")));
    }
}
