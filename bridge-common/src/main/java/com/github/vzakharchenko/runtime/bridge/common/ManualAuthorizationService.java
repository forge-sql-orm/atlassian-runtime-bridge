package com.github.vzakharchenko.runtime.bridge.common;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import java.util.Optional;

/**
 * Seeds Spring's {@code SecurityContextHolder} with a {@code ForgeAuthentication} so that the
 * product adapters ({@link JiraProductAdapter}, {@link ConfluenceProductAdapter}, {@link
 * OtherProductAdapter}) can run outside a normal Forge/Connect request thread — schedulers, queues,
 * outbound webhooks, webtriggers, or any controller annotated with {@code @IgnoreJwt} that rebuilds
 * identity from its own inputs.
 *
 * <p>Two implementations ship with this library; consumers pick exactly one transitively via the
 * runtime module they depend on:
 *
 * <ul>
 *   <li><strong>{@code bridge-forge-connect}</strong> for classic Connect / Forge Remote apps,
 *   <li><strong>{@code bridge-connect-container}</strong> for apps running on Forge Containers.
 * </ul>
 *
 * <p>Both implementations enforce <strong>tenant isolation</strong>: when a {@code
 * ForgeAuthentication} is already present in the security context, the {@code cloudId} supplied
 * here must match the context's {@code cloudId} or every overload throws {@code
 * IllegalStateException} with a message of the form {@code "Cross tenant authorization is not
 * allowed: expected cloudId=…, received cloudId=…"}. This blocks, for example, a webtrigger URL
 * minted in tenant A from driving Jira REST calls against tenant B.
 *
 * <p>Typical usage from a background thread:
 *
 * <pre>{@code
 * manualAuthorizationService.authorize(cloudId, installationId, Optional.of(accountId));
 * try {
 *   jiraProductAdapter.impersonation(hostUser).exchange(...);
 * } finally {
 *   SecurityContextHolder.clearContext(); // mandatory for pooled threads
 * }
 * }</pre>
 *
 * <p>Inside an HTTP request thread the Spring Security filter chain already clears the context for
 * you, so the {@code finally} is only required when you control the thread yourself.
 */
public interface ManualAuthorizationService {

  /**
   * Installs a {@code ForgeAuthentication} backed by the given {@code AtlassianHostUser}; the
   * user's {@code userAccountId} (if present) is preserved, so subsequent product calls execute
   * with user-scoped authorization.
   *
   * @param atlassianHostUser host and user identity to authorize as; must not be {@code null}
   * @throws IllegalStateException if a {@code ForgeAuthentication} from a different tenant is
   *     already in the security context (cross-tenant guard)
   */
  void authorize(AtlassianHostUser atlassianHostUser);

  /**
   * Installs a {@code ForgeAuthentication} for the given {@code AtlassianHost} with no {@code
   * userAccountId} (service identity) <em>unless</em> the existing security context already holds
   * an {@code AtlassianHostUser} for the same tenant, in which case that user's {@code accountId}
   * is preserved.
   *
   * @param atlassianHost host identity to authorize as; must not be {@code null}
   * @throws IllegalStateException if a {@code ForgeAuthentication} from a different tenant is
   *     already in the security context (cross-tenant guard)
   */
  void authorize(AtlassianHost atlassianHost);

  /**
   * Installs a {@code ForgeAuthentication} for a <em>minimal</em> host built from the supplied
   * identifiers. Use this overload when only ids are available — typically inside webtrigger
   * controllers, scheduled jobs, or queue consumers that pass {@code cloudId} / {@code
   * installationId} explicitly.
   *
   * <p>Populate additional host fields (base URL, shared secret, entitlements, etc.) yourself if a
   * downstream API needs them; the minimal host is sufficient for sidecar-routed calls in the
   * container runtime and for Forge offline-token calls in the hybrid runtime.
   *
   * @param cloudId Atlassian Cloud site id; must be non-{@code null}
   * @param installationId Forge installation id (ARI or raw UUID, depending on the implementation);
   *     must be non-{@code null}
   * @param accountId optional Atlassian account id to act as; when present (and non-blank), the
   *     resulting {@code AtlassianHostUser} carries it for user-scoped REST calls
   * @throws IllegalStateException if a {@code ForgeAuthentication} from a different tenant is
   *     already in the security context (cross-tenant guard)
   */
  void authorize(String cloudId, String installationId, Optional<String> accountId);
}
