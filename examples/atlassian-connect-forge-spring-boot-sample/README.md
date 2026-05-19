# Atlassian Connect + Forge (single Spring Boot backend)

One codebase for **Connect** (Jira iframe) and **Forge** (Custom UI + remote backend): shared UI and Jira calls; only the transport to your backend and the authorization model for Jira API calls differ.

The sample is a **Maven multi-module** project:

| Module | Artifact | Role |
|--------|----------|------|
| **`frontend/`** | `sample-frontend` | TypeScript/React sources, Thymeleaf templates, Forge `customUI` shell; `npm run build` produces Connect `bundle.js` (classpath) and Forge `customUI/` (for `forge deploy`). |
| **`core/`** | `sample-core` | Shared business logic: `HelloWorldController`, Jira DTOs, `atlassian-connect.json`. |
| **`forge-connect/`** | `sample-forge-connect` | **Hybrid** runnable app: Connect lifecycle + iframe UI **and** Forge Custom UI / remotes. `manifest.yml` includes `connectModules` and `app.connect` with JWT. |
| **`forge-remote/`** | `sample-forge-remote` | **Forge-only** deployment descriptor: same backend as `forge-connect`, but manifest has **no** `connectModules` — the add-on is fully migrated off Connect UI and lifecycle in Forge. |
| **`forge-container/`** | `sample-forge-container` | **Forge Containers**: Spring runs **in a container** on Atlassian infrastructure; uses `bridge-connect-container` (egress sidecar, ingress headers). See **[`forge-container/README.md`](forge-container/README.md)**. |

### Hybrid vs Forge-only vs Forge Container

| | **`forge-connect`** | **`forge-remote`** | **`forge-container`** |
|---|---------------------|-------------------|------------------------|
| Target audience | Coexist Connect + Forge during migration | Production shape after Connect → Forge migration | Spring on **Forge Containers** (EAP) |
| Jira UI | Connect iframe **and** Forge Custom UI | Forge Custom UI only | Forge Custom UI only |
| `connectModules` | Yes | **Removed** | **Removed** |
| Backend wiring | `remotes.baseUrl` → your HTTPS URL | `remotes.baseUrl` → your URL | **`services`** + container image in ECR |
| Custom UI build output | `./customUI` | `../forge-connect/customUI` | `./customUI` (via `npm run build:container`) |
| Spring Boot | `mvn spring-boot:run` in **`forge-connect/`** | Same JVM as hybrid | `mvn spring-boot:run` in **`forge-container/`** |
| Deploy docs | This README | [`forge-remote/manifest.yml`](forge-remote/manifest.yml) | **[`forge-container/README.md`](forge-container/README.md)** |

Both manifests share the same **`app.id`**, triggers (`scheduledTrigger`, install/upgrade → `POST /system/sync`), and endpoint paths on the Spring app. Only the Forge-side wiring and Connect module surface differ.

Run **Spring Boot** from **`forge-connect/`** (hybrid / forge-remote backend) or **`forge-container/`** (Containers). Run the **Forge CLI** from the module whose `manifest.yml` you deploy.

**Forge Container quick start:** [`forge-container/README.md`](forge-container/README.md) — local: `mvn spring-boot:run` + `./dev-loop.sh`; cloud: copy `build-and-deploy.sh.example` → `build-and-deploy.sh` (not committed).

---

## Prerequisites

