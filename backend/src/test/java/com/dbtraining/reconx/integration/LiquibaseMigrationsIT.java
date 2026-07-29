package com.dbtraining.reconx.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class LiquibaseMigrationsIT {

    private static final int MINIMUM_POSTGRES_CHANGESETS = 23;

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reconx_fresh")
            .withUsername("reconx_test")
            .withPassword("reconx_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void freshPostgresAppliesLiquibaseChangesAndSeedData() {
        assertThat(jdbc.queryForObject("SELECT version()", String.class))
                .contains("PostgreSQL");

        Integer appliedChangesets = jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog", Integer.class);
        assertThat(appliedChangesets).isGreaterThanOrEqualTo(MINIMUM_POSTGRES_CHANGESETS);

        Integer softDeleteChangesets = jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE id = ?", Integer.class,
                "009-add-trade-deleted-at");
        assertThat(softDeleteChangesets).isEqualTo(1);

        Integer nonDeletedTrades = jdbc.queryForObject(
                "SELECT COUNT(*) FROM trades WHERE deleted_at IS NULL", Integer.class);
        assertThat(nonDeletedTrades).isGreaterThanOrEqualTo(10);

        Integer deletedAtColumns = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = 'trades'
                  AND column_name = 'deleted_at'
                """, Integer.class);
        assertThat(deletedAtColumns).isEqualTo(1);
    }
}
