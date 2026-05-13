package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Manually installs Forge-compatible {@link org.springframework.security.core.Authentication} into
 * {@link SecurityContextHolder} so product adapters can run <strong>outside</strong> a normal Forge
 * / Jira HTTP request thread.
 *
 * <p>The bridge {@linkplain com.github.vzakharchenko.runtime.bridge.common.JiraProductAdapter
 * Jira}, {@linkplain com.github.vzakharchenko.runtime.bridge.common.ConfluenceProductAdapter
 * Confluence}, and {@linkplain com.github.vzakharchenko.runtime.bridge.common.OtherProductAdapter
 * Other} adapters (see {@code com.github.vzakharchenko.runtime.bridge.forge.products}) choose
 * Connect vs Forge backends from the current {@code SecurityContext}. In schedulers, message
 * consumers, outbound webhooks, or any work that was <em>not</em> triggered from the Jira/Forge
 * host with an active invocation context, that context is missing: {@code
 * authenticatedAsCurrentUser()}, {@code impersonation(...)}, and related paths then have nothing to
 * bind tokens to unless you set it explicitly.
 *
 * <p><strong>Typical pattern:</strong> run the block that calls the adapters on a thread where you
 * either clear inherited security state or start with a blank context, call {@link #authorize} with
 * the {@link AtlassianHostUser} or {@link AtlassianHost} you already resolved (from your own store,
 * job payload, etc.), perform REST calls through the adapters, then clear the context when the
 * worker finishes (especially for pooled threads) so credentials do not leak across tasks.
 *
 * <p>If you use a <strong>fully custom</strong> authorization model for those paths, you must
 * either reimplement the same token wiring inside that layer or isolate work as above and delegate
 * token construction to {@link AtlassianForgeSecurityBridgeService} via this service instead of
 * expecting the adapters to infer Forge context on their own.
 */
@Service
public class ManualAuthorizationService {
  private final AtlassianForgeSecurityBridgeService forgeSecurityBridgeService;

  public ManualAuthorizationService(
      AtlassianForgeSecurityBridgeService forgeSecurityBridgeService) {
    this.forgeSecurityBridgeService = forgeSecurityBridgeService;
  }

  /**
   * Sets {@link SecurityContextHolder} to a {@link
   * com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication} for the given user on the
   * resolved host (user-scoped product REST).
   *
   * @param atlassianHostUser host + user identity your background job or integration should act as
   */
  public void authorize(AtlassianHostUser atlassianHostUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(forgeSecurityBridgeService.getAuthentication(atlassianHostUser));
  }

  /**
   * Sets {@link SecurityContextHolder} to installation-level Forge authentication (no user
   * principal).
   *
   * @param atlassianHost host / installation your code should call the product API as
   */
  public void authorize(AtlassianHost atlassianHost) {
    SecurityContextHolder.getContext()
        .setAuthentication(forgeSecurityBridgeService.getAuthentication(atlassianHost));
  }

  /**
   * Convenience overload: builds a minimal {@link AtlassianHost} from {@code cloudId} and {@code
   * installationId}, then delegates to {@link #authorize(AtlassianHost)} or {@link
   * #authorize(AtlassianHostUser)} when {@code accountId} is present and non-blank.
   *
   * <p>The host row is only populated enough for {@link AtlassianForgeSecurityBridgeService} to
   * resolve tokens from persistence; add further fields yourself if product calls need them.
   *
   * @param cloudId Atlassian Cloud id for the site (never {@code null})
   * @param installationId Forge / Connect installation id (never {@code null})
   * @param accountId optional Atlassian account id for user-scoped REST; blank values are ignored
   */
  public void authorize(String cloudId, String installationId, Optional<String> accountId) {
    Objects.requireNonNull(cloudId, "cloudId");
    Objects.requireNonNull(installationId, "installationId");
    Objects.requireNonNull(accountId, "accountId");

    AtlassianHost host = new AtlassianHost();
    host.setAddonInstalled(true);
    host.setCloudId(cloudId);
    host.setInstallationId(installationId);

    accountId
        .filter(id -> !id.isBlank())
        .map(id -> AtlassianHostUser.builder(host).withUserAccountId(id).build())
        .ifPresentOrElse(this::authorize, () -> authorize(host));
  }
}
