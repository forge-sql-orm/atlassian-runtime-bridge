package com.github.vzakharchenko.runtime.bridge.common;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers {@link SystemCallInterceptor}; picked up by both runtime modules' component scan. */
@Configuration(proxyBeanMethods = false)
public class SystemCallWebConfiguration implements WebMvcConfigurer {

  private final ObjectProvider<SystemTokenPolicy> tokenPolicy;

  public SystemCallWebConfiguration(ObjectProvider<SystemTokenPolicy> tokenPolicy) {
    this.tokenPolicy = tokenPolicy;
  }

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry.addInterceptor(new SystemCallInterceptor(tokenPolicy.getIfAvailable()));
  }
}
