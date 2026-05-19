package com.github.vzakharchenko.runtime.bridge.containers.products;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.github.vzakharchenko.runtime.bridge.common.JiraProductAdapter;
import com.github.vzakharchenko.runtime.bridge.containers.EgressClientService;
import com.github.vzakharchenko.runtime.bridge.containers.EgressClientServiceImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Container {@link JiraProductAdapter}: Jira REST calls go through the egress sidecar ({@code
 * /jira/…}) using {@code forge-proxy-authorization} derived from the current {@link
 * ForgeAuthentication}.
 *
 * <p>Requires {@link ForgeAuthentication} in {@link SecurityContextHolder} (typically set by {@link
 * com.github.vzakharchenko.runtime.bridge.containers.filters.ContainerAuthorizationFilter} or
 * {@link com.github.vzakharchenko.runtime.bridge.containers.ManualAuthorizationServiceImpl}).
 */
@Component
public class JiraProductAdapterImpl implements JiraProductAdapter {
  private final EgressClientService egressClientService;

  public JiraProductAdapterImpl(EgressClientService egressClientService) {
    this.egressClientService = egressClientService;
  }

  @Override
  public RestTemplate authenticatedAsAddon(AtlassianHost host) {
    if (SecurityContextHolder.getContext().getAuthentication()
        instanceof ForgeAuthentication authentication) {
      AtlassianHostUser principal = (AtlassianHostUser) authentication.getPrincipal();
      return egressClientService.jiraTemplateRequest(
          EgressClientServiceImpl.InstallationAuth.asApp(principal.getHost().getInstallationId()));
    } else {
      throw new IllegalStateException("Not ForgeAuthentication. Container mode");
    }
  }

  @Override
  public RestTemplate authenticatedAsCurrentUser() {
    if (SecurityContextHolder.getContext().getAuthentication()
        instanceof ForgeAuthentication authentication) {
      AtlassianHostUser principal = (AtlassianHostUser) authentication.getPrincipal();
      return egressClientService.jiraTemplateRequest(
          EgressClientServiceImpl.InstallationAuth.asUser(
              principal.getHost().getInstallationId(), principal.getUserAccountId().orElseThrow()));
    } else {
      throw new IllegalStateException("Not ForgeAuthentication. Container mode");
    }
  }

  @Override
  public RestTemplate impersonation(AtlassianHostUser hostUser) {
    if (SecurityContextHolder.getContext().getAuthentication()
        instanceof ForgeAuthentication authentication) {
      AtlassianHostUser principal = (AtlassianHostUser) authentication.getPrincipal();
      return egressClientService.jiraTemplateRequest(
          EgressClientServiceImpl.InstallationAuth.asUser(
              principal.getHost().getInstallationId(), hostUser.getUserAccountId().orElseThrow()));
    } else {
      throw new IllegalStateException("Not ForgeAuthentication. Container mode");
    }
  }
}
