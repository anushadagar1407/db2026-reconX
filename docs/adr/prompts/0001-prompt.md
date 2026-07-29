Write an ADR using Michael Nygard format.

System: ReconX
Decision: Partition trades by trade_date.

Alternatives:
- No partitioning
- Partition by trade ID

Constraints:
- 50k trades per day
- 5-year retention
- PostgreSQL 16