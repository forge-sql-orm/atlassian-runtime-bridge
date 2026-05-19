package com.github.vzakharchenko.runtime.bridge.containers;

/**
 * HTTP headers added by the Forge platform to inbound requests to a containerised service.
 *
 * @see <a
 *     href="https://developer.atlassian.com/platform/forge/containers-reference/ref-api/">Containers
 *     API contract</a>
 */
public final class ForgeIngressHeaders {

  /** Unique id for the invocation; forwarded to egress as {@code Forge id=…}. */
  public static final String INVOCATION_ID = "x-forge-invocation-id";

  /** Base64 invocation log metadata for structured logging (see {@link ForgeLogConstants}). */
  public static final String INVOCATION_LOGGING_ATTRIBUTES = "x-forge-invocation-log-attributes";

  private ForgeIngressHeaders() {}
}
