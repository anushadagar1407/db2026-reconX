Write an ADR using Michael Nygard format.

System: ReconX
Decision: Store instrument metadata in JSONB.

Alternatives:
- Dedicated columns
- Entity Attribute Value model

Constraints:
- Multiple asset classes
- Frequently changing attributes