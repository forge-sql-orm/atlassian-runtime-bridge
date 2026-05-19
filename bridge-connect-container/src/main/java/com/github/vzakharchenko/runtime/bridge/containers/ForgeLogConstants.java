package com.github.vzakharchenko.runtime.bridge.containers;

/**
 * Structured logging keys for Forge Container invocations (Developer Console log filtering).
 */
public final class ForgeLogConstants {

  /**
   * JSON log field for Forge invocation metadata (value from {@link
   * ForgeIngressHeaders#INVOCATION_LOGGING_ATTRIBUTES}).
   */
  public static final String INVOCATION_ATTRIBUTES = "forge_invocation";

  private ForgeLogConstants() {}
}
