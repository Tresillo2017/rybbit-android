# Feature guide

For the agents building the screens. Read `docs/ARCHITECTURE.md` first (the contract). This file lists what
already exists, the navigation skeleton you plug into, and the one worked example to copy.

Base package: `app.rybbitclient`. Source root: `app/src/main/java/app/rybbitclient`.

## 1. Ground rules

* Route classes end in `Route` (`SiteRoute`, `SessionDetailRoute`...). This avoids clashes with models
  (`Site`, `Goal`, `Funnel`, `Event`, `UserDetail`).
* Routes carry only ids and simple values. `siteId` is `Site.analyticsId` (the URL id), never `Site.id`.
* Immutable state. Update with `copy(...)` or a new value; never mutate a shared object.
* No Hilt, no KSP. Get dependencies from `LocalAppContainer`.
* Files under 400 lines, functions under 50 lines. Never log credentials.
* To remove an instance, call `container.removeInstance(id)`. Do not call `instanceRepository.remove`
  directly: that skips the HTTP client cache and feature-availability records.

## 2. Getting dependencies and building a ViewModel

```kotlin
val container = LocalAppContainer.current
val viewModel: SessionsViewModel = viewModel(key = "sessions-$siteId") {
    SessionsViewModel(siteId, container.analyticsRepository)
}
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

`viewModel(key = ...)` keeps one ViewModel per site per screen. Use the same key scheme everywhere.
Use `collectAsStateWithLifecycle()` (lifecycle-runtime-compose) to collect `StateFlow`s in Compose.

`AppContainer` exposes: `httpClientFactory`, `instanceRepository`, `authService`, `featureAvailability`,
`apiProvider`, `analyticsRepository`, and `removeInstance(instanceId)` (suspend).

## 3. Data layer

### AnalyticsRepository (interface, `core/data/AnalyticsRepository.kt`)

Every method reads the active instance. Failures throw `ApiException` subtypes (section 5). No writes exist.
Defaults: `DEFAULT_LIMIT = 50`, `DEFAULT_LIVE_MINUTES = 5`, `DEFAULT_GOALS_PAGE_SIZE = 10`,
`DEFAULT_USERS_PAGE_SIZE = 100`, `DEFAULT_RETENTION_RANGE = 30` (constants on the companion object).

```kotlin
suspend fun isFeatureAvailable(feature: Feature): Boolean
suspend fun organizations(): List<Organization>
suspend fun sites(organizationId: String): List<Site>
suspend fun site(siteId: String): Site
suspend fun overview(siteId: String, query: AnalyticsQuery): Overview
suspend fun overviewTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<OverviewPoint>
suspend fun metric(siteId: String, query: AnalyticsQuery, parameter: String, limit: Int = 50, page: Int = 1): MetricPage
suspend fun liveUserCount(siteId: String, minutes: Int = 5): Int
suspend fun sessions(siteId: String, query: AnalyticsQuery, page: Int = 1, limit: Int = 50): List<SessionSummary>
suspend fun sessionDetail(siteId: String, sessionId: String): SessionDetailResponse
suspend fun sessionLocations(siteId: String, query: AnalyticsQuery): List<SessionLocation>
suspend fun events(siteId: String, query: AnalyticsQuery, pageSize: Int = 50, beforeCursor: String? = null): EventsResponse
suspend fun eventNames(siteId: String, query: AnalyticsQuery): List<EventName>
suspend fun eventProperties(siteId: String, query: AnalyticsQuery, eventName: String): List<EventPropertyCount>
suspend fun eventsTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket): List<EventTimePoint>
suspend fun errorNames(siteId: String, query: AnalyticsQuery, limit: Int = 50): List<ErrorName>
suspend fun errorEvents(siteId: String, query: AnalyticsQuery, errorMessage: String, limit: Int = 50): List<ErrorEvent>
suspend fun errorTimeSeries(siteId: String, query: AnalyticsQuery, bucket: TimeBucket, errorMessage: String? = null): List<ErrorTimePoint>
suspend fun goals(siteId: String, query: AnalyticsQuery, page: Int = 1, pageSize: Int = 10): GoalsResponse
suspend fun goalSessions(siteId: String, query: AnalyticsQuery, goalId: Int, page: Int = 1, limit: Int = 50): List<SessionSummary>
suspend fun funnels(siteId: String): List<Funnel>
suspend fun analyzeFunnel(siteId: String, query: AnalyticsQuery, steps: List<FunnelStep>): List<FunnelStepResult>
suspend fun users(siteId: String, query: AnalyticsQuery, page: Int = 1, pageSize: Int = 100): UsersResponse
suspend fun userDetail(siteId: String, userId: String): UserDetail
suspend fun retention(siteId: String, mode: String = "day", range: Int = 30): RetentionData
suspend fun performanceOverview(siteId: String, query: AnalyticsQuery): PerformanceOverview
suspend fun pages(siteId: String, query: AnalyticsQuery, limit: Int = 50): List<PageRow>   // cloud-only, see gaps
```

Notes:
* `eventProperties` sends the event name as `eventName` (not confirmed by the docs; fixtures agree).
* `errorEvents` and `errorTimeSeries` use the camelCase `errorMessage` parameter (the fixtures use it).
* `pages` is gated: on a 404 `API_ROUTE_NOT_FOUND` it throws `FeatureUnavailable` and records that for the instance.
* `Feature` (`core/data/FeatureAvailability.kt`): `Feature.Pages`, `Feature.Performance`. `isFeatureAvailable`
  returns false only after the server said 404 for that feature on this app run (in memory).

### Models (`core/model/`)

Key fields (all lenient; most nullable):
* `Organization(id, name, slug, role, sites)`; `OrganizationSites(organization, sites)`.
* `Site(id, siteId: Long?, name, domain, ...)`. Use `site.analyticsId` for URLs.
* `Overview`, `OverviewPoint(time, ...)`, `MetricRow(value, count, percentage, pageviews, timeOnPageSeconds, bounceRate)`,
  `MetricPage(data, totalCount)`, `LiveUserCount(count)`.
* `SessionSummary(sessionId, ...)`, `SessionDetailResponse(session, events, pagination)`, `SessionLocation`.
* `Event(timestamp, eventName, properties: String)` (properties is JSON text; decode on demand),
  `EventsResponse(data, cursor)`, `EventCursor(hasMore, oldestCursor, newestCursor, nextCursor)`.
* `ErrorName(value, errorName, count, sessionCount)`, `ErrorEvent(message, sessionId)`, `ErrorTimePoint`.
* `Goal(goalId: Int, name, conversionRate)`, `GoalsResponse(data, meta)`.
* `Funnel(id: Int, name, steps)`, `FunnelStep(type, value, name)`, `FunnelStepResult`.
* `UserSummary`, `UsersResponse`, `UserDetail`.
* `RetentionData(cohorts, maxPeriods)`, `PerformanceOverview`, `PageRow` (no fixture yet).
* `Instance(id, displayName, baseUrl, kind: InstanceKind, authType: AuthType, accountLabel, createdAtMillis: Long)`.
  `InstanceKind { Cloud, SelfHosted }`, `AuthType { ApiKey, Session }`, `instance.isInsecure` for `http://`.

