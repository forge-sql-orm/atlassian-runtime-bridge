package com.github.vzakharchenko.runtime.bridge.containers;

import com.atlassian.connect.spring.*;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Loads {@link AppContext} from {@link EgressClientService#getInvocationContext(String)} and
 * maps it to {@link ForgeAuthentication} with {@link AtlassianHostUser} when {@code cloudId} is
 * present.
 */
@Service
public class ForgeContextServiceImpl implements ForgeContextService {
  private static final Logger log = LoggerFactory.getLogger(ForgeContextServiceImpl.class);
  private final EgressClientService egressClientService;
  private final ObjectMapper objectMapper;

  public ForgeContextServiceImpl(
      EgressClientService egressClientService, Optional<ObjectMapper> objectMapper) {
    this.egressClientService = egressClientService;
    this.objectMapper = objectMapper.orElseGet(ObjectMapper::new);
  }

  @Override
  public Optional<ForgeAuthentication> emulateForgeAuthentication(final String invocationId) {
    var responseBody = egressClientService.getInvocationContext(invocationId).getBody();
    var appContext = parseJsonNode(objectMapper, responseBody, AppContext.class).orElse(null);
    var accountId = accountId(appContext);

    var token = new ForgeInvocationToken();
    token.setApp(buildForgeApp(appContext));
    token.setContext(responseBody);
    if (accountId != null) {
      token.setPrincipal(accountId);
    }

    var apiContext = new ForgeApiContext(token, Optional.empty(), Optional.empty());
    if (appContext == null) {
      return Optional.of(new ForgeAuthentication(apiContext));
    }

    Optional<AtlassianHost> atlassianHost = buildAtlassianHost(appContext);
    if (atlassianHost.isEmpty()) {
      return Optional.empty();
    }
    var hostUserBuilder = AtlassianHostUser.builder(atlassianHost.get());
    if (accountId != null) {
      hostUserBuilder.withUserAccountId(accountId);
    }
    return Optional.of(new ForgeAuthentication(apiContext, hostUserBuilder.build()));
  }

  @Nullable
  private static String accountId(@Nullable final AppContext context) {
    return Optional.ofNullable(context)
        .map(AppContext::getContext)
        .map(AppContext.Context::getAccountId)
        .orElse(null);
  }

  /**
   * Extract the trailing identifier from a Forge ARI such as {@code
   * ari:cloud:ecosystem::installation/8d5f651a-...} → {@code 8d5f651a-...}.
   */
  private static String idFromAri(final String ari) {
    var idx = ari.lastIndexOf('/');
    return idx >= 0 ? ari.substring(idx + 1) : ari;
  }

  private ForgeApp buildForgeApp(@Nullable final AppContext context) {
    var app = new ForgeApp();
    if (context == null || context.getApp() == null) {
      return app;
    }
    var appData = context.getApp();
    app.setId(appData.getId());
    app.setInstallationId(appData.getInstallationId());
    Optional.ofNullable(appData.getEnvironment())
        .map(env -> objectMapper.convertValue(env, ForgeEnvironment.class))
        .ifPresent(app::setEnvironment);
    Optional.ofNullable(appData.getModule())
        .map(mod -> objectMapper.convertValue(mod, ForgeAppModule.class))
        .ifPresent(app::setModule);
    return app;
  }

  private Optional<AtlassianHost> buildAtlassianHost(final AppContext context) {
    if (context == null) {
      log.warn("Context is null");
      return Optional.empty();
    }
    var installationId =
        Optional.ofNullable(context.getApp())
            .map(AppContext.App::getInstallationId)
            .map(ForgeContextServiceImpl::idFromAri)
            .orElse(null);

    var ctx = Optional.ofNullable(context.getContext());
    var cloudId = ctx.map(AppContext.Context::getCloudId).orElse(null);
    var siteUrl = ctx.map(AppContext.Context::getSiteUrl).orElse(null);
    if (cloudId == null) {
      log.warn("CloudId is null");
      return Optional.empty();
    }
    return atlassianHost(installationId, siteUrl, cloudId);
  }

  private Optional<AtlassianHost> atlassianHost(
      final String installationId, final String baseUrl, final String cloudId) {
    var host = new AtlassianHost();
    host.setAddonInstalled(true);
    host.setInstallationId(installationId);
    host.setBaseUrl(baseUrl);
    host.setCloudId(cloudId);
    host.setClientKey(cloudId);
    return Optional.of(host);
  }

  /**
   * Deserializes a {@link JsonNode} subtree; returns empty on parse failure (logged).
   */
  public static <T> Optional<T> parseJsonNode(
      ObjectMapper objectMapper, JsonNode node, Class<T> tClass) {
    try {
      return Optional.of(objectMapper.treeToValue(node, tClass));
    } catch (JsonProcessingException e) {
      log.error("Parsing error", e);
      return Optional.empty();
    }
  }
}
