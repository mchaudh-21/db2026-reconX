package com.dbtraining.reconx.day4;

import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.service.TradeHistoryService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV052 — proves insert + three committed updates create four revisions.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.liquibase.enabled=true"
})
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TradeHistoryService.class)
class TradeHistoryIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TradeHistoryService historyService;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void insertAndThreeUpdatesProduceFourRevisions() {
        TransactionTemplate transactions =
                new TransactionTemplate(transactionManager);

        Long tradeId = transactions.execute(status -> {
            Counterparty counterparty = new Counterparty();
            counterparty.setName("Day 4 Counterparty");
            counterparty.setLeiCode("DAY4-LEI-0000000001");
            counterparty.setRegion("US");
            entityManager.persist(counterparty);

            Instrument instrument = new Instrument();
            instrument.setSymbol("D4TST");
            instrument.setName("Day 4 Test Instrument");
            instrument.setAssetClass(TradeType.AssetClass.EQUITY);
            instrument.setCurrency("USD");
            instrument.setMetadata(java.util.Map.of("isin", "US0000000001"));
            entityManager.persist(instrument);

            Trade trade = new Trade();
            trade.setTradeRef("EQU-20260729-9001");
            trade.setCounterparty(counterparty);
            trade.setInstrument(instrument);
            trade.setAssetClass("EQUITY");
            trade.setSide("BUY");
            trade.setQuantity(new BigDecimal("10.0000"));
            trade.setPrice(new BigDecimal("100.0000"));
            trade.setTradeDate(LocalDate.of(2026, 7, 29));
            entityManager.persist(trade);
            entityManager.flush();

            return trade.getId();
        });

        assertThat(tradeId).isNotNull();

        for (int update = 1; update <= 3; update++) {
            BigDecimal newPrice = new BigDecimal("100.0000")
                    .add(BigDecimal.valueOf(update));

            transactions.executeWithoutResult(status -> {
                Trade trade = entityManager.find(Trade.class, tradeId);
                trade.setPrice(newPrice);
                entityManager.flush();
            });
        }

        List<Number> revisions = transactions.execute(
                status -> historyService.revisionsFor(tradeId)
        );

        assertThat(revisions).hasSize(4);

        for (Number revision : revisions) {
            Trade snapshot = transactions.execute(
                    status -> historyService.snapshotAt(tradeId, revision)
            );
            assertThat(snapshot).isNotNull();
            assertThat(snapshot.getTradeRef())
                    .isEqualTo("EQU-20260729-9001");
        }
    }
}
