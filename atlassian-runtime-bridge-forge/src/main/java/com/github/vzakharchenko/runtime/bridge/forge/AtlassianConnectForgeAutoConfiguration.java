package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.internal.AtlassianConnectAutoConfiguration;
import com.atlassian.connect.spring.internal.AtlassianConnectProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

/**
 * Auto-configuration for the <strong>Connect + Forge hybrid</strong> layout: full Connect
 * {@link AtlassianConnectAutoConfiguration} stays active while this class registers bridge beans
 * (component scan for {@code com.github.vzakharchenko.runtime.bridge.*}).
 * <p>
 * Declares:
 * <ul>
 *     <li>{@link AtlassianForgeFilter} — upgrades {@link com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication}
 *         with a resolved {@link com.atlassian.connect.spring.AtlassianHostUser};</li>
 *     <li>a shared {@link RestClient} pointing at Atlassian GraphQL ({@link #API_ATLASSIAN_COM_GRAPHQL})
 *         for {@link ImpersonationUserServiceImpl} and related Forge flows.</li>
 * </ul>
 * Filter order uses {@link com.atlassian.connect.spring.internal.AtlassianConnectProperties#getForgeFilterOrder()}.
 */
@Configuration
@EnableRetry
@ComponentScan(basePackageClasses = {AtlassianConnectAutoConfiguration.class})
@EnableConfigurationProperties(AtlassianConnectProperties.class)
@ComponentScan("com.github.vzakharchenko.runtime.bridge.common")
@ComponentScan("com.github.vzakharchenko.runtime.bridge.forge")
public class AtlassianConnectForgeAutoConfiguration {

    public static final String API_ATLASSIAN_COM_GRAPHQL = "https://api.atlassian.com/graphql";

    @Bean
    public FilterRegistrationBean<AtlassianForgeFilter> pluginForgeFilterRegistrationBean(AtlassianConnectProperties atlassianConnectProperties, AtlassianForgeFilter pluginForgeFilter) {
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