Timestamps: event and session timestamps are UTC. Time-series `time` values are already in the requested
zone. The `Rybbit*` helpers in `core/network/RybbitTimestamps.kt` parse them; never `Instant.parse` raw strings.

## 4. Query types (`core/query/`)

```kotlin
data class AnalyticsQuery(val range: DateRange, val filters: List<Filter> = emptyList(), val zone: ZoneId = ZoneId.systemDefault())
```
* `DateRange`: `Today`, `Yesterday`, `Last24Hours`, `Last7Days`, `Last30Days`, `Last90Days`, `ThisMonth`,
  `LastMonth`, `ThisYear`, `AllTime` (objects), and `Custom(start: LocalDate, end: LocalDate)`.
* `DateRangeResolver(clock, zone)`: `resolve(range): ResolvedRange`, `previousPeriod(range): ResolvedRange?`.
* `ResolvedRange`: `Dates(start, end)`, `PastMinutes(startMinutesAgo, endMinutesAgo)`, `Unbounded`.
* `TimeBucket(apiValue)`: `Minute, FiveMinutes, TenMinutes, FifteenMinutes, Hour, Day, Week, Month`.
  `TimeBucket.defaultFor(resolved)` picks one. `query.defaultBucket(clock)` does the same from a query.
* `Filter(parameter: String, type: FilterType, values: List<String>)`. `FilterType` has 14 values (`Equals`,
  `NotEquals`, `Contains`, `NotContains`, `StartsWith`, `EndsWith`, `Regex`, `NotRegex`, `IsNull`,
  `IsNotNull`, `GreaterThan`, `LessThan`, `GreaterThanOrEqual`, `LessThanOrEqual`). `FilterEncoder.encode` builds the JSON.
