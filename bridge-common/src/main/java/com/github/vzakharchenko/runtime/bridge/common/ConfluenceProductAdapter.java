package com.github.vzakharchenko.runtime.bridge.common;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import org.springframework.web.client.RestTemplate;

/**
 * Product-scoped HTTP client for <strong>Confluence</strong> REST APIs.
 *
 * <p>Same select/route model as {@link JiraProductAdapter}: application code depends on the
 * interface; the implementation comes from whichever runtime module is on the classpath ({@code
 * bridge-forge-connect} for hybrid Connect / Forge Remote apps, {@code bridge-connect-container}
 * for Forge Containers — exactly one of the two).
 */
public interface ConfluenceProductAdapter {

  /**
   * Returns a {@code RestTemplate} that authenticates as the Connect add-on / Forge app
   * installation (service identity).
   *
   * @param host installation whose credentials/installation id back the request
   */
  RestTemplate authenticatedAsAddon(AtlassianHost host);

  /**
   * Returns a {@code RestTemplate} that authenticates as the signed-in user of the current request
   * thread. Only valid inside a request that already carries a populated security context.
   */
  RestTemplate authenticatedAsCurrentUser();

  /**
   * Returns a {@code RestTemplate} that acts as a specific {@link AtlassianHostUser}; the Forge
   * path uses the same impersonation pipeline as {@link JiraProductAdapter#impersonation}.
   *
   * @param hostUser host + user identity to impersonate; {@code accountId} must be non-blank for
   *     the user-scoped variant
   */
  RestTemplate impersonation(AtlassianHostUser hostUser);
}
