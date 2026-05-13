package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.*;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.atlassian.connect.spring.internal.auth.frc.ForgeRemoteEnforcer;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vzakharchenko.runtime.bridge.common.AtlassianHostContextEnricher;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link AtlassianForgeMigrationService}.
 * <p>
 * It reconstructs {@link com.atlassian.connect.spring.AtlassianHost} and
 * {@link com.atlassian.connect.spring.AtlassianHostUser} instances from Forge
 * invocation tokens (online and offline), optionally enriching the host
 * via {@link com.github.vzakharchenko.runtime.bridge.common.AtlassianHostContextEnricher},
 * and exposes helpers to obtain Spring Security {@link Authentication} objects.
 */
@Component
public class AtlassianForgeMigrationServiceImpl implements AtlassianForgeMigrationService {
    private static final Logger log = LoggerFactory.getLogger(ForgeRemoteEnforcer.class);

    private final ForgeSecurityContextRetriever forgeSecurityContextRetriever;
    private final ForgeSystemAccessTokenRepository forgeSystemAccessTokenRepository;
    private final ImpersonationUserServiceImpl impersonationUserService;
    private final Optional<AtlassianHostContextEnricher<ForgeApiContext>> atlassianHostContextEnricher;
    private final ObjectMapper objectMapper;

    @Value("${app.id}")
    private String appId;

    public AtlassianForgeMigrationServiceImpl(ForgeSecurityContextRetriever forgeSecurityContextRetriever,
                                              ForgeSystemAccessTokenRepository forgeSystemAccessTokenRepository,
                                              ImpersonationUserServiceImpl impersonationUserService,
                                              Optional<AtlassianHostContextEnricher<ForgeApiContext>> atlassianHostContextEnricher,
                                              ObjectMapper objectMapper) {
        this.forgeSecurityContextRetriever = forgeSecurityContextRetriever;
        this.forgeSystemAccessTokenRepository = forgeSystemAccessTokenRepository;
        this.impersonationUserService = impersonationUserService;
        this.objectMapper = objectMapper;
        this.atlassianHostContextEnricher = atlassianHostContextEnricher;
    }

    private Optional<AtlassianHost> fromApiContext(Optional<ForgeApiContext> forgeApiContext) {
        if (forgeApiContext.isEmpty()) {
            return Optional.empty();
        }
        var apiCtx = forgeApiContext.get();
        var invocationToken = apiCtx.getForgeInvocationToken();
        var ctx = objectMapper.convertValue(Objects.requireNonNull(invocationToken.getContext(), "Forge Invocation API Context cannot be null"), InvocationContext.class);
        var forgeApp = invocationToken.getApp();

        Optional<AtlassianHost> atlassianHost = atlassianHost(
                forgeApp.getInstallationId(),
                ctx.cloudId(),
                Objects.requireNonNullElse(ctx.clientKey(), ctx.cloudId()),
                ctx.supportEntitlementNumber()
        );
        if (atlassianHostContextEnricher.isPresent()) {
            return atlassianHostContextEnricher.get().update(atlassianHost, forgeApiContext);
        }
        return atlassianHost;
    }

    private Optional<AtlassianHost> atlassianHost(String installationId,
                                                  String cloudId,
                                                  String clientKey,
                                                  String entitlementNumber) {
        var host = new AtlassianHost();
        host.setAddonInstalled(true);
        host.setInstallationId(installationId);
        host.setCloudId(cloudId);
        host.setClientKey(clientKey);
        host.setServiceEntitlementNumber(entitlementNumber);
        return Optional.of(host);
    }

    private Optional<AtlassianHost> fromOfflineToken(Optional<ForgeSystemAccessToken> accessToken, String siteUrl, String cloudId, String clientKey, String entitlementNumber) {
        if (accessToken.isEmpty()) {
            return Optional.empty();
        }
        var forgeSystemAccessToken = accessToken.get();
        var maybeCtx = forgeSecurityContextRetriever.getForgeApiContext();
        if (maybeCtx.isPresent() && isFromContext(forgeSystemAccessToken, maybeCtx.get())) {
            return fromApiContext(maybeCtx);
        }
        log.info("Forge Host from offline token: {}", forgeSystemAccessToken);
        return atlassianHost(
                forgeSystemAccessToken.getInstallationId(),
                cloudId,
                clientKey,
                entitlementNumber
        );
    }

