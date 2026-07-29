package com.dbtraining.reconx.observability;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component("reconxKafka")
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaHealthIndicator extends AbstractHealthIndicator {

    static final int REQUEST_TIMEOUT_MS = 2_000;
    static final int DEFAULT_API_TIMEOUT_MS = 3_000;
    static final int HEALTH_CHECK_TIMEOUT_SECONDS = 2;

    private final String bootstrapServers;
    private final Function<Map<String, Object>, AdminClient> adminClientFactory;

    public KafkaHealthIndicator(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this(bootstrapServers, AdminClient::create);
    }

    KafkaHealthIndicator(
            String bootstrapServers,
            Function<Map<String, Object>, AdminClient> adminClientFactory) {
        super("ReconX Kafka health check failed");
        this.bootstrapServers = bootstrapServers;
        this.adminClientFactory = adminClientFactory;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Map<String, Object> config = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, REQUEST_TIMEOUT_MS,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, DEFAULT_API_TIMEOUT_MS);

        try (AdminClient adminClient = adminClientFactory.apply(config)) {
            DescribeClusterResult cluster = adminClient.describeCluster();
            String clusterId = cluster.clusterId()
                    .get(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            int nodeCount = cluster.nodes()
                    .get(HEALTH_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .size();

            builder.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodeCount);
        } catch (Exception exception) {
            builder.down(exception);
        }
    }
}
