package com.github.vzakharchenko.runtime.bridge.common;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import org.springframework.web.client.RestTemplate;

/**
 * Product-scoped HTTP client for <strong>non-Jira / non-Confluence</strong> Atlassian Cloud routes
 * — typically things you would reach with Forge's generic {@code api.asApp().requestJira/
 * requestConfluence/fetch}, e.g. Atlassian platform APIs, marketplace endpoints, custom
 * service-to-service URLs.
 *
 * <p>Same select/route model as {@link JiraProductAdapter} and {@link ConfluenceProductAdapter}:
 * application code depends on this interface; the implementation is provided by either {@code
 * bridge-forge-connect} (hybrid Connect / Forge Remote) or {@code bridge-connect-container} (Forge
 * Containers) — never both on the same classpath.
 */
public interface OtherProductAdapter {

  /**
   * Returns a {@code RestTemplate} that authenticates as the Connect add-on / Forge app
   * installation (service identity).
   *
   * @param host installation whose credentials/installation id back the request
   */
  RestTemplate authenticatedAsAddon(AtlassianHost host);

  /**
   * Returns a {@code RestTemplate} that authenticates as the signed-in host user (Connect iframe
   * JWT or Forge user token, depending on runtime). Only valid inside a populated request thread.
   */
  RestTemplate authenticatedAsCurrentUser();

  /**
   * Returns a {@code RestTemplate} that acts as a specific {@link AtlassianHostUser}
   * (impersonation). Forge path uses the same impersonation pipeline as the Jira/Confluence
   * adapters; Connect path uses the configured host's shared secret.
   *
   * @param hostUser host + user identity to impersonate
   */
  RestTemplate impersonation(AtlassianHostUser hostUser);
}
