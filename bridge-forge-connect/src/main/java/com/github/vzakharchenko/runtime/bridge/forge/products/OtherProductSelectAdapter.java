package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.AtlassianForgeRestClients;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.github.vzakharchenko.runtime.bridge.common.OtherProductAdapter;
import com.github.vzakharchenko.runtime.bridge.forge.ImpersonationUserService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

/**
 * Spring {@link Component} implementing {@link OtherProductAdapter} with automatic Connect vs Forge
 * delegation. See {@link AbstractProductSelectAdapter} for routing logic.
 */
@Component
public class OtherProductSelectAdapter extends AbstractProductSelectAdapter
    implements OtherProductAdapter {

  public OtherProductSelectAdapter(
      AtlassianHostRestClients atlassianHostRestClients,
      AtlassianForgeRestClients atlassianForgeRestClients,
      ForgeSecurityContextRetriever forgeSecurityContextRetriever,
      RestTemplateBuilder restTemplateBuilder,
      ImpersonationUserService impersonationUserService) {
    super(
        atlassianHostRestClients,
        new OtherProductForgeAdapter(
            atlassianForgeRestClients,
            forgeSecurityContextRetriever,
            restTemplateBuilder,
            impersonationUserService));
  }
}
