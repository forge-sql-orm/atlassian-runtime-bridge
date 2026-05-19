package com.github.vzakharchenko.runtime.bridge.containers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ManualAuthorizationServiceImplTest {

  private static final String CLOUD_A = "cloud-a";
  private static final String CLOUD_B = "cloud-b";
  private static final String INSTALLATION = "ari:cloud:ecosystem::installation/inst-1";

  private ManualAuthorizationServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ManualAuthorizationServiceImpl();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authorize_withoutExistingAuthentication_createsForgeAuthentication() {
    service.authorize(CLOUD_A, INSTALLATION, Optional.of("account-1"));

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isInstanceOf(ForgeAuthentication.class);
    var user = (AtlassianHostUser) authentication.getPrincipal();
    assertThat(user.getHost().getCloudId()).isEqualTo(CLOUD_A);
    assertThat(user.getHost().getInstallationId()).isEqualTo(INSTALLATION);
    assertThat(user.getUserAccountId()).contains("account-1");
  }

  @Test
  void authorize_withoutAccountId_doesNotSetUserAccountId() {
    service.authorize(CLOUD_A, INSTALLATION, Optional.empty());

    var user =
        (AtlassianHostUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(user.getUserAccountId()).isEmpty();
  }

  @Test
  void authorize_withForgeHostUser_updatesAccountIdWhenPresent() {
    var existing = ContainersTestFixtures.hostUser(CLOUD_A, INSTALLATION, "old-account");
    SecurityContextHolder.getContext()
        .setAuthentication(ContainersTestFixtures.forgeAuthentication(existing));

    service.authorize(CLOUD_A, INSTALLATION, Optional.of("new-account"));

    var user =
        (AtlassianHostUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(user.getUserAccountId()).contains("new-account");
    assertThat(user.getHost().getCloudId()).isEqualTo(CLOUD_A);
  }

  @Test
  void authorize_withForgeHostUser_crossTenantFailsWithDetails() {
    var existing = ContainersTestFixtures.hostUser(CLOUD_A, INSTALLATION, "account");
    SecurityContextHolder.getContext()
        .setAuthentication(ContainersTestFixtures.forgeAuthentication(existing));

    Optional<String> noAccount = Optional.empty();
    assertThatThrownBy(() -> service.authorize(CLOUD_B, INSTALLATION, noAccount))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("expected cloudId=" + CLOUD_A)
        .hasMessageContaining("received cloudId=" + CLOUD_B);
  }

  @Test
  void authorize_atlassianHost_preservesHostUserAccountWhenPrincipalIsHostUser() {
    var hostUser = ContainersTestFixtures.hostUser(CLOUD_A, INSTALLATION, "account");
    SecurityContextHolder.getContext()
        .setAuthentication(ContainersTestFixtures.forgeAuthentication(hostUser));

    service.authorize(ContainersTestFixtures.host(CLOUD_A, INSTALLATION));

    var user =
        (AtlassianHostUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(user.getUserAccountId()).contains("account");
  }

  @Test
  void authorize_atlassianHostUser_delegatesToCloudInstallationAndAccount() {
    var hostUser = ContainersTestFixtures.hostUser(CLOUD_A, INSTALLATION, "account-99");
    SecurityContextHolder.getContext()
        .setAuthentication(ContainersTestFixtures.forgeAuthentication(hostUser));

    service.authorize(hostUser);

    var user =
        (AtlassianHostUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(user.getUserAccountId()).contains("account-99");
  }
}
