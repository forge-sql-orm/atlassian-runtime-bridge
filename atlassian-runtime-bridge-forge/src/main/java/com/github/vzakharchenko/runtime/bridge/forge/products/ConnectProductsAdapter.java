package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.github.vzakharchenko.runtime.bridge.common.ConfluenceProductAdapter;
import com.github.vzakharchenko.runtime.bridge.common.JiraProductAdapter;
import com.github.vzakharchenko.runtime.bridge.common.OtherProductAdapter;
import org.springframework.web.client.RestTemplate;

/**
 * Connect-only delegation to {@link AtlassianHostRestClients}. Used by the {@code
 * *ProductSelectAdapter} beans when the request is <strong>not</strong> Forge-authenticated (no
 * {@link com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication}).
 *
 * <p>Implements all three product adapter interfaces because Connect’s REST client factory exposes
 * the same entry points for Jira, Confluence, and generic routes.
 */
public class ConnectProductsAdapter
    implements ConfluenceProductAdapter, OtherProductAdapter, JiraProductAdapter {
  private final AtlassianHostRestClients atlassianHostRestClients;

  public ConnectProductsAdapter(AtlassianHostRestClients atlassianHostRestClients) {
    this.atlassianHostRestClients = atlassianHostRestClients;
  }

  @Override
  public RestTemplate authenticatedAsAddon(AtlassianHost host) {
    return atlassianHostRestClients.authenticatedAsAddon(host);
  }

  @Override
  public RestTemplate authenticatedAsCurrentUser() {
    return atlassianHostRestClients.authenticatedAsHostActor();
  }

  @Override
  public RestTemplate impersonation(AtlassianHostUser hostUser) {
    return atlassianHostRestClients.authenticatedAs(hostUser);
  }
}
