package com.github.vzakharchenko.runtime.bridge.forge;

/**
 * Obtains a <strong>user-scoped</strong> offline auth token for Atlassian product APIs, given a
 * system-level bearer already issued to the app (Forge offline / system token).
 *
 * <p>Implementations typically call Atlassian GraphQL ({@code offlineUserAuthToken}) with the
 * appropriate product {@code contextIds} (see {@link AtlassianForgeUtils#getContextId(String)}).
 */
@FunctionalInterface
public interface ImpersonationUserService {

  /**
   * Exchanges the system token for a user token via {@code offlineUserAuthToken}.
   *
   * @param userId Atlassian account id ({@code ari:cloud:identity::user/...} or bare account id,
   *     per API contract)
   * @param contextId product context ARI (for example Jira site {@code ari:cloud:jira::site/...})
   * @param authHeader bearer token for the app installation (passed as {@code Authorization} to
   *     GraphQL)
   * @return bearer token for the impersonated user
   */
  String impersonateUser(String userId, String contextId, String authHeader);
}
