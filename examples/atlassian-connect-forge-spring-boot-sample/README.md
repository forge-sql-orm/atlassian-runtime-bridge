# Atlassian Connect + Forge (single Spring Boot backend)

One codebase for **Connect** (Jira iframe) and **Forge** (Custom UI + `forge-remote` to the same URL): shared UI and Jira calls; only the transport to your backend and the authorization model for Jira API calls differ.

---

## Prerequisites

- [Forge CLI](https://developer.atlassian.com/platform/forge/getting-started/) (run `forge login`)
- JDK 21+ and Maven 3.9+
- Node.js + npm (frontend build)
- A public **HTTPS** URL for your Spring app (e.g. [ngrok](https://ngrok.com/)) — use the **same** URL in `manifest.yml` (`remotes[].baseUrl`) and in `application.yml` (`addon.base-url`)

---

## Install and run (step by step)

### 1. Register the Forge app

From the directory that contains `manifest.yml`:

```bash
cd examples/atlassian-connect-forge-spring-boot-sample
forge register
```

Follow the CLI prompts (developer space, app name, etc.). Atlassian will assign an **app id** in ARI form.

### 2. Copy `app.id` into Spring

1. Open [`manifest.yml`](manifest.yml) and read **`app.id`** (under the top-level `app:` key).
2. Paste the same value into [`src/main/resources/application.yml`](src/main/resources/application.yml) as **`app.id`**: the bridge reads `${app.id}` (e.g. when building Forge invocation context).

**`app.id` must match between the manifest and `application.yml`.**

### 3. Point everything at your public backend URL

- In **`manifest.yml`**: `remotes` → `baseUrl` = your Spring app’s HTTPS origin (same host Connect and Forge use).
- In **`application.yml`**: `addon.base-url` = the same URL (Connect descriptor and Forge remote must hit the **same** instance).

### 4. Build the project

From **this** sample module (or from the repo root with `-pl`):

```bash
mvn clean install
```

The Maven build runs npm `prebuild`: dependencies install and **both** Connect and Forge bundles are produced (see [Frontend](#frontend-connect-and-forge)).

### 5. Run Spring Boot

From your IDE or:

```bash
mvn spring-boot:run
```

Confirm the app is reachable at the URL you set in `addon.base-url` / `remotes.baseUrl`.

### 6. Deploy and install Forge

```bash
forge deploy
forge install
```

Pick the product (Jira), environment, and site. After install, Jira will expose **Connect** entry points (`connectModules`) and **Forge** modules (`modules`), both targeting the **same** backend via `forge-remote` / Connect base URL.

### 7. Verify in Jira

1. Open Jira on the site where you installed the app.
2. **Connect**: header item from `jira:generalPages` (e.g. “Atlaskit”) — iframe loads your Spring; JWT via Connect.
3. **Forge**: **Apps** → your app with the Forge global page — Custom UI; calls to your Spring use the **remote** path (`@forge/bridge`) with Forge auth (app user / system token as declared in the manifest).

Both paths share **one** UI/logic repo; the differences:

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

1. **`scheduledTrigger`** in `manifest.yml` — periodic call to remote `system-token-sync` → Spring `POST /system/sync` ([`ForgeController`](src/main/java/sample/connect/spring/atlaskit/ForgeController.java)).
2. **`trigger`** on `avi:forge:installed:app` and `avi:forge:upgraded:app` — same endpoint after install/upgrade.
3. Any **Custom UI traffic** over `forge-remote` to your backend — your Connect Spring stack may persist/update whatever is needed for later calls (implement in `ForgeController` / services).

The sample `POST /system/sync` only returns the string `synced`; a real app should persist refreshed tokens there.

---

## Frontend: Connect and Forge

Sources: [`src/main/resources/javascript/`](src/main/resources/javascript/).

- Shared: `main.ts`, `shared/helloBackend.ts`, `shared/transportContract.ts`, etc.
- Transport split: **`connect.ts`** vs **`forge.ts`**; the build swaps the **`app-transport`** alias (same import in code, different implementation).

### Commands

| Command | Purpose |
|--------|---------|
| `npm install` | Dependencies (`@forge/bridge`, React, esbuild, …). |
| `npm run build:connect` | **Connect bundle**: entry `main.ts` → `target/classes/static/bundle.js` (IIFE, esbuild). Alias `app-transport` → **`connect.ts`**: plain **`window.fetch`** to your Spring with Connect JWT. |
| `npm run build:forge` | **Forge Custom UI**: entry `forgeEntry.ts` → `customUI/bundle.js` + copied `index.html`. Alias `app-transport` → **`forge.ts`**: **`@forge/bridge`** (`requestRemote`) to remote `connect`. |
| `npm run build` | Both bundles (`build:connect` then `build:forge`). Invoked from Maven via `prebuild` / `exec-maven-plugin`. |
| `npm run watch` | Rebuild **Connect** bundle on file changes (iframe dev loop). |
| `npm run typecheck` | `tsc --noEmit` only. |

Connect page template: [`src/main/resources/templates/atlaskit.html`](src/main/resources/templates/atlaskit.html).  
Forge Custom UI: [`src/main/resources/customUI/`](src/main/resources/customUI/) plus output under module root `customUI/` for `forge deploy`.

---

## Useful files

| File | Role |
|------|------|
| [`manifest.yml`](manifest.yml) | Hybrid Forge + Connect: `app.id`, remotes, `jira:globalPage`, `scheduledTrigger`, `trigger`, endpoints. |
| [`src/main/resources/application.yml`](src/main/resources/application.yml) | `app.id`, datasource, `addon.base-url`. |
| [`src/main/resources/atlassian-connect.json`](src/main/resources/atlassian-connect.json) | Connect descriptor (module keys, etc.). |

---

## Checklist before first `forge deploy`

- [ ] `forge register` done; `app.id` matches in manifest and `application.yml`
- [ ] `remotes[].baseUrl` and `addon.base-url` point at your Spring **HTTPS** URL
- [ ] `mvn clean install` succeeded (including `npm run build`)
- [ ] Spring is running and reachable from the internet at that URL
- [ ] `forge deploy` and `forge install` target the intended Jira Cloud site

After that, Jira should show both Connect and Forge entry points into the **same** backend, with different authorization when calling Jira APIs, as described above.
