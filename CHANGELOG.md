# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [0.2.1] - 2026-11-16

### Fixed
- Releases are now signed with one fixed key, so a new version installs over the previous one and keeps your
  servers. Earlier releases (0.1.0, 0.2.0) used throwaway keys and cannot be updated in place: uninstall them once
  before installing 0.2.1.

## [0.2.0] - 2026-11-16

Material 3 Expressive redesign, with personalization settings.

### Added
- Settings: theme (system, light, dark), dynamic color, six accent colors, pure black for OLED, default date
  range, smooth chart lines, haptic feedback, reduce motion, and reset to defaults.
- Haptic feedback for tab and chip selection, chart scrubbing, pull-to-refresh, toggles, and connection results.
- Navigation rail on windows 600dp and wider; the Expressive navigation bar on phones.
- Predictive back, and a splash screen that animates out.

### Changed
- Redesigned on Material 3 Expressive: spring-based motion, rounder shapes, collapsing large top bars,
  segmented lists, the morphing loading indicator, and shape-morphing KPI tiles.
- Screens slide and fade on theme motion springs; site tabs cross-fade.
- The brand color scheme now uses green-tinted surfaces instead of the baseline purple tint.
- Material 3 is pinned to 1.5.0-rc01 for the expressive components.

### Fixed
- Light flash behind content in dark theme.
- Looping animations now stop when system "Remove animations" is on.

## [0.1.0] - 2026-11-16

First public pre-release.

### Added
- Rybbit Cloud and self-hosted servers, with multiple servers and switching between them.
- Sign-in with an API key, or with email and password (which creates a personal API key).
- Credentials encrypted with the Android Keystore.
- Overview: KPIs with previous-period deltas, trend chart, breakdowns, tap-to-filter and live visitors.
- Sessions, session timeline, users and user profiles.
- Events with property breakdowns and a live stream; errors with stack traces.
- Goals, funnels, retention cohorts, realtime feed and Web Vitals.
- Detection of features a self-hosted server doesn't offer, which are hidden instead of erroring.
- Redirect handling: same-host http→https and sub-path redirects are adopted; other redirects are refused.

[0.2.1]: https://github.com/Tresillo2017/rybbit-android/releases/tag/v0.2.1
[0.2.0]: https://github.com/Tresillo2017/rybbit-android/releases/tag/v0.2.0
[0.1.0]: https://github.com/Tresillo2017/rybbit-android/releases/tag/v0.1.0
