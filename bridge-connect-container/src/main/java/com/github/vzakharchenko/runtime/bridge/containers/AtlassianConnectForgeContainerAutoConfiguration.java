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
   * Exposes {@link ContainerAuthorizationFilter} as a Spring bean so {@code
   * ContainerWebSecurityConfiguration} can insert it inside the Spring Security chain via {@code
   * addFilterAfter(SecurityContextHolderFilter.class)}. The filter must run <strong>after</strong>
   * Spring Security's {@code SecurityContextHolderFilter} — otherwise the latter loads a deferred
   * {@code SecurityContext} from the (null) repository on a stateless request and overwrites the
   * authentication this filter installed.
   */
  @Bean
  ContainerAuthorizationFilter containerAuthorizationFilter(
      ForgeContextService forgeContextService) {
    return new ContainerAuthorizationFilter(forgeContextService);
  }

  /**
   * Disables Spring Boot's default servlet-container registration for {@link
   * ContainerAuthorizationFilter}; the filter participates in the Spring Security chain only.
   */
  @Bean
  FilterRegistrationBean<ContainerAuthorizationFilter> containerAuthorizationFilterRegistration(
      ContainerAuthorizationFilter filter) {
    var registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
