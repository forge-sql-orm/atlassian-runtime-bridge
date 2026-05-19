package com.github.vzakharchenko.runtime.bridge.containers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.vzakharchenko.runtime.bridge.containers.EgressClientServiceImpl.AuthType;
import com.github.vzakharchenko.runtime.bridge.containers.EgressClientServiceImpl.InstallationAuth;
import com.github.vzakharchenko.runtime.bridge.containers.exceptions.EgressRequestException;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class EgressClientServiceImplTest {

  private static final String INSTALLATION_ID = "inst";

  private MockRestServiceServer mockServer;
  private EgressClientServiceImpl service;

  @BeforeEach
  void setUp() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    RestClient restClient = restClientBuilder.build();
    RestClient.Builder builder = mock(RestClient.Builder.class);
    when(builder.build()).thenReturn(restClient);
    service = new EgressClientServiceImpl(Optional.of(builder));
    ReflectionTestUtils.setField(service, "egressProxyUrl", "http://egress-proxy:7072");
  }

  @Test
  void installationAuth_factories() {
    assertThat(InstallationAuth.installation(INSTALLATION_ID))
        .isEqualTo(new InstallationAuth(INSTALLATION_ID, null, null));
    assertThat(InstallationAuth.asApp(INSTALLATION_ID))
        .isEqualTo(new InstallationAuth(INSTALLATION_ID, AuthType.APP, null));
    assertThat(InstallationAuth.asUser(INSTALLATION_ID, "acc"))
        .isEqualTo(new InstallationAuth(INSTALLATION_ID, AuthType.USER, "acc"));
  }

  @Test
  void jiraEgressBaseUrl_handlesTrailingSlash() {
    ReflectionTestUtils.setField(service, "egressProxyUrl", "http://proxy:7072");
    assertThat((String) ReflectionTestUtils.invokeMethod(service, "jiraEgressBaseUrl"))
        .isEqualTo("http://proxy:7072/jira");

    ReflectionTestUtils.setField(service, "egressProxyUrl", "http://proxy:7072/");
    assertThat((String) ReflectionTestUtils.invokeMethod(service, "jiraEgressBaseUrl"))
        .isEqualTo("http://proxy:7072/jira");
  }

  @Test
  void buildJiraUri_appendsApiPath() {
    URI uri = ReflectionTestUtils.invokeMethod(service, "buildJiraUri", "/rest/api/3/myself");
    assertThat(uri).hasToString("http://egress-proxy:7072/jira/rest/api/3/myself");
  }

  @Test
  void getInvocationContext_callsSidecarWithInvocationAuth() {
    ObjectNode body = new ObjectMapper().createObjectNode().put("ok", true);
    mockServer
        .expect(MockRestRequestMatchers.requestTo("http://egress-proxy:7072/invocation/context"))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andExpect(
            MockRestRequestMatchers.header(
                ForgeEgressHeaders.FORGE_AUTHORIZATION, "Forge id=inv-123"))
        .andRespond(
            MockRestResponseCreators.withSuccess(body.toString(), MediaType.APPLICATION_JSON));

    var response = service.getInvocationContext("inv-123");

    assertThat(response.getBody()).isEqualTo(body);
    mockServer.verify();
  }

  @Test
  void sendJiraRequest_usesJiraPrefixAndInstallationAuth() {
    ObjectNode body = new ObjectMapper().createObjectNode().put("id", "1");
    mockServer
        .expect(
            MockRestRequestMatchers.requestTo("http://egress-proxy:7072/jira/rest/api/3/myself"))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andExpect(
            MockRestRequestMatchers.header(
                ForgeEgressHeaders.FORGE_AUTHORIZATION,
                "Forge installationId=inst-1,as=user,accountId=acc-1"))
        .andRespond(
            MockRestResponseCreators.withSuccess(body.toString(), MediaType.APPLICATION_JSON));

    var response =
        service.sendJiraRequest(
            InstallationAuth.asUser(INSTALLATION_ID + "-1", "acc-1"),
            HttpMethod.GET,
            "/rest/api/3/myself",
            null);

    assertThat(response.getBody()).isEqualTo(body);
    mockServer.verify();
  }

  @Test
  void execute_throwsEgressRequestExceptionOnErrorStatus() {
    mockServer
        .expect(MockRestRequestMatchers.anything())
        .andRespond(MockRestResponseCreators.withBadRequest().body("bad request"));

    assertThatThrownBy(() -> service.getInvocationContext("inv"))
        .isInstanceOf(EgressRequestException.class)
        .hasMessageContaining("Context request failed with status 400")
        .hasMessageContaining("bad request");
  }

  @Test
  void jiraTemplateRequest_usesJiraBaseUrlForRelativePaths() {
    RestTemplate template =
        service.jiraTemplateRequest(InstallationAuth.asApp(INSTALLATION_ID + "-1"));
    var handler = template.getUriTemplateHandler();

    assertThat(handler.expand("/rest/api/3/myself"))
        .hasToString("http://egress-proxy:7072/jira/rest/api/3/myself");
  }
}
