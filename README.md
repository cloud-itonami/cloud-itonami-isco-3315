# cloud-itonami-isco-3315

Open Occupation Blueprint for **ISCO-08 3315**: Valuers and Loss Assessors.

This repository designs a forkable OSS business for an independent valuation and loss assessment practice: a property-condition documentation robot manages assessment records under a governor-gated actor, so the practice keeps its own valuation records instead of renting a closed claims SaaS.

**Maturity: `:implemented`.** `src/valuation/` implements the
`ValuationActor` as a `langgraph.graph/state-graph` (`valuation.actor`)
wired to a `Valuation Advisor` (`valuation.advisor`) and an independent
`ValuationAssessmentGovernor` (`valuation.governor`), following the
itonami actor pattern (ADR-2607011000): `:intake -> :advise -> :govern
-> :decide -+-> :commit (:ok?) +-> :request-approval (:escalate?,
human-in-the-loop interrupt) +-> :hold (:hard?)`. 14 tests / 29
assertions green (`clojure -M:test`). HARD invariants (always hold,
never overridable): client provenance, no-actuation (`:effect` must be
`:propose`), a registered asset basis for any valuation proposal, the
valuation amount not exceeding the client's registered
assessment-authority ceiling (issuance beyond it is unauthorized issuance,
not routine valuation), and site inspection completion before any valuation
can be issued (issuing without inspection evidence is incomplete due
diligence, not professional service). Always-escalate ops (human sign-off
regardless of confidence, mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)): `:finalize-assessment`
and `:issue-valuation`.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a property-condition documentation robot performs damage photographing, assessment-report assembly and physical filing under an actor that proposes
actions and an independent **Valuation Assessment Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
valuation issuance above the client's registered assessment-authority ceiling) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
claim/valuation request + property inspection + policy terms
        |
        v
Valuation Advisor -> Valuation Assessment Governor -> issue valuation/assessment, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3315`). Required capabilities:

- :robotics
- :identity
- :forms
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
