package com.github.vzakharchenko.runtime.bridge.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

/**
 * Contract tests for product adapter interfaces: each exposes addon, current-user, and
 * impersonation {@link RestTemplate} entry points with the expected arguments.
 */
class ProductAdaptersContractTest {

  private static AtlassianHost sampleHost() {
    AtlassianHost host = new AtlassianHost();
    host.setInstallationId("inst-1");
    host.setCloudId("cloud-1");
    return host;
  }

  private static AtlassianHostUser sampleUser(AtlassianHost host) {
    return AtlassianHostUser.builder(host).withUserAccountId("ari:cloud:identity::user/1").build();
  }

  @Nested
  class JiraProductAdapterContract {

    private StubJiraProductAdapter adapter;

    @BeforeEach
    void createAdapter() {
      adapter = new StubJiraProductAdapter();
    }

    @Test
    void authenticatedAsAddonReturnsConfiguredTemplate() {
      AtlassianHost host = sampleHost();
      RestTemplate expected = new RestTemplate();
      adapter.configureAddon(expected);

      assertThat(adapter.authenticatedAsAddon(host)).isSameAs(expected);
    }

    @Test
    void authenticatedAsAddonReceivesHost() {
      AtlassianHost host = sampleHost();
      adapter.authenticatedAsAddon(host);

      assertThat(adapter.lastAddonHost).isSameAs(host);
    }

    @Test
    void authenticatedAsCurrentUserReturnsConfiguredTemplate() {
      RestTemplate expected = new RestTemplate();
      adapter.configureCurrentUser(expected);

      assertThat(adapter.authenticatedAsCurrentUser()).isSameAs(expected);
    }

    @Test
    void impersonationReturnsConfiguredTemplate() {
      AtlassianHost host = sampleHost();
      AtlassianHostUser user = sampleUser(host);
      RestTemplate expected = new RestTemplate();
      adapter.configureImpersonation(expected);

      assertThat(adapter.impersonation(user)).isSameAs(expected);
    }

    @Test
    void impersonationReceivesHostUser() {
      AtlassianHost host = sampleHost();
      AtlassianHostUser user = sampleUser(host);
      adapter.impersonation(user);

      assertThat(adapter.lastImpersonationUser).isSameAs(user);
    }
  }

  @Nested
  class ConfluenceProductAdapterContract {

    private StubConfluenceProductAdapter adapter;

    @BeforeEach
    void createAdapter() {
      adapter = new StubConfluenceProductAdapter();
    }

    @Test
    void authenticatedAsAddonReturnsConfiguredTemplate() {
      AtlassianHost host = sampleHost();
      RestTemplate expected = new RestTemplate();
      adapter.configureAddon(expected);

      assertThat(adapter.authenticatedAsAddon(host)).isSameAs(expected);
    }

    @Test
    void authenticatedAsAddonReceivesHost() {
      AtlassianHost host = sampleHost();
      adapter.authenticatedAsAddon(host);

      assertThat(adapter.lastAddonHost).isSameAs(host);
    }

    @Test
    void authenticatedAsCurrentUserReturnsConfiguredTemplate() {
      RestTemplate expected = new RestTemplate();
      adapter.configureCurrentUser(expected);

      assertThat(adapter.authenticatedAsCurrentUser()).isSameAs(expected);
    }

    @Test
    void impersonationReturnsConfiguredTemplate() {
      AtlassianHost host = sampleHost();
      AtlassianHostUser user = sampleUser(host);
      RestTemplate expected = new RestTemplate();
      adapter.configureImpersonation(expected);

      assertThat(adapter.impersonation(user)).isSameAs(expected);
    }

    @Test
    void impersonationReceivesHostUser() {
      AtlassianHost host = sampleHost();
      AtlassianHostUser user = sampleUser(host);
      adapter.impersonation(user);

      assertThat(adapter.lastImpersonationUser).isSameAs(user);
    }
  }

  @Nested
  class OtherProductAdapterContract {

    private StubOtherProductAdapter adapter;