    private boolean isFromContext(ForgeSystemAccessToken token, ForgeApiContext ctx) {
        var invocationToken = ctx.getForgeInvocationToken();
        var app = invocationToken.getApp();
        return Objects.equals(app.getInstallationId(), token.getInstallationId());
    }

    private Optional<AtlassianHostUser> createAtlassianHostUserFromContext(Optional<ForgeApiContext> apiContext) {
        return fromApiContext(apiContext).map(host -> {
            var maybeAccountId = apiContext
                    .map(ctx -> ctx.getForgeInvocationToken().getPrincipal())
                    .filter(StringUtils::isNotEmpty);
            AtlassianHostUser.AtlassianHostUserBuilder hostUserBuilder = AtlassianHostUser.builder(host);
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
        var userId = hostUser.getUserAccountId().orElseThrow(() -> new AccessDeniedException("Access denied"));
        return new ForgeAuthentication(createApiContext(hostUser.getHost(), Optional.of(userId)), hostUser);
    }

    @Override
    public Authentication getAuthentication(AtlassianHost host) {
        return new ForgeAuthentication(createApiContext(host, Optional.empty()), AtlassianHostUser.builder(host).build());
    }

    private ForgeApiContext createApiContext(AtlassianHost host, Optional<String> userId) {
        String installationId = host.getInstallationId();
        var forgeSystemAccessToken = retrieveSystemAccessToken(installationId);
        String apiBaseUrl = forgeSystemAccessToken.getApiBaseUrl();
        String offlineToken = forgeSystemAccessToken.getAccessToken();

        if (offlineToken == null) {
            throw new AccessDeniedException(installationId + " .Offline token does not exists or expired");
        }

        var forgeInvocationToken = buildForgeInvocationToken(installationId, host, apiBaseUrl, userId);
        var userToken = userId.map(
                id -> impersonationUserService.impersonateUser(id, getContextId(apiBaseUrl), offlineToken));

        return new ForgeApiContext(forgeInvocationToken, userToken, Optional.of(offlineToken));
    }

    private ForgeSystemAccessToken retrieveSystemAccessToken(String installationId) {
        var now = Timestamp.from(Instant.now());
        return forgeSystemAccessTokenRepository
                .findByInstallationIdAndExpirationTimeAfter(installationId, now)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
    }

    private ForgeInvocationToken buildForgeInvocationToken(
            String installationId,
            AtlassianHost host,
            String apiBaseUrl,
            Optional<String> userId
    ) {
        var forgeInvocationToken = new ForgeInvocationToken();
        forgeInvocationToken.setApp(buildForgeApp(installationId, apiBaseUrl));
        userId.ifPresent(forgeInvocationToken::setPrincipal);
        forgeInvocationToken.setContext(objectMapper.convertValue(
                new InvocationContext(host.getCloudId(), host.getClientKey(), host.getBaseUrl(), host.getEntitlementNumber()),
                JsonNode.class
        ));

        return forgeInvocationToken;
    }

    private ForgeApp buildForgeApp(String installationId, String apiBaseUrl) {
        var app = new ForgeApp();
        app.setInstallationId(installationId);
        app.setApiBaseUrl(apiBaseUrl);
        app.setId(appId);
        return app;
    }

    @Override
    public boolean isMigratedToForge(AtlassianHost atlassianHost) {
        if (StringUtils.isEmpty(atlassianHost.getInstallationId())) {
            log.warn("Customer {} doesnt migrate to atlassian forge", atlassianHost.getClientKey());
            return false;
        }
        return true;
    }

    private record InvocationContext(String cloudId,
                                     String clientKey,
                                     String siteUrl,
                                     String supportEntitlementNumber) {
    }

    public String getContextId(String getApiBaseUrl) {
        String[] parts = getApiBaseUrl.split("/");
        return "ari:cloud:jira::site/" + parts[parts.length - 1];
    }
}
