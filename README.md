# atlassian-runtime-bridge

![Maven Central Version](https://img.shields.io/maven-central/v/com.github.vzakharchenko/atlassian-runtime-bridge)
![GitHub License](https://img.shields.io/github/license/forge-sql-orm/atlassian-runtime-bridge)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/forge-sql-orm/atlassian-runtime-bridge)
![GitHub Release](https://img.shields.io/github/v/release/forge-sql-orm/atlassian-runtime-bridge)


[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=forge-sql-orm_atlassian-runtime-bridge&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=forge-sql-orm_atlassian-runtime-bridge)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=forge-sql-orm_atlassian-runtime-bridge&metric=bugs)](https://sonarcloud.io/summary/new_code?id=forge-sql-orm_atlassian-runtime-bridge)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=forge-sql-orm_atlassian-runtime-bridge&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=forge-sql-orm_atlassian-runtime-bridge)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=forge-sql-orm_atlassian-runtime-bridge&metric=coverage)](https://sonarcloud.io/summary/new_code?id=forge-sql-orm_atlassian-runtime-bridge)

[![Maintainability](https://qlty.sh/gh/forge-sql-orm/projects/atlassian-runtime-bridge/maintainability.svg)](https://qlty.sh/gh/forge-sql-orm/projects/atlassian-runtime-bridge)
[![Code Coverage](https://qlty.sh/gh/forge-sql-orm/projects/atlassian-runtime-bridge/coverage.svg)](https://qlty.sh/gh/forge-sql-orm/projects/atlassian-runtime-bridge)

**One Spring codebase** for Connect, Forge Remote, and Forge Containers — product APIs go through shared adapters (`JiraProductAdapter`, …); the bridge picks Connect JWT, Forge tokens, or the container egress sidecar from `SecurityContext`.

| You are building… | Maven artifact | Sample module |
|-------------------|----------------|---------------|
| Connect iframe **and/or** Forge Remote (hybrid or migrated) | [`bridge-forge-connect`](#maven-coordinates) | [`forge-connect/`](examples/atlassian-connect-forge-spring-boot-sample/forge-connect/) → then [`forge-remote/`](examples/atlassian-connect-forge-spring-boot-sample/forge-remote/) |
| Forge Containers (isolated cloud) | [`bridge-connect-container`](#forge-containers-bridge-connect-container) | [`forge-container/`](examples/atlassian-connect-forge-spring-boot-sample/forge-container/) |

**Working end-to-end example** (Connect + Forge Remote + Containers in one repo): **[`examples/atlassian-connect-forge-spring-boot-sample`](examples/atlassian-connect-forge-spring-boot-sample/)** — see its [README](examples/atlassian-connect-forge-spring-boot-sample/README.md) for install/run.

Do **not** put `bridge-forge-connect` and `bridge-connect-container` on the same classpath.

Built against **Atlassian Connect Spring Boot 6.x** and **Spring Boot 3.5.x** (root `pom.xml`).

---

## Migration: Connect → Forge Remote

Target manifest shape: [`forge-remote/manifest.yml`](examples/atlassian-connect-forge-spring-boot-sample/forge-remote/manifest.yml) (Forge modules only, no `connectModules`). Hybrid stepping stone: [`forge-connect/manifest.yml`](examples/atlassian-connect-forge-spring-boot-sample/forge-connect/manifest.yml).

### 1. Tenant keys: `cloudId` instead of `clientKey`

Move **your** persistence off `AtlassianHost.clientKey`:

- Internal entities and repositories should use **`cloudId`** as the tenant key.
- There must be **no FK or JPA relation** from your tables to Connect’s `AtlassianHost` entity — that table is Connect lifecycle metadata, not your domain model.

**Transitional pattern** (while some sites still exist only on native Connect):

| Column / lookup | Purpose |
|-----------------|--------|
| `cloudId` | Primary tenant id going forward |
| `clientKey` | Legacy id during migration only |

Lookup order: **`cloudId` first**, then **`clientKey`** if missing (for the rare native-Connect-only installs). A small mapping table `(cloud_id, client_key, installation_id)` is enough; drop `client_key` from app queries once all tenants are on Forge.

The bridge can still **read** Connect’s host row via `installationId` for hybrid enrichment ([`ConnectOnForgeContext`](#built-in-connect-row-by-installationid)) — that is separate from your entity model.

### 2. Add `bridge-forge-connect`

```xml
<dependency>
    <groupId>com.github.vzakharchenko</groupId>
    <artifactId>bridge-forge-connect</artifactId>
    <version>1.0.1</version>
</dependency>
```

Keep Connect starters on the classpath during hybrid; use `@SpringBootApplication` only — the bridge loads from `AutoConfiguration.imports` ([details](#enabling-the-bridge-in-your-spring-boot-app-hybrid--forge-remote)). Set **`app.id`** to the Forge manifest `app.id`.

Refactor services to call **`JiraProductAdapter`** / **`ManualAuthorizationService`** instead of branching on Connect vs Forge.

### 3. Two frontend builds (Connect iframe vs Forge Custom UI)

Ship **two bundles** (or one repo with a **platform alias** — same idea as [this community post](https://community.developer.atlassian.com/t/disable-forge-tunnel/96807/10?u=vzakharchenko)):

- **Connect** — iframe / `AP.request` transport.
- **Forge** — Custom UI + `@forge/bridge` transport.

The sample uses **esbuild aliases** (`app-transport` → `connect.ts` | `forge.ts` | `container.ts`):

```json
"build:connect": "esbuild ... --alias:app-transport=./.../connect.ts",
"build:forge": "esbuild ... --alias:app-transport=./.../forge.ts"
```

See [`frontend/package.json`](examples/atlassian-connect-forge-spring-boot-sample/frontend/package.json). Output: Connect `bundle.js` on the classpath; Forge `customUI/` for `forge deploy` (see [`forge-remote/manifest.yml`](examples/atlassian-connect-forge-spring-boot-sample/forge-remote/manifest.yml) `resources`).

### 4. Forge manifest: add Forge modules, remove Connect modules

In the manifest you deploy:

- **Add** Forge `modules` (`jira:globalPage`, `endpoint`, `scheduledTrigger`, `trigger`, `remotes`, …) pointing at your Spring **`baseUrl`**.
- **Remove** `connectModules` (lifecycle, `generalPages`, …) when you no longer serve Connect UI from the descriptor.

Compare hybrid vs Forge-only in the sample:

| | Hybrid | Forge Remote |
|---|--------|--------------|
| Manifest | [`forge-connect/manifest.yml`](examples/atlassian-connect-forge-spring-boot-sample/forge-connect/manifest.yml) | [`forge-remote/manifest.yml`](examples/atlassian-connect-forge-spring-boot-sample/forge-remote/manifest.yml) |
| `connectModules` | Present | **Absent** |
| `app.connect.remote` | `connect` | `forge-remote` |

### 5. Deploy and cut over

1. `mvn clean install` (builds frontend + Spring).
2. Run Spring from **`forge-connect/`** (same JVM serves both transports until Connect UI is retired).
3. `forge deploy` from **`forge-remote/`** when ready; `forge install --upgrade`.
4. Retire Connect iframe traffic; you are on **Forge Remote**.

---

## Migration: Forge Remote → Forge Containers

Harder path — only after **Forge Remote** works. Target: [`forge-container/manifest.yml`](examples/atlassian-connect-forge-spring-boot-sample/forge-container/manifest.yml) (`services`, container image, egress sidecar). Runbook: [`forge-container/README.md`](examples/atlassian-connect-forge-spring-boot-sample/forge-container/README.md).

### 1. Complete Connect → Forge Remote first

Containers assume Forge invocation context and platform routing, not Connect iframe JWT + public `remotes.baseUrl` as the primary runtime.

### 2. Swap the bridge module

| Remove | Add |
|--------|-----|
| `bridge-forge-connect` | `bridge-connect-container` |

Separate Spring module or profile: **`forge-container/`** in the sample. Entry point scans container auto-config only ([`AddonApplication`](examples/atlassian-connect-forge-spring-boot-sample/forge-container/src/main/java/sample/connect/spring/atlaskit/AddonApplication.java)).

### 3. Replace the data layer

Forge Containers run in **isolated cloud** — no Connect JPA host table on the classpath (auto-excluded). Migrate persistence off **Hibernate/JDBC** to platform storage, for example:

- [Forge SQL](https://developer.atlassian.com/platform/forge/storage-reference/sql/) (HTTP SQL API), or
- Forge **KVS** / other Forge storage modules.

Tenant identity remains **`cloudId`** / **`installationId`** from invocation context or webtrigger query params — not `clientKey`.

### 4. Container image, manifest, deploy

1. `forge containers create -k <container-key>` (sample: `java-service`).
2. Build Custom UI: `npm run build:container` → `forge-container/customUI/`.
3. Docker image from repo root → push to Forge ECR → `forge variables set TAG` → `forge deploy`.
4. Local loop: Spring on host + proxy sidecar + `forge tunnel` ([`dev-loop.sh`](examples/atlassian-connect-forge-spring-boot-sample/forge-container/dev-loop.sh)).

You now run on **Forge Containers** and **isolated cloud**.

---

## Modules

| Artifact | Role |
|----------|------|
| **`bridge-common`** | Shared API: `JiraProductAdapter`, `ConfluenceProductAdapter`, `OtherProductAdapter`, `ManualAuthorizationService`, `AtlassianHostContextEnricher`. |
| **`bridge-forge-connect`** | Hybrid / **Forge Remote**: security bridge, Forge filter, select adapters, optional Connect host lookup by `installationId`. |
| **`bridge-connect-container`** | **Forge Containers**: ingress headers, egress sidecar, container Jira adapter; excludes Connect/JPA auto-config. |

## Maven coordinates

**Hybrid / Forge Remote:**

```xml
<dependency>
    <groupId>com.github.vzakharchenko</groupId>
    <artifactId>bridge-forge-connect</artifactId>
    <version>1.0.1</version>
</dependency>
```

**Forge Containers:**

```xml
<dependency>
    <groupId>com.github.vzakharchenko</groupId>
    <artifactId>bridge-connect-container</artifactId>
    <version>1.0.1</version>
</dependency>
```

You still need Atlassian Connect Spring Boot artifacts on the classpath where noted below (see the [sample](examples/atlassian-connect-forge-spring-boot-sample/) `pom.xml`). For **Connect-persisted host rows** in hybrid apps, add **`atlassian-connect-spring-boot-jpa-starter`** and a datasource — the forge bridge registers an extra repository only when Connect’s JPA auto-configuration is present (see [Connect host persistence](#connect-host-persistence-jpa)). **Container apps do not use JPA** — the container module excludes datasource/JPA auto-configurations automatically.

---

## Library reference

### Enabling the bridge in your Spring Boot app (hybrid / Forge Remote)

1. Add the dependency above (plus Connect starter, JPA if you persist hosts/tokens, web, etc.).
2. Use a normal **`@SpringBootApplication`** in your app package. **`bridge-forge-connect`** registers via **`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`** — no mandatory `@ComponentScan` on the bridge.

```java
@SpringBootApplication
public class MyApplication { /* ... */ }
```

Optional: anchor component scan on **`AtlassianConnectForgeAutoConfiguration`** if you want your application class in a different package than your controllers but still scan them together (do not scan the bridge class *in addition* to the auto-config import — that duplicates beans).

3. Set **`app.id`** to your Forge manifest `app.id` (used when reconstructing `ForgeApp` metadata in `AtlassianForgeSecurityBridgeServiceImpl`).

**`AtlassianConnectForgeAutoConfiguration`** (`@AutoConfigureAfter` Connect) keeps the stock Connect auto-configuration and adds:

- **`AtlassianForgeFilter`** — runs after Forge auth and replaces `ForgeAuthentication` with one backed by a resolved `AtlassianHostUser` when possible.
- **`graphqlClient`** — shared `RestClient` for `https://api.atlassian.com/graphql` (offline user token / impersonation).

**`AtlassianConnectForgeJpaAutoConfiguration`** is a separate auto-configuration entry, active when Connect’s JPA starter is on the classpath (see below).

### Configuration classes

| Class | When active | Role |
|-------|-------------|------|
| **`AtlassianConnectForgeAutoConfiguration`** | Always (via `AutoConfiguration.imports`) | Filter, GraphQL `RestClient`, bridge component scan. |
| **`AtlassianConnectForgeJpaAutoConfiguration`** | Only if `com.atlassian.connect.spring.internal.jpa.AtlassianJpaAutoConfiguration` is on the classpath (Connect JPA starter) | Adds **`AtlassianHostByInstallationIdRepository`** via `@EnableJpaRepositories` **after** Connect’s own JPA config — no `@Primary`, no changes to `AtlassianHostRepository`. |

### Calling Jira, Confluence, or generic product REST

Define dependencies on the **adapter interfaces** from `common` (`JiraProductAdapter`, …). The **forge** module registers **select** beans (`JiraProductSelectAdapter`, …) that:

- If the current `SecurityContext` holds **`ForgeAuthentication`**, delegate to Forge `AtlassianForgeRestClients`-backed implementations.
- Otherwise delegate to classic Connect `AtlassianHostRestClients`-backed code paths.

So the same service layer can run in Connect-only, Forge-only, or hybrid deployments without branching on product type everywhere.

### Security bridge and manual authorization

**`AtlassianForgeSecurityBridgeService`** maps Forge invocation / persisted system tokens into the same `AtlassianHost`, `AtlassianHostUser`, and Spring `Authentication` types Connect Spring already uses, and can build `ForgeAuthentication` for programmatic use.

**`ManualAuthorizationService`** sets that authentication on **`SecurityContextHolder`**. Use it whenever product adapters must run **outside** a normal Forge/Jira request thread (schedulers, queues, outbound webhooks, etc.), because the select adapters key off the security context.

Typical pattern: run work on a thread with a clean or known context, call `authorize(...)`, invoke the adapters, then **clear the security context** when the task ends (especially for thread pools).

`ManualAuthorizationService` overloads:

- `authorize(AtlassianHostUser)` / `authorize(AtlassianHost)` when you already have full host objects.
- `authorize(String cloudId, String installationId, Optional<String> accountId)` for a **minimal** host built from ids (non-null `cloudId` / `installationId`; optional non-blank account id for user-scoped REST). Populate extra host fields yourself if a given API needs them.

### Host enrichment from Forge context

When resolving an `AtlassianHost` from a live **`ForgeApiContext`**, **`AtlassianForgeSecurityBridgeServiceImpl`** builds a **minimal** host (`installationId`, `cloudId`, `clientKey`, `addonInstalled`) from the Forge invocation token, then runs every registered **`AtlassianHostContextEnricher<ForgeApiContext>`** in ascending **`order()`** (lower runs first).

### Built-in: Connect row by `installationId`

If you use Connect’s host table (JPA), the forge module registers **`ConnectOnForgeContext`** automatically when Connect’s **`AtlassianJpaAutoConfiguration`** is present. It:

1. Looks up the row with **`AtlassianHostByInstallationIdRepository.findByInstallationId(...)`** (Spring Data derived query; **not** on stock `AtlassianHostRepository`, which is keyed by `clientKey`).
2. **Merges** the persisted row with the minimal Forge host via **`AtlassianHostMerge`**: Connect columns (`sharedSecret`, `baseUrl`, entitlements, audit fields, …) come from the database; non-null scalar fields from the invocation token overlay the copy (e.g. fresh **`cloudId`**). The JPA entity returned from the repository is **not** mutated.

**`ConnectOnForgeContext.CONNECT_LOOKUP_ORDER`** is `1`, so this enricher runs before custom enrichers that keep the default **`order()`** (`Integer.MAX_VALUE`).

Without JPA / without a matching row, the minimal host is unchanged and the bridge still works for Forge-only flows.

### Custom enrichers

Implement **`AtlassianHostContextEnricher<ForgeApiContext>`** in `common` and register it as a Spring bean if you need more than the Connect table (custom flags, external tenant registry, per-tenant config, etc.). The enricher receives the host produced by the previous step in the chain and returns the next one — so a custom bean can **fully replace** the `AtlassianHost`, copy fields from any source, or attach a richer subclass (see below). Override **`order()`** when your logic must run before or after **`ConnectOnForgeContext`** (which uses `order() = 1`).

```java
@Component
public class TenantConfigEnricher implements AtlassianHostContextEnricher<ForgeApiContext> {

  private final TenantConfigRepository tenants;

  public TenantConfigEnricher(TenantConfigRepository tenants) {
    this.tenants = tenants;
  }

  @Override
  public int order() {
    return 100; // after ConnectOnForgeContext (1), before defaults (Integer.MAX_VALUE)
  }

  @Override
  public Optional<AtlassianHost> update(
      Optional<AtlassianHost> host, Optional<ForgeApiContext> ctx) {
    return host.map(h -> {
      h.setBaseUrl(tenants.resolveBaseUrl(h.getInstallationId())); // or build a fresh instance
      return h;
    });
  }
}
```

### Extending `AtlassianHost` with your own fields

`com.atlassian.connect.spring.AtlassianHost` is **not `final`** — you can subclass it and carry extra state (feature flags, tenant tier, cached entitlement details, etc.) through the same `AtlassianHostUser` / security context the bridge already propagates. Return your subclass from an enricher and downstream code (product adapters, custom `@Service` beans) can cast or call typed helpers on it:

```java
public class TenantAwareHost extends AtlassianHost {
  private String tier;          // e.g. "free" / "standard" / "premium"
  private Set<String> features; // tenant-level feature toggles

  public String getTier()              { return tier; }
  public void setTier(String tier)     { this.tier = tier; }
  public Set<String> getFeatures()     { return features; }
  public void setFeatures(Set<String> f) { this.features = f; }
}

@Component
public class TenantAwareHostEnricher implements AtlassianHostContextEnricher<ForgeApiContext> {

  @Override
  public Optional<AtlassianHost> update(
      Optional<AtlassianHost> host, Optional<ForgeApiContext> ctx) {
    return host.map(base -> {
      TenantAwareHost enriched = new TenantAwareHost();
      // copy whatever Connect / ConnectOnForgeContext already filled in:
      enriched.setClientKey(base.getClientKey());
      enriched.setInstallationId(base.getInstallationId());
      enriched.setCloudId(base.getCloudId());
      enriched.setBaseUrl(base.getBaseUrl());
      enriched.setSharedSecret(base.getSharedSecret());
      // add your own:
      enriched.setTier(lookupTier(base.getInstallationId()));
      enriched.setFeatures(lookupFeatures(base.getInstallationId()));
      return enriched;
    });
  }
}
```

Notes when subclassing:

- **Don't mutate JPA-managed instances.** `ConnectOnForgeContext` already guards against this via `AtlassianHostMerge`; if your enricher receives a host that came from `AtlassianHostRepository`, build a new object instead of calling setters on the input.
- **Persisting your subclass is your responsibility.** Connect Spring's `AtlassianHostRepository` stores the base `AtlassianHost` columns only — extra fields on your subclass live in memory unless you persist them yourself (separate table, your own JPA entity, cache, etc.).
- **Downstream code reaches your fields via `(TenantAwareHost) hostUser.getHost()`** (or an `instanceof` pattern). The bridge's select adapters do not care about the concrete type — they only read the standard Connect fields.

### Connect host persistence (JPA)

Connect’s **`AtlassianJpaAutoConfiguration`** already enables **`AtlassianHostRepository`**, **`AtlassianHostMappingRepository`**, and **`ForgeSystemAccessTokenRepository`**.

The bridge adds a **separate** repository interface:

- **`AtlassianHostByInstallationIdRepository`** — `Optional<AtlassianHost> findByInstallationId(String installationId)`

It is enabled by **`AtlassianConnectForgeJpaAutoConfiguration`**, which is conditional on the Connect JPA auto-configuration class name (string-based `@ConditionalOnClass` / `@AutoConfigureAfter`, so the bridge JAR does not require that class at compile time for consumers that omit JPA).

**`ConnectOnForgeContext`** injects `Optional<AtlassianHostByInstallationIdRepository>`: if the bean is absent, enrichment from the host table is skipped.

### Forge Containers (`bridge-connect-container`)

Use this module when your Spring Boot app runs **inside a Forge Container** (platform routes traffic to your image; Jira/Confluence REST goes through the **egress proxy sidecar**, not Connect host REST clients or a public `remotes.baseUrl`).

Official reference: [Forge Containers](https://developer.atlassian.com/platform/forge/containers-reference/).

### How it differs from `bridge-forge-connect`

| | **`bridge-forge-connect`** | **`bridge-connect-container`** |
|---|---------------------------|--------------------------------|
| Traffic | Browser / Forge Remote → your HTTPS URL | Forge → your container; sidecar on `FORGE_EGRESS_PROXY_URL` |
| Jira REST | Connect `AtlassianHostRestClients` or Forge `AtlassianForgeRestClients` (select adapters) | Always via egress proxy (`/jira/...`) |
| Ingress auth | Connect JWT + `AtlassianForgeFilter` | `x-forge-invocation-id` → sidecar `/invocation/context` |
| Connect JPA / host table | Optional enrichment | **Excluded** (no datasource) |
| Spring registration | Boot **`AutoConfiguration.imports`** | Boot **`AutoConfiguration.imports`** + environment post-processor |

### Enabling in your app

1. Add **`bridge-connect-container`** and **`spring-boot-starter-web`**.
2. Add **`atlassian-connect-spring-boot-core`** only (types for `ForgeAuthentication`, `AtlassianHost`, …) — **not** `atlassian-connect-spring-boot-starter`, **not** `atlassian-connect-spring-boot-jpa-starter`, **not** `bridge-forge-connect`.
3. Anchor component scan on the container auto-configuration (same pattern as hybrid, different anchor class):

```java
@SpringBootApplication
@ComponentScan(basePackageClasses = {
        com.github.vzakharchenko.runtime.bridge.containers
                .AtlassianConnectForgeContainerAutoConfiguration.class,
        MyApplication.class
})
public class MyApplication { /* ... */ }
```

**`AtlassianConnectForgeContainerAutoConfiguration`** is also listed in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, so beans load even without scan; scanning the class keeps your app and bridge packages aligned (see the [sample `AddonApplication`](examples/atlassian-connect-forge-spring-boot-sample/forge-container/src/main/java/sample/connect/spring/atlaskit/AddonApplication.java)).

On startup, **`ContainerAutoConfigurationEnvironmentPostProcessor`** merges **`spring.autoconfigure.exclude`** so Connect web/JPA, JDBC, Liquibase, Quartz, and Redis auto-configurations do not start in a typical container deployment (see **`ContainerExcludedAutoConfigurations`**).

### Configuration

| Property / env | Default | Purpose |
|----------------|---------|---------|
| **`egress.proxy.url`** | `http://localhost:7072` | Base URL of the Forge **egress sidecar** (local docker-compose or platform-injected `FORGE_EGRESS_PROXY_URL` in cloud) |
| **`FORGE_EGRESS_PROXY_URL`** | — | Often set in `application.yaml` as `${FORGE_EGRESS_PROXY_URL:http://localhost:7072}` (see [sample `application.yaml`](examples/atlassian-connect-forge-spring-boot-sample/forge-container/src/main/resources/application.yaml)) |
| **`app.id`** | — | Forge manifest `app.id` when building `ForgeApp` metadata in manual authorization paths |
| **`bridge.container.security.public-paths`** | `[/health]` | Ant patterns exempt from HTTP security auth (health, etc.); other paths need `ContainerAuthorizationFilter` or manual auth |

Example `application.yaml`:

```yaml
egress:
  proxy:
    url: ${FORGE_EGRESS_PROXY_URL:http://localhost:7072}

bridge:
  container:
    security:
      public-paths:
        - /health
        - /actuator/health/**
        - /api/public/**
```

**Local dev:** run Spring on the host (`mvn spring-boot:run`, port **8080**), start the platform **proxy sidecar** (ports **7071** / **7072**), then `forge tunnel`. Full steps: **[`forge-container/README.md`](examples/atlassian-connect-forge-spring-boot-sample/forge-container/README.md)**.

**Cloud deploy:** build the image from the **repository root** (`forge-container/Dockerfile`), push to Forge ECR (`forge containers create` + `docker push`), set Forge variable **`TAG`**, `forge deploy`. Use [`build-and-deploy.sh.example`](examples/atlassian-connect-forge-spring-boot-sample/forge-container/build-and-deploy.sh.example) locally (the real `build-and-deploy.sh` is gitignored).

Expose a platform health check (sample: **`GET /health`** → `OK`).

### Runtime flow

```text
Jira / Forge UI
      │
      ▼
Forge platform ──ingress──► your container :8080
      │                      (x-forge-invocation-id)
      │                      ContainerAuthorizationFilter
      │                      → ForgeAuthentication in SecurityContext
      │
      └──egress sidecar :7072──► Jira REST (/jira/rest/api/…)
             ▲
             └── EgressClientService / JiraProductAdapterImpl
                 (forge-proxy-authorization: Forge id=… / installationId=…)
```

### Beans you use in application code

| Bean | Role |
|------|------|
| **`JiraProductAdapter`** (`JiraProductAdapterImpl`) | Same interface as hybrid; implementation calls **`EgressClientService.jiraTemplateRequest`** |
| **`EgressClientService`** | Sidecar JSON APIs (`/invocation/context`) and Jira path factory |
| **`ForgeContextService`** | Resolves invocation context from egress for ingress filter |
| **`ManualAuthorizationService`** (`ManualAuthorizationServiceImpl`) | Seed **`SecurityContextHolder`** for background work, webtriggers, or tests (rejects cross-tenant `cloudId` when a context already exists) |
| **`ContainerAuthorizationFilter`** | Ingress: when `x-forge-invocation-id` is present, populates `SecurityContextHolder` with `ForgeAuthentication`. Inserted **inside** the Spring Security chain via `addFilterAfter(SecurityContextHolderFilter.class)` — running before `SecurityContextHolderFilter` (or entirely outside the chain) would let it reload an empty deferred context from the stateless repository and overwrite the installed authentication, after which `AuthorizationFilter` rejects non-public paths with 403 |
| **`ContainerWebSecurityConfiguration`** | Stateless HTTP security (no form login). Public paths come from `bridge.container.security.public-paths` (default `[/health]`); everything else requires an authenticated principal set by `ContainerAuthorizationFilter` |

**`ManualAuthorizationService`** overloads match the hybrid module (`authorize(AtlassianHostUser)`, `authorize(AtlassianHost)`, `authorize(cloudId, installationId, accountId)`). Clear the security context after background tasks.

### Reaching endpoints via webtrigger

The container is not internet-routable — only Forge platform reaches it. To expose a backend route as a public URL (browser, cron, external system), declare a **`webtrigger`** module in `manifest.yml` pointing at an `endpoint` that maps to your Spring path:

```yaml
modules:
  webtrigger:
    - key: my-trigger
      endpoint: my-trigger-ep
  endpoint:
    - key: my-trigger-ep
      service: java-service
      route:
        path: /api/my-trigger      # your @GetMapping path
```

Then `forge webtrigger create -e <env>` returns a URL of the form `https://<app-id>.hello.atlassian-dev.net/x1/<webtrigger-token>` that Forge routes to that endpoint inside your container. The token authorizes the **URL**, not the caller — treat it as a capability URL and add caller-side controls when needed.

Webtrigger requests do not carry a Connect iframe JWT. The recommended controller pattern is:

```java
@GetMapping("/api/my-trigger")
@IgnoreJwt
@ResponseBody
public Map<String, String> myTrigger(
    @RequestParam String accountId, @AuthenticationPrincipal AtlassianHostUser user) {

  manualAuthorizationService.authorize(user.getHost().getCloudId(), user.getHost().getInstallationId(), Optional.of(accountId));
  // select adapters now see a ForgeAuthentication with that AtlassianHostUser
  return ...;
}
```

`ManualAuthorizationService.authorize(...)` seeds `SecurityContextHolder` with a `ForgeAuthentication` so the **select adapters** (`JiraProductAdapter`, …) call the egress sidecar as that user without any further wiring. The same call also enforces **tenant isolation** — if a `ForgeAuthentication` is already in the context (e.g. you compose the trigger with another authenticated flow), the new `cloudId` must match the existing one, otherwise an `IllegalStateException("Cross tenant authorization is not allowed: …")` is thrown. This prevents a webtrigger URL minted for tenant A from being used to drive Jira calls against tenant B.

End-to-end example with real URL shape, query parameters, and CLI commands: **[`forge-container/README.md` → Calling `/api/impersonation` via webtrigger](examples/atlassian-connect-forge-spring-boot-sample/forge-container/README.md#calling-apiimpersonation-via-webtrigger)**.

### Manifest and container image (sample)

In **[`examples/.../forge-container/manifest.yml`](examples/atlassian-connect-forge-spring-boot-sample/forge-container/manifest.yml)**:

- **`containers[].key`**: `java-service` (must match `forge containers create -k …` and ECR repo name)
- **`services`**: routes Forge modules to the container
- **`app.connect.remote`**: Connect key for shared descriptors (no Connect iframe in this sample)
- Deployed image tag: Forge variable **`${TAG}`**

See **[`forge-container/README.md`](examples/atlassian-connect-forge-spring-boot-sample/forge-container/README.md)** for register, Custom UI build, `dev-loop.sh`, and deploy.

## Build

From the repository root (requires a JDK matching **`java.version`** in the root `pom.xml`, currently **21**):

```bash
mvn clean install
```

The [sample app](examples/atlassian-connect-forge-spring-boot-sample/) may use a different Java level in its own `pom.xml`.

### Code style

- **Format (Google Java Format + import cleanup):** `mvn spotless:apply`
- **Check formatting without writing files:** `mvn spotless:check`
- **`mvn verify`** runs **Spotless** (`spotless:check`), **Checkstyle**, **SpotBugs** (`spotbugs:check`), and **PMD** (`pmd:check`) on each library module.

To skip Spotless temporarily (for example during a large merge): `mvn verify -Dspotless.skip=true`. To skip Checkstyle: `mvn verify -Dcheckstyle.skip=true`. To skip SpotBugs: `mvn verify -Dspotbugs.skip=true`. To skip PMD: `mvn verify -Dpmd.skip=true`.

SpotBugs uses **`config/spotbugs/exclude-filter.xml`** for known false positives (extend with `<Match>` as needed). PMD rules are the built-in **`category/java/errorprone.xml`** and **`category/java/bestpractices.xml`** (see root `pom.xml`); reports: **`target/pmd.xml`** per module.

### Error Prone

[Error Prone](https://errorprone.info/) runs as a **javac plugin** during **`compile`** (so **`mvn clean install`** and **`mvn verify`** both compile sources with checks enabled).

- **Skip temporarily:** `mvn … -Derrorprone.skip=true` (profile **`errorprone-off`**).
- **JDK module flags** for the build JVM live in **`.mvn/jvm.config`**; forked `javac` also gets the `-J--add-exports` / `-J--add-opens` flags from **`maven-compiler-plugin`** (see [Error Prone Maven install](https://errorprone.info/docs/installation#maven)).
- Version: **`error-prone.version`** in the root `pom.xml` (currently aligned with `error_prone_core` on Maven Central).

### Git pre-commit hook

Hooks live in **`.githooks/`** (tracked in git). **`pre-commit`** runs **`mvn spotless:apply`**, then **`mvn clean install`** at the repository root (full reactor including **`bridge-connect-container`**, **tests on**, **`verify`** including **JaCoCo** check and report), then **`mvn jacoco:report`** for **`bridge-common`** and **`bridge-forge-connect`** (refreshes HTML under each module’s **`target/site/jacoco/`**), prints **`file://…/target/site/jacoco/index.html`** paths, then **`mvn clean install`** for **`examples/atlassian-connect-forge-spring-boot-sample`** (example build and tests).

**Automatic registration:** building from the **repository root** runs **`scripts/install-git-hooks.sh`** on the **`initialize`** phase (via **`exec-maven-plugin`**, `inherited=false`), so a normal **`mvn clean install`** (or any goal that runs `initialize`) sets **`git config core.hooksPath .githooks`** for this clone.

- **Manual install** (same effect): `./scripts/install-git-hooks.sh`
- **Skip from Maven:** `-DinstallGitHooks.skip=true`
- **Skip in scripts:** `CI` set (e.g. GitHub Actions), **`GIT_HOOKS_INSTALL=0`**, or not a Git checkout — the script exits without changing Git config.
- **Bypass hook on commit:** `SKIP_HOOKS=1 git commit …`
- **Note:** `spotless:apply` can reformat files outside the staged set; review `git status` and re-stage if needed before committing again.
