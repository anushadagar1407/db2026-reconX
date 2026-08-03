package com.dbtraining.reconx;

import com.dbtraining.reconx.config.CacheConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the ReconX trade reconciliation service.
 *
 * <p>Activated capabilities:
 * <ul>
 *   <li>{@link EnableJpaAuditing} — ADV050 @CreatedDate / @LastModifiedDate population.</li>
 *   <li>{@link CacheConfig}      — ADV081 caching for instrument lookups.</li>
 *   <li>{@link EnableKafka}      — ADV128–ADV133 Kafka producers and @KafkaListener consumers.</li>
 *   <li>{@link EnableAsync}      — ADV037 CompletableFuture-based parallel reconciliation.</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableKafka
@EnableAsync
public class ReconxApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconxApplication.class, args);
    }
}