* `query.toParams(clock)` and `query.previousPeriod(clock)` produce the URL parameter maps.
* Metric parameters: `pathname, page_title, entry_page, exit_page, referrer, channel, utm_source, utm_medium,
  utm_campaign, country, region, city, browser, operating_system, device_type, language, hostname`.

Turn the device picker's UTC-midnight millis into `LocalDate` with `Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()`.

## 5. Auth, instances, errors

### AuthService (`core/auth/AuthService.kt`)
* `suspend fun probe(baseUrl: String)`: throws if the server is not Rybbit.
* `suspend fun signInWithApiKey(baseUrl: String, key: String): List<Organization>`
* `suspend fun signInWithPassword(baseUrl: String, email: String, password: String, deviceName: String): Credential`
  (creates an API key; falls back to a session credential).

Normalize user-typed URLs with `UrlNormalizer` before storing. Cloud preset: `https://app.rybbit.io`.

### InstanceRepository (`core/storage/InstanceRepository.kt`)
Flows: `instances: Flow<List<Instance>>`, `activeInstanceId: Flow<String?>`, `activeInstance: Flow<Instance?>`,
`lastSelectedSiteId(instanceId): Flow<String?>`.
Suspend: `instance(id)`, `add(instance, credential)`, `update(instance)`, `updateCredential(id, credential)`,
`credentialFor(id)`, `setActive(id)`, `setLastSelectedSiteId(id, siteId?)`. `remove(id)` exists, but use
`container.removeInstance(id)`.
Credentials live only in the encrypted `SecretStore`.

### ApiException (`core/network/ApiException.kt`), sealed
`Unauthorized`, `Forbidden`, `FeatureUnavailable` (404 + `API_ROUTE_NOT_FOUND`), `NotFound`,
`RateLimited(retryAfterSeconds)`, `ClientError(status)`, `Server(status)`, `InvalidCredentials`
(all `HttpStatusException` with `status`, `code`, `serverMessage`, `resolution`), plus `Network(cause)` and
`InvalidResponse(cause)`.

Map errors to copy with `ApiException.toErrorCopy()` (`feature/common/ErrorCopy.kt`), which returns
`ErrorCopy(title, message)`. Never show `serverMessage` as a title.

`FeatureUnavailable` means "not on this server": show `UnavailableView()`, not `ErrorView`.

## 6. UI kit (`core/ui/`)

Theme: `RybbitTheme` is already applied by `MainActivity`; do not wrap again.
* `MaterialTheme.rybbitColors`: `positive`, `negative`, `neutral`, `chartSeries`, `chartPrevious`, `barTrack`,
  `selectedWash`, `heatLow`, `heatHigh`, `live`, `isDark`.
* Typography: `Typography.metricValue`, `Typography.metricDelta`, `Typography.tabularBody`, `Typography.overline`,
  and `TabularNumbers` (`"tnum"`) for aligned digits.
