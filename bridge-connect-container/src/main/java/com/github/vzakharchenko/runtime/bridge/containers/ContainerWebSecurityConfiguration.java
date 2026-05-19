package com.github.vzakharchenko.runtime.bridge.containers;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security for Forge Container apps when {@link ContainerExcludedAutoConfigurations} vetoes {@code
 * AtlassianConnectAutoConfiguration} (and its {@code AtlassianConnectWebSecurityConfiguration}).
 *
 * <p>Without this bean, Spring Boot's default security enables form login but no {@code
 * AuthenticationProvider}, which breaks any {@code /login} attempt. Ingress auth is handled by
 * {@link com.github.vzakharchenko.runtime.bridge.containers.filters.ContainerAuthorizationFilter}
 * and {@link ManualAuthorizationServiceImpl}; HTTP security stays open so those filters/controllers
 * can run.
 *
 * <p>Paths exposed without authentication are taken from {@link ContainerSecurityProperties} —
 * override {@code bridge.container.security.public-paths} when an app uses different health/probe
 * endpoints than the defaults.
 */
@AutoConfiguration
@EnableConfigurationProperties(ContainerSecurityProperties.class)
public class ContainerWebSecurityConfiguration {

  /** Stateless HTTP security; public paths come from {@link ContainerSecurityProperties}. */
  @Bean
  SecurityFilterChain containerSecurityFilterChain(
      HttpSecurity http, ContainerSecurityProperties properties) throws Exception {
    String[] publicPaths = properties.getPublicPaths().toArray(new String[0]);
    http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(publicPaths).permitAll().anyRequest().authenticated());
    return http.build();
  }
}
