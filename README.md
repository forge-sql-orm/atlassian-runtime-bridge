# atlassian-runtime-bridge

Spring Boot helpers for **hybrid Atlassian Connect + Forge** add-ons: one codebase can serve classic Connect iframe traffic and **Forge Remote** / container runtimes while reusing the same product-facing abstractions.

Built against **Atlassian Connect Spring Boot 6.x** and **Spring Boot 3.4.x** (see the root `pom.xml`).

## Modules

| Artifact | Role |
|----------|------|
| **`atlassian-runtime-bridge-common`** | Small, dependency-light API surface: product-scoped HTTP entry points (`JiraProductAdapter`, `ConfluenceProductAdapter`, `OtherProductAdapter`) and optional `AtlassianHostContextEnricher` for enriching `AtlassianHost` built from Forge context. |
| **`atlassian-runtime-bridge-forge`** | Depends on `common`. Registers Forge-oriented beans: security bridge, servlet filter, GraphQL impersonation, Connect-vs-Forge **select** adapters, and related utilities. |

Application code should depend only on **`atlassian-runtime-bridge-forge`** (it pulls `common` transitively).

## Maven coordinates

```xml
<dependency>
    <groupId>com.github.vzakharchenko</groupId>
    <artifactId>atlassian-runtime-bridge-forge</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

You still need Atlassian Connect Spring Boot starters on the classpath (see the [sample](examples/atlassian-connect-forge-spring-boot-sample/) `pom.xml`).

## Enabling the bridge in your Spring Boot app

1. Add the dependency above (plus Connect starter, JPA if you persist hosts/tokens, web, etc.).
2. **Component-scan** the bridge packages. The library is not registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`; you anchor scanning on `AtlassianConnectForgeAutoConfiguration` so all `com.github.vzakharchenko.runtime.bridge.*` beans load together with your app:

```java
@SpringBootApplication
@EnableRetry
@ComponentScan(basePackageClasses = {
        com.github.vzakharchenko.runtime.bridge.forge.AtlassianConnectForgeAutoConfiguration.class,
        MyApplication.class
})
public class MyApplication { /* ... */ }
```

3. Set **`app.id`** to your Forge manifest `app.id` (used when reconstructing `ForgeApp` metadata in `AtlassianForgeSecurityBridgeServiceImpl`).

`AtlassianConnectForgeAutoConfiguration` keeps the stock Connect auto-configuration and adds:

- **`AtlassianForgeFilter`** — runs after Forge auth and replaces `ForgeAuthentication` with one backed by a resolved `AtlassianHostUser` when possible.
- **`graphqlClient`** — shared `RestClient` for `https://api.atlassian.com/graphql` (offline user token / impersonation).

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

## Optional host enrichment

Implement **`AtlassianHostContextEnricher`** with context type **`ForgeApiContext`** (from Atlassian Connect Spring) if the default host built from Forge context is missing fields your code needs (`baseUrl`, `clientKey`, entitlement number, flags, etc.). The bridge invokes it when resolving hosts from live Forge API context.

## Example project

See **[examples/atlassian-connect-forge-spring-boot-sample](examples/atlassian-connect-forge-spring-boot-sample/)** and its **[README](examples/atlassian-connect-forge-spring-boot-sample/README.md)** for `manifest.yml`, tunneling, and end-to-end run instructions.

## Build

From the repository root (requires a JDK matching `maven.compiler.source` / `target` in the root `pom.xml`, currently **25**):

```bash
mvn -DskipTests install
```

The [sample app](examples/atlassian-connect-forge-spring-boot-sample/) may use a different Java level in its own `pom.xml`.
