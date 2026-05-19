package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import java.util.Optional;
import org.springframework.security.core.Authentication;

/**
 * Security bridge between Forge (invocation / offline tokens) and the Atlassian Connect Spring
 * model: the same {@link com.atlassian.connect.spring.AtlassianHost}, {@link
 * com.atlassian.connect.spring.AtlassianHostUser}, and {@link Authentication} types the rest of
 * Connect already use, backed by {@link
 * com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication} where appropriate.
 *
 * <p>Typical uses:
 *
 * <ul>
 *   <li>{@link #getAtlassianHostUserFromContext()} — build the current user from FIT / offline
 *       context;
 *   <li>{@link #getAuthentication(AtlassianHostUser)} / {@link #getAuthentication(AtlassianHost)} —
 *       construct {@link com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication} for
 *       programmatic flows (see also {@link ManualAuthorizationServiceImpl});
 * </ul>
 */
public interface AtlassianForgeSecurityBridgeService {

  /**
   * Resolves {@link AtlassianHostUser} from the current {@link
   * com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever} context when a
   * FIT / offline token is present.
   */
  Optional<AtlassianHostUser> getAtlassianHostUserFromContext();

  /**
   * Builds {@link com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication} for a
   * concrete user, including user token impersonation when an account id is set on {@code
   * atlassianHostUser}.
   */
  Authentication getAuthentication(AtlassianHostUser atlassianHostUser);

  /**
   * Builds {@link com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication} for app-level
   * (installation) access without a user principal.
   */
  Authentication getAuthentication(AtlassianHost atlassianHost);
}
