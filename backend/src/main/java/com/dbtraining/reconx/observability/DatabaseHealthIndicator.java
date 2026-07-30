package com.dbtraining.reconx.observability;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

@Component("reconxDatabase")
public class DatabaseHealthIndicator extends AbstractHealthIndicator {

    private static final String QUERY = "SELECT 1";
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        super("ReconX database health check failed");
        this.dataSource = dataSource;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        long start = System.nanoTime();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout((int) TIMEOUT.toSeconds());
            try (ResultSet resultSet = statement.executeQuery(QUERY)) {
                resultSet.next();
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            builder.up()
                    .withDetail("query", QUERY)
                    .withDetail("elapsedMs", elapsedMs);
        } catch (SQLException exception) {
            builder.down(exception)
                    .withDetail("query", QUERY);
        }
    }
}
