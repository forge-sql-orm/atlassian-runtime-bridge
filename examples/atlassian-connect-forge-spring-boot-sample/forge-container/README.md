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
| Webtrigger | `impersonation-webtrigger` → `GET /api/impersonation` |
| Health check (deployed) | `GET /health` |
| Connect key | `runtime-bridge-spring-boot-sample-atlaskit` (for shared descriptors) |

Tunnel block in `manifest.yml` is commented out for **cloud** deploy (image from ECR). Uncomment for manifest-driven local Docker tunnel if you prefer that flow.

---

## Useful commands

| Task | Command |
|------|---------|
| Run locally | `mvn spring-boot:run` (this module) |
| Package JAR | `mvn package` from sample parent: `-pl forge-container -am` |
| Docker image (local test) | From repo root: `docker build -f examples/.../forge-container/Dockerfile .` |
| Container logs (cloud) | `forge logs --containers` |
| Webtrigger URL | `forge webtrigger` (after deploy) |

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
