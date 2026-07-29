package com.dbtraining.reconx.repository.entity;

import com.dbtraining.reconx.model.TradeType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * TICKET-ADV051 — instrument entity with portable JSONB metadata mapping.
 */
@Entity
@Table(name = "instruments")
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 20)
    private TradeType.AssetClass assetClass;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 12)
    private String isin;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    public Instrument() {
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public TradeType.AssetClass getAssetClass() {
        return assetClass;
    }

    public String getCurrency() {
        return currency;
    }

    public String getIsin() {
        return isin;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setSymbol(String value) {
        this.symbol = value;
    }

    public void setName(String value) {
        this.name = value;
    }

    public void setAssetClass(TradeType.AssetClass value) {
        this.assetClass = Objects.requireNonNull(value, "assetClass");
    }

    public void setAssetClass(String value) {
        setAssetClass(TradeType.AssetClass.valueOf(
                Objects.requireNonNull(value, "assetClass").trim().toUpperCase()
        ));
    }

    public void setCurrency(String value) {
        this.currency = value;
    }

    public void setIsin(String value) {
        this.isin = value;
    }

    public void setMetadata(Map<String, Object> value) {
        this.metadata = value == null ? new HashMap<>() : new HashMap<>(value);
    }
}