* `Spacing`: `xxs, xs, sm, md, lg, xl, xxl, screenGutter (16dp), minTouchTarget (48dp)`.

Format (pure Kotlin, `core/ui/format/`): `EMPTY_VALUE = "—"`.
* `formatFullNumber(value: Double, locale)`, `formatCompactNumber(value: Double, locale)` (1.2k, 3.4M)
* `formatPercent(percent: Double, locale, fractionDigits = 1)`, `formatDeltaPercent(deltaPercent: Double, ...)` (+12.3%)
* `formatBounceRate(bouncePercent: Double, locale)`
* `formatDuration(seconds: Double)` / `formatDuration(seconds: Long)` (45s, 1m 05s, 2h 03m)
* `formatRelativeTime(timestampMillis: Long, nowMillis = System.currentTimeMillis())` (English only)
* `countryFlagEmoji(code)`, `countryDisplayName(code, locale)`, `countryLabel(code, locale)`

All percent inputs are 0..100 (matching `bounce_rate` and `conversion_rate`).

Components (`core/ui/components/`). Parameter names as written; `modifier` is last-ish and defaults to `Modifier`:
* `LoadingView(modifier, skeleton = true, rowCount = 3)`
* `ErrorView(title, message, modifier, onRetry: (() -> Unit)? = null, retryLabel = "Try again")`
* `EmptyView(title, message, modifier, icon = Icons.Outlined.Inbox)`
* `UnavailableView(modifier, title = "Not available on this server", message = "...")`
* `Modifier.shimmerPlaceholder(shape)`
* `KpiCard(label, value, modifier, deltaPercent: Double? = null, isInverse = false, selected = false, onClick: (() -> Unit)? = null)`
* `KpiItem(key, label, value, deltaPercent: Double? = null, isInverse = false)` and
  `KpiGrid(items: List<KpiItem>, modifier, selectedKey: String? = null, columns = 2, onItemClick: ((String) -> Unit)? = null)`
* `LineChart(series: List<ChartSeries>, modifier, height = 220.dp, smooth = true, summary: String? = null, valueFormatter = compact, xLabelFormatter: (Int) -> String = { "" }, onPointSelected: ((Int?) -> Unit)? = null)`
  where `ChartSeries(label, values: List<Double>, isPrevious = false)`.
* `BarItem(key, label, value: Double, valueText, percentText: String? = null)`
* `BarListRow(label, valueText, fraction: Float, modifier, rank: Int? = null, percentText, onClick, leading)`
* `BarListBody(items: List<BarItem>, modifier, limit = 5, onShowMore, onItemClick: ((String) -> Unit)?, itemLeading, emptyMessage)`
* `BarListCard(title, items, modifier, subtitle, limit = 5, onShowMore, onItemClick, itemLeading)`
* `BreakdownCard(title, tabs: List<String>, selectedTab: Int, onTabSelected: (Int) -> Unit, items, modifier, subtitle, limit = 5, onShowMore, onItemClick, itemLeading, emptyMessage)`
* `SectionCard(title, modifier, subtitle, action: (() -> Unit)? = null, content: ColumnScope.() -> Unit)`
  (note: `action` is a click handler, not a composable slot).
* `DateRangeBar(options: List<DateRangeOption>, selectedKey: String?, onPresetSelected: (String) -> Unit, onCustomRangeSelected: (startMillis: Long, endMillis: Long) -> Unit, modifier, customLabel = "Custom", customSelected = false, initialStartMillis: Long? = null, initialEndMillis: Long? = null)`
  where `DateRangeOption(key, label)`.
* `FilterChipsRow(filters: List<ActiveFilter>, onRemove: (key: String) -> Unit, onClearAll: () -> Unit, modifier)`
  where `ActiveFilter(key, label)`.
* `LiveBadge(count: Int?, modifier, suffix = "online")`
* `FunnelChart(steps: List<FunnelStepItem>, modifier, summary: String? = null)`
  where `FunnelStepItem(label, count: Double, conversionPercent: Double? = null, dropoffPercent: Double? = null)`.
