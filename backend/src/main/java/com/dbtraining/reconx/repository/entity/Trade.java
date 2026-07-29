package com.dbtraining.reconx.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * TICKET-ADV050 / ADV052 — persistent, audited trade entity.
 */
@Entity
@Table(
        name = "trades",
        indexes = {
                @Index(name = "idx_trades_trade_date", columnList = "trade_date"),
                @Index(name = "idx_trades_status", columnList = "status")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Audited
@SQLRestriction("deleted_at IS NULL")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_ref", nullable = false, unique = true, length = 30)
    private String tradeRef;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counterparty_id", nullable = false)
    private Counterparty counterparty;

    @Column(name = "asset_class", nullable = false, length = 20)
    private String assetClass;

    @Column(nullable = false, length = 4)
    private String side;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradeStatus status = TradeStatus.PENDING;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "modified_at")
    private Instant modifiedAt;

    public Trade() {
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.status = TradeStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public String getTradeRef() {
        return tradeRef;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public Counterparty getCounterparty() {
        return counterparty;
    }

    public String getAssetClass() {
        return assetClass;
    }

    public String getSide() {
        return side;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public TradeStatus getStatus() {
        return status;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public void setTradeRef(String value) {
        this.tradeRef = value;
    }

    public void setInstrument(Instrument value) {
        this.instrument = value;
    }

    public void setCounterparty(Counterparty value) {
        this.counterparty = value;
    }

    public void setAssetClass(String value) {
        this.assetClass = value;
    }

    public void setSide(String value) {
        this.side = value;
    }

    public void setQuantity(BigDecimal value) {
        this.quantity = value;
    }

    public void setPrice(BigDecimal value) {
        this.price = value;
    }

    public void setTradeDate(LocalDate value) {
        this.tradeDate = value;
    }

    public void setStatus(TradeStatus value) {
        this.status = Objects.requireNonNull(value, "status");
    }

    /**
     * Compatibility boundary for existing controller/service code that still
     * receives status as a String.
     */
    public void setStatus(String value) {
        setStatus(TradeStatus.valueOf(
                Objects.requireNonNull(value, "status").trim().toUpperCase()
        ));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Trade trade)) {
            return false;
        }
        return id != null && id.equals(trade.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
