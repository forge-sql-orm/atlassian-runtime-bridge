package com.github.vzakharchenko.runtime.bridge.containers.filters;

import static com.github.vzakharchenko.runtime.bridge.containers.ForgeIngressHeaders.INVOCATION_ID;
import static org.springframework.util.StringUtils.isEmpty;

import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.github.vzakharchenko.runtime.bridge.containers.ForgeContextService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ingress filter for Forge Container requests: when {@link
 * ForgeIngressHeaders#INVOCATION_ID} is present, loads {@link ForgeAuthentication} via {@link
 * ForgeContextService} and installs it for the remainder of the filter chain.
 *
 * <p>The security context is always cleared in a {@code finally} block so thread-local state does
 * not leak across requests.
 */
@Component
public class ContainerAuthorizationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(ContainerAuthorizationFilter.class);

  private final ForgeContextService forgeContextService;

  public ContainerAuthorizationFilter(ForgeContextService forgeContextService) {
    this.forgeContextService = forgeContextService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) {
    try {
      String invocationId = request.getHeader(INVOCATION_ID);
      if (isEmpty(invocationId)) {
        filterChain.doFilter(request, response);
        return;
      }
      Optional<ForgeAuthentication> forgeAuthentication =
          forgeContextService.emulateForgeAuthentication(invocationId);
      if (forgeAuthentication.isEmpty()) {
        filterChain.doFilter(request, response);
        return;
      }
      SecurityContextHolder.getContext().setAuthentication(forgeAuthentication.get());
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      log.error("Failed to set up Forge authentication", e);
      throw new AccessDeniedException("Failed to set up Forge authentication", e);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
