package com.github.vzakharchenko.runtime.bridge.containers;

import com.github.vzakharchenko.runtime.bridge.containers.filters.ContainerAuthorizationFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot auto-configuration for the Forge Containers runtime: scans the bridge {@code common}
 * API package and the {@code containers} package so the egress client, ingress filters, Forge
 * context service, and product adapters get registered without consumer-side
 * {@code @ComponentScan}.
 *
 * <p>Registered via {@code
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}. Atlassian
 * Connect/JPA auto-configurations are suppressed via {@link
 * ContainerAutoConfigurationEnvironmentPostProcessor} ({@code spring.autoconfigure.exclude}).
 */
@AutoConfiguration
@ComponentScan("com.github.vzakharchenko.runtime.bridge.common")
@ComponentScan("com.github.vzakharchenko.runtime.bridge.containers")
public class AtlassianConnectForgeContainerAutoConfiguration {

  /**
   * Registers {@link ContainerAuthorizationFilter} ahead of Spring Security
   * ({@code SecurityProperties.Filter.DEFAULT_ORDER = -100}). Running before the security chain
   * means the filter seeds {@code SecurityContextHolder} with {@code ForgeAuthentication} before
   * {@code AuthorizationFilter} evaluates {@code anyRequest().authenticated()}; otherwise Spring
   * Security rejects every non-public path with 403.
   */
  @Bean
  FilterRegistrationBean<ContainerAuthorizationFilter> containerAuthorizationFilterRegistration(
      ForgeContextService forgeContextService) {
    var registration =
        new FilterRegistrationBean<>(new ContainerAuthorizationFilter(forgeContextService));
    registration.setOrder(-150);
    return registration;
  }
}
