package com.github.vzakharchenko.runtime.bridge.containers.products;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.connect.spring.AtlassianHostUser;
import com.github.vzakharchenko.runtime.bridge.containers.ContainersTestFixtures;
import com.github.vzakharchenko.runtime.bridge.containers.EgressClientService;
import com.github.vzakharchenko.runtime.bridge.containers.EgressClientServiceImpl.InstallationAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class JiraProductAdapterImplTest {

  private static final String CLOUD_ID = "cloud";
  private static final String INSTALLATION = "ari:cloud:ecosystem::installation/inst-1";

  @Mock private EgressClientService egressClientService;

  private JiraProductAdapterImpl adapter;

  @BeforeEach
  void setUp() {
    adapter = new JiraProductAdapterImpl(egressClientService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticatedAsAddon_usesAppInstallationAuth() {
    var user = ContainersTestFixtures.hostUser(CLOUD_ID, INSTALLATION, "acc");
    SecurityContextHolder.getContext()
        .setAuthentication(ContainersTestFixtures.forgeAuthentication(user));
    RestTemplate restTemplate = new RestTemplate();
    when(egressClientService.jiraTemplateRequest(any())).thenReturn(restTemplate);

    assertThat(adapter.authenticatedAsAddon(user.getHost())).isSameAs(restTemplate);

    var auth = ArgumentCaptor.forClass(InstallationAuth.class);
    verify(egressClientService).jiraTemplateRequest(auth.capture());
    assertThat(auth.getValue()).isEqualTo(InstallationAuth.asApp(INSTALLATION));
  }

  @Test
  void authenticatedAsCurrentUser_usesUserInstallationAuth() {
    var user = ContainersTestFixtures.hostUser(CLOUD_ID, INSTALLATION, "acc-1");
    SecurityContextHolder.getContext()
        .setAuthentication(ContainersTestFixtures.forgeAuthentication(user));
    when(egressClientService.jiraTemplateRequest(any())).thenReturn(new RestTemplate());

    adapter.authenticatedAsCurrentUser();

    var auth = ArgumentCaptor.forClass(InstallationAuth.class);
    verify(egressClientService).jiraTemplateRequest(auth.capture());
    assertThat(auth.getValue()).isEqualTo(InstallationAuth.asUser(INSTALLATION, "acc-1"));
  }

  @Test
  void impersonation_usesTargetUserAccount() {
    var contextUser = ContainersTestFixtures.hostUser(CLOUD_ID, INSTALLATION, "ctx-acc");
    var targetUser = ContainersTestFixtures.hostUser(CLOUD_ID, INSTALLATION, "target-acc");
    SecurityContextHolder.getContext()
        .setAuthentication(ContainersTestFixtures.forgeAuthentication(contextUser));
    when(egressClientService.jiraTemplateRequest(any())).thenReturn(new RestTemplate());

    adapter.impersonation(targetUser);

    var auth = ArgumentCaptor.forClass(InstallationAuth.class);
    verify(egressClientService).jiraTemplateRequest(auth.capture());
    assertThat(auth.getValue()).isEqualTo(InstallationAuth.asUser(INSTALLATION, "target-acc"));
  }

  @Test
  void withoutForgeAuthentication_throws() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("x", "y"));

    AtlassianHostUser user = ContainersTestFixtures.hostUser(CLOUD_ID, INSTALLATION, "acc");
    assertThatThrownBy(() -> adapter.authenticatedAsAddon(user.getHost()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Not ForgeAuthentication");
  }
}
