package com.dbtraining.reconx.day4;

import com.dbtraining.reconx.dto.PagedResponse;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import com.dbtraining.reconx.service.TradeHistoryService;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.validation.Validation;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnterpriseFoundationContractTest {

    @Test
    void tradeEntityUsesAuditingLazyRelationsAndStringEnumStatus() throws Exception {
        assertThat(Trade.class.isAnnotationPresent(Audited.class)).isTrue();

        Field instrument = Trade.class.getDeclaredField("instrument");
        Field counterparty = Trade.class.getDeclaredField("counterparty");
        assertThat(instrument.getAnnotation(ManyToOne.class).fetch())
                .isEqualTo(FetchType.LAZY);
        assertThat(counterparty.getAnnotation(ManyToOne.class).fetch())
                .isEqualTo(FetchType.LAZY);

        Field status = Trade.class.getDeclaredField("status");
        assertThat(status.getType()).isEqualTo(TradeStatus.class);
        assertThat(status.getAnnotation(Enumerated.class).value())
                .isEqualTo(EnumType.STRING);
    }

    @Test
    void instrumentMapsMetadataWithJsonBinaryType() throws Exception {
        Field metadata = Instrument.class.getDeclaredField("metadata");
        Type type = metadata.getAnnotation(Type.class);

        assertThat(type).isNotNull();
        assertThat(type.value()).isEqualTo(JsonBinaryType.class);

        Instrument instrument = new Instrument();
        instrument.setAssetClass(TradeType.AssetClass.EQUITY);
        instrument.setMetadata(Map.of(
                "isin", "GB00B16GWD56",
                "cusip", "037833100"
        ));

        assertThat(instrument.getMetadata())
                .containsEntry("isin", "GB00B16GWD56")
                .containsEntry("cusip", "037833100");
    }

    @Test
    void tradeRequestRejectsBlankRefZeroPriceAndFutureDate() {
        TradeRequest invalid = new TradeRequest(
                "",
                1L,
                1L,
                "EQUITY",
                "BUY",
                new BigDecimal("1"),
                BigDecimal.ZERO,
                LocalDate.now().plusDays(1)
        );

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(invalid);
            assertThat(violations)
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("tradeRef", "price", "tradeDate");
        }
    }

    @Test
    void pagedResponseOfMapsPageIntoStableEnvelope() {
        var page = new PageImpl<>(
                List.of("ref-1", "ref-2"),
                PageRequest.of(2, 2),
                9
        );

        PagedResponse<Integer> response =
                PagedResponse.of(page, String::length);

        assertThat(response.items()).containsExactly(5, 5);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(9);
        assertThat(response.totalPages()).isEqualTo(5);
    }

    @Test
    void tradeHistoryMethodsAreReadOnlyTransactions() throws Exception {
        Method revisions = TradeHistoryService.class
                .getMethod("revisionsFor", Long.class);
        Method snapshot = TradeHistoryService.class
                .getMethod("snapshotAt", Long.class, Number.class);

        assertThat(revisions.getAnnotation(Transactional.class).readOnly())
                .isTrue();
        assertThat(snapshot.getAnnotation(Transactional.class).readOnly())
                .isTrue();
    }
}
