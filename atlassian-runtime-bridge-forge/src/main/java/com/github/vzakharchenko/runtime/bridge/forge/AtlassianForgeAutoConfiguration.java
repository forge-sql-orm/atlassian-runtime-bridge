package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.ForgeSystemAccessTokenRepository;
import com.atlassian.connect.spring.internal.AtlassianConnectAutoConfiguration;
import com.atlassian.connect.spring.internal.AtlassianConnectContextModelAttributeProvider;
import com.atlassian.connect.spring.internal.AtlassianConnectProperties;
import com.atlassian.connect.spring.internal.auth.LifecycleURLHelper;
import com.atlassian.connect.spring.internal.auth.RequireAuthenticationHandlerInterceptor;
import com.atlassian.connect.spring.internal.auth.frc.ForgeInvocationTokenAuthenticationFilter;
import com.atlassian.connect.spring.internal.auth.frc.ForgeInvocationTokenValidator;
import com.atlassian.connect.spring.internal.descriptor.AddonDescriptorController;
import com.atlassian.connect.spring.internal.descriptor.AddonDescriptorLoader;
import com.atlassian.connect.spring.internal.request.AtlassianHostRestClientsImpl;
import com.atlassian.connect.spring.internal.request.UserAgentProvider;
import com.atlassian.connect.spring.internal.request.jwt.SelfAuthenticationTokenGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;

/**
 * Forge-only auto-configuration used when the core Connect auto-configuration
 * is not present on the classpath.
 * <p>
 * It registers the Forge invocation token filter and {@link AtlassianForgeFilter}
 * while deliberately excluding most of the classic Connect web stack to avoid
 * conflicting JWT / request handling.
 */
@Configuration
@ComponentScan(basePackageClasses = {AtlassianConnectAutoConfiguration.class},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        LifecycleURLHelper.class,
                        AddonDescriptorLoader.class,
                        SelfAuthenticationTokenGenerator.class,
                        AtlassianConnectContextModelAttributeProvider.class,
                        AddonDescriptorController.class,
                        UserAgentProvider.class,
                        AtlassianHostRestClientsImpl.class,
                        RequireAuthenticationHandlerInterceptor.class
                }),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.atlassian\\.connect\\.spring\\.internal\\.lifecycle\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.atlassian\\.connect\\.spring\\.internal\\.auth\\.jwt\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.atlassian\\.connect\\.spring\\.internal\\.request\\.jwt\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.atlassian\\.connect\\.spring\\.internal\\.request\\.oauth2\\..*"
                )
        })
@ComponentScan("com.github.vzakharchenko.runtime.bridge.common")
@ComponentScan("com.github.vzakharchenko.runtime.bridge.forge")
@EnableConfigurationProperties(AtlassianConnectProperties.class)
@ConditionalOnMissingClass("com.atlassian.connect.spring.internal.AtlassianConnectAutoConfiguration")
@EnableCaching
@EnableAsync
public class AtlassianForgeAutoConfiguration {

    public static final String API_ATLASSIAN_COM_GRAPHQL = "https://api.atlassian.com/graphql";


    private final ForgeInvocationTokenValidator validator;

    public AtlassianForgeAutoConfiguration(ForgeInvocationTokenValidator validator) {
        this.validator = validator;
    }

    @Bean
    public FilterRegistrationBean<ForgeInvocationTokenAuthenticationFilter> forgeInvocationTokenAuthenticationFilterRegistrationBean(
            AtlassianConnectProperties atlassianConnectProperties,
            ForgeSystemAccessTokenRepository forgeSystemAccessTokenRepository,
            LifecycleURLHelper lifecycleURLHelper) {
        FilterRegistrationBean<ForgeInvocationTokenAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ForgeInvocationTokenAuthenticationFilter(
                forgeSystemAccessTokenRepository,
                validator,
                lifecycleURLHelper));
        registrationBean.setOrder(atlassianConnectProperties.getForgeFilterOrder());
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<AtlassianForgeFilter> pluginForgeFilterRegistrationBean(AtlassianConnectProperties atlassianConnectProperties,
                                                                                          AtlassianForgeFilter pluginForgeFilter) {
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
