# atlassian-runtime-bridge

Spring Boot helpers for **hybrid Atlassian Connect + Forge** add-ons: one codebase can serve classic Connect iframe traffic and **Forge Remote** / container runtimes while reusing the same product-facing abstractions.

Built against **Atlassian Connect Spring Boot 6.x** and **Spring Boot 3.4.x** (see the root `pom.xml`).

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=forge-sql-orm_atlassian-runtime-bridge&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=forge-sql-orm_atlassian-runtime-bridge)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=forge-sql-orm_atlassian-runtime-bridge&metric=bugs)](https://sonarcloud.io/summary/new_code?id=forge-sql-orm_atlassian-runtime-bridge)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=forge-sql-orm_atlassian-runtime-bridge&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=forge-sql-orm_atlassian-runtime-bridge)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=forge-sql-orm_atlassian-runtime-bridge&metric=coverage)](https://sonarcloud.io/summary/new_code?id=forge-sql-orm_atlassian-runtime-bridge)

## Modules

| Artifact | Role |
|----------|------|
| **`atlassian-runtime-bridge-common`** | Small, dependency-light API surface: product-scoped HTTP entry points (`JiraProductAdapter`, `ConfluenceProductAdapter`, `OtherProductAdapter`) and `AtlassianHostContextEnricher` for enriching `AtlassianHost` built from Forge context. |
| **`atlassian-runtime-bridge-forge`** | Depends on `common`. Registers Forge-oriented beans: security bridge, servlet filter, GraphQL impersonation, Connect-vs-Forge **select** adapters, optional JPA lookup by `installationId`, and related utilities. |

Application code should depend only on **`atlassian-runtime-bridge-forge`** (it pulls `common` transitively).

## Maven coordinates

```xml
<dependency>
    <groupId>com.github.vzakharchenko</groupId>
    <artifactId>atlassian-runtime-bridge-forge</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

You still need Atlassian Connect Spring Boot starters on the classpath (see the [sample](examples/atlassian-connect-forge-spring-boot-sample/) `pom.xml`). For **Connect-persisted host rows** (shared secret, `baseUrl`, entitlements, etc.) add **`atlassian-connect-spring-boot-jpa-starter`** and a datasource — the bridge registers an extra repository only when Connect’s JPA auto-configuration is present (see [Connect host persistence](#connect-host-persistence-jpa)).

## Enabling the bridge in your Spring Boot app

1. Add the dependency above (plus Connect starter, JPA if you persist hosts/tokens, web, etc.).
2. **Component-scan** the bridge packages. The library is not registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`; you anchor scanning on `AtlassianConnectForgeAutoConfiguration` so all `com.github.vzakharchenko.runtime.bridge.*` beans load together with your app:

```java
@SpringBootApplication
@ComponentScan(basePackageClasses = {
        com.github.vzakharchenko.runtime.bridge.forge.AtlassianConnectForgeAutoConfiguration.class,
        MyApplication.class
})
public class MyApplication { /* ... */ }
```

3. Set **`app.id`** to your Forge manifest `app.id` (used when reconstructing `ForgeApp` metadata in `AtlassianForgeSecurityBridgeServiceImpl`).

**`AtlassianConnectForgeAutoConfiguration`** keeps the stock Connect auto-configuration and adds:

- **`AtlassianForgeFilter`** — runs after Forge auth and replaces `ForgeAuthentication` with one backed by a resolved `AtlassianHostUser` when possible.
- **`graphqlClient`** — shared `RestClient` for `https://api.atlassian.com/graphql` (offline user token / impersonation).

Component scan on `com.github.vzakharchenko.runtime.bridge.forge` also picks up **`AtlassianConnectForgeJpaAutoConfiguration`** when Connect’s JPA starter is on the classpath (separate class; see below).

### Configuration classes

| Class | When active | Role |
|-------|-------------|------|
| **`AtlassianConnectForgeAutoConfiguration`** | Always (once component-scanned) | Filter, GraphQL `RestClient`, bridge component scan. |
| **`AtlassianConnectForgeJpaAutoConfiguration`** | Only if `com.atlassian.connect.spring.internal.jpa.AtlassianJpaAutoConfiguration` is on the classpath (Connect JPA starter) | Adds **`AtlassianHostByInstallationIdRepository`** via `@EnableJpaRepositories` **after** Connect’s own JPA config — no `@Primary`, no changes to `AtlassianHostRepository`. |

## Calling Jira, Confluence, or generic product REST

Define dependencies on the **adapter interfaces** from `common` (`JiraProductAdapter`, …). The **forge** module registers **select** beans (`JiraProductSelectAdapter`, …) that:

- If the current `SecurityContext` holds **`ForgeAuthentication`**, delegate to Forge `AtlassianForgeRestClients`-backed implementations.
- Otherwise delegate to classic Connect `AtlassianHostRestClients`-backed code paths.

So the same service layer can run in Connect-only, Forge-only, or hybrid deployments without branching on product type everywhere.

## Security bridge and manual authorization

**`AtlassianForgeSecurityBridgeService`** maps Forge invocation / persisted system tokens into the same `AtlassianHost`, `AtlassianHostUser`, and Spring `Authentication` types Connect Spring already uses, and can build `ForgeAuthentication` for programmatic use.

**`ManualAuthorizationService`** sets that authentication on **`SecurityContextHolder`**. Use it whenever product adapters must run **outside** a normal Forge/Jira request thread (schedulers, queues, outbound webhooks, etc.), because the select adapters key off the security context.

Typical pattern: run work on a thread with a clean or known context, call `authorize(...)`, invoke the adapters, then **clear the security context** when the task ends (especially for thread pools).

`ManualAuthorizationService` overloads:

- `authorize(AtlassianHostUser)` / `authorize(AtlassianHost)` when you already have full host objects.
- `authorize(String cloudId, String installationId, Optional<String> accountId)` for a **minimal** host built from ids (non-null `cloudId` / `installationId`; optional non-blank account id for user-scoped REST). Populate extra host fields yourself if a given API needs them.

## Host enrichment from Forge context

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

## Connect host persistence (JPA)

Connect’s **`AtlassianJpaAutoConfiguration`** already enables **`AtlassianHostRepository`**, **`AtlassianHostMappingRepository`**, and **`ForgeSystemAccessTokenRepository`**.

The bridge adds a **separate** repository interface:

- **`AtlassianHostByInstallationIdRepository`** — `Optional<AtlassianHost> findByInstallationId(String installationId)`

It is enabled by **`AtlassianConnectForgeJpaAutoConfiguration`**, which is conditional on the Connect JPA auto-configuration class name (string-based `@ConditionalOnClass` / `@AutoConfigureAfter`, so the bridge JAR does not require that class at compile time for consumers that omit JPA).

**`ConnectOnForgeContext`** injects `Optional<AtlassianHostByInstallationIdRepository>`: if the bean is absent, enrichment from the host table is skipped.

## Example project

See **[examples/atlassian-connect-forge-spring-boot-sample](examples/atlassian-connect-forge-spring-boot-sample/)** and its **[README](examples/atlassian-connect-forge-spring-boot-sample/README.md)** for `manifest.yml`, tunneling, and end-to-end run instructions.

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

Hooks live in **`.githooks/`** (tracked in git). **`pre-commit`** runs **`mvn spotless:apply`**, then **`mvn clean install`** at the repository root (full reactor, **tests on**, **`verify`** including **JaCoCo** check and report), then **`mvn jacoco:report`** for **`atlassian-runtime-bridge-common`** and **`atlassian-runtime-bridge-forge`** (refreshes HTML under each module’s **`target/site/jacoco/`**), prints **`file://…/target/site/jacoco/index.html`** paths, then **`mvn clean install`** for **`examples/atlassian-connect-forge-spring-boot-sample`** (example build and tests).

**Automatic registration:** building from the **repository root** runs **`scripts/install-git-hooks.sh`** on the **`initialize`** phase (via **`exec-maven-plugin`**, `inherited=false`), so a normal **`mvn clean install`** (or any goal that runs `initialize`) sets **`git config core.hooksPath .githooks`** for this clone.

- **Manual install** (same effect): `./scripts/install-git-hooks.sh`
- **Skip from Maven:** `-DinstallGitHooks.skip=true`
- **Skip in scripts:** `CI` set (e.g. GitHub Actions), **`GIT_HOOKS_INSTALL=0`**, or not a Git checkout — the script exits without changing Git config.
- **Bypass hook on commit:** `SKIP_HOOKS=1 git commit …`
- **Note:** `spotless:apply` can reformat files outside the staged set; review `git status` and re-stage if needed before committing again.
