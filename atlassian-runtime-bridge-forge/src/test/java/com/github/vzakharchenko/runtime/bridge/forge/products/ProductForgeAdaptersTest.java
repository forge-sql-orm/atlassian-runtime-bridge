package com.github.vzakharchenko.runtime.bridge.forge.products;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.ForgeApiContext;
import com.atlassian.connect.spring.ForgeApp;
import com.atlassian.connect.spring.ForgeInvocationToken;
import com.atlassian.connect.spring.ForgeRequestProductMethods;
import com.github.vzakharchenko.runtime.bridge.forge.ImpersonationUserService;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
@ExtendWith(MockitoExtension.class)
class ProductForgeAdaptersTest {

  private static final class RecordingImpersonation implements ImpersonationUserService {

    String lastUserId;
    String lastContextId;
    String lastAuthHeader;

    @Override
    public String impersonateUser(String userId, String contextId, String authHeader) {
      lastUserId = userId;
      lastContextId = contextId;
      lastAuthHeader = authHeader;
      return "user-jwt";
    }
  }

  @Mock private ForgeRequestProductMethods addonMethods;
  @Mock private ForgeRequestProductMethods userMethods;

  private final StubForgeSecurityContextRetriever forgeSecurityContextRetriever =
      new StubForgeSecurityContextRetriever();
  private final RecordingImpersonation recordingImpersonation = new RecordingImpersonation();

  private RestTemplateBuilder restTemplateBuilder;
  private StubAtlassianForgeRestClients forgeRestClients;

  @BeforeEach
  void setUp() {
    restTemplateBuilder = new RestTemplateBuilder();
    forgeSecurityContextRetriever.setForgeApiContext(Optional.empty());
    forgeRestClients = new StubAtlassianForgeRestClients(addonMethods, userMethods);
  }

  private static AtlassianHost sampleHost() {
    AtlassianHost host = new AtlassianHost();
    host.setInstallationId("inst-forge-1");
    return host;
  }

  private static AtlassianHostUser hostUserWithAccount(AtlassianHost host) {
    return AtlassianHostUser.builder(host).withUserAccountId("ari:cloud:identity::user/99").build();
  }

  private static ForgeApiContext forgeContextWithAppToken(String apiBaseUrl, String appToken) {
    ForgeApp forgeApp = new ForgeApp();
    forgeApp.setInstallationId("inst-forge-1");
    forgeApp.setApiBaseUrl(apiBaseUrl);
    ForgeInvocationToken forgeInvocationToken = new ForgeInvocationToken();
    forgeInvocationToken.setApp(forgeApp);
    return new ForgeApiContext(forgeInvocationToken, Optional.empty(), Optional.of(appToken));
  }

  @Nested
  class JiraPaths {

    @Test
    void authenticatedAsAddon_requestsJiraTemplateFromForgeAsApp() {
      AtlassianHost host = sampleHost();
      RestTemplate expected = new RestTemplate();
      when(addonMethods.requestJira()).thenReturn(expected);

      JiraProductForgeAdapter adapter =
          new JiraProductForgeAdapter(
              forgeRestClients,
              forgeSecurityContextRetriever,
              restTemplateBuilder,
              recordingImpersonation);

      assertThat(adapter.authenticatedAsAddon(host)).isSameAs(expected);
      verify(addonMethods).requestJira();
    }

    @Test
    void authenticatedAsCurrentUser_requestsJiraTemplateFromForgeAsUser() {
      RestTemplate expected = new RestTemplate();
      when(userMethods.requestJira()).thenReturn(expected);

      JiraProductForgeAdapter adapter =
          new JiraProductForgeAdapter(
              forgeRestClients,
              forgeSecurityContextRetriever,
              restTemplateBuilder,
              recordingImpersonation);

      assertThat(adapter.authenticatedAsCurrentUser()).isSameAs(expected);
      verify(userMethods).requestJira();
    }
  }

  @Nested
  class ConfluencePaths {

