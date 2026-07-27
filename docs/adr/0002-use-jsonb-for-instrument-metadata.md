# ADR 0002: Use JSONB for Instrument Metadata

## Status

Accepted

## Context

ReconX instruments have stable attributes such as symbol, ISIN, asset class,
and currency. They may also have optional attributes such as sector, issuer,
rating, and tags. These optional attributes differ by asset class and may
change as the platform grows.

We considered adding a relational column for every attribute, creating a
separate attribute table, storing plain JSON text, and using PostgreSQL JSONB.

Adding columns for every possible attribute would require frequent migrations.
An entity-attribute-value table would make validation and querying more
complicated. Plain JSON text would not provide efficient containment queries.

## Decision

ReconX will store optional instrument attributes in a `metadata` column using
PostgreSQL JSONB. The column will be `NOT NULL` and will default to an empty
JSON object.

Stable and frequently joined fields will remain normal relational columns.
JSONB will only contain optional or schema-flexible metadata.

## Consequences

New optional attributes can be introduced without a database migration, and
the application can use PostgreSQL JSON containment operators.

The application must validate the expected metadata structure. JSONB can allow
inconsistent property names or value types if validation is omitted. Important
relational fields must not be moved into JSONB merely to avoid schema design.