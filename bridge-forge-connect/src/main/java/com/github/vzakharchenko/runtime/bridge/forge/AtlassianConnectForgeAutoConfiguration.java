package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.internal.AtlassianConnectAutoConfiguration;
import com.atlassian.connect.spring.internal.AtlassianConnectProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

/**
 * Spring Boot auto-configuration for the <strong>Connect + Forge hybrid</strong> layout: Connect
 * {@link AtlassianConnectAutoConfiguration} stays active while this class registers bridge beans.
 *
 * <p>Registered via {@code
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}. Consumers only
 * need the {@code bridge-forge-connect} dependency plus Connect starters; do not
 * {@code @ComponentScan} this class (duplicate beans). Optional: anchor scan on this class only to
 * co-locate your app package with the bridge (see README).
 *
 * <p>Declares:
 *
 * <ul>
 *   <li>{@link AtlassianForgeFilter} — upgrades {@link
 *       com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication} with a resolved {@link
 *       com.atlassian.connect.spring.AtlassianHostUser};
 *   <li>a shared {@link RestClient} pointing at Atlassian GraphQL ({@link
 *       #API_ATLASSIAN_COM_GRAPHQL}) for {@link ImpersonationUserServiceImpl} and related Forge
 *       flows.
 * </ul>
 *
 * Filter order uses {@link
 * com.atlassian.connect.spring.internal.AtlassianConnectProperties#getForgeFilterOrder()}.
 */
@AutoConfiguration
@AutoConfigureAfter(AtlassianConnectAutoConfiguration.class)
@EnableRetry
@EnableConfigurationProperties(AtlassianConnectProperties.class)
@ComponentScan(basePackageClasses = {AtlassianConnectAutoConfiguration.class})
@ComponentScan("com.github.vzakharchenko.runtime.bridge.common")
@ComponentScan(
    basePackages = "com.github.vzakharchenko.runtime.bridge.forge",
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
              AtlassianConnectForgeAutoConfiguration.class,
              AtlassianConnectForgeJpaAutoConfiguration.class
            }))
public class AtlassianConnectForgeAutoConfiguration {

  public static final String API_ATLASSIAN_COM_GRAPHQL = "https://api.atlassian.com/graphql";

  @Bean
  public FilterRegistrationBean<AtlassianForgeFilter> pluginForgeFilterRegistrationBean(
      AtlassianConnectProperties atlassianConnectProperties,
      AtlassianForgeFilter pluginForgeFilter) {
    FilterRegistrationBean<AtlassianForgeFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(pluginForgeFilter);
    registrationBean.setOrder(atlassianConnectProperties.getForgeFilterOrder() + 1);
    return registrationBean;
  }

  @Bean
  public RestClient graphqlClient() {
    return RestClient.create(API_ATLASSIAN_COM_GRAPHQL);
  }
}
