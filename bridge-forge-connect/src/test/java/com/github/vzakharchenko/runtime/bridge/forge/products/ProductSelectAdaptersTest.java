package com.github.vzakharchenko.runtime.bridge.forge.products;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.ForgeApiContext;
import com.atlassian.connect.spring.ForgeInvocationToken;
import com.atlassian.connect.spring.ForgeRequestProductMethods;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.github.vzakharchenko.runtime.bridge.forge.ImpersonationUserService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
@ExtendWith(MockitoExtension.class)
class ProductSelectAdaptersTest {

  @Mock private AtlassianHostRestClients atlassianHostRestClients;
  @Mock private ImpersonationUserService impersonationUserService;
  @Mock private ForgeRequestProductMethods addonMethods;
  @Mock private ForgeRequestProductMethods userMethods;

  private final StubForgeSecurityContextRetriever forgeSecurityContextRetriever =
      new StubForgeSecurityContextRetriever();

  private RestTemplateBuilder restTemplateBuilder;
  private StubAtlassianForgeRestClients forgeRestClients;

  @BeforeEach
  void setUp() {
    restTemplateBuilder = new RestTemplateBuilder();
    forgeRestClients = new StubAtlassianForgeRestClients(addonMethods, userMethods);
    forgeSecurityContextRetriever.setForgeApiContext(Optional.empty());
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private static AtlassianHost sampleHost() {
    AtlassianHost host = new AtlassianHost();
    host.setInstallationId("inst-1");
    return host;
  }

  private static AtlassianHostUser sampleUser(AtlassianHost host) {
    return AtlassianHostUser.builder(host).withUserAccountId("ari:user:1").build();
  }

  private static ForgeApiContext minimalForgeApiContext() {
    return new ForgeApiContext(new ForgeInvocationToken(), Optional.empty(), Optional.empty());
  }

  private void useConnectAuthentication() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("user", "cred"));
  }

  private void useForgeAuthentication() {
    AtlassianHost host = sampleHost();
    ForgeApiContext ctx = minimalForgeApiContext();
    SecurityContextHolder.getContext()
        .setAuthentication(new ForgeAuthentication(ctx, AtlassianHostUser.builder(host).build()));
  }

  @Nested
  class JiraProductSelectAdapterTests {

    private JiraProductSelectAdapter adapter;

    @BeforeEach
    void createAdapter() {
      adapter =
          new JiraProductSelectAdapter(
              atlassianHostRestClients,
              forgeRestClients,
              forgeSecurityContextRetriever,
              restTemplateBuilder,
              impersonationUserService);
    }

    @Test
    void authenticatedAsAddon_usesConnectWhenNotForge() {
      useConnectAuthentication();
      AtlassianHost host = sampleHost();
      RestTemplate connectRt = new RestTemplate();
      when(atlassianHostRestClients.authenticatedAsAddon(host)).thenReturn(connectRt);

      assertThat(adapter.authenticatedAsAddon(host)).isSameAs(connectRt);
      verify(atlassianHostRestClients).authenticatedAsAddon(host);
      verifyNoInteractions(addonMethods, userMethods);
    }

    @Test
    void authenticatedAsAddon_usesForgeWhenForgeAuthentication() {
      useForgeAuthentication();
      AtlassianHost host = sampleHost();
      RestTemplate forgeRt = new RestTemplate();
      when(addonMethods.requestJira()).thenReturn(forgeRt);

      assertThat(adapter.authenticatedAsAddon(host)).isSameAs(forgeRt);
      verify(addonMethods).requestJira();
      verifyNoInteractions(atlassianHostRestClients);
    }

    @Test
    void authenticatedAsCurrentUser_usesConnectWhenNotForge() {
      useConnectAuthentication();
      RestTemplate connectRt = new RestTemplate();
      when(atlassianHostRestClients.authenticatedAsHostActor()).thenReturn(connectRt);

      assertThat(adapter.authenticatedAsCurrentUser()).isSameAs(connectRt);
      verify(atlassianHostRestClients).authenticatedAsHostActor();
      verifyNoInteractions(addonMethods, userMethods);
    }

    @Test
    void authenticatedAsCurrentUser_usesForgeWhenForgeAuthentication() {
      useForgeAuthentication();
      RestTemplate forgeRt = new RestTemplate();
      when(userMethods.requestJira()).thenReturn(forgeRt);

      assertThat(adapter.authenticatedAsCurrentUser()).isSameAs(forgeRt);
      verify(userMethods).requestJira();
      verifyNoInteractions(atlassianHostRestClients);
    }

    @Test
    void impersonation_usesConnectWhenNotForge() {
      useConnectAuthentication();
      AtlassianHost host = sampleHost();
      AtlassianHostUser user = sampleUser(host);
      RestTemplate connectRt = new RestTemplate();
      when(atlassianHostRestClients.authenticatedAs(user)).thenReturn(connectRt);

      assertThat(adapter.impersonation(user)).isSameAs(connectRt);
      verify(atlassianHostRestClients).authenticatedAs(user);
      verifyNoInteractions(addonMethods, userMethods);
    }

    @Test
    void impersonation_usesForgeAdapterWhenForgeAuthentication() {
      useForgeAuthentication();
      AtlassianHost host = sampleHost();
      AtlassianHostUser user = sampleUser(host);

      assertThatThrownBy(() -> adapter.impersonation(user))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Forge API context not found");
      verifyNoInteractions(atlassianHostRestClients);
    }
  }

  @Nested
  class ConfluenceProductSelectAdapterTests {

    private ConfluenceProductSelectAdapter adapter;

    @BeforeEach
    void createAdapter() {
      adapter =
          new ConfluenceProductSelectAdapter(
              atlassianHostRestClients,
              forgeRestClients,
              forgeSecurityContextRetriever,
              restTemplateBuilder,
              impersonationUserService);
    }

    @Test
    void authenticatedAsAddon_usesForgeConfluenceWhenForgeAuthentication() {
      useForgeAuthentication();
      AtlassianHost host = sampleHost();
      RestTemplate forgeRt = new RestTemplate();
      when(addonMethods.requestConfluence()).thenReturn(forgeRt);

      assertThat(adapter.authenticatedAsAddon(host)).isSameAs(forgeRt);
      verify(addonMethods).requestConfluence();
    }

    @Test
    void authenticatedAsCurrentUser_usesConnectWhenNotForge() {
      useConnectAuthentication();
      RestTemplate connectRt = new RestTemplate();
      when(atlassianHostRestClients.authenticatedAsHostActor()).thenReturn(connectRt);

      assertThat(adapter.authenticatedAsCurrentUser()).isSameAs(connectRt);
      verify(atlassianHostRestClients).authenticatedAsHostActor();
    }
  }

  @Nested
  class OtherProductSelectAdapterTests {

    private OtherProductSelectAdapter adapter;

    @BeforeEach
    void createAdapter() {
      adapter =
          new OtherProductSelectAdapter(
              atlassianHostRestClients,
              forgeRestClients,
              forgeSecurityContextRetriever,
              restTemplateBuilder,
              impersonationUserService);
    }

    @Test
    void authenticatedAsAddon_usesForgeGenericRequestWhenForgeAuthentication() {
      useForgeAuthentication();
      AtlassianHost host = sampleHost();
      RestTemplate forgeRt = new RestTemplate();
      when(addonMethods.request()).thenReturn(forgeRt);

      assertThat(adapter.authenticatedAsAddon(host)).isSameAs(forgeRt);
      verify(addonMethods).request();
    }

    @Test
    void impersonation_usesConnectWhenNotForge() {
      useConnectAuthentication();
      AtlassianHost host = sampleHost();
      AtlassianHostUser user = sampleUser(host);
      RestTemplate connectRt = new RestTemplate();
      when(atlassianHostRestClients.authenticatedAs(user)).thenReturn(connectRt);

      assertThat(adapter.impersonation(user)).isSameAs(connectRt);
      verify(atlassianHostRestClients).authenticatedAs(user);
    }
  }
}