    @Test
    void authenticatedAsAddon_requestsConfluenceTemplateFromForgeAsApp() {
      AtlassianHost host = sampleHost();
      RestTemplate expected = new RestTemplate();
      when(addonMethods.requestConfluence()).thenReturn(expected);

      ConfluenceProductForgeAdapter adapter =
          new ConfluenceProductForgeAdapter(
              forgeRestClients,
              forgeSecurityContextRetriever,
              restTemplateBuilder,
              recordingImpersonation);

      assertThat(adapter.authenticatedAsAddon(host)).isSameAs(expected);
      verify(addonMethods).requestConfluence();
    }

    @Test
    void authenticatedAsCurrentUser_requestsConfluenceTemplateFromForgeAsUser() {
      RestTemplate expected = new RestTemplate();
      when(userMethods.requestConfluence()).thenReturn(expected);

      ConfluenceProductForgeAdapter adapter =
          new ConfluenceProductForgeAdapter(
              forgeRestClients,
              forgeSecurityContextRetriever,
              restTemplateBuilder,
              recordingImpersonation);

      assertThat(adapter.authenticatedAsCurrentUser()).isSameAs(expected);
      verify(userMethods).requestConfluence();
    }
  }

  @Nested
  class OtherPaths {

    @Test
    void authenticatedAsAddon_requestsGenericTemplateFromForgeAsApp() {
      AtlassianHost host = sampleHost();
      RestTemplate expected = new RestTemplate();
      when(addonMethods.request()).thenReturn(expected);

      OtherProductForgeAdapter adapter =
          new OtherProductForgeAdapter(
              forgeRestClients,
              forgeSecurityContextRetriever,
              restTemplateBuilder,
              recordingImpersonation);

      assertThat(adapter.authenticatedAsAddon(host)).isSameAs(expected);
      verify(addonMethods).request();
    }

    @Test
    void authenticatedAsCurrentUser_requestsGenericTemplateFromForgeAsUser() {
      RestTemplate expected = new RestTemplate();
      when(userMethods.request()).thenReturn(expected);

      OtherProductForgeAdapter adapter =
          new OtherProductForgeAdapter(
              forgeRestClients,
              forgeSecurityContextRetriever,
              restTemplateBuilder,
              recordingImpersonation);

      assertThat(adapter.authenticatedAsCurrentUser()).isSameAs(expected);
      verify(userMethods).request();
    }
  }

  @Nested
  class ImpersonationAndContext {

    private JiraProductForgeAdapter jiraAdapter;

    @BeforeEach
    void createAdapter() {
      jiraAdapter =
          new JiraProductForgeAdapter(
              forgeRestClients,
              forgeSecurityContextRetriever,
              restTemplateBuilder,
              recordingImpersonation);
    }

    @Test
    void impersonation_throwsWhenForgeApiContextMissing() {
      AtlassianHost host = sampleHost();
      AtlassianHostUser user = hostUserWithAccount(host);

      assertThatThrownBy(() -> jiraAdapter.impersonation(user))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Forge API context not found");
    }

    @Test
    void impersonation_passesContextIdAndAppTokenToImpersonationService() {
      String apiBase = "https://api.atlassian.com/ex/jira/site-abc";
      forgeSecurityContextRetriever.setForgeApiContext(
          Optional.of(forgeContextWithAppToken(apiBase, "fit-app-token")));

      AtlassianHost host = sampleHost();
      AtlassianHostUser user = hostUserWithAccount(host);

      assertThat(jiraAdapter.impersonation(user)).isNotNull();
      assertThat(recordingImpersonation.lastUserId).isEqualTo("ari:cloud:identity::user/99");
      assertThat(recordingImpersonation.lastContextId).isEqualTo("ari:cloud:jira::site/site-abc");
      assertThat(recordingImpersonation.lastAuthHeader).isEqualTo("fit-app-token");
    }

    @Test
    void impersonation_throwsWhenUserAccountIdMissing() {
      forgeSecurityContextRetriever.setForgeApiContext(
          Optional.of(forgeContextWithAppToken("https://api.atlassian.com/ex/jira/x", "t")));

      AtlassianHost host = sampleHost();
      AtlassianHostUser user = AtlassianHostUser.builder(host).build();

      assertThatThrownBy(() -> jiraAdapter.impersonation(user))
          .isInstanceOf(NoSuchElementException.class);
    }
  }
}
