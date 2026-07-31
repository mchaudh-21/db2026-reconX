package com.dbtraining.reconx.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

/**
 * ADV096 — JMX controls under reconx:type=ReconConfig.
 */
@Component
@ManagedResource(
        objectName = "reconx:type=ReconConfig",
        description = "Runtime ReconX controls"
)
public class ReconConfig {

    private final CacheManager cacheManager;
    private volatile double priceTolerance;
    private volatile boolean priceToleranceOverridden;
    private volatile boolean cachingEnabled = true;

    public ReconConfig(
            CacheManager cacheManager,
            @Value("${reconx.reconciliation.price-tolerance:0.01}")
            double priceTolerance
    ) {
        this.cacheManager = cacheManager;
        this.priceTolerance = validate(priceTolerance);
    }

    @ManagedAttribute(description = "Price tolerance between 0.0 and 1.0")
    public double getPriceTolerance() {
        return priceTolerance;
    }

    @ManagedAttribute
    public void setPriceTolerance(double priceTolerance) {
        this.priceTolerance = validate(priceTolerance);
        this.priceToleranceOverridden = true;
    }

    @ManagedAttribute(description = "Whether reference-data caching is active")
    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    @ManagedAttribute
    public void setCachingEnabled(boolean cachingEnabled) {
        this.cachingEnabled = cachingEnabled;
        if (!cachingEnabled) {
            clearCache();
        }
    }

    @ManagedOperation(description = "Clear every configured cache")
    public void clearCache() {
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    public boolean hasPriceToleranceOverride() {
        return priceToleranceOverridden;
    }

    private static double validate(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                    "priceTolerance must be between 0.0 and 1.0"
            );
        }
        return value;
    }
}
