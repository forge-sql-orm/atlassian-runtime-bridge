package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 */
@Component
public class AtlassianForgeFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AtlassianForgeFilter.class);
  private final AtlassianForgeSecurityBridgeService forgeSecurityBridgeService;

  public AtlassianForgeFilter(AtlassianForgeSecurityBridgeService forgeSecurityBridgeService) {
    this.forgeSecurityBridgeService = forgeSecurityBridgeService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof ForgeAuthentication forgeAuthentication) {
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
}
