package com.github.vzakharchenko.runtime.bridge.containers;

/**
 * HTTP headers for outbound requests to the Forge Containers egress sidecar.
 *
 * @see <a
 *     href="https://developer.atlassian.com/platform/forge/containers-reference/ref-api/">Containers
 *     API contract</a>
 */
public final class ForgeEgressHeaders {

  /**
   * Authorization for egress proxy requests ({@code Forge id=…} or {@code Forge installationId=…}).
   */
  public static final String FORGE_AUTHORIZATION = "forge-proxy-authorization";

  private ForgeEgressHeaders() {}
}
