package com.dbtraining.reconx.observability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLTimeoutException;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DatabaseHealthIndicatorTest {

    private static final String QUERY = "SELECT 1";

    private DataSource dataSource;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(Statement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(QUERY)).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
    }

    @Test
    void registersWithTheReconXDatabaseActuatorName() {
        Component component = DatabaseHealthIndicator.class.getAnnotation(Component.class);

        assertThat(component).isNotNull();
        assertThat(component.value()).isEqualTo("reconxDatabase");
    }

    @Test
    void successfulQueryReportsQueryElapsedTimeAndTimeout() throws SQLException {
        Health health = newIndicator().health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("query", QUERY);
        Object elapsedMs = health.getDetails().get("elapsedMs");
        assertThat(elapsedMs).isInstanceOf(Long.class);
        assertThat((Long) elapsedMs).isGreaterThanOrEqualTo(0L);
        verify(statement).setQueryTimeout(2);
        verify(statement).executeQuery(QUERY);
        verify(resultSet).next();
    }

    @Test
    void successfulQueryClosesEveryJdbcResource() throws SQLException {
        newIndicator().health();

        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void queryTimeoutReportsDownWithQueryAndClosesOpenedResources() throws SQLException {
        SQLTimeoutException timeout = new SQLTimeoutException("statement timed out");
        when(statement.executeQuery(QUERY)).thenThrow(timeout);

        Health health = newIndicator().health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("query", QUERY)
                .containsEntry("error", timeout.toString());
        verify(statement).setQueryTimeout(2);
        verify(statement).executeQuery(QUERY);
        verify(statement).close();
        verify(connection).close();
        verifyNoInteractions(resultSet);
    }

    @Test
    void connectionFailureReportsDownWithQuery() throws SQLException {
        SQLException failure = new SQLException("database unavailable");
        when(dataSource.getConnection()).thenThrow(failure);

        Health health = newIndicator().health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("query", QUERY)
                .containsEntry("error", failure.toString());
        verify(dataSource).getConnection();
        verifyNoInteractions(connection, statement, resultSet);
    }

    private DatabaseHealthIndicator newIndicator() {
        return new DatabaseHealthIndicator(dataSource);
    }
}
