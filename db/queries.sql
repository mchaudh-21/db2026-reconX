-- ============================================================================
-- TICKET-ADV010 — VWAP per instrument per day (window function)
-- ============================================================================
SELECT
    t.trade_ref,
    t.instrument_id,
    t.trade_date,
    i.symbol,
    t.quantity,
    t.price,
    t.quantity * t.price AS notional,

    SUM(t.price * t.quantity) OVER (
        PARTITION BY t.instrument_id, t.trade_date
    )
    / NULLIF(
        SUM(t.quantity) OVER (
            PARTITION BY t.instrument_id, t.trade_date
        ),
        0
    ) AS vwap,

    ROW_NUMBER() OVER (
        PARTITION BY t.instrument_id, t.trade_date
        ORDER BY t.created_at
    ) AS trade_row_number,

    SUM(t.quantity) OVER (
        PARTITION BY t.instrument_id, t.trade_date
        ORDER BY t.created_at
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS cumulative_quantity

FROM trades t
JOIN instruments i
    ON i.id = t.instrument_id
WHERE t.deleted_at IS NULL
  AND t.asset_class = 'EQUITY'
ORDER BY
    t.trade_date DESC,
    t.instrument_id,
    t.created_at;

-- ============================================================================
-- ADV008 — REFRESH the daily-summary materialised view
-- ============================================================================
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_recon_summary;

-- ============================================================================
-- ADV009 — JSONB lookup: which instruments have sector = 'Banking'?
-- ============================================================================
SELECT id, symbol, metadata
FROM instruments
WHERE metadata @> '{"sector":"Banking"}'::jsonb;