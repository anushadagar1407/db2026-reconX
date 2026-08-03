package com.dbtraining.reconx.integration;

import com.dbtraining.reconx.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class LiquibaseMigrationsIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void freshPostgresAppliesLiquibaseChangesAndSeedData() {
        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(13);

        Integer nonDeletedTrades = jdbc.queryForObject(
                "SELECT COUNT(*) FROM trades WHERE deleted_at IS NULL", Integer.class);
        assertThat(nonDeletedTrades).isGreaterThanOrEqualTo(10);
    }
}
