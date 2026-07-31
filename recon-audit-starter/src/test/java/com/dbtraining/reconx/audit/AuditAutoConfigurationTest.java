package com.dbtraining.reconx.audit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AuditAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    AuditAutoConfiguration.class
                            )
                    );

    @Test
    void autoConfiguresByDefault() {
        runner.run(context ->
                assertThat(context)
                        .hasSingleBean(AuditEventPublisher.class)
        );
    }

    @Test
    void propertyCanDisableTheStarter() {
        runner
                .withPropertyValues(
                        "reconx.audit.enabled=false"
                )
                .run(context ->
                        assertThat(context)
                                .doesNotHaveBean(
                                        AuditEventPublisher.class
                                )
                );
    }
}
