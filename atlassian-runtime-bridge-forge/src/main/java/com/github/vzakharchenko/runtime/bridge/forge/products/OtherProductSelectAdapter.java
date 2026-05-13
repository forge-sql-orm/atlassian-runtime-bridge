package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.AtlassianForgeRestClients;
import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.github.vzakharchenko.runtime.bridge.common.OtherProductAdapter;
import com.github.vzakharchenko.runtime.bridge.forge.ImpersonationUserService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Spring {@link Component} implementing {@link OtherProductAdapter} with <strong>automatic</strong>
 * Connect vs Forge delegation for non-Jira, non-Confluence Forge {@code request()} targets.
 */
@Component
public class OtherProductSelectAdapter implements OtherProductAdapter {
  private final ConnectProductsAdapter connectProductsAdapter;
  private final OtherProductForgeAdapter otherProductForgeAdapter;

  public OtherProductSelectAdapter(
      AtlassianHostRestClients atlassianHostRestClients,
      AtlassianForgeRestClients atlassianForgeRestClients,
      ForgeSecurityContextRetriever forgeSecurityContextRetriever,
      RestTemplateBuilder restTemplateBuilder,
      ImpersonationUserService impersonationUserService) {
    this.connectProductsAdapter = new ConnectProductsAdapter(atlassianHostRestClients);
    this.otherProductForgeAdapter =
        new OtherProductForgeAdapter(
            atlassianForgeRestClients,
            forgeSecurityContextRetriever,
            restTemplateBuilder,
            impersonationUserService);
  }

  private boolean isForge() {
    return SecurityContextHolder.getContext().getAuthentication() instanceof ForgeAuthentication;
  }

  @Override
  public RestTemplate authenticatedAsAddon(AtlassianHost host) {
    return isForge()
        ? otherProductForgeAdapter.authenticatedAsAddon(host)
        : connectProductsAdapter.authenticatedAsAddon(host);
  }

  @Override
  public RestTemplate authenticatedAsCurrentUser() {
    return isForge()
        ? otherProductForgeAdapter.authenticatedAsCurrentUser()
        : connectProductsAdapter.authenticatedAsCurrentUser();
  }

  @Override
  public RestTemplate impersonation(AtlassianHostUser hostUser) {
    return isForge()
        ? otherProductForgeAdapter.impersonation(hostUser)
        : connectProductsAdapter.impersonation(hostUser);
  }
}
