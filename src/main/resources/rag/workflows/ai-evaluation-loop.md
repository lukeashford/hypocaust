# AI Evaluation Loop

Summary: A quality assurance workflow where a generator model produces outputs and an evaluator
model reviews against explicit criteria; discrepancies trigger targeted refinements until
acceptance.

## Phases

- Generate candidate output
- Evaluate against rubric
- Diagnose issues and propose fixes
- Apply minimal edits
- Re-evaluate; stop when passing

## Notes

- Keep deltas small to avoid regression
- Maintain a change log for traceability
