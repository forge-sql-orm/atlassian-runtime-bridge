package com.github.vzakharchenko.runtime.bridge.containers.exceptions;

/**
 * Non-2xx response from the Forge Containers egress sidecar ({@link
 * com.github.vzakharchenko.runtime.bridge.containers.EgressClientServiceImpl}).
 */
public class EgressRequestException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * @param message includes request type, HTTP status, and response body snippet
   */
  public EgressRequestException(String message) {
    super(message);
  }
}
