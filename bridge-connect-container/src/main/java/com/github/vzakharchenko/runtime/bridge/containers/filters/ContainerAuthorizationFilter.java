package com.github.vzakharchenko.runtime.bridge.containers.filters;

import static com.github.vzakharchenko.runtime.bridge.containers.ForgeIngressHeaders.INVOCATION_ID;
import static org.springframework.util.StringUtils.hasText;

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
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ingress filter for Forge Container requests: when {@code x-forge-invocation-id} is present, loads
 * {@link ForgeAuthentication} via {@link ForgeContextService} and installs it on {@code
 * SecurityContextHolder} for the remainder of the filter chain.
 *
 * <p>Inserted inside Spring Security's filter chain by {@code ContainerWebSecurityConfiguration}
 * via {@code addFilterAfter(SecurityContextHolderFilter.class)}. The placement is intentional —
 * running before {@code SecurityContextHolderFilter} (or entirely outside the security chain as a
 * {@code FilterRegistrationBean}) lets that filter load a deferred empty {@code SecurityContext}
 * from the stateless repository and overwrite whatever this filter installed, after which {@code
 * AuthorizationFilter} rejects non-public paths with 403. Cleanup of the security context is
 * delegated to {@code SecurityContextHolderFilter}, which clears it in its own {@code finally}
 * block at the end of the request.
 *
 * <p>Spring Boot's default servlet-container registration is suppressed by a disabled {@code
 * FilterRegistrationBean} in {@link
 * com.github.vzakharchenko.runtime.bridge.containers.AtlassianConnectForgeContainerAutoConfiguration};
 * the filter participates in the Spring Security chain only.
 */
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
      if (!hasText(invocationId)) {
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
    }
  }
}