- [Forge CLI](https://developer.atlassian.com/platform/forge/getting-started/) (run `forge login`)
- JDK 21+ and Maven 3.9+
- Node.js + npm (frontend build)
- A public **HTTPS** URL for your Spring app (e.g. [ngrok](https://ngrok.com/)) — use the **same** URL in `manifest.yml` (`remotes[].baseUrl`) and in `application.yml` (`addon.base-url`)

---

## Install and run (step by step)

### 1. Register the Forge app

From **`forge-connect/`** (hybrid) or **`forge-remote/`** (Forge-only) — one registration per `app.id`:

```bash
cd examples/atlassian-connect-forge-spring-boot-sample/forge-connect
# or: cd .../forge-remote
forge register
```

Follow the CLI prompts (developer space, app name, etc.). Atlassian will assign an **app id** in ARI form.

### 2. Copy `app.id` into Spring

1. Open [`forge-connect/manifest.yml`](forge-connect/manifest.yml) and read **`app.id`** (under the top-level `app:` key).
2. Paste the same value into [`forge-connect/src/main/resources/application.yml`](forge-connect/src/main/resources/application.yml) as **`app.id`**: the bridge reads `${app.id}` (e.g. when building Forge invocation context).

**`app.id` must match between the manifest and `application.yml`.**

### 3. Point everything at your public backend URL

- In the manifest you deploy (**[`forge-connect/manifest.yml`](forge-connect/manifest.yml)** or **[`forge-remote/manifest.yml`](forge-remote/manifest.yml)**): `remotes` → `baseUrl` = your Spring app’s HTTPS origin.
- In **`forge-connect/src/main/resources/application.yml`**: `addon.base-url` = the same URL (Spring and Forge remotes must hit the **same** instance).

### 4. Build the project

From the **sample parent** (builds `frontend` → `core` → `forge-connect`):

```bash
cd examples/atlassian-connect-forge-spring-boot-sample
mvn clean install
```

The **`frontend`** module runs `npm run build` during `generate-resources`:

- **Connect**: `frontend/target/classes/static/bundle.js` → packaged in `sample-frontend` JAR (on the Spring classpath).
- **Forge**: `forge-connect/customUI/bundle.js` + `index.html` (used by `forge deploy`; path in manifest: `./customUI`).

### 5. Run Spring Boot

From **`forge-connect`**:

```bash
cd forge-connect
mvn spring-boot:run
```

Or from the parent with `-pl`:

```bash
mvn spring-boot:run -pl forge-connect
```

Confirm the app is reachable at the URL you set in `addon.base-url` / `remotes.baseUrl`.

### 6. Deploy and install Forge

**Hybrid (Connect + Forge)** — from [`forge-connect/`](forge-connect/):

```bash
cd forge-connect
forge deploy
forge install
```

**Forge-only (post-migration)** — from [`forge-remote/`](forge-remote/) after the same `mvn clean install` and with Spring still running from `forge-connect`:

```bash
cd forge-remote
forge deploy
forge install
```

Use **one** manifest per Forge app installation on a site (do not deploy both manifests to the same app id unless you intend to replace the hybrid deployment).

Pick the product (Jira), environment, and site.

- **Hybrid**: Jira exposes **Connect** entry points (`connectModules`) and **Forge** modules (`modules`), both targeting the **same** Spring backend.
- **Forge-only**: only Forge modules — no Connect general page or Connect lifecycle URLs in the manifest; users open the app via **Apps** → Forge global page (Custom UI).

### 7. Verify in Jira

**Hybrid (`forge-connect` manifest)**

1. Open Jira on the site where you installed the app.
2. **Connect**: header item from `jira:generalPages` (e.g. “Atlaskit”) — iframe loads your Spring; JWT via Connect.
3. **Forge**: **Apps** → your app with the Forge global page — Custom UI; calls to your Spring use the remote (`@forge/bridge`) with Forge auth.

**Forge-only (`forge-remote` manifest)**

1. There is **no** Connect navigation item from `connectModules`.
2. **Apps** → your app → Forge global page (Custom UI) — same React bundle as hybrid (`forge-connect/customUI`), remote key `forge-remote` in the manifest.

Both deployment modes share **one** Spring codebase and frontend repo; the differences:

| | Connect (iframe) | Forge (Custom UI) |
|---|------------------|-------------------|
| Backend call | Direct `fetch` from the browser to your origin | `requestRemote` from `@forge/bridge` to the same remote |
| Jira from backend | Connect / user context (as wired in `JiraProductAdapter`) | Forge: impersonation / offline system token, etc. |

For token refresh details, see `/api/impersonation` below and `manifest.yml` (`scheduledTrigger` / `trigger`).

---

## Public `/api/impersonation` endpoint (development)

In the browser (for local debugging only — **do not expose without protection in production**):

```text
http://localhost:8080/api/impersonation?accountId=<ACCOUNT_ID>&cloudId=<CLOUD_ID>&installationId=<INSTALLATION_ID>
```

The backend establishes host context and calls Jira **`GET /rest/api/3/myself`** as the given user, using a **Forge system / offline token** that must already be stored in your DB (see below).

### Where to get query parameters

- **`accountId`** — Atlassian account id (e.g. `557058:…` or UUID). In Jira: open **Profile / View profile** and read `accountId=...` from the profile URL (or the user link).
- **`cloudId`** — cloud / site id. While logged into Atlassian, open:

  `https://<your-site>.atlassian.net/_edge/tenant_info`

  In the JSON response, find the cloud identifier and use it as `cloudId`.

- **`installationId`** — Forge/Connect installation id for your app on that site. From the CLI:

  ```bash
  forge install list
  ```

  Find the installation id for the site and app you care about.

### System / offline token

This endpoint assumes a **valid Forge system access token** for the installation (lifetime is typically on the order of **a few hours**; Atlassian’s docs often cite ~4 hours — confirm against current platform policy).

The token must be refreshed periodically or Jira calls will start failing. In this sample, refresh is **driven** by:

1. **`scheduledTrigger`** in `manifest.yml` — periodic call to remote `system-token-sync` → Spring `POST /system/sync` ([`ForgeController`](forge-connect/src/main/java/sample/connect/spring/atlaskit/ForgeController.java)).
2. **`trigger`** on `avi:forge:installed:app` and `avi:forge:upgraded:app` — same endpoint after install/upgrade.
3. Any **Custom UI traffic** over `forge-remote` to your backend — your Connect Spring stack may persist/update whatever is needed for later calls (implement in `ForgeController` / services).

The sample `POST /system/sync` only returns the string `synced`; a real app should persist refreshed tokens there.

---

## Frontend: Connect and Forge

Sources: [`frontend/src/main/resources/javascript/`](frontend/src/main/resources/javascript/).

- Shared: `main.ts`, `shared/helloBackend.ts`, `shared/transportContract.ts`, etc.
- Transport split: **`connect.ts`** vs **`forge.ts`**; the build swaps the **`app-transport`** alias (same import in code, different implementation).

### Commands (from `frontend/`)

| Command | Purpose |
|--------|---------|
| `npm install` | Dependencies (`@forge/bridge`, React, esbuild, …). |
| `npm run build:connect` | **Connect bundle** → `frontend/target/classes/static/bundle.js` (included in `sample-frontend` JAR). |
| `npm run build:forge` | **Forge Custom UI** → `forge-connect/customUI/bundle.js` + copied `index.html`. |
| `npm run build` | Both bundles. Invoked from Maven (`frontend` module, `generate-resources`). |
| `npm run watch` | Rebuild **Connect** bundle on file changes (iframe dev loop). |
| `npm run typecheck` | `tsc --noEmit` only. |

Connect page template: [`frontend/src/main/resources/templates/atlaskit.html`](frontend/src/main/resources/templates/atlaskit.html).  
Forge Custom UI shell: [`frontend/src/main/resources/customUI/`](frontend/src/main/resources/customUI/) → build output under **`forge-connect/customUI/`**.

---

## Useful files

| File | Role |
|------|------|
| [`forge-connect/manifest.yml`](forge-connect/manifest.yml) | **Hybrid**: `connectModules`, `app.connect.authentication: jwt`, remote `connect`, Forge `modules` + triggers. |
| [`forge-remote/manifest.yml`](forge-remote/manifest.yml) | **Forge-only**: no `connectModules`; remote `forge-remote`; Custom UI resource points at `../forge-connect/customUI`. |
| [`forge-container/README.md`](forge-container/README.md) | **Forge Containers**: local dev (`dev-loop.sh`), Docker/ECR deploy, manifest `java-service`. |
| [`forge-connect/src/main/resources/application.yml`](forge-connect/src/main/resources/application.yml) | `app.id`, datasource, `addon.base-url` (used by Spring for **both** deployment modes). |
| [`core/src/main/resources/atlassian-connect.json`](core/src/main/resources/atlassian-connect.json) | Connect descriptor (still on classpath for hybrid; unused by Forge-only UI surface). |

---

## Checklist before first `forge deploy`

- [ ] `forge register` done in **`forge-connect/`** or **`forge-remote/`** (same `app.id` in both manifests)
- [ ] `app.id` matches in the manifest you deploy and in `forge-connect/.../application.yml`
- [ ] `remotes[].baseUrl` (in that manifest) and `addon.base-url` point at your Spring **HTTPS** URL
- [ ] `mvn clean install` succeeded from the sample parent (including `npm run build` in `frontend`)
- [ ] Spring is running from **`forge-connect`** and reachable from the internet at that URL
- [ ] `forge deploy` and `forge install` run from **`forge-connect/`** (hybrid) **or** **`forge-remote/`** (Forge-only)

After **hybrid** deploy, Jira shows Connect and Forge entry points. After **forge-remote** deploy, only the Forge Custom UI path remains — same backend, migration-complete manifest.
