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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class LiquibaseMigrationsIT {

    private static final List<String> REQUIRED_POSTGRES_CHANGESETS = List.of(
            "009-create-envers-revision-info",
            "009-create-trades-aud",
            "010-create-envers-revision-sequence",
            "011-create-recon-trade-inputs",
            "011-create-recon-results",
            "012-add-trade-deleted-at");

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

        List<String> appliedRequiredChangesets = jdbc.queryForList("""
                SELECT id
                FROM databasechangelog
                WHERE id IN (?, ?, ?, ?, ?, ?)
                ORDER BY orderexecuted
                """, String.class, REQUIRED_POSTGRES_CHANGESETS.toArray());
        assertThat(appliedRequiredChangesets)
                .containsExactlyElementsOf(REQUIRED_POSTGRES_CHANGESETS);

        List<String> enversTables = jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = current_schema()
                  AND table_name IN ('revinfo', 'trades_aud', 'recon_trade_inputs', 'recon_results')
                """, String.class);
        assertThat(enversTables).containsExactlyInAnyOrder(
                "revinfo", "trades_aud", "recon_trade_inputs", "recon_results");

        Integer revisionSequences = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.sequences
                WHERE sequence_schema = current_schema()
                  AND sequence_name = 'revinfo_seq'
                """, Integer.class);
        assertThat(revisionSequences).isEqualTo(1);

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
