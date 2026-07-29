# ADR-0002 — Store Instrument Metadata in JSONB

## Status

Accepted

## Context

Different asset classes require different sets of attributes.

Maintaining dedicated columns for every optional attribute would lead to
schema growth and frequent migrations.

## Decision

The instruments table will contain a metadata JSONB column.

Optional attributes such as sector, issuer, rating, exchange and tags
will be stored inside JSONB.

## Consequences

### Positive

- Flexible schema
- Reduced migration frequency
- Supports evolving business requirements

### Negative

- Validation shifts to application logic
- More complex querying
- More difficult reporting than strongly typed columns