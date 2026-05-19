package com.github.vzakharchenko.runtime.bridge.containers;

import com.github.vzakharchenko.runtime.bridge.containers.filters.ContainerAuthorizationFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

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

  /**
   * Stateless HTTP security. {@link ContainerAuthorizationFilter} is inserted right after Spring
   * Security's {@code SecurityContextHolderFilter} — running earlier (e.g. outside the chain or
   * before this filter) would let {@code SecurityContextHolderFilter} load an empty deferred
   * context from the {@code NullSecurityContextRepository} (stateless policy) and overwrite the
   * {@code ForgeAuthentication} we just installed, causing {@code AuthorizationFilter} to reject
   * non-public paths.
   */
  @Bean
  SecurityFilterChain containerSecurityFilterChain(
      HttpSecurity http,
      ContainerSecurityProperties properties,
      ContainerAuthorizationFilter containerAuthorizationFilter)
      throws Exception {
    String[] publicPaths = properties.getPublicPaths().toArray(new String[0]);
    http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterAfter(containerAuthorizationFilter, SecurityContextHolderFilter.class)
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(publicPaths).permitAll().anyRequest().authenticated());
    return http.build();
  }
}
