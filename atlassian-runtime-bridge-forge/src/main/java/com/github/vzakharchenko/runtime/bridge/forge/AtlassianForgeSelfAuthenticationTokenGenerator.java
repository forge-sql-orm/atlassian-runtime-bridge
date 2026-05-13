package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.internal.AtlassianConnectProperties;
import com.atlassian.connect.spring.internal.auth.AbstractAuthentication;
import com.atlassian.connect.spring.internal.descriptor.AddonConfigurationService;
import com.atlassian.connect.spring.internal.request.jwt.SelfAuthenticationTokenGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Specialised {@link SelfAuthenticationTokenGenerator} that disables self
 * JWT generation for Forge-originated requests.
 * <p>
 * For classic Connect requests (identified by {@link AbstractAuthentication})
 * it delegates to the parent implementation; for Forge traffic it returns
 * an empty token so that all authentication is driven by Forge invocation
 * tokens instead of Connect self-auth.
 */
@Primary
@ConditionalOnMissingBean(AtlassianForgeAutoConfiguration.class)
@Component
public class AtlassianForgeSelfAuthenticationTokenGenerator extends SelfAuthenticationTokenGenerator {

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
