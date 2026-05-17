package com.github.vzakharchenko.runtime.bridge.forge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ImpersonationUserServiceImplTest {

  private static final String GRAPHQL_URL = "https://api.atlassian.com/graphql";

  private static final String SUCCESS_WITH_TOKEN =
      "{\"data\":{\"offlineUserAuthToken\":"
          + "{\"success\":true,\"error\":null,"
          + "\"authToken\":{\"token\":\"offline-bearer\",\"ttl\":3600}}}}";

  private static final String FAILURE_WITH_MESSAGE =
      "{\"data\":{\"offlineUserAuthToken\":"
          + "{\"success\":false,\"error\":{\"message\":\"user blocked\"},\"authToken\":null}}}";

  private static final String SUCCESS_WITHOUT_TOKEN =
      "{\"data\":{\"offlineUserAuthToken\":"
          + "{\"success\":true,\"error\":null,\"authToken\":null}}}";

  private MockRestServiceServer mockRestServiceServer;

  private ImpersonationUserServiceImpl impersonationUserService;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl(GRAPHQL_URL);
    mockRestServiceServer = MockRestServiceServer.bindTo(builder).build();
    RestClient restClient = builder.build();
    impersonationUserService = new ImpersonationUserServiceImpl(restClient);
  }

  @AfterEach
  void tearDown() {
    mockRestServiceServer.verify();
  }

  @Test
  void impersonateUser_returnsBearerTokenWhenGraphQlSucceeds() {
    mockRestServiceServer
        .expect(requestTo(GRAPHQL_URL))
        .andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON).body(SUCCESS_WITH_TOKEN));

    assertThat(impersonationUserService.impersonateUser("ari:user:1", "ctx-1", "fit-secret"))
        .as("mutation must return nested offline token string")
        .isEqualTo("offline-bearer");
  }

  @Test
  void impersonateUser_throwsWhenGraphQlReportsFailure() {
    mockRestServiceServer
        .expect(requestTo(GRAPHQL_URL))
        .andRespond(
            withSuccess().contentType(MediaType.APPLICATION_JSON).body(FAILURE_WITH_MESSAGE));

    assertThatThrownBy(
            () -> impersonationUserService.impersonateUser("ari:user:1", "ctx-1", "fit-secret"))
        .as("GraphQL success=false must surface as access denied")
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void impersonateUser_throwsWhenAuthTokenMissing() {
    mockRestServiceServer
        .expect(requestTo(GRAPHQL_URL))
        .andRespond(
            withSuccess().contentType(MediaType.APPLICATION_JSON).body(SUCCESS_WITHOUT_TOKEN));

    assertThatThrownBy(
            () -> impersonationUserService.impersonateUser("ari:user:1", "ctx-1", "fit-secret"))
        .as("successful mutation without authToken must be rejected")
        .isInstanceOf(AccessDeniedException.class);
  }
}
