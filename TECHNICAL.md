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
