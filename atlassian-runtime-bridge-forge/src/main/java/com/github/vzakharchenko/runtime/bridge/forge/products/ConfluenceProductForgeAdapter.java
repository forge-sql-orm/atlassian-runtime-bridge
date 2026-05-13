package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.AtlassianForgeRestClients;
import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.github.vzakharchenko.runtime.bridge.common.ConfluenceProductAdapter;
import com.github.vzakharchenko.runtime.bridge.forge.ImpersonationUserService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

/**
 * Forge path for <strong>Confluence</strong> REST: {@code requestConfluence()} on {@link
 * com.atlassian.connect.spring.AtlassianForgeRestClients}. Impersonation is shared via {@link
 * AbstractProductForgeAdapter}.
 */
public class ConfluenceProductForgeAdapter extends AbstractProductForgeAdapter
    implements ConfluenceProductAdapter {

  public ConfluenceProductForgeAdapter(
      AtlassianForgeRestClients atlassianForgeRestClients,
      ForgeSecurityContextRetriever forgeSecurityContextRetriever,
      RestTemplateBuilder restTemplateBuilder,
      ImpersonationUserService impersonationUserService) {
    super(
        atlassianForgeRestClients,
        forgeSecurityContextRetriever,
        restTemplateBuilder,
        impersonationUserService);
  }

  @Override
  public RestTemplate authenticatedAsAddon(AtlassianHost host) {
    return asAddon(host).requestConfluence();
  }

  @Override
  public RestTemplate authenticatedAsCurrentUser() {
    return asCurrentUser().requestConfluence();
  }
}
