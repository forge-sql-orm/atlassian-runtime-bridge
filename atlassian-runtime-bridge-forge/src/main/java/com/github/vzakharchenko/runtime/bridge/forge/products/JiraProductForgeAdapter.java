package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.AtlassianForgeRestClients;
import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.github.vzakharchenko.runtime.bridge.common.JiraProductAdapter;
import com.github.vzakharchenko.runtime.bridge.forge.ImpersonationUserService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

/**
 * Forge path for <strong>Jira</strong> REST: uses {@link com.atlassian.connect.spring.AtlassianForgeRestClients}
 * {@code requestJira()} for app and user-scoped clients. Impersonation inherits
 * {@link AbstractProductForgeAdapter#impersonation(AtlassianHostUser)}.
 */
public class JiraProductForgeAdapter extends AbstractProductForgeAdapter implements JiraProductAdapter {

    public JiraProductForgeAdapter(AtlassianForgeRestClients atlassianForgeRestClients,
                                   ForgeSecurityContextRetriever forgeSecurityContextRetriever,
                                   RestTemplateBuilder restTemplateBuilder,
                                   ImpersonationUserService impersonationUserService) {
        super(atlassianForgeRestClients, forgeSecurityContextRetriever, restTemplateBuilder, impersonationUserService);
    }

    @Override
    public RestTemplate authenticatedAsAddon(AtlassianHost host) {
        return asAddon(host).requestJira();
    }

    @Override
    public RestTemplate authenticatedAsCurrentUser() {
        return asCurrentUser().requestJira();
    }
}
