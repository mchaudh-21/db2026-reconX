# Prompt for ADR 0003

Create a Michael Nygard Architecture Decision Record for ReconX explaining why
instruments.metadata should use a GIN index with jsonb_path_ops. The principal
query uses the JSONB containment operator. Compare no index, B-tree, default
GIN, and GIN with jsonb_path_ops. Include Status, Context, Decision, and
Consequences.