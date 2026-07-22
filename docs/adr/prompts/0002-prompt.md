# Prompt for ADR 0002

Create a Michael Nygard Architecture Decision Record for ReconX explaining why
optional instrument attributes such as sector, issuer, rating, and tags should
be stored in PostgreSQL JSONB. Compare JSONB with adding relational columns,
an entity-attribute-value table, and plain JSON text. Include Status, Context,
Decision, and Consequences. Keep stable fields such as symbol and ISIN as
relational columns.