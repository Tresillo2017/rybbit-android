# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

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

[0.1.0]: https://github.com/Tresillo2017/rybbit-android/releases/tag/v0.1.0
