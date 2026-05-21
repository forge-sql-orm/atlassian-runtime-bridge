# Contributing to atlassian-runtime-bridge

Thank you for your interest in this project. Contributions — bug reports, documentation, tests, and code — are welcome.

Please read this guide before opening a pull request. For migration context and architecture, start with the [README](README.md).

## Code of conduct

This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you agree to uphold it. Report concerns to **vaszakharchenko@gmail.com**.

## Ways to contribute

- **Issues** — use the [GitHub issue templates](.github/ISSUE_TEMPLATE/) (bug, feature, documentation, migration help). Blank issues are disabled; security reports use [SECURITY.md](SECURITY.md) only.
- **Bug reports** — reproducible steps, expected vs actual behavior, runtime (Connect hybrid, Forge Remote, or Forge Containers), JDK/Maven versions.
- **Documentation** — README, sample module READMEs, Javadoc, migration notes.
- **Tests** — especially for `bridge-forge-connect` and `bridge-connect-container` behavior you change.
- **Sample app** — changes under `examples/atlassian-connect-forge-spring-boot-sample/` should stay aligned with the documented migration paths.
- **Features** — open an issue first for larger changes (new adapters, breaking API changes, new modules).

## Project layout

| Path | Purpose |
|------|---------|
| `bridge-common/` | Shared interfaces (`JiraProductAdapter`, `ManualAuthorizationService`, …) |
| `bridge-forge-connect/` | Connect + Forge Remote / hybrid runtime |
| `bridge-connect-container/` | Forge Containers runtime (egress sidecar, ingress filters) |
| `examples/atlassian-connect-forge-spring-boot-sample/` | End-to-end sample (not published to Maven Central) |

**Important:** `bridge-forge-connect` and `bridge-connect-container` target **different** deployments. Do not add both to the same application module or classpath. PRs that touch both should explain why (usually they should be split).

## Development environment

- **JDK 21** (see `java.version` in the root `pom.xml`)
- **Maven 3.9+**
- For sample frontend work: **Node.js** + npm (used by `examples/.../frontend` during Maven build)

## Build and test

From the repository root:

```bash
# Format sources (run before committing)
mvn spotless:apply

# Full library build: tests + verify (Checkstyle, SpotBugs, PMD, Spotless check, JaCoCo)
mvn clean install

# Single module (faster iteration)
mvn -pl bridge-forge-connect test
mvn -pl bridge-connect-container test
```

Build the sample (resolves the bridge from Maven Central — no local `install` required, unless you are testing unreleased changes from `bridge-*` modules):

```bash
mvn clean install -f examples/atlassian-connect-forge-spring-boot-sample/pom.xml
```

CI runs the same gates as above — see [`.github/workflows/build.yml`](.github/workflows/build.yml).

### Skipping checks (local only)

Use sparingly; CI does not skip these by default.

```bash
mvn verify -Dspotless.skip=true
mvn verify -Dcheckstyle.skip=true
mvn verify -Dspotbugs.skip=true
mvn verify -Dpmd.skip=true
mvn compile -Derrorprone.skip=true
```

### Git hooks (optional)

A **pre-commit** hook in [`.githooks/`](.githooks/) runs Spotless, full `mvn clean install` for libraries, then the sample build. It is installed automatically on `mvn` from the repo root (`scripts/install-git-hooks.sh`), or manually:

```bash
./scripts/install-git-hooks.sh
```

Bypass for one commit: `SKIP_HOOKS=1 git commit …`

## Code style

- **Java formatting:** [Spotless](https://github.com/diffplug/spotless) with Google Java Format (`mvn spotless:apply`).
- **Static analysis:** Checkstyle, SpotBugs (`config/spotbugs/exclude-filter.xml`), PMD, [Error Prone](https://errorprone.info/) on compile.
- **Tests:** JUnit 5; keep tests focused on real behavior. JaCoCo line coverage is enforced on library modules during `verify`.
- **Javadoc:** Public API in bridge modules should stay documented; match existing tone in the package you edit.

## Pull requests

1. Fork the repository and create a branch from `main`.
2. Make focused changes; one logical topic per PR when possible.
3. Run `mvn spotless:apply` and `mvn clean install` at the root (and the sample build if you touched `examples/`).
4. Add or update tests for code changes.
5. Update README or module docs if behavior or setup changes.
6. Open a PR against `main` using the [pull request template](.github/pull_request_template.md) (summary, modules, tests, checklist).

Maintainers will review for correctness, migration story consistency, and CI green status.

## Commit messages

Prefer clear, imperative subjects. Examples:

- `fix bridge-forge-connect: handle blank accountId in manual auth`
- `docs: clarify Connect → Forge Remote step 3 in README`
- `test: add egress URL join cases for trailing slash`

No strict convention required; readable history is enough.

## Security

Do not open public issues for security vulnerabilities. See [SECURITY.md](SECURITY.md) for how to report and what is in scope.

## License

By contributing, you agree that your contributions will be licensed under the same license as the project — see [LICENSE](LICENSE) (MIT).

## Questions

- **Usage / migration:** [README](README.md), [sample README](examples/atlassian-connect-forge-spring-boot-sample/README.md), [forge-container README](examples/atlassian-connect-forge-spring-boot-sample/forge-container/README.md)
- **Security:** [SECURITY.md](SECURITY.md)
- **CoC / conduct:** [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- **Everything else:** [GitHub Issues](https://github.com/forge-sql-orm/atlassian-runtime-bridge/issues/new/choose) (pick a template) or **vaszakharchenko@gmail.com**
