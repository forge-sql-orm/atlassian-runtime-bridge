package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import org.springframework.security.core.Authentication;

import java.util.Optional;

/**
 * Contract for bridging Forge invocation context into the Connect security model.
 * <p>
 * Implementations know how to:
 * <ul>
 *     <li>reconstruct an {@link AtlassianHostUser} from the current Forge invocation,</li>
 *     <li>create {@link Authentication} objects backed by Forge tokens,</li>
 *     <li>and report whether a given Connect host has already been migrated to Forge.</li>
 * </ul>
 */
public interface AtlassianForgeMigrationService {

    /**
     * Builds an {@link AtlassianHostUser} from the current Forge invocation context if available.
     */
    Optional<AtlassianHostUser> getAtlassianHostUserFromContext();

    /**
     * Creates an {@link Authentication} for a specific Forge user on a given host.
     */
    Authentication getAuthentication(AtlassianHostUser atlassianHostUser);

    /**
     * Creates an {@link Authentication} for host level access (no specific user).
     */
    Authentication getAuthentication(AtlassianHost atlassianHost);

    /**
     * Indicates whether a Connect installation has been migrated to Forge.
     */
    boolean isMigratedToForge(AtlassianHost atlassianHost);
}
