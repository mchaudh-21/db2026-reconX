package com.dbtraining.reconx.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Local-only H2 data required for the no-Docker Day 6 runtime checks.
 */
@Component
@Profile("local")
public class LocalDemoDataInitializer
        implements ApplicationRunner {

    private static final String TRADER_HASH =
            "$2y$10$LIpkayi0QDWyVguPtEQbkOATM7PTnMKFDNq47K92TUx2LHTBFQf1i";

    private static final String RECON_HASH =
            "$2y$10$UVYhlvPX38zSdPhY6bz4ee453bh6gXp9guhAfV5IU2SjcWPm0eWKq";

    private final JdbcTemplate jdbc;

    public LocalDemoDataInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        insertIfMissing(
                "SELECT COUNT(*) FROM counterparties WHERE id = 1",
                """
                INSERT INTO counterparties (
                    id,
                    name,
                    lei_code,
                    region
                )
                VALUES (
                    1,
                    'Goldman Sachs International',
                    'W22LROWP2IHZNBB6K528',
                    'EMEA'
                )
                """
        );

        insertIfMissing(
                "SELECT COUNT(*) FROM instruments WHERE id = 1",
                """
                INSERT INTO instruments (
                    id,
                    symbol,
                    name,
                    asset_class,
                    currency,
                    isin,
                    metadata
                )
                VALUES (
                    1,
                    'SAP.DE',
                    'SAP SE',
                    'EQUITY',
                    'EUR',
                    'DE0007164600',
                    JSON '{}'
                )
                """
        );

        /*
         * H2 treats a plain '{}' default as a JSON string.
         * JsonBinaryType expects a JSON object for Map<String, Object>.
         * This repairs any locally seeded instrument rows.
         */
        jdbc.update(
                "UPDATE instruments SET metadata = JSON '{}'"
        );

        insertIfMissing(
                """
                SELECT COUNT(*)
                FROM users
                WHERE email = 'trader@db.com'
                """,
                """
                INSERT INTO users (
                    email,
                    password_hash,
                    role,
                    enabled
                )
                VALUES (
                    'trader@db.com',
                    '%s',
                    'TRADER',
                    TRUE
                )
                """.formatted(TRADER_HASH)
        );

        insertIfMissing(
                """
                SELECT COUNT(*)
                FROM users
                WHERE email = 'recon@db.com'
                """,
                """
                INSERT INTO users (
                    email,
                    password_hash,
                    role,
                    enabled
                )
                VALUES (
                    'recon@db.com',
                    '%s',
                    'RECON_ANALYST',
                    TRUE
                )
                """.formatted(RECON_HASH)
        );
    }

    private void insertIfMissing(
            String countSql,
            String insertSql
    ) {
        Long count = jdbc.queryForObject(
                countSql,
                Long.class
        );

        if (count != null && count == 0L) {
            jdbc.update(insertSql);
        }
    }
}