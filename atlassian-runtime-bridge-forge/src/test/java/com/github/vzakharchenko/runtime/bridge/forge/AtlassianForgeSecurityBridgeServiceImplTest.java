package com.github.vzakharchenko.runtime.bridge.forge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.ForgeApiContext;
import com.atlassian.connect.spring.ForgeApp;
import com.atlassian.connect.spring.ForgeInvocationToken;
import com.atlassian.connect.spring.ForgeSystemAccessToken;
import com.atlassian.connect.spring.ForgeSystemAccessTokenRepository;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.vzakharchenko.runtime.bridge.common.AtlassianHostContextEnricher;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AtlassianForgeSecurityBridgeServiceImplTest {

  private static final String INSTALLATION_ID = "inst-forge-1";
  private static final String CLOUD_ID = "cloud-99";
  private static final String CLIENT_KEY = "ck-forge";
  private static final String SITE_URL = "https://example.atlassian.net";
  private static final String API_BASE = "https://api.atlassian.com/ex/jira/site-x";
  private static final String IDENTITY_USER_ARI = "ari:cloud:identity::user/1";
  private static final String JIRA_SITE_CONTEXT_ARI = "ari:cloud:jira::site/site-x";

  /**
   * Avoids mocking Connect {@link ForgeSecurityContextRetriever} (not reliably mockable on all
   * JDKs).
   */
  private static final class StubForgeSecurityContextRetriever
      extends ForgeSecurityContextRetriever {

    private Optional<ForgeApiContext> forgeApiContext = Optional.empty();

    void setForgeApiContext(Optional<ForgeApiContext> ctx) {
      this.forgeApiContext = ctx;
    }

    @Override
    public Optional<ForgeApiContext> getForgeApiContext() {
      return forgeApiContext;
    }
  }

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

  private final StubForgeSecurityContextRetriever forgeSecurityContextRetriever =
      new StubForgeSecurityContextRetriever();

  @Mock private ForgeSystemAccessTokenRepository forgeSystemAccessTokenRepository;

  @Mock private AtlassianHostContextEnricher<ForgeApiContext> atlassianHostContextEnricher;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void resetForgeApiContextStub() {
    forgeSecurityContextRetriever.setForgeApiContext(Optional.empty());
  }

  private AtlassianForgeSecurityBridgeServiceImpl newService(
      Optional<List<AtlassianHostContextEnricher<ForgeApiContext>>> enrichers) {
    return newService(
        enrichers,
        (userId, contextId, authHeader) -> {
          throw new AssertionError("impersonation must not run for this test setup");
        });
  }

  private AtlassianForgeSecurityBridgeServiceImpl newService(
      Optional<List<AtlassianHostContextEnricher<ForgeApiContext>>> enrichers,
      ImpersonationUserService impersonationUserService) {
    AtlassianForgeSecurityBridgeServiceImpl impl =
        new AtlassianForgeSecurityBridgeServiceImpl(
            forgeSecurityContextRetriever,
            forgeSystemAccessTokenRepository,
            impersonationUserService,
            enrichers,
            Optional.of(objectMapper));
    ReflectionTestUtils.setField(impl, "appId", "test-forge-app-id");
    return impl;
  }

  private static ForgeApiContext forgeApiContext(
      ObjectMapper mapper,
      String installationId,
      String cloudId,
      String clientKey,
      String siteUrl,
      String principalOrNull) {
    ObjectNode contextJson = mapper.createObjectNode();
    contextJson.put("cloudId", cloudId);
    contextJson.put("clientKey", clientKey);
    contextJson.put("siteUrl", siteUrl);
    ForgeApp forgeApp = new ForgeApp();
    forgeApp.setInstallationId(installationId);
    ForgeInvocationToken forgeInvocationToken = new ForgeInvocationToken();
    forgeInvocationToken.setApp(forgeApp);
    forgeInvocationToken.setContext(contextJson);
    if (principalOrNull != null) {
      forgeInvocationToken.setPrincipal(principalOrNull);
    }
    return new ForgeApiContext(forgeInvocationToken, Optional.empty(), Optional.empty());
  }

  private static AtlassianHost sampleHost() {
    AtlassianHost host = new AtlassianHost();
    host.setInstallationId(INSTALLATION_ID);
    host.setCloudId(CLOUD_ID);
    host.setClientKey(CLIENT_KEY);
    host.setBaseUrl(SITE_URL);
    return host;
  }

  private static ForgeSystemAccessToken sampleStoredToken() {
    ForgeSystemAccessToken token = new ForgeSystemAccessToken();
    token.setApiBaseUrl(API_BASE);
    token.setAccessToken("offline-token-value");
    return token;
  }

  @Nested
  class GetAtlassianHostUserFromContext {

    private AtlassianForgeSecurityBridgeServiceImpl service;

    @BeforeEach
    void setUp() {
      service = newService(Optional.empty());
    }

    @Test
    void returnsEmptyWhenRetrieverHasNoForgeContext() {
      forgeSecurityContextRetriever.setForgeApiContext(Optional.empty());

      assertThat(service.getAtlassianHostUserFromContext())
          .as("without Forge API context there is no host user to build")
          .isEmpty();
    }

    @Test
    void mapsInvocationContextToHostUserWithAccountIdWhenPrincipalPresent() {
      ForgeApiContext ctx =
          forgeApiContext(
              objectMapper, INSTALLATION_ID, CLOUD_ID, CLIENT_KEY, SITE_URL, IDENTITY_USER_ARI);
      forgeSecurityContextRetriever.setForgeApiContext(Optional.of(ctx));

      assertThat(
              service
                  .getAtlassianHostUserFromContext()
                  .flatMap(AtlassianHostUser::getUserAccountId))
          .as("FIT principal becomes the Atlassian account id on the host user")
          .contains(IDENTITY_USER_ARI);
    }

    @Test
    void mapsInvocationContextToHostUserWithoutAccountWhenPrincipalMissing() {
      ForgeApiContext ctx =
          forgeApiContext(objectMapper, INSTALLATION_ID, CLOUD_ID, CLIENT_KEY, SITE_URL, null);
      forgeSecurityContextRetriever.setForgeApiContext(Optional.of(ctx));

      assertThat(
              service
                  .getAtlassianHostUserFromContext()
                  .flatMap(AtlassianHostUser::getUserAccountId))
          .as("no principal in the invocation token means no user account id on the host user")
          .isEmpty();
    }

    @Test
    void delegatesToHostContextEnricherWhenRegistered() {
      ForgeApiContext ctx =
          forgeApiContext(objectMapper, INSTALLATION_ID, CLOUD_ID, CLIENT_KEY, SITE_URL, null);
      forgeSecurityContextRetriever.setForgeApiContext(Optional.of(ctx));
      AtlassianHost enriched = new AtlassianHost();
      enriched.setInstallationId(INSTALLATION_ID);
      enriched.setCloudId(CLOUD_ID);
      enriched.setClientKey("enriched-client-key");
      when(atlassianHostContextEnricher.update(any(), any())).thenReturn(Optional.of(enriched));

      AtlassianForgeSecurityBridgeServiceImpl withEnricher =
          newService(Optional.of(List.of(atlassianHostContextEnricher)));

      assertThat(
              withEnricher.getAtlassianHostUserFromContext().map(u -> u.getHost().getClientKey()))
          .as("enricher output must replace the minimally built host")
          .contains("enriched-client-key");
    }
  }

  @Nested
  class GetAuthenticationForHostUser {

    private final RecordingImpersonation recordingImpersonation = new RecordingImpersonation();

    private AtlassianForgeSecurityBridgeServiceImpl service;

    @BeforeEach
    void setUp() {
      service = newService(Optional.empty(), recordingImpersonation);
    }

    private void givenValidOfflineToken() {
      when(forgeSystemAccessTokenRepository.findByInstallationIdAndExpirationTimeAfter(
              eq(INSTALLATION_ID), any(Timestamp.class)))
          .thenReturn(Optional.of(sampleStoredToken()));
    }

    @Test
    void throwsWhenUserAccountIdMissing() {
      AtlassianHostUser withoutAccount = AtlassianHostUser.builder(sampleHost()).build();

      assertThatThrownBy(() -> service.getAuthentication(withoutAccount))
          .as("user-scoped authentication requires an account id on the host user")
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void passesExpectedArgumentsToImpersonation() {
      givenValidOfflineToken();
      AtlassianHostUser hostUser =
          AtlassianHostUser.builder(sampleHost()).withUserAccountId(IDENTITY_USER_ARI).build();

      service.getAuthentication(hostUser);

      assertThat(
              List.of(
                  recordingImpersonation.lastUserId,
                  recordingImpersonation.lastContextId,
                  recordingImpersonation.lastAuthHeader))
          .as("GraphQL impersonation uses account id, Jira site context ARI, and offline token")
          .containsExactly(IDENTITY_USER_ARI, JIRA_SITE_CONTEXT_ARI, "offline-token-value");
    }

    @Test
    void returnsForgeAuthenticationWithSameHostUserPrincipal() {
      givenValidOfflineToken();
      AtlassianHostUser hostUser =
          AtlassianHostUser.builder(sampleHost()).withUserAccountId(IDENTITY_USER_ARI).build();

      assertThat(service.getAuthentication(hostUser).getPrincipal())
          .as("Connect adapters expect the original AtlassianHostUser as principal")
          .isSameAs(hostUser);
    }

    @Test
    void throwsWhenOfflineAccessTokenMissing() {
      ForgeSystemAccessToken missingOffline = new ForgeSystemAccessToken();
      missingOffline.setApiBaseUrl(API_BASE);
      missingOffline.setAccessToken(null);
      when(forgeSystemAccessTokenRepository.findByInstallationIdAndExpirationTimeAfter(
              eq(INSTALLATION_ID), any(Timestamp.class)))
          .thenReturn(Optional.of(missingOffline));
      AtlassianHostUser hostUser =
          AtlassianHostUser.builder(sampleHost()).withUserAccountId(IDENTITY_USER_ARI).build();

      assertThatThrownBy(() -> service.getAuthentication(hostUser))
          .as("stored system token without offline secret cannot authorize")
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void throwsWhenNoStoredSystemToken() {
      when(forgeSystemAccessTokenRepository.findByInstallationIdAndExpirationTimeAfter(
              eq(INSTALLATION_ID), any(Timestamp.class)))
          .thenReturn(Optional.empty());
      AtlassianHostUser hostUser =
          AtlassianHostUser.builder(sampleHost()).withUserAccountId(IDENTITY_USER_ARI).build();

      assertThatThrownBy(() -> service.getAuthentication(hostUser))
          .as("missing persisted Forge system token must deny access")
          .isInstanceOf(AccessDeniedException.class);
    }
  }

  @Nested
  class GetAuthenticationForHost {

    private AtlassianForgeSecurityBridgeServiceImpl service;

    @BeforeEach
    void setUp() {
      service =
          newService(
              Optional.empty(),
              (userId, contextId, authHeader) -> {
                throw new AssertionError("installation-only path must not call impersonation");
              });
      when(forgeSystemAccessTokenRepository.findByInstallationIdAndExpirationTimeAfter(
              eq(INSTALLATION_ID), any(Timestamp.class)))
          .thenReturn(Optional.of(sampleStoredToken()));
    }

    @Test
    void returnsForgeAuthenticationWithInstallationLevelUser() {
      assertThat(service.getAuthentication(sampleHost()))
          .as("installation-level auth still uses ForgeAuthentication type")
          .isInstanceOf(ForgeAuthentication.class);
    }
  }
}
