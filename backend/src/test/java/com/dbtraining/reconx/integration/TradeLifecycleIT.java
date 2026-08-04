package com.dbtraining.reconx.integration;

import com.dbtraining.reconx.support.PostgresTestConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TradeLifecycleIT {

    private static KafkaContainer kafkaContainer;

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        String externalBootstrapServers = System.getenv("TEST_KAFKA_BOOTSTRAP_SERVERS");
        if (externalBootstrapServers != null && !externalBootstrapServers.isBlank()) {
            registry.add("spring.kafka.bootstrap-servers", () -> externalBootstrapServers);
            return;
        }

        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
                .withEmbeddedZookeeper();
        kafkaContainer.start();
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @AfterAll
    static void stopKafkaContainer() {
        if (kafkaContainer != null) {
            kafkaContainer.stop();
        }
    }

    @Autowired
    private TestRestTemplate http;

    private static String token;
    private static Long createdId;
    private static String reconJobId;
    private static Long breakId;

    @Test
    @Order(1)
    void loginAsAdmin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var response = http.postForEntity(
                "/auth/login",
                new HttpEntity<>("""
                        {"username":"admin@db.com","password":"admin123"}
                        """, headers),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        token = response.getBody().path("token").asText();
        assertThat(token).isNotBlank();
    }

    @Test
    @Order(2)
    void createTrade() {
        var response = http.exchange(
                "/v1/trades",
                HttpMethod.POST,
                authenticatedRequest("""
                        {
                          "tradeRef":"INT-20260315-0001",
                          "instrumentId":1,
                          "counterpartyId":1,
                          "assetClass":"EQUITY",
                          "side":"BUY",
                          "quantity":100.0,
                          "price":245.50,
                          "tradeDate":"2026-03-15"
                        }
                        """),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        createdId = response.getBody().path("id").asLong();
        assertThat(createdId).isPositive();
    }

    @Test
    @Order(3)
    void getTradeBack() {
        var response = http.exchange(
                "/v1/trades?status=PENDING",
                HttpMethod.GET,
                authenticatedRequest(),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("totalElements").asLong()).isPositive();
    }

    @Test
    @Order(4)
    void patchStatus() {
        var response = http.exchange(
                "/v1/trades/{id}/status",
                HttpMethod.PATCH,
                authenticatedRequest("""
                        {"status":"MATCHED"}
                        """),
                JsonNode.class,
                createdId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("status").asText()).isEqualTo("MATCHED");
    }

    @Test
    @Order(5)
    void triggerRecon() {
        var response = http.exchange(
                "/v1/recon/run",
                HttpMethod.POST,
                authenticatedRequest("""
                        {"from":"2026-03-01","to":"2026-03-31"}
                        """),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        reconJobId = response.getBody().path("jobId").asText();
        assertThat(reconJobId).isNotBlank();
    }

    @Test
    @Order(6)
    void resolveBreak() {
        breakId = 1L;
        var response = http.exchange(
                "/v1/recon/results/{id}/resolve",
                HttpMethod.PUT,
                authenticatedRequest("""
                        {"note":"Confirmed via counterparty email on 2026-03-16."}
                        """),
                JsonNode.class,
                breakId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("status").asText()).isEqualTo("RESOLVED");
    }

    private HttpEntity<Void> authenticatedRequest() {
        return new HttpEntity<>(authenticatedHeaders());
    }

    private HttpEntity<String> authenticatedRequest(String body) {
        return new HttpEntity<>(body, authenticatedHeaders());
    }

    private HttpHeaders authenticatedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}
