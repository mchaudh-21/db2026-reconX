package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADV064 and ADV067 persistence proof.
 *
 * Uses an isolated H2 database so it cannot collide with the persistent
 * in-memory database used by TradeHistoryIntegrationTest.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tradecrudtest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.liquibase.enabled=true"
})
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(TradeService.class)
class TradeCrudPersistenceIntegrationTest {

    @Autowired
    private TradeService service;

    @Autowired
    private TradeRepository tradeRepo;

    @Autowired
    private CounterpartyRepository cpRepo;

    @Autowired
    private InstrumentRepository instRepo;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private TradeEventProducer events;

    @MockitoBean
    private TradeMetrics metrics;

    @Test
    void softDeleteHidesTradeButKeepsPhysicalRow() {
        Counterparty counterparty = new Counterparty();
        counterparty.setName("Apex Integration");
        counterparty.setLeiCode("APEX-DAY5-000000001");
        counterparty.setRegion("US");
        counterparty = cpRepo.save(counterparty);

        Instrument instrument = new Instrument();
        instrument.setSymbol("D5AAPL");
        instrument.setName("Day 5 Apple");
        instrument.setAssetClass(TradeType.AssetClass.EQUITY);
        instrument.setCurrency("USD");
        instrument.setIsin("US0378331005");
        instrument.setMetadata(Map.of("source", "day5-test"));
        instrument = instRepo.save(instrument);

        TradeRequest request = new TradeRequest(
                "EQU-20260730-9001",
                instrument.getId(),
                counterparty.getId(),
                "EQUITY",
                "BUY",
                new BigDecimal("100.0000"),
                new BigDecimal("245.5000"),
                LocalDate.now()
        );

        Trade created = service.create(request, "trader@db.com");

        entityManager.flush();
        entityManager.clear();

        assertThat(service.list(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        ).getContent())
                .extracting(Trade::getId)
                .contains(created.getId());

        service.softDelete(created.getId(), "admin@db.com");

        entityManager.flush();
        entityManager.clear();

        assertThat(tradeRepo.findById(created.getId())).isEmpty();

        assertThat(service.list(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        )).isEmpty();

        Number physicalRows = (Number) entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM trades
                        WHERE id = :id
                          AND deleted_at IS NOT NULL
                        """)
                .setParameter("id", created.getId())
                .getSingleResult();

        assertThat(physicalRows.intValue()).isEqualTo(1);
    }
}