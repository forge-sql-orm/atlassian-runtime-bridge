package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

/**
 * Shared Connect-vs-Forge delegation for product select adapters. Routes each call to either the
 * Forge adapter or the Connect adapter based on whether the current authentication is a {@link
 * ForgeAuthentication}.
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
abstract class AbstractProductSelectAdapter {

  private final ConnectProductsAdapter connectProductsAdapter;
  private final AbstractProductForgeAdapter forgeAdapter;

  protected AbstractProductSelectAdapter(
      AtlassianHostRestClients atlassianHostRestClients, AbstractProductForgeAdapter forgeAdapter) {
    this.connectProductsAdapter = new ConnectProductsAdapter(atlassianHostRestClients);
    this.forgeAdapter = forgeAdapter;
  }

  private boolean isForge() {
    return SecurityContextHolder.getContext().getAuthentication() instanceof ForgeAuthentication;
  }

  public RestTemplate authenticatedAsAddon(AtlassianHost host) {
    return isForge()
        ? forgeAdapter.authenticatedAsAddon(host)
        : connectProductsAdapter.authenticatedAsAddon(host);
  }

  public RestTemplate authenticatedAsCurrentUser() {
    return isForge()
        ? forgeAdapter.authenticatedAsCurrentUser()
        : connectProductsAdapter.authenticatedAsCurrentUser();
  }

  public RestTemplate impersonation(AtlassianHostUser hostUser) {
    return isForge()
        ? forgeAdapter.impersonation(hostUser)
        : connectProductsAdapter.impersonation(hostUser);
  }
}
