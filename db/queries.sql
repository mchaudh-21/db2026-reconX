-- ============================================================================
-- TICKET-ADV010 — VWAP per instrument per day (window function)
-- ============================================================================
SELECT DISTINCT
    t.instrument_id,
    t.trade_date,
    SUM(t.price * t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date)
        / NULLIF(SUM(t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date), 0)
        AS vwap
FROM trades t
WHERE t.deleted_at IS NULL
  AND t.asset_class = 'EQUITY'
ORDER BY t.trade_date DESC, t.instrument_id;


-- ============================================================================
-- TICKET-ADV011 — Recursive CTE: trade lifecycle rollup
-- ============================================================================
WITH RECURSIVE trade_lifecycle AS (

    -- Base case
    SELECT
        t.id AS trade_id,
        t.trade_ref,
        1 AS stage,
        'EXECUTION'::VARCHAR AS stage_name,
        t.created_at AS event_at,
        'EXECUTED'::VARCHAR AS event_status
    FROM trades t
    WHERE t.deleted_at IS NULL

    UNION ALL

    -- Recursive step
    SELECT
        tl.trade_id,
        tl.trade_ref,
        tl.stage + 1,
        CASE tl.stage + 1
            WHEN 2 THEN 'CONFIRMATION'
            WHEN 3 THEN 'SETTLEMENT'
            WHEN 4 THEN 'RECON_BREAK'
            WHEN 5 THEN 'RESOLUTION'
        END,
        tl.event_at + INTERVAL '1 hour',
        CASE tl.stage + 1
            WHEN 2 THEN 'CONFIRMED'
            WHEN 3 THEN 'SETTLED'
            WHEN 4 THEN 'OPEN'
            WHEN 5 THEN 'RESOLVED'
        END
    FROM trade_lifecycle tl
    WHERE tl.stage < 5
)

SELECT
    trade_id,
    trade_ref,
    stage,
    stage_name,
    event_at,
    event_status
FROM trade_lifecycle
ORDER BY trade_id, stage;

-- ============================================================================
-- ADV008 — REFRESH the daily-summary materialised view (concurrent so it can
--          run while the dashboard is reading it)
-- ============================================================================
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_recon_summary;

-- ============================================================================
-- ADV009 — JSONB lookup: which instruments have sector = 'Banking'?
-- ============================================================================
SELECT
    id,
    symbol,
    metadata
FROM instruments
WHERE metadata @> '{"sector":"Banking"}'::jsonb;