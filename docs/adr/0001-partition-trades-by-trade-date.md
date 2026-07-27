# ADR 0001: Partition Trades by Trade Date

## Status

Accepted

## Context

ReconX is expected to process approximately 50,000 trades per day and may
retain up to 91 million trade records. Most reconciliation, audit, and
end-of-day reporting queries filter trades by a date or date range.

Keeping all trades in one unpartitioned table would cause increasingly large
table and index scans. We considered an unpartitioned table, partitioning by
counterparty, partitioning by instrument, and range partitioning by trade date.

Counterparty and instrument partitioning would create uneven partitions and
would not align with the system's date-based reconciliation workload.

## Decision

The `trades` table will use PostgreSQL range partitioning on `trade_date`.
One child partition will be created per calendar month, with a default
partition to prevent valid out-of-window trades from being rejected.

Initial development data will cover April through July 2026.

## Consequences

Date-filtered queries can use partition pruning and avoid scanning unrelated
months. Old data can be archived or removed by detaching complete partitions.

The team must create future partitions before each new month and monitor the
default partition. Primary keys and unique constraints must also account for
PostgreSQL's partition-key requirements. Queries that do not filter by
`trade_date` may still scan every partition.