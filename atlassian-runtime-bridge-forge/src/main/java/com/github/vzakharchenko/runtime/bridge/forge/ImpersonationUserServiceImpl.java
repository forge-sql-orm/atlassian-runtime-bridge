package com.github.vzakharchenko.runtime.bridge.forge;

import java.util.Objects;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@link ImpersonationUserService} backed by Spring's {@link HttpSyncGraphQlClient} over the shared
 * {@link RestClient} bean ({@code graphqlClient} from {@link
 * AtlassianConnectForgeAutoConfiguration}).
 *
 * <p>Posts the {@code offlineUserAuthToken} mutation to {@code https://api.atlassian.com/graphql},
 * validates {@code success} and error payloads, and returns the nested bearer token. {@link
 * Retryable} replays transient {@link RestClientException}s.
 */
@Component
public class ImpersonationUserServiceImpl implements ImpersonationUserService {
  private static final String OFFLINE_USER_AUTH_TOKEN_QUERY =
      """
            mutation OfflineTokenMutation($userId: String!, $contextIds: [ID!]!) {
                offlineUserAuthToken(
                    input: {userId: $userId, contextIds: $contextIds}
                ) {
                    success
                    errors {
                        message
                    }
                    authToken {
                        token
                        ttl
                    }
                }
            }
            """;
  private final RestClient aggRestClient;

  public ImpersonationUserServiceImpl(RestClient aggRestClient) {
    this.aggRestClient = aggRestClient;
  }

  @Retryable(retryFor = RestClientException.class)
  @Override
  public String impersonateUser(String userId, String contextId, String authHeader) {
    var aggGqlClient =
        HttpSyncGraphQlClient.builder(aggRestClient)
            .headers((headers) -> headers.setBearerAuth(authHeader))
            .build();

    String[] contextIds = {contextId};

    var authTokenResponse =
        aggGqlClient
            .document(OFFLINE_USER_AUTH_TOKEN_QUERY)
            .variable("contextIds", contextIds)
            .variable("userId", userId)
            .retrieveSync("offlineUserAuthToken")
            .toEntity(AuthTokenResponse.class);

    if (authTokenResponse == null) {
      throw new AccessDeniedException("Auth Token Response is null");
    }
    if (!authTokenResponse.success()) {
      throw new AccessDeniedException(
          Objects.isNull(authTokenResponse.error())
              ? "Authorization Error"
              : authTokenResponse.error().message());
    }
    var authToken =
        Objects.isNull(authTokenResponse.authToken()) ? null : authTokenResponse.authToken();
    if (authToken == null || Objects.isNull(authToken.token)) {
      throw new AccessDeniedException(
          "Authentication failed "
              + (Objects.isNull(authTokenResponse.error())
                  ? "Authorization Error"
                  : authTokenResponse.error().message()));
    }
    return authToken.token();
  }

  record Error(String message) {}

  record AuthToken(String token, Integer ttl) {}

  record AuthTokenResponse(boolean success, Error error, AuthToken authToken) {}
}
