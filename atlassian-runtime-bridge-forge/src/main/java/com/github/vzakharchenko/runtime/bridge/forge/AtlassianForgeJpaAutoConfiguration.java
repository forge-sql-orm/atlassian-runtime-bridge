package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.AtlassianHostMappingRepository;
import com.atlassian.connect.spring.ForgeSystemAccessTokenRepository;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Atlassian Connect add-ons using Spring Data JPA and Liquibase.
 */
@Configuration
@AutoConfigureAfter({DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureBefore(LiquibaseAutoConfiguration.class)
@EnableJpaRepositories(basePackageClasses = {
        AtlassianHostMappingRepository.class,
        ForgeSystemAccessTokenRepository.class,
})
@EnableJpaAuditing
@PropertySource("classpath:config/atlassian-connect-spring-boot-jpa-starter.properties")

@ConditionalOnMissingClass("com.atlassian.connect.spring.internal.jpa.AtlassianJpaAutoConfiguration")
public class AtlassianForgeJpaAutoConfiguration {
}