* `RetentionGrid(cohorts: List<RetentionCohort>, modifier, periodLabel: (Int) -> String = { "P$it" }, cohortHeader = "Cohort", sizeHeader = "Users")`
  where `RetentionCohort(label, size: Int, percentages: List<Double?>)`.
* `Sparkline(values: List<Double>, modifier, color: Color? = null, height = 36.dp, summary: String? = null)`

Always give charts a `summary` string (accessibility).

## 7. Navigation skeleton and the placeholders you own

Flow: `SplashRoute` (loads instances) → `OnboardingGraphRoute` (Welcome) or `SitesRoute`.
Main flow: `SitesRoute` → `SiteRoute(siteId, siteName)`, which hosts a `SiteScaffold` (top bar, five-tab
bottom bar, inner NavHost of tab routes). Detail routes sit on the outer NavHost (no tab bar).

Navigation actions go through `AppNavigator` (`navigation/AppNavigator.kt`), which is already wired. Feature
screens receive lambdas only; never pass a NavController.

Every placeholder file exists and compiles. Replace the body and KEEP THE SIGNATURE (navigation depends on it).
The body must be a complete screen: use `Scaffold` + `TopAppBar` + back for non-tab screens, and only
the content for tab screens (the shell provides chrome and padding).

| Route | Placeholder file (owner) | Signature |
|---|---|---|
| `WelcomeRoute` | `feature/onboarding/WelcomeScreen.kt` | `WelcomeScreen(onAddCloud, onAddSelfHosted)` |
| `AddInstanceRoute` | `feature/onboarding/AddInstanceScreen.kt` | `AddInstanceScreen(isCloud, onInstanceAdded, onBack)` |
| `SitesRoute` | `feature/sites/SitesScreen.kt` | `SitesScreen(onSiteSelected: (siteId, siteName), onOpenSettings, onAddInstance)` |
| `SettingsRoute` | `feature/settings/SettingsScreen.kt` | `SettingsScreen(onBack, onAddInstance, onAllInstancesRemoved)` |
| `OverviewTabRoute` | `feature/overview/OverviewScreen.kt` | `OverviewScreen(siteId, onOpenRealtime)` |
| `SessionsTabRoute` | `feature/sessions/SessionsScreen.kt` | `SessionsScreen(siteId, onOpenSession: (sessionId))` |
| `EventsTabRoute` | `feature/events/EventsScreen.kt` | `EventsScreen(siteId, onOpenEvent: (eventName))` |
| `UsersTabRoute` | `feature/users/UsersScreen.kt` | `UsersScreen(siteId, onOpenUser: (userId))` |
| `MoreTabRoute` | `feature/more/MoreScreen.kt` (done, the example) | `MoreScreen(siteId, onOpenGoals, onOpenFunnels, onOpenErrors, onOpenRetention, onOpenRealtime, onOpenWebVitals)` |
| `SessionDetailRoute` | `feature/sessions/SessionDetailScreen.kt` | `SessionDetailScreen(siteId, sessionId, onBack)` |
| `UserDetailRoute` | `feature/users/UserDetailScreen.kt` | `UserDetailScreen(siteId, userId, onOpenSession, onBack)` |
| `EventDetailRoute` | `feature/events/EventDetailScreen.kt` | `EventDetailScreen(siteId, eventName, onBack)` |
| `ErrorDetailRoute` | `feature/errors/ErrorDetailScreen.kt` | `ErrorDetailScreen(siteId, errorMessage, onBack)` |
| `GoalsRoute` | `feature/goals/GoalsScreen.kt` | `GoalsScreen(siteId, onBack)` |
| `FunnelsRoute` | `feature/goals/FunnelsScreen.kt` | `FunnelsScreen(siteId, onOpenFunnel: (funnelId: Int), onBack)` |
| `FunnelDetailRoute` | `feature/goals/FunnelDetailScreen.kt` | `FunnelDetailScreen(siteId, funnelId: Int, onBack)` |
| `ErrorsRoute` | `feature/errors/ErrorsScreen.kt` | `ErrorsScreen(siteId, onOpenError: (errorMessage), onBack)` |
| `RetentionRoute` | `feature/retention/RetentionScreen.kt` | `RetentionScreen(siteId, onBack)` |
| `RealtimeRoute` | `feature/realtime/RealtimeScreen.kt` | `RealtimeScreen(siteId, onOpenSession, onBack)` |
| `WebVitalsRoute` | `feature/performance/WebVitalsScreen.kt` | `WebVitalsScreen(siteId, onBack)` |

