package com.dbtraining.reconx.day6;

import com.dbtraining.reconx.config.ObservabilityConfig;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.service.ReconciliationEngine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationTimerTest {

    @Test
    void timedAspectRecordsEngineInvocation() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(
                             TestConfig.class
                     )) {
            ReconciliationEngine engine =
                    context.getBean(
                            ReconciliationEngine.class
                    );

            MeterRegistry registry =
                    context.getBean(MeterRegistry.class);

            engine.reconcile(
                    List.of(),
                    List.of(),
                    ReconciliationRule.EXACT
            );

            assertThat(
                    registry.get(
                            "reconciliation.duration"
                    )
                            .timer()
                            .count()
            ).isEqualTo(1L);
        }
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @Import(ObservabilityConfig.class)
    static class TestConfig {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        ReconciliationEngine reconciliationEngine() {
            return new ReconciliationEngine();
        }
    }
}
