Write an ADR using Michael Nygard format.

System: ReconX
Decision: Use GIN jsonb_path_ops index.

Alternatives:
- No index
- B-tree index

Constraints:
- Fast metadata lookup
- PostgreSQL JSONB queries