package com.github.vzakharchenko.runtime.bridge.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.client.RestTemplate;

/**
 * Contract tests for product adapter interfaces: each exposes addon, current-user, and
 * impersonation {@link RestTemplate} entry points with the expected arguments. The same five
 * assertions run against Jira, Confluence, and Other stubs via {@link ParameterizedTest}.
 */
class ProductAdaptersContractTest {

  private static final String ADAPTERS_SOURCE = "adapters";
  private static final String DISPLAY_NAME = "{0}";

  @SuppressWarnings("UnusedMethod") // referenced by @MethodSource(ADAPTERS_SOURCE) via reflection
  private static Stream<Arguments> adapters() {
    return Stream.of(
        Arguments.of(Named.<StubProductAdapterBase>of("Jira", new StubJiraProductAdapter())),
        Arguments.of(
            Named.<StubProductAdapterBase>of("Confluence", new StubConfluenceProductAdapter())),
        Arguments.of(Named.<StubProductAdapterBase>of("Other", new StubOtherProductAdapter())));
  }

  private static AtlassianHost sampleHost() {
    AtlassianHost host = new AtlassianHost();
    host.setInstallationId("inst-1");
    host.setCloudId("cloud-1");
    return host;
  }

  private static AtlassianHostUser sampleUser(AtlassianHost host) {
    return AtlassianHostUser.builder(host).withUserAccountId("ari:cloud:identity::user/1").build();
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource(ADAPTERS_SOURCE)
  void authenticatedAsAddonReturnsConfiguredTemplate(StubProductAdapterBase adapter) {
    AtlassianHost host = sampleHost();
    RestTemplate expected = new RestTemplate();
    adapter.configureAddon(expected);

    assertThat(adapter.authenticatedAsAddon(host)).isSameAs(expected);
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource(ADAPTERS_SOURCE)
  void authenticatedAsAddonReceivesHost(StubProductAdapterBase adapter) {
    AtlassianHost host = sampleHost();
    adapter.authenticatedAsAddon(host);

    assertThat(adapter.lastAddonHost).isSameAs(host);
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource(ADAPTERS_SOURCE)
  void authenticatedAsCurrentUserReturnsConfiguredTemplate(StubProductAdapterBase adapter) {
    RestTemplate expected = new RestTemplate();
    adapter.configureCurrentUser(expected);

    assertThat(adapter.authenticatedAsCurrentUser()).isSameAs(expected);
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource(ADAPTERS_SOURCE)
  void impersonationReturnsConfiguredTemplate(StubProductAdapterBase adapter) {
    AtlassianHost host = sampleHost();
    AtlassianHostUser user = sampleUser(host);
    RestTemplate expected = new RestTemplate();
    adapter.configureImpersonation(expected);

    assertThat(adapter.impersonation(user)).isSameAs(expected);
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource(ADAPTERS_SOURCE)
  void impersonationReceivesHostUser(StubProductAdapterBase adapter) {
    AtlassianHost host = sampleHost();
    AtlassianHostUser user = sampleUser(host);
    adapter.impersonation(user);

    assertThat(adapter.lastImpersonationUser).isSameAs(user);
  }

  private abstract static class StubProductAdapterBase {
    AtlassianHost lastAddonHost;
    AtlassianHostUser lastImpersonationUser;
    private RestTemplate addonClient = new RestTemplate();
    private RestTemplate currentUserClient = new RestTemplate();
    private RestTemplate impersonationClient = new RestTemplate();

    abstract RestTemplate authenticatedAsAddon(AtlassianHost host);

    abstract RestTemplate authenticatedAsCurrentUser();

    abstract RestTemplate impersonation(AtlassianHostUser hostUser);

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
