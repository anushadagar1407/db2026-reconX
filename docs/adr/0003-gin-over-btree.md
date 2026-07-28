# ADR-0003 — Use GIN Index for JSONB Searches

## Status

Accepted

## Context

ReconX frequently performs containment queries against the
instruments.metadata JSONB column.

B-tree indexes are not suitable for efficient JSONB containment searches.

## Decision

A PostgreSQL GIN index using jsonb_path_ops will be created on
instruments.metadata.

The index will support efficient use of the @> operator.

## Consequences

### Positive

- Faster JSONB containment queries
- Smaller index size than generic jsonb_ops
- Better performance for metadata searches

### Negative

- Additional storage overhead
- Slower writes compared to no index
- Index maintenance cost