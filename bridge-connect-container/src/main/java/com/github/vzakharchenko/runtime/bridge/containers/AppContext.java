package com.github.vzakharchenko.runtime.bridge.containers;

/**
 * Forge Containers invocation context, deserialized from the egress {@code /invocation/context}
 * response.
 */
public class AppContext {
  private App app;
  private Context context;

  public App getApp() {
    return app;
  }

  public void setApp(App app) {
    this.app = app;
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  /** App and installation section of the invocation context payload. */
  public static class App {
    private String id;
    private String installationId;
    private Environment environment;
    private Module module;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getInstallationId() {
      return installationId;
    }

    public void setInstallationId(String installationId) {
      this.installationId = installationId;
    }

    public Environment getEnvironment() {
      return environment;
    }

    public void setEnvironment(Environment environment) {
      this.environment = environment;
    }

    public Module getModule() {
      return module;
    }

    public void setModule(Module module) {
      this.module = module;
    }
  }

  /** Forge environment (type/id) from the context payload. */
  public static class Environment {
    private String type;
    private String id;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }

  /** Forge module (key/type) that triggered the invocation. */
  public static class Module {
    private String key;
    private String type;

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }
  }

  /** Site and user section of the invocation context payload. */
  public static class Context {
    private String cloudId;
    private String moduleKey;
    private UserAccess userAccess;
    private String siteUrl;
    private String accountId;

    public String getCloudId() {
      return cloudId;
    }

    public void setCloudId(String cloudId) {
      this.cloudId = cloudId;
    }

    public String getModuleKey() {
      return moduleKey;
    }

    public void setModuleKey(String moduleKey) {
      this.moduleKey = moduleKey;
    }

    public UserAccess getUserAccess() {
      return userAccess;
    }

    public void setUserAccess(UserAccess userAccess) {
      this.userAccess = userAccess;
    }

    public String getSiteUrl() {
      return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
      this.siteUrl = siteUrl;
    }

    public String getAccountId() {
      return accountId;
    }

    public void setAccountId(String accountId) {
      this.accountId = accountId;
    }
  }

  /** User access flags from the context payload. */
  public static class UserAccess {
    private Boolean enabled;

    public Boolean getEnabled() {
      return enabled;
    }

    public void setEnabled(Boolean enabled) {
      this.enabled = enabled;
    }
  }
}
