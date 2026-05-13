package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.internal.AtlassianConnectAutoConfiguration;
import com.atlassian.connect.spring.internal.AtlassianConnectProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

import static com.atlassian.connect.spring.internal.descriptor.AddonDescriptorLoader.DESCRIPTOR_FILENAME;

/**
 * Auto-configuration that wires Forge support into a classic Connect
 * Spring Boot application.
 * <p>
 * It keeps the standard {@link AtlassianConnectAutoConfiguration} in place,
 * registers the {@link AtlassianForgeFilter} and exposes a shared {@link RestClient}
 * for calling Atlassian GraphQL APIs from Forge-related services.
 */
@Configuration
@EnableRetry
@ComponentScan(basePackageClasses = {AtlassianConnectAutoConfiguration.class})
@ConditionalOnResource(resources = "classpath:" + DESCRIPTOR_FILENAME)
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
    public RestClient graphqlClient(
    ) {
        return RestClient.create(API_ATLASSIAN_COM_GRAPHQL);
    }
}
