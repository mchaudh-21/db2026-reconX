# ReconX Architecture Decision Records

This directory contains Architecture Decision Records for significant technical
decisions made during the development of ReconX.

Each ADR follows the Michael Nygard format:

1. Title
2. Status
3. Context
4. Decision
5. Consequences

## AI Prompt Template

The following prompt may be used to draft future ADRs:

> Create an Architecture Decision Record using the Michael Nygard format.
> Include Title, Status, Context, Decision, and Consequences. Make the ADR
> specific to ReconX, a financial trade-reconciliation platform expected to
> process approximately 50,000 trades per day and retain up to 91 million
> trade records. Identify the alternatives considered, operational constraints,
> advantages, disadvantages, and future responsibilities resulting from the
> decision. Do not invent requirements that have not been provided.

The prompt used for each ADR must be committed under `docs/adr/prompts/`.
AI-generated drafts must be reviewed and edited by the team before acceptance.