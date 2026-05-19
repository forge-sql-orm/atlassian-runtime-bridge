package com.github.vzakharchenko.runtime.bridge.containers;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.ForgeApiContext;
import com.atlassian.connect.spring.ForgeApp;
import com.atlassian.connect.spring.ForgeInvocationToken;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.github.vzakharchenko.runtime.bridge.common.ManualAuthorizationService;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Container implementation of {@link ManualAuthorizationService}: seeds {@link
 * SecurityContextHolder} with {@link ForgeAuthentication} so shared controllers (e.g. {@code
 * /api/impersonation}) can call {@link
 * com.github.vzakharchenko.runtime.bridge.common.JiraProductAdapter} without Connect iframe JWT.
 *
 * <p>When a {@link ForgeAuthentication} is already present, {@code cloudId} must match the context
 * host (cross-tenant calls are rejected). {@link AtlassianHostUser#withUserAccountId} is applied
 * only when {@code accountId} is non-empty.
 */
@Service
public class ManualAuthorizationServiceImpl implements ManualAuthorizationService {

  @Override
  public void authorize(AtlassianHostUser atlassianHostUser) {
    authorize(
        atlassianHostUser.getHost().getCloudId(),
        atlassianHostUser.getHost().getInstallationId(),
        atlassianHostUser.getUserAccountId());
  }

  @Override
  public void authorize(AtlassianHost atlassianHost) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof ForgeAuthentication forgeAuthentication) {
      Object principal = forgeAuthentication.getPrincipal();
      if (principal instanceof AtlassianHostUser hostUser) {
        authorize(hostUser);
        return;
      }
      if (principal instanceof AtlassianHost host) {
        assertSameCloud(host.getCloudId(), atlassianHost.getCloudId());
      }
    }
    authorize(atlassianHost.getCloudId(), atlassianHost.getInstallationId(), Optional.empty());
  }

  @Override
  public void authorize(String cloudId, String installationId, Optional<String> accountId) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof ForgeAuthentication forgeAuthentication) {
      AtlassianHost host = resolveHost(forgeAuthentication.getPrincipal(), cloudId, installationId);
      assertSameCloud(host.getCloudId(), cloudId);
      applyForgeAuthentication(forgeAuthentication.getDetails(), host, accountId);
      return;
    }
    applyForgeAuthenticationWithoutContext(cloudId, installationId, accountId);
  }

  private static AtlassianHost resolveHost(
      Object principal, String cloudId, String installationId) {
    if (principal instanceof AtlassianHostUser hostUser) {
      return hostUser.getHost();
    }
    if (principal instanceof AtlassianHost host) {
      return host;
    }
    return createHost(cloudId, installationId);
  }

  private static void assertSameCloud(String contextCloudId, String requestedCloudId) {
    if (!Objects.equals(contextCloudId, requestedCloudId)) {
      throw crossTenantException(contextCloudId, requestedCloudId);
    }
  }

  private static IllegalStateException crossTenantException(
      String contextCloudId, String requestedCloudId) {
    return new IllegalStateException(
        "Cross tenant authorization is not allowed: expected cloudId="
            + contextCloudId
            + ", received cloudId="
            + requestedCloudId);
  }

  private static AtlassianHost createHost(String cloudId, String installationId) {
    AtlassianHost host = new AtlassianHost();
    host.setCloudId(cloudId);
    host.setClientKey(cloudId);
    host.setInstallationId(installationId);
    return host;
  }

  private static AtlassianHostUser buildHostUser(AtlassianHost host, Optional<String> accountId) {
    var builder = AtlassianHostUser.builder(host);
    accountId.ifPresent(builder::withUserAccountId);
    return builder.build();
  }

  private static void applyForgeAuthentication(
      ForgeApiContext context, AtlassianHost host, Optional<String> accountId) {
    SecurityContextHolder.getContext()
        .setAuthentication(new ForgeAuthentication(context, buildHostUser(host, accountId)));
  }

  private static void applyForgeAuthenticationWithoutContext(
      String cloudId, String installationId, Optional<String> accountId) {
    ForgeInvocationToken token = new ForgeInvocationToken();
    ForgeApp app = new ForgeApp();
    app.setInstallationId(installationId);
    token.setApp(app);
    applyForgeAuthentication(
        new ForgeApiContext(token, Optional.empty(), Optional.empty()),
        createHost(cloudId, installationId),
        accountId);
  }
}
