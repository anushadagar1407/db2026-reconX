package com.dbtraining.reconx.observability;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaHealthIndicatorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(IndicatorConfiguration.class);

    @Test
    void indicatorIsNotRegisteredWithoutBootstrapServers() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean("reconxKafka"));
    }

    @Test
    void indicatorIsRegisteredWithBootstrapServers() {
        contextRunner
                .withPropertyValues("spring.kafka.bootstrap-servers=localhost:9092")
                .run(context -> assertThat(context).hasSingleBean(KafkaHealthIndicator.class));
    }

    @Test
    void healthyClusterReportsClusterDetailsAndConfiguredTimeouts() {
        AdminClient adminClient = mock(AdminClient.class);
        DescribeClusterResult cluster = mock(DescribeClusterResult.class);
        when(adminClient.describeCluster()).thenReturn(cluster);
        when(cluster.clusterId()).thenReturn(KafkaFuture.completedFuture("reconx-cluster"));
        when(cluster.nodes()).thenReturn(KafkaFuture.completedFuture(List.of(
                new Node(1, "broker-1", 9092),
                new Node(2, "broker-2", 9092))));

        AtomicReference<Map<String, Object>> capturedConfig = new AtomicReference<>();
        KafkaHealthIndicator indicator = new KafkaHealthIndicator("broker-1:9092", config -> {
            capturedConfig.set(config);
            return adminClient;
        });

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("clusterId", "reconx-cluster")
                .containsEntry("nodeCount", 2);
        assertThat(capturedConfig).hasValueSatisfying(config -> assertThat(config)
                .containsEntry("bootstrap.servers", "broker-1:9092")
                .containsEntry("request.timeout.ms", 2_000)
                .containsEntry("default.api.timeout.ms", 3_000));
        verify(adminClient).describeCluster();
        verify(adminClient).close();
    }

    @Test
    void failedClusterCheckReportsDown() {
        RuntimeException failure = new RuntimeException("broker unavailable");
        KafkaHealthIndicator indicator = new KafkaHealthIndicator(
                "broker-1:9092", config -> {
                    throw failure;
                });

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import(KafkaHealthIndicator.class)
    static class IndicatorConfiguration {
    }
}
