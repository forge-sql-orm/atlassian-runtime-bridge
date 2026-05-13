package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.AtlassianForgeRestClients;
import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.github.vzakharchenko.runtime.bridge.common.JiraProductAdapter;
import com.github.vzakharchenko.runtime.bridge.forge.ImpersonationUserService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Spring {@link Component} implementing {@link JiraProductAdapter} with <strong>automatic</strong>
 * Connect vs Forge delegation.
 * <p>
 * If {@link SecurityContextHolder} holds a {@link ForgeAuthentication}, requests go through
 * {@link JiraProductForgeAdapter}; otherwise {@link ConnectProductsAdapter} (classic Connect
 * {@link AtlassianHostRestClients}) is used. Same interface for iframe and Forge Custom UI code paths.
 */
@Component
public class JiraProductSelectAdapter implements JiraProductAdapter {
    private final ConnectProductsAdapter connectProductsAdapter;
    private final JiraProductForgeAdapter jiraProductForgeAdapter;


    public JiraProductSelectAdapter(AtlassianHostRestClients atlassianHostRestClients,
                                    AtlassianForgeRestClients atlassianForgeRestClients,
                                    ForgeSecurityContextRetriever forgeSecurityContextRetriever,
                                    RestTemplateBuilder restTemplateBuilder,
                                    ImpersonationUserService impersonationUserService) {
        this.connectProductsAdapter = new ConnectProductsAdapter(atlassianHostRestClients);
        this.jiraProductForgeAdapter = new JiraProductForgeAdapter(atlassianForgeRestClients, forgeSecurityContextRetriever, restTemplateBuilder, impersonationUserService);
    }

    private boolean isForge() {
        return SecurityContextHolder.getContext().getAuthentication() instanceof ForgeAuthentication;
    }

    @Override
    public RestTemplate authenticatedAsAddon(AtlassianHost host) {
        return isForge() ? jiraProductForgeAdapter.authenticatedAsAddon(host) : connectProductsAdapter.authenticatedAsAddon(host);
    }

    @Override
    public RestTemplate authenticatedAsCurrentUser() {
        return isForge() ? jiraProductForgeAdapter.authenticatedAsCurrentUser() : connectProductsAdapter.authenticatedAsCurrentUser();
    }

    @Override
    public RestTemplate impersonation(AtlassianHostUser hostUser) {
        return isForge() ? jiraProductForgeAdapter.impersonation(hostUser) : connectProductsAdapter.impersonation(hostUser);
    }
}
