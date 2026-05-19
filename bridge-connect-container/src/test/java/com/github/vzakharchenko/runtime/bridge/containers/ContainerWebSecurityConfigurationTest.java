package com.github.vzakharchenko.runtime.bridge.containers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.github.vzakharchenko.runtime.bridge.containers.filters.ContainerAuthorizationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;

@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
class ContainerWebSecurityConfigurationTest {

  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  SecurityAutoConfiguration.class,
                  SecurityFilterAutoConfiguration.class,
                  DispatcherServletAutoConfiguration.class,
                  HttpMessageConvertersAutoConfiguration.class,
                  WebMvcAutoConfiguration.class))
          .withUserConfiguration(ContainerWebSecurityConfiguration.class, StubFilterConfig.class);

  @Test
  void securityFilterChainBeanIsCreatedWithDefaultPublicPaths() {
    runner.run(
        ctx -> {
          assertThat(ctx).hasSingleBean(SecurityFilterChain.class);
          assertThat(ctx).hasSingleBean(ContainerSecurityProperties.class);
          assertThat(ctx.getBean(ContainerSecurityProperties.class).getPublicPaths())
              .containsExactly("/health");
        });
  }

  @Test
  void publicPathsPropertyOverrideIsBound() {
    runner
        .withPropertyValues(
            "bridge.container.security.public-paths=/health,/actuator/health/**,/api/public/**")
        .run(
            ctx -> {
              assertThat(ctx).hasSingleBean(SecurityFilterChain.class);
              assertThat(ctx.getBean(ContainerSecurityProperties.class).getPublicPaths())
                  .containsExactly("/health", "/actuator/health/**", "/api/public/**");
            });
  }

  @Configuration
  static class StubFilterConfig {
    @Bean
    ContainerAuthorizationFilter containerAuthorizationFilter() {
      return new ContainerAuthorizationFilter(mock(ForgeContextService.class));
    }
  }
}
