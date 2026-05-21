# Security policy

## Supported versions

Security fixes are applied to the **latest release** on the `main` branch and published to [Maven Central](https://central.sonatype.com/search?q=g:com.github.vzakharchenko) as a new patch/minor version when appropriate.

| Version | Supported |
|---------|-----------|
| Latest `1.0.x` (or current minor on `main`) | Yes |
| Older releases | No — upgrade to the latest release |

The sample under `examples/atlassian-connect-forge-spring-boot-sample/` is demonstration code only; it is not versioned or published as a library artifact.

## Reporting a vulnerability

**Please do not report security vulnerabilities through public GitHub Issues.**

Send a private report to:

**vaszakharchenko@gmail.com**

Include as much detail as you can:

- Description of the issue and potential impact
- Steps to reproduce (code snippet, configuration, or minimal project if possible)
- Affected module (`bridge-common`, `bridge-forge-connect`, `bridge-connect-container`, or sample)
- Library version or commit SHA
- Runtime context (Connect hybrid, Forge Remote, Forge Containers) if relevant
- Your contact (optional), if you want a reply

## What we will do

1. Acknowledge receipt within a reasonable time (typically a few business days).
2. Investigate and confirm the issue.
3. Prepare a fix on a private branch when warranted.
4. Release a patched version and document the advisory (GitHub Security Advisory or release notes), crediting reporters who wish to be named.

## Scope

**In scope**

- Security defects in this repository’s library code (authentication/authorization handling, egress/ingress clients, filters, host merge/enrichment, and related APIs).
- Unsafe defaults or missing validation in the bridge that could affect consumers.

**Out of scope**

- Vulnerabilities in **your** application built on top of the bridge (deployment, secrets, network exposure, custom controllers).
- Issues in **Atlassian Connect Spring Boot**, **Forge**, **Jira/Confluence**, or third-party dependencies — report those to the respective vendors.
- Misconfiguration of Forge manifests, tunnels, ngrok URLs, or container images in the sample app.
- Denial-of-service or rate limiting unless caused by a clear defect in the bridge itself.

## Secure development

Contributors: see [CONTRIBUTING.md](CONTRIBUTING.md). Do not commit secrets, `.env` files with real credentials, or production `app.id` / tokens. The sample uses placeholders; keep local deploy scripts (e.g. `build-and-deploy.sh`) out of git when they contain account-specific values.

## Coordinated disclosure

We prefer coordinated disclosure. Please allow time for a fix before public discussion. We will work with you on a reasonable disclosure timeline once the issue is understood.
