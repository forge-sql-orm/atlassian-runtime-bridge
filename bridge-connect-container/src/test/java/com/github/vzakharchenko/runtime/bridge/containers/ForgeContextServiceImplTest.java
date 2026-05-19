package com.github.vzakharchenko.runtime.bridge.containers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ForgeContextServiceImplTest {

  private static final String INVOCATION_ID = "inv-1";
  private static final String INSTALLATION_ARI = "ari:cloud:ecosystem::installation/inst-uuid";
  private static final String CLOUD_ID = "cloud-1";

  @Mock private EgressClientService egressClientService;

  private ForgeContextServiceImpl service;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service = new ForgeContextServiceImpl(egressClientService, Optional.of(objectMapper));
  }

  @Test
  void emulateForgeAuthentication_buildsHostUserFromContext() {
    var body =
        ContainersTestFixtures.invocationContextJson(
            objectMapper, CLOUD_ID, INSTALLATION_ARI, "account-1");
    when(egressClientService.getInvocationContext(INVOCATION_ID))
        .thenReturn(ResponseEntity.ok(body));

    Optional<ForgeAuthentication> authentication =
        service.emulateForgeAuthentication(INVOCATION_ID);

    assertThat(authentication).isPresent();
    var user = (AtlassianHostUser) authentication.get().getPrincipal();
    assertThat(user.getHost().getCloudId()).isEqualTo(CLOUD_ID);
    assertThat(user.getHost().getInstallationId()).isEqualTo("inst-uuid");
    assertThat(user.getHost().getBaseUrl()).isEqualTo("https://example.atlassian.net");
    assertThat(user.getUserAccountId()).contains("account-1");
  }

  @Test
  void emulateForgeAuthentication_withoutAccountId_omitsUserAccountId() {
    var body =
        ContainersTestFixtures.invocationContextJson(
            objectMapper, CLOUD_ID, INSTALLATION_ARI, null);
    when(egressClientService.getInvocationContext(INVOCATION_ID))
        .thenReturn(ResponseEntity.ok(body));

    Optional<ForgeAuthentication> authentication =
        service.emulateForgeAuthentication(INVOCATION_ID);

    assertThat(authentication).isPresent();
    var user = (AtlassianHostUser) authentication.get().getPrincipal();
    assertThat(user.getUserAccountId()).isEmpty();
  }

  @Test
  void emulateForgeAuthentication_withoutCloudId_returnsEmpty() {
    var body = objectMapper.createObjectNode();
    body.putObject("app").put("installationId", INSTALLATION_ARI);
    when(egressClientService.getInvocationContext(INVOCATION_ID))
        .thenReturn(ResponseEntity.ok(body));

    assertThat(service.emulateForgeAuthentication(INVOCATION_ID)).isEmpty();
  }

  @Test
  void emulateForgeAuthentication_withUnparseableBody_returnsAuthenticationWithoutPrincipal() {
    when(egressClientService.getInvocationContext(INVOCATION_ID))
        .thenReturn(ResponseEntity.ok(TextNode.valueOf("not-json-object")));

    Optional<ForgeAuthentication> authentication =
        service.emulateForgeAuthentication(INVOCATION_ID);

    assertThat(authentication).isPresent();
    assertThat(authentication.get().getPrincipal()).isNull();
  }

  @Test
  void parseJsonNode_returnsEmptyOnInvalidShape() {
    assertThat(
            ForgeContextServiceImpl.parseJsonNode(
                objectMapper, TextNode.valueOf("x"), AppContext.class))
        .isEmpty();
  }
}
