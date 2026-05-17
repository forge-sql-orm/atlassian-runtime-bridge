package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.AtlassianForgeRestClients;
import com.atlassian.connect.spring.ForgeRequestProductMethods;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.github.vzakharchenko.runtime.bridge.common.ConfluenceProductAdapter;
import com.github.vzakharchenko.runtime.bridge.forge.ImpersonationUserService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

/** Forge path for <strong>Confluence</strong> REST: delegates to {@code requestConfluence()}. */
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
  protected RestTemplate forgeMethod(ForgeRequestProductMethods methods) {
    return methods.requestConfluence();
  }
}
