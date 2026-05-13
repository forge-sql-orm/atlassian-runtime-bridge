package com.github.vzakharchenko.runtime.bridge.common;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import org.springframework.web.client.RestTemplate;

/**
 * Product-scoped HTTP client for <strong>Confluence</strong> REST APIs.
 * <p>
 * Application code depends on this interface only. The Forge bridge module registers a
 * {@code com.github.vzakharchenko.runtime.bridge.forge.products.ConfluenceProductSelectAdapter} bean
 * that <strong>automatically</strong> delegates to Connect or Forge implementations according to
 * the active authentication (see {@code com.github.vzakharchenko.runtime.bridge.forge.products}).
 */
public interface ConfluenceProductAdapter {
    /** Calls Confluence as the Connect add-on / Forge app installation (service identity). */
    RestTemplate authenticatedAsAddon(AtlassianHost host);

    /** Calls Confluence as the signed-in user in the host product. */
    RestTemplate authenticatedAsCurrentUser();

    /**
     * Calls Confluence as a specific {@link AtlassianHostUser}.
     * Forge path uses the same impersonation pipeline as other Forge product adapters.
     */
    RestTemplate impersonation(AtlassianHostUser hostUser);
}
