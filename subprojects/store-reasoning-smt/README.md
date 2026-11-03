# SMT solver strategies

The following strategies are the available integrations in the SMT propagator:

- `RESET` rebuilds the current assertion set for each check and serves as a
  reference strategy.
- `POP_REPLAY` (default) keeps assertion batches in solver scopes. Removing an
  assertion pops the affected suffix and replays the current assertions lost
  with it. Older domain bounds remain asserted while the current domain refines
  them.
- `GUARDED_ASSUMPTIONS` retains guarded assertions and checks with the current
  activation literals. Previously seen rule instances and domain values can be
  reactivated without replaying a suffix.

Both incremental strategies (`POP_REPLAY` and `GUARDED_ASSUMPTIONS`) use an
assertion journal fed by rule and variable listeners.
The enum's `create(context, solver)` factory constructs a `SolverStrategy` with
`synchronize`, `check`, and `reset` interaction points. Each strategy
owns its solver state and history; the journal owns current sources and dirty
keys, passing read-only views to the strategy.
Updates are coalesced until the next check; query changes are flushed before
cached results are reused. Partial and candidate solvers keep separate journals.

The stack rebuilds at 1,024 batches or when retained bundles exceed both 1,024
and four times the current source count. Guarded assertions rebuild at the same
bundle threshold. Rebuilding drops backend history while retaining current
sources. Existing model-context expression and variable caches are independent
of these limits.

Configure the SMT propagator as:

```java
new SmtPropagator()
        .solverStrategy(SmtSolverStrategy.POP_REPLAY)
        .rule(precondition, assertedTerm);
```
