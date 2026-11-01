# Submit and save exercise results

**Intent:** Let a signed-in user submit every exercise attempt and retain its results, including incomplete or failed attempts. The current exercise client only displays live figures; no submission path exists.

**Approach:** Add a Submit action to every exercise mode and an authenticated backend submission path. The server looks up the exercise in `CannedCatalog`, evaluates prose using the exact Wagner–Fischer alignment and numeric answers using exact numeric comparison, derives pass/fail, and writes a score record plus a practice-session summary transactionally for the authenticated user. Show the saved verdict and metrics on success; preserve the form for correction/retry on failure.

**Data rules:** Use the existing tables and rules: prose passes at accuracy >= 0.95; numeric tasks pass only when correct. Save partial attempts with `passed=false`. Do not persist the prompt or typed text. Never accept user identity, expected answers, accuracy, or pass state from the browser as authoritative. Graduation attempts must use their existing lifecycle/link tables.

**Alternatives:** (1) Save only the current prose drill — rejected because the user asked for each exercise. (2) Trust client-computed scores — rejected because clients can alter results. (3) Server-evaluated submissions for all exercise modes using the existing schema — recommended and selected.

**Constraints/non-goals:** No new dependencies or schema changes unless inspection shows the existing graduation schema cannot represent a submitted attempt. Keep exercise content transient in the browser and database writes scoped to the authenticated owner. Do not add auto-submit, alter exercise content, or introduce new progression rules.

**Verification:** Add failing-first tests for evaluation boundaries, authenticated and invalid submissions, persisted session/score records (including partial attempts), and the submit UI/result state. Run the relevant module tests and the project verification suite.
