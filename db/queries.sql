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
    t.price * t.quantity AS notional,
    SUM(t.price * t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date)
        / NULLIF(SUM(t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date), 0)
            AS vwap
FROM trades t
JOIN instruments i ON i.id = t.instrument_id
WHERE t.deleted_at IS NULL
  AND t.asset_class = 'EQUITY'
ORDER BY t.trade_date DESC, t.instrument_id;


-- ============================================================================
-- TICKET-ADV011 — Recursive CTE: trade lifecycle (execution -> settlement
--                -> recon_break -> resolution)
-- ============================================================================
WITH RECURSIVE trade_lifecycle AS (
    -- Anchor: every trade starts at execution.
    SELECT
        t.id           AS trade_id,
        t.trade_ref,
        t.trade_date,
        1              AS stage,
        'EXECUTION'    AS stage_name,
        t.created_at   AS event_at,
        t.status       AS event_status
    FROM trades t
    WHERE t.deleted_at IS NULL

    UNION ALL

    -- Each branch emits only the stage following the current one.
    SELECT
        tl.trade_id,
        tl.trade_ref,
        tl.trade_date,
        tl.stage + 1,
        next_event.stage_name,
        next_event.event_at,
        next_event.event_status
    FROM trade_lifecycle tl
    JOIN LATERAL (
        SELECT
            'CONFIRMATION'::text AS stage_name,
            COALESCE(t.modified_at, t.created_at) AS event_at,
            t.status::text AS event_status
        FROM trades t
        WHERE tl.stage = 1
          AND t.id = tl.trade_id
          AND t.trade_date = tl.trade_date

        UNION ALL

        SELECT
            'SETTLEMENT',
            s.settlement_date::timestamp,
            s.status::text
        FROM settlements s
        WHERE tl.stage = 2
          AND s.trade_id = tl.trade_id
          AND s.trade_date = tl.trade_date

        UNION ALL

        SELECT
            'RECON_BREAK',
            rb.detected_at,
            rb.status::text
        FROM recon_breaks rb
        WHERE tl.stage = 3
          AND rb.trade_id = tl.trade_id
          AND rb.trade_date = tl.trade_date

        UNION ALL

        SELECT
            'RESOLUTION',
            rb.resolved_at,
            rb.status::text
        FROM recon_breaks rb
        WHERE tl.stage = 4
          AND rb.trade_id = tl.trade_id
          AND rb.trade_date = tl.trade_date
          AND rb.resolved_at IS NOT NULL
    ) next_event ON TRUE
    WHERE tl.stage < 5
)
SELECT trade_id, trade_ref, stage, stage_name, event_at, event_status
FROM trade_lifecycle
ORDER BY trade_id, stage;


-- ============================================================================
-- ADV008 — REFRESH the daily-summary materialised view (concurrent so it can
--         run while the dashboard is reading it)
-- ============================================================================
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_recon_summary;


-- ============================================================================
-- ADV009 — JSONB lookup: which instruments have sector = 'Banking'?
-- ============================================================================
SELECT id, symbol, metadata
FROM instruments
WHERE metadata @> '{"sector":"Banking"}'::jsonb;
