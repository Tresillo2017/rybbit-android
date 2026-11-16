# Rybbit Client for Android — Architecture contract

A native Android dashboard for [Rybbit](https://rybbit.com) analytics that works with **Rybbit Cloud**
(`https://app.rybbit.io`) and **any self-hosted instance**. Read-only analytics viewer (plus nothing
destructive) — it never writes analytics data.

Stack: Kotlin 2.3 (AGP 9 built-in Kotlin), Jetpack Compose + Material 3, Navigation Compose (type-safe
`@Serializable` routes), Ktor client (OkHttp engine) + kotlinx.serialization, DataStore, Android
Keystore. **No Hilt / no KSP** — manual DI through `AppContainer`. minSdk 26, compile/target 37, JDK 17 bytecode.

Build: `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew assembleDebug testDebugUnitTest`

## Ground truth (verified against a live self-hosted server — overrides the docs)

`docs/fixtures/*.json` are **real responses** captured from a self-hosted Rybbit (v2.x, Nov 2026) for
site 1 with `start_date=2026-11-01&end_date=2026-11-30&time_zone=Europe/Madrid`. Model DTOs on them.
`docs/api-subset.md` is the official docs for the endpoints we use (fields/params); `docs/openapi.json`
lists every route. When docs and fixtures disagree, fixtures win.

### Authentication
* **API key** (primary): `Authorization: Bearer <key>`. Works on cloud and self-hosted. Personal or org key.
* **Email + password** (self-hosted convenience, also cloud): `POST {base}/api/auth/sign-in/email`
  body `{"email","password"}` with header `Origin: {base}` (required, else 403 `MISSING_OR_NULL_ORIGIN`).
  The response JSON `token` is **NOT** usable as a Bearer token (401). The session is the
  `Set-Cookie` header: name is `__Secure-better-auth.session_token` in production *or*
  `better-auth.session_token` otherwise — **store whatever name=value pair is received**, never hardcode.
  The cookie carries the `Secure` flag, so a Ktor/OkHttp cookie jar drops it on `http://` instances:
  **capture it from the header manually and send it manually** as `Cookie: name=value`.
  After sign-in, immediately `POST {base}/api/user/api-keys` `{"name":"Rybbit Android (<device model>)"}`
  with the cookie + Origin → response `{"key": "..."}` → persist **the API key** and use only it afterwards
  (sessions expire in 7 days; keys don't). If key creation fails (old server / no permission), fall back to
  storing the session cookie as the credential (auth type `Session`) and prompt re-login on 401.
* Errors are JSON `{"error","code","message","resolution"}` (see `unauthorized.json`). 401 code
  `AUTHENTICATION_REQUIRED`; 403 `FORBIDDEN` (also returned for a site id you can't access / doesn't exist).

### Instance URL handling
User types `analytics.example.com`, `https://analytics.example.com/`, `https://x.com/api`, or
`http://192.168.1.10:3001`. Normalize: trim, default scheme `https://` when missing, lowercase host,
drop trailing `/`, drop a trailing `/api`, **keep** any other path prefix (reverse-proxy sub-path). API
paths are `{base}/api/...`. Validate with `GET {base}/api/health` (200 = Rybbit reachable) then
`GET {base}/api/organizations` with the credential. `http://` is allowed (LAN installs) but the UI must
show a warning. Cloud preset: `https://app.rybbit.io`.

### Self-hosted feature gaps
Some routes don't exist on self-hosted builds: 404 with code `API_ROUTE_NOT_FOUND` (see `pages.json`).
Treat that code as **"feature unavailable on this instance"** → hide that screen/tab or show a calm
"Not available on this server" state, never a red error. Known cloud-only: `/pages`, web vitals
(performance returns all-null percentiles on self-hosted), bots, GSC, SQL query. Cache availability
per instance in memory.

### Query parameters (shared by most `/api/sites/{site}/...` analytics endpoints)
`start_date`, `end_date` (`yyyy-MM-dd`) + `time_zone` (IANA, use device `ZoneId.systemDefault()`), **or**
`past_minutes_start`/`past_minutes_end` for rolling windows (e.g. last 30 min / 24 h),
`filters` = URL-encoded JSON array `[{"parameter":"browser","type":"equals","value":["Firefox"]}]`
(see `overview_filtered.json`). Time series: `bucket` = `minute|five_minutes|ten_minutes|fifteen_minutes|hour|day|week|month`.
Metric breakdown: `parameter` (`pathname`, `page_title`, `entry_page`, `exit_page`, `referrer`,
`channel`, `utm_source`, `utm_medium`, `utm_campaign`, `country`, `region`, `city`, `browser`,
`operating_system`, `device_type`, `language`, `hostname`…), `limit`, `page`.
Errors per-message endpoints use camelCase **`errorMessage`** (docs say `error_message` — wrong).

### Response quirks (parse leniently!)
* `Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; explicitNulls = false }`.
  Every non-essential field nullable or defaulted. Older servers differ.
* Numbers sometimes arrive as strings: `goals.meta.total` is `"0"`. Use a lenient number serializer.
* Metric responses are double-nested `{"data":{"data":[...],"totalCount":N}}`; overview is `{"data":{...}}`;
  sessions/events/users are `{"data":[...]}` (+ optional meta/cursor — check fixtures).
* `errors/time-series` wraps as `{"success":true,"data":[...]}`.
* `live-user-count` is `{"count":N}`; `/api/organizations` is a bare array; `segments` is a bare array.
* Timestamps are `"yyyy-MM-dd HH:mm:ss"` (no `T`, no zone; sometimes fractional seconds).
  **Event/session timestamps are UTC.** **Time-series bucket `time` values are already in the requested
  `time_zone`** (local wall time). Parse both explicitly; never `Instant.parse` them raw.
* `properties` on events is a JSON **string** (`"{}"`), decode on demand.

## Package layout (`app/src/main/java/app/rybbitclient/`)

```
RybbitApp.kt, MainActivity.kt
di/AppContainer.kt                 manual DI graph (singletons) created in RybbitApp
core/model/                        domain + @Serializable DTOs (Instance, Organization, Site, Overview,
                                   TimeSeriesPoint, MetricRow, Session, SessionDetail, Event, User, Goal,
                                   Funnel, FunnelStep, ErrorName, ErrorEvent, Retention, …)
core/network/                      HttpClientFactory, RybbitApi (one method per endpoint), ApiException
                                   hierarchy, LenientNumberSerializer, timestamp parsing, UrlNormalizer
core/auth/                         AuthService: probe(baseUrl), signInWithApiKey, signInWithPassword
core/storage/                      InstanceRepository (DataStore + JSON), SecretCipher (Keystore AES/GCM),
                                   SecretStore interface (+ in-memory impl for tests)
core/data/                         AnalyticsRepository(interface)+impl, FeatureAvailability
core/query/                        DateRange (presets: Today, Yesterday, Last24h, Last7d, Last30d,
                                   Last90d, ThisMonth, LastMonth, ThisYear, Custom) → params, previous
                                   period, Filter, AnalyticsQuery, bucket selection
core/ui/                           theme/, components/ (KpiCard, LineChart, BarListCard, StateViews,
                                   DateRangeBar, FilterChipsRow, SectionCard), format/ (numbers,
                                   durations, percent, flags, relative time)
navigation/                        Routes.kt (@Serializable), AppNavHost.kt, SiteScaffold (bottom bar)
feature/<name>/                    <Name>Screen.kt, <Name>ViewModel.kt (+ components) per feature
```

## Conventions
* ViewModels expose `StateFlow<UiState>`; UiState is an immutable data class or sealed interface
  (`Loading / Content / Error(ApiException) / Unavailable`). No mutation of shared objects.
* ViewModels are created with `viewModel { XViewModel(container.analyticsRepository, …) }` using
  `LocalAppContainer` (CompositionLocal provided in MainActivity).
* Repositories return `Result`-like `ApiResult<T>` or throw typed `ApiException`; UI maps exceptions to
  friendly copy (offline, unauthorized → "re-authenticate", rate limited, server error, unavailable).
* Pull-to-refresh on every data screen. Every screen designs loading / empty / error / long content.
* Dates shown in the device locale & zone. Numbers compact (1.2k). Durations `1m 05s`.
* Accessibility: content descriptions on icon buttons, min 48dp touch targets, charts get a text summary.
* Files < 400 lines, functions < 50 lines. Unit tests (JUnit4 + kotlinx-coroutines-test + Ktor MockEngine)
  for network decoding of **every fixture**, URL normalization, date ranges, formatters, auth flow, ViewModels.
* Never log credentials. Credentials only in `SecretStore` (encrypted), never in DataStore plaintext.
