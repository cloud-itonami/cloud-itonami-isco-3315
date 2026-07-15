# cloud-itonami-isco-3315

Open Occupation Blueprint for **ISCO-08 3315**: Valuers and Loss Assessors.

This repository designs a forkable OSS business for an independent valuation and loss assessment practice: a property-condition documentation robot manages assessment records under a governor-gated actor, so the practice keeps its own valuation records instead of renting a closed claims SaaS.

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
