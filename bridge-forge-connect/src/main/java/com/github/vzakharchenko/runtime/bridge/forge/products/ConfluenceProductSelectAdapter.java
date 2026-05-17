package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.AtlassianForgeRestClients;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.github.vzakharchenko.runtime.bridge.common.ConfluenceProductAdapter;
import com.github.vzakharchenko.runtime.bridge.forge.ImpersonationUserService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

/**
 * Spring {@link Component} implementing {@link ConfluenceProductAdapter} with automatic Connect vs
 * Forge delegation. See {@link AbstractProductSelectAdapter} for routing logic.
 */
@Component
public class ConfluenceProductSelectAdapter extends AbstractProductSelectAdapter
    implements ConfluenceProductAdapter {

  public ConfluenceProductSelectAdapter(
      AtlassianHostRestClients atlassianHostRestClients,
      AtlassianForgeRestClients atlassianForgeRestClients,
      ForgeSecurityContextRetriever forgeSecurityContextRetriever,
      RestTemplateBuilder restTemplateBuilder,
      ImpersonationUserService impersonationUserService) {
    super(
        atlassianHostRestClients,
        new ConfluenceProductForgeAdapter(
            atlassianForgeRestClients,
            forgeSecurityContextRetriever,
            restTemplateBuilder,
            impersonationUserService));
  }
}
