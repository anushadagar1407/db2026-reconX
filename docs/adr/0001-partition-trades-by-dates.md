# ADR-0001 — Partition Trades by Trade Date

## Status

Accepted

## Context

ReconX is expected to process approximately 50,000 trades per day with
five years of retention. Most reconciliation and reporting queries filter
by trade date ranges.

A single large trades table would become increasingly expensive to query
and maintain.

## Decision

The trades table will be partitioned using PostgreSQL RANGE partitioning
on trade_date.

Monthly partitions will be used and a default partition will catch
out-of-range records.

## Consequences

### Positive

- Faster date-range queries through partition pruning
- Easier archival strategy
- Reduced index sizes

### Negative

- Increased operational complexity
- Composite primary key requirements in PostgreSQL
- Partition maintenance required