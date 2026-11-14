# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - Unreleased

### Added

- **Authorization on top of authentication.** The bridge keeps authenticating callers by default (Forge invocation token → `ForgeAuthentication` / `AtlassianHostUser`); authorization is now opt-in, in the same per-handler, annotation-driven style Atlassian Connect used. See the README section *Authentication vs. authorization, and `@SystemCallOnly`*.
- `@SystemCallOnly` (`bridge-common`): restricts a handler or controller to Forge system calls (`scheduledTrigger` / `trigger` modules). A request is rejected with HTTP 403 when it has no `ForgeAuthentication` or carries a user (invocation token `principal` or `AtlassianHostUser` account id). Works in both `bridge-forge-connect` and `bridge-connect-container`.
- `SystemCallVerifier` for programmatic checks, and `SystemTokenPolicy` as the extension point for system token validation.
- `ForgeSystemTokenPolicy` (`bridge-forge-connect`): validates the system (offline) token from `x-forge-oauth-system` — RS256 signature against the Atlassian JWKS, `iss`, `exp` (clock skew from `app.clockTolerance`), and that `aud` contains the installation id of the invocation token. Configurable with `bridge.forge.system-token.jwks-url` and `bridge.forge.system-token.issuer`.
- Documentation for the above in the README.

### Changed

- **System token validation is always on (`bridge-forge-connect`).** If a request carries a system token it is validated on every endpoint, with or without `@SystemCallOnly`; an invalid, expired or foreign token is answered with HTTP 403 and the security context is cleared. Requests without a system token are not affected. The first validation fetches the Atlassian JWKS (`https://auth.atlassian.com/.well-known/jwks.json`), so outbound access to `auth.atlassian.com` is required.
- `AtlassianForgeFilter` gained a constructor taking a `SystemTokenPolicy` (the single-argument constructor remains and skips token validation).
- Dependencies updated:
  - Spring Boot 3.5.14 → 3.5.16
  - Spring Security 6.5.10 → 6.5.11
  - Tomcat 10.1.55 → 10.1.61
  - Atlassian Connect Spring Boot 6.0.4 → 6.0.8
  - JaCoCo 0.8.14 → 0.8.16
  - SpotBugs Maven plugin 4.9.8.3 → 4.10.5.0
  - Exec Maven plugin 3.6.3 → 3.7.0
  - Central Publishing Maven plugin 0.10.0 → 0.12.0
  - Sonar Maven plugin 5.6.0.6792 → 5.8.0.7211

### Notes

- On Forge Containers the system token stays in the egress sidecar and is not visible to the application, so only the no-user check of `@SystemCallOnly` applies there.
- The sample project under `examples/` now uses `2.0.0-SNAPSHOT` and demonstrates `@SystemCallOnly` on the trigger endpoint. Build the library first (`mvn clean install`) so the sample can resolve it from the local repository until `2.0.0` is released.

## [1.0.2]

See the [GitHub release](https://github.com/forge-sql-orm/atlassian-runtime-bridge/releases/tag/v1.0.2).
