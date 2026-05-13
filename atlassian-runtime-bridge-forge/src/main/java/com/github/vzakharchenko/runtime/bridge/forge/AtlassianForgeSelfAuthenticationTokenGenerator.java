package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.internal.AtlassianConnectProperties;
import com.atlassian.connect.spring.internal.auth.AbstractAuthentication;
import com.atlassian.connect.spring.internal.descriptor.AddonConfigurationService;
import com.atlassian.connect.spring.internal.request.jwt.SelfAuthenticationTokenGenerator;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * {@link SelfAuthenticationTokenGenerator} override for hybrid Connect + Forge apps.
 *
 * <p>Connect Spring uses self-authentication JWTs for classic iframe / REST calls ({@link
 * AbstractAuthentication} in the security context). Forge requests instead carry invocation tokens;
 * returning an empty string here avoids minting a conflicting self-auth JWT while still delegating
 * to {@code super} for genuine Connect traffic.
 *
 * <p>Requires the Connect Spring Boot 6.x constructor {@code (AddonConfigurationService,
 * AtlassianConnectProperties)} on the base class.
 */
@Primary
@Component
public class AtlassianForgeSelfAuthenticationTokenGenerator
    extends SelfAuthenticationTokenGenerator {

  public AtlassianForgeSelfAuthenticationTokenGenerator(
      AddonConfigurationService addonConfigurationService,
      AtlassianConnectProperties atlassianConnectProperties) {
    super(addonConfigurationService, atlassianConnectProperties);
  }

  private boolean isConnectRequest() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication instanceof AbstractAuthentication;
  }

  @Override
  public String createSelfAuthenticationToken(AtlassianHostUser hostUser) {
    return isConnectRequest() ? super.createSelfAuthenticationToken(hostUser) : "";
  }
}
