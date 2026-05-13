package com.github.vzakharchenko.runtime.bridge.common;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import org.springframework.web.client.RestTemplate;

/**
 * Product-scoped HTTP client for <strong>non-Jira / non-Confluence</strong> Atlassian Cloud routes
 * (generic Forge {@code request()} / Connect equivalent).
 * <p>
 * Application code depends on this interface only. The Forge bridge module registers an
 * {@code com.github.vzakharchenko.runtime.bridge.forge.products.OtherProductSelectAdapter} bean that
 * <strong>automatically</strong> switches between Connect and Forge backends like the Jira and
 * Confluence adapters.
 */
public interface OtherProductAdapter {
    /** Calls the product API as the Connect add-on / Forge app installation. */
    RestTemplate authenticatedAsAddon(AtlassianHost host);

    /** Calls the product API as the current host user. */
    RestTemplate authenticatedAsCurrentUser();

    /** Calls the product API as a specific {@link AtlassianHostUser} (impersonation). */
    RestTemplate impersonation(AtlassianHostUser hostUser);
}
