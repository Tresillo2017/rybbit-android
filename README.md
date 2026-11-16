# Rybbit Client for Android

A native Android dashboard for [Rybbit](https://rybbit.com) analytics. Works with **Rybbit Cloud** and
**self-hosted** Rybbit servers, and can hold several servers/accounts at once.

Kotlin · Jetpack Compose · Material 3 (dynamic color, dark mode) · Ktor · DataStore · Android Keystore.
minSdk 26, targetSdk 37.

## Features

- **Servers:** Rybbit Cloud preset or any self-hosted URL (`analytics.example.com`, sub-paths, ports and
  LAN `http://` addresses are all accepted, with a warning for unencrypted http). Multiple servers with a
  quick switcher.
- **Sign-in:** an API key (personal or organization), or email + password. Password sign-in creates a
  personal API key named `Rybbit Android (<device>)` and stores only that key, so you don't get logged out
  when the 7-day session expires. Credentials are AES-GCM encrypted with an Android Keystore key.
- **Sites:** every organization and site you can access, with live visitor counts.
- **Overview:** live visitors, KPIs with change against the previous period (visitors, sessions,
  pageviews, pages/session, bounce rate, duration), an interactive trend chart, and breakdowns for pages,
  sources/channels/UTM, locations, devices and languages. Tap a row to filter the whole dashboard.
- **Sessions:** session list and timeline (pageviews, custom events with properties, errors).
- **Users:** user list, search, and profile with sessions.
- **Events:** custom events over time, property breakdowns, live event stream.
- **More:** goals with conversion rates, funnels, retention cohorts, errors with stack traces, a realtime
  feed, and Web Vitals (cloud only).
- **Shared date range and filters:** presets plus a custom range, applied across every tab of a site.
- **Self-hosted feature gaps:** cloud-only features are detected at runtime and hidden instead of erroring.

## Build

```bash
export JAVA_HOME=/path/to/jdk-21          # AGP 9.4 needs JDK 17+
./gradlew assembleDebug                   # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest               # unit tests: fixtures, view models, auth, storage
./gradlew assembleRelease                 # minified; debug-signed until you add a signing config
```

### Live-server integration tests

`LiveServerIntegrationTest` runs against a real server over the real HTTP stack, and is skipped unless
these are set:

```bash
RYBBIT_IT_URL=http://localhost:3001 RYBBIT_IT_EMAIL=you@example.com \
RYBBIT_IT_PASSWORD=... RYBBIT_IT_SITE=1 ./gradlew testDebugUnitTest
```

A local self-hosted stack for development: clone `rybbit-io/rybbit`, set `BASE_URL`,
`BETTER_AUTH_SECRET` and the other variables in `.env`, then run `docker compose up -d backend`. From the
Android emulator, the server is at `http://10.0.2.2:3001`.

## Project layout

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), which covers the contract, the verified API quirks and
the package layout, and [`docs/FEATURE_GUIDE.md`](docs/FEATURE_GUIDE.md). `docs/fixtures/` holds real API
responses captured from a self-hosted server, which the unit tests decode.

## Known limitations

- Read-only: the app views analytics; settings, goals and funnels are managed in the web dashboard.
- Session replay, the SQL explorer and Search Console are not implemented.
- User search returns no results on the tested self-hosted build, even with the documented
  `search`/`search_field` parameters. The client sends them as documented.
- A 401 asks you to re-enter an API key in Settings. There is no in-place password re-login yet.
- Every email/password sign-in creates a long-lived personal API key. Removing a server in the app does
  not revoke it; revoke it in the web dashboard under Settings → Account → API keys.
- If the server redirects (http→https or a reverse proxy), the app signs in to and stores the redirect
  target. Authenticated requests never follow redirects, so credentials can't leak to another host.
- For a self-signed HTTPS server, install its CA on the device (user CAs are trusted for self-hosted hosts
  but not for `app.rybbit.io`).
