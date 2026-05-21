## Summary

<!-- What changed and why (1–3 sentences). Link migration/README impact if relevant. -->

Fixes #<!-- issue number or "n/a" -->

## Type of change

- [ ] Bug fix (non-breaking)
- [ ] New feature / API (non-breaking or breaking — explain below)
- [ ] Documentation only
- [ ] Sample app (`examples/atlassian-connect-forge-spring-boot-sample/`)
- [ ] CI / build / tooling
- [ ] Refactor / tests (no behavior change)

## Modules touched

- [ ] `bridge-common`
- [ ] `bridge-forge-connect` (Connect / Forge Remote)
- [ ] `bridge-connect-container` (Forge Containers)
- [ ] Example / frontend
- [ ] Root docs (`README`, `CONTRIBUTING`, …)
- [ ] None of the above (explain in Summary)

<!-- If both bridge-forge-connect and bridge-connect-container change, explain why in Summary (usually split PRs). -->

## Breaking change

- [ ] No
- [ ] Yes — describe migration steps for consumers

## How tested

<!-- Commands you ran; CI mirrors this. -->

```bash
mvn spotless:apply
mvn clean install
# If examples/ changed:
mvn clean install -f examples/atlassian-connect-forge-spring-boot-sample/pom.xml
```

- [ ] `mvn spotless:apply` + `mvn clean install` at repo root
- [ ] Sample build (if `examples/` changed)
- [ ] New or updated unit tests added for code changes
- [ ] Javadoc / README updated when public behavior or setup changed

## Checklist

- [ ] PR targets `main` and has a focused scope (one topic when possible)
- [ ] No secrets, real `app.id`, production URLs, or personal deploy scripts committed
- [ ] I read [CONTRIBUTING.md](../CONTRIBUTING.md)
