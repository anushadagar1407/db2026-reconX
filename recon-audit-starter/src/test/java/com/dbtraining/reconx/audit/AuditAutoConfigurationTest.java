package com.dbtraining.reconx.audit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AuditAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class));

    @Test
    void configuresPublisherByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AuditEventPublisher.class);
            assertThat(context).hasSingleBean(AuditProperties.class);
            assertThat(context.getBean(AuditProperties.class).isEnabled()).isTrue();
            assertThat(context.getBean(AuditProperties.class).getTopic()).isEqualTo("audit-events");
        });
    }

    @Test
    void disablesPublisherWhenPropertyIsFalse() {
        contextRunner
                .withPropertyValues("reconx.audit.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AuditEventPublisher.class));
    }

    @Test
    void backsOffWhenConsumerProvidesPublisher() {
        AuditEventPublisher customPublisher = new AuditEventPublisher(event -> { }, new AuditProperties());

        contextRunner
                .withBean(AuditEventPublisher.class, () -> customPublisher)
                .run(context -> assertThat(context.getBean(AuditEventPublisher.class))
                        .isSameAs(customPublisher));
    }

    @Test
    void registersBootThreeImportsMetadata() throws IOException {
        String resourceName = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";
        try (InputStream imports = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertThat(imports).isNotNull();
            assertThat(new String(imports.readAllBytes(), StandardCharsets.UTF_8))
                    .contains(AuditAutoConfiguration.class.getName());
        }
    }
}
