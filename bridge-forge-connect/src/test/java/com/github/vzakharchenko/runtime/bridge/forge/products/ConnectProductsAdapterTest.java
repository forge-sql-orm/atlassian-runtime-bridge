package com.github.vzakharchenko.runtime.bridge.forge.products;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import com.atlassian.connect.spring.AtlassianHostUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
@ExtendWith(MockitoExtension.class)
class ConnectProductsAdapterTest {

  @Mock private AtlassianHostRestClients atlassianHostRestClients;

  private ConnectProductsAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new ConnectProductsAdapter(atlassianHostRestClients);
  }

  @Test
  void authenticatedAsAddon_delegatesToConnectRestClients() {
    AtlassianHost host = new AtlassianHost();
    host.setInstallationId("inst-1");
    RestTemplate expected = new RestTemplate();
    when(atlassianHostRestClients.authenticatedAsAddon(host)).thenReturn(expected);

    assertThat(adapter.authenticatedAsAddon(host)).isSameAs(expected);
    verify(atlassianHostRestClients).authenticatedAsAddon(host);
  }

  @Test
  void authenticatedAsCurrentUser_delegatesToHostActor() {
    RestTemplate expected = new RestTemplate();
    when(atlassianHostRestClients.authenticatedAsHostActor()).thenReturn(expected);

    assertThat(adapter.authenticatedAsCurrentUser()).isSameAs(expected);
    verify(atlassianHostRestClients).authenticatedAsHostActor();
  }

  @Test
  void impersonation_delegatesToAuthenticatedAsHostUser() {
    AtlassianHost host = new AtlassianHost();
    host.setInstallationId("inst-1");
    AtlassianHostUser user =
        AtlassianHostUser.builder(host).withUserAccountId("ari:user:1").build();
    RestTemplate expected = new RestTemplate();
    when(atlassianHostRestClients.authenticatedAs(user)).thenReturn(expected);

    assertThat(adapter.impersonation(user)).isSameAs(expected);
    verify(atlassianHostRestClients).authenticatedAs(user);
  }
}
