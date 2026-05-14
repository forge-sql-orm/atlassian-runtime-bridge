package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.*;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.github.vzakharchenko.runtime.bridge.forge.AtlassianForgeUtils;
import com.github.vzakharchenko.runtime.bridge.forge.ImpersonationUserService;
import java.util.Optional;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

/**
 * Shared Forge-side plumbing for product {@link RestTemplate}s: resolves {@link ForgeApiContext},
 * builds user bearer tokens via {@link ImpersonationUserService}, and configures roots/headers.
 *
 * <p>Concrete product adapters ({@link JiraProductForgeAdapter}, {@link
 * ConfluenceProductForgeAdapter}, {@link OtherProductForgeAdapter}) only choose which Forge client
 * method ({@code requestJira()}, {@code requestConfluence()}, {@code request()}) to call for addon
 * and current-user modes.
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class AbstractProductForgeAdapter {

  private final AtlassianForgeRestClients atlassianForgeRestClients;
  private final ForgeSecurityContextRetriever forgeSecurityContextRetriever;
  private final RestTemplateBuilder restTemplateBuilder;
  private final ImpersonationUserService impersonationUserService;

  protected AbstractProductForgeAdapter(
      AtlassianForgeRestClients atlassianForgeRestClients,
      ForgeSecurityContextRetriever forgeSecurityContextRetriever,
      RestTemplateBuilder restTemplateBuilder,
      ImpersonationUserService impersonationUserService) {
    this.atlassianForgeRestClients = atlassianForgeRestClients;
    this.forgeSecurityContextRetriever = forgeSecurityContextRetriever;
    this.restTemplateBuilder = restTemplateBuilder;
    this.impersonationUserService = impersonationUserService;
  }

  protected ForgeRequestProductMethods asAddon(AtlassianHost host) {
    return atlassianForgeRestClients.asApp(host.getInstallationId());
  }

  protected ForgeRequestProductMethods asCurrentUser() {
    return atlassianForgeRestClients.asUser();
  }

  public RestTemplate impersonation(AtlassianHostUser hostUser) {
    Optional<ForgeApiContext> forgeApiContext = forgeSecurityContextRetriever.getForgeApiContext();
    if (forgeApiContext.isEmpty()) {
      throw new IllegalStateException(
          "Forge API context not found. Maybe you run outside of request thread. "
              + "You can set up authorization manually using ManualAuthorizationService");
    }
    String apiToken = forgeApiContext.get().getAppToken().orElseThrow();
    var apiBaseUrl = forgeApiContext.get().getForgeInvocationToken().getApp().getApiBaseUrl();
    String userToken =
        impersonationUserService.impersonateUser(
            hostUser.getUserAccountId().orElseThrow(),
            AtlassianForgeUtils.getContextId(apiBaseUrl),
            apiToken);
    return authenticatedWithSystemApiToken(apiBaseUrl, userToken);
  }

  public RestTemplate authenticatedWithSystemApiToken(String baseUrl, String apiToken) {
    return restTemplateBuilder
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
        .rootUri(baseUrl)
        .build();
  }
}
