package com.github.vzakharchenko.runtime.bridge.forge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ManualAuthorizationServiceImplTest {

  private static final String CREDENTIALS_PLACEHOLDER = "n/a";

  @Mock private AtlassianForgeSecurityBridgeService forgeSecurityBridgeService;

  private ManualAuthorizationServiceImpl manualAuthorizationServiceImpl;

  @BeforeEach
  void setUp() {
    manualAuthorizationServiceImpl = new ManualAuthorizationServiceImpl(forgeSecurityBridgeService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  class AuthorizeAtlassianHostUser {

    private AtlassianHostUser atlassianHostUser;

    @BeforeEach
    void prepareUser() {
      AtlassianHost host = new AtlassianHost();
      host.setClientKey("ck");
      atlassianHostUser = AtlassianHostUser.builder(host).withUserAccountId("ari:user:1").build();
    }

    @Test
    void requestsAuthenticationFromBridge() {
      Authentication authentication =
          new UsernamePasswordAuthenticationToken("delegated", CREDENTIALS_PLACEHOLDER);
      when(forgeSecurityBridgeService.getAuthentication(same(atlassianHostUser)))
          .thenReturn(authentication);

      manualAuthorizationServiceImpl.authorize(atlassianHostUser);

      verify(forgeSecurityBridgeService).getAuthentication(same(atlassianHostUser));
    }

    @Test
    void installsReturnedAuthenticationInSecurityContext() {
      Authentication authentication =
          new UsernamePasswordAuthenticationToken("delegated", CREDENTIALS_PLACEHOLDER);
      when(forgeSecurityBridgeService.getAuthentication(same(atlassianHostUser)))
          .thenReturn(authentication);

      manualAuthorizationServiceImpl.authorize(atlassianHostUser);

      assertThat(SecurityContextHolder.getContext().getAuthentication())
          .as("SecurityContext must hold the Authentication produced by the bridge")
          .isSameAs(authentication);
    }
  }

  @Nested
  class AuthorizeAtlassianHost {

    private AtlassianHost atlassianHost;

    @BeforeEach
    void prepareHost() {
      atlassianHost = new AtlassianHost();
      atlassianHost.setClientKey("installation-only");
    }

    @Test
    void requestsInstallationAuthenticationFromBridge() {
      Authentication authentication =
          new UsernamePasswordAuthenticationToken("app", CREDENTIALS_PLACEHOLDER);
      when(forgeSecurityBridgeService.getAuthentication(same(atlassianHost)))
          .thenReturn(authentication);

      manualAuthorizationServiceImpl.authorize(atlassianHost);

      verify(forgeSecurityBridgeService).getAuthentication(same(atlassianHost));
    }

    @Test
    void installsReturnedInstallationAuthenticationInSecurityContext() {
      Authentication authentication =
          new UsernamePasswordAuthenticationToken("app", CREDENTIALS_PLACEHOLDER);
      when(forgeSecurityBridgeService.getAuthentication(same(atlassianHost)))
          .thenReturn(authentication);

      manualAuthorizationServiceImpl.authorize(atlassianHost);

      assertThat(SecurityContextHolder.getContext().getAuthentication())
          .as("SecurityContext must hold installation-level Authentication from the bridge")
          .isSameAs(authentication);
    }
  }

  @Nested
  class AuthorizeByCloudInstallationAndAccount {

    @Test
    void rejectsNullCloudId() {
      Optional<String> noAccount = Optional.empty();
      assertThatThrownBy(
              () -> manualAuthorizationServiceImpl.authorize(null, "installation", noAccount))
          .as("cloudId is required for manual host materialization")
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullInstallationId() {
      Optional<String> noAccount = Optional.empty();
      assertThatThrownBy(() -> manualAuthorizationServiceImpl.authorize("cloud", null, noAccount))
          .as("installationId is required for manual host materialization")
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullAccountIdOptional() {
      assertThatThrownBy(
              () -> manualAuthorizationServiceImpl.authorize("cloud", "installation", null))
          .as("accountId Optional wrapper must not be null")
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    void blankAccountIdAuthorizesInstallationOnly() {
      Authentication installationAuth =
          new UsernamePasswordAuthenticationToken("installation", CREDENTIALS_PLACEHOLDER);
      when(forgeSecurityBridgeService.getAuthentication(any(AtlassianHost.class)))
          .thenReturn(installationAuth);

      manualAuthorizationServiceImpl.authorize("cloud-1", "inst-1", Optional.of("   "));

      verify(forgeSecurityBridgeService).getAuthentication(any(AtlassianHost.class));
    }

    @Test
    void nonBlankAccountIdAuthorizesUser() {
      Authentication userAuth =
          new UsernamePasswordAuthenticationToken("user", CREDENTIALS_PLACEHOLDER);
      when(forgeSecurityBridgeService.getAuthentication(any(AtlassianHostUser.class)))
          .thenReturn(userAuth);

      manualAuthorizationServiceImpl.authorize("cloud-2", "inst-2", Optional.of("ari:cloud:acct"));

      verify(forgeSecurityBridgeService).getAuthentication(any(AtlassianHostUser.class));
    }
  }
}