Each file's KDoc repeats its parameter contract. If a feature needs a different signature, change
`navigation/*.kt` in the same change (the call site is the only other place that knows it).

Adding a route: add the class to `navigation/Routes.kt`, register it in the matching `*Routes.kt` file
(`DetailRoutes.kt` for detail screens; `SiteTabHost.kt` for tabs), and add an `AppNavigator` function.

Owned by the navigation layer, not feature agents: `navigation/*`, `MainActivity.kt`, `di/*`.

## 8. UiState pattern

Each screen has a sealed `UiState` and one ViewModel. The ViewModel owns loading; the screen only renders.

```
sealed interface XUiState {
    data object Loading : XUiState
    data class Content(..., val isRefreshing: Boolean = false) : XUiState
    data class Error(val error: ApiException) : XUiState
}
```
Rules:
* `private val mutableState = MutableStateFlow<XUiState>(XUiState.Loading)`; expose `val uiState: StateFlow<...> = mutableState.asStateFlow()`.
* Load in `init` and `refresh()` via `viewModelScope.launch`. Catch `ApiException` only.
* Pull-to-refresh keeps `Content` and sets `isRefreshing = true`. Show `Loading` only on the first load.
* Empty results are `Content` with an empty list. Render `EmptyView`, not an error.
* `FeatureUnavailable` on a screen is not an error: map it to `UnavailableView` (section 5).
* Time windows and filters belong in the ViewModel (a `MutableStateFlow<AnalyticsQuery>`), so changing a range triggers a reload.

## 9. Worked example: More

Read these three files together:
* `feature/more/MoreUiState.kt`: the sealed state, with `Content.isRefreshing` and `Error(error: ApiException)`.
* `feature/more/MoreViewModel.kt`: the ViewModel. It loads the funnel count and feature availability, and
  shows the `init`/`refresh`/`catch ApiException` pattern.
* `feature/more/MoreScreen.kt`: renders all three states (`LoadingView`, `ErrorView` with `toErrorCopy()`,
  and a `PullToRefreshBox` with `LazyColumn` of `ListItem`s). Web Vitals is a conditional row.

The ViewModel's test template is `app/src/test/java/app/rybbitclient/feature/more/MoreViewModelTest.kt`. It
uses a hand-written `FakeAnalytics : AnalyticsRepository` (every method except the ones under test throws
`error(...)`), `UnconfinedTestDispatcher` with `Dispatchers.setMain`/`resetMain`, and AAA-style tests.
Copy it for each new ViewModel.

Web Vitals gate: `webVitalsAvailable = isCloud && analytics.isFeatureAvailable(Feature.Performance)`.
Self-hosted servers answer performance requests with empty percentiles rather than a 404, so
`WebVitalsScreen` must show `UnavailableView` when every percentile is null.

## 10. Known gaps (owned by the lead)

* `pages` has only the 404 mapping; no success fixture exists, and `PageRow` is unverified.
* `performanceOverview` has no success fixture. Self-hosted returns null percentiles without a 404 (see section 9).
* `eventProperties` sends `eventName`. The docs don't confirm it, and the fixture agrees.
* `GET /api/health` (used by `probe`) is not in the OpenAPI document.
* `KeystoreSecretCipher` has no JVM test; the store logic is tested through a fake cipher.
* Sites and Settings placeholders have no real instance management yet.
