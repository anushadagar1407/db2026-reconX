package com.dbtraining.reconx.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = OpenApiConfigTest.TestApplication.class,
        properties = {
                "server.servlet.context-path=/api"
        })
@AutoConfigureMockMvc(addFilters = false)
class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiMetadataIncludesContactAndBearerJwtScheme() {
        OpenAPI openApi = config.reconxOpenAPI();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("ReconX API");
        assertThat(openApi.getInfo().getDescription()).isEqualTo("Trade reconciliation platform — DB TDI 2026");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("v1.0.0");
        assertThat(openApi.getInfo().getContact().getName()).isEqualTo("ReconX Team");
        assertThat(openApi.getInfo().getContact().getEmail()).isEqualTo("reconx-team@dbtraining.com");

        SecurityScheme bearerAuth = openApi.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(bearerAuth.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(bearerAuth.getScheme()).isEqualTo("bearer");
        assertThat(bearerAuth.getBearerFormat()).isEqualTo("JWT");
        assertThat(openApi.getSecurity()).singleElement()
                .satisfies(requirement -> assertThat(requirement).containsKey("bearerAuth"));
    }

    @Test
    void groupsUseExactAudienceNamesAndPathPatterns() {
        assertThat(config.publicApi().getGroup()).isEqualTo("public");
        assertThat(config.publicApi().getPathsToMatch())
                .containsExactly("/v1/trades/**", "/v1/recon/**");

        assertThat(config.adminApi().getGroup()).isEqualTo("admin");
        assertThat(config.adminApi().getPathsToMatch())
                .containsExactly("/v1/admin/**", "/actuator/**");
    }

    @Test
    void publicDocsContainOnlyPublicPaths() throws Exception {
        mockMvc.perform(get("/api/v1/api-docs/public").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.info.title").value("ReconX API"))
                .andExpect(jsonPath("$.paths['/v1/trades/example']").exists())
                .andExpect(jsonPath("$.paths['/v1/recon/example']").exists())
                .andExpect(jsonPath("$.paths['/v1/admin/example']").doesNotExist())
                .andExpect(jsonPath("$.paths['/actuator/example']").doesNotExist());
    }

    @Test
    void adminDocsContainOnlyAdminPaths() throws Exception {
        mockMvc.perform(get("/api/v1/api-docs/admin").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/v1/admin/example']").exists())
                .andExpect(jsonPath("$.paths['/actuator/example']").exists())
                .andExpect(jsonPath("$.paths['/v1/trades/example']").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/recon/example']").doesNotExist());
    }

    @Test
    void swaggerUiLoadsThroughTheApiContextPath() throws Exception {
        mockMvc.perform(get("/api/swagger-ui.html").contextPath("/api"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/api/swagger-ui/index.html"));

        mockMvc.perform(get("/api/swagger-ui/index.html").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            KafkaAutoConfiguration.class,
            LiquibaseAutoConfiguration.class
    })
    @Import({OpenApiConfig.class, PublicEndpoints.class, AdminEndpoints.class})
    static class TestApplication {
    }

    @RestController
    static class PublicEndpoints {

        @GetMapping("/v1/trades/example")
        String trade() {
            return "trade";
        }

        @GetMapping("/v1/recon/example")
        String recon() {
            return "recon";
        }
    }

    @RestController
    static class AdminEndpoints {

        @GetMapping("/v1/admin/example")
        String admin() {
            return "admin";
        }

        @GetMapping("/actuator/example")
        String actuator() {
            return "actuator";
        }
    }
}
