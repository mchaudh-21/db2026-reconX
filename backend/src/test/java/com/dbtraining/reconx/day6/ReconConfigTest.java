package com.dbtraining.reconx.day6;

import com.dbtraining.reconx.config.ReconConfig;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReconConfigTest {

    @Test
    void changesToleranceAndClearsCaches() {
        ConcurrentMapCacheManager manager =
                new ConcurrentMapCacheManager(
                        "instruments",
                        "counterparties"
                );

        manager.getCache("instruments")
                .put("SAP.DE", "cached");

        ReconConfig config =
                new ReconConfig(manager, 0.01);

        config.setPriceTolerance(0.02);

        assertThat(config.getPriceTolerance())
                .isEqualTo(0.02);

        assertThat(config.hasPriceToleranceOverride())
                .isTrue();

        config.clearCache();

        assertThat(manager.getCache("instruments")
                .get("SAP.DE"))
                .isNull();

        assertThatThrownBy(() ->
                config.setPriceTolerance(1.01)
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
