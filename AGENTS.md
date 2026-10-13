# AI Development Instructions

These requirements apply to every feature, bug fix, and behavior change.

## Mandatory testing policy
Every added feature MUST include appropriate automated tests. Tests are part of the feature, not a later optional phase.

### Before implementation
1. Inspect relevant existing code and tests.
2. Define expected behavior and explicit acceptance criteria.
3. Identify normal cases, boundaries, invalid inputs, and likely regressions.
4. Write tests first when practical, or alongside implementation.

### During implementation
1. Make the smallest understandable change that satisfies requirements.
2. Keep parsing, graphics logic, project data, and UI behavior separated.
3. Avoid unnecessary dependencies, abstractions, services, accounts, telemetry, and network requirements.
4. Preserve existing behavior unless a change is explicitly requested.

### Before declaring completion
1. Run new or modified unit tests.
2. Run the complete test suite with `mvn test`.
3. Run relevant integration or end-to-end tests when available.
4. Fix failures and rerun affected tests.
5. Check every acceptance criterion.
6. Report the exact command and actual results.

### Test quality
- Verify behavior and expected results, not merely that code executes.
- Include boundary conditions, malformed inputs, and failure paths where relevant.
- Prefer deterministic fixtures that do not depend on external services or copyrighted ROMs.
- Add regression tests for confirmed bugs.
- Never delete, bypass, or weaken tests just to obtain a passing build.
- Never change expected results merely to accommodate incorrect code.
- Never claim tests passed unless they were actually executed and passed.
- If tests cannot run, state why and list outstanding verification.

A feature is complete only when its acceptance criteria are met, relevant tests pass, and limitations are documented. Compilation alone is not proof of correctness.

## Project-specific rules
- Never overwrite a ROM source file by default.
- Keep visual tile mappings separate from underlying CHR data.
- Validate lengths, offsets, and indices before access.
- Keep core ROM/graphics logic independent of JavaFX where practical.
- Prefer clear, documented, portable local file formats.
