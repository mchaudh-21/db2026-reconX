# ADR 0003: Use a GIN Index for JSONB Metadata

## Status

Accepted

## Context

ReconX must search instrument metadata using containment expressions such as:

`metadata @> '{"sector":"Technology"}'`

We considered no index, a B-tree index, a default GIN index, and a GIN index
using the `jsonb_path_ops` operator class.

A B-tree index is suited to ordering and whole-value equality, but it does not
efficiently support the required JSONB containment queries. Sequential scans
would become increasingly expensive as the instrument catalogue grows.

## Decision

ReconX will create a GIN index on `instruments.metadata` using
`jsonb_path_ops`. This option is optimized for the JSONB containment operator
used by the application.

## Consequences

Containment searches can use bitmap index scans instead of sequentially
scanning every instrument. This supports responsive metadata filtering.

The index requires additional storage and makes inserts and metadata updates
more expensive. `jsonb_path_ops` supports fewer operators than the default GIN
operator class. If future queries require unsupported operators, the team must
reconsider the operator class or introduce an additional index.