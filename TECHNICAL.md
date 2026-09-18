# Technical Notes

This file contains developer-facing instructions for maintaining and releasing PocketBudget KE.

## Release Flow
- Pushes to `main` run the CI build and produce a debug APK artifact.
- Version tags like `v1.0.1` create a signed GitHub Release.
- Manual release runs derive the tag from `versionName` in [app/build.gradle.kts](app/build.gradle.kts).

## Before Releasing
1. Update `versionCode` and `versionName` in [app/build.gradle.kts](app/build.gradle.kts).
2. Confirm the app builds successfully on `main`.
3. Create and push a version tag such as:
```bash
git tag v1.0.1
git push origin v1.0.1
```
4. Share the GitHub Release URL with testers.

## GitHub Actions
- [ci-build.yml](.github/workflows/ci-build.yml) builds the debug APK on every push to `main`.
- [release-apk.yml](.github/workflows/release-apk.yml) publishes signed release builds from version tags.

## Signing and Secrets
Required repository secrets:
- `SIGNING_KEYSTORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

The repository also includes a template file for reference:
- [github-secrets.env.example](github-secrets.env.example)

## Local Security Guardrails
Enable the repo hook locally with:
```bash
git config core.hooksPath .githooks
```

The hook blocks commits containing:
- `*.keystore`
- `keystore.properties`

## Notes
- Keep the release keystore backed up outside the repository.
- Never commit real secret values or signing files.

## Cash-flow classification
Transaction category and cash-flow meaning are intentionally separate.

- `Income`: money received from an external source.
- `Expense`: money spent.
- `Savings`: money moved into or out of a user-designated savings pocket or till.
- `Borrowing`: loan or credit proceeds, such as a Fuliza disbursement. This
  increases available cash but is not earned income.
- `Transfer`: a legacy bucket retained for database compatibility. New and
  migrated `Transfer & Cash` transactions are treated as `Expense`, because
  money sent, withdrawn, or moved out is spending from the tracked wallet.

`Received` messages default to `Income`, but a user-selected `Savings` category is
learned for both the party and account/till name. Later withdrawals from that
same actor remain `Savings`. Savings is excluded from ordinary income/expense
totals and remains exposed as net movement. `Transfer & Cash` is included in
expense totals, category spending, budgets, analytics, exports, and daily
summaries.

`Debt & Credit` is used for Fuliza and other credit activity. Loan proceeds are
tracked as borrowing and affect net cash; repayments remain expense-side cash
outflows. The app does not yet split a repayment into principal and interest
because the imported M-Pesa message does not reliably provide that breakdown.

The Kotlin-side rules live in `CashFlowClassifier`; database summary queries use
the same bucket semantics for dashboard totals and filtering.
