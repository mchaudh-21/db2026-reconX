package com.dbtraining.reconx.day6;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Day 6 application-context proof.
 *
 * Uses its own H2 database so it cannot collide with the persistent
 * in-memory database used by earlier integration tests.
 */
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.datasource.url=jdbc:h2:mem:day6context;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON"
})
@ActiveProfiles("dev")
class ApplicationContextSmokeTest {

    @Test
    void contextLoads() {
    }
}