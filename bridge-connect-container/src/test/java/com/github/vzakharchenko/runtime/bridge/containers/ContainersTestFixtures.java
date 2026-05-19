package com.github.vzakharchenko.runtime.bridge.containers;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.ForgeApiContext;
import com.atlassian.connect.spring.ForgeApp;
import com.atlassian.connect.spring.ForgeInvocationToken;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;

public final class ContainersTestFixtures {

  private ContainersTestFixtures() {}

  public static AtlassianHost host(String cloudId, String installationId) {
    AtlassianHost host = new AtlassianHost();
    host.setCloudId(cloudId);
    host.setClientKey(cloudId);
    host.setInstallationId(installationId);
    host.setBaseUrl("https://" + cloudId + ".atlassian.net");
    return host;
  }

  public static AtlassianHostUser hostUser(String cloudId, String installationId, String accountId) {
    return AtlassianHostUser.builder(host(cloudId, installationId)).withUserAccountId(accountId).build();
  }

  public static ForgeAuthentication forgeAuthentication(AtlassianHostUser user) {
    ForgeInvocationToken token = new ForgeInvocationToken();
    ForgeApp app = new ForgeApp();
    app.setInstallationId(user.getHost().getInstallationId());
    token.setApp(app);
    return new ForgeAuthentication(new ForgeApiContext(token, Optional.empty(), Optional.empty()), user);
  }

  public static ForgeAuthentication forgeAuthentication(AtlassianHost host) {
    return forgeAuthentication(AtlassianHostUser.builder(host).build());
  }

  public static ObjectNode invocationContextJson(
      ObjectMapper mapper, String cloudId, String installationAri, String accountId) {
    ObjectNode root = mapper.createObjectNode();
    ObjectNode app = root.putObject("app");
    app.put("id", "ari:cloud:ecosystem::app/test-app");
    app.put("installationId", installationAri);
    ObjectNode context = root.putObject("context");
    context.put("cloudId", cloudId);
    context.put("siteUrl", "https://example.atlassian.net");
    if (accountId != null) {
      context.put("accountId", accountId);
    }
    return root;
  }
}
