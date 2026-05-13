package com.github.vzakharchenko.runtime.bridge.forge;

/**
 * Service for obtaining a user-scoped token by impersonating a user
 * in a given Forge context.
 */
public interface ImpersonationUserService {

    /**
     * Requests an offline user auth token for the given user and context.
     *
     * @param userId    Atlassian account identifier of the user to impersonate
     * @param contextId Forge context id (for example Jira site ARI)
     * @param authHeader system-level bearer token used to call Atlassian GraphQL
     * @return bearer token representing the impersonated user
     */
    String impersonateUser(String userId, String contextId, String authHeader);
}
