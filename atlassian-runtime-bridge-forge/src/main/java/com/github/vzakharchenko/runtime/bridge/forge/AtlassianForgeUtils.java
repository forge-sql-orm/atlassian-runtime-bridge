package com.github.vzakharchenko.runtime.bridge.forge;

/**
 * Small helpers shared by Forge bridge code.
 */
public class AtlassianForgeUtils {
    public AtlassianForgeUtils() {
    }

    /**
     * Builds a Jira <em>site</em> context id (ARI) from a Forge {@code apiBaseUrl}.
     * <p>
     * Atlassian GraphQL mutations such as {@code offlineUserAuthToken} expect
     * {@code contextIds} in ARI form; for Jira Cloud the site id is the last path segment
     * of the product API base URL (for example {@code https://api.atlassian.com/ex/jira/<cloudId>}).
     *
     * @param getApiBaseUrl Forge app {@code apiBaseUrl} (slash-separated URL string)
     * @return ARI of the form {@code ari:cloud:jira::site/<siteId>}, where {@code siteId} is the last path segment of {@code getApiBaseUrl}
     */
    public static String getContextId(String getApiBaseUrl) {
        String[] parts = getApiBaseUrl.split("/");
        return "ari:cloud:jira::site/" + parts[parts.length - 1];
    }
}
