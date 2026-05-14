package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.AtlassianForgeRestClients;
import com.atlassian.connect.spring.ForgeRequestProductMethods;
import org.springframework.web.client.RestTemplate;

/**
 * Test double for {@link AtlassianForgeRestClients}: the Connect type is a concrete class, so
 * Mockito inline mocking fails on some JDKs when combined with other mocks.
 */
public final class StubAtlassianForgeRestClients extends AtlassianForgeRestClients {

  private final ForgeRequestProductMethods addonMethods;
  private final ForgeRequestProductMethods userMethods;

  public StubAtlassianForgeRestClients(
      ForgeRequestProductMethods addonMethods, ForgeRequestProductMethods userMethods) {
    this.addonMethods = addonMethods;
    this.userMethods = userMethods;
  }

  @Override
  public ForgeRequestProductMethods asApp() {
    return addonMethods;
  }

  @Override
  public ForgeRequestProductMethods asApp(String installationId) {
    return addonMethods;
  }

  @Override
  public ForgeRequestProductMethods asUser() {
    return userMethods;
  }

  @Override
  public RestTemplate request() {
    return addonMethods.request();
  }

  @Override
  public RestTemplate requestConfluence() {
    return addonMethods.requestConfluence();
  }

  @Override
  public RestTemplate requestJira() {
    return addonMethods.requestJira();
  }
}
