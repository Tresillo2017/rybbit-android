# Contributing

Thanks for helping improve Rybbit for Android!

## Ground rules

- **Open an issue first** for anything larger than a small fix, so we can agree on the approach.
- **The app is read-only.** It views analytics and doesn't change Rybbit data or settings.
- **Fixtures win over docs.** The Rybbit API sometimes differs from its documentation. Base models on real
  responses: add or refresh a fixture in `docs/fixtures/` and decode it in a test.
- **Self-hosted first.** Every feature has to degrade cleanly on self-hosted servers. When an endpoint
  returns `404 API_ROUTE_NOT_FOUND`, hide the feature instead of showing an error.

## Development setup

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Requirements: JDK 17+ (21 recommended) and Android SDK platform 37. See the README for running against a
live server.

## Code style

- Kotlin official style. Immutable `data class` UI state, exposed as `StateFlow` from ViewModels.
- Keep files under about 400 lines and functions under about 50 lines.
- Reuse the UI kit in `core/ui/` (charts, KPI tiles, state views, formatters) instead of reimplementing.
- Design every state: loading, empty, error, unavailable and long content.
- Don't log credentials, and keep them out of navigation arguments and `rememberSaveable`.

## Tests

- Add unit tests with every change: ViewModels with a fake `AnalyticsRepository`, and network decoding
  with Ktor `MockEngine` and fixtures.
- For UI changes, regenerate the screenshots with `./gradlew recordRoborazziDebug` and include them in
  the PR.

## Commits and pull requests

- Use [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `refactor:`, `docs:`,
  `test:`, `chore:`, `ci:`.
- Keep PRs focused, fill in the PR template, and make sure CI is green.

## Releasing

Releases are built by the `Release` workflow when a `v*` tag is pushed. It signs the APK with the project's
fixed key from these repository secrets: `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`,
`RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`, and it fails rather than publish a debug-signed APK.
Never change or lose this key: Android only updates an installed app when the new APK has the same signature.
Local `assembleRelease` builds fall back to the debug key unless `RYBBIT_KEYSTORE`,
`RYBBIT_KEYSTORE_PASSWORD`, `RYBBIT_KEY_ALIAS` and `RYBBIT_KEY_PASSWORD` are set.
