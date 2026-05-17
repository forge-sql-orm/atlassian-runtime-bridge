package com.github.vzakharchenko.runtime.bridge.common;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import org.springframework.web.client.RestTemplate;

/**
 * Product-scoped HTTP client for <strong>Jira</strong> REST APIs.
 *
 * <p>Application code depends on this interface only. The Forge bridge module registers a {@code
 * com.github.vzakharchenko.runtime.bridge.forge.products.JiraProductSelectAdapter} bean that
 * <strong>automatically</strong> delegates to either Connect ({@code AtlassianHostRestClients}) or
 * Forge ({@code AtlassianForgeRestClients}) based on the current {@code SecurityContext} (Forge
 * requests use {@link com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication}).
 */
public interface JiraProductAdapter {
  /** Calls Jira as the Connect add-on / Forge app installation (service identity). */
  RestTemplate authenticatedAsAddon(AtlassianHost host);

  /** Calls Jira as the signed-in user in the host product (iframe or Forge user context). */
  RestTemplate authenticatedAsCurrentUser();

  /**
   * Calls Jira as a specific {@link AtlassianHostUser} (act-as / impersonation semantics). On Forge
   * this typically exchanges the app token for a user bearer via GraphQL impersonation.
   */
  RestTemplate impersonation(AtlassianHostUser hostUser);
}
