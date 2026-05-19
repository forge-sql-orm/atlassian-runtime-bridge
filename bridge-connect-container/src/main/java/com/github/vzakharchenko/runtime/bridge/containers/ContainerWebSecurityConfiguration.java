package com.github.vzakharchenko.runtime.bridge.containers;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security for Forge Container apps when {@link ContainerExcludedAutoConfigurations} vetoes
 * {@code AtlassianConnectAutoConfiguration} (and its {@code AtlassianConnectWebSecurityConfiguration}).
 *
 * <p>Without this bean, Spring Boot's default security enables form login but no
 * {@code AuthenticationProvider}, which breaks any {@code /login} attempt. Ingress auth is handled
 * by {@link com.github.vzakharchenko.runtime.bridge.containers.filters.ContainerAuthorizationFilter}
 * and {@link ManualAuthorizationServiceImpl}; HTTP security stays open so those filters/controllers
 * can run.
 */
@AutoConfiguration
public class ContainerWebSecurityConfiguration {

  /** Stateless, permit-all HTTP security; Forge auth is applied by servlet filters. */
  @Bean
  SecurityFilterChain containerSecurityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/health/**","/health")
                    .permitAll()
                    .anyRequest()
                    .permitAll());
    return http.build();
  }
}
