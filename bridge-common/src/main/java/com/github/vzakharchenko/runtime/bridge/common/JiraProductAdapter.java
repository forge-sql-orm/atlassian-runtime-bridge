package com.github.vzakharchenko.runtime.bridge.common;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import org.springframework.web.client.RestTemplate;

/**
 * Product-scoped HTTP client for <strong>Jira</strong> REST APIs.
 *
 * <p>Application code depends on this interface only; the implementation is supplied by whichever
 * runtime module is on the classpath:
 *
 * <ul>
 *   <li><strong>{@code bridge-forge-connect}</strong> registers {@code JiraProductSelectAdapter}
 *       which delegates at runtime to either Connect ({@code AtlassianHostRestClients}) or Forge
 *       ({@code AtlassianForgeRestClients}) based on whether the current {@code SecurityContext}
 *       holds a {@code ForgeAuthentication}.
 *   <li><strong>{@code bridge-connect-container}</strong> registers {@code JiraProductAdapterImpl},
 *       which routes every call through the Forge Containers egress sidecar (no Connect REST
 *       clients, no host-row lookup).
 * </ul>
 *
 * <p>The two adapters must <strong>not</strong> coexist on the same classpath — pick one runtime
 * module per application.
 */
public interface JiraProductAdapter {

  /**
   * Returns a {@code RestTemplate} that authenticates as the Connect add-on / Forge app
   * installation (service identity). Use for endpoints that do not require user context, e.g.
   * webhook subscriptions, install/uninstall hygiene, app-scoped reads.
   *
   * @param host installation whose credentials/installation id back the request
   */
  RestTemplate authenticatedAsAddon(AtlassianHost host);

  /**
   * Returns a {@code RestTemplate} that authenticates as the signed-in user of the current HTTP
   * request — Connect iframe JWT or Forge user-scoped token, depending on the active runtime. Only
   * meaningful inside a request thread that already carries a populated security context.
   */
  RestTemplate authenticatedAsCurrentUser();

  /**
   * Returns a {@code RestTemplate} that acts as a specific {@link AtlassianHostUser}. On Forge this
   * typically swaps the app token for a user bearer via GraphQL impersonation; on Containers it
   * sets {@code forge-proxy-authorization: Forge id=…/installationId=…,as=user,accountId=…}.
   *
   * @param hostUser host + user identity to impersonate; {@code accountId} must be non-blank for
   *     the user-scoped variant
   */
  RestTemplate impersonation(AtlassianHostUser hostUser);
}
