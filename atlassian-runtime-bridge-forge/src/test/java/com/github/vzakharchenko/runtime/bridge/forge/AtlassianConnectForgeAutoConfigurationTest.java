package com.github.vzakharchenko.runtime.bridge.forge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.atlassian.connect.spring.internal.AtlassianConnectProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AtlassianConnectForgeAutoConfigurationTest {

  private final AtlassianConnectForgeAutoConfiguration configuration =
      new AtlassianConnectForgeAutoConfiguration();

  @Nested
  @ExtendWith(MockitoExtension.class)
  class PluginForgeFilterRegistrationBean {

    @Mock private AtlassianForgeSecurityBridgeService forgeSecurityBridgeService;

    private AtlassianConnectProperties atlassianConnectProperties;

    private AtlassianForgeFilter atlassianForgeFilter;

    @BeforeEach
    void setUp() {
      atlassianConnectProperties = new AtlassianConnectProperties();
      atlassianConnectProperties.setForgeFilterOrder(9000);
      atlassianForgeFilter = new AtlassianForgeFilter(forgeSecurityBridgeService);
    }

    @Test
    void registersFilterAtForgeOrderPlusOne() {
      FilterRegistrationBean<AtlassianForgeFilter> bean =
          configuration.pluginForgeFilterRegistrationBean(
              atlassianConnectProperties, atlassianForgeFilter);

      assertThat(bean.getOrder())
          .as("bridge filter must run immediately after Connect forge filter order")
          .isEqualTo(9001);
    }

    @Test
    void setsDeclaredFilterInstance() {
      FilterRegistrationBean<AtlassianForgeFilter> bean =
          configuration.pluginForgeFilterRegistrationBean(
              atlassianConnectProperties, atlassianForgeFilter);

      assertThat(bean.getFilter())
          .as("registration must use the provided AtlassianForgeFilter bean")
          .isSameAs(atlassianForgeFilter);
    }
  }

  @Test
  void graphqlClient_resolvesRelativePathsAgainstAtlassianGraphqlBaseUri() {
    RestClient created = configuration.graphqlClient();
    RestClient.Builder builder = created.mutate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    RestClient client = builder.build();
    server
        .expect(
            requestTo(AtlassianConnectForgeAutoConfiguration.API_ATLASSIAN_COM_GRAPHQL + "/health"))
        .andRespond(withSuccess());

    assertThatCode(
            () -> {
              client.get().uri("/health").retrieve().toBodilessEntity();
              server.verify();
            })
        .as("GET /health must hit the configured Atlassian GraphQL base URL")
        .doesNotThrowAnyException();
  }
}
