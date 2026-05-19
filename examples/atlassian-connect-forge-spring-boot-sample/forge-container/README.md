# Forge Container sample (`forge-container`)

Spring Boot app that runs **on Atlassian Forge Containers** (containerised service + egress proxy), using [`bridge-connect-container`](../../../../bridge-connect-container) instead of Connect JPA / hybrid `bridge-forge-connect`.

Shared business logic lives in **`core/`**; Custom UI is built by **`frontend/`** into `customUI/` (same bundle as `forge-connect`).

| Compared to | Transport | Spring module |
|-------------|-----------|---------------|
| **`forge-connect`** | Connect iframe JWT + Forge remote to your URL | Full Connect starter + `bridge-forge-connect` |
| **`forge-remote`** | Forge remote only (`remotes.baseUrl`) | Same JVM as `forge-connect` |
| **`forge-container`** (here) | Forge routes to **your container**; Jira via egress sidecar | `bridge-connect-container` only |

---

## Prerequisites

- [Forge CLI](https://developer.atlassian.com/platform/forge/getting-started/) (`forge login`)
- Access to **Forge Containers** (EAP) for your app
- JDK 21+, Maven 3.9+, Docker
- Node.js + npm (for Custom UI — see parent [README](../README.md))
- Forge app registered from this directory (`forge register`)

---

## One-time setup

### 1. Register the app (from this directory)

```bash
cd examples/atlassian-connect-forge-spring-boot-sample/forge-container
forge register
```

Copy **`app.id`** from `manifest.yml` into [`.env.example`](.env.example) → `.env` as `APP_ID`.

### 2. Create the container repository in Forge ECR

The ECR image name must match **`containers[].key`** in `manifest.yml` (this sample uses `java-service`):

```bash
forge containers create -k java-service
```

Note the repository URI, e.g. `forge-ecr.services.atlassian.com/forge/<APP_UUID>/java-service`.

### 3. Build Custom UI

From the sample parent (or `frontend/`):

```bash
cd examples/atlassian-connect-forge-spring-boot-sample
mvn clean install -pl frontend -am
# or: cd frontend && npm run build:container
```

This produces `forge-container/customUI/` (`bundle.js` + `index.html`) referenced by `manifest.yml`.

### 4. Install bridge libraries locally

```bash
cd /path/to/atlassian-runtime-bridge
mvn install -pl bridge-common,bridge-connect-container -DskipTests \
  -Dspotbugs.skip=true -Dpmd.skip=true -Dcheckstyle.skip=true -Dspotless.skip=true -Djacoco.skip=true
```

---

## Local development (`dev-loop.sh`)

Typical loop: **Spring on the host** + **platform proxy sidecar in Docker** + **`forge tunnel`**.

### 1. Configure `.env`

```bash
cp .env.example .env
```

| Variable | Source |
|----------|--------|
| `APP_ID` | `manifest.yml` → `app.id` (UUID only, or full ARI — see below) |
| `ENV_ID` | Forge environment UUID (`forge install list` or developer console) |
| `ENV_NAME` | Forge CLI environment name (e.g. `development`) |
| `ENABLE_DEBUG` | Optional remote debug on port `8000` when using full Docker image |

For the sidecar, `APP_ID` / `ENV_ID` in `.env` are expanded into ARIs in `docker-compose.yml`.

### 2. Build and run Spring (host)

```bash
cd examples/atlassian-connect-forge-spring-boot-sample/forge-container
mvn spring-boot:run
```

- App listens on **8080** (or `SERVER_PORT` from Forge when deployed).
- Egress proxy URL defaults to `http://localhost:7072` ([`application.yaml`](src/main/resources/application.yaml)).

Health (local / platform): **`GET /health`** → `OK` ([`ContainerHealthEndpoint`](src/main/java/sample/connect/spring/atlaskit/ContainerHealthEndpoint.java)).

Only `/health` is exposed without authentication by default (`ContainerWebSecurityConfiguration`). If your app uses different liveness/readiness endpoints (Spring Boot Actuator, internal probes, public API paths), override them in `application.yaml`:

```yaml
bridge:
  container:
    security:
      public-paths:
        - /health
        - /actuator/health/**
        - /api/public/**
```

The list **replaces** the default — include `/health` if you still need it.

### 3. Start sidecar + tunnel

```bash
./dev-loop.sh
```

What it does:

1. `forge containers docker-login`
2. Pulls `forge-platform/proxy-sidecar`
3. `docker compose up -d` — sidecar forwards egress to `http://host.docker.internal:8080`
4. `forge tunnel -e "$ENV_NAME"` — routes Forge traffic to your local stack

Open Jira → **Apps** → **Atlaskit (Forge Container)**.

To stop: Ctrl+C (compose is torn down via trap).

### Manual compose (without tunnel)

```bash
# Terminal 1
mvn spring-boot:run

# Terminal 2
docker compose up
```

Sidecar ports: **7071** / **7072** (see `docker-compose.yml`).

---

## Cloud deploy (image push + `forge deploy`)

`build-and-deploy.sh` is **local only** (not in git — copy from [`build-and-deploy.sh.example`](build-and-deploy.sh.example)). It builds the image from the **repository root**, pushes to Forge ECR, sets `TAG`, and deploys.

### 1. Prepare the script

```bash
cp build-and-deploy.sh.example build-and-deploy.sh
chmod +x build-and-deploy.sh
# Edit APP_ID, FORGE_ENV, CONTAINER_KEY if needed
```

### 2. Run deploy

```bash
FORGE_ENV=your-forge-environment ./build-and-deploy.sh
```

Equivalent manual steps:

```bash
# From repository root
export TAG=$(date +%s)
export APP_ID=<your-app-uuid>
export CONTAINER_KEY=java-service
export REPO_URI="forge-ecr.services.atlassian.com/forge/${APP_ID}/${CONTAINER_KEY}"

forge containers docker-login
docker build --platform linux/amd64 \
  -f examples/atlassian-connect-forge-spring-boot-sample/forge-container/Dockerfile \
  -t "${REPO_URI}:${TAG}" \
  .
docker push "${REPO_URI}:${TAG}"

cd examples/atlassian-connect-forge-spring-boot-sample/forge-container
forge variables set TAG "$TAG" -e "$FORGE_ENV"
forge deploy -e "$FORGE_ENV"
forge install --upgrade -e "$FORGE_ENV"
```

`manifest.yml` uses `tag: ${TAG}` on the container image; `forge variables set TAG` must run before deploy.

---

## Manifest overview

| Piece | This sample |
|-------|-------------|
| Service / container key | `java-service` |
| Custom UI | `./customUI` |
| Global page | `java-service-ui` → `GET /atlaskit/api/hello` |
| Webtrigger | `impersonation-webtrigger` → `GET /api/impersonation` (only entry point — see [Calling `/api/impersonation` via webtrigger](#calling-apiimpersonation-via-webtrigger)) |
| Health check (deployed) | `GET /health` |
| Connect key | `runtime-bridge-spring-boot-sample-atlaskit` (for shared descriptors) |

Tunnel block in `manifest.yml` is commented out for **cloud** deploy (image from ECR). Uncomment for manifest-driven local Docker tunnel if you prefer that flow.

---

## Calling `/api/impersonation` via webtrigger

The container itself is **not** internet-routable — Forge platform fronts every request. The `@IgnoreJwt` endpoint `GET /api/impersonation` ([`BusinessLogicController`](../core/src/main/java/sample/connect/spring/atlaskit/BusinessLogicController.java)) is exposed by the `impersonation-webtrigger` module in [`manifest.yml`](manifest.yml#L40-L42), so the only way to hit it is the Forge-issued webtrigger URL.

### 1. Create the webtrigger URL (once per environment)

```bash
forge webtrigger create -e "$FORGE_ENV"
```

Pick `impersonation-webtrigger` when prompted. Forge prints a URL of the form:

```
https://<app-id>.hello.atlassian-dev.net/x1/<webtrigger-token>
```

- `<app-id>` matches `manifest.yml` → `app.id` UUID (this sample: `cbad754b-3ea4-4759-a6ef-eb575b1a7427`).
- `<webtrigger-token>` is opaque, environment-scoped, and **does not authenticate the caller** — treat it as a capability URL.
- Reuse it across browser tabs; revoke / rotate via `forge webtrigger delete`.

List existing URLs at any time:

```bash
forge webtrigger -e "$FORGE_ENV"
```

### 2. Invoke it from the browser

Append the impersonation parameters the controller expects:

```
https://<app-id>.hello.atlassian-dev.net/x1/<webtrigger-token>?accountId=<accountId>&cloudId=<cloudId>&installationId=<installationId>
```

| Query param | How to find it |
|-------------|----------------|
| `accountId` | Jira user atlassian-account-id you want to impersonate (`/rest/api/3/myself` on a Connect/Forge install) |
| `cloudId` | Cloud site id of the tenant where the app is installed (`/_edge/tenant_info`) |
| `installationId` | Forge installation id for that tenant — `forge install list` shows it; the value is also persisted by the sample on install (no `ari:cloud:ecosystem::installation/` prefix needed — the controller adds it) |

Forge platform routes the request to the container's `/api/impersonation` endpoint, which calls `manualAuthorizationService.authorize(host)` and then `JiraProductAdapter.impersonation(...)` to hit `/rest/api/3/myself` as that user.

### 3. Tenant isolation

[`ManualAuthorizationService.authorize(...)`](../../../bridge-connect-container/src/main/java/com/github/vzakharchenko/runtime/bridge/containers/ManualAuthorizationServiceImpl.java) **always** verifies that the requested `cloudId` matches the one already in the security context (set by `ContainerAuthorizationFilter` from the inbound Forge invocation token). Cross-tenant calls fail fast:

```
IllegalStateException: Cross tenant authorization is not allowed:
  expected cloudId=<tenant-A>, received cloudId=<tenant-B>
```

In practice this means you cannot use a webtrigger URL minted in **tenant A** to authorize Jira REST calls against **tenant B**, even though the URL is publicly reachable. The check runs on every `authorize(...)` overload — `AtlassianHostUser`, `AtlassianHost`, and `(cloudId, installationId, accountId)` — and applies equally to background threads (schedulers, queues) that re-authorize after a context switch.

> Webtrigger URLs do not authenticate the caller, so don't expose this endpoint to production users without an extra layer (network restriction, shared secret in a custom header, Forge FIT, etc.). The tenant guard prevents tenant-confusion attacks but not arbitrary impersonation **within** the same tenant.

---

## Useful commands

| Task | Command |
|------|---------|
| Run locally | `mvn spring-boot:run` (this module) |
| Package JAR | `mvn package` from sample parent: `-pl forge-container -am` |
| Docker image (local test) | From repo root: `docker build -f examples/.../forge-container/Dockerfile .` |
| Container logs (cloud) | `forge logs --containers` |
| Webtrigger URL — create | `forge webtrigger create -e "$FORGE_ENV"` (pick `impersonation-webtrigger`) |
| Webtrigger URL — list/revoke | `forge webtrigger -e "$FORGE_ENV"` / `forge webtrigger delete` |

---

## Files in this module

| File | Role |
|------|------|
| [`manifest.yml`](manifest.yml) | Forge Containers app + modules |
| [`Dockerfile`](Dockerfile) | Multi-stage build from **repo root** |
| [`docker-compose.yml`](docker-compose.yml) | Local **proxy sidecar** only |
| [`dev-loop.sh`](dev-loop.sh) | Sidecar + `forge tunnel` |
| [`.env.example`](.env.example) | Template for local sidecar / tunnel |
| [`build-and-deploy.sh.example`](build-and-deploy.sh.example) | Template for ECR push + deploy (copy to `build-and-deploy.sh`) |
| [`src/.../AddonApplication.java`](src/main/java/sample/connect/spring/atlaskit/AddonApplication.java) | Entry point |

---

## See also

- Parent sample overview: [../README.md](../README.md)
- Forge Containers docs: [Containers reference](https://developer.atlassian.com/platform/forge/containers-reference/)
