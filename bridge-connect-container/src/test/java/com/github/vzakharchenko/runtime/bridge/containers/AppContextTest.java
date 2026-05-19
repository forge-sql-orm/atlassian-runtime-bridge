package com.github.vzakharchenko.runtime.bridge.containers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals"})
class AppContextTest {

  @Test
  void appAccessorsRoundTrip() {
    var environment = new AppContext.Environment();
    environment.setType("development");
    environment.setId("env-1");

    var module = new AppContext.Module();
    module.setKey("java-service-ui");
    module.setType("jira:globalPage");

    var app = new AppContext.App();
    app.setId("ari:cloud:ecosystem::app/uuid");
    app.setInstallationId("ari:cloud:ecosystem::installation/inst-1");
    app.setEnvironment(environment);
    app.setModule(module);

    assertThat(app.getId()).isEqualTo("ari:cloud:ecosystem::app/uuid");
    assertThat(app.getInstallationId()).isEqualTo("ari:cloud:ecosystem::installation/inst-1");
    assertThat(app.getEnvironment()).isSameAs(environment);
    assertThat(app.getEnvironment().getType()).isEqualTo("development");
    assertThat(app.getEnvironment().getId()).isEqualTo("env-1");
    assertThat(app.getModule()).isSameAs(module);
    assertThat(app.getModule().getKey()).isEqualTo("java-service-ui");
    assertThat(app.getModule().getType()).isEqualTo("jira:globalPage");
  }

  @Test
  void contextAccessorsRoundTrip() {
    var userAccess = new AppContext.UserAccess();
    userAccess.setEnabled(Boolean.TRUE);

    var context = new AppContext.Context();
    context.setCloudId("cloud-1");
    context.setModuleKey("java-service-ui");
    context.setUserAccess(userAccess);
    context.setSiteUrl("https://example.atlassian.net");
    context.setAccountId("acc-1");

    assertThat(context.getCloudId()).isEqualTo("cloud-1");
    assertThat(context.getModuleKey()).isEqualTo("java-service-ui");
    assertThat(context.getUserAccess()).isSameAs(userAccess);
    assertThat(context.getUserAccess().getEnabled()).isTrue();
    assertThat(context.getSiteUrl()).isEqualTo("https://example.atlassian.net");
    assertThat(context.getAccountId()).isEqualTo("acc-1");
  }

  @Test
  void rootAccessorsRoundTrip() {
    var app = new AppContext.App();
    var context = new AppContext.Context();

    var payload = new AppContext();
    payload.setApp(app);
    payload.setContext(context);

    assertThat(payload.getApp()).isSameAs(app);
    assertThat(payload.getContext()).isSameAs(context);
  }
}
