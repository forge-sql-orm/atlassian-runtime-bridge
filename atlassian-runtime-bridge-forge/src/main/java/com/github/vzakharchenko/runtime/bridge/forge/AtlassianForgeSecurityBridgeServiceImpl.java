package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.*;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vzakharchenko.runtime.bridge.common.AtlassianHostContextEnricher;
import io.micrometer.common.util.StringUtils;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Default {@link AtlassianForgeSecurityBridgeService} implementation.
 *
 * <p>Resolves {@link com.atlassian.connect.spring.AtlassianHost} from live {@link ForgeApiContext}
 * (invocation token JSON) or from persisted {@link ForgeSystemAccessToken} rows, optionally passes
 * the host through {@link
 * com.github.vzakharchenko.runtime.bridge.common.AtlassianHostContextEnricher}, and builds {@link
 * com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication} backed by stored offline
 * tokens and optional user impersonation via {@link ImpersonationUserService}.
 *
 * <p>{@code app.id} ({@link Value}) must match the Forge manifest {@code app.id} so {@link
 * ForgeApp} metadata lines up with the deployed app.
 */
@Component
public class AtlassianForgeSecurityBridgeServiceImpl
    implements AtlassianForgeSecurityBridgeService {

  private final ForgeSecurityContextRetriever forgeSecurityContextRetriever;
  private final ForgeSystemAccessTokenRepository forgeSystemAccessTokenRepository;
  private final ImpersonationUserService impersonationUserService;
  private final List<AtlassianHostContextEnricher<ForgeApiContext>> atlassianHostContextEnrichers;
  private final ObjectMapper objectMapper;

  @Value("${app.id}")
  private String appId;

  public AtlassianForgeSecurityBridgeServiceImpl(
      ForgeSecurityContextRetriever forgeSecurityContextRetriever,
      ForgeSystemAccessTokenRepository forgeSystemAccessTokenRepository,
      ImpersonationUserService impersonationUserService,
      Optional<List<AtlassianHostContextEnricher<ForgeApiContext>>> atlassianHostContextEnrichers,
      Optional<ObjectMapper> objectMapper) {
    this.forgeSecurityContextRetriever = forgeSecurityContextRetriever;
    this.forgeSystemAccessTokenRepository = forgeSystemAccessTokenRepository;
    this.impersonationUserService = impersonationUserService;
    this.objectMapper = objectMapper.orElseGet(ObjectMapper::new);
    this.atlassianHostContextEnrichers =
        atlassianHostContextEnrichers
            .map(
                enrichers -> {
                  var sorted = new ArrayList<>(enrichers);
                  sorted.sort(Comparator.comparingInt(AtlassianHostContextEnricher::order));
                  return List.copyOf(sorted);
                })
            .orElseGet(Collections::emptyList);
  }

  private Optional<AtlassianHost> fromApiContext(Optional<ForgeApiContext> forgeApiContext) {
    if (forgeApiContext.isEmpty()) {
      return Optional.empty();
    }
    var apiCtx = forgeApiContext.get();
    var invocationToken = apiCtx.getForgeInvocationToken();
    var ctx =
        objectMapper.convertValue(
            Objects.requireNonNull(
                invocationToken.getContext(), "Forge Invocation API Context cannot be null"),
            InvocationContext.class);
    var forgeApp = invocationToken.getApp();

    Optional<AtlassianHost> host =
        atlassianHost(
            forgeApp.getInstallationId(),
            ctx.cloudId(),
            Objects.requireNonNullElse(ctx.clientKey(), ctx.cloudId()));
    for (AtlassianHostContextEnricher<ForgeApiContext> enricher : atlassianHostContextEnrichers) {
      host = enricher.update(host, forgeApiContext);
    }
    return host;
  }

  private Optional<AtlassianHost> atlassianHost(
      String installationId, String cloudId, String clientKey) {
    var host = new AtlassianHost();
    host.setAddonInstalled(true);
    host.setInstallationId(installationId);
    host.setCloudId(cloudId);
    host.setClientKey(clientKey);
    return Optional.of(host);
  }

  private Optional<AtlassianHostUser> createAtlassianHostUserFromContext(
      Optional<ForgeApiContext> apiContext) {
    return fromApiContext(apiContext)
        .map(
            host -> {
              var maybeAccountId =
                  apiContext
                      .map(ctx -> ctx.getForgeInvocationToken().getPrincipal())
                      .filter(StringUtils::isNotEmpty);
              AtlassianHostUser.AtlassianHostUserBuilder hostUserBuilder =
                  AtlassianHostUser.builder(host);
              return maybeAccountId
                  .map(accountId -> hostUserBuilder.withUserAccountId(accountId).build())
                  .orElseGet(hostUserBuilder::build);
            });
  }

  @Override
  public Optional<AtlassianHostUser> getAtlassianHostUserFromContext() {
    Optional<ForgeApiContext> context = forgeSecurityContextRetriever.getForgeApiContext();
    return createAtlassianHostUserFromContext(context);
  }

  @Override
  public Authentication getAuthentication(AtlassianHostUser hostUser) {
    var userId =
        hostUser.getUserAccountId().orElseThrow(() -> new AccessDeniedException("Access denied"));
    return new ForgeAuthentication(
        createApiContext(hostUser.getHost(), Optional.of(userId)), hostUser);
  }

  @Override
  public Authentication getAuthentication(AtlassianHost host) {
    return new ForgeAuthentication(
        createApiContext(host, Optional.empty()), AtlassianHostUser.builder(host).build());
  }

  private ForgeApiContext createApiContext(AtlassianHost host, Optional<String> userId) {
    String installationId = host.getInstallationId();
    var forgeSystemAccessToken = retrieveSystemAccessToken(installationId);
    String apiBaseUrl = forgeSystemAccessToken.getApiBaseUrl();
    String offlineToken = forgeSystemAccessToken.getAccessToken();

    if (offlineToken == null) {
      throw new AccessDeniedException(
          installationId + " .Offline token does not exists or expired");
    }

    var forgeInvocationToken = buildForgeInvocationToken(installationId, host, apiBaseUrl, userId);
    var userToken =
        userId.map(
            id ->
                impersonationUserService.impersonateUser(
                    id, AtlassianForgeUtils.getContextId(apiBaseUrl), offlineToken));

    return new ForgeApiContext(forgeInvocationToken, userToken, Optional.of(offlineToken));
  }

  private ForgeSystemAccessToken retrieveSystemAccessToken(String installationId) {
    var now = Timestamp.from(Instant.now());
    return forgeSystemAccessTokenRepository
        .findByInstallationIdAndExpirationTimeAfter(installationId, now)
        .orElseThrow(() -> new AccessDeniedException("Access denied"));
  }

  private ForgeInvocationToken buildForgeInvocationToken(
      String installationId, AtlassianHost host, String apiBaseUrl, Optional<String> userId) {
    var forgeInvocationToken = new ForgeInvocationToken();
    forgeInvocationToken.setApp(buildForgeApp(installationId, apiBaseUrl));
    userId.ifPresent(forgeInvocationToken::setPrincipal);
    forgeInvocationToken.setContext(
        objectMapper.convertValue(
            new InvocationContext(host.getCloudId(), host.getClientKey(), host.getBaseUrl()),
            JsonNode.class));

    return forgeInvocationToken;
  }

  private ForgeApp buildForgeApp(String installationId, String apiBaseUrl) {
    var app = new ForgeApp();
    app.setInstallationId(installationId);
    app.setApiBaseUrl(apiBaseUrl);
    app.setId(appId);
    return app;
  }

  private record InvocationContext(String cloudId, String clientKey, String siteUrl) {}
}
