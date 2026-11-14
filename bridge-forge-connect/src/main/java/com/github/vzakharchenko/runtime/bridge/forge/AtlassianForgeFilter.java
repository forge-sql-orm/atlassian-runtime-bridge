package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.github.vzakharchenko.runtime.bridge.common.SystemTokenPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter registered after Forge invocation authentication. When the current {@link
 * org.springframework.security.core.context.SecurityContext} holds a {@link ForgeAuthentication}
 * whose principal is not yet a full {@link com.atlassian.connect.spring.AtlassianHostUser}, this
 * filter replaces it with a new {@link ForgeAuthentication} built from {@link
 * AtlassianForgeSecurityBridgeService#getAtlassianHostUserFromContext()}.
 *
 * <p>Early Forge authentication from Connect Spring carries only invocation metadata; host/user
 * enrichment runs here so controllers and {@code JiraProductAdapter} see the same model as in
 * classic Connect requests.
 *
 * <p>When a {@link SystemTokenPolicy} is available, a system (offline) token in the request is
 * validated here for every endpoint; an invalid one is answered with HTTP 403 and the security
 * context is cleared. Whether a request <em>must</em> carry a system token is an authorization
 * decision left to the application ({@code @SystemCallOnly}).
 */
@Component
public class AtlassianForgeFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AtlassianForgeFilter.class);
  private final AtlassianForgeSecurityBridgeService forgeSecurityBridgeService;
  private final SystemTokenPolicy systemTokenPolicy;

  /** Without a policy the system token is not validated. */
  public AtlassianForgeFilter(AtlassianForgeSecurityBridgeService forgeSecurityBridgeService) {
    this(forgeSecurityBridgeService, null);
  }

  @Autowired
  public AtlassianForgeFilter(
      AtlassianForgeSecurityBridgeService forgeSecurityBridgeService,
      SystemTokenPolicy systemTokenPolicy) {
    this.forgeSecurityBridgeService = forgeSecurityBridgeService;
    this.systemTokenPolicy = systemTokenPolicy;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof ForgeAuthentication forgeAuthentication) {
      if (hasInvalidSystemToken(forgeAuthentication)) {
        SecurityContextHolder.clearContext();
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Forge system token");
        return;
      }
      try {
        forgeSecurityBridgeService
            .getAtlassianHostUserFromContext()
            .ifPresentOrElse(
                user -> {
                  var ctx = forgeAuthentication.getDetails();
                  var auth = new ForgeAuthentication(ctx, user);
                  SecurityContextHolder.getContext().setAuthentication(auth);
                },
                () ->
                    LOGGER.error(
                        "No AtlassianHostUser found for the provided Forge invocation token"));

      } catch (Exception e) {
        LOGGER.error("Failed to set up Forge authentication", e);
      }
    }
    filterChain.doFilter(request, response);
  }

  /** A system (offline) token present in the request is always validated, on every endpoint. */
  private boolean hasInvalidSystemToken(ForgeAuthentication authentication) {
    return systemTokenPolicy != null
        && systemTokenPolicy.isSystemTokenPresent(authentication)
        && !systemTokenPolicy.hasSystemToken(authentication);
  }
}
