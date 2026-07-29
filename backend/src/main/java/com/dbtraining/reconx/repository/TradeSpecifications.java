package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * TICKET-ADV056 — composable filters for trade queries.
 */
public final class TradeSpecifications {

    private TradeSpecifications() {
    }

    public static Specification<Trade> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return cb.conjunction();
            }

            TradeStatus tradeStatus =
                    TradeStatus.valueOf(status.trim().toUpperCase());

            return cb.equal(root.get("status"), tradeStatus);
        };
    }

    public static Specification<Trade> tradeDateBetween(
            LocalDate from,
            LocalDate to
    ) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            if (from == null) {
                return cb.lessThanOrEqualTo(root.get("tradeDate"), to);
            }
            if (to == null) {
                return cb.greaterThanOrEqualTo(root.get("tradeDate"), from);
            }
            return cb.between(root.get("tradeDate"), from, to);
        };
    }

    public static Specification<Trade> hasCounterparty(Long counterpartyId) {
        return (root, query, cb) -> counterpartyId == null
                ? cb.conjunction()
                : cb.equal(
                        root.get("counterparty").get("id"),
                        counterpartyId
                );
    }
}