    @BeforeEach
    void createAdapter() {
      adapter = new StubOtherProductAdapter();
    }

    @Test
    void authenticatedAsAddonReturnsConfiguredTemplate() {
      AtlassianHost host = sampleHost();
      RestTemplate expected = new RestTemplate();
      adapter.configureAddon(expected);

      assertThat(adapter.authenticatedAsAddon(host)).isSameAs(expected);
    }

    @Test
    void authenticatedAsAddonReceivesHost() {
      AtlassianHost host = sampleHost();
      adapter.authenticatedAsAddon(host);

      assertThat(adapter.lastAddonHost).isSameAs(host);
    }

    @Test
    void authenticatedAsCurrentUserReturnsConfiguredTemplate() {
      RestTemplate expected = new RestTemplate();
      adapter.configureCurrentUser(expected);

      assertThat(adapter.authenticatedAsCurrentUser()).isSameAs(expected);
    }

    @Test
    void impersonationReturnsConfiguredTemplate() {
      AtlassianHost host = sampleHost();
      AtlassianHostUser user = sampleUser(host);
      RestTemplate expected = new RestTemplate();
      adapter.configureImpersonation(expected);

      assertThat(adapter.impersonation(user)).isSameAs(expected);
    }

    @Test
    void impersonationReceivesHostUser() {
      AtlassianHost host = sampleHost();
      AtlassianHostUser user = sampleUser(host);
      adapter.impersonation(user);

      assertThat(adapter.lastImpersonationUser).isSameAs(user);
    }
  }

  private static class StubProductAdapterBase {
    AtlassianHost lastAddonHost;
    AtlassianHostUser lastImpersonationUser;
    private RestTemplate addonClient = new RestTemplate();
    private RestTemplate currentUserClient = new RestTemplate();
    private RestTemplate impersonationClient = new RestTemplate();

    void configureAddon(RestTemplate template) {
      addonClient = template;
    }

    void configureCurrentUser(RestTemplate template) {
      currentUserClient = template;
    }

    void configureImpersonation(RestTemplate template) {
      impersonationClient = template;
    }

    RestTemplate restTemplateForAddon() {
      return addonClient;
    }

    RestTemplate restTemplateForCurrentUser() {
      return currentUserClient;
    }

    RestTemplate restTemplateForImpersonation() {
      return impersonationClient;
    }
  }

  private static final class StubJiraProductAdapter extends StubProductAdapterBase
      implements JiraProductAdapter {

    @Override
    public RestTemplate authenticatedAsAddon(AtlassianHost host) {
      lastAddonHost = host;
      return restTemplateForAddon();
    }

    @Override
    public RestTemplate authenticatedAsCurrentUser() {
      return restTemplateForCurrentUser();
    }

    @Override
    public RestTemplate impersonation(AtlassianHostUser hostUser) {
      lastImpersonationUser = hostUser;
      return restTemplateForImpersonation();
    }
  }

  private static final class StubConfluenceProductAdapter extends StubProductAdapterBase
      implements ConfluenceProductAdapter {

    @Override
    public RestTemplate authenticatedAsAddon(AtlassianHost host) {
      lastAddonHost = host;
      return restTemplateForAddon();
    }

    @Override
    public RestTemplate authenticatedAsCurrentUser() {
      return restTemplateForCurrentUser();
    }

    @Override
    public RestTemplate impersonation(AtlassianHostUser hostUser) {
      lastImpersonationUser = hostUser;
      return restTemplateForImpersonation();
    }
  }

  private static final class StubOtherProductAdapter extends StubProductAdapterBase
      implements OtherProductAdapter {

    @Override
    public RestTemplate authenticatedAsAddon(AtlassianHost host) {
      lastAddonHost = host;
      return restTemplateForAddon();
    }

    @Override
    public RestTemplate authenticatedAsCurrentUser() {
      return restTemplateForCurrentUser();
    }

    @Override
    public RestTemplate impersonation(AtlassianHostUser hostUser) {
      lastImpersonationUser = hostUser;
      return restTemplateForImpersonation();
    }
  }
}
