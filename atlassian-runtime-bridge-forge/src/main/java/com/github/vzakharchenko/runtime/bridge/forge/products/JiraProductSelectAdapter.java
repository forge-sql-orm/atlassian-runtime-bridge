package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.AtlassianForgeRestClients;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.github.vzakharchenko.runtime.bridge.common.JiraProductAdapter;
import com.github.vzakharchenko.runtime.bridge.forge.ImpersonationUserService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

/**
 * Spring {@link Component} implementing {@link JiraProductAdapter} with automatic Connect vs Forge
 * delegation. See {@link AbstractProductSelectAdapter} for routing logic.
 */
@Component
public class JiraProductSelectAdapter extends AbstractProductSelectAdapter
    implements JiraProductAdapter {

  public JiraProductSelectAdapter(
      AtlassianHostRestClients atlassianHostRestClients,
      AtlassianForgeRestClients atlassianForgeRestClients,
      ForgeSecurityContextRetriever forgeSecurityContextRetriever,
      RestTemplateBuilder restTemplateBuilder,
      ImpersonationUserService impersonationUserService) {
    super(
        atlassianHostRestClients,
        new JiraProductForgeAdapter(
            atlassianForgeRestClients,
            forgeSecurityContextRetriever,
            restTemplateBuilder,
            impersonationUserService));
  }
}
