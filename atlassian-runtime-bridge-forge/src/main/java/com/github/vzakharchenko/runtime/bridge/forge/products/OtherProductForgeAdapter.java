package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.AtlassianForgeRestClients;
import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import com.github.vzakharchenko.runtime.bridge.common.OtherProductAdapter;
import com.github.vzakharchenko.runtime.bridge.forge.ImpersonationUserService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

/**
 * Forge path for <strong>generic</strong> product REST ({@code request()}), used when the host product
 * is neither Jira nor Confluence or when using the default Forge client surface.
 */
public class OtherProductForgeAdapter extends AbstractProductForgeAdapter implements OtherProductAdapter {
    public OtherProductForgeAdapter(AtlassianForgeRestClients atlassianForgeRestClients,
                                    ForgeSecurityContextRetriever forgeSecurityContextRetriever,
                                    RestTemplateBuilder restTemplateBuilder,
                                    ImpersonationUserService impersonationUserService) {
        super(atlassianForgeRestClients, forgeSecurityContextRetriever, restTemplateBuilder, impersonationUserService);
    }

    @Override
    public RestTemplate authenticatedAsAddon(AtlassianHost host) {
        return asAddon(host).request();
    }

    @Override
    public RestTemplate authenticatedAsCurrentUser() {
        return asCurrentUser().request();
    }
}
