# Prompt for ADR 0001

Create a Michael Nygard Architecture Decision Record for ReconX explaining why
a PostgreSQL trades table should use monthly RANGE partitioning on trade_date.
ReconX processes approximately 50,000 trades per day and may retain 91 million
records. Compare an unpartitioned table, counterparty partitioning, instrument
partitioning, and date partitioning. Include Status, Context, Decision, and
Consequences, including partition pruning, future partition maintenance, and a
default partition.