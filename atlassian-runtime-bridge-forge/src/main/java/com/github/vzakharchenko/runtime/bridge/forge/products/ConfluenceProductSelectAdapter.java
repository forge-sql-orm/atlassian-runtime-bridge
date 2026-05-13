package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.AtlassianForgeRestClients;
import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.github.vzakharchenko.runtime.bridge.common.ConfluenceProductAdapter;
import com.github.vzakharchenko.runtime.bridge.forge.ImpersonationUserService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Spring {@link Component} implementing {@link ConfluenceProductAdapter} with <strong>automatic</strong>
 * Connect vs Forge delegation (same pattern as {@link JiraProductSelectAdapter}).
 */
@Component
public class ConfluenceProductSelectAdapter implements ConfluenceProductAdapter {
    private final ConnectProductsAdapter connectProductsAdapter;
    private final ConfluenceProductForgeAdapter confluenceProductForgeAdapter;

    public ConfluenceProductSelectAdapter(AtlassianHostRestClients atlassianHostRestClients,
                                          AtlassianForgeRestClients atlassianForgeRestClients,
                                          ForgeSecurityContextRetriever forgeSecurityContextRetriever,
                                          RestTemplateBuilder restTemplateBuilder,
                                          ImpersonationUserService impersonationUserService) {
        this.connectProductsAdapter = new ConnectProductsAdapter(atlassianHostRestClients);
        this.confluenceProductForgeAdapter = new ConfluenceProductForgeAdapter(atlassianForgeRestClients, forgeSecurityContextRetriever, restTemplateBuilder, impersonationUserService);
    }

    private boolean isForge() {
        return SecurityContextHolder.getContext().getAuthentication() instanceof ForgeAuthentication;
    }

    @Override
    public RestTemplate authenticatedAsAddon(AtlassianHost host) {
        return isForge() ? confluenceProductForgeAdapter.authenticatedAsAddon(host) : connectProductsAdapter.authenticatedAsAddon(host);
    }

    @Override
    public RestTemplate authenticatedAsCurrentUser() {
        return isForge() ? confluenceProductForgeAdapter.authenticatedAsCurrentUser() : connectProductsAdapter.authenticatedAsCurrentUser();
    }

    @Override
    public RestTemplate impersonation(AtlassianHostUser hostUser) {
        return isForge() ? confluenceProductForgeAdapter.impersonation(hostUser) : connectProductsAdapter.impersonation(hostUser);
    }
}
